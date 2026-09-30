package com.ismailmushraf.bujo.fragments;

import android.app.Dialog;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.*;
import android.widget.*;
import androidx.annotation.NonNull;
import androidx.fragment.app.DialogFragment;
import com.ismailmushraf.bujo.R;
import com.ismailmushraf.bujo.coach.*;
import com.ismailmushraf.bujo.db.DatabaseManager;
import com.ismailmushraf.bujo.models.*;
import com.ismailmushraf.bujo.utils.AppExecutors;
import org.json.*;
import java.util.*;

/** The coach is a conversational Plan Day flow. */
public final class CoachWizardDialog extends DialogFragment {
    private static final String TAG = "coach_wizard";
    private static final String BREAKDOWN_TAG = "coach_task_breakdown";
    private static final String ARG_BREAKDOWN_TASK = "breakdown_task";
    private static final String ARG_BREAKDOWN_PARENT_ID = "breakdown_parent_id";
    private static final String ARG_BREAKDOWN_PROJECT_ID = "breakdown_project_id";
    private static final String ARG_BREAKDOWN_PROJECT_TAG = "breakdown_project_tag";
    private static final String ARG_BREAKDOWN_DEADLINE = "breakdown_deadline";
    private View root;
    private int step = 0;
    private final ArrayList<JSONObject> suggestions = new ArrayList<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable typewriterRunnable;
    private String breakdownTask = "";
    private int breakdownParentId;
    private int breakdownProjectId;
    private String breakdownProjectTag;
    private long breakdownDeadline;
    private int scheduleStartMinutes;
    private int scheduleEndMinutes;
    private int chatQuestion;
    private boolean refiningPlan;
    private String feeling = "";
    private String reflection = "";
    private String gratitude = "";
    private CoachStore.Record latestRecord;
    private String latestPlan = "";

    public static void show(androidx.fragment.app.FragmentManager manager) {
        if (manager.findFragmentByTag(TAG) == null)
            new CoachWizardDialog().show(manager, TAG);
    }

    /** Opens Coach directly in a focused flow for splitting one task into small actions. */
    public static void showBreakdown(androidx.fragment.app.FragmentManager manager, Entry task) {
        if (manager.isStateSaved()) return;
        if (task == null || task.getId() <= 0) return;
        CoachWizardDialog dialog = new CoachWizardDialog();
        Bundle args = new Bundle();
        args.putString(ARG_BREAKDOWN_TASK, task.getContent() == null ? "" : task.getContent().trim());
        args.putInt(ARG_BREAKDOWN_PARENT_ID, task.getId());
        args.putInt(ARG_BREAKDOWN_PROJECT_ID, task.getProjectId());
        args.putString(ARG_BREAKDOWN_PROJECT_TAG, task.getProjectTag());
        args.putLong(ARG_BREAKDOWN_DEADLINE, task.getDeadline());
        dialog.setArguments(args);
        // This is called from a sidebar click. Attach it immediately rather than queueing it
        // behind any fragment transaction that may still be removing a previous Coach dialog.
        dialog.showNow(manager, BREAKDOWN_TAG + "_" + android.os.SystemClock.elapsedRealtime());
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle state) {
        Bundle args = getArguments();
        breakdownTask = args == null ? "" : args.getString(ARG_BREAKDOWN_TASK, "").trim();
        breakdownParentId = args == null ? 0 : args.getInt(ARG_BREAKDOWN_PARENT_ID);
        breakdownProjectId = args == null ? 0 : args.getInt(ARG_BREAKDOWN_PROJECT_ID);
        breakdownProjectTag = args == null ? null : args.getString(ARG_BREAKDOWN_PROJECT_TAG);
        breakdownDeadline = args == null ? 0 : args.getLong(ARG_BREAKDOWN_DEADLINE);
        root = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_coach_wizard, null);
        Dialog d = new android.app.AlertDialog.Builder(requireContext(), R.style.BujoDialog).setView(root).create();
        // A plan can be in progress; leaving is always an explicit Cancel action.
        setCancelable(false);
        d.setCanceledOnTouchOutside(false);

