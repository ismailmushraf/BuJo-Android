package com.ismailmushraf.bujo.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "bujo.db";
    private static final int DATABASE_VERSION = 1; // Fresh start, set to 1

    // --- ENTRIES TABLE ---
    public static final String TABLE_ENTRIES = "entries";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_TYPE = "type"; // "*", "-", "o"
    public static final String COLUMN_CONTENT = "content";
    public static final String COLUMN_CONTEXT = "context";
    public static final String COLUMN_COMPLETED = "completed";
    public static final String COLUMN_MIGRATED = "migrated";
    public static final String COLUMN_DEADLINE = "deadline";
    public static final String COLUMN_PROJECT_ID = "project_id";
    public static final String COLUMN_HAS_TIME = "has_time";

    // --- PROJECTS TABLE ---
    public static final String TABLE_PROJECTS = "projects";
    public static final String COLUMN_PROJECT_NAME = "name";

    // --- WORKOUT SETS TABLE ---
    public static final String TABLE_WORKOUT_SETS = "workout_sets";
    public static final String COLUMN_DATE_STR = "date_str";
    public static final String COLUMN_EXERCISE = "exercise";
    public static final String COLUMN_WEIGHT = "weight";
    public static final String COLUMN_REPS = "reps";
    public static final String COLUMN_NOTE = "note";

    // --- WORKOUT SESSIONS TABLE (For the timer) ---
    public static final String TABLE_SESSIONS = "workout_sessions";
    public static final String COLUMN_DURATION = "duration_ms";

    // --- TABLE CREATION STRINGS ---
    private static final String TABLE_CREATE_ENTRIES =
            "CREATE TABLE " + TABLE_ENTRIES + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_TYPE + " TEXT, " +
                    COLUMN_CONTENT + " TEXT, " +
                    COLUMN_CONTEXT + " TEXT, " +
                    COLUMN_COMPLETED + " INTEGER DEFAULT 0, " +
                    COLUMN_MIGRATED + " INTEGER DEFAULT 0, " +
                    COLUMN_DEADLINE + " INTEGER, " +
                    COLUMN_PROJECT_ID + " INTEGER, " +
                    COLUMN_HAS_TIME + " INTEGER DEFAULT 0" +
                    ");";

    private static final String TABLE_CREATE_PROJECTS =
            "CREATE TABLE " + TABLE_PROJECTS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_PROJECT_NAME + " TEXT" +
                    ");";

    private static final String TABLE_CREATE_WORKOUT_SETS =
            "CREATE TABLE " + TABLE_WORKOUT_SETS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_DATE_STR + " TEXT, " +
                    COLUMN_EXERCISE + " TEXT, " +
                    COLUMN_WEIGHT + " REAL, " +
                    COLUMN_REPS + " INTEGER, " +
                    COLUMN_NOTE + " TEXT" +
                    ");";

    private static final String TABLE_CREATE_SESSIONS =
            "CREATE TABLE " + TABLE_SESSIONS + " (" +
                    COLUMN_DATE_STR + " TEXT PRIMARY KEY, " +
                    COLUMN_DURATION + " INTEGER DEFAULT 0" +
                    ");";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        // Creates all tables cleanly on the first launch
        db.execSQL(TABLE_CREATE_ENTRIES);
        db.execSQL(TABLE_CREATE_PROJECTS);
        db.execSQL(TABLE_CREATE_WORKOUT_SETS);
        db.execSQL(TABLE_CREATE_SESSIONS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Since you are starting fresh, any future upgrades will just drop and recreate
        // (You can change this later if you need to preserve data between versions)
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ENTRIES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PROJECTS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_WORKOUT_SETS);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_SESSIONS);
        onCreate(db);
    }
}