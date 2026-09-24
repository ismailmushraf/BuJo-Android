package com.ismailmushraf.bujo.fragments;

import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.ServiceConnection;
import android.os.Bundle;
import android.os.IBinder;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import com.ismailmushraf.bujo.MainActivity;
import com.ismailmushraf.bujo.R;
import com.ismailmushraf.bujo.db.DatabaseManager;
import com.ismailmushraf.bujo.models.Entry;
import com.ismailmushraf.bujo.services.PomodoroService;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

public class PomodoroFragment extends Fragment implements PomodoroService.OnTimerTickListener {

    private TextView tvCountdown, tvModeLabel;
    private Button btnPrimary, btnSecondary, btnSkip;
    private Spinner spinnerTasks, spinnerFocusDuration, spinnerBreakDuration;

    private PomodoroService pomodoroService;
    private boolean isBound = false;
    private DatabaseManager dbManager;

    private final ServiceConnection connection = new ServiceConnection() {
        @Override
        public void onServiceConnected(ComponentName name, IBinder service) {
            PomodoroService.PomodoroBinder binder = (PomodoroService.PomodoroBinder) service;
            pomodoroService = binder.getService();
            isBound = true;
            pomodoroService.setTickListener(PomodoroFragment.this);
            updateUIFromService();
        }

        @Override
        public void onServiceDisconnected(ComponentName name) {
            isBound = false;
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_pomodoro, container, false);

        tvCountdown = root.findViewById(R.id.tv_countdown);
        tvModeLabel = root.findViewById(R.id.tv_mode_label);
        btnPrimary = root.findViewById(R.id.btn_timer_primary);
        btnSecondary = root.findViewById(R.id.btn_timer_secondary);
        btnSkip = root.findViewById(R.id.btn_timer_skip);
        spinnerTasks = root.findViewById(R.id.spinner_tasks);
        spinnerFocusDuration = root.findViewById(R.id.spinner_focus_duration);
        spinnerBreakDuration = root.findViewById(R.id.spinner_break_duration);

        dbManager = new DatabaseManager(getActivity());
        dbManager.open();

        setupTaskSpinner();
        setupDurationSpinners();

        btnPrimary.setOnClickListener(v -> handlePrimaryClick());
        btnSecondary.setOnClickListener(v -> handleSecondaryClick());
        btnSkip.setOnClickListener(v -> handleSkipClick());

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setToolbarTitle("FOCUS TIMER");
            ((MainActivity) getActivity()).setToolbarSubtitle("");
        }