        root.findViewById(R.id.coach_wizard_cancel_step1).setOnClickListener(v -> dismiss());
        root.findViewById(R.id.coach_wizard_cancel_step2).setOnClickListener(v -> dismiss());
        root.findViewById(R.id.coach_wizard_cancel_step3).setOnClickListener(v -> dismiss());
        root.findViewById(R.id.coach_wizard_next_step1).setOnClickListener(v -> next());
        root.findViewById(R.id.coach_wizard_next_step2).setOnClickListener(v -> next());
        root.findViewById(R.id.coach_wizard_back_step2).setOnClickListener(v -> {
            step--;
            render();
        });
        root.findViewById(R.id.coach_wizard_add_step3).setOnClickListener(v -> addSelected());

        root.findViewById(R.id.coach_refine_simpler).setOnClickListener(v -> beginRefinement());

        setupScheduleControls();

        if (!breakdownTask.isEmpty()) {
            step = 2;
            ((TextView) root.findViewById(R.id.coach_wizard_message)).setText("Breaking this task into small, actionable steps…");
            ((TextView) root.findViewById(R.id.coach_results_selection_label)).setText("SELECT SUBTASKS TO ADD");
            ((TextView) root.findViewById(R.id.coach_wizard_add_step3)).setText("Add selected subtasks");
            root.findViewById(R.id.coach_wizard_add_step3).setEnabled(false);
            root.findViewById(R.id.coach_refine_simpler).setVisibility(View.GONE);
            root.findViewById(R.id.coach_refine_work).setVisibility(View.GONE);
            root.findViewById(R.id.coach_refine_low_effort).setVisibility(View.GONE);
        }
        render();
        if (breakdownTask.isEmpty()) showQuestion();
        if (!breakdownTask.isEmpty()) root.post(this::submitBreakdown);
        return d;
    }

    private void next() {
        if (!breakdownTask.isEmpty()) return;
        String answer = text(R.id.coach_chat_reply);
        if (chatQuestion == 0) feeling = answer;
        else if (chatQuestion == 1) reflection = answer;
        else if (chatQuestion == 2) gratitude = answer;
        else if (chatQuestion == 4) {
            if (answer.isEmpty()) {
                Toast.makeText(requireContext(), "Tell Nova what you would like to change.", Toast.LENGTH_SHORT).show();
                return;
            }
            addUserBubble(answer);
            submitCheckin(answer);
            return;
        }
        if (chatQuestion == 3) {
            int minutes = scheduleEndMinutes - scheduleStartMinutes;
            if (minutes < 0 || minutes > 960) {
                Toast.makeText(requireContext(), R.string.coach_invalid_schedule, Toast.LENGTH_SHORT).show();
                return;
            }
            addUserBubble(formatScheduleTime(scheduleStartMinutes) + " to " + formatScheduleTime(scheduleEndMinutes));
            submitCheckin(null);
            return;
        }
        addUserBubble(answer.isEmpty() ? "I’d rather skip this for now." : answer);
        chatQuestion++;
        showQuestion();
    }

    private void showQuestion() {
        LinearLayout chips = root.findViewById(R.id.coach_chat_chips);
        EditText reply = root.findViewById(R.id.coach_chat_reply);
        View schedule = root.findViewById(R.id.coach_chat_schedule);
        TextView next = root.findViewById(R.id.coach_wizard_next_step1);
        chips.removeAllViews();
        reply.setText("");
        reply.setVisibility(View.VISIBLE);
        schedule.setVisibility(View.GONE);

        if (chatQuestion == 0) {
            addNovaBubble("Good morning. Before we plan anything, how are you feeling about today?");
            reply.setHint("Or tell Nova in your own words…");
            addAnswerChip(chips, "Calm");
            addAnswerChip(chips, "Focused");
            addAnswerChip(chips, "Overwhelmed");
            next.setText("Next");
        } else if (chatQuestion == 1) {
            addNovaBubble("Is anything taking up space in your mind today?");
            reply.setHint("Share only what feels useful…");
            addAnswerChip(chips, "Skip");
            next.setText("Next");
        } else if (chatQuestion == 2) {
            addNovaBubble("Before we continue, what are you grateful for today? You can share one thing or a few.");
            reply.setHint("I’m grateful for…");
            addAnswerChip(chips, "Skip");
            next.setText("Next");
        } else if (chatQuestion == 3) {
            addNovaBubble("When would you like to start, and when are you done for the day? I’ll plan only within that window.");
            reply.setVisibility(View.GONE);
            schedule.setVisibility(View.VISIBLE);
            next.setText("Plan my day");
        } else {
            addNovaBubble("What would you like to change about this plan?");
            reply.setHint("For example: fewer tasks, more work, or lower effort…");
            next.setText("Refine plan");
        }
        scrollChatToBottom();
    }

    private void addAnswerChip(LinearLayout parent, String answer) {
        TextView chip = new TextView(requireContext());
        chip.setText(answer);
        chip.setTextSize(12);
        chip.setTextColor(android.graphics.Color.WHITE);
        chip.setGravity(Gravity.CENTER);
        chip.setBackgroundResource(R.drawable.shape_chip_selected);
        int horizontal = dp(10);
        chip.setPadding(horizontal, dp(6), horizontal, dp(6));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.setMargins(dp(4), 0, 0, 0);
        parent.addView(chip, params);
        chip.setOnClickListener(v -> {
            ((EditText) root.findViewById(R.id.coach_chat_reply)).setText(answer.equals("Skip") ? "" : answer);
            next();
        });
    }

    private void addNovaBubble(String message) {
        TextView bubble = new TextView(requireContext());
        bubble.setText(message);
        bubble.setTextColor(androidx.core.content.ContextCompat.getColor(requireContext(), R.color.bujo_text));
        bubble.setTextSize(15);
        bubble.setBackgroundResource(R.drawable.shape_coach_bubble);
        bubble.setPadding(dp(12), dp(10), dp(12), dp(10));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.START;
        params.setMargins(0, 0, dp(32), dp(10));
        ((LinearLayout) root.findViewById(R.id.coach_chat_messages)).addView(bubble, params);
    }

    private void addUserBubble(String answer) {
        TextView bubble = new TextView(requireContext());
        bubble.setText(answer);
        bubble.setTextColor(android.graphics.Color.WHITE);
        bubble.setTextSize(14);
        bubble.setBackgroundResource(R.drawable.shape_chip_selected);
        bubble.setPadding(dp(12), dp(9), dp(12), dp(9));
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT, LinearLayout.LayoutParams.WRAP_CONTENT);
        params.gravity = Gravity.END;
        params.setMargins(dp(32), 0, 0, dp(14));
        ((LinearLayout) root.findViewById(R.id.coach_chat_messages)).addView(bubble, params);
    }

    private void scrollChatToBottom() {
        ScrollView scroll = root.findViewById(R.id.coach_chat_scroll);
        scroll.post(() -> scroll.fullScroll(View.FOCUS_DOWN));
    }

    private int dp(int value) {
        return (int) (value * getResources().getDisplayMetrics().density + .5f);
    }

    private void beginRefinement() {
        refiningPlan = true;
        chatQuestion = 4;
        step = 0;
        root.findViewById(R.id.coach_wizard_next_step1).setEnabled(true);
        ((LinearLayout) root.findViewById(R.id.coach_chat_messages)).removeAllViews();
        render();
        showQuestion();
    }

    private void submitCheckin(String refinementNote) {
        int minutes = scheduleEndMinutes - scheduleStartMinutes;
        if (minutes < 0 || minutes > 960) {
            Toast.makeText(requireContext(), R.string.coach_invalid_schedule, Toast.LENGTH_SHORT).show();
            return;
        }
        try {
            JSONArray gratitudeItems = new JSONArray();
            if (!gratitude.isEmpty()) gratitudeItems.put(gratitude);
            JSONObject input = new JSONObject()
                    .put("kind", "day planning")
                    // Retain these fields so existing check-in history remains readable.
                    .put("mood", feeling)
                    .put("energy", "Not specified")
                    .put("feeling", feeling)
                    .put("available_minutes", minutes)
                    .put("schedule_start", formatScheduleTime(scheduleStartMinutes))
                    .put("schedule_end", formatScheduleTime(scheduleEndMinutes))
                    .put("reflection", reflection)
                    .put("gratitude", gratitudeItems)
                    .put("note", reflection);
            if (refinementNote != null && !refinementNote.trim().isEmpty()) {
                input.put("refinement_note", refinementNote);
            }
            submit(input, minutes, refinementNote);
        } catch (Exception e) {
            error();
        }
    }

    private void setupScheduleControls() {
        Calendar now = Calendar.getInstance();
        int currentMinute = now.get(Calendar.HOUR_OF_DAY) * 60 + now.get(Calendar.MINUTE);
        scheduleStartMinutes = Math.max(6 * 60, Math.min(currentMinute, 20 * 60));
        scheduleEndMinutes = 20 * 60;
        updateScheduleLabels();
        root.findViewById(R.id.coach_wizard_start_time).setOnClickListener(v -> showTimePicker(true));
        root.findViewById(R.id.coach_wizard_end_time).setOnClickListener(v -> showTimePicker(false));
    }

    private void showTimePicker(boolean start) {
        int selected = start ? scheduleStartMinutes : scheduleEndMinutes;
        new android.app.TimePickerDialog(requireContext(), (view, hour, minute) -> {
            if (start) scheduleStartMinutes = hour * 60 + minute;
            else scheduleEndMinutes = hour * 60 + minute;
            updateScheduleLabels();
        }, selected / 60, selected % 60, false).show();
    }

    private void updateScheduleLabels() {
        ((TextView) root.findViewById(R.id.coach_wizard_start_time)).setText("Start · " + formatScheduleTime(scheduleStartMinutes));
        ((TextView) root.findViewById(R.id.coach_wizard_end_time)).setText("Done · " + formatScheduleTime(scheduleEndMinutes));
    }

    private String formatScheduleTime(int minutes) {
        int hour24 = minutes / 60;
        int hour12 = hour24 % 12;
        if (hour12 == 0) hour12 = 12;
        return String.format(Locale.getDefault(), "%d:%02d %s", hour12, minutes % 60,
                hour24 < 12 ? "AM" : "PM");
    }

    private String text(int viewId) {
        return ((EditText) root.findViewById(viewId)).getText().toString().trim();
    }

    /** Sends only the selected task and fixed micro-action constraints; no check-in is required. */
    private void submitBreakdown() {
        try {
            JSONObject input = new JSONObject()
                    .put("kind", "task breakdown")
                    .put("available_minutes", 45)
                    .put("note", "Break down: " + breakdownTask)
                    .put("breakdown_task", breakdownTask);
            submit(input, 45, null);
        } catch (JSONException error) {
            error();
        }
    }

    private void submit(JSONObject input, int targetMinutes, String refinementNote) {
        try {
            CoachEngine engine = CoachEngine.get(requireContext());
            if (!engine.submit(input, true, record -> {
                if (!isAdded()) return;
                setRefineButtonsEnabled(true);
                if (record == null) {
                    error();
                    return;
                }
                try {
                    JSONObject answer = new JSONObject(record.response);
                    latestRecord = record;
                    latestPlan = CoachProtocol.display(answer);
                    JSONArray list = answer.getJSONArray("suggestions");
                    suggestions.clear();
                    for (int i = 0; i < list.length(); i++) suggestions.add(list.getJSONObject(i));
                    String message = answer.getString("message");
                    if (!"ai".equals(record.status)) message += "\n\nOffline fallback: " + engine.notice();
                    showResults(message, targetMinutes);
                    if (!breakdownTask.isEmpty()) root.findViewById(R.id.coach_wizard_add_step3).setEnabled(true);
                } catch (Exception e) { error(); }
            })) {
                ((TextView) root.findViewById(R.id.nova_status)).setText("Nova is already working. Please wait a moment.");
                setRefineButtonsEnabled(true);
                return;
            }
            ((TextView) root.findViewById(R.id.nova_status)).setText("Nova is thinking…");
            root.findViewById(R.id.coach_wizard_next_step1).setEnabled(false);
            setRefineButtonsEnabled(false);
        } catch (Exception e) { error(); }
    }

    private void setRefineButtonsEnabled(boolean enabled) {
        root.findViewById(R.id.coach_refine_simpler).setEnabled(enabled);
        root.findViewById(R.id.coach_refine_work).setEnabled(enabled);
        root.findViewById(R.id.coach_refine_low_effort).setEnabled(enabled);
    }

    private void error() {
        if (!isAdded()) return;
        ((TextView) root.findViewById(R.id.nova_status)).setText("Nova needs a moment to reconnect");
        root.findViewById(R.id.coach_wizard_next_step1).setEnabled(true);
        setRefineButtonsEnabled(true);
    }

    private void showResults(String message, int minutes) {
        step = 2;
        Button refine = root.findViewById(R.id.coach_refine_simpler);
        // This is now the single visible Refine action. Re-enable it explicitly after a
        // completed request instead of inheriting the old three-button request state.
        refine.setEnabled(true);
        refine.setClickable(true);
        refine.setOnClickListener(v -> beginRefinement());
        animateTypewriter(message);

        LinearLayout list = root.findViewById(R.id.coach_wizard_suggestions);
        list.removeAllViews();
        Map<Integer, String> names = projects();

        for (JSONObject item : suggestions) {
            View row = LayoutInflater.from(requireContext()).inflate(R.layout.item_coach_suggestion, list, false);
            int id = item.optInt("project_id");
            int taskEst = item.optInt("estimated_minutes", 0);
            if (taskEst <= 0) {
                taskEst = (!suggestions.isEmpty() && minutes > 0) ? Math.max(5, minutes / suggestions.size()) : 15;
            }
            String effortLabel = "⏱️ ~" + taskEst + "m";
            ((TextView) row.findViewById(R.id.coach_suggestion_title)).setText(item.optString("title"));
            ((TextView) row.findViewById(R.id.coach_suggestion_project)).setText(!breakdownTask.isEmpty()
                    ? "SUBTASK"
                    : (id == 0 ? "GENERAL" : "PROJECT · " + names.get(id)));
            ((TextView) row.findViewById(R.id.coach_suggestion_effort)).setText(effortLabel);
            ((TextView) row.findViewById(R.id.coach_suggestion_reason)).setText(item.optString("reason"));
            row.setTag(item);
            row.setOnClickListener(v -> {
                boolean selected = !Boolean.TRUE.equals(v.getTag(R.id.coach_suggestion_tick));
                v.setTag(R.id.coach_suggestion_tick, selected);
                v.findViewById(R.id.coach_suggestion_tick).setBackgroundResource(selected ? R.drawable.bb10_checkbox_checked_bg : R.drawable.bb10_checkbox_unchecked_bg);
            });
            list.addView(row);
        }
        if (suggestions.isEmpty()) {
            TextView none = new TextView(requireContext());
            none.setText("No tasks were suggested. You can keep your current plan.");
            none.setPadding(12, 12, 12, 12);
            list.addView(none);
        }
        render();
    }

    private void animateTypewriter(final String fullText) {
        if (typewriterRunnable != null) {
            handler.removeCallbacks(typewriterRunnable);
        }
        final TextView msgView = root.findViewById(R.id.coach_wizard_message);
        msgView.setText("");
        final int textLength = fullText.length();

        typewriterRunnable = new Runnable() {
            private int index = 0;

            @Override
            public void run() {
                if (!isAdded()) return;
                int stepChars = Math.min(3, textLength - index);
                index += stepChars;
                msgView.setText(fullText.substring(0, index));
                if (index < textLength) {
                    handler.postDelayed(this, 15);
                }
            }
        };
        handler.post(typewriterRunnable);
    }

    private Map<Integer, String> projects() {
        Map<Integer, String> map = new HashMap<>();
        DatabaseManager db = new DatabaseManager(requireContext());
        db.open();
        try {
            for (Project p : db.getAllProjects())
                map.put(p.getId(), p.getName());
        } finally {
            db.close();
        }
        return map;
    }

    private void addSelected() {
        DatabaseManager db = new DatabaseManager(requireContext());
        int count = 0;
        int duplicatesSkipped = 0;
        try {
            db.open();
            Map<Integer, String> names = projects();
            ArrayList<Entry> selected = new ArrayList<>();
            LinearLayout list = root.findViewById(R.id.coach_wizard_suggestions);
            for (int i = 0; i < list.getChildCount(); i++) {
                View child = list.getChildAt(i);
                if (Boolean.TRUE.equals(child.getTag(R.id.coach_suggestion_tick))) {
                    JSONObject item = (JSONObject) child.getTag();
                    int project = breakdownParentId > 0 ? breakdownProjectId : item.optInt("project_id");
                    String title = item.getString("title").trim();
                    boolean alreadyExists = breakdownParentId > 0
                            ? db.hasUnfinishedSubtask(breakdownParentId, title)
                            : db.hasUnfinishedTopLevelTask(title, project);
                    if (alreadyExists) {
                        duplicatesSkipped++;
                        continue;
                    }
                    Entry e = new Entry();
                                e.setContent(title);
                    if (breakdownParentId > 0) {
                        e.setParentId(breakdownParentId);
                        e.setProjectId(breakdownProjectId);
                        e.setProjectTag(breakdownProjectTag);
                        e.setDeadline(breakdownDeadline);
                    } else {
                        e.setDeadline(System.currentTimeMillis());
                    }
                    if (breakdownParentId == 0 && project != 0) {
                        e.setProjectId(project);
                        e.setProjectTag(names.get(project));
                    }
                    selected.add(e);
                }
            }
            if (selected.isEmpty()) {
                Toast.makeText(requireContext(), duplicatesSkipped > 0
                        ? (breakdownParentId > 0 ? "Those subtasks already exist." : "Those suggested tasks are already in your task list.")
                        : "Select at least one task.", Toast.LENGTH_SHORT).show();
                return;
            }
            db.insertEntriesAtomically(selected);
            count = selected.size();
        } catch (Exception error) {
            Toast.makeText(requireContext(), "Could not add the selected tasks. Nothing was changed.", Toast.LENGTH_LONG).show();
            return;
        } finally {
            db.close();
        }
        final int addedCount = count;
        final android.content.Context appContext = requireContext().getApplicationContext();
        final CoachStore.Record record = latestRecord;
        final String plan = latestPlan;
        AppExecutors.getInstance().diskIO().execute(() -> {
            if (breakdownParentId == 0 && record != null && !plan.isEmpty()) {
                try (CoachStore store = new CoachStore(appContext)) {
                    store.accept(record.id, CoachEngine.today(), plan);
                } catch (RuntimeException ignored) { }
            }
            AppExecutors.getInstance().mainThread().execute(() -> {
                if (!isAdded()) return;
                if (getActivity() instanceof com.ismailmushraf.bujo.MainActivity) {
                    ((com.ismailmushraf.bujo.MainActivity) getActivity()).refreshTaskSurfaces();
                }
                Toast.makeText(requireContext(), breakdownParentId > 0
                        ? addedCount + " subtask(s) added."
                        : getString(R.string.coach_toast_added, addedCount), Toast.LENGTH_SHORT).show();
                dismiss();
            });
        });
    }

    private void render() {
        TextView titleView = root.findViewById(R.id.coach_wizard_title);
        TextView statusView = root.findViewById(R.id.nova_status);
        titleView.setText("NOVA");
        if (!breakdownTask.isEmpty()) {
            statusView.setText(step == 2 ? "Turning one task into clear next steps" : "Your AI Coach");
        } else if (step == 0) {
            statusView.setText(refiningPlan ? "Let’s tune your plan" : "Let’s check in before we plan");
        } else if (step == 1) {
            statusView.setText("Let’s make room for your real day");
        } else {
            statusView.setText("Your focused plan is ready");
        }

        int activeColor = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.bb10_blue);
        int inactiveColor = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.bujo_divider);
        int[] progressIds = {R.id.coach_progress_1, R.id.coach_progress_2, R.id.coach_progress_3};
        for (int i = 0; i < progressIds.length; i++)
            root.findViewById(progressIds[i]).setBackgroundColor(i <= step ? activeColor : inactiveColor);

        int[] ids = {R.id.coach_step_mood, R.id.coach_step_time, R.id.coach_step_results};
        for (int i = 0; i < ids.length; i++) {
            root.findViewById(ids[i]).setVisibility(i == step ? View.VISIBLE : View.GONE);
        }

        root.findViewById(R.id.coach_footer_step1).setVisibility(step == 0 ? View.VISIBLE : View.GONE);
        root.findViewById(R.id.coach_footer_step2).setVisibility(step == 1 ? View.VISIBLE : View.GONE);
        root.findViewById(R.id.coach_footer_step3).setVisibility(step == 2 ? View.VISIBLE : View.GONE);
    }

    @Override
    public void onDestroyView() {
        if (typewriterRunnable != null) {
            handler.removeCallbacks(typewriterRunnable);
        }
        super.onDestroyView();
    }

    @Override
    public void onStart() {
        super.onStart();
        if (getDialog() != null && getDialog().getWindow() != null) {
            android.util.DisplayMetrics m = getResources().getDisplayMetrics();
            getDialog().getWindow().setLayout((int) (m.widthPixels * .82), (int) (m.heightPixels * .76));
            getDialog().getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);
        }
    }
}
