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

        root.findViewById(R.id.coach_refine_simpler).setOnClickListener(v -> refine("Focus on simpler, shorter tasks under 10 minutes."));
        root.findViewById(R.id.coach_refine_work).setOnClickListener(v -> refine("Prioritize work project tasks."));
        root.findViewById(R.id.coach_refine_low_effort).setOnClickListener(v -> refine("Focus on low effort, low energy tasks."));

        bindChipGroup(root.findViewById(R.id.coach_wizard_mood), R.id.mood_ok);
        bindChipGroup(root.findViewById(R.id.coach_wizard_energy), R.id.energy_medium);

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
        if (!breakdownTask.isEmpty()) root.post(this::submitBreakdown);
        return d;
    }

    private void bindChipGroup(LinearLayout group, int defaultSelectedId) {
        if (group == null) return;
        View initialSelected = group.findViewById(defaultSelectedId);
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            child.setOnClickListener(v -> selectChip(group, v));
        }
        selectChip(group, initialSelected != null ? initialSelected : group.getChildAt(0));
    }

    private void selectChip(LinearLayout group, View selected) {
        int textColor = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.bujo_text);
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            boolean isSelected = child == selected;
            child.setSelected(isSelected);
            if (child instanceof TextView)
                ((TextView) child).setTextColor(isSelected ? android.graphics.Color.WHITE : textColor);
        }
    }

    private void next() {
        if (step < 1) {
            step++;
            render();
            return;
        }
        submitCheckin(null);
    }

    private void submitCheckin(String refinementNote) {
        int minutes;
        try {
            minutes = Integer.parseInt(((EditText) root.findViewById(R.id.coach_wizard_minutes)).getText().toString());
        } catch (Exception e) {
            minutes = -1;
        }
        if (minutes < 0 || minutes > 960) {
            ((EditText) root.findViewById(R.id.coach_wizard_minutes)).setError(getString(R.string.coach_invalid_minutes));
            return;
        }
        try {
            JSONObject input = new JSONObject()
                    .put("kind", "day planning")
                    .put("mood", choice(R.id.coach_wizard_mood))
                    .put("energy", choice(R.id.coach_wizard_energy))
                    .put("available_minutes", minutes)
                    .put("note", ((EditText) root.findViewById(R.id.coach_wizard_note)).getText().toString());
            if (refinementNote != null && !refinementNote.trim().isEmpty()) {
                input.put("refinement_note", refinementNote);
            }
            submit(input, minutes, refinementNote);
        } catch (Exception e) {
            error();
        }
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
                TextView status = root.findViewById(R.id.coach_wizard_status);
                status.setText(R.string.coach_busy);
                status.setVisibility(View.VISIBLE);
                setRefineButtonsEnabled(true);
                return;
            }
            TextView status = root.findViewById(R.id.coach_wizard_status);
            status.setText(refinementNote != null ? R.string.coach_refining : R.string.coach_working);
            status.setVisibility(View.VISIBLE);
            root.findViewById(R.id.coach_wizard_next_step2).setEnabled(false);
            setRefineButtonsEnabled(false);
        } catch (Exception e) { error(); }
    }

    private void refine(String note) {
        submitCheckin(note);
    }

    private void setRefineButtonsEnabled(boolean enabled) {
        root.findViewById(R.id.coach_refine_simpler).setEnabled(enabled);
        root.findViewById(R.id.coach_refine_work).setEnabled(enabled);
        root.findViewById(R.id.coach_refine_low_effort).setEnabled(enabled);
    }

    private String choice(int id) {
        LinearLayout group = root.findViewById(id);
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child.isSelected() && child instanceof TextView)
                return ((TextView) child).getText().toString();
        }
        return "";
    }

    private void error() {
        if (!isAdded()) return;
        TextView status = root.findViewById(R.id.coach_wizard_status);
        status.setText(CoachEngine.get(requireContext()).notice());
        status.setVisibility(View.VISIBLE);
        root.findViewById(R.id.coach_wizard_next_step2).setEnabled(true);
        setRefineButtonsEnabled(true);
    }

    private void showResults(String message, int minutes) {
        step = 2;
        root.findViewById(R.id.coach_wizard_status).setVisibility(View.GONE);
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
                    e.setSignifier("*");
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
        if (!breakdownTask.isEmpty()) {
            titleView.setText("BREAK DOWN TASK");
        } else if (step == 0) {
            titleView.setText(R.string.coach_header_step1);
        } else if (step == 1) {
            titleView.setText(R.string.coach_header_step2);
        } else {
            titleView.setText(R.string.coach_header_step3);
        }

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
