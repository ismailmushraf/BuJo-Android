package com.ismailmushraf.bujo.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;
import com.ismailmushraf.bujo.R;
import com.ismailmushraf.bujo.adapters.EntryAdapter;
import com.ismailmushraf.bujo.models.Entry;
import java.util.ArrayList;
import java.util.List;

public class DailyLogFragment extends Fragment {

    private EntryAdapter adapter;
    private List<Entry> mockEntries;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_daily_log, container, false);

        TextView tvDate = (TextView) root.findViewById(R.id.tv_date_title);
        ListView listView = (ListView) root.findViewById(R.id.lv_daily_bullets);

        tvDate.setText("TODAY'S LOG");

        mockEntries = new ArrayList<Entry>();
        mockEntries.add(new Entry(1, "*", "Fix OkHttp TLS handshake configuration", "Network", false));
        mockEntries.add(new Entry(2, "-", "Review Android 4.3 memory leaks", null, false));
        mockEntries.add(new Entry(3, "o", "Client review briefing", "Consulting", false));

        adapter = new EntryAdapter(getActivity(), mockEntries);
        listView.setAdapter(adapter);

        // Making the list responsive
        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Entry clickedEntry = mockEntries.get(position);
                // Toggle the completion state
                clickedEntry.setCompleted(!clickedEntry.isCompleted());
                // Tell the adapter to redraw the list
                adapter.notifyDataSetChanged();
            }
        });

        return root;
    }
}