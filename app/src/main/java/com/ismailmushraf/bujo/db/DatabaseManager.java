package com.ismailmushraf.bujo.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.ismailmushraf.bujo.models.Entry;
import com.ismailmushraf.bujo.models.Project;
import com.ismailmushraf.bujo.models.WorkoutSet;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class DatabaseManager {

    private DatabaseHelper dbHelper;
    private SQLiteDatabase database;

    public DatabaseManager(Context context) {
        dbHelper = new DatabaseHelper(context);
    }

    public void open() {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public long insertEntry(Entry entry) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_TYPE, entry.getSignifier());
        values.put(DatabaseHelper.COLUMN_CONTENT, entry.getContent());
        values.put(DatabaseHelper.COLUMN_CONTEXT, entry.getProjectTag());
        values.put(DatabaseHelper.COLUMN_COMPLETED, entry.isCompleted() ? 1 : 0);
        values.put(DatabaseHelper.COLUMN_MIGRATED, entry.isMigrated() ? 1 : 0);
        values.put(DatabaseHelper.COLUMN_DEADLINE, entry.getDeadline());
        values.put(DatabaseHelper.COLUMN_HAS_TIME, entry.hasTime() ? 1 : 0);
        values.put(DatabaseHelper.COLUMN_PROJECT_ID, entry.getProjectId());
        return database.insert(DatabaseHelper.TABLE_ENTRIES, null, values);
    }

    public int updateEntry(Entry entry) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_TYPE, entry.getSignifier());
        values.put(DatabaseHelper.COLUMN_CONTENT, entry.getContent());
        values.put(DatabaseHelper.COLUMN_CONTEXT, entry.getProjectTag());
        values.put(DatabaseHelper.COLUMN_COMPLETED, entry.isCompleted() ? 1 : 0);
        values.put(DatabaseHelper.COLUMN_MIGRATED, entry.isMigrated() ? 1 : 0);
        values.put(DatabaseHelper.COLUMN_DEADLINE, entry.getDeadline());
        values.put(DatabaseHelper.COLUMN_HAS_TIME, entry.hasTime() ? 1 : 0);
        values.put(DatabaseHelper.COLUMN_PROJECT_ID, entry.getProjectId());

        return database.update(DatabaseHelper.TABLE_ENTRIES, values, DatabaseHelper.COLUMN_ID + " = ?",
                new String[]{String.valueOf(entry.getId())});
    }

    public void deleteEntry(int id) {
        database.delete(DatabaseHelper.TABLE_ENTRIES, DatabaseHelper.COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
    }

    public List<Entry> getEntriesForProject(int projectId) {
        return getEntries(DatabaseHelper.COLUMN_PROJECT_ID + " = " + projectId);
    }

    public List<Entry> getEntriesWithDeadlines() {
        return getEntries(DatabaseHelper.COLUMN_DEADLINE + " > 0");
    }

    public List<Entry> getMigratedEntries() {
        return getEntries(DatabaseHelper.COLUMN_MIGRATED + " = 1");
    }

    private List<Entry> getEntries(String selection) {
        List<Entry> entries = new ArrayList<>();
        Cursor cursor = database.query(DatabaseHelper.TABLE_ENTRIES, null, selection, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                Entry entry = new Entry();
                entry.setId(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID)));
                entry.setSignifier(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TYPE)));
                entry.setContent(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CONTENT)));
                entry.setProjectTag(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CONTEXT)));
                entry.setCompleted(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_COMPLETED)) == 1);
                entry.setMigrated(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_MIGRATED)) == 1);
                entry.setDeadline(cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DEADLINE)));
                entry.setProjectId(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PROJECT_ID)));

                int hasTimeIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_HAS_TIME);
                if (hasTimeIndex >= 0) {
                    entry.setHasTime(cursor.getInt(hasTimeIndex) == 1);
                }

                entries.add(entry);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return entries;
    }

    public long insertProject(Project project) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_PROJECT_NAME, project.getName());
        return database.insert(DatabaseHelper.TABLE_PROJECTS, null, values);
    }

    public boolean deleteProjectAndUnassignEntries(int projectId) {
        database.beginTransaction();
        try {
            ContentValues entryValues = new ContentValues();
            entryValues.put(DatabaseHelper.COLUMN_PROJECT_ID, 0);
            entryValues.putNull(DatabaseHelper.COLUMN_CONTEXT);
            database.update(DatabaseHelper.TABLE_ENTRIES, entryValues,
                    DatabaseHelper.COLUMN_PROJECT_ID + " = ?",
                    new String[]{String.valueOf(projectId)});

            int deleted = database.delete(DatabaseHelper.TABLE_PROJECTS,
                    DatabaseHelper.COLUMN_ID + " = ?",
                    new String[]{String.valueOf(projectId)});
            database.setTransactionSuccessful();
            return deleted > 0;
        } finally {
            database.endTransaction();
        }
    }

    public List<Project> getAllProjects() {
        List<Project> projects = new ArrayList<>();
        Cursor cursor = database.query(DatabaseHelper.TABLE_PROJECTS, null, null, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                Project project = new Project();
                project.setId(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID)));
                project.setName(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PROJECT_NAME)));
                projects.add(project);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return projects;
    }

    public Project getOrCreateProject(String name) {
        Cursor cursor = database.query(DatabaseHelper.TABLE_PROJECTS, null,
                DatabaseHelper.COLUMN_PROJECT_NAME + " = ?", new String[]{name}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            Project project = new Project();
            project.setId(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID)));
            project.setName(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PROJECT_NAME)));
            cursor.close();
            return project;
        }
        if (cursor != null) {
            cursor.close();
        }

        Project newProject = new Project();
        newProject.setName(name);
        long id = insertProject(newProject);
        newProject.setId((int) id);
        return newProject;
    }

    public List<Entry> getTodayEntries() {
        Calendar copy = Calendar.getInstance();
        copy.set(Calendar.HOUR_OF_DAY, 0);
        copy.set(Calendar.MINUTE, 0);
        copy.set(Calendar.SECOND, 0);
        copy.set(Calendar.MILLISECOND, 0);
        long start = copy.getTimeInMillis();

        copy.set(Calendar.HOUR_OF_DAY, 23);
        copy.set(Calendar.MINUTE, 59);
        copy.set(Calendar.SECOND, 59);
        copy.set(Calendar.MILLISECOND, 999);
        long end = copy.getTimeInMillis();

        return getEntries(DatabaseHelper.COLUMN_DEADLINE + " >= " + start + " AND " + DatabaseHelper.COLUMN_DEADLINE + " <= " + end);
    }

    public List<Entry> getInboxEntries() {
        return getEntries(DatabaseHelper.COLUMN_DEADLINE + " <= 0 OR " + DatabaseHelper.COLUMN_DEADLINE + " IS NULL");
    }

    public int clearCompletedTasks() {
        return database.delete(DatabaseHelper.TABLE_ENTRIES, DatabaseHelper.COLUMN_COMPLETED + " = 1", null);
    }

    // --- WORKOUT METHODS ---
    public long insertWorkoutSet(WorkoutSet ws) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_DATE_STR, ws.getDateStr());
        values.put(DatabaseHelper.COLUMN_EXERCISE, ws.getExercise());
        values.put(DatabaseHelper.COLUMN_WEIGHT, ws.getWeight());
        values.put(DatabaseHelper.COLUMN_REPS, ws.getReps());
        values.put(DatabaseHelper.COLUMN_NOTE, ws.getNote());
        return database.insert(DatabaseHelper.TABLE_WORKOUT_SETS, null, values);
    }

    public void deleteWorkoutSet(int id) {
        database.delete(DatabaseHelper.TABLE_WORKOUT_SETS, DatabaseHelper.COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
    }

    // Calculates the PR using the Epley 1RM formula or Max Reps for bodyweight
    public double getPersonalRecord(String exercise) {
        double maxPR = 0;
        Cursor cursor = database.query(DatabaseHelper.TABLE_WORKOUT_SETS,
                new String[]{DatabaseHelper.COLUMN_WEIGHT, DatabaseHelper.COLUMN_REPS},
                DatabaseHelper.COLUMN_EXERCISE + " = ? COLLATE NOCASE", new String[]{exercise},
                null, null, null);

        if (cursor != null && cursor.moveToFirst()) {
            do {
                double w = cursor.getDouble(0);
                int r = cursor.getInt(1);
                double score = (w <= 0) ? r : (w * (1.0 + (r / 30.0)));
                if (score > maxPR) maxPR = score;
            } while (cursor.moveToNext());
            cursor.close();
        }
        return maxPR;
    }

    // Retrieves today's sets, grouped into UI elements
    public List<Object> getGroupedDailyWorkouts(String dateStr) {
        List<Object> list = new ArrayList<>();
        Cursor cursor = database.query(DatabaseHelper.TABLE_WORKOUT_SETS, null,
                DatabaseHelper.COLUMN_DATE_STR + " = ?", new String[]{dateStr},
                null, null, DatabaseHelper.COLUMN_EXERCISE + " ASC, " + DatabaseHelper.COLUMN_ID + " ASC");

        if (cursor != null && cursor.moveToFirst()) {
            String currentEx = "";
            int setNum = 1;
            do {
                WorkoutSet ws = new WorkoutSet();
                ws.setId(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID)));
                ws.setDateStr(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DATE_STR)));
                ws.setExercise(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_EXERCISE)));
                ws.setWeight(cursor.getDouble(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_WEIGHT)));
                ws.setReps(cursor.getInt(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_REPS)));
                ws.setNote(cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_NOTE)));

                if (!ws.getExercise().equalsIgnoreCase(currentEx)) {
                    currentEx = ws.getExercise();
                    setNum = 1;

                    double pr = getPersonalRecord(currentEx);
                    String prText = (pr == Math.floor(pr)) ? String.valueOf((int)pr) : String.format(Locale.US, "%.1f", pr);
                    String type = (ws.getWeight() <= 0) ? "Reps" : "kg 1RM";

                    list.add(currentEx.toUpperCase() + " (PR: " + prText + " " + type + ")");
                }

                ws.setSetNumber(setNum++);
                list.add(ws);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return list;
    }

    public List<String> getUniqueExerciseNames() {
        List<String> names = new ArrayList<>();
        names.add("Pull-ups");
        names.add("Dumbbell Press");
        names.add("Mobility Flow");
        names.add("Yoga");

        Cursor cursor = database.query(true, DatabaseHelper.TABLE_WORKOUT_SETS,
                new String[]{DatabaseHelper.COLUMN_EXERCISE}, null, null, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                String name = cursor.getString(0);
                if (!names.contains(name)) names.add(name);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return names;
    }

    public void saveSessionDuration(String dateStr, long durationMs) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_DATE_STR, dateStr);
        values.put(DatabaseHelper.COLUMN_DURATION, durationMs);
        database.insertWithOnConflict(DatabaseHelper.TABLE_SESSIONS, null, values, SQLiteDatabase.CONFLICT_REPLACE);
    }

    public long getSessionDuration(String dateStr) {
        long duration = 0;
        Cursor cursor = database.query(DatabaseHelper.TABLE_SESSIONS, new String[]{DatabaseHelper.COLUMN_DURATION},
                DatabaseHelper.COLUMN_DATE_STR + " = ?", new String[]{dateStr}, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            duration = cursor.getLong(0);
            cursor.close();
        }
        return duration;
    }

    public List<String> getAllWorkoutDatesDescending() {
        List<String> dates = new ArrayList<>();
        Cursor cursor = database.query(true, DatabaseHelper.TABLE_WORKOUT_SETS,
                new String[]{DatabaseHelper.COLUMN_DATE_STR},
                null, null, null, null, DatabaseHelper.COLUMN_DATE_STR + " DESC", null);
        if (cursor != null && cursor.moveToFirst()) {
            do {
                dates.add(cursor.getString(0));
            } while (cursor.moveToNext());
            cursor.close();
        }
        return dates;
    }
}