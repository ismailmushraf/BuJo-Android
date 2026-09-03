package com.ismailmushraf.bujo.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.SystemClock;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.AlphaAnimation;
import android.view.animation.Animation;
import android.view.animation.AnimationSet;
import android.view.animation.ScaleAnimation;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.Chronometer;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.TextView;

import com.ismailmushraf.bujo.MainActivity;
import com.ismailmushraf.bujo.R;
import com.ismailmushraf.bujo.db.DatabaseManager;
import com.ismailmushraf.bujo.models.WorkoutSet;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class WorkoutFragment extends Fragment {

    private DatabaseManager dbManager;
    private Chronometer chronometer;
    private boolean isTracking = false;
    private long accumulatedTime = 0;
    private String todayStr;

    private AutoCompleteTextView autoExercise;
    private EditText etWeight, etReps, etNote;
    private ListView lvToday;
    private Button btnToggle;
    private View celebrationLayout;

    private List<Object> todayItems = new ArrayList<>();
    private WorkoutAdapter listAdapter;

    // Reusable adapter to prevent GC pressure
    private ArrayAdapter<String> autoAdapter;
    private List<String> suggestionsList = new ArrayList<>();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setHasOptionsMenu(true);
        dbManager = new DatabaseManager(getActivity());
        todayStr = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_workout, container, false);

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setToolbarTitle("WORKOUT");
            ((MainActivity) getActivity()).setToolbarSubtitle(todayStr);
        }

        dbManager.open();

        lvToday = root.findViewById(R.id.lv_today_workouts);
        celebrationLayout = root.findViewById(R.id.layout_celebration);

        View headerView = inflater.inflate(R.layout.header_workout, lvToday, false);
        lvToday.addHeaderView(headerView, null, false);

        chronometer = headerView.findViewById(R.id.chronometer);
        btnToggle = headerView.findViewById(R.id.btn_timer_toggle);
        autoExercise = headerView.findViewById(R.id.auto_exercise);
        etWeight = headerView.findViewById(R.id.et_weight);
        etReps = headerView.findViewById(R.id.et_reps);
        etNote = headerView.findViewById(R.id.et_note);

        accumulatedTime = dbManager.getSessionDuration(todayStr);
        chronometer.setBase(SystemClock.elapsedRealtime() - accumulatedTime);

        // --- FOCUS FIX FOR BB10 RUNTIME ---
        autoExercise.setFocusable(false);
        autoExercise.setFocusableInTouchMode(false);
        autoExercise.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, android.view.MotionEvent event) {
                autoExercise.setFocusable(true);
                autoExercise.setFocusableInTouchMode(true);
                return false;
            }
        });

        // --- AUTOCOMPLETE SELECTION FIX ---
        autoAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_dropdown_item_1line, suggestionsList);
        autoExercise.setAdapter(autoAdapter);

        autoExercise.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                String selectedText = (String) parent.getItemAtPosition(position);
                autoExercise.setText(selectedText);
                autoExercise.setSelection(selectedText.length()); // Move cursor to end
                etWeight.requestFocus(); // Move focus to Weight input automatically
            }
        });

        btnToggle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!isTracking) {
                    chronometer.setBase(SystemClock.elapsedRealtime() - accumulatedTime);
                    chronometer.start();
                    btnToggle.setText("STOP SESSION");
                    isTracking = true;
                } else {
                    chronometer.stop();
                    accumulatedTime = SystemClock.elapsedRealtime() - chronometer.getBase();
                    dbManager.saveSessionDuration(todayStr, accumulatedTime);
                    btnToggle.setText("START SESSION");
                    isTracking = false;
                }
            }
        });

        headerView.findViewById(R.id.btn_add_set).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String ex = autoExercise.getText().toString().trim();
                if (ex.isEmpty()) return;

                double w = etWeight.getText().toString().isEmpty() ? 0 : Double.parseDouble(etWeight.getText().toString());
                int r = etReps.getText().toString().isEmpty() ? 0 : Integer.parseInt(etReps.getText().toString());
                String n = etNote.getText().toString().trim();

                double oldPR = dbManager.getPersonalRecord(ex);
                double newScore = (w <= 0) ? r : (w * (1.0 + (r / 30.0)));

                WorkoutSet ws = new WorkoutSet(todayStr, ex, w, r, n);
                long insertedId = dbManager.insertWorkoutSet(ws);

                if (insertedId != -1 && getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).animatePointsChange(10, v);
                }

                etReps.setText("");
                etNote.setText("");

                InputMethodManager imm = (InputMethodManager) getActivity().getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) imm.hideSoftInputFromWindow(v.getWindowToken(), 0);

                refreshUI();

                if (newScore > oldPR && oldPR > 0) {
                    triggerPRAnimation();
                }
            }
        });

        listAdapter = new WorkoutAdapter();
        lvToday.setAdapter(listAdapter);

        lvToday.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                int adjPos = position - lvToday.getHeaderViewsCount();
                if (adjPos >= 0 && adjPos < todayItems.size()) {
                    Object item = todayItems.get(adjPos);
                    if (item instanceof WorkoutSet) {
                        final WorkoutSet ws = (WorkoutSet) item;
                        new AlertDialog.Builder(getActivity())
                                .setTitle("Manage Set")
                                .setMessage("Delete this set?")
                                .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                                    @Override
                                    public void onClick(DialogInterface dialog, int which) {
                                        int appliedPoints = dbManager.deleteWorkoutSet(ws.getId());
                                        if (getActivity() instanceof MainActivity) {
                                            ((MainActivity) getActivity()).animatePointsChange(appliedPoints, view);
                                            ((MainActivity) getActivity()).refreshProfileIcon();
                                        }
                                        refreshUI();
                                    }
                                })
                                .setNegativeButton("Cancel", null)
                                .show();
                    }
                }
            }
        });

        refreshUI();
        return root;
    }

    private void refreshUI() {
        // Efficient dataset update without re-instantiating adapters
        suggestionsList.clear();
        suggestionsList.addAll(dbManager.getUniqueExerciseNames());
        autoAdapter.notifyDataSetChanged();

        todayItems.clear();
        todayItems.addAll(dbManager.getGroupedDailyWorkouts(todayStr));
        listAdapter.notifyDataSetChanged();
    }

    private void triggerPRAnimation() {
        celebrationLayout.setVisibility(View.VISIBLE);

        AnimationSet animSet = new AnimationSet(true);
        ScaleAnimation scale = new ScaleAnimation(0.2f, 1f, 0.2f, 1f, Animation.RELATIVE_TO_SELF, 0.5f, Animation.RELATIVE_TO_SELF, 0.5f);
        scale.setDuration(600);
        AlphaAnimation fadeOut = new AlphaAnimation(1f, 0f);
        fadeOut.setStartOffset(1500);
        fadeOut.setDuration(500);

        animSet.addAnimation(scale);
        animSet.addAnimation(fadeOut);

        animSet.setAnimationListener(new Animation.AnimationListener() {
            @Override public void onAnimationStart(Animation animation) {}
            @Override public void onAnimationRepeat(Animation animation) {}
            @Override public void onAnimationEnd(Animation animation) {
                celebrationLayout.setVisibility(View.GONE);
            }
        });

        celebrationLayout.startAnimation(animSet);
    }

    // --- OPTIMIZED ADAPTER WITH VIEWHOLDER PATTERN ---
    private class WorkoutAdapter extends BaseAdapter {
        private static final int TYPE_HEADER = 0;
        private static final int TYPE_ITEM = 1;

        @Override public int getCount() { return todayItems.size(); }
        @Override public Object getItem(int position) { return todayItems.get(position); }
        @Override public long getItemId(int position) { return position; }
        @Override public int getViewTypeCount() { return 2; }
        @Override public int getItemViewType(int position) {
            return (todayItems.get(position) instanceof String) ? TYPE_HEADER : TYPE_ITEM;
        }

        private class ItemViewHolder {
            TextView tvLine1;
            TextView tvLine2;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            int type = getItemViewType(position);
            Object data = todayItems.get(position);

            if (type == TYPE_HEADER) {
                TextView tv;
                if (convertView == null) {
                    tv = new TextView(getActivity());
                    tv.setPadding(16, 24, 16, 8);
                    tv.setTextSize(16);
                    tv.setTextColor(getResources().getColor(R.color.bujo_text_secondary));
                    tv.setTypeface(null, android.graphics.Typeface.BOLD);
                    tv.setBackgroundColor(getResources().getColor(R.color.bujo_divider));
                    convertView = tv;
                } else {
                    tv = (TextView) convertView;
                }
                tv.setText((String) data);
            } else {
                ItemViewHolder holder;
                if (convertView == null) {
                    LinearLayout ll = new LinearLayout(getActivity());
                    ll.setOrientation(LinearLayout.VERTICAL);
                    ll.setPadding(32, 16, 16, 16);

                    holder = new ItemViewHolder();
                    holder.tvLine1 = new TextView(getActivity());
                    holder.tvLine1.setTextColor(getResources().getColor(R.color.bujo_text));
                    holder.tvLine1.setTextSize(16);

                    holder.tvLine2 = new TextView(getActivity());
                    holder.tvLine2.setTextColor(getResources().getColor(R.color.bujo_text_secondary));
                    holder.tvLine2.setTextSize(14);

                    ll.addView(holder.tvLine1);
                    ll.addView(holder.tvLine2);

                    convertView = ll;
                    convertView.setTag(holder);
                } else {
                    holder = (ItemViewHolder) convertView.getTag();
                }

                WorkoutSet ws = (WorkoutSet) data;
                String textLine1 = "Round " + ws.getSetNumber() + ": " + ws.getReps() + " reps";
                if (ws.getWeight() > 0) textLine1 += " @ " + ws.getWeight() + " kg";

                holder.tvLine1.setText(textLine1);

                if (ws.getNote() != null && !ws.getNote().isEmpty()) {
                    holder.tvLine2.setVisibility(View.VISIBLE);
                    holder.tvLine2.setText("Note: " + ws.getNote());
                } else {
                    holder.tvLine2.setVisibility(View.GONE);
                }
            }
            return convertView;
        }
    }

    @Override
    public void onCreateOptionsMenu(Menu menu, MenuInflater inflater) {
        MenuItem item = menu.add("History");
        item.setShowAsAction(MenuItem.SHOW_AS_ACTION_ALWAYS);
        item.setIcon(android.R.drawable.ic_menu_recent_history);
        item.setOnMenuItemClickListener(new MenuItem.OnMenuItemClickListener() {
            @Override
            public boolean onMenuItemClick(MenuItem item) {
                if (isTracking) btnToggle.performClick();
                FragmentTransaction ft = getActivity().getSupportFragmentManager().beginTransaction();
                ft.replace(R.id.fragment_container, new WorkoutHistoryFragment());
                ft.addToBackStack(null);
                ft.commit();
                return true;
            }
        });
    }

    @Override
    public void onPause() {
        super.onPause();
        if (isTracking) {
            accumulatedTime = SystemClock.elapsedRealtime() - chronometer.getBase();
            dbManager.saveSessionDuration(todayStr, accumulatedTime);
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        dbManager.close();
    }
}
