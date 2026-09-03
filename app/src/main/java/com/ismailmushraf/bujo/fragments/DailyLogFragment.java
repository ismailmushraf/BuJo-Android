package com.ismailmushraf.bujo.fragments;

import android.app.AlertDialog;
import android.graphics.Paint;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.ismailmushraf.bujo.MainActivity;
import com.ismailmushraf.bujo.R;
import com.ismailmushraf.bujo.adapters.EntryAdapter;
import com.ismailmushraf.bujo.db.DatabaseManager;
import com.ismailmushraf.bujo.models.Entry;
import com.ismailmushraf.bujo.models.Project;
import com.ismailmushraf.bujo.utils.EntryUIHelper;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

public class DailyLogFragment extends Fragment {

    private EntryAdapter adapter;
    private List<Entry> entries;
    private ListView listView;
    private DatabaseManager dbManager;
    private EntryUIHelper uiHelper;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_daily_log, container, false);

        if (getActivity() instanceof MainActivity) {
            SimpleDateFormat sdf = new SimpleDateFormat("EEEE, MMMM d", Locale.US);
            ((MainActivity) getActivity()).setToolbarTitle(sdf.format(new Date()).toUpperCase());
            ((MainActivity) getActivity()).setToolbarSubtitle("");
        }

        listView = (ListView) root.findViewById(R.id.lv_daily_bullets);
        final EditText etNewEntry = (EditText) root.findViewById(R.id.et_new_entry);

        dbManager = new DatabaseManager(getActivity());
        dbManager.open();

        uiHelper = new EntryUIHelper(getActivity(), dbManager, new EntryUIHelper.OnEntryUpdatedListener() {
            @Override
            public void onEntryUpdated() {
                loadEntries();
                updateCompletionRatio();
            }
        });

        loadEntries();
        updateCompletionRatio();

        listView.setOnItemClickListener(null);
        listView.setOnItemLongClickListener(null);

        etNewEntry.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView v, int actionId, KeyEvent event) {
                if (actionId == android.view.inputmethod.EditorInfo.IME_ACTION_DONE || actionId == 0) {
                    processNewEntry(etNewEntry);
                    return true;
                }
                return false;
            }
        });

        etNewEntry.setOnKeyListener(new View.OnKeyListener() {
            @Override
            public boolean onKey(View v, int keyCode, KeyEvent event) {
                if (event.getAction() == KeyEvent.ACTION_DOWN &&
                        (keyCode == KeyEvent.KEYCODE_ENTER || keyCode == KeyEvent.KEYCODE_NUMPAD_ENTER || keyCode == KeyEvent.KEYCODE_PLUS)) {
                    processNewEntry(etNewEntry);
                    return true;
                }
                return false;
            }
        });

        TextView btnEmoji = (TextView) root.findViewById(R.id.btn_emoji);
        btnEmoji.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                uiHelper.showEmojiPicker(etNewEntry);
            }
        });

        final android.widget.Button btnPlan = (android.widget.Button) root.findViewById(R.id.btn_plan_tomorrow);
        btnPlan.setText("Plan Day");
        btnPlan.setEnabled(true);
        btnPlan.setTextColor(getResources().getColor(R.color.bujo_text));
        btnPlan.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showRecommendationDialog();
            }
        });

        return root;
    }

    private void showRecommendationDialog() {
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_plan_day, null);
        final android.widget.RadioGroup rgTarget = (android.widget.RadioGroup) view.findViewById(R.id.rg_plan_target);
        final TextView tvHours = (TextView) view.findViewById(R.id.tv_available_hours);
        final ListView lvRecs = (ListView) view.findViewById(R.id.lv_recommendations);

        View footer = new View(getActivity());
        int footerHeight = (int) (24 * getResources().getDisplayMetrics().density);
        footer.setLayoutParams(new ListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, footerHeight));
        lvRecs.addFooterView(footer, null, false);

        final List<Entry> currentRecs = new ArrayList<>();
        final ArrayAdapter<String> adapterRecs = new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_multiple_choice, new ArrayList<String>());
        lvRecs.setAdapter(adapterRecs);

        final Runnable refreshRecs = new Runnable() {
            @Override
            public void run() {
                boolean isToday = rgTarget.getCheckedRadioButtonId() == R.id.rb_today;
                int hours = calculateAvailableHours(isToday);
                tvHours.setText("Available working hours: " + hours + "h");
                currentRecs.clear();
                currentRecs.addAll(dbManager.getSmartRecommendations(hours));
                adapterRecs.clear();
                for (Entry e : currentRecs) adapterRecs.add(e.getContent());
                adapterRecs.notifyDataSetChanged();
                for (int i = 0; i < adapterRecs.getCount(); i++) lvRecs.setItemChecked(i, true);
            }
        };

        rgTarget.setOnCheckedChangeListener(new android.widget.RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(android.widget.RadioGroup group, int checkedId) {
                refreshRecs.run();
            }
        });

        refreshRecs.run();

        new android.app.AlertDialog.Builder(getActivity())
               .setTitle("Plan Sessions")
               .setView(view)
               .setPositiveButton("Add to Log", new android.content.DialogInterface.OnClickListener() {
                   @Override
                   public void onClick(android.content.DialogInterface dialog, int which) {
                       boolean isToday = rgTarget.getCheckedRadioButtonId() == R.id.rb_today;
                       Calendar targetDate = Calendar.getInstance();
                       if (!isToday) targetDate.add(Calendar.DAY_OF_YEAR, 1);
                       targetDate.set(Calendar.HOUR_OF_DAY, 12);
                       targetDate.set(Calendar.MINUTE, 0);

                       android.util.SparseBooleanArray checked = lvRecs.getCheckedItemPositions();
                       int totalCommitment = 0;
                       for (int i = 0; i < currentRecs.size(); i++) {
                           if (checked.get(i)) {
                               Entry e = currentRecs.get(i);
                               e.setDeadline(targetDate.getTimeInMillis());
                               e.setCreatedAt(System.currentTimeMillis());
                               if (dbManager.insertEntry(e) != -1) {
                                   totalCommitment += dbManager.calculateCommitmentReward(e);
                               }
                           }
                       }
                       loadEntries();
                       
                       if (totalCommitment > 0 && getActivity() instanceof MainActivity) {
                           MainActivity main = (MainActivity) getActivity();
                           main.showPlanningBonusModal(totalCommitment);
                           main.refreshProfileIcon(); // Immediate refresh
                       }
                   }
               })
               .setNegativeButton(android.R.string.cancel, null)
               .show();
    }

    private void showTaskDetailDialog(final Entry parent) {
        View view = LayoutInflater.from(getActivity()).inflate(R.layout.dialog_task_detail, null);
        final TextView tvTitle = (TextView) view.findViewById(R.id.detail_title);
        final LinearLayout subtaskContainer = (LinearLayout) view.findViewById(R.id.detail_subtask_container);
        View btnAddSubtask = view.findViewById(R.id.btn_detail_add_subtask);

        tvTitle.setText(parent.getContent());

        final java.util.Set<Integer> deletingIds = new java.util.HashSet<>();
        final Runnable[] refreshRef = new Runnable[1];

        refreshRef[0] = new Runnable() {
            @Override
            public void run() {
                if (getActivity() == null) return;
                subtaskContainer.removeAllViews();
                List<Entry> children = dbManager.getChildEntries(parent.getId());
                for (final Entry sub : children) {
                    if (deletingIds.contains(sub.getId())) continue;

                    View row = LayoutInflater.from(getActivity()).inflate(R.layout.item_subtask_row, subtaskContainer, false);
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
                            // Sub-task time picker
                            new android.app.TimePickerDialog(getActivity(), new android.app.TimePickerDialog.OnTimeSetListener() {
                                @Override
                                public void onTimeSet(android.widget.TimePicker view, int hourOfDay, int minute) {
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
                            deletingIds.add(sub.getId());
                            dbManager.deleteEntry(sub.getId());
                            refreshRef[0].run();
                        }
                    });

                    // Use TextWatcher for immediate saving - works better with physical keyboards
                    content.addTextChangedListener(new android.text.TextWatcher() {
                        @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
                        @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
                        @Override
                        public void afterTextChanged(android.text.Editable s) {
                            String val = s.toString();
                            if (!val.equals(sub.getContent())) {
                                sub.setContent(val);
                                dbManager.updateEntry(sub);
                            }
                        }
                    });

                    subtaskContainer.addView(row);
                }
            }
        };

        refreshRef[0].run();

        btnAddSubtask.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                // Check if last one is empty - now querying fresh from DB
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

        final AlertDialog dialog = new AlertDialog.Builder(getActivity(), R.style.BujoDialog)
                .setView(view)
                .create();


        dialog.setOnShowListener(new android.content.DialogInterface.OnShowListener() {
            @Override
            public void onShow(android.content.DialogInterface dialogInterface) {
                // Increase Dialog Width for BlackBerry Passport
                if (dialog.getWindow() != null) {
                    android.util.DisplayMetrics metrics = new android.util.DisplayMetrics();
                    getActivity().getWindowManager().getDefaultDisplay().getMetrics(metrics);
                    int width = (int) (metrics.widthPixels * 0.94);
                    dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
                    
                    // Force focusable mode and show keyboard
                    dialog.getWindow().clearFlags(android.view.WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | android.view.WindowManager.LayoutParams.FLAG_ALT_FOCUSABLE_IM);
                    dialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_VISIBLE);
                }

                // Focus first empty row
                for (int i = 0; i < subtaskContainer.getChildCount(); i++) {
                    View row = subtaskContainer.getChildAt(i);
                    final EditText et = (EditText) row.findViewById(R.id.subtask_content);
                    if (et.getText().toString().isEmpty()) {
                        et.requestFocus();
                        // Force native keyboard show for emulator/legacy devices
                        et.postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) 
                                        getActivity().getSystemService(android.content.Context.INPUT_METHOD_SERVICE);
                                if (imm != null) imm.showSoftInput(et, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
                            }
                        }, 100);
                        break;
                    }
                }
            }
        });

        dialog.setOnDismissListener(new android.content.DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(android.content.DialogInterface dialog) {
                // Cleanup any empty sub-tasks on exit
                List<Entry> children = dbManager.getChildEntries(parent.getId());
                for (Entry sub : children) {
                    if (sub.getContent().trim().isEmpty()) {
                        dbManager.deleteEntry(sub.getId());
                    }
                }
                loadEntries();
            }
        });

        dialog.show();
    }

    private int calculateAvailableHours(boolean isToday) {
        if (!isToday) return 14;
        Calendar now = Calendar.getInstance();
        int hour = now.get(Calendar.HOUR_OF_DAY);
        int start = 6;
        int end = 20;
        if (hour >= end) return 0;
        return Math.max(0, end - Math.max(start, hour));
    }

    private void processNewEntry(EditText etNewEntry) {
        String content = etNewEntry.getText().toString();
        if (!content.trim().isEmpty()) {
            Entry newEntry = com.ismailmushraf.bujo.utils.EntryParser.parse(content);
            int projectId = 0;
            if (newEntry.getProjectTag() != null) {
                Project p = dbManager.getOrCreateProject(newEntry.getProjectTag());
                projectId = p.getId();
                if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).refreshDrawer();
            }
            newEntry.setDeadline(Calendar.getInstance().getTimeInMillis());
            newEntry.setProjectId(projectId);
            long insertedId = dbManager.insertEntry(newEntry);
            
            int commitment = dbManager.calculateCommitmentReward(newEntry);
            if (insertedId != -1 && commitment > 0 && getActivity() instanceof MainActivity) {
                ((MainActivity) getActivity()).animatePointsChange(commitment, etNewEntry);
            }

            loadEntries();
            updateCompletionRatio();
            etNewEntry.setText("");
        }
    }

    private void loadEntries() {
        int index = listView.getFirstVisiblePosition();
        View v = listView.getChildAt(0);
        int top = (v == null) ? 0 : (v.getTop() - listView.getPaddingTop());

        entries = dbManager.getTodayEntries();

        if (adapter == null || listView.getAdapter() == null) {
            adapter = new EntryAdapter(getActivity(), entries, true, true);
            adapter.setUIHelper(uiHelper);
            adapter.setOnEntryInteractionListener(new EntryAdapter.OnEntryInteractionListener() {
                @Override
                public void onEntryTextClick(Entry entry) {
                    showTaskDetailDialog(entry);
                }

                @Override
                public void onEntryLongClick(Entry entry, View view) {
                    uiHelper.showContextDialog(entry, view);
                }
            });
            listView.setAdapter(adapter);
        } else {
            adapter.clear();
            adapter.addAll(entries);
            adapter.notifyDataSetChanged();
            listView.setSelectionFromTop(index, top);
        }
    }

    private void updateCompletionRatio() {
        String completion;
        if (entries == null || entries.isEmpty()) {
            completion = "0/0";
        } else {
            int completedCount = 0;
            int totalTasks = 0;
            for (Entry entry : entries) {
                if (entry.getParentId() == 0 && "*".equals(entry.getSignifier())) {
                    totalTasks++;
                    if (entry.isCompleted()) completedCount++;
                }
            }
            completion = totalTasks == 0 ? entries.size() + " items" : completedCount + "/" + totalTasks;
        }
        if (getActivity() instanceof MainActivity) ((MainActivity) getActivity()).setToolbarSubtitle(completion);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (dbManager != null) dbManager.close();
    }
}
