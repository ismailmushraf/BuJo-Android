package com.ismailmushraf.bujo;

import android.content.Context;
import android.content.ContextWrapper;
import android.database.sqlite.SQLiteDatabase;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import androidx.test.platform.app.InstrumentationRegistry;
import com.ismailmushraf.bujo.coach.CoachStore;
import org.json.JSONObject;
import org.junit.Test;
import org.junit.runner.RunWith;
import java.io.File;
import static org.junit.Assert.*;

@RunWith(AndroidJUnit4.class)
public class CoachStoreTest {
    @Test public void historySurvivesFailureAndPlansUndoWithoutDuplicates() throws Exception {
        Context app=InstrumentationRegistry.getInstrumentation().getTargetContext();
        File temporary=File.createTempFile("coach-test-",".db",app.getCacheDir());
        Context isolated=new ContextWrapper(app) {
            @Override public File getDatabasePath(String name) { return temporary; }
            @Override public SQLiteDatabase openOrCreateDatabase(String name,int mode,SQLiteDatabase.CursorFactory factory,
                    android.database.DatabaseErrorHandler errorHandler) {
                return SQLiteDatabase.openOrCreateDatabase(temporary,null);
            }
        };
        try(CoachStore store=new CoachStore(isolated)) {
            long id=store.insert(new JSONObject().put("mood","Low"),"pending");
            store.recoverInterrupted();
            assertEquals("interrupted",store.recent().get(0).status);
            store.finish(id,"offline advice","failed");
            assertEquals("Low",new JSONObject(store.recent().get(0).input).getString("mood"));
            assertTrue(store.accept(id,"2026-09-27","First plan"));
            assertFalse(store.accept(id,"2026-09-27","Duplicate"));
            long second=store.insert(new JSONObject(),"local");
            assertTrue(store.accept(second,"2026-09-27","Second plan"));
            assertEquals("Second plan",store.activePlan("2026-09-27"));
            store.undo("2026-09-27");
            assertEquals("First plan",store.activePlan("2026-09-27"));
            assertEquals("",store.activePlan("2026-09-28"));
            store.clear();
            assertTrue(store.recent().isEmpty());
        } finally { SQLiteDatabase.deleteDatabase(temporary); }
    }
}
