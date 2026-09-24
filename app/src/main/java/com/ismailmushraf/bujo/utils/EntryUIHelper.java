package com.ismailmushraf.bujo.utils;

import android.app.AlarmManager;
import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.app.PendingIntent;
import android.app.TimePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Paint;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.DisplayMetrics;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import com.ismailmushraf.bujo.R;
import com.ismailmushraf.bujo.db.DatabaseManager;
import com.ismailmushraf.bujo.models.Entry;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class EntryUIHelper {

    public interface OnEntryUpdatedListener {
        void onEntryUpdated();
    }

    private final Context context;
    private final DatabaseManager dbManager;
    private final OnEntryUpdatedListener listener;

    public EntryUIHelper(Context context, DatabaseManager dbManager, OnEntryUpdatedListener listener) {
        this.context = context;
        this.dbManager = dbManager;
        this.listener = listener;
    }

    public void showContextDialog(final Entry entry, final View sourceView) {
        if (entry.isLocked()) {
            new AlertDialog.Builder(context, R.style.BujoDialog)
                    .setTitle("Task Locked")
                    .setMessage("Modifications are disabled for this task. It remains set in stone for the day to encourage commitment.")
                    .setPositiveButton(android.R.string.ok, null)
                    .show();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.BujoDialog);
        builder.setTitle("Options");

        List<String> optionsList = new ArrayList<>();
        optionsList.add("Edit Item");
        optionsList.add("Set Date");
        optionsList.add("Set Reminder Time");
        optionsList.add(entry.isMigrated() ? "Mark as Not Migrated" : "Migrate to Future List");
        if ("*".equals(entry.getSignifier()) && !entry.isMigrated()) {
            optionsList.add("Lock Task");
        }
        optionsList.add("Delete Item");

        String[] options = optionsList.toArray(new String[0]);

        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                String selected = options[which];
                if (selected.equals("Edit Item")) {
                    showEditDialog(entry);
                } else if (selected.equals("Set Date")) {
                    showDatePicker(entry, sourceView);
                } else if (selected.equals("Set Reminder Time")) {
                    showTimePicker(entry);
                } else if (selected.equals("Mark as Not Migrated") || selected.equals("Migrate to Future List")) {
                    boolean migrating = !entry.isMigrated();
                    long prevDeadline = entry.getDeadline();
                    boolean wasToday = DatabaseManager.isToday(prevDeadline) && "*".equals(entry.getSignifier()) && entry.getParentId() == 0;

                    entry.setMigrated(migrating);
                    if (migrating) {
                        entry.setDeadline(0);
                        entry.setLockedManually(false); // Unlock when moving to logbook
                        scheduleNotification(entry);
                    } else {
                        // Reset timestamp and set to today when un-migrating
                        entry.setCreatedAt(System.currentTimeMillis());
                        entry.setLockedManually(false); // Ensure lock is lifted
                        entry.setCompleted(false); // Mark as uncompleted when bringing back to Today
                        Calendar today = Calendar.getInstance();
                        today.set(Calendar.HOUR_OF_DAY, 12);
                        today.set(Calendar.MINUTE, 0);
                        entry.setDeadline(today.getTimeInMillis());
                    }
                    dbManager.updateEntry(entry);

                    boolean isToday = DatabaseManager.isToday(entry.getDeadline()) && "*".equals(entry.getSignifier()) && entry.getParentId() == 0;
                    if (context instanceof com.ismailmushraf.bujo.MainActivity) {
                        if (wasToday && !isToday) {
                            ((com.ismailmushraf.bujo.MainActivity) context).animatePointsChange(-5, sourceView);
                        } else if (!wasToday && isToday) {
                            ((com.ismailmushraf.bujo.MainActivity) context).animatePointsChange(5, sourceView);
                        }
                    }

                    listener.onEntryUpdated();
                } else if (selected.equals("Lock Task")) {
                    showLockConfirmation(entry);
                } else if (selected.equals("Delete Item")) {
                    int pointsDeducted = dbManager.deleteEntry(entry.getId());
                    if (pointsDeducted > 0 && context instanceof com.ismailmushraf.bujo.MainActivity) {
                        ((com.ismailmushraf.bujo.MainActivity) context).animatePointsChange(-pointsDeducted, sourceView);
                    }
                    listener.onEntryUpdated();
                }
            }
        });
        builder.show();
    }

    private void showLockConfirmation(final Entry entry) {
        new AlertDialog.Builder(context, R.style.BujoDialog)
                .setTitle("Confirm Lock")
                .setMessage("Locking this task will make it unchangeable and non-deletable for the rest of the day. Are you sure?")
                .setPositiveButton("Lock Forever", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        entry.setLockedManually(true);
                        dbManager.updateEntry(entry);
                        listener.onEntryUpdated();
                        Toast.makeText(context, "Task locked.", Toast.LENGTH_SHORT).show();
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void showEditDialog(final Entry entry) {
        if (entry.isLocked()) {
            Toast.makeText(context, "Item is locked.", Toast.LENGTH_SHORT).show();
            return;
        }
        final EditText input = new EditText(context);
        input.setText(entry.getContent());
        input.setSelection(input.length());
        new AlertDialog.Builder(context, R.style.BujoDialog)
                .setTitle("Edit item")
                .setView(input)
                .setNegativeButton(android.R.string.cancel, null)
                .setPositiveButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        String content = input.getText().toString().trim();
                        if (!content.isEmpty()) {
                            entry.setContent(content);
                            dbManager.updateEntry(entry);
                            listener.onEntryUpdated();
                        }
                    }
                })
                .show();
    }

    private void showDatePicker(final Entry entry, final View sourceView) {
        final Calendar c = Calendar.getInstance();
        if (entry.getDeadline() > 0) {
            c.setTimeInMillis(entry.getDeadline());
        }

        DatePickerDialog datePickerDialog = new DatePickerDialog(context,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        Calendar selected = Calendar.getInstance();

                        if (entry.getDeadline() > 0) {
                            selected.setTimeInMillis(entry.getDeadline());
                        } else {
                            selected.set(Calendar.HOUR_OF_DAY, 12);
                            selected.set(Calendar.MINUTE, 0);
                            selected.set(Calendar.SECOND, 0);
                        }

                        selected.set(Calendar.YEAR, year);
                        selected.set(Calendar.MONTH, monthOfYear);
                        selected.set(Calendar.DAY_OF_MONTH, dayOfMonth);

                        long prevDeadline = entry.getDeadline();
                        boolean wasToday = DatabaseManager.isToday(prevDeadline) && "*".equals(entry.getSignifier()) && entry.getParentId() == 0;

                        entry.setDeadline(selected.getTimeInMillis());
                        entry.setHasTime(false);

                        boolean isToday = DatabaseManager.isToday(entry.getDeadline()) && "*".equals(entry.getSignifier()) && entry.getParentId() == 0;

                        dbManager.updateEntry(entry);
                        scheduleNotification(entry);

                        if (context instanceof com.ismailmushraf.bujo.MainActivity) {
                            if (!wasToday && isToday) {
                                ((com.ismailmushraf.bujo.MainActivity) context).animatePointsChange(5, sourceView);
                            } else if (wasToday && !isToday) {
                                ((com.ismailmushraf.bujo.MainActivity) context).animatePointsChange(-5, sourceView);
                            }
                        }

                        listener.onEntryUpdated();
                    }
                }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));

        datePickerDialog.show();
    }

    private void showTimePicker(final Entry entry) {
        final Calendar c = Calendar.getInstance();
        if (entry.getDeadline() > 0) {
            c.setTimeInMillis(entry.getDeadline());
        }

        TimePickerDialog timePickerDialog = new TimePickerDialog(context,
                new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        Calendar selected = Calendar.getInstance();

                        if (entry.getDeadline() > 0) {
                            selected.setTimeInMillis(entry.getDeadline());
                        }

                        selected.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        selected.set(Calendar.MINUTE, minute);
                        selected.set(Calendar.SECOND, 0);

                        entry.setDeadline(selected.getTimeInMillis());
                        entry.setHasTime(true);

                        dbManager.updateEntry(entry);
                        scheduleNotification(entry);
                        listener.onEntryUpdated();
                    }
                }, c.get(Calendar.HOUR_OF_DAY), c.get(Calendar.MINUTE), false);

        timePickerDialog.show();
    }

    private void scheduleNotification(Entry entry) {
        Intent intent = new Intent(context, AlarmReceiver.class);
        intent.putExtra("TASK_CONTENT", entry.getContent());
        intent.putExtra("TASK_ID", entry.getId());

        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                entry.getId(),
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        if (alarmManager != null) {
            if (entry.getDeadline() > 0 && entry.hasTime()) {
                alarmManager.set(AlarmManager.RTC_WAKEUP, entry.getDeadline(), pendingIntent);
            } else {
                alarmManager.cancel(pendingIntent);
            }
        }
    }

    public void showEmojiPicker(final EditText targetEditText) {
        final String[] emojis = {
                "\uD83D\uDE0A", "\uD83D\uDE02", "\u263A", "\u270C", "\u2665",
                "\uD83D\uDCDD", "\uD83D\uDCCC", "\uD83D\uDCC5", "\uD83D\uDCA1", "\uD83D\uDCBB",
                "\u2714", "\u2716", "\u2757", "\u2753", "\u2B50", "\u2705", "\u274C", "\u23F0",
                "\uD83D\uDCDA", "\uD83C\uDFAF", "\u270F\uFE0F", "\u270D", "\u2709", "\u260E",
                "\uD83D\uDCAA", "\uD83C\uDFC3", "\uD83D\uDEB2", "\uD83C\uDFC6", "\uD83D\uDD25",
                "\u231A", "\u23F3", "\u2605", "\u2606", "\u26A1", "\u26BD", "\u26F3", "\u2600", "\u2601"
        };

        GridView gridView = new GridView(context);
        gridView.setNumColumns(5);
        gridView.setPadding(16, 32, 16, 32);
        gridView.setVerticalSpacing(32);

        ArrayAdapter<String> adapter = new ArrayAdapter<String>(
                context, android.R.layout.simple_list_item_1, emojis) {
            @Override
            public View getView(int position, View convertView, ViewGroup parent) {
                TextView view = (TextView) super.getView(position, convertView, parent);
                view.setTextSize(26);
                view.setGravity(Gravity.CENTER);
                return view;
            }
        };

        gridView.setAdapter(adapter);

        AlertDialog.Builder builder = new AlertDialog.Builder(context, R.style.BujoDialog);
        builder.setTitle("Select Emoji");
        builder.setView(gridView);
        final AlertDialog dialog = builder.create();

        gridView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                targetEditText.append(emojis[position]);
                dialog.dismiss();
            }
        });
        dialog.show();
    }

    public void toggleEntryCompletion(Entry entry, View sourceView) {
        if ("-".equals(entry.getSignifier())) return;

        int pointsBefore = dbManager.getUserStats()[0];
        entry.setCompleted(!entry.isCompleted());
        dbManager.updateEntry(entry);
        int appliedPoints = dbManager.getUserStats()[0] - pointsBefore;

        dbManager.evaluateDailyStreak();

        if (context instanceof com.ismailmushraf.bujo.MainActivity) {
            ((com.ismailmushraf.bujo.MainActivity) context).animatePointsChange(appliedPoints, sourceView);
        }

        if (listener != null) {
            listener.onEntryUpdated();
        }
    }

    public void showAddEventDialog(final int projectId, final String projectTag) {
        final View view = LayoutInflater.from(context).inflate(R.layout.dialog_add_event, null);
        final EditText etName = view.findViewById(R.id.et_event_name);
        final EditText etDesc = view.findViewById(R.id.et_event_desc);
        final Button btnDate = view.findViewById(R.id.btn_event_date);
        final Button btnTime = view.findViewById(R.id.btn_event_time);

        final Calendar selected = Calendar.getInstance();
        final boolean[] hasTime = {false};

        final SimpleDateFormat df = new SimpleDateFormat("MMM d, yyyy", Locale.US);
        final SimpleDateFormat tf = new SimpleDateFormat("h:mm a", Locale.US);

        btnDate.setText("Date: " + df.format(selected.getTime()));

        btnDate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new DatePickerDialog(context, new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(android.widget.DatePicker view, int year, int month, int dayOfMonth) {
                        selected.set(Calendar.YEAR, year);
                        selected.set(Calendar.MONTH, month);
                        selected.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                        btnDate.setText("Date: " + df.format(selected.getTime()));
                    }
                }, selected.get(Calendar.YEAR), selected.get(Calendar.MONTH), selected.get(Calendar.DAY_OF_MONTH)).show();
            }
        });

        btnTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new TimePickerDialog(context, new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        selected.set(Calendar.HOUR_OF_DAY, hourOfDay);
                        selected.set(Calendar.MINUTE, minute);
                        selected.set(Calendar.SECOND, 0);
                        hasTime[0] = true;
                        btnTime.setText("Time: " + tf.format(selected.getTime()));
                    }
                }, selected.get(Calendar.HOUR_OF_DAY), selected.get(Calendar.MINUTE), false).show();
            }
        });

        final AlertDialog dialog = new AlertDialog.Builder(context, R.style.BujoDialog)
                .setView(view)
                .setPositiveButton("Schedule", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface d, int which) {
                        String name = etName.getText().toString().trim();
                        String desc = etDesc.getText().toString().trim();
                        if (!name.isEmpty()) {
                            Entry event = new Entry();
                            event.setSignifier("o");
                            String content = name;
                            if (!desc.isEmpty()) content += "\n" + desc;
                            event.setContent(content);
                            event.setDeadline(selected.getTimeInMillis());
                            event.setHasTime(hasTime[0]);
                            event.setProjectId(projectId);
                            event.setProjectTag(projectTag);
                            event.setCreatedAt(System.currentTimeMillis());
                            dbManager.insertEntry(event);
                            if (listener != null) listener.onEntryUpdated();
                        }
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .create();

        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface d) {
                if (dialog.getWindow() != null) {
                    DisplayMetrics metrics = context.getResources().getDisplayMetrics();
                    int width = (int) (metrics.widthPixels * 0.94);
                    dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
                }

                int color = context.getResources().getColor(R.color.bujo_text);
                dialog.getButton(AlertDialog.BUTTON_POSITIVE).setTextColor(color);
                dialog.getButton(AlertDialog.BUTTON_NEGATIVE).setTextColor(color);
            }
        });

        dialog.show();
    }

    public void showTaskDetailDialog(final Entry parent) {
        if (parent.isLocked()) {
            Toast.makeText(context, "Task is locked. Subtasks cannot be edited.", Toast.LENGTH_SHORT).show();
        }
        View view = LayoutInflater.from(context).inflate(R.layout.dialog_task_detail, null);
        final TextView tvTitle = (TextView) view.findViewById(R.id.detail_title);
        final LinearLayout subtaskContainer = (LinearLayout) view.findViewById(R.id.detail_subtask_container);
        View btnAddSubtask = view.findViewById(R.id.btn_detail_add_subtask);

        tvTitle.setText(parent.getContent());

        final Set<Integer> deletingIds = new HashSet<>();
        final Runnable[] refreshRef = new Runnable[1];

        refreshRef[0] = new Runnable() {
            @Override
            public void run() {
                subtaskContainer.removeAllViews();
                List<Entry> children = dbManager.getChildEntries(parent.getId());
                for (final Entry sub : children) {
                    if (deletingIds.contains(sub.getId())) continue;

                    View row = LayoutInflater.from(context).inflate(R.layout.item_subtask_row, subtaskContainer, false);
                    final TextView sig = (TextView) row.findViewById(R.id.subtask_signifier);
                    final EditText content = (EditText) row.findViewById(R.id.subtask_content);
                    final TextView tvTime = (TextView) row.findViewById(R.id.subtask_time);
                    View btnDelete = row.findViewById(R.id.subtask_delete);

                    sig.setText(sub.isCompleted() ? "✓" : "");
                    content.setText(sub.getContent());
                    
                    if (sub.hasTime()) {
                        tvTime.setVisibility(View.VISIBLE);
                        SimpleDateFormat stf = new SimpleDateFormat("h:mm a", Locale.US);
                        tvTime.setText("Target: " + stf.format(new Date(sub.getDeadline())));
                    } else {
                        tvTime.setVisibility(View.GONE);
                    }

                    if (sub.isCompleted()) content.setPaintFlags(content.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);

                    sig.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            sub.setCompleted(!sub.isCompleted());
                            dbManager.updateEntry(sub);

                            sig.setText(sub.isCompleted() ? "✓" : "");
                            if (sub.isCompleted()) content.setPaintFlags(content.getPaintFlags() | Paint.STRIKE_THRU_TEXT_FLAG);
                            else content.setPaintFlags(content.getPaintFlags() & (~Paint.STRIKE_THRU_TEXT_FLAG));
                        }
                    });

                    sig.setOnLongClickListener(new View.OnLongClickListener() {
                        @Override
                        public boolean onLongClick(View v) {
                            if (parent.isLocked()) return true;
                            new TimePickerDialog(context, new TimePickerDialog.OnTimeSetListener() {
                                @Override
                                public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                                    Calendar c = Calendar.getInstance();
                                    c.setTimeInMillis(sub.getDeadline());
                                    c.set(Calendar.HOUR_OF_DAY, hourOfDay);
                                    c.set(Calendar.MINUTE, minute);
                                    sub.setDeadline(c.getTimeInMillis());
                                    sub.setHasTime(true);
                                    dbManager.updateEntry(sub);
                                    refreshRef[0].run();
                                }
                            }, 12, 0, false).show();
                            return true;
                        }
                    });

                    btnDelete.setOnClickListener(new View.OnClickListener() {
                        @Override
                        public void onClick(View v) {
                            if (parent.isLocked()) return;
                            deletingIds.add(sub.getId());
                            dbManager.deleteEntry(sub.getId());
                            refreshRef[0].run();
                        }
                    });

                    if (parent.isLocked()) {
                        content.setEnabled(false);
                    } else {
                        content.addTextChangedListener(new TextWatcher() {
                            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                            @Override
                            public void afterTextChanged(Editable s) {
                                String val = s.toString();
                                if (!val.equals(sub.getContent())) {
                                    sub.setContent(val);
                                    dbManager.updateEntry(sub);
                                }
                            }
                        });
                    }

                    subtaskContainer.addView(row);
                }
            }
        };

        refreshRef[0].run();

        if (parent.isLocked()) {
            btnAddSubtask.setVisibility(View.GONE);
        } else {
            btnAddSubtask.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    List<Entry> currentChildren = dbManager.getChildEntries(parent.getId());
                    if (!currentChildren.isEmpty() && currentChildren.get(currentChildren.size() - 1).getContent().trim().isEmpty()) {
                        return;
                    }

                    Entry newSub = new Entry();
                    newSub.setSignifier("*");
                    newSub.setContent("");
                    newSub.setParentId(parent.getId());
                    newSub.setProjectId(parent.getProjectId());
                    newSub.setProjectTag(parent.getProjectTag());
                    newSub.setDeadline(parent.getDeadline());
                    newSub.setCreatedAt(System.currentTimeMillis());
                    dbManager.insertEntry(newSub);
                    refreshRef[0].run();

                    View lastRow = subtaskContainer.getChildAt(subtaskContainer.getChildCount() - 1);
                    if (lastRow != null) {
                        EditText et = (EditText) lastRow.findViewById(R.id.subtask_content);
                        et.requestFocus();
                    }
                }
            });
        }

        final AlertDialog dialog = new AlertDialog.Builder(context, R.style.BujoDialog)
                .setView(view)
                .create();


        dialog.setOnShowListener(new DialogInterface.OnShowListener() {
            @Override
            public void onShow(DialogInterface dialogInterface) {
                if (dialog.getWindow() != null) {
                    DisplayMetrics metrics = context.getResources().getDisplayMetrics();
                    int width = (int) (metrics.widthPixels * 0.94);
                    dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
                    dialog.getWindow().clearFlags(WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
                    dialog.getWindow().setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }

                if (!parent.isLocked()) {
                    for (int i = 0; i < subtaskContainer.getChildCount(); i++) {
                        View row = subtaskContainer.getChildAt(i);
                        final EditText et = (EditText) row.findViewById(R.id.subtask_content);
                        if (et.getText().toString().isEmpty()) {
                            et.requestFocus();
                            et.postDelayed(new Runnable() {
                                @Override
                                public void run() {
                                    InputMethodManager imm = (InputMethodManager) 
                                            context.getSystemService(Context.INPUT_METHOD_SERVICE);
                                    if (imm != null) imm.showSoftInput(et, InputMethodManager.SHOW_IMPLICIT);
                                }
                            }, 100);
                            break;
                        }
                    }
                }
            }
        });

        dialog.setOnDismissListener(new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                List<Entry> children = dbManager.getChildEntries(parent.getId());
                for (Entry sub : children) {
                    if (sub.getContent().trim().isEmpty()) {
                        dbManager.deleteEntry(sub.getId());
                    }
                }
                if (listener != null) listener.onEntryUpdated();
            }
        });

        dialog.show();
    }
}
