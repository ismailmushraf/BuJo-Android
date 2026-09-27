package com.ismailmushraf.bujo.coach;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import org.json.JSONObject;
import java.util.ArrayList;
import java.util.List;

/** Separate private history: never changes the journal or participates in journal exports. */
public final class CoachStore extends SQLiteOpenHelper {
    public CoachStore(Context context) { super(context, "coach_private.db", null, 1); }
    @Override public void onCreate(SQLiteDatabase db) {
        db.execSQL("CREATE TABLE checkins (_id INTEGER PRIMARY KEY AUTOINCREMENT, created INTEGER NOT NULL, input TEXT NOT NULL, response TEXT NOT NULL DEFAULT '', status TEXT NOT NULL)");
        db.execSQL("CREATE TABLE focus_plan (_id INTEGER PRIMARY KEY AUTOINCREMENT, checkin_id INTEGER UNIQUE NOT NULL, day TEXT NOT NULL, plan TEXT NOT NULL, active INTEGER NOT NULL DEFAULT 1)");
    }
    @Override public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        throw new IllegalStateException("An explicit coach migration is required");
    }
    public long insert(JSONObject input, String status) {
        ContentValues values = new ContentValues();
        values.put("created", System.currentTimeMillis());
        values.put("input", input.toString());
        values.put("status", status);
        return getWritableDatabase().insertOrThrow("checkins", null, values);
    }
    public void finish(long id, String response, String status) {
        ContentValues values = new ContentValues();
        values.put("response", response); values.put("status", status);
        getWritableDatabase().update("checkins", values, "_id=?", new String[]{String.valueOf(id)});
    }
    public void recoverInterrupted() {
        ContentValues v = new ContentValues(); v.put("status", "interrupted");
        getWritableDatabase().update("checkins", v, "status=?", new String[]{"pending"});
    }
    public List<Record> recent() {
        List<Record> result = new ArrayList<>();
        try (Cursor c = getReadableDatabase().query("checkins", null, null, null, null, null, "_id DESC", "20")) {
            while (c.moveToNext()) result.add(new Record(c.getLong(c.getColumnIndexOrThrow("_id")),
                    c.getLong(c.getColumnIndexOrThrow("created")), c.getString(c.getColumnIndexOrThrow("input")),
                    c.getString(c.getColumnIndexOrThrow("response")), c.getString(c.getColumnIndexOrThrow("status"))));
        }
        return result;
    }
    public boolean accept(long checkinId, String day, String plan) {
        ContentValues v = new ContentValues(); v.put("checkin_id", checkinId);
        v.put("day", day); v.put("plan", plan);
        return getWritableDatabase().insertWithOnConflict("focus_plan", null, v, SQLiteDatabase.CONFLICT_IGNORE) != -1;
    }
    public String activePlan(String day) {
        try (Cursor c = getReadableDatabase().query("focus_plan", new String[]{"plan"},
                "day=? AND active=1", new String[]{day}, null, null, "_id DESC", "1")) {
            return c.moveToFirst() ? c.getString(0) : "";
        }
    }
    public void undo(String day) {
        getWritableDatabase().execSQL("UPDATE focus_plan SET active=0 WHERE _id=(SELECT MAX(_id) FROM focus_plan WHERE day=? AND active=1)", new Object[]{day});
    }
    public void clear() {
        SQLiteDatabase db = getWritableDatabase(); db.beginTransaction();
        try { db.delete("focus_plan", null, null); db.delete("checkins", null, null); db.setTransactionSuccessful(); }
        finally { db.endTransaction(); }
    }
    public static final class Record {
        public final long id, created;
        public final String input, response, status;
        Record(long id, long created, String input, String response, String status) {
            this.id=id; this.created=created; this.input=input; this.response=response; this.status=status;
        }
    }
}
