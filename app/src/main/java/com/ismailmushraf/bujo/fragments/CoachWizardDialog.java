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
import org.json.*;
import java.util.*;

/** The coach is a conversational Plan Day flow. */
public final class CoachWizardDialog extends DialogFragment {
    private View root;
    private int step = 0;
    private final ArrayList<JSONObject> suggestions = new ArrayList<>();
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Runnable typewriterRunnable;

    public static void show(androidx.fragment.app.FragmentManager manager) {
        if (manager.findFragmentByTag("coach_wizard") == null)
            new CoachWizardDialog().show(manager, "coach_wizard");
    }

    @NonNull
    @Override
    public Dialog onCreateDialog(Bundle state) {
        root = LayoutInflater.from(requireContext()).inflate(R.layout.dialog_coach_wizard, null);
        Dialog d = new android.app.AlertDialog.Builder(requireContext(), R.style.BujoDialog).setView(root).create();

        root.findViewById(R.id.coach_wizard_cancel).setOnClickListener(v -> dismiss());
        root.findViewById(R.id.coach_wizard_next).setOnClickListener(v -> next());
        root.findViewById(R.id.coach_wizard_back).setOnClickListener(v -> {
            step--;
            render();
        });
        root.findViewById(R.id.coach_wizard_add).setOnClickListener(v -> addSelected());

        root.findViewById(R.id.coach_refine_simpler).setOnClickListener(v -> refine("Focus on simpler, shorter tasks under 10 minutes."));
        root.findViewById(R.id.coach_refine_work).setOnClickListener(v -> refine("Prioritize work project tasks."));
        root.findViewById(R.id.coach_refine_low_effort).setOnClickListener(v -> refine("Focus on low effort, low energy tasks."));

        bindChipGroupTextColors(root.findViewById(R.id.coach_wizard_mood));
        bindChipGroupTextColors(root.findViewById(R.id.coach_wizard_energy));

        render();
        return d;
    }

    private void bindChipGroupTextColors(RadioGroup group) {
        if (group == null) return;
        int textColor = androidx.core.content.ContextCompat.getColor(requireContext(), R.color.bujo_text);
        group.setOnCheckedChangeListener((g, checkedId) -> {
            for (int i = 0; i < g.getChildCount(); i++) {
                View child = g.getChildAt(i);
                if (child instanceof RadioButton) {
                    RadioButton rb = (RadioButton) child;
                    rb.setTextColor(rb.getId() == checkedId ? android.graphics.Color.WHITE : textColor);
                }
            }
        });
        int initialChecked = group.getCheckedRadioButtonId();
        for (int i = 0; i < group.getChildCount(); i++) {
            View child = group.getChildAt(i);
            if (child instanceof RadioButton) {
                RadioButton rb = (RadioButton) child;
                rb.setTextColor(rb.getId() == initialChecked ? android.graphics.Color.WHITE : textColor);
            }
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
        final int targetMinutes = minutes;
        try {
            JSONObject input = new JSONObject()
                    .put("kind", "day planning")
                    .put("mood", choice(R.id.coach_wizard_mood))
                    .put("energy", choice(R.id.coach_wizard_energy))
                    .put("available_minutes", targetMinutes)
                    .put("note", ((EditText) root.findViewById(R.id.coach_wizard_note)).getText().toString());

            if (refinementNote != null && !refinementNote.trim().isEmpty()) {
                input.put("refinement_note", refinementNote);
            }

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
                    JSONArray list = answer.getJSONArray("suggestions");
                    suggestions.clear();
                    for (int i = 0; i < list.length(); i++)
                        suggestions.add(list.getJSONObject(i));
                    String message = answer.getString("message");
                    if (!"ai".equals(record.status))
                        message += "\n\nOffline fallback: " + engine.notice();
                    showResults(message, targetMinutes);
                } catch (Exception e) {
                    error();
                }
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
            root.findViewById(R.id.coach_wizard_next).setEnabled(false);
            setRefineButtonsEnabled(false);
        } catch (Exception e) {
            error();
        }
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
        RadioGroup group = root.findViewById(id);
        int checkedId = group.getCheckedRadioButtonId();
        RadioButton b = root.findViewById(checkedId);
        return b != null ? b.getText().toString() : "";
    }

    private void error() {
        if (!isAdded()) return;
        TextView status = root.findViewById(R.id.coach_wizard_status);
        status.setText(CoachEngine.get(requireContext()).notice());
        status.setVisibility(View.VISIBLE);
        root.findViewById(R.id.coach_wizard_next).setEnabled(true);
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
            ((TextView) row.findViewById(R.id.coach_suggestion_project)).setText(id == 0 ? "GENERAL" : "PROJECT · " + names.get(id));
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
        try {
            Map<Integer, String> names = projects();
            ArrayList<Entry> selected = new ArrayList<>();
            LinearLayout list = root.findViewById(R.id.coach_wizard_suggestions);
            for (int i = 0; i < list.getChildCount(); i++) {
                View child = list.getChildAt(i);
                if (Boolean.TRUE.equals(child.getTag(R.id.coach_suggestion_tick))) {
                    JSONObject item = (JSONObject) child.getTag();
                    Entry e = new Entry();
                    e.setSignifier("*");
                    e.setContent(item.getString("title"));
                    e.setDeadline(System.currentTimeMillis());
                    int project = item.optInt("project_id");
                    if (project != 0) {
                        e.setProjectId(project);
                        e.setProjectTag(names.get(project));
                    }
                    selected.add(e);
                }
            }
            if (selected.isEmpty()) {
                Toast.makeText(requireContext(), "Select at least one task.", Toast.LENGTH_SHORT).show();
                return;
            }
            db.open();
            db.insertEntriesAtomically(selected);
            count = selected.size();
        } catch (Exception error) {
            Toast.makeText(requireContext(), "Could not add the selected tasks. Nothing was changed.", Toast.LENGTH_LONG).show();
            return;
        } finally {
            db.close();
        }
        androidx.fragment.app.Fragment active = getParentFragmentManager().findFragmentById(R.id.fragment_container);
        if (active instanceof DailyLogFragment)
            ((DailyLogFragment) active).refreshFromCoach();
        Toast.makeText(requireContext(), getString(R.string.coach_toast_added, count), Toast.LENGTH_SHORT).show();
        dismiss();
    }

    private void render() {
        TextView titleView = root.findViewById(R.id.coach_wizard_title);
        if (step == 0) {
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

        boolean showBack = step == 1;
        boolean showNext = step < 2;
        boolean showAdd = step == 2;

        root.findViewById(R.id.coach_wizard_back).setVisibility(showBack ? View.VISIBLE : View.GONE);
        root.findViewById(R.id.coach_wizard_next).setVisibility(showNext ? View.VISIBLE : View.GONE);
        root.findViewById(R.id.coach_wizard_add).setVisibility(showAdd ? View.VISIBLE : View.GONE);
        root.findViewById(R.id.coach_wizard_divider_after_cancel).setVisibility((showBack || showNext || showAdd) ? View.VISIBLE : View.GONE);
        root.findViewById(R.id.coach_wizard_divider_after_back).setVisibility(showBack && showNext ? View.VISIBLE : View.GONE);
        root.findViewById(R.id.coach_wizard_divider_after_next).setVisibility(View.GONE);
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
