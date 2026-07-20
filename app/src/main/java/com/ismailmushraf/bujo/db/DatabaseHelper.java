package com.ismailmushraf.bujo.db;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

public class DatabaseHelper extends SQLiteOpenHelper {

    private static final String DATABASE_NAME = "bujo.db";
    private static final int DATABASE_VERSION = 1;

    public static final String TABLE_ENTRIES = "entries";
    public static final String COLUMN_ID = "_id";
    public static final String COLUMN_TYPE = "type"; // "*", "-", "o"
    public static final String COLUMN_CONTENT = "content";
    public static final String COLUMN_CONTEXT = "context";
    public static final String COLUMN_COMPLETED = "completed";
    public static final String COLUMN_MIGRATED = "migrated";
    public static final String COLUMN_DEADLINE = "deadline";
    public static final String COLUMN_PROJECT_ID = "project_id";
    public static final String COLUMN_HAS_TIME = "has_time"; // FIX 2: Add column name

    public static final String TABLE_PROJECTS = "projects";
    public static final String COLUMN_PROJECT_NAME = "name";

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
            COLUMN_HAS_TIME + " INTEGER DEFAULT 0 " +
            ");";

    private static final String TABLE_CREATE_PROJECTS =
            "CREATE TABLE " + TABLE_PROJECTS + " (" +
            COLUMN_ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
            COLUMN_PROJECT_NAME + " TEXT" +
            ");";

    public DatabaseHelper(Context context) {
        super(context, DATABASE_NAME, null, DATABASE_VERSION);
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(TABLE_CREATE_ENTRIES);
        db.execSQL(TABLE_CREATE_PROJECTS);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_ENTRIES);
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_PROJECTS);
        onCreate(db);
    }
}
