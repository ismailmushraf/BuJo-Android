package com.ismailmushraf.bujo.models;

public class Entry {
    private int id;
    private String signifier;    // "*", "-", "o"
    private String content;       // The actual logged text
    private String projectTag;    // Optional project tag (e.g., "ProjectPhoenix")
    private boolean isCompleted;

    public Entry(int id, String signifier, String content, String projectTag, boolean isCompleted) {
        this.id = id;
        this.signifier = signifier;
        this.content = content;
        this.projectTag = projectTag;
        this.isCompleted = isCompleted;
    }

    // Getters and Setters
    public int getId() { return id; }
    public String getSignifier() { return signifier; }
    public String getContent() { return content; }
    public String getProjectTag() { return projectTag; }
    public boolean isCompleted() { return isCompleted; }

    public void setCompleted(boolean completed) { isCompleted = completed; }
}