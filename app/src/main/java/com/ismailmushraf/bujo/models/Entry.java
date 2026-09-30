package com.ismailmushraf.bujo.models;

public class Entry {
    private int id;
    private String content;       // The actual logged text
    private String projectTag;    // Optional project tag (e.g., "ProjectPhoenix")
    private boolean isCompleted;
    private boolean isMigrated;
    private long deadline;
    private int projectId;
    private boolean hasTime;
    private long completedAt;
    private long createdAt;
    private int parentId;
    private boolean isAudited;
    private boolean isLockedManually;

    public Entry(int id, String content, String projectTag, boolean isCompleted) {
        this.id = id;
        this.content = content;
        this.projectTag = projectTag;
        this.isCompleted = isCompleted;
        this.hasTime = false;
    }

    public Entry() {
    }

    public boolean isLocked() {
        if (isLockedManually) return true;
        
        if (!isMigrated) {
            // Only a task explicitly scheduled for Today can lock automatically.
            // Inbox/no-date tasks, overdue tasks, and future tasks remain editable.
            if (deadline <= 0 || isOverdue() || isFuture()) {
                return false;
            }
            // Tasks scheduled for Today lock after 3 hours
            long threeHoursInMillis = 3 * 60 * 60 * 1000L;
            return (System.currentTimeMillis() - createdAt) > threeHoursInMillis;
        }
        return false;
    }

    public boolean isOverdue() {
        if (deadline <= 0) return false;
        java.util.Calendar today = java.util.Calendar.getInstance();
        today.set(java.util.Calendar.HOUR_OF_DAY, 0);
        today.set(java.util.Calendar.MINUTE, 0);
        today.set(java.util.Calendar.SECOND, 0);
        today.set(java.util.Calendar.MILLISECOND, 0);
        return deadline < today.getTimeInMillis();
    }

    public boolean isFuture() {
        if (deadline <= 0) return false;
        java.util.Calendar today = java.util.Calendar.getInstance();
        today.set(java.util.Calendar.HOUR_OF_DAY, 23);
        today.set(java.util.Calendar.MINUTE, 59);
        today.set(java.util.Calendar.SECOND, 59);
        today.set(java.util.Calendar.MILLISECOND, 999);
        return deadline > today.getTimeInMillis();
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public String getProjectTag() { return projectTag; }
    public void setProjectTag(String projectTag) { this.projectTag = projectTag; }
    
    public boolean isCompleted() { return isCompleted; }
    public void setCompleted(boolean completed) { isCompleted = completed; }
    
    public boolean isMigrated() { return isMigrated; }
    public void setMigrated(boolean migrated) { isMigrated = migrated; }
    
    public long getDeadline() { return deadline; }
    public void setDeadline(long deadline) { this.deadline = deadline; }
    
    public int getProjectId() { return projectId; }
    public void setProjectId(int projectId) { this.projectId = projectId; }

    public boolean hasTime() { return hasTime; }
    public void setHasTime(boolean hasTime) { this.hasTime = hasTime; }

    public long getCompletedAt() { return completedAt; }
    public void setCompletedAt(long completedAt) { this.completedAt = completedAt; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public int getParentId() { return parentId; }
    public void setParentId(int parentId) { this.parentId = parentId; }

    public boolean isAudited() { return isAudited; }
    public void setAudited(boolean audited) { this.isAudited = audited; }

    public boolean isLockedManually() { return isLockedManually; }
    public void setLockedManually(boolean lockedManually) { isLockedManually = lockedManually; }
}
