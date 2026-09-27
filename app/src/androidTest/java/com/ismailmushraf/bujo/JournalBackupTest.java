package com.ismailmushraf.bujo;

import android.content.Context;
import android.content.ContextWrapper;
import android.database.DatabaseErrorHandler;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.platform.app.InstrumentationRegistry;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import com.ismailmushraf.bujo.db.DatabaseHelper;
import com.ismailmushraf.bujo.db.JournalBackup;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class JournalBackupTest {
    @Test public void roundTripAndInvalidRestorePreserveLiveConnection() throws Exception {
        Context base = InstrumentationRegistry.getInstrumentation().getTargetContext();
        File directory = new File(base.getCacheDir(), "backup-test-" + System.nanoTime());
        assertTrue(directory.mkdir());
        Context isolated = new ContextWrapper(base) {
            @Override public Context getApplicationContext() { return this; }
            @Override public File getDatabasePath(String name) { return new File(directory, name); }
            @Override public SQLiteDatabase openOrCreateDatabase(String name, int mode,
                    SQLiteDatabase.CursorFactory factory) {
                return SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name), factory);
            }
            @Override public SQLiteDatabase openOrCreateDatabase(String name, int mode,
                    SQLiteDatabase.CursorFactory factory, DatabaseErrorHandler handler) {
                return SQLiteDatabase.openOrCreateDatabase(getDatabasePath(name).getPath(), factory, handler);
            }
        };
        File snapshot = new File(directory, "snapshot.db");
        DatabaseHelper helper = new DatabaseHelper(isolated);
        try {
            SQLiteDatabase live = helper.getWritableDatabase();
            live.execSQL("INSERT INTO entries(type, content) VALUES ('*', 'Original task')");
            JournalBackup.transfer(isolated, snapshot, false);
            live.execSQL("UPDATE entries SET content='Changed task'");
            JournalBackup.transfer(isolated, snapshot, true);
            assertEquals("Original task", android.database.DatabaseUtils.stringForQuery(live,
                    "SELECT content FROM entries LIMIT 1", null));
            try (SQLiteDatabase legacy = SQLiteDatabase.openDatabase(snapshot.getPath(), null, 0)) {
                legacy.execSQL("ALTER TABLE projects RENAME TO previous_projects");
                legacy.execSQL("CREATE TABLE projects AS SELECT _id, name, weight, created_at FROM previous_projects");
                legacy.execSQL("DROP TABLE previous_projects");
                legacy.setVersion(12);
            }
            JournalBackup.transfer(isolated, snapshot, true);
            assertEquals("Original task", android.database.DatabaseUtils.stringForQuery(live,
                    "SELECT content FROM entries LIMIT 1", null));
            try (SQLiteDatabase broken = SQLiteDatabase.openDatabase(snapshot.getPath(), null, 0)) {
                broken.execSQL("DROP TABLE projects");
            }
            live.execSQL("UPDATE entries SET content='Must survive rollback'");
            try {
                JournalBackup.transfer(isolated, snapshot, true);
                fail("Invalid schema must be rejected");
            } catch (java.io.IOException expected) {
                assertEquals("Must survive rollback", android.database.DatabaseUtils.stringForQuery(live,
                        "SELECT content FROM entries LIMIT 1", null));
            }
        } finally {
            helper.close();
            File[] files = directory.listFiles();
            if (files != null) for (File file : files) file.delete();
            directory.delete();
        }
    }
}
