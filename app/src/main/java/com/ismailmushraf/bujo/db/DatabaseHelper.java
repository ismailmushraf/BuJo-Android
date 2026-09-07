package com.ismailmushraf.bujo.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "bujo.db";
    private static final int DATABASE_VERSION = 12; // Added is_locked for task immutability

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
    public static final String COLUMN_COMPLETED_AT = "completed_at";
    public static final String COLUMN_CREATED_AT = "created_at";
    public static final String COLUMN_PARENT_ID = "parent_id";
    public static final String COLUMN_IS_AUDITED = "is_audited";
    public static final String COLUMN_IS_LOCKED = "is_locked";

    // --- PROJECTS TABLE ---
    public static final String TABLE_PROJECTS = "projects";
    public static final String COLUMN_PROJECT_NAME = "name";
    public static final String COLUMN_PROJECT_WEIGHT = "weight";
    public static final String COLUMN_PROJECT_CREATED_AT = "created_at";

    // --- HABITS TABLE ---
    public static final String TABLE_HABITS = "habits";
    public static final String COLUMN_HABIT_NAME = "name";
    public static final String COLUMN_HABIT_COMMITMENT = "commitment_days";
    public static final String COLUMN_HABIT_START_DATE = "start_date"; // YYYY-MM-DD
    public static final String COLUMN_HABIT_CREATED_AT = "created_at";
    public static final String COLUMN_HABIT_DEADLINE_TIME = "deadline_time";
    public static final String COLUMN_HABIT_HAS_TIME = "has_time";

    // --- HABIT LOGS TABLE ---
    public static final String TABLE_HABIT_LOGS = "habit_logs";
    public static final String COLUMN_HABIT_ID = "habit_id";
    public static final String COLUMN_LOG_DATE = "date_str"; // YYYY-MM-DD
    public static final String COLUMN_LOG_COMPLETED = "completed";
    public static final String COLUMN_LOG_ON_TIME = "on_time";

    // --- WORKOUT SETS TABLE ---
    public static final String TABLE_WORKOUT_SETS = "workout_sets";
    public static final String COLUMN_DATE_STR = "date_str";
    public static final String COLUMN_EXERCISE = "exercise";
    public static final String COLUMN_WEIGHT = "weight";
    public static final String COLUMN_REPS = "reps";
    public static final String COLUMN_NOTE = "note";

    // --- WORKOUT SESSIONS TABLE ---
    public static final String TABLE_SESSIONS = "workout_sessions";
    public static final String COLUMN_DURATION = "duration_ms";

    // --- USER STATS TABLE (Gamification) ---
    public static final String TABLE_USER_STATS = "user_stats";
    public static final String COLUMN_POINTS = "total_points";
    public static final String COLUMN_CURRENT_STREAK = "current_streak";
    public static final String COLUMN_LONGEST_STREAK = "longest_streak";
    public static final String COLUMN_REST_TOKENS = "rest_tokens";
    public static final String COLUMN_LAST_ACTIVE_DATE = "last_active_date";
    public static final String COLUMN_POINTS_TASKS = "points_tasks";
    public static final String COLUMN_POINTS_HABITS = "points_habits";
    public static final String COLUMN_POINTS_WORKOUTS = "points_workouts";
    public static final String COLUMN_LAST_PRODUCTIVE_DATE = "last_productive_date";

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
                    COLUMN_HAS_TIME + " INTEGER DEFAULT 0, " +
                    COLUMN_COMPLETED_AT + " INTEGER, " +
                    COLUMN_CREATED_AT + " INTEGER, " +
                    COLUMN_PARENT_ID + " INTEGER DEFAULT 0, " +
                    COLUMN_IS_AUDITED + " INTEGER DEFAULT 0, " +
                    COLUMN_IS_LOCKED + " INTEGER DEFAULT 0" +
                    ");";

    private static final String TABLE_CREATE_PROJECTS =
            "CREATE TABLE " + TABLE_PROJECTS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_PROJECT_NAME + " TEXT, " +
                    COLUMN_PROJECT_WEIGHT + " INTEGER DEFAULT 1, " +
                    COLUMN_PROJECT_CREATED_AT + " INTEGER" +
                    ");";

    private static final String TABLE_CREATE_HABITS =
            "CREATE TABLE " + TABLE_HABITS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_HABIT_NAME + " TEXT, " +
                    COLUMN_HABIT_COMMITMENT + " INTEGER, " +
                    COLUMN_HABIT_START_DATE + " TEXT, " +
                    COLUMN_HABIT_CREATED_AT + " INTEGER, " +
                    COLUMN_HABIT_DEADLINE_TIME + " INTEGER, " +
                    COLUMN_HABIT_HAS_TIME + " INTEGER DEFAULT 0" +
                    ");";

    private static final String TABLE_CREATE_HABIT_LOGS =
            "CREATE TABLE " + TABLE_HABIT_LOGS + " (" +
                    COLUMN_HABIT_ID + " INTEGER, " +
                    COLUMN_LOG_DATE + " TEXT, " +
                    COLUMN_LOG_COMPLETED + " INTEGER DEFAULT 0, " +
                    COLUMN_LOG_ON_TIME + " INTEGER DEFAULT 0, " +
                    "PRIMARY KEY (" + COLUMN_HABIT_ID + ", " + COLUMN_LOG_DATE + ")" +
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

    private static final String TABLE_CREATE_USER_STATS =
            "CREATE TABLE " + TABLE_USER_STATS + " (" +
                    COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                    COLUMN_POINTS + " INTEGER DEFAULT 0, " +
                    COLUMN_CURRENT_STREAK + " INTEGER DEFAULT 0, " +
                    COLUMN_LONGEST_STREAK + " INTEGER DEFAULT 0, " +
                    COLUMN_REST_TOKENS + " INTEGER DEFAULT 0, " +
                    COLUMN_LAST_ACTIVE_DATE + " TEXT, " +
                    COLUMN_POINTS_TASKS + " INTEGER DEFAULT 0, " +
                    COLUMN_POINTS_HABITS + " INTEGER DEFAULT 0, " +
                    COLUMN_POINTS_WORKOUTS + " INTEGER DEFAULT 0, " +
                    COLUMN_LAST_PRODUCTIVE_DATE + " TEXT" +
                    ");";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE_ENTRIES);
        db.execSQL(TABLE_CREATE_PROJECTS);
        db.execSQL(TABLE_CREATE_HABITS);
        db.execSQL(TABLE_CREATE_HABIT_LOGS);
        db.execSQL(TABLE_CREATE_WORKOUT_SETS);
        db.execSQL(TABLE_CREATE_SESSIONS);
        db.execSQL(TABLE_CREATE_USER_STATS);
        db.execSQL("INSERT INTO " + TABLE_USER_STATS + " (" + COLUMN_POINTS + ") VALUES (0);");
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        if (oldVersion < 2) {
            db.execSQL(TABLE_CREATE_USER_STATS);
            db.execSQL("INSERT INTO " + TABLE_USER_STATS + " (" + COLUMN_POINTS + ") VALUES (0);");
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE projects ADD COLUMN weight INTEGER DEFAULT 1;");
            db.execSQL("ALTER TABLE entries ADD COLUMN completed_at INTEGER;");
        }
        if (oldVersion < 4) {
            db.execSQL("ALTER TABLE projects ADD COLUMN created_at INTEGER;");
            db.execSQL("ALTER TABLE entries ADD COLUMN created_at INTEGER;");
            
            long now = System.currentTimeMillis();
            db.execSQL("UPDATE projects SET created_at = " + now);
            db.execSQL("UPDATE entries SET created_at = " + now);
        }
        if (oldVersion < 5) {
            db.execSQL(TABLE_CREATE_HABITS);
            db.execSQL(TABLE_CREATE_HABIT_LOGS);
        }
        if (oldVersion < 6) {
            db.execSQL("ALTER TABLE " + TABLE_ENTRIES + " ADD COLUMN " + COLUMN_PARENT_ID + " INTEGER DEFAULT 0;");
        }
        if (oldVersion < 7) {
            db.execSQL("ALTER TABLE " + TABLE_HABITS + " ADD COLUMN " + COLUMN_HABIT_DEADLINE_TIME + " INTEGER;");
            db.execSQL("ALTER TABLE " + TABLE_HABITS + " ADD COLUMN " + COLUMN_HABIT_HAS_TIME + " INTEGER DEFAULT 0;");
        }
        if (oldVersion < 8) {
            db.execSQL("ALTER TABLE " + TABLE_USER_STATS + " ADD COLUMN " + COLUMN_POINTS_TASKS + " INTEGER DEFAULT 0;");
            db.execSQL("ALTER TABLE " + TABLE_USER_STATS + " ADD COLUMN " + COLUMN_POINTS_HABITS + " INTEGER DEFAULT 0;");
            db.execSQL("ALTER TABLE " + TABLE_USER_STATS + " ADD COLUMN " + COLUMN_POINTS_WORKOUTS + " INTEGER DEFAULT 0;");
        }
        if (oldVersion < 9) {
            db.execSQL("ALTER TABLE " + TABLE_ENTRIES + " ADD COLUMN " + COLUMN_IS_AUDITED + " INTEGER DEFAULT 0;");
        }
        if (oldVersion < 10) {
            db.execSQL("ALTER TABLE " + TABLE_HABIT_LOGS + " ADD COLUMN " + COLUMN_LOG_ON_TIME + " INTEGER DEFAULT 0;");
        }
        if (oldVersion < 11) {
            db.execSQL("ALTER TABLE " + TABLE_USER_STATS + " ADD COLUMN " + COLUMN_LAST_PRODUCTIVE_DATE + " TEXT;");
        }
        if (oldVersion < 12) {
            db.execSQL("ALTER TABLE " + TABLE_ENTRIES + " ADD COLUMN " + COLUMN_IS_LOCKED + " INTEGER DEFAULT 0;");
        }
    }
}
