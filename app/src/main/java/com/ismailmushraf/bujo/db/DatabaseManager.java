package com.ismailmushraf.bujo.db;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;

import com.ismailmushraf.bujo.models.Entry;
import com.ismailmushraf.bujo.models.Habit;
import com.ismailmushraf.bujo.models.Project;
import com.ismailmushraf.bujo.models.WorkoutSet;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class DatabaseManager {

    public static final String CAT_TASKS = "tasks";
    public static final String CAT_HABITS = "habits";
    public static final String CAT_WORKOUTS = "workouts";

    private DatabaseHelper dbHelper;
    private SQLiteDatabase database;

    public DatabaseManager(Context context) {
        dbHelper = new DatabaseHelper(context);
    }

    public void open() {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        if (dbHelper != null) {
            dbHelper.close();
        }
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
        values.put(DatabaseHelper.COLUMN_PARENT_ID, entry.getParentId());
        values.put(DatabaseHelper.COLUMN_CREATED_AT, entry.getCreatedAt() > 0 ? entry.getCreatedAt() : System.currentTimeMillis());
        values.put(DatabaseHelper.COLUMN_IS_AUDITED, entry.isAudited() ? 1 : 0);
        if (entry.isCompleted()) {
            values.put(DatabaseHelper.COLUMN_COMPLETED_AT, entry.getCompletedAt() > 0 ? entry.getCompletedAt() : System.currentTimeMillis());
        }
        long id = database.insert(DatabaseHelper.TABLE_ENTRIES, null, values);
        // Never reward a task that failed to be persisted.
        if (id != -1) {
            adjustPoints(calculateCommitmentReward(entry), CAT_TASKS);
        }
        return id;
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
        values.put(DatabaseHelper.COLUMN_PARENT_ID, entry.getParentId());
        values.put(DatabaseHelper.COLUMN_IS_AUDITED, entry.isAudited() ? 1 : 0);

        int pointsDelta = 0;
        Cursor c = database.query(DatabaseHelper.TABLE_ENTRIES,
                new String[]{DatabaseHelper.COLUMN_COMPLETED_AT, DatabaseHelper.COLUMN_DEADLINE,
                        DatabaseHelper.COLUMN_HAS_TIME, DatabaseHelper.COLUMN_TYPE,
                        DatabaseHelper.COLUMN_PROJECT_ID, DatabaseHelper.COLUMN_PARENT_ID},
                DatabaseHelper.COLUMN_ID + " = ?", new String[]{String.valueOf(entry.getId())}, null, null, null);
        if (c != null && c.moveToFirst()) {
            long previousCompletedAt = c.getLong(0);
            long previousDeadline = c.getLong(1);
            boolean previousHasTime = c.getInt(2) == 1;
            Entry previous = new Entry();
            previous.setSignifier(c.getString(3));
            previous.setProjectId(c.getInt(4));
            previous.setParentId(c.getInt(5));
            previous.setDeadline(previousDeadline);
            c.close();

            if (entry.isCompleted() && previousCompletedAt <= 0) {
                long completedAt = System.currentTimeMillis();
                values.put(DatabaseHelper.COLUMN_COMPLETED_AT, completedAt);
                entry.setCompletedAt(completedAt);
                pointsDelta += calculateCompletionReward(entry)
                        + ((entry.hasTime() && completedAt <= entry.getDeadline()) ? 10 : 0);
            } else if (!entry.isCompleted() && previousCompletedAt > 0) {
                values.putNull(DatabaseHelper.COLUMN_COMPLETED_AT);
                entry.setCompletedAt(0);
                pointsDelta -= (calculateCompletionReward(previous)
                        + ((previousHasTime && previousCompletedAt <= previousDeadline) ? 10 : 0));
            }

            // Commitment status changes
            boolean wasCommittedToday = isToday(previousDeadline) && "*".equals(previous.getSignifier()) && previous.getParentId() == 0;
            boolean isCommittedToday = isToday(entry.getDeadline()) && "*".equals(entry.getSignifier()) && entry.getParentId() == 0;

            if (!wasCommittedToday && isCommittedToday) {
                pointsDelta += 5;
            } else if (wasCommittedToday && !isCommittedToday) {
                pointsDelta -= 5;
            }
        } else if (c != null) {
            c.close();
        }

        int updated = database.update(DatabaseHelper.TABLE_ENTRIES, values, DatabaseHelper.COLUMN_ID + " = ?",
                new String[]{String.valueOf(entry.getId())});
        if (updated != 0 && pointsDelta != 0) adjustPoints(pointsDelta, CAT_TASKS);
        return updated;
    }

    public int deleteEntry(int id) {
        int pointsDeducted = 0;
        // Deduct reward on deletion ONLY IF NOT COMPLETED to prevent point farming
        Cursor c = database.query(DatabaseHelper.TABLE_ENTRIES, null, 
                DatabaseHelper.COLUMN_ID + " = ?", new String[]{String.valueOf(id)}, null, null, null);
        if (c != null && c.moveToFirst()) {
            boolean completed = c.getInt(c.getColumnIndexOrThrow(DatabaseHelper.COLUMN_COMPLETED)) == 1;
            
            if (!completed) {
                Entry e = new Entry();
                e.setSignifier(c.getString(c.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TYPE)));
                e.setProjectId(c.getInt(c.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PROJECT_ID)));
                e.setParentId(c.getInt(c.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PARENT_ID)));
                e.setDeadline(c.getLong(c.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DEADLINE)));
                
                pointsDeducted = calculateCommitmentReward(e);
                if (pointsDeducted > 0) {
                    pointsDeducted = -adjustPoints(-pointsDeducted, CAT_TASKS);
                }
            }
            c.close();
        }
        database.delete(DatabaseHelper.TABLE_ENTRIES, DatabaseHelper.COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        return pointsDeducted;
    }

    public List<Entry> getEntriesForProject(int projectId) {
        return getEntries(DatabaseHelper.COLUMN_PROJECT_ID + " = " + projectId + " AND " + DatabaseHelper.COLUMN_PARENT_ID + " = 0", null);
    }

    public List<Entry> getCompletedEntriesForProject(int projectId) {
        return getEntries(DatabaseHelper.COLUMN_PROJECT_ID + " = " + projectId + " AND " + DatabaseHelper.COLUMN_PARENT_ID + " = 0 AND " + DatabaseHelper.COLUMN_COMPLETED + " = 1", DatabaseHelper.COLUMN_COMPLETED_AT + " DESC");
    }

    public List<Entry> getEntriesWithDeadlines() {
        return getEntries(DatabaseHelper.COLUMN_DEADLINE + " > 0", null);
    }

    public List<Entry> getMigratedEntries() {
        return getEntries(DatabaseHelper.COLUMN_MIGRATED + " = 1", null);
    }

    public List<Entry> getChildEntries(int parentId) {
        return getEntries(DatabaseHelper.COLUMN_PARENT_ID + " = " + parentId, null);
    }

    private List<Entry> getEntries(String selection, String orderBy) {
        List<Entry> entries = new ArrayList<>();
        Cursor cursor = database.query(DatabaseHelper.TABLE_ENTRIES, null, selection, null, null, null, orderBy);
        if (cursor != null && cursor.moveToFirst()) {
            int idIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID);
            int typeIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TYPE);
            int contentIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CONTENT);
            int contextIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_CONTEXT);
            int completedIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_COMPLETED);
            int migratedIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_MIGRATED);
            int deadlineIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DEADLINE);
            int projectIdIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PROJECT_ID);
            int hasTimeIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_HAS_TIME);
            int completedAtIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_COMPLETED_AT);
            int createdAtIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_CREATED_AT);
            int parentIdIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_PARENT_ID);
            int auditedIndex = cursor.getColumnIndex(DatabaseHelper.COLUMN_IS_AUDITED);

            do {
                Entry entry = new Entry();
                entry.setId(cursor.getInt(idIndex));
                entry.setSignifier(cursor.getString(typeIndex));
                entry.setContent(cursor.getString(contentIndex));
                entry.setProjectTag(cursor.getString(contextIndex));
                entry.setCompleted(cursor.getInt(completedIndex) == 1);
                entry.setMigrated(cursor.getInt(migratedIndex) == 1);
                entry.setDeadline(cursor.getLong(deadlineIndex));
                entry.setProjectId(cursor.getInt(projectIdIndex));

                if (hasTimeIndex >= 0) entry.setHasTime(cursor.getInt(hasTimeIndex) == 1);
                if (completedAtIndex >= 0) entry.setCompletedAt(cursor.getLong(completedAtIndex));
                if (createdAtIndex >= 0) entry.setCreatedAt(cursor.getLong(createdAtIndex));
                if (parentIdIndex >= 0) entry.setParentId(cursor.getInt(parentIdIndex));
                if (auditedIndex >= 0) entry.setAudited(cursor.getInt(auditedIndex) == 1);

                entries.add(entry);
            } while (cursor.moveToNext());
            cursor.close();
        }
        return entries;
    }

    public long insertProject(Project project) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_PROJECT_NAME, project.getName());
        values.put(DatabaseHelper.COLUMN_PROJECT_WEIGHT, project.getWeight() > 0 ? project.getWeight() : 1);
        values.put(DatabaseHelper.COLUMN_PROJECT_CREATED_AT, project.getCreatedAt() > 0 ? project.getCreatedAt() : System.currentTimeMillis());
        return database.insert(DatabaseHelper.TABLE_PROJECTS, null, values);
    }

    public int updateProject(Project project) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_PROJECT_NAME, project.getName());
        values.put(DatabaseHelper.COLUMN_PROJECT_WEIGHT, project.getWeight());
        int updated = database.update(DatabaseHelper.TABLE_PROJECTS, values, DatabaseHelper.COLUMN_ID + " = ?",
                new String[]{String.valueOf(project.getId())});

        if (updated > 0 && project.getId() > 0 && project.getName() != null) {
            ContentValues entryValues = new ContentValues();
            entryValues.put(DatabaseHelper.COLUMN_CONTEXT, project.getName());
            database.update(DatabaseHelper.TABLE_ENTRIES, entryValues,
                    DatabaseHelper.COLUMN_PROJECT_ID + " = ?",
                    new String[]{String.valueOf(project.getId())});
        }
        return updated;
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

    public int deleteProjectAndAllEntries(int projectId) {
        int uncompletedTopLevelTasks = 0;
        Cursor countCursor = database.rawQuery("SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_ENTRIES +
                        " WHERE " + DatabaseHelper.COLUMN_PROJECT_ID + " = ? AND " +
                        DatabaseHelper.COLUMN_COMPLETED + " = 0 AND " +
                        DatabaseHelper.COLUMN_TYPE + " = '*' AND " +
                        DatabaseHelper.COLUMN_PARENT_ID + " = 0",
                new String[]{String.valueOf(projectId)});
        if (countCursor != null && countCursor.moveToFirst()) {
            uncompletedTopLevelTasks = countCursor.getInt(0);
            countCursor.close();
        } else if (countCursor != null) {
            countCursor.close();
        }
        database.beginTransaction();
        try {
            database.delete(DatabaseHelper.TABLE_ENTRIES,
                    DatabaseHelper.COLUMN_PROJECT_ID + " = ?",
                    new String[]{String.valueOf(projectId)});

            database.delete(DatabaseHelper.TABLE_PROJECTS,
                    DatabaseHelper.COLUMN_ID + " = ?",
                    new String[]{String.valueOf(projectId)});

            database.setTransactionSuccessful();
        } finally {
            database.endTransaction();
        }
        // Removing an unfinished task cancels its creation reward. Completed-task XP is
        // intentionally retained as historical progress, just like clearing completed tasks.
        return uncompletedTopLevelTasks > 0
                ? adjustPoints(-(uncompletedTopLevelTasks * 5), CAT_TASKS) : 0;
    }

    public List<Project> getAllProjects() {
        List<Project> projects = new ArrayList<>();
        Cursor cursor = database.query(DatabaseHelper.TABLE_PROJECTS, null, null, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            int idIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID);
            int nameIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PROJECT_NAME);
            int weightIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PROJECT_WEIGHT);
            int createdAtIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PROJECT_CREATED_AT);

            do {
                Project project = new Project();
                project.setId(cursor.getInt(idIndex));
                project.setName(cursor.getString(nameIndex));
                if (weightIndex >= 0) project.setWeight(cursor.getInt(weightIndex));
                if (createdAtIndex >= 0) project.setCreatedAt(cursor.getLong(createdAtIndex));
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
            int idIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID);
            int nameIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PROJECT_NAME);
            int weightIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PROJECT_WEIGHT);
            int createdAtIndex = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PROJECT_CREATED_AT);

            Project project = new Project();
            project.setId(cursor.getInt(idIndex));
            project.setName(cursor.getString(nameIndex));
            if (weightIndex >= 0) project.setWeight(cursor.getInt(weightIndex));
            if (createdAtIndex >= 0) project.setCreatedAt(cursor.getLong(createdAtIndex));
            cursor.close();
            return project;
        }
        if (cursor != null) cursor.close();

        Project newProject = new Project();
        newProject.setName(name);
        newProject.setWeight(1);
        newProject.setCreatedAt(System.currentTimeMillis());
        long id = insertProject(newProject);
        newProject.setId((int) id);
        return newProject;
    }

    public List<Entry> getTodayEntries() {
        Calendar copy = Calendar.getInstance();
        copy.set(Calendar.HOUR_OF_DAY, 0); copy.set(Calendar.MINUTE, 0);
        copy.set(Calendar.SECOND, 0); copy.set(Calendar.MILLISECOND, 0);
        long start = copy.getTimeInMillis();
        copy.set(Calendar.HOUR_OF_DAY, 23); copy.set(Calendar.MINUTE, 59);
        copy.set(Calendar.SECOND, 59); copy.set(Calendar.MILLISECOND, 999);
        long end = copy.getTimeInMillis();
        // Only return top-level entries (parent_id == 0) for the main list
        return getEntries(DatabaseHelper.COLUMN_DEADLINE + " >= " + start + " AND " + DatabaseHelper.COLUMN_DEADLINE + " <= " + end + " AND " + DatabaseHelper.COLUMN_PARENT_ID + " = 0", null);
    }


    public List<Entry> getInboxEntries() {
        return getEntries(DatabaseHelper.COLUMN_DEADLINE + " <= 0 OR " + DatabaseHelper.COLUMN_DEADLINE + " IS NULL", null);
    }

    public int clearCompletedTasks() {
        return database.delete(DatabaseHelper.TABLE_ENTRIES, DatabaseHelper.COLUMN_COMPLETED + " = 1", null);
    }

    public float getProjectEfficiency(int projectId) {
        long sevenDaysAgo = System.currentTimeMillis() - (7 * 24 * 60 * 60 * 1000L);
        String selection = DatabaseHelper.COLUMN_PROJECT_ID + " = " + projectId + 
                " AND " + DatabaseHelper.COLUMN_TYPE + " = '*'" +
                " AND (" + DatabaseHelper.COLUMN_CREATED_AT + " >= " + sevenDaysAgo + 
                " OR " + DatabaseHelper.COLUMN_COMPLETED_AT + " >= " + sevenDaysAgo + ")";
        List<Entry> projectTasks = getEntries(selection, null);
        if (projectTasks.isEmpty()) return 1.0f;
        int total = projectTasks.size();
        int completed = 0;
        for (Entry e : projectTasks) if (e.isCompleted()) completed++;
        return (float) completed / total;
    }

    public List<Entry> getSmartRecommendations(int maxHours) {
        List<Entry> suggestions = new ArrayList<>();
        List<Project> projects = getAllProjects();
        if (projects.isEmpty()) return suggestions;

        List<ProjectScore> projectScores = new ArrayList<>();
        for (Project p : projects) {
            float efficiency = getProjectEfficiency(p.getId());
            float deficiency = 1.0f - efficiency;
            float score = p.getWeight() * (deficiency + 0.1f);
            projectScores.add(new ProjectScore(p, score));
        }

        Collections.sort(projectScores, new Comparator<ProjectScore>() {
            @Override public int compare(ProjectScore p1, ProjectScore p2) {
                return Float.compare(p2.score, p1.score);
            }
        });

        int remainingBudget = maxHours;
        for (ProjectScore ps : projectScores) {
            if (remainingBudget <= 0) break;
            int cost = determineBestCost(ps.project.getWeight(), remainingBudget);
            if (cost > 0) {
                suggestions.add(createSessionEntry(ps.project, cost, false));
                remainingBudget -= cost;
            }
        }
        
        int safetyIter = 0;
        while (remainingBudget >= 1 && safetyIter < 5) {
            boolean added = false;
            for (ProjectScore ps : projectScores) {
                if (remainingBudget <= 0) break;
                int cost = (remainingBudget >= 2) ? 2 : 1;
                suggestions.add(createSessionEntry(ps.project, cost, true));
                remainingBudget -= cost;
                added = true;
            }
            if (!added) break;
            safetyIter++;
        }
        return suggestions;
    }

    private int determineBestCost(int weight, int budget) {
        if (weight >= 5 && budget >= 4) return 4;
        if (weight >= 4 && budget >= 3) return 3;
        if (budget >= 2) return 2;
        if (budget >= 1) return 1;
        return 0;
    }

    private Entry createSessionEntry(Project p, int hours, boolean isBonus) {
        String text;
        String name = p.getName();
        if (!isBonus) {
            if (hours == 4) text = "4h Focus: " + name;
            else if (hours == 3) text = name + " Deep Work";
            else text = name + " Session (" + hours + "h)";
        } else {
            if (hours >= 2) text = "Bonus Sprint: " + name;
            else text = "Final 1h Wrap-up: " + name;
        }
        Entry e = new Entry();
        e.setSignifier("*");
        e.setContent(text);
        e.setProjectId(p.getId());
        e.setProjectTag(name);
        e.setParentId(0);
        return e;
    }

    private static class ProjectScore {
        Project project;
        float score;
        ProjectScore(Project p, float s) { this.project = p; this.score = s; }
    }

    public long insertHabit(Habit habit) {
        ContentValues v = new ContentValues();
        v.put(DatabaseHelper.COLUMN_HABIT_NAME, habit.getName());
        v.put(DatabaseHelper.COLUMN_HABIT_COMMITMENT, habit.getCommitmentDays());
        v.put(DatabaseHelper.COLUMN_HABIT_START_DATE, habit.getStartDate());
        v.put(DatabaseHelper.COLUMN_HABIT_CREATED_AT, System.currentTimeMillis());
        v.put(DatabaseHelper.COLUMN_HABIT_DEADLINE_TIME, habit.getDeadlineTime());
        v.put(DatabaseHelper.COLUMN_HABIT_HAS_TIME, habit.hasTime() ? 1 : 0);
        return database.insert(DatabaseHelper.TABLE_HABITS, null, v);
    }

    public List<Habit> getAllHabits() {
        List<Habit> habits = new ArrayList<>();
        Cursor c = database.query(DatabaseHelper.TABLE_HABITS, null, null, null, null, null, null);
        if (c != null && c.moveToFirst()) {
            int idIdx = c.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID);
            int nameIdx = c.getColumnIndexOrThrow(DatabaseHelper.COLUMN_HABIT_NAME);
            int commIdx = c.getColumnIndexOrThrow(DatabaseHelper.COLUMN_HABIT_COMMITMENT);
            int startIdx = c.getColumnIndexOrThrow(DatabaseHelper.COLUMN_HABIT_START_DATE);
            int createIdx = c.getColumnIndexOrThrow(DatabaseHelper.COLUMN_HABIT_CREATED_AT);
            int deadlineIdx = c.getColumnIndexOrThrow(DatabaseHelper.COLUMN_HABIT_DEADLINE_TIME);
            int hasTimeIdx = c.getColumnIndexOrThrow(DatabaseHelper.COLUMN_HABIT_HAS_TIME);
            do {
                Habit h = new Habit();
                h.setId(c.getInt(idIdx));
                h.setName(c.getString(nameIdx));
                h.setCommitmentDays(c.getInt(commIdx));
                h.setStartDate(c.getString(startIdx));
                h.setCreatedAt(c.getLong(createIdx));
                h.setDeadlineTime(c.getLong(deadlineIdx));
                h.setHasTime(c.getInt(hasTimeIdx) == 1);
                habits.add(h);
            } while (c.moveToNext());
            c.close();
        }
        return habits;
    }

    public void deleteHabit(int habitId) {
        database.beginTransaction();
        try {
            database.delete(DatabaseHelper.TABLE_HABIT_LOGS, DatabaseHelper.COLUMN_HABIT_ID + " = ?", new String[]{String.valueOf(habitId)});
            database.delete(DatabaseHelper.TABLE_HABITS, DatabaseHelper.COLUMN_ID + " = ?", new String[]{String.valueOf(habitId)});
            database.setTransactionSuccessful();
        } finally { database.endTransaction(); }
    }

    public boolean isHabitCompletedOnDate(int habitId, String dateStr) {
        Cursor c = database.query(DatabaseHelper.TABLE_HABIT_LOGS, new String[]{DatabaseHelper.COLUMN_LOG_COMPLETED},
                DatabaseHelper.COLUMN_HABIT_ID + " = ? AND " + DatabaseHelper.COLUMN_LOG_DATE + " = ?",
                new String[]{String.valueOf(habitId), dateStr}, null, null, null);
        boolean completed = false;
        if (c != null) {
            if (c.moveToFirst()) {
                completed = c.getInt(0) == 1;
            }
            c.close();
        }
        return completed;
    }

    public boolean isHabitOnTimeOnDate(int habitId, String dateStr) {
        Cursor c = database.query(DatabaseHelper.TABLE_HABIT_LOGS, new String[]{DatabaseHelper.COLUMN_LOG_ON_TIME},
                DatabaseHelper.COLUMN_HABIT_ID + " = ? AND " + DatabaseHelper.COLUMN_LOG_DATE + " = ?",
                new String[]{String.valueOf(habitId), dateStr}, null, null, null);
        boolean onTime = false;
        if (c != null) {
            if (c.moveToFirst()) {
                onTime = c.getInt(0) == 1;
            }
            c.close();
        }
        return onTime;
    }

    public Map<String, Boolean> getHabitCompletionMap(int habitId, String startDate, String endDate) {
        Map<String, Boolean> map = new HashMap<>();
        String selection = DatabaseHelper.COLUMN_HABIT_ID + " = ? AND " +
                DatabaseHelper.COLUMN_LOG_DATE + " BETWEEN ? AND ?";
        Cursor c = database.query(DatabaseHelper.TABLE_HABIT_LOGS,
                new String[]{DatabaseHelper.COLUMN_LOG_DATE, DatabaseHelper.COLUMN_LOG_COMPLETED},
                selection, new String[]{String.valueOf(habitId), startDate, endDate},
                null, null, null);
        if (c != null && c.moveToFirst()) {
            do {
                map.put(c.getString(0), c.getInt(1) == 1);
            } while (c.moveToNext());
            c.close();
        }
        return map;
    }

    public void updateHabit(Habit habit) {
        ContentValues v = new ContentValues();
        v.put(DatabaseHelper.COLUMN_HABIT_NAME, habit.getName());
        v.put(DatabaseHelper.COLUMN_HABIT_COMMITMENT, habit.getCommitmentDays());
        v.put(DatabaseHelper.COLUMN_HABIT_DEADLINE_TIME, habit.getDeadlineTime());
        v.put(DatabaseHelper.COLUMN_HABIT_HAS_TIME, habit.hasTime() ? 1 : 0);
        database.update(DatabaseHelper.TABLE_HABITS, v, DatabaseHelper.COLUMN_ID + " = ?",
                new String[]{String.valueOf(habit.getId())});
    }

    public int logHabit(int habitId, String dateStr, boolean completed, boolean onTime) {
        boolean wasCompleted = isHabitCompletedOnDate(habitId, dateStr);
        boolean wasOnTime = isHabitOnTimeOnDate(habitId, dateStr);
        ContentValues v = new ContentValues();
        v.put(DatabaseHelper.COLUMN_HABIT_ID, habitId);
        v.put(DatabaseHelper.COLUMN_LOG_DATE, dateStr);
        v.put(DatabaseHelper.COLUMN_LOG_COMPLETED, completed ? 1 : 0);
        v.put(DatabaseHelper.COLUMN_LOG_ON_TIME, onTime ? 1 : 0);
        long result = database.insertWithOnConflict(DatabaseHelper.TABLE_HABIT_LOGS, null, v, SQLiteDatabase.CONFLICT_REPLACE);
        if (result == -1) return 0;

        int requestedDelta = 0;
        if (!wasCompleted && completed) {
            requestedDelta = 10 + (onTime ? 10 : 0);
        } else if (wasCompleted && !completed) {
            requestedDelta = -10 - (wasOnTime ? 10 : 0);
        }
        int appliedDelta = adjustPoints(requestedDelta, CAT_HABITS);
        evaluateDailyStreak();
        return appliedDelta;
    }

    public Map<Integer, Integer> getAllHabitCompletionCounts() {
        Map<Integer, Integer> map = new HashMap<>();
        String query = "SELECT " + DatabaseHelper.COLUMN_HABIT_ID + ", COUNT(*) FROM " + DatabaseHelper.TABLE_HABIT_LOGS +
                " WHERE " + DatabaseHelper.COLUMN_LOG_COMPLETED + " = 1 GROUP BY " + DatabaseHelper.COLUMN_HABIT_ID;
        Cursor c = database.rawQuery(query, null);
        if (c != null && c.moveToFirst()) {
            do {
                map.put(c.getInt(0), c.getInt(1));
            } while (c.moveToNext());
            c.close();
        }
        return map;
    }

    public int getHabitTotalCompletions(int habitId) {
        Cursor c = database.rawQuery("SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_HABIT_LOGS + 
                " WHERE " + DatabaseHelper.COLUMN_HABIT_ID + " = ? AND " + DatabaseHelper.COLUMN_LOG_COMPLETED + " = 1",
                new String[]{String.valueOf(habitId)});
        int count = 0;
        if (c != null && c.moveToFirst()) { count = c.getInt(0); c.close(); }
        return count;
    }

    public boolean wasProductiveOnDate(String dateStr) {
        // 1. Tasks completed on dateStr
        String taskQuery = "SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_ENTRIES + 
                " WHERE " + DatabaseHelper.COLUMN_COMPLETED + " = 1 AND " +
                "date(" + DatabaseHelper.COLUMN_COMPLETED_AT + "/1000, 'unixepoch', 'localtime') = ? AND " +
                DatabaseHelper.COLUMN_TYPE + " = '*' AND " +
                DatabaseHelper.COLUMN_PARENT_ID + " = 0";
        Cursor c1 = database.rawQuery(taskQuery, new String[]{dateStr});
        if (c1 != null) {
            if (c1.moveToFirst() && c1.getInt(0) > 0) {
                c1.close();
                return true;
            }
            c1.close();
        }

        // 2. Habits completed on dateStr
        String habitQuery = "SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_HABIT_LOGS +
                " WHERE " + DatabaseHelper.COLUMN_LOG_DATE + " = ? AND " +
                DatabaseHelper.COLUMN_LOG_COMPLETED + " = 1";
        Cursor c2 = database.rawQuery(habitQuery, new String[]{dateStr});
        if (c2 != null) {
            if (c2.moveToFirst() && c2.getInt(0) > 0) {
                c2.close();
                return true;
            }
            c2.close();
        }

        // 3. Workouts logged on dateStr
        String workoutQuery = "SELECT COUNT(*) FROM " + DatabaseHelper.TABLE_WORKOUT_SETS +
                " WHERE " + DatabaseHelper.COLUMN_DATE_STR + " = ?";
        Cursor c3 = database.rawQuery(workoutQuery, new String[]{dateStr});
        if (c3 != null) {
            if (c3.moveToFirst() && c3.getInt(0) > 0) {
                c3.close();
                return true;
            }
            c3.close();
        }

        return false;
    }

    public AuditResult evaluateDailyStreak() {
        String todayStr = new SimpleDateFormat("yyyy-MM-dd", Locale.US).format(new Date());
        Cursor c = database.query(DatabaseHelper.TABLE_USER_STATS,
                new String[]{DatabaseHelper.COLUMN_LAST_ACTIVE_DATE, DatabaseHelper.COLUMN_CURRENT_STREAK,
                        DatabaseHelper.COLUMN_LONGEST_STREAK, DatabaseHelper.COLUMN_REST_TOKENS,
                        DatabaseHelper.COLUMN_LAST_PRODUCTIVE_DATE},
                null, null, null, null, null);
        if (c == null || !c.moveToFirst()) {
            if (c != null) c.close();
            return null;
        }

        String lastDateStr = c.getString(0);
        int currentStreak = c.getInt(1);
        int longestStreak = c.getInt(2);
        int tokens = c.getInt(3);
        String lastProductiveDate = c.getString(4);
        c.close();
        boolean productiveToday = wasProductiveOnDate(todayStr);

        // Older builds could leave a one-day streak behind on the first app launch,
        // before any task, habit, or workout had been completed. Repair only that
        // impossible first-day state; historical streaks survive clearing old entries.
        if (lastProductiveDate == null && todayStr.equals(lastDateStr)
                && currentStreak == 1 && longestStreak == 1
                && !productiveToday) {
            currentStreak = 0;
            longestStreak = 0;
            tokens = 0;
        } else if (todayStr.equals(lastProductiveDate) && !productiveToday) {
            currentStreak = 0;
            if (longestStreak == 1) longestStreak = 0;
            tokens = 0;
            lastProductiveDate = null;
        }

        AuditResult audit = null;
        if (lastDateStr != null && !lastDateStr.isEmpty() && !todayStr.equals(lastDateStr)) {
            audit = performDailyAudit(lastDateStr, todayStr);
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                Calendar cal = Calendar.getInstance();
                cal.setTime(sdf.parse(lastDateStr));
                Date today = sdf.parse(todayStr);
                // A productive last-evaluated day was already counted when it happened.
                // An unproductive one still needs to consume a token or end the streak.
                if (!wasProductiveOnDate(lastDateStr)) {
                    if (tokens > 0) tokens--;
                    else currentStreak = 0;
                }
                cal.add(Calendar.DAY_OF_YEAR, 1);
                // Process only later, previously un-evaluated days in chronological order.
                while (cal.getTime().before(today)) {
                    String dateCheck = sdf.format(cal.getTime());
                    if (wasProductiveOnDate(dateCheck)) {
                        currentStreak++;
                        longestStreak = Math.max(longestStreak, currentStreak);
                        if (currentStreak % 7 == 0) tokens = Math.min(2, tokens + 1);
                    } else if (tokens > 0) {
                        tokens--;
                    } else {
                        currentStreak = 0;
                    }
                    cal.add(Calendar.DAY_OF_YEAR, 1);
                }
            } catch (Exception e) {
                e.printStackTrace();
            }
        }

        if (productiveToday && !todayStr.equals(lastProductiveDate)) {
            currentStreak++;
            longestStreak = Math.max(longestStreak, currentStreak);
            if (currentStreak % 7 == 0) tokens = Math.min(2, tokens + 1);
            lastProductiveDate = todayStr;
        } else if (!productiveToday && todayStr.equals(lastProductiveDate)) {
            // A same-day undo must undo the streak increment as well. If that increment
            // created this week's token, remove it so toggling cannot farm rest tokens.
            if (currentStreak > 0 && currentStreak % 7 == 0 && tokens > 0) tokens--;
            currentStreak = Math.max(0, currentStreak - 1);
            lastProductiveDate = null;
        }

        updateStats(currentStreak, longestStreak, tokens, todayStr);
        markProductiveToday(lastProductiveDate);
        return audit;
    }

    private void markProductiveToday(String todayStr) {
        ContentValues cv = new ContentValues();
        cv.put(DatabaseHelper.COLUMN_LAST_PRODUCTIVE_DATE, todayStr);
        database.update(DatabaseHelper.TABLE_USER_STATS, cv, null, null);
    }

    private void updateStats(int streak, int longest, int tokens, String date) {
        ContentValues cv = new ContentValues();
        cv.put(DatabaseHelper.COLUMN_CURRENT_STREAK, streak);
        cv.put(DatabaseHelper.COLUMN_LONGEST_STREAK, longest);
        cv.put(DatabaseHelper.COLUMN_REST_TOKENS, tokens);
        cv.put(DatabaseHelper.COLUMN_LAST_ACTIVE_DATE, date);
        database.update(DatabaseHelper.TABLE_USER_STATS, cv, null, null);
    }

    public long insertWorkoutSet(WorkoutSet ws) {
        ContentValues values = new ContentValues();
        values.put(DatabaseHelper.COLUMN_DATE_STR, ws.getDateStr());
        values.put(DatabaseHelper.COLUMN_EXERCISE, ws.getExercise());
        values.put(DatabaseHelper.COLUMN_WEIGHT, ws.getWeight());
        values.put(DatabaseHelper.COLUMN_REPS, ws.getReps());
        values.put(DatabaseHelper.COLUMN_NOTE, ws.getNote());
        long id = database.insert(DatabaseHelper.TABLE_WORKOUT_SETS, null, values);
        if (id != -1) {
            adjustPoints(10, CAT_WORKOUTS);
            evaluateDailyStreak();
        }
        return id;
    }

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

    public int adjustPoints(int amount) {
        return adjustPoints(amount, CAT_TASKS);
    }

    public int adjustPoints(int amount, String category) {
        if (amount == 0) return 0;
        database.beginTransaction();
        try {
            int[] stats = getUserStats();
            
            // Check if user stats record exists
            Cursor check = database.query(DatabaseHelper.TABLE_USER_STATS, null,
                    null, null, null, null, null);
            if (check == null || !check.moveToFirst()) {
                // Initialize if missing
                ContentValues initial = new ContentValues();
                initial.put(DatabaseHelper.COLUMN_POINTS, 0);
                database.insert(DatabaseHelper.TABLE_USER_STATS, null, initial);
                stats = new int[7]; // Reset local stats array to 0s
            }
            if (check != null) check.close();

            int currentTasks = stats[4];
            int currentHabits = stats[5];
            int currentWorkouts = stats[6];

            int newTasks = currentTasks;
            int newHabits = currentHabits;
            int newWorkouts = currentWorkouts;
            if (CAT_HABITS.equals(category)) {
                newHabits = Math.max(0, currentHabits + amount);
            } else if (CAT_WORKOUTS.equals(category)) {
                newWorkouts = Math.max(0, currentWorkouts + amount);
            } else {
                newTasks = Math.max(0, currentTasks + amount);
            }
            int appliedDelta;
            if (CAT_HABITS.equals(category)) appliedDelta = newHabits - currentHabits;
            else if (CAT_WORKOUTS.equals(category)) appliedDelta = newWorkouts - currentWorkouts;
            else appliedDelta = newTasks - currentTasks;

            ContentValues cv = new ContentValues();
            // The breakdown is the source of truth; keeping the total as its sum prevents
            // deductions in one category from silently changing another category's XP.
            cv.put(DatabaseHelper.COLUMN_POINTS_TASKS, newTasks);
            cv.put(DatabaseHelper.COLUMN_POINTS_HABITS, newHabits);
            cv.put(DatabaseHelper.COLUMN_POINTS_WORKOUTS, newWorkouts);
            cv.put(DatabaseHelper.COLUMN_POINTS, newTasks + newHabits + newWorkouts);
            database.update(DatabaseHelper.TABLE_USER_STATS, cv, null, null);
            database.setTransactionSuccessful();
            return appliedDelta;
        } finally {
            database.endTransaction();
        }
    }

    public int[] getUserStats() {
        int[] stats = new int[7]; 
        Cursor cursor = database.query(DatabaseHelper.TABLE_USER_STATS,
                new String[]{DatabaseHelper.COLUMN_POINTS, DatabaseHelper.COLUMN_CURRENT_STREAK,
                        DatabaseHelper.COLUMN_LONGEST_STREAK, DatabaseHelper.COLUMN_REST_TOKENS,
                        DatabaseHelper.COLUMN_POINTS_TASKS, DatabaseHelper.COLUMN_POINTS_HABITS,
                        DatabaseHelper.COLUMN_POINTS_WORKOUTS},
                null, null, null, null, null);
        if (cursor != null && cursor.moveToFirst()) {
            for (int i = 0; i < 7; i++) stats[i] = cursor.getInt(i);
            cursor.close();
        }
        return stats;
    }

    public static boolean isToday(long timestamp) {
        if (timestamp <= 0) return false;
        Calendar cal = Calendar.getInstance();
        Calendar target = Calendar.getInstance();
        target.setTimeInMillis(timestamp);
        return cal.get(Calendar.YEAR) == target.get(Calendar.YEAR) &&
               cal.get(Calendar.DAY_OF_YEAR) == target.get(Calendar.DAY_OF_YEAR);
    }

    public int calculateCommitmentReward(Entry entry) {
        if (!"*".equals(entry.getSignifier()) || entry.getParentId() != 0) return 0;
        if (!isToday(entry.getDeadline())) return 0;
        return 5;
    }

    public int calculateCompletionReward(Entry entry) {
        if (!"*".equals(entry.getSignifier()) || entry.getParentId() != 0) return 0;
        int totalValue = calculateTotalTaskValue(entry);
        return Math.max(0, totalValue - 5);
    }

    public int calculateTotalTaskValue(Entry entry) {
        if (!"*".equals(entry.getSignifier()) || entry.getParentId() != 0) return 0;
        int weight = 1;
        if (entry.getProjectId() > 0) {
            Cursor c = database.query(DatabaseHelper.TABLE_PROJECTS, new String[]{DatabaseHelper.COLUMN_PROJECT_WEIGHT},
                    DatabaseHelper.COLUMN_ID + " = ?", new String[]{String.valueOf(entry.getProjectId())}, null, null, null);
            if (c != null && c.moveToFirst()) {
                weight = c.getInt(0);
                c.close();
            }
        }
        return 10 + (Math.max(1, weight) - 1) * 5;
    }

    public int calculatePenalty(Entry entry) {
        if (entry.getParentId() != 0 || !"*".equals(entry.getSignifier())) return 0;
        // Total audit penalty is 10 (Commitment 5 + Extra 5)
        return 5; 
    }

    public static class AuditResult {
        public int totalPenalty = 0;
        public int missedTasks = 0;
    }

    public AuditResult performDailyAudit(String lastActiveDateStr, String todayStr) {
        AuditResult result = new AuditResult();
        try {
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            Date lastDate = sdf.parse(lastActiveDateStr);
            Date todayDate = sdf.parse(todayStr);
            Calendar cal = Calendar.getInstance();
            cal.setTime(lastDate);
            while (cal.getTime().before(todayDate)) {
                String auditDate = sdf.format(cal.getTime());
                String query = "SELECT * FROM " + DatabaseHelper.TABLE_ENTRIES + 
                        " WHERE " + DatabaseHelper.COLUMN_COMPLETED + " = 0 AND " +
                        DatabaseHelper.COLUMN_IS_AUDITED + " = 0 AND " +
                        "date(" + DatabaseHelper.COLUMN_DEADLINE + "/1000, 'unixepoch', 'localtime') = ? AND " +
                        DatabaseHelper.COLUMN_TYPE + " = '*' AND " +
                        DatabaseHelper.COLUMN_PARENT_ID + " = 0";
                Cursor c = database.rawQuery(query, new String[]{auditDate});
                if (c != null && c.moveToFirst()) {
                    int projIdx = c.getColumnIndexOrThrow(DatabaseHelper.COLUMN_PROJECT_ID);
                    do {
                        Entry e = new Entry();
                        e.setSignifier("*");
                        e.setProjectId(c.getInt(projIdx));
                        int commitment = calculateCommitmentReward(e);
                        int penalty = calculatePenalty(e);
                        result.totalPenalty += (commitment + penalty);
                        result.missedTasks++;
                    } while (c.moveToNext());
                    c.close();
                    
                    // Mark as audited
                    ContentValues cvAudit = new ContentValues();
                    cvAudit.put(DatabaseHelper.COLUMN_IS_AUDITED, 1);
                    database.update(DatabaseHelper.TABLE_ENTRIES, cvAudit, 
                            "date(" + DatabaseHelper.COLUMN_DEADLINE + "/1000, 'unixepoch', 'localtime') = ? AND " +
                            DatabaseHelper.COLUMN_TYPE + " = '*' AND " +
                            DatabaseHelper.COLUMN_PARENT_ID + " = 0 AND " +
                            DatabaseHelper.COLUMN_IS_AUDITED + " = 0", new String[]{auditDate});
                }
                cal.add(Calendar.DAY_OF_YEAR, 1);
            }
            if (result.totalPenalty > 0) result.totalPenalty = -adjustPoints(-result.totalPenalty, CAT_TASKS);
        } catch (Exception e) { e.printStackTrace(); }
        return result;
    }

    public int deleteWorkoutSet(int id) {
        int deleted = database.delete(DatabaseHelper.TABLE_WORKOUT_SETS, DatabaseHelper.COLUMN_ID + " = ?", new String[]{String.valueOf(id)});
        return deleted > 0 ? adjustPoints(-10, CAT_WORKOUTS) : 0;
    }

    public List<Object> getGroupedDailyWorkouts(String dateStr) {
        List<Object> list = new ArrayList<>();
        Cursor cursor = database.query(DatabaseHelper.TABLE_WORKOUT_SETS, null,
                DatabaseHelper.COLUMN_DATE_STR + " = ?", new String[]{dateStr},
                null, null, DatabaseHelper.COLUMN_EXERCISE + " ASC, " + DatabaseHelper.COLUMN_ID + " ASC");
        if (cursor != null && cursor.moveToFirst()) {
            int idIdx = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID);
            int dateIdx = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DATE_STR);
            int exIdx = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_EXERCISE);
            int weightIdx = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_WEIGHT);
            int repsIdx = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_REPS);
            int noteIdx = cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_NOTE);
            String currentEx = "";
            int setNum = 1;
            do {
                WorkoutSet ws = new WorkoutSet();
                ws.setId(cursor.getInt(idIdx));
                ws.setDateStr(cursor.getString(dateIdx));
                ws.setExercise(cursor.getString(exIdx));
                ws.setWeight(cursor.getDouble(weightIdx));
                ws.setReps(cursor.getInt(repsIdx));
                ws.setNote(cursor.getString(noteIdx));
                if (!ws.getExercise().equalsIgnoreCase(currentEx)) {
                    currentEx = ws.getExercise();
                    setNum = 1;
                    double pr = getPersonalRecord(currentEx);
                    String prText = (pr == Math.floor(pr)) ? String.valueOf((int)pr) : String.format(Locale.US, "%.1f", pr);
                    list.add(currentEx.toUpperCase() + " (PR: " + prText + ")");
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
        names.add("Pull-ups"); names.add("Dumbbell Press");
        names.add("Mobility Flow"); names.add("Yoga");
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
            do { dates.add(cursor.getString(0)); } while (cursor.moveToNext());
            cursor.close();
        }
        return dates;
    }
}
