package com.ismailmushraf.bujo.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.ismailmushraf.bujo.MainActivity;
import com.ismailmushraf.bujo.R;
import com.ismailmushraf.bujo.db.DatabaseManager;
import com.ismailmushraf.bujo.models.WorkoutSet;

import java.util.ArrayList;
import java.util.List;

public class WorkoutHistoryFragment extends Fragment {

    private DatabaseManager dbManager;
    private List<Object> historyItems; // Holds both Strings (Dates) and WorkoutSets

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_workout_history, container, false);

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setToolbarTitle("WORKOUT HISTORY");
            ((MainActivity) getActivity()).setToolbarSubtitle("");
        }

        dbManager = new DatabaseManager(getActivity());
        dbManager.open();

        historyItems = new ArrayList<>();
        List<String> dates = dbManager.getAllWorkoutDatesDescending();

        for (String date : dates) {
            long durationMs = dbManager.getSessionDuration(date);
            long minutes = (durationMs / 1000) / 60;
            historyItems.add(date + " (Duration: " + minutes + " min)"); // Header

            // We use getGroupedDailyWorkouts logic to fetch properly sorted sets
            List<Object> items = dbManager.getGroupedDailyWorkouts(date);
            historyItems.addAll(items); // Includes exercises and set details
        }

        ListView lvHistory = root.findViewById(R.id.lv_history);
        lvHistory.setAdapter(new HistoryAdapter());

        return root;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        dbManager.close();
    }

    private class HistoryAdapter extends BaseAdapter {
        private static final int TYPE_HEADER = 0;
        private static final int TYPE_ITEM = 1;

        @Override public int getCount() { return historyItems.size(); }
        @Override public Object getItem(int position) { return historyItems.get(position); }
        @Override public long getItemId(int position) { return position; }
        @Override public int getViewTypeCount() { return 2; }
        @Override public int getItemViewType(int position) {
            return (historyItems.get(position) instanceof String) ? TYPE_HEADER : TYPE_ITEM;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            int type = getItemViewType(position);

            if (convertView == null) {
                if (type == TYPE_HEADER) {
                    TextView tv = new TextView(getActivity());
                    tv.setPadding(16, 32, 16, 8);
                    tv.setTextSize(14);
                    tv.setTextColor(getResources().getColor(R.color.bujo_text_secondary));
                    tv.setTypeface(null, android.graphics.Typeface.BOLD);
                    tv.setBackgroundColor(getResources().getColor(R.color.bujo_divider));
                    convertView = tv;
                } else {
                    // Create view programmatically to match the new set-based detail view
                    LinearLayout ll = new LinearLayout(getActivity());
                    ll.setOrientation(LinearLayout.VERTICAL);
                    ll.setPadding(32, 16, 16, 16);

                    TextView tvLine1 = new TextView(getActivity());
                    tvLine1.setId(android.R.id.text1);
                    tvLine1.setTextColor(getResources().getColor(R.color.bujo_text));
                    tvLine1.setTextSize(16);

                    TextView tvLine2 = new TextView(getActivity());
                    tvLine2.setId(android.R.id.text2);
                    tvLine2.setTextColor(getResources().getColor(R.color.bujo_text_secondary));
                    tvLine2.setTextSize(14);

                    ll.addView(tvLine1);
                    ll.addView(tvLine2);
                    convertView = ll;
                }
            }

            if (type == TYPE_HEADER) {
                ((TextView) convertView).setText((String) historyItems.get(position));
            } else {
                WorkoutSet ws = (WorkoutSet) historyItems.get(position);
                String line1 = "Round " + ws.getSetNumber() + ": " + ws.getReps() + " reps";
                if (ws.getWeight() > 0) line1 += " @ " + ws.getWeight() + " kg";

                ((TextView) convertView.findViewById(android.R.id.text1)).setText(line1);

                TextView noteView = convertView.findViewById(android.R.id.text2);
                if (ws.getNote() != null && !ws.getNote().isEmpty()) {
                    noteView.setVisibility(View.VISIBLE);
                    noteView.setText("Note: " + ws.getNote());
                } else {
                    noteView.setVisibility(View.GONE);
                }
            }
            return convertView;
        }
    }
}