package com.ismailmushraf.bujo.fragments;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.Environment;
import android.preference.PreferenceManager;
import android.support.v4.app.Fragment;
import android.support.v7.app.AppCompatDelegate;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.RadioGroup;
import android.widget.Spinner;
import android.widget.Toast;

import com.ismailmushraf.bujo.MainActivity;
import com.ismailmushraf.bujo.R;
import com.ismailmushraf.bujo.db.DatabaseManager;

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

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View root = inflater.inflate(R.layout.fragment_settings, container, false);

        if (getActivity() instanceof MainActivity) {
            ((MainActivity) getActivity()).setToolbarTitle("SETTINGS");
            ((MainActivity) getActivity()).setToolbarSubtitle("");
        }

        prefs = PreferenceManager.getDefaultSharedPreferences(getActivity());

        // --- 1. Theme Configuration ---
        RadioGroup rgTheme = (RadioGroup) root.findViewById(R.id.rg_theme);
        int currentTheme = prefs.getInt("theme_mode", AppCompatDelegate.MODE_NIGHT_AUTO);

        if (currentTheme == AppCompatDelegate.MODE_NIGHT_NO) {
            rgTheme.check(R.id.rb_theme_light);
        } else if (currentTheme == AppCompatDelegate.MODE_NIGHT_YES) {
            rgTheme.check(R.id.rb_theme_dark);
        } else {
            rgTheme.check(R.id.rb_theme_auto);
        }

        rgTheme.setOnCheckedChangeListener(new RadioGroup.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(RadioGroup group, int checkedId) {
                int mode = AppCompatDelegate.MODE_NIGHT_AUTO;
                if (checkedId == R.id.rb_theme_light) {
                    mode = AppCompatDelegate.MODE_NIGHT_NO;
                } else if (checkedId == R.id.rb_theme_dark) {
                    mode = AppCompatDelegate.MODE_NIGHT_YES;
                }

                prefs.edit().putInt("theme_mode", mode).apply();
                AppCompatDelegate.setDefaultNightMode(mode);
                if (getActivity() != null) {
                    getActivity().recreate();
                }
            }
        });

        // --- 2. Startup Screen Configuration ---
        Spinner spinnerStartup = (Spinner) root.findViewById(R.id.spinner_startup);
        final String[] screens = {"Inbox", "Today", "Calendar"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getActivity(), android.R.layout.simple_spinner_dropdown_item, screens);
        spinnerStartup.setAdapter(adapter);

        String currentStartup = prefs.getString("startup_screen", "Today");
        if ("Inbox".equals(currentStartup)) {
            spinnerStartup.setSelection(0);
        } else if ("Calendar".equals(currentStartup)) {
            spinnerStartup.setSelection(2);
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

        // --- 3. Backup & Restore Configuration ---
        Button btnExport = (Button) root.findViewById(R.id.btn_export_backup);
        btnExport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File rootDir = Environment.getExternalStorageDirectory();
                showExportFolderChooser(rootDir);
            }
        });

        Button btnImport = (Button) root.findViewById(R.id.btn_import_backup);
        btnImport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                File rootDir = Environment.getExternalStorageDirectory();
                showImportFileChooser(rootDir);
            }
        });

        // --- 4. Clear Completed Tasks ---
        Button btnClear = (Button) root.findViewById(R.id.btn_clear_completed);
        btnClear.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(getActivity())
                        .setTitle("Clear Completed Tasks")
                        .setMessage("Are you sure you want to permanently delete all completed tasks across all projects and logs?")
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

    // --- FOLDER SELECTION DIALOG FOR EXPORT ---
    private void showExportFolderChooser(final File currentDir) {
        File[] files = currentDir.listFiles();
        List<String> folderNames = new ArrayList<>();
        final List<File> folderFiles = new ArrayList<>();

        folderNames.add("✔ [SAVE IN THIS FOLDER]");
        folderFiles.add(currentDir);

        if (currentDir.getParentFile() != null && currentDir.getParentFile().canRead()) {
            folderNames.add(".. (Go Up)");
            folderFiles.add(currentDir.getParentFile());
        }

        if (files != null) {
            List<File> subDirs = new ArrayList<>();
            for (File f : files) {
                if (f.isDirectory() && f.canRead()) {
                    subDirs.add(f);
                }
            }
            Collections.sort(subDirs);
            for (File dir : subDirs) {
                folderNames.add("/ " + dir.getName());
                folderFiles.add(dir);
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Select Export Location:\n" + currentDir.getAbsolutePath());
        builder.setItems(folderNames.toArray(new String[0]), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    performExport(currentDir);
                } else if (folderNames.get(which).equals(".. (Go Up)")) {
                    showExportFolderChooser(folderFiles.get(which));
                } else {
                    showExportFolderChooser(folderFiles.get(which));
                }
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.show();
    }

    // --- FILE SELECTION DIALOG FOR IMPORT ---
    private void showImportFileChooser(final File currentDir) {
        File[] files = currentDir.listFiles();
        List<String> displayNames = new ArrayList<>();
        final List<File> targetFiles = new ArrayList<>();

        if (currentDir.getParentFile() != null && currentDir.getParentFile().canRead()) {
            displayNames.add(".. (Go Up)");
            targetFiles.add(currentDir.getParentFile());
        }

        if (files != null) {
            List<File> subDirs = new ArrayList<>();
            List<File> dbFiles = new ArrayList<>();

            for (File f : files) {
                if (f.isDirectory() && f.canRead()) {
                    subDirs.add(f);
                } else if (f.isFile() && (f.getName().endsWith(".db") || f.getName().endsWith(".bak"))) {
                    dbFiles.add(f);
                }
            }

            Collections.sort(subDirs);
            Collections.sort(dbFiles);

            for (File dir : subDirs) {
                displayNames.add("/ " + dir.getName());
                targetFiles.add(dir);
            }
            for (File dbFile : dbFiles) {
                displayNames.add("📄 " + dbFile.getName());
                targetFiles.add(dbFile);
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        builder.setTitle("Select Backup File:\n" + currentDir.getAbsolutePath());
        builder.setItems(displayNames.toArray(new String[0]), new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                File selected = targetFiles.get(which);
                if (selected.isDirectory()) {
                    showImportFileChooser(selected);
                } else {
                    confirmAndImport(selected);
                }
            }
        });
        builder.setNegativeButton(android.R.string.cancel, null);
        builder.show();
    }

    private void confirmAndImport(final File selectedDbFile) {
        new AlertDialog.Builder(getActivity())
                .setTitle("Restore Backup")
                .setMessage("Restore from " + selectedDbFile.getName() + "? This will overwrite your current journal completely.")
                .setPositiveButton("Restore", new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        performImport(selectedDbFile);
                    }
                })
                .setNegativeButton(android.R.string.cancel, null)
                .show();
    }

    private void performExport(File targetFolder) {
        try {
            File currentDB = getActivity().getDatabasePath("bujo.db");
            File backupFile = new File(targetFolder, "BuJo_Backup.db");

            if (currentDB.exists()) {
                copyFile(currentDB, backupFile);
                Toast.makeText(getActivity(), "Exported to: " + backupFile.getAbsolutePath(), Toast.LENGTH_LONG).show();
            } else {
                Toast.makeText(getActivity(), "Database not found!", Toast.LENGTH_SHORT).show();
            }
        } catch (Exception e) {
            Toast.makeText(getActivity(), "Export failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void performImport(File backupFile) {
        try {
            File currentDB = getActivity().getDatabasePath("bujo.db");

            // 1. Close open connections to prevent SQLite disk locking crash
            DatabaseManager dbManager = new DatabaseManager(getActivity());
            dbManager.close();

            // 2. Clean up temporary journal & WAL files before replacing the DB file
            File journal = new File(currentDB.getPath() + "-journal");
            File wal = new File(currentDB.getPath() + "-wal");
            File shm = new File(currentDB.getPath() + "-shm");
            if (journal.exists()) journal.delete();
            if (wal.exists()) wal.delete();
            if (shm.exists()) shm.delete();

            // 3. Overwrite current DB with backup
            copyFile(backupFile, currentDB);

            Toast.makeText(getActivity(), "Restore successful! Restarting...", Toast.LENGTH_SHORT).show();

            // 4. Clean restart of the main activity
            if (getActivity() != null) {
                Intent intent = getActivity().getBaseContext().getPackageManager()
                        .getLaunchIntentForPackage(getActivity().getBaseContext().getPackageName());
                if (intent != null) {
                    intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                }
                getActivity().finish();
            }
        } catch (Exception e) {
            Toast.makeText(getActivity(), "Restore failed: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    private void copyFile(File src, File dst) throws IOException {
        FileInputStream inStream = new FileInputStream(src);
        FileOutputStream outStream = new FileOutputStream(dst);
        FileChannel inChannel = inStream.getChannel();
        FileChannel outChannel = outStream.getChannel();
        try {
            inChannel.transferTo(0, inChannel.size(), outChannel);
        } finally {
            inStream.close();
            outStream.close();
        }
    }
}