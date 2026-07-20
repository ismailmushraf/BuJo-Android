package com.ismailmushraf.bujo.models;

public class Entry {
    private int id;
    private String signifier;    // "*", "-", "o"
    private String content;       // The actual logged text
    private String projectTag;    // Optional project tag (e.g., "ProjectPhoenix")
    private boolean isCompleted;
    private boolean isMigrated;
    private long deadline;
    private int projectId;
    private boolean hasTime;

    public Entry(int id, String signifier, String content, String projectTag, boolean isCompleted) {
        this.id = id;
        this.signifier = signifier;
        this.content = content;
        this.projectTag = projectTag;
        this.isCompleted = isCompleted;
        this.hasTime = false;
    }

    public Entry() {
    }

    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getSignifier() { return signifier; }
    public void setSignifier(String signifier) { this.signifier = signifier; }
    
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
}