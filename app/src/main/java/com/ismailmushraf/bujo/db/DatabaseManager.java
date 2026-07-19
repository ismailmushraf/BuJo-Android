package com.ismailmushraf.bujo.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.ismailmushraf.bujo.models.Entry;
import com.ismailmushraf.bujo.models.Project;

import java.util.ArrayList;
import java.util.List;

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

    public List<Entry> getDailyLogEntries() {
        // Daily Log entries are those that are NOT assigned to a specific project.
        return getEntries(DatabaseHelper.COLUMN_PROJECT_ID + " = 0 OR " + DatabaseHelper.COLUMN_PROJECT_ID + " IS NULL");
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

    /**
     * Removes a project without removing the journal entries that belong to it.
     * Those entries become part of the Daily Log again, which makes this action
     * reversible from the user's point of view rather than silently losing notes.
     */
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
}
