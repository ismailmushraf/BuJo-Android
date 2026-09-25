package com.ismailmushraf.bujo.fragments;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import com.ismailmushraf.bujo.utils.BB10ToggleSwitch;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;

import com.ismailmushraf.bujo.MainActivity;
import com.ismailmushraf.bujo.R;
import com.ismailmushraf.bujo.db.DatabaseManager;
import com.ismailmushraf.bujo.models.Entry;
import com.ismailmushraf.bujo.models.Project;
import com.ismailmushraf.bujo.utils.BB10DialogHelper;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class EditTaskFragment extends Fragment {

    private static final String ARG_ENTRY_ID = "entry_id";

    private int entryId = -1;
    private DatabaseManager dbManager;

    private String currentContent = "";
    private boolean isCompleted = false;
    private long dueDate = 0;
    private boolean hasDueDate = false;
    private long reminderTime = 0;
    private boolean hasReminder = false;
    private int selectedProjectId = 0;
    private String selectedProjectTag = "";

    // Initial state tracking for unsaved changes comparison
    private String initialContent = "";
    private boolean initialCompleted = false;
    private long initialDueDate = 0;
    private boolean initialHasDueDate = false;
    private long initialReminder = 0;
    private boolean initialHasReminder = false;
    private int initialProjectId = 0;

    public static class SubtaskItem {
        public int id;
        public String content;
        public boolean isCompleted;
        public SubtaskItem(int id, String content, boolean isCompleted) {
            this.id = id;
            this.content = content;
            this.isCompleted = isCompleted;
        }
    }

    private final List<SubtaskItem> subtasksList = new java.util.ArrayList<>();
    private android.widget.LinearLayout containerSubtasks;

    private TextView btnSave;
    private EditText etTitle;
    private TextView tvStatusTick;
    private BB10ToggleSwitch cbDueDateToggle;
    private View layoutDueDatePicker;
    private TextView tvDueDateValue;
    private BB10ToggleSwitch cbReminderToggle;
    private View layoutReminderPicker;
    private TextView tvReminderValue;
    private TextView tvReminderWarning;
    private View btnSelectProject;
    private View viewProjectColorBlock;
    private TextView tvSelectedProjectName;

    private final SimpleDateFormat dateDisplayFormat = new SimpleDateFormat("M/d/yy", Locale.US);
    private final SimpleDateFormat dateTimeDisplayFormat = new SimpleDateFormat("M/d/yy hh:mm a", Locale.US);

    public static EditTaskFragment newInstance(int entryId) {
        EditTaskFragment fragment = new EditTaskFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_ENTRY_ID, entryId);
        fragment.setArguments(args);
        return fragment;
    }

    public static EditTaskFragment newInstanceForCreate() {
        return newInstanceForCreate(0, "");
    }

    public static EditTaskFragment newInstanceForCreate(int defaultProjectId, String defaultProjectTag) {
        EditTaskFragment fragment = new EditTaskFragment();
        Bundle args = new Bundle();
        args.putInt(ARG_ENTRY_ID, -1);
        args.putInt("default_project_id", defaultProjectId);
        args.putString("default_project_tag", defaultProjectTag);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_edit_task, container, false);

        if (getArguments() != null) {
            entryId = getArguments().getInt(ARG_ENTRY_ID, -1);
        }

        dbManager = new DatabaseManager(getActivity());
        dbManager.open();

        boolean isCreateMode = (entryId <= 0);

        if (isCreateMode && getArguments() != null) {
            selectedProjectId = getArguments().getInt("default_project_id", 0);
            selectedProjectTag = getArguments().getString("default_project_tag", "");
            if (selectedProjectTag == null) selectedProjectTag = "";
        }

        TextView tvHeaderTitle = (TextView) root.findViewById(R.id.tv_task_header_title);
        if (tvHeaderTitle != null) {
            tvHeaderTitle.setText(isCreateMode ? "New Task" : "Edit Task");
        }

        btnSave = (TextView) root.findViewById(R.id.btn_save_task);
        etTitle = (EditText) root.findViewById(R.id.et_edit_task_title);
        tvStatusTick = (TextView) root.findViewById(R.id.tv_task_status_tick);
        containerSubtasks = (android.widget.LinearLayout) root.findViewById(R.id.container_subtasks);
        cbDueDateToggle = (BB10ToggleSwitch) root.findViewById(R.id.cb_due_date_toggle);
        layoutDueDatePicker = root.findViewById(R.id.layout_due_date_picker);
        tvDueDateValue = (TextView) root.findViewById(R.id.tv_due_date_value);
        cbReminderToggle = (BB10ToggleSwitch) root.findViewById(R.id.cb_reminder_toggle);
        layoutReminderPicker = root.findViewById(R.id.layout_reminder_picker);
        tvReminderValue = (TextView) root.findViewById(R.id.tv_reminder_value);
        tvReminderWarning = (TextView) root.findViewById(R.id.tv_reminder_past_warning);
        btnSelectProject = root.findViewById(R.id.btn_select_project);
        viewProjectColorBlock = root.findViewById(R.id.view_project_color_block);
        tvSelectedProjectName = (TextView) root.findViewById(R.id.tv_selected_project_name);

        if (!isCreateMode) {
            Entry entry = loadEntryById(entryId);
            if (entry != null) {
                currentContent = entry.getContent() != null ? entry.getContent() : "";
                isCompleted = entry.isCompleted();
                selectedProjectId = entry.getProjectId();
                selectedProjectTag = entry.getProjectTag() != null ? entry.getProjectTag() : "";

                if (entry.getDeadline() > 0) {
                    if (entry.hasTime()) {
                        reminderTime = entry.getDeadline();
                        hasReminder = true;
                    } else {
                        dueDate = entry.getDeadline();
                        hasDueDate = true;
                    }
                }
            }
        }

        // Store initial state
        initialContent = currentContent;
        initialCompleted = isCompleted;
        initialDueDate = dueDate;
        initialHasDueDate = hasDueDate;
        initialReminder = reminderTime;
        initialHasReminder = hasReminder;
        initialProjectId = selectedProjectId;

        // Populate initial UI
        etTitle.setText(currentContent);
        updateStatusTickUI();
        updateDueDateUI(false);
        updateReminderUI(false);
        updateProjectUI();

        subtasksList.clear();
        if (!isCreateMode && entryId > 0) {
            List<Entry> childEntries = dbManager.getChildEntries(entryId);
            for (Entry child : childEntries) {
                subtasksList.add(new SubtaskItem(child.getId(), child.getContent() != null ? child.getContent() : "", child.isCompleted()));
            }
        }
        if (subtasksList.isEmpty() || !subtasksList.get(subtasksList.size() - 1).content.trim().isEmpty()) {
            subtasksList.add(new SubtaskItem(0, "", false));
        }
        renderSubtasks(false);

        etTitle.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                checkSaveButtonState();
            }
            @Override public void afterTextChanged(Editable s) {}
        });

        // 1. Status Tick Click Listener
        tvStatusTick.setOnClickListener(v -> {
            isCompleted = !isCompleted;
            updateStatusTickUI();
            checkSaveButtonState();
        });

        // 2. Due Date Toggle & Picker
        cbDueDateToggle.setOnCheckedChangeListener((toggle, isChecked) -> {
            hasDueDate = isChecked;
            if (isChecked && dueDate <= 0) {
                Calendar c = Calendar.getInstance();
                c.set(Calendar.HOUR_OF_DAY, 12);
                c.set(Calendar.MINUTE, 0);
                dueDate = c.getTimeInMillis();
            }
            updateDueDateUI(true);
            checkSaveButtonState();
        });

        layoutDueDatePicker.setOnClickListener(v -> showDatePickerForDueDate());

        // 3. Reminder Toggle & Picker
        cbReminderToggle.setOnCheckedChangeListener((toggle, isChecked) -> {
            hasReminder = isChecked;
            if (isChecked && reminderTime <= 0) {
                Calendar c = Calendar.getInstance();
                c.set(Calendar.HOUR_OF_DAY, 12);
                c.set(Calendar.MINUTE, 0);
                reminderTime = c.getTimeInMillis();
            }
            updateReminderUI(true);
            checkSaveButtonState();
        });

        layoutReminderPicker.setOnClickListener(v -> showDateTimePickerForReminder());

        // 4. Project Selector Click
        btnSelectProject.setOnClickListener(v -> showProjectPickerDialog());

        // Cancel Button Action
        root.findViewById(R.id.btn_cancel_task).setOnClickListener(v -> handleCancelAction());

        // Save Button Action
        root.findViewById(R.id.btn_save_task).setOnClickListener(v -> handleSaveAction(isCreateMode));

        checkSaveButtonState();
        return root;
    }

    private Entry loadEntryById(int id) {
        return dbManager.getEntryById(id);
    }

    private void updateStatusTickUI() {
        if (isCompleted) {
            tvStatusTick.setBackgroundResource(R.drawable.bb10_checkbox_checked_bg);
            tvStatusTick.setText("✓");
        } else {
            tvStatusTick.setBackgroundResource(R.drawable.bb10_checkbox_unchecked_bg);
            tvStatusTick.setText("");
        }
    }

    private void renderSubtasks(boolean focusLast) {
        if (containerSubtasks == null || getActivity() == null) return;
        containerSubtasks.removeAllViews();

        for (int i = 0; i < subtasksList.size(); i++) {
            final int index = i;
            final SubtaskItem item = subtasksList.get(i);

            View row = LayoutInflater.from(getActivity()).inflate(R.layout.item_edit_subtask_row, containerSubtasks, false);
            final TextView tvTick = row.findViewById(R.id.subtask_tick);
            final EditText etContent = row.findViewById(R.id.et_subtask_content);
            final View btnDelete = row.findViewById(R.id.btn_delete_subtask);

            tvTick.setText(item.isCompleted ? "✓" : "");
            tvTick.setBackgroundResource(item.isCompleted ? R.drawable.bb10_checkbox_checked_bg : R.drawable.bb10_checkbox_unchecked_bg);
            etContent.setText(item.content);

            if (item.isCompleted) {
                etContent.setPaintFlags(etContent.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
            } else {
                etContent.setPaintFlags(etContent.getPaintFlags() & (~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG));
            }

            tvTick.setOnClickListener(v -> {
                item.isCompleted = !item.isCompleted;
                tvTick.setText(item.isCompleted ? "✓" : "");
                tvTick.setBackgroundResource(item.isCompleted ? R.drawable.bb10_checkbox_checked_bg : R.drawable.bb10_checkbox_unchecked_bg);
                if (item.isCompleted) {
                    etContent.setPaintFlags(etContent.getPaintFlags() | android.graphics.Paint.STRIKE_THRU_TEXT_FLAG);
                } else {
                    etContent.setPaintFlags(etContent.getPaintFlags() & (~android.graphics.Paint.STRIKE_THRU_TEXT_FLAG));
                }
                checkSaveButtonState();
            });

            btnDelete.setOnClickListener(v -> {
                if (subtasksList.size() > 1) {
                    subtasksList.remove(index);
                    renderSubtasks(false);
                    checkSaveButtonState();
                } else {
                    item.content = "";
                    item.isCompleted = false;
                    renderSubtasks(false);
                    checkSaveButtonState();
                }
            });

            etContent.addTextChangedListener(new TextWatcher() {
                @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                @Override
                public void onTextChanged(CharSequence s, int start, int before, int count) {
                    item.content = s.toString();
                    if (index == subtasksList.size() - 1 && !item.content.trim().isEmpty()) {
                        subtasksList.add(new SubtaskItem(0, "", false));
                        renderSubtasks(false);
                    }
                    checkSaveButtonState();
                }
                @Override public void afterTextChanged(Editable s) {}
            });

            etContent.setOnEditorActionListener((v, actionId, event) -> {
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_NEXT ||
                    actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE ||
                    actionId == 0) {
                    if (!item.content.trim().isEmpty()) {
                        if (index == subtasksList.size() - 1) {
                            subtasksList.add(new SubtaskItem(0, "", false));
                        }
                        renderSubtasks(true);
                        return true;
                    }
                }
                return false;
            });

            containerSubtasks.addView(row);

            if (focusLast && index == subtasksList.size() - 1) {
                etContent.requestFocus();
            }
        }
    }

    private void animateViewVisibility(final View view, final boolean show) {
        if (view == null) return;
        float shiftDistance = -15f * getResources().getDisplayMetrics().density;
        if (show) {
            if (view.getVisibility() == View.VISIBLE && view.getAlpha() == 1f) return;
            view.setVisibility(View.VISIBLE);
            view.setAlpha(0f);
            view.setTranslationY(shiftDistance);
            view.animate()
                    .alpha(1f)
                    .translationY(0f)
                    .setDuration(220)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                    .setListener(null)
                    .start();
        } else {
            if (view.getVisibility() == View.GONE) return;
            view.animate()
                    .alpha(0f)
                    .translationY(shiftDistance)
                    .setDuration(180)
                    .setInterpolator(new android.view.animation.AccelerateInterpolator())
                    .setListener(new android.animation.AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(android.animation.Animator animation) {
                            view.setVisibility(View.GONE);
                            view.setTranslationY(0f);
                            view.setAlpha(1f);
                        }
                    })
                    .start();
        }
    }

    private void updateDueDateUI() {
        updateDueDateUI(false);
    }

    private void updateDueDateUI(boolean animate) {
        cbDueDateToggle.setChecked(hasDueDate, animate);
        if (animate) {
            animateViewVisibility(layoutDueDatePicker, hasDueDate);
        } else {
            layoutDueDatePicker.setVisibility(hasDueDate ? View.VISIBLE : View.GONE);
            layoutDueDatePicker.setAlpha(1f);
            layoutDueDatePicker.setTranslationY(0f);
        }
        if (hasDueDate && dueDate > 0) {
            tvDueDateValue.setText(dateDisplayFormat.format(new Date(dueDate)));
        } else {
            tvDueDateValue.setText("");
        }
    }

    private void updateReminderUI() {
        updateReminderUI(false);
    }

    private void updateReminderUI(boolean animate) {
        cbReminderToggle.setChecked(hasReminder, animate);
        boolean isPast = hasReminder && reminderTime > 0 && reminderTime < System.currentTimeMillis();
        if (animate) {
            animateViewVisibility(layoutReminderPicker, hasReminder);
            animateViewVisibility(tvReminderWarning, isPast);
        } else {
            layoutReminderPicker.setVisibility(hasReminder ? View.VISIBLE : View.GONE);
            layoutReminderPicker.setAlpha(1f);
            layoutReminderPicker.setTranslationY(0f);
            tvReminderWarning.setVisibility(isPast ? View.VISIBLE : View.GONE);
        }
        if (hasReminder && reminderTime > 0) {
            tvReminderValue.setText(dateTimeDisplayFormat.format(new Date(reminderTime)));
        } else {
            tvReminderValue.setText("");
        }
    }

    private void updateProjectUI() {
        if (selectedProjectId > 0) {
            Project project = null;
            List<Project> allProjects = dbManager.getAllProjects();
            for (Project p : allProjects) {
                if (p.getId() == selectedProjectId) {
                    project = p;
                    break;
                }
            }
            if (project != null) {
                tvSelectedProjectName.setText(project.getName());
                selectedProjectTag = project.getName();
                int color = project.getColor() != 0 ? project.getColor() : getResources().getColor(R.color.bb10_folder_blue);
                setProjectColorBlock(color);
                return;
            }
        }
        tvSelectedProjectName.setText("Unfiled");
        selectedProjectTag = "";
        setProjectColorBlock(getResources().getColor(R.color.bujo_divider));
    }

    private void setProjectColorBlock(int color) {
        if (viewProjectColorBlock == null) return;
        viewProjectColorBlock.setBackgroundColor(color);
    }

    private void checkSaveButtonState() {
        if (btnSave == null || etTitle == null) return;

        String newTitle = etTitle.getText().toString().trim();
        boolean isValid = !newTitle.isEmpty();
        boolean isCreateMode = (entryId <= 0);
        boolean hasChanged = isCreateMode ? !newTitle.isEmpty() : hasUnsavedChanges();
        boolean enable = isValid && hasChanged;

        btnSave.setEnabled(enable);
        btnSave.setTextColor(enable ? android.graphics.Color.parseColor("#00a8df") : android.graphics.Color.parseColor("#A0C8E6"));
    }

    private void showDatePickerForDueDate() {
        final Calendar c = Calendar.getInstance();
        if (dueDate > 0) c.setTimeInMillis(dueDate);

        DatePickerDialog dpd = new DatePickerDialog(getActivity(), (view, year, month, dayOfMonth) -> {
            Calendar selected = Calendar.getInstance();
            selected.set(year, month, dayOfMonth, 12, 0, 0);
            dueDate = selected.getTimeInMillis();
            hasDueDate = true;
            updateDueDateUI();
            checkSaveButtonState();
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        dpd.show();
    }

    private void showDateTimePickerForReminder() {
        final Calendar c = Calendar.getInstance();
        if (reminderTime > 0) c.setTimeInMillis(reminderTime);

        DatePickerDialog dpd = new DatePickerDialog(getActivity(), (view, year, month, dayOfMonth) -> {
            final Calendar selected = Calendar.getInstance();
            selected.set(Calendar.YEAR, year);
            selected.set(Calendar.MONTH, month);
            selected.set(Calendar.DAY_OF_MONTH, dayOfMonth);

            TimePickerDialog tpd = new TimePickerDialog(getActivity(), (timeView, hourOfDay, minute) -> {
                selected.set(Calendar.HOUR_OF_DAY, hourOfDay);
                selected.set(Calendar.MINUTE, minute);
                selected.set(Calendar.SECOND, 0);
                reminderTime = selected.getTimeInMillis();
                hasReminder = true;
                updateReminderUI();
                checkSaveButtonState();
            }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false);
            tpd.show();
        }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        dpd.show();
    }

    private void showProjectPickerDialog() {
        List<Project> projects = dbManager.getAllProjects();
        List<Project> pickerList = new java.util.ArrayList<>();

        Project unfiled = new Project();
        unfiled.setId(0);
        unfiled.setName("Unfiled (None)");
        unfiled.setColor(getResources().getColor(R.color.bujo_divider));
        pickerList.add(unfiled);
        pickerList.addAll(projects);

        View view = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_bb10_project_picker, null);
        android.widget.ListView listView = (android.widget.ListView) view.findViewById(R.id.lv_project_picker);
        View btnCancel = view.findViewById(R.id.btn_project_picker_cancel);

        ArrayAdapter<Project> pickerAdapter = new ArrayAdapter<Project>(getActivity(), 0, pickerList) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                if (convertView == null) {
                    convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_bb10_project_picker, parent, false);
                }
                Project item = getItem(position);
                View colorBar = convertView.findViewById(R.id.picker_project_color_bar);
                TextView tvName = (TextView) convertView.findViewById(R.id.picker_project_name);

                if (item != null) {
                    tvName.setText(item.getName());
                    int color = (item.getColor() != 0) ? item.getColor() : getResources().getColor(R.color.bb10_folder_blue);
                    if (item.getId() == 0) color = getResources().getColor(R.color.bujo_divider);
                    if (colorBar != null) colorBar.setBackgroundColor(color);
                }
                return convertView;
            }
        };

        listView.setAdapter(pickerAdapter);

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.BujoDialog);
        builder.setView(view);
        final AlertDialog dialog = builder.create();

        btnCancel.setOnClickListener(v -> dialog.dismiss());

        listView.setOnItemClickListener((parent, v, position, id) -> {
            dialog.dismiss();
            Project selected = pickerList.get(position);
            if (selected.getId() == 0) {
                selectedProjectId = 0;
                selectedProjectTag = "";
            } else {
                selectedProjectId = selected.getId();
                selectedProjectTag = selected.getName();
            }
            updateProjectUI();
            checkSaveButtonState();
        });

        dialog.setOnShowListener(d -> {
            if (dialog.getWindow() != null) {
                android.util.DisplayMetrics metrics = getResources().getDisplayMetrics();
                int width = (int) (metrics.widthPixels * 0.88);
                dialog.getWindow().setLayout(width, android.view.ViewGroup.LayoutParams.WRAP_CONTENT);
            }
        });

        dialog.show();
    }

    private boolean hasUnsavedChanges() {
        String newTitle = etTitle.getText().toString().trim();
        return !newTitle.equals(initialContent)
                || isCompleted != initialCompleted
                || hasDueDate != initialHasDueDate
                || (hasDueDate && dueDate != initialDueDate)
                || hasReminder != initialHasReminder
                || (hasReminder && reminderTime != initialReminder)
                || selectedProjectId != initialProjectId;
    }

    private void handleCancelAction() {
        if (hasUnsavedChanges()) {
            BB10DialogHelper.showConfirmDialog(getActivity(), "Discard Changes", "Are you sure you want to discard your changes?", "Discard", () -> {
                if (getFragmentManager() != null) {
                    getFragmentManager().popBackStack();
                }
            });
        } else {
            if (getFragmentManager() != null) {
                getFragmentManager().popBackStack();
            }
        }
    }

    private void handleSaveAction(boolean isCreateMode) {
        String title = etTitle.getText().toString().trim();
        if (title.isEmpty()) return;

        Entry entry = new Entry();
        if (!isCreateMode && entryId > 0) {
            entry.setId(entryId);
        }
        entry.setSignifier("*");
        entry.setContent(title);
        entry.setCompleted(isCompleted);
        entry.setProjectId(selectedProjectId);
        entry.setProjectTag(selectedProjectTag);

        if (hasReminder && reminderTime > 0) {
            entry.setDeadline(reminderTime);
            entry.setHasTime(true);
        } else if (hasDueDate && dueDate > 0) {
            entry.setDeadline(dueDate);
            entry.setHasTime(false);
        } else {
            entry.setDeadline(0);
            entry.setHasTime(false);
        }

        long parentEntryId = entryId;
        if (isCreateMode) {
            entry.setCreatedAt(System.currentTimeMillis());
            parentEntryId = dbManager.insertEntry(entry);
        } else {
            dbManager.updateEntry(entry);
        }

        if (parentEntryId > 0) {
            List<Entry> existingChildren = dbManager.getChildEntries((int) parentEntryId);
            java.util.Set<Integer> keptChildIds = new java.util.HashSet<>();

            for (SubtaskItem subItem : subtasksList) {
                String subContent = subItem.content != null ? subItem.content.trim() : "";
                if (subContent.isEmpty()) continue;

                if (subItem.id > 0) {
                    keptChildIds.add(subItem.id);
                    Entry child = new Entry();
                    child.setId(subItem.id);
                    child.setParentId((int) parentEntryId);
                    child.setSignifier("*");
                    child.setContent(subContent);
                    child.setCompleted(subItem.isCompleted);
                    child.setProjectId(selectedProjectId);
                    child.setProjectTag(selectedProjectTag);
                    dbManager.updateEntry(child);
                } else {
                    Entry child = new Entry();
                    child.setParentId((int) parentEntryId);
                    child.setSignifier("*");
                    child.setContent(subContent);
                    child.setCompleted(subItem.isCompleted);
                    child.setProjectId(selectedProjectId);
                    child.setProjectTag(selectedProjectTag);
                    child.setCreatedAt(System.currentTimeMillis());
                    long insertedChildId = dbManager.insertEntry(child);
                    if (insertedChildId > 0) {
                        keptChildIds.add((int) insertedChildId);
                    }
                }
            }

            for (Entry oldChild : existingChildren) {
                if (!keptChildIds.contains(oldChild.getId())) {
                    dbManager.deleteEntry(oldChild.getId());
                }
            }
        }

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).refreshDrawer();
        }

        if (getFragmentManager() != null) {
            getFragmentManager().popBackStack();
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (dbManager != null) dbManager.close();
    }
}
