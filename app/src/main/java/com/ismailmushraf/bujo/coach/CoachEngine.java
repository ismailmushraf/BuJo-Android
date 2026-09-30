package com.ismailmushraf.bujo.coach;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import com.ismailmushraf.bujo.R;
import com.ismailmushraf.bujo.db.DatabaseManager;
import com.ismailmushraf.bujo.models.Entry;
import com.ismailmushraf.bujo.models.Habit;
import com.ismailmushraf.bujo.models.Project;
import okhttp3.*;
import org.conscrypt.Conscrypt;
import org.json.*;
import java.io.IOException;
import java.io.InputStream;
import java.io.ByteArrayOutputStream;
import java.security.Security;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;

/** Application-owned work survives screen changes; no Activity or View is retained. */
public final class CoachEngine {
    private static final String TAG = "BuJoCoach";
    private static CoachEngine instance;
    private final Context context;
    private final ExecutorService worker=Executors.newSingleThreadExecutor();
    private final Handler main=new Handler(Looper.getMainLooper());
    private final Set<Runnable> listeners=new HashSet<>();
    private final AtomicBoolean busy=new AtomicBoolean();
    private OkHttpClient client;
    private boolean secureTransport;
    private String notice="";
    private CoachEngine(Context context) {
        this.context=context.getApplicationContext();
        worker.execute(() -> {
            try {
                Security.insertProviderAt(Conscrypt.newProvider(),1);
                secureTransport=Security.getProviders().length>0 && Conscrypt.isConscrypt(Security.getProviders()[0]);
                if(secureTransport) client=new OkHttpClient.Builder().connectTimeout(15,TimeUnit.SECONDS)
                        .readTimeout(45,TimeUnit.SECONDS).writeTimeout(15,TimeUnit.SECONDS)
                        .callTimeout(60,TimeUnit.SECONDS).followRedirects(false).followSslRedirects(false).build();
            } catch (RuntimeException | LinkageError error) {
                Log.e(TAG, "Could not initialise secure coach networking", error);
                secureTransport=false;
            }
            try(CoachStore store=new CoachStore(this.context)) { store.recoverInterrupted(); }
            catch(RuntimeException error) {
                Log.e(TAG, "Could not recover coach history", error);
                changed(this.context.getString(R.string.coach_storage_error));
            }
        });
    }
    public static synchronized void initialize(Context context) {
        if(instance==null) instance=new CoachEngine(context);
    }
    public static CoachEngine get(Context context) { initialize(context); return instance; }
    public void listen(Runnable listener) { listeners.add(listener); }
    public void unlisten(Runnable listener) { listeners.remove(listener); }
    public boolean isBusy() { return busy.get(); }
    public String notice() { return notice; }
    private void changed(String text) {
        main.post(() -> {
            notice=text;
            for(Runnable listener:new ArrayList<>(listeners)) {
                try {
                    listener.run();
                } catch (RuntimeException error) {
                    Log.e(TAG, "Coach screen refresh failed", error);
                }
            }
        });
    }
    public static String today() { return new SimpleDateFormat("yyyy-MM-dd",Locale.US).format(new Date()); }
    public interface HistoryListener { void loaded(List<CoachStore.Record> records, String plan); }
    public void load(HistoryListener result) {
        worker.execute(() -> {
            try(CoachStore store=new CoachStore(context)) {
                List<CoachStore.Record> records=store.recent(); String active=store.activePlan(today());
                main.post(() -> result.loaded(records,active));
            } catch(RuntimeException ignored) {
                main.post(() -> result.loaded(Collections.emptyList(),""));
            }
        });
    }
    public boolean submit(JSONObject checkin, boolean online) {
        return submit(checkin,online,null);
    }
    public interface GuidanceListener { void complete(CoachStore.Record record); }
    public boolean submit(JSONObject checkin, boolean online, GuidanceListener listener) {
        if(!busy.compareAndSet(false,true)) return false;
        changed(context.getString(R.string.coach_working));
        worker.execute(() -> {
            long id=-1;
            String message=context.getString(R.string.coach_saved);
            CoachStore.Record completed=null;
            try(CoachStore store=new CoachStore(context)) {
                JSONObject snapshot=snapshot(checkin);
                id=store.insert(snapshot,"pending");
                CoachPreferences.get(context).edit().putLong("last_checkin",System.currentTimeMillis()).apply();
                JSONObject fallback=localAnswer(snapshot);
                store.finish(id,fallback.toString(),"pending");
                SharedPreferences prefs=CoachPreferences.get(context);
                if(online && prefs.getBoolean("consent",false) && !prefs.getString("key","").isEmpty()) {
                    try {
                        JSONObject answer=CoachProtocol.parse(send(CoachProtocol.request(snapshot)),snapshot);
                        store.finish(id,answer.toString(),"ai");
                    } catch (Exception e) {
                        store.finish(id,fallback.toString(),"failed");
                        message=failure(e);
                    }
                } else {
                    store.finish(id,fallback.toString(),"local");
                    message=!prefs.getBoolean("consent",false)
                            ? context.getString(R.string.coach_consent_required)
                            : prefs.getString("key","").isEmpty()
                            ? context.getString(R.string.coach_key_required_online)
                            : context.getString(R.string.coach_local_notice);
                }
                for(CoachStore.Record record:store.recent()) if(record.id==id) { completed=record; break; }
            } catch (Throwable error) {
                Log.e(TAG, "Coach check-in failed before a response could be saved", error);
                message=displayError(context.getString(R.string.coach_storage_error), error);
            } finally { busy.set(false); changed(message); final CoachStore.Record result=completed;
                if(listener!=null) main.post(() -> listener.complete(result)); }
        });
        return true;
    }
    private JSONObject snapshot(JSONObject input) throws JSONException {
        JSONObject snapshot=new JSONObject(input.toString()).put("day",today())
                .put("timezone",TimeZone.getDefault().getID());
        SharedPreferences prefs=CoachPreferences.get(context);
        JSONArray tasks=new JSONArray(), habits=new JSONArray(), projects=new JSONArray();
        DatabaseManager db=new DatabaseManager(context); db.open();
        try {
            if(prefs.getBoolean("share_tasks",false)) {
                for(Entry entry:db.getTodayEntries()) {
                    if(!"*".equals(entry.getSignifier()) || tasks.length()>=30) continue;
                    tasks.put(new JSONObject().put("id",entry.getId()).put("text",clip(entry.getContent(),300))
                            .put("completed",entry.isCompleted()).put("locked",entry.isLocked())
                            .put("deadline",entry.getDeadline()));
                }
                List<Project> all=db.getAllProjects();
                Collections.sort(all,(a,b) -> b.getWeight()-a.getWeight());
                for(Project project:all) {
                    if(projects.length()>=15) break;
                    JSONObject item=new JSONObject().put("id",project.getId()).put("name",clip(project.getName(),120))
                            .put("priority_weight",project.getWeight()).put("unfinished",new JSONArray()).put("recent_completed",new JSONArray());
                    for(Entry entry:db.getEntriesForProject(project.getId()))
                        if(!entry.isCompleted() && item.getJSONArray("unfinished").length()<5)
                            item.getJSONArray("unfinished").put(clip(entry.getContent(),180));
                    for(Entry entry:db.getCompletedEntriesForProject(project.getId()))
                        if(item.getJSONArray("recent_completed").length()<3) item.getJSONArray("recent_completed").put(clip(entry.getContent(),180));
                    projects.put(item);
                }
            }
            if(prefs.getBoolean("share_habits",false)) {
                Calendar c = Calendar.getInstance();
                c.add(Calendar.DATE, -7);
                String sevenDaysAgo = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(c.getTime());
                for(Habit habit:db.getAllHabits()) {
                    if(habits.length()>=20 || habit.getStartDate().compareTo(today())>0) continue;
                    Map<String, Boolean> compMap = db.getHabitCompletionMap(habit.getId(), sevenDaysAgo, today());
                    int recent7Completed = 0;
                    for (Boolean b : compMap.values()) {
                        if (Boolean.TRUE.equals(b)) recent7Completed++;
                    }
                    habits.put(new JSONObject().put("name",clip(habit.getName(),200))
                            .put("completed_today", Boolean.TRUE.equals(compMap.get(today())))
                            .put("recent_7_days_completed", recent7Completed)
                            .put("total_completions", db.getHabitTotalCompletions(habit.getId())));
                }
            }
        } finally { db.close(); }
        Calendar yCal = Calendar.getInstance();
        yCal.add(Calendar.DATE, -1);
        String yesterdayStr = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(yCal.getTime());
        snapshot.put("yesterday", yesterdayStr);
        snapshot.put("coach_vibe", CoachPreferences.vibe(context));
        return snapshot.put("tasks",tasks).put("habits",habits).put("projects",projects);
    }
    private JSONObject localAnswer(JSONObject snapshot) throws JSONException {
        boolean rest=snapshot.optInt("available_minutes",0)==0;
        int mins=snapshot.optInt("available_minutes",30);
        JSONArray suggestions=new JSONArray();
        if(!rest) {
            String breakdownTask=snapshot.optString("breakdown_task","").trim();
            if(!breakdownTask.isEmpty()) {
                String task=clip(breakdownTask,120);
                suggestions.put(new JSONObject().put("title","Define the first small action for " + task)
                        .put("project_id",0).put("estimated_minutes",15)
                        .put("reason","Start by making the next action clear and concrete."));
                suggestions.put(new JSONObject().put("title","Do one 15-minute piece of " + task)
                        .put("project_id",0).put("estimated_minutes",15)
                        .put("reason","A short focused block makes the task easier to begin."));
                suggestions.put(new JSONObject().put("title","Review progress and write the next step for " + task)
                        .put("project_id",0).put("estimated_minutes",15)
                        .put("reason","Close the loop so the task is ready for the next session."));
                return new JSONObject().put("message",context.getString(R.string.coach_local_message))
                        .put("suggestions",suggestions);
            }
            JSONArray projects=snapshot.optJSONArray("projects");
            if(projects!=null && projects.length()>0) {
                List<JSONObject> selected=new ArrayList<>();
                Set<Integer> representedWeights=new HashSet<>();
                // Cover each priority level first, then fill any remaining slots by priority.
                for(int i=0;i<projects.length() && selected.size()<5;i++) {
                    JSONObject project=projects.getJSONObject(i);
                    if(representedWeights.add(project.optInt("priority_weight",1))) selected.add(project);
                }
                for(int i=0;i<projects.length() && selected.size()<5;i++) {
                    JSONObject project=projects.getJSONObject(i);
                    if(!selected.contains(project)) selected.add(project);
                }
                int count=Math.min(selected.size(), Math.max(1, mins));
                int totalWeight=0;
                for(int i=0;i<count;i++) totalWeight+=Math.max(1,selected.get(i).optInt("priority_weight",1));
                int remaining=mins;
                for(int i=0;i<count;i++) {
                    JSONObject project=selected.get(i);
                    int slotsLeft=count-i-1;
                    int allocation=i==count-1 ? remaining : Math.max(1,
                            Math.min(remaining-slotsLeft, Math.round((float)mins*Math.max(1,project.optInt("priority_weight",1))/totalWeight)));
                    remaining-=allocation;
                    suggestions.put(new JSONObject().put("title","Take one meaningful next step for "+project.getString("name"))
                            .put("project_id",project.getInt("id")).put("estimated_minutes",allocation)
                            .put("reason","Offline suggestion balanced by this project’s priority."));
                }
            }
            if(suggestions.length()==0) suggestions.put(new JSONObject()
                    .put("title","Write down the smallest next action for today")
                    .put("project_id",0).put("estimated_minutes", Math.max(1, mins))
                    .put("reason","A short offline starting point while guidance is unavailable."));
        }
        return new JSONObject().put("message",context.getString(R.string.coach_local_message))
                .put("suggestions",suggestions);
    }
    private static String clip(String text,int max) {
        return text==null ? "" : text.substring(0,Math.min(max,text.length()));
    }
    private String send(JSONObject payload) throws Exception {
        if(!secureTransport || client==null) throw new IOException("TLS");
        if(!hasUsableNetwork()) throw new IOException("NETWORK_UNVALIDATED");
        String model=CoachPreferences.model(context);
        if(!CoachPreferences.validModel(model)) throw new IOException("MODEL");
        String key=CoachPreferences.get(context).getString("key","");
        Log.d(TAG, "Starting Gemini request with model " + model);
        Request request=new Request.Builder()
                .url("https://generativelanguage.googleapis.com/v1beta/models/"+model+":generateContent")
                .header("x-goog-api-key",key)
                .post(RequestBody.create(MediaType.parse("application/json; charset=utf-8"),payload.toString())).build();
        try(Response response=client.newCall(request).execute()) {
            Log.d(TAG, "Gemini response: HTTP " + response.code());
            if(!response.isSuccessful()) throw new IOException("HTTP_"+response.code());
            if(response.body()==null) throw new IOException("EMPTY");
            // Do not buffer unbounded server output on a legacy device.
            try(InputStream input=response.body().byteStream(); ByteArrayOutputStream bytes=new ByteArrayOutputStream()) {
                byte[] buffer=new byte[4096]; int count;
                while((count=input.read(buffer))!=-1) {
                    if(bytes.size()+count>128*1024) throw new IOException("SIZE");
                    bytes.write(buffer,0,count);
                }
                return bytes.toString("UTF-8");
            }
        }
    }
    private boolean hasUsableNetwork() {
        ConnectivityManager manager=(ConnectivityManager)context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if(manager==null) return false;
        if(Build.VERSION.SDK_INT>=23) {
            Network network=manager.getActiveNetwork();
            NetworkCapabilities capabilities=network==null?null:manager.getNetworkCapabilities(network);
            return capabilities!=null && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
                    && capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED);
        }
        @SuppressWarnings("deprecation")
        NetworkInfo info=manager.getActiveNetworkInfo();
        return info!=null && info.isConnected();
    }
    private String failure(Exception error) {
        Log.e(TAG, "Gemini coach request failed", error);
        String code=error.getMessage();
        int resource=R.string.coach_network_error;
        if("HTTP_400".equals(code) || "HTTP_401".equals(code) || "HTTP_403".equals(code)) resource=R.string.coach_key_error;
        else if("HTTP_404".equals(code) || "MODEL".equals(code)) resource=R.string.coach_model_error;
        else if("HTTP_429".equals(code)) resource=R.string.coach_quota_error;
        else if("NETWORK_UNVALIDATED".equals(code)) resource=R.string.coach_no_internet;
        else if("TLS".equals(code)) resource=R.string.coach_tls_error;
        else if(error instanceof JSONException) resource=R.string.coach_response_error;
        return displayError(context.getString(resource), error);
    }
    private static String displayError(String summary, Throwable error) {
        String detail=error.getMessage();
        if(detail==null || detail.trim().isEmpty()) detail=error.getClass().getSimpleName();
        detail=detail.replace('\n',' ').replace('\r',' ').trim();
        if(detail.length()>180) detail=detail.substring(0,180);
        return summary + "\n\nError: " + detail;
    }
    public void testConnection() {
        if(!busy.compareAndSet(false,true)) return;
        changed(context.getString(R.string.coach_working));
        worker.execute(() -> {
            String result;
            try {
                JSONObject sample=new JSONObject().put("day",today()).put("kind","connection test")
                        .put("available_minutes",5).put("tasks",new JSONArray()).put("habits",new JSONArray());
                CoachProtocol.parse(send(CoachProtocol.request(sample)),sample);
                result=context.getString(R.string.coach_connection_ok);
            } catch(Throwable error) {
                Log.e(TAG, "Coach connection test failed", error);
                result=error instanceof Exception ? failure((Exception)error)
                        : displayError(context.getString(R.string.coach_network_error),error);
            }
            busy.set(false); changed(result);
        });
    }
    public void accept(CoachStore.Record record) {
        if(!busy.compareAndSet(false,true)) return;
        worker.execute(() -> {
            String result;
            try(CoachStore store=new CoachStore(context)) {
                JSONObject input=new JSONObject(record.input), answer=new JSONObject(record.response);
                if(!today().equals(input.getString("day"))) throw new JSONException("Expired");
                JSONObject current=snapshot(input);
                CoachProtocol.validate(answer,current);
                if(!input.getJSONArray("tasks").toString().equals(current.getJSONArray("tasks").toString()))
                    throw new JSONException("Tasks changed");
                boolean saved=store.accept(record.id,today(),CoachProtocol.display(answer));
                result=context.getString(saved?R.string.coach_plan_saved:R.string.coach_already_applied);
            } catch(Exception e) { result=context.getString(R.string.coach_stale); }
            busy.set(false); changed(result);
        });
    }
    public void clear(boolean history) {
        if(!busy.compareAndSet(false,true)) return;
        worker.execute(() -> {
            String result=context.getString(R.string.coach_saved);
            try(CoachStore store=new CoachStore(context)) {
                if(history) store.clear(); else store.undo(today());
            } catch(RuntimeException e) { result=context.getString(R.string.coach_storage_error); }
            finally { busy.set(false); changed(result); }
        });
    }
}
