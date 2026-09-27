package com.ismailmushraf.bujo.db;

import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/** Copies rows within SQLite transactions; never replaces a live database file. */
public final class JournalBackup {
    private JournalBackup() {}

    public static void transfer(Context context, File snapshot, boolean restore) throws IOException {
        DatabaseHelper helper = new DatabaseHelper(context.getApplicationContext());
        SQLiteDatabase db = helper.getWritableDatabase();
        boolean attached = false;
        try {
            if (restore) {
                try (SQLiteDatabase source = SQLiteDatabase.openDatabase(snapshot.getPath(), null,
                        SQLiteDatabase.OPEN_READWRITE);
                     Cursor check = source.rawQuery("PRAGMA integrity_check", null)) {
                    if (!check.moveToFirst() || !"ok".equals(check.getString(0))
                            || source.getVersion() < 1 || source.getVersion() > db.getVersion()) {
                        throw new IOException("Backup is damaged or belongs to a different database version.");
                    }
                    if (source.getVersion() < db.getVersion()) {
                        // Migrate the private temporary copy; leave the selected document untouched.
                        source.beginTransaction();
                        try {
                            helper.onUpgrade(source, source.getVersion(), db.getVersion());
                            source.setVersion(db.getVersion());
                            source.setTransactionSuccessful();
                        } finally {
                            source.endTransaction();
                        }
                    }
                }
            }
            db.execSQL("ATTACH DATABASE ? AS journal_backup", new Object[]{snapshot.getPath()});
            attached = true;
            List<String[]> tables = new ArrayList<>();
            try (Cursor cursor = db.rawQuery("SELECT name, sql FROM main.sqlite_master "
                    + "WHERE type='table' AND name NOT LIKE 'sqlite_%' AND name != 'android_metadata'", null)) {
                while (cursor.moveToNext()) tables.add(new String[]{cursor.getString(0), cursor.getString(1)});
            }
            db.beginTransaction();
            try {
                for (String[] table : tables) {
                    String name = quote(table[0]);
                    String columns = columns(db, "main", table[0]);
                    if (restore) {
                        if (!columns.equals(columns(db, "journal_backup", table[0]))) {
                            throw new IOException("Backup schema does not match this app.");
                        }
                        db.execSQL("DELETE FROM main." + name);
                        db.execSQL("INSERT INTO main." + name + " (" + columns + ") SELECT "
                                + columns + " FROM journal_backup." + name);
                    } else {
                        // Schema comes only from our own database, never from an imported file.
                        String definition = table[1].substring(table[1].indexOf('('));
                        db.execSQL("CREATE TABLE journal_backup." + name + " " + definition);
                        db.execSQL("INSERT INTO journal_backup." + name + " SELECT * FROM main." + name);
                    }
                }
                if (!restore) db.execSQL("PRAGMA journal_backup.user_version=" + db.getVersion());
                db.setTransactionSuccessful();
            } finally {
                db.endTransaction();
            }
        } finally {
            if (attached) db.execSQL("DETACH DATABASE journal_backup");
            helper.close();
        }
    }

    private static String columns(SQLiteDatabase db, String schema, String table) {
        List<String> names = new ArrayList<>();
        try (Cursor cursor = db.rawQuery("PRAGMA " + schema + ".table_info(" + quote(table) + ")", null)) {
            while (cursor.moveToNext()) names.add(quote(cursor.getString(1)));
        }
        java.util.Collections.sort(names);
        return android.text.TextUtils.join(",", names);
    }

    private static String quote(String name) { return "\"" + name.replace("\"", "\"\"") + "\""; }
}
