package com.ismailmushraf.bujo.utils;

import android.app.AlertDialog;
import android.app.DatePickerDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.TextView;

import com.ismailmushraf.bujo.db.DatabaseManager;
import com.ismailmushraf.bujo.models.Entry;

import java.util.Calendar;

public class EntryUIHelper {

    // Callback interface so the fragment can reload data when the database changes
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

    public void showContextDialog(final Entry entry) {
        AlertDialog.Builder builder = new AlertDialog.Builder(context);
        builder.setTitle("Options");

        String[] options = {
                "Edit Item",
                "Set Deadline",
                entry.isMigrated() ? "Mark as Not Migrated" : "Migrate to Future List",
                "Delete Item"
        };

        builder.setItems(options, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialog, int which) {
                if (which == 0) {
                    showEditDialog(entry);
                } else if (which == 1) {
                    showDatePicker(entry);
                } else if (which == 2) {
                    boolean migrating = !entry.isMigrated();
                    entry.setMigrated(migrating);
                    if (migrating) {
                        entry.setDeadline(0);
                    }
                    dbManager.updateEntry(entry);
                    listener.onEntryUpdated();
                } else if (which == 3) {
                    dbManager.deleteEntry(entry.getId());
                    listener.onEntryUpdated();
                }
            }
        });
        builder.show();
    }

    private void showEditDialog(final Entry entry) {
        final EditText input = new EditText(context);
        input.setText(entry.getContent());
        input.setSelection(input.length());
        new AlertDialog.Builder(context)
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

    private void showDatePicker(final Entry entry) {
        final Calendar c = Calendar.getInstance();
        if (entry.getDeadline() > 0) {
            c.setTimeInMillis(entry.getDeadline());
        }

        DatePickerDialog datePickerDialog = new DatePickerDialog(context,
                new DatePickerDialog.OnDateSetListener() {
                    @Override
                    public void onDateSet(DatePicker view, int year, int monthOfYear, int dayOfMonth) {
                        Calendar selected = Calendar.getInstance();
                        selected.set(year, monthOfYear, dayOfMonth, 12, 0, 0);
                        entry.setDeadline(selected.getTimeInMillis());
                        dbManager.updateEntry(entry);
                        listener.onEntryUpdated();
                    }
                }, c.get(Calendar.YEAR), c.get(Calendar.MONTH), c.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    public void showEmojiPicker(final EditText targetEditText) {
        final String[] emojis = {
                // --- Core Expressions ---
                "\uD83D\uDE0A", // 😊 (Happy)
                "\uD83D\uDE02", // 😂 (Joy)
                "\u263A", // ☺ (Classic Smile)
                "\u270C", // ✌ (Victory/Peace)
                "\u2665", // ♥ (Heart)

                // --- Productivity & Work ---
                "\uD83D\uDCDD", // 📝 (Memo/Log)
                "\uD83D\uDCCC", // 📌 (Pushpin)
                "\uD83D\uDCC5", // 📅 (Calendar)
                "\uD83D\uDCA1", // 💡 (Idea/Lightbulb)
                "\uD83D\uDCBB", // 💻 (Laptop)
                "\u2714", // ✔ (Check)
                "\u2716", // ✖ (Cross)
                "\u2757", // ❗ (Exclamation/Important)
                "\u2753", // ❓ (Question)
                "\u2B50", // ⭐ (Star)
                "\u2705", // ✅ (Checkmark)
                "\u274C", // ❌ (Cross out)
                "\u23F0", // ⏰ (Alarm clock)

                // --- Study & Learning ---
                "\uD83D\uDCDA", // 📚 (Books)
                "\uD83C\uDFAF", // 🎯 (Target/Goals)
                "\u270F\uFE0F", // ✏️ (Pencil)
                "\u270D", // ✍ (Writing Hand)
                "\u2709", // ✉ (Envelope/Mail)
                "\u260E", // ☎ (Phone/Call)

                // --- Fitness & Energy ---
                "\uD83D\uDCAA", // 💪 (Flex/Strength)
                "\uD83C\uDFC3", // 🏃 (Runner/Cardio)
                "\uD83D\uDEB2", // 🚲 (Bicycle)
                "\uD83C\uDFC6", // 🏆 (Trophy/Milestone)
                "\uD83D\uDD25", // 🔥 (Streak/Fire)

                // --- Time & Planning ---
                "\u231A", // ⌚ (Watch)
                "\u23F3", // ⏳ (Hourglass)
                "\u2605", // ★ (Solid Star - Priority)
                "\u2606", // ☆ (Outline Star)

                // --- Environment & Misc ---
                "\u26A1", // ⚡ (Lightning/Energy)
                "\u26BD", // ⚽ (Soccer/Sports)
                "\u26F3", // ⛳ (Golf/Flag/Milestone)
                "\u2600", // ☀ (Sun/Morning)
                "\u2601", // ☁ (Cloud)
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

        AlertDialog.Builder builder = new AlertDialog.Builder(context);
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
}