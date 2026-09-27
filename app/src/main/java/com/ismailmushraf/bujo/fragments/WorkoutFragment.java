package com.ismailmushraf.bujo.fragments;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.os.Bundle;
import android.os.SystemClock;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentTransaction;
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
import android.widget.Toast;

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
    private View btnToggle; // Changed from Button to View to handle ImageView cast
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
        View btnReset = headerView.findViewById(R.id.btn_timer_reset);
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
                    if (btnToggle instanceof android.widget.ImageView) {
                        ((android.widget.ImageView) btnToggle).setImageResource(R.drawable.ic_bb10_pause);
                    }
                    isTracking = true;
                } else {
                    chronometer.stop();
                    accumulatedTime = SystemClock.elapsedRealtime() - chronometer.getBase();
                    dbManager.saveSessionDuration(todayStr, accumulatedTime);
                    if (btnToggle instanceof android.widget.ImageView) {
                        ((android.widget.ImageView) btnToggle).setImageResource(R.drawable.ic_bb10_play);
                    }
                    isTracking = false;
                }
            }
        });

        btnReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (isTracking) {
                    chronometer.stop();
                    isTracking = false;
                    if (btnToggle instanceof android.widget.ImageView) {
                        ((android.widget.ImageView) btnToggle).setImageResource(R.drawable.ic_bb10_play);
                    }
                }
                accumulatedTime = 0;
                chronometer.setBase(SystemClock.elapsedRealtime());
                dbManager.saveSessionDuration(todayStr, 0);
            }
        });

        headerView.findViewById(R.id.btn_add_set).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String ex = autoExercise.getText().toString().trim();
                if (ex.isEmpty()) return;

                double w;
                int r;
                try {
                    w = etWeight.getText().toString().isEmpty() ? 0 : Double.parseDouble(etWeight.getText().toString());
                    r = etReps.getText().toString().isEmpty() ? 0 : Integer.parseInt(etReps.getText().toString());
                } catch (NumberFormatException exception) {
                    Toast.makeText(getActivity(), "Enter a valid weight and whole-number rep count.", Toast.LENGTH_SHORT).show();
                    return;
                }
                String n = etNote.getText().toString().trim();

                double oldPR = dbManager.getPersonalRecord(ex);
                double newScore = (w <= 0) ? r : (w * (1.0 + (r / 30.0)));

                WorkoutSet ws = new WorkoutSet(todayStr, ex, w, r, n);
                long insertedId = dbManager.insertWorkoutSet(ws);

                if (insertedId != -1 && getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).animatePointsChange(com.ismailmushraf.bujo.utils.GamificationManager.POINTS_WORKOUT_SET, v);
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
                // Future use if needed
            }
        });

        lvToday.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {
            @Override
            public boolean onItemLongClick(AdapterView<?> parent, final View view, int position, long id) {
                int adjPos = position - lvToday.getHeaderViewsCount();
                if (adjPos >= 0 && adjPos < todayItems.size()) {
                    Object item = todayItems.get(adjPos);
                    if (item instanceof WorkoutSet) {
                        final WorkoutSet ws = (WorkoutSet) item;
                        showBB10ContextMenu(ws, view);
                        return true;
                    }
                }
                return false;
            }
        });

        refreshUI();
        return root;
    }

    private void showBB10ContextMenu(final WorkoutSet ws, final View sourceView) {
        com.ismailmushraf.bujo.utils.BB10DialogHelper.showSidebar(getActivity(), R.layout.dialog_bb10_context_sidebar, (dialog, view) -> {
            TextView tvTitle = view.findViewById(R.id.sidebar_task_title);
            String title = ws.getExercise() + " (Set " + ws.getSetNumber() + ")";
            tvTitle.setText(title);

            ListView lvOptions = view.findViewById(R.id.lv_sidebar_options);
            lvOptions.setAdapter(new ArrayAdapter<>(getActivity(), android.R.layout.simple_list_item_1, new ArrayList<String>()));

            view.findViewById(R.id.sidebar_bottom_delete).setOnClickListener(v -> {
                dialog.dismiss();
                com.ismailmushraf.bujo.utils.BB10DialogHelper.showConfirmDialog(getActivity(), "Delete Set", "Are you sure you want to delete this set?", "Delete", () -> {
                    int appliedPoints = dbManager.deleteWorkoutSet(ws.getId());
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).animatePointsChange(appliedPoints, sourceView);
                        ((MainActivity) getActivity()).refreshProfileIcon();
                    }
                    refreshUI();
                });
            });
        });
    }

    private void refreshUI() {
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
                    convertView = LayoutInflater.from(getActivity()).inflate(R.layout.item_workout_set_row, parent, false);
                    holder = new ItemViewHolder();
                    holder.tvLine1 = convertView.findViewById(R.id.tv_set_title);
                    holder.tvLine2 = convertView.findViewById(R.id.tv_set_note);
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

    public void openRightSidebar() {
        com.ismailmushraf.bujo.utils.BB10DialogHelper.showSidebar(getActivity(), R.layout.dialog_bb10_workout_sidebar, (dialog, view) -> {
            class SidebarOption {
                final String title;
                final int iconResId;
                SidebarOption(String title, int iconResId) {
                    this.title = title;
                    this.iconResId = iconResId;
                }
            }

            List<SidebarOption> optionsList = new ArrayList<>();
            optionsList.add(new SidebarOption("History", R.drawable.ic_bb10_history));

            ListView lvOptions = view.findViewById(R.id.lv_sidebar_options);
            android.widget.ArrayAdapter<SidebarOption> adapter = new android.widget.ArrayAdapter<SidebarOption>(getActivity(), R.layout.item_sidebar_option, optionsList) {
                @Override
                public View getView(int position, View convertView, ViewGroup parent) {
                    if (convertView == null) {
                        convertView = LayoutInflater.from(getContext()).inflate(R.layout.item_sidebar_option, parent, false);
                    }
                    SidebarOption option = getItem(position);
                    TextView tv = convertView.findViewById(R.id.tv_option_title);
                    android.widget.ImageView iv = convertView.findViewById(R.id.iv_option_icon);
                    if (option != null) {
                        tv.setText(option.title);
                        iv.setImageResource(option.iconResId);
                    }
                    return convertView;
                }
            };
            lvOptions.setAdapter(adapter);

            lvOptions.setOnItemClickListener((parent, v, position, id) -> {
                dialog.dismiss();
                if (optionsList.get(position).title.equals("History")) {
                    if (isTracking) btnToggle.performClick();
                    if (getActivity() instanceof MainActivity) {
                        ((MainActivity) getActivity()).pushFragment(new WorkoutHistoryFragment());
                    }
                }
            });
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