        return root;
    }

    private void setupTaskSpinner() {
        List<Entry> tasks = dbManager.getTodayEntries();
        List<String> taskNames = new ArrayList<>();
        taskNames.add("General Focus Session");
        for (Entry e : tasks) {
            if (!e.isCompleted() && "*".equals(e.getSignifier())) {
                taskNames.add(e.getContent());
            }
        }
        if (getActivity() != null) {
            ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, taskNames);
            adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
            spinnerTasks.setAdapter(adapter);
        }
    }

    private void setupDurationSpinners() {
        if (getActivity() == null) return;

        Integer[] focusValues = {15, 25, 50, 90, 120};
        ArrayAdapter<Integer> focusAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, focusValues);
        focusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerFocusDuration.setAdapter(focusAdapter);
        spinnerFocusDuration.setSelection(1); // Default to 25

        Integer[] breakValues = {5, 10, 15, 30};
        ArrayAdapter<Integer> breakAdapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_item, breakValues);
        breakAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinnerBreakDuration.setAdapter(breakAdapter);
        spinnerBreakDuration.setSelection(0); // Default to 5
    }

    private void handlePrimaryClick() {
        if (!isBound) return;

        if (pomodoroService.isRunning()) {
            pomodoroService.pauseTimer();
        } else if (pomodoroService.isPaused()) {
            pomodoroService.resumeTimer();
        } else {
            boolean isFocus = "FOCUS".equals(tvModeLabel.getText().toString());
            int focusMins = (Integer) spinnerFocusDuration.getSelectedItem();
            int breakMins = (Integer) spinnerBreakDuration.getSelectedItem();
            
            int currentMins = isFocus ? focusMins : breakMins;

            String task = spinnerTasks.getSelectedItem().toString();
            pomodoroService.setDurations(focusMins, breakMins);
            pomodoroService.startTimer(currentMins * 60 * 1000L, isFocus, task);
        }
        updateUIFromService();
    }

    private void handleSecondaryClick() {
        if (!isBound) return;
        pomodoroService.stopTimer();
        updateUIFromService();
    }

    private void handleSkipClick() {
        if (!isBound) return;
        pomodoroService.skipTimer();
        updateUIFromService();
    }

    private void updateUIFromService() {
        if (pomodoroService == null) return;

        boolean isRunning = pomodoroService.isRunning();
        boolean isPaused = pomodoroService.isPaused();

        if (isRunning || isPaused) {
            btnPrimary.setText(isRunning ? "PAUSE" : "RESUME");
            btnSecondary.setEnabled(true);
            btnSecondary.setAlpha(1.0f);
            
            boolean isFocus = pomodoroService.isFocusMode();
            tvModeLabel.setText(isFocus ? "FOCUS" : "BREAK");
            btnSkip.setVisibility(isFocus ? View.GONE : View.VISIBLE);
            onTick(pomodoroService.getTimeLeft(), isFocus);
            
            // Set current task in spinner if not already set correctly
            String task = pomodoroService.getCurrentTask();
            ArrayAdapter adapter = (ArrayAdapter) spinnerTasks.getAdapter();
            if (adapter != null) {
                int pos = adapter.getPosition(task);
                if (pos != -1) {
                    spinnerTasks.setSelection(pos);
                }
            }

            spinnerTasks.setEnabled(false);
            spinnerFocusDuration.setEnabled(false);
            spinnerBreakDuration.setEnabled(false);

            // Sync spinners with service durations
            syncSpinnersWithService();
        } else {
            boolean isFocus = pomodoroService.isFocusMode();
            tvModeLabel.setText(isFocus ? "FOCUS" : "BREAK");
            btnPrimary.setText(isFocus ? "START FOCUS" : "START BREAK");
            btnSecondary.setEnabled(false);
            btnSecondary.setAlpha(0.5f);
            btnSkip.setVisibility(isFocus ? View.GONE : View.VISIBLE);
            
            spinnerTasks.setEnabled(true);
            spinnerFocusDuration.setEnabled(true);
            spinnerBreakDuration.setEnabled(true);
            
            // Reset countdown text from settings if idle
            int mins = isFocus
                    ? (Integer) spinnerFocusDuration.getSelectedItem()
                    : (Integer) spinnerBreakDuration.getSelectedItem();
            tvCountdown.setText(String.format(Locale.US, "%02d:00", mins));
        }
    }

    @SuppressWarnings("unchecked")
    private void syncSpinnersWithService() {
        if (pomodoroService == null) return;
        
        ArrayAdapter<Integer> focusAdapter = (ArrayAdapter<Integer>) spinnerFocusDuration.getAdapter();
        if (focusAdapter != null) {
            int pos = focusAdapter.getPosition(pomodoroService.getFocusDuration());
            if (pos != -1) spinnerFocusDuration.setSelection(pos);
        }

        ArrayAdapter<Integer> breakAdapter = (ArrayAdapter<Integer>) spinnerBreakDuration.getAdapter();
        if (breakAdapter != null) {
            int pos = breakAdapter.getPosition(pomodoroService.getBreakDuration());
            if (pos != -1) spinnerBreakDuration.setSelection(pos);
        }
    }

    @Override
    public void onTick(long millisUntilFinished, boolean isFocus) {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            int minutes = (int) (millisUntilFinished / 1000) / 60;
            int seconds = (int) (millisUntilFinished / 1000) % 60;
            tvCountdown.setText(String.format(Locale.US, "%02d:%02d", minutes, seconds));
            tvModeLabel.setText(isFocus ? "FOCUS" : "BREAK");
        });
    }

    @Override
    public void onFinish() {
        if (getActivity() == null) return;
        getActivity().runOnUiThread(() -> {
            boolean wasFocus = tvModeLabel.getText().toString().equals("FOCUS");
            tvModeLabel.setText(wasFocus ? "BREAK" : "FOCUS");
            Toast.makeText(getActivity(), wasFocus ? "Focus session complete! Time for a break." : "Break over! Ready to focus?", Toast.LENGTH_LONG).show();
            updateUIFromService();
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        Intent intent = new Intent(getActivity(), PomodoroService.class);
        if (getActivity() != null) {
            getActivity().startService(intent); // Ensure service stays alive when unbound
            getActivity().bindService(intent, connection, Context.BIND_AUTO_CREATE);
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (isBound && getActivity() != null) {
            getActivity().unbindService(connection);
            isBound = false;
        }
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (dbManager != null) dbManager.close();
    }
}
