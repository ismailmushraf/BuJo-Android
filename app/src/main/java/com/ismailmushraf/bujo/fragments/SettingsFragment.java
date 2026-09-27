package com.ismailmushraf.bujo.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import androidx.fragment.app.Fragment;
import androidx.appcompat.app.AppCompatDelegate;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import com.ismailmushraf.bujo.MainActivity;
import com.ismailmushraf.bujo.R;
import com.ismailmushraf.bujo.db.DatabaseManager;
import com.ismailmushraf.bujo.coach.CoachApiKey;
import com.ismailmushraf.bujo.coach.CoachEngine;
import com.ismailmushraf.bujo.coach.CoachPreferences;
import com.ismailmushraf.bujo.coach.CoachScheduler;
import com.ismailmushraf.bujo.utils.BB10DialogHelper;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.channels.FileChannel;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public class SettingsFragment extends Fragment {

    private SharedPreferences prefs;
    private boolean isSpinnerInitialized = false;
    private CoachEngine coachEngine;
    private Runnable coachListener;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_settings, container, false);
        bindCoachSettings(root);

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setToolbarTitle("SETTINGS");
            ((MainActivity) getActivity()).setToolbarSubtitle("");
        }

        prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        // --- 1. Theme Configuration (BB10 Dropdown) ---
        Spinner spinnerTheme = (Spinner) root.findViewById(R.id.spinner_theme);
        final String[] themes = {"Light Theme", "Dark Theme", "Auto"};
        ArrayAdapter<String> themeAdapter = new ArrayAdapter<>(getActivity(), R.layout.item_bb10_spinner, themes);
        themeAdapter.setDropDownViewResource(R.layout.item_bb10_spinner_dropdown);
        spinnerTheme.setAdapter(themeAdapter);

        int currentTheme = prefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_AUTO);
        if (currentTheme == AppCompatDelegate.MODE_NIGHT_NO) {
            spinnerTheme.setSelection(0);
        } else if (currentTheme == AppCompatDelegate.MODE_NIGHT_YES) {
            spinnerTheme.setSelection(1);
        } else {
            spinnerTheme.setSelection(2);
        }

        spinnerTheme.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            private boolean initialized = false;

            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (!initialized) {
                    initialized = true;
                    return;
                }
                int mode = AppCompatDelegate.MODE_NIGHT_AUTO;
                if (position == 0) {
                    mode = AppCompatDelegate.MODE_NIGHT_NO;
                } else if (position == 1) {
                    mode = AppCompatDelegate.MODE_NIGHT_YES;
                }

                if (mode != prefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_AUTO)) {
                    prefs.edit().putInt("theme_mode", mode).apply();
                    AppCompatDelegate.setDefaultNightMode(mode);
                    if (getActivity() != null) getActivity().recreate();
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // --- 2. Startup Screen Configuration (BB10 Dropdown) ---
        Spinner spinnerStartup = (Spinner) root.findViewById(R.id.spinner_startup);
        final String[] screens = {"Inbox", "Today"};
        ArrayAdapter<String> startupAdapter = new ArrayAdapter<>(getActivity(), R.layout.item_bb10_spinner, screens);
        startupAdapter.setDropDownViewResource(R.layout.item_bb10_spinner_dropdown);
        spinnerStartup.setAdapter(startupAdapter);

        String currentStartup = prefs.getString("startup_screen", "Today");
        if ("Inbox".equals(currentStartup)) {
            spinnerStartup.setSelection(0);
        } else {
            spinnerStartup.setSelection(1);
        }

        spinnerStartup.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (isSpinnerInitialized) {
                    prefs.edit().putString("startup_screen", screens[position]).apply();
                }
                isSpinnerInitialized = true;
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        // --- 3. Gamification Configuration ---
        CheckBox cbSound = (CheckBox) root.findViewById(R.id.cb_sound_effects);
        CheckBox cbAnim = (CheckBox) root.findViewById(R.id.cb_points_animation);

        cbSound.setChecked(prefs.getBoolean("enable_sounds", true));
        cbAnim.setChecked(prefs.getBoolean("enable_animations", true));

        cbSound.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.edit().putBoolean("enable_sounds", isChecked).apply();
            }
        });

        cbAnim.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                prefs.edit().putBoolean("enable_animations", isChecked).apply();
            }
        });

        // --- 4. Backup & Restore Configuration ---
        View btnExport = root.findViewById(R.id.btn_export_backup);
        btnExport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseBackup(false);
            }
        });

        View btnImport = root.findViewById(R.id.btn_import_backup);
        btnImport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                chooseBackup(true);
            }
        });

        // --- 4. Clear Completed Tasks ---
        View btnClear = root.findViewById(R.id.btn_clear_completed);
        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(getActivity(), R.style.BujoDialog)
                        .setTitle(getString(com.ismailmushraf.bujo.R.string.ui_clear_completed_tasks_2ee9aa))
                        .setMessage(getString(com.ismailmushraf.bujo.R.string.ui_are_you_sure_you_want_to_permanently_delete_all__ea6d98))
                        .setPositiveButton("Delete", new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                DatabaseManager db = new DatabaseManager(getActivity());
                                db.open();
                                int deletedCount = db.clearCompletedTasks();
                                db.close();
                                Toast.makeText(getActivity(), deletedCount + " tasks cleared", Toast.LENGTH_SHORT).show();
                            }
                        })
                        .setNegativeButton(android.R.string.cancel, null)
                        .show();
            }
        });

        return root;
    }

    private void bindCoachSettings(View root) {
        final CoachEngine coach=CoachEngine.get(requireContext());
        coachEngine=coach;
        final SharedPreferences coachPrefs=CoachPreferences.get(requireContext());
        EditText key=root.findViewById(R.id.coach_api_key), model=root.findViewById(R.id.coach_model);
        EditText start=root.findViewById(R.id.coach_start), end=root.findViewById(R.id.coach_end);
        key.setText(coachPrefs.getString("key", ""));
        key.setTransformationMethod(android.text.method.PasswordTransformationMethod.getInstance());
        model.setText(CoachPreferences.model(requireContext())); start.setText(String.valueOf(coachPrefs.getInt("start",8))); end.setText(String.valueOf(coachPrefs.getInt("end",20)));
        int[] ids={R.id.coach_consent,R.id.coach_share_tasks,R.id.coach_share_habits,R.id.coach_enabled}; String[] names={"consent","share_tasks","share_habits","enabled"};
        for(int i=0;i<ids.length;i++) ((CheckBox)root.findViewById(ids[i])).setChecked(coachPrefs.getBoolean(names[i],false));
        View.OnClickListener save=v -> {
            String clean=CoachApiKey.normalize(key.getText().toString());
            if(!CoachApiKey.valid(clean)){ key.setError(getString(R.string.coach_invalid_key)); return; }
            String selectedModel=model.getText().toString().trim();
            if(!CoachPreferences.validModel(selectedModel)){ model.setError("Enter a valid Gemini model ID."); return; }
            try { int from=Integer.parseInt(start.getText().toString()), to=Integer.parseInt(end.getText().toString()); if(from<0||to>23||from>=to) throw new IllegalArgumentException(); SharedPreferences.Editor edit=coachPrefs.edit().putString("key",clean).putString("model",selectedModel).putInt("start",from).putInt("end",to).remove("snooze"); for(int i=0;i<ids.length;i++) edit.putBoolean(names[i],((CheckBox)root.findViewById(ids[i])).isChecked()); edit.apply(); key.setText(clean); CoachScheduler.schedule(requireContext()); setCoachStatus(root,getString(R.string.coach_saved)); } catch(Exception error) { end.setError(getString(R.string.coach_invalid_settings)); }
        };
        root.findViewById(R.id.coach_save_settings).setOnClickListener(save);
        root.findViewById(R.id.coach_test).setOnClickListener(v -> {
            // A connection test must not depend on optional reminder hours or sharing choices.
            String clean=CoachApiKey.normalize(key.getText().toString());
            if(!CoachApiKey.valid(clean)) { key.setError(getString(R.string.coach_invalid_key)); return; }
            String selectedModel=model.getText().toString().trim();
            if(!CoachPreferences.validModel(selectedModel)) { model.setError("Enter a valid Gemini model ID."); return; }
            coachPrefs.edit().putString("key",clean).putString("model",selectedModel).apply();
            key.setText(clean);
            setCoachStatus(root,getString(R.string.coach_working));
            coach.testConnection();
        });
        root.findViewById(R.id.coach_forget).setOnClickListener(v -> { coachPrefs.edit().remove("key").putBoolean("consent",false).apply(); key.setText(""); ((CheckBox)root.findViewById(R.id.coach_consent)).setChecked(false); });
        root.findViewById(R.id.coach_delete).setOnClickListener(v -> BB10DialogHelper.showConfirmDialog(requireContext(),getString(R.string.coach_delete),getString(R.string.coach_delete_confirm),getString(R.string.coach_delete),()->coach.clear(true)));
        coachListener=() -> {
            View screen=getView();
            if(screen==null || !isAdded()) return;
            android.widget.TextView status=screen.findViewById(R.id.coach_settings_status);
            if(status!=null) { status.setText(coach.notice()); status.setVisibility(View.VISIBLE); }
        };
        coach.listen(coachListener);
    }

    private void setCoachStatus(View root, String message) {
        android.widget.TextView status=root.findViewById(R.id.coach_settings_status);
        if(status!=null) { status.setText(message); status.setVisibility(View.VISIBLE); }
    }

    @Override public void onDestroyView() {
        if(coachEngine!=null && coachListener!=null) coachEngine.unlisten(coachListener);
        coachListener=null;
        coachEngine=null;
        super.onDestroyView();
    }

    private static final int EXPORT_BACKUP = 401;
    private static final int IMPORT_BACKUP = 402;

    private void chooseBackup(boolean restore) {
        if (android.os.Build.VERSION.SDK_INT < 19) {
            // Android 4.3 predates the system document creator. Keep its file chooser.
            if (restore) showLegacyFiles(Environment.getExternalStorageDirectory(), true);
            else showLegacyFiles(Environment.getExternalStorageDirectory(), false);
            return;
        }
        Intent intent = new Intent(restore ? Intent.ACTION_OPEN_DOCUMENT : Intent.ACTION_CREATE_DOCUMENT);
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.setType(restore ? "*/*" : "application/octet-stream");
        if (!restore) intent.putExtra(Intent.EXTRA_TITLE, "BuJo_Backup.db");
        try {
            startActivityForResult(intent, restore ? IMPORT_BACKUP : EXPORT_BACKUP);
        } catch (android.content.ActivityNotFoundException exception) {
            Toast.makeText(getContext(), "No document picker is available.", Toast.LENGTH_LONG).show();
        }
    }

    private void showLegacyFiles(File folder, boolean restore) {
        List<File> targets = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        if (!restore) { targets.add(new File(folder, "BuJo_Backup.db")); labels.add("Save here"); }
        if (folder.getParentFile() != null && folder.getParentFile().canRead()) {
            targets.add(folder.getParentFile()); labels.add("Go up");
        }
        File[] files = folder.listFiles();
        if (files != null) {
            java.util.Arrays.sort(files);
            for (File file : files) {
                if (file.isDirectory() || (restore && (file.getName().endsWith(".db") || file.getName().endsWith(".bak")))) {
                    targets.add(file); labels.add(file.getName());
                }
            }
        }
        new AlertDialog.Builder(requireContext(), R.style.BujoDialog)
                .setTitle(folder.getPath())
                .setItems(labels.toArray(new String[0]), (dialog, index) -> {
                    File chosen = targets.get(index);
                    if (chosen.isDirectory()) showLegacyFiles(chosen, restore);
                    else confirmTransfer(android.net.Uri.fromFile(chosen), restore);
                }).setNegativeButton(android.R.string.cancel, null).show();
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if ((requestCode == IMPORT_BACKUP || requestCode == EXPORT_BACKUP)
                && resultCode == android.app.Activity.RESULT_OK && data != null && data.getData() != null) {
            confirmTransfer(data.getData(), requestCode == IMPORT_BACKUP);
        }
    }

    private void confirmTransfer(android.net.Uri uri, boolean restore) {
        if (!restore) { transferBackup(uri, false); return; }
        new AlertDialog.Builder(requireContext(), R.style.BujoDialog)
                .setTitle(getString(com.ismailmushraf.bujo.R.string.ui_restore_backup_cb51e4))
                .setMessage(getString(com.ismailmushraf.bujo.R.string.ui_replace_your_current_journal_with_this_backup_f45514))
                .setPositiveButton("Restore", (dialog, which) -> transferBackup(uri, true))
                .setNegativeButton(android.R.string.cancel, null).show();
    }

    private void transferBackup(android.net.Uri uri, boolean restore) {
        final android.content.Context context = requireContext().getApplicationContext();
        com.ismailmushraf.bujo.utils.AppExecutors.getInstance().diskIO().execute(() -> {
            File temporary = null;
            String message;
            boolean success = false;
            try {
                temporary = File.createTempFile("journal-backup-", ".db", context.getCacheDir());
                if (restore) {
                    try (java.io.InputStream input = context.getContentResolver().openInputStream(uri);
                         java.io.OutputStream output = new FileOutputStream(temporary)) {
                        copy(input, output);
                    }
                }
                com.ismailmushraf.bujo.db.JournalBackup.transfer(context, temporary, restore);
                if (!restore) {
                    try (java.io.InputStream input = new FileInputStream(temporary);
                         java.io.OutputStream output = context.getContentResolver().openOutputStream(uri, "wt")) {
                        copy(input, output);
                    }
                }
                success = true;
                message = restore ? "Backup restored." : "Backup exported.";
            } catch (Exception exception) {
                message = "Backup failed: " + exception.getMessage();
            } finally {
                if (temporary != null) temporary.delete();
            }
            final String result = message;
            final boolean restored = restore && success;
            com.ismailmushraf.bujo.utils.AppExecutors.getInstance().mainThread().execute(() -> {
                Toast.makeText(context, result, Toast.LENGTH_LONG).show();
                if (restored && isAdded()) {
                    Intent restart = new Intent(context, MainActivity.class);
                    restart.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(restart);
                }
            });
        });
    }

    private static void copy(java.io.InputStream input, java.io.OutputStream output) throws IOException {
        if (input == null || output == null) throw new IOException("Cannot open the selected document.");
        byte[] buffer = new byte[8192];
        int count;
        while ((count = input.read(buffer)) != -1) output.write(buffer, 0, count);
        output.flush();
    }
}
