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
    private List<Object> entries;
    private List<Object> allEntriesList = new ArrayList<>();
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

        etNewEntry.addTextChangedListener(new android.text.TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterEntries(s.toString());
            }
            @Override public void afterTextChanged(android.text.Editable s) {}
        });

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

        View btnPlan = root.findViewById(R.id.btn_plan_tomorrow);
        if (btnPlan != null) {
            btnPlan.setOnClickListener(v -> showRecommendationDialog());
        }

        return root;
    }

    public void showRecommendationDialog() {
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

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity(), R.style.BujoDialog);
        builder.setView(view);
        final AlertDialog dialog = builder.create();

        view.findViewById(R.id.btn_plan_cancel).setOnClickListener(v -> dialog.dismiss());

        view.findViewById(R.id.btn_plan_add).setOnClickListener(v -> {
            dialog.dismiss();
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
                main.refreshProfileIcon();
            }
        });

        dialog.setOnShowListener(d -> {
            if (dialog.getWindow() != null) {
                android.util.DisplayMetrics metrics = getResources().getDisplayMetrics();
                int width = (int) (metrics.widthPixels * 0.88);
                dialog.getWindow().setLayout(width, ViewGroup.LayoutParams.WRAP_CONTENT);
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
            
            if (insertedId != -1 && getActivity() instanceof MainActivity) {
                int commitment = dbManager.calculateCommitmentReward(newEntry);
                if (commitment > 0) {
                    ((MainActivity) getActivity()).animatePointsChange(commitment, etNewEntry);
                }
            }

            loadEntries();
            updateCompletionRatio();
            etNewEntry.setText("");
        }
    }

    private void filterEntries(String query) {
        if (adapter == null) return;
        List<Object> filtered = com.ismailmushraf.bujo.utils.SearchHelper.filter(allEntriesList, query, item -> {
            if (item instanceof Entry) {
                Entry e = (Entry) item;
                return e.getContent() != null ? e.getContent() : "";
            } else if (item instanceof String) {
                return (String) item;
            }
            return "";
        });
        adapter.clear();
        adapter.addAll(filtered);
        adapter.notifyDataSetChanged();
    }

    private void loadEntries() {
        int index = listView.getFirstVisiblePosition();
        View v = listView.getChildAt(0);
        int top = (v == null) ? 0 : (v.getTop() - listView.getPaddingTop());

        allEntriesList = new ArrayList<>(dbManager.getTodayEntries());
        entries = new ArrayList<>(allEntriesList);

        if (adapter == null || listView.getAdapter() == null) {
            adapter = new EntryAdapter(getActivity(), entries, true, true);
            adapter.setUIHelper(uiHelper);
            adapter.setOnEntryInteractionListener(new EntryAdapter.OnEntryInteractionListener() {
                @Override
                public void onEntryTextClick(Entry entry) {
                    if (getFragmentManager() != null && entry != null) {
                        android.support.v4.app.FragmentTransaction ft = getFragmentManager().beginTransaction();
                        ft.setCustomAnimations(R.anim.slide_in_right, R.anim.slide_out_left, R.anim.slide_in_left, R.anim.slide_out_right);
                        ft.replace(R.id.fragment_container, EditTaskFragment.newInstance(entry.getId()));
                        ft.addToBackStack(null);
                        ft.commit();
                    }
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
            for (Object item : entries) {
                if (item instanceof Entry) {
                    Entry entry = (Entry) item;
                    if (entry.getParentId() == 0 && "*".equals(entry.getSignifier())) {
                        totalTasks++;
                        if (entry.isCompleted()) completedCount++;
                    }
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
