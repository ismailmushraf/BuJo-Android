package com.ismailmushraf.bujo.models;

public class Habit {
    private int id;
    private String name;
    private int commitmentDays;
    private String startDate; // YYYY-MM-DD
    private long createdAt;
    private long deadlineTime;
    private boolean hasTime;

    public Habit() {}

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }

    public String getName() { return name; }
    public void setName(String name) { this.name = name; }

    public int getCommitmentDays() { return commitmentDays; }
    public void setCommitmentDays(int commitmentDays) { this.commitmentDays = commitmentDays; }

    public String getStartDate() { return startDate; }
    public void setStartDate(String startDate) { this.startDate = startDate; }

    public long getCreatedAt() { return createdAt; }
    public void setCreatedAt(long createdAt) { this.createdAt = createdAt; }

    public long getDeadlineTime() { return deadlineTime; }
    public void setDeadlineTime(long deadlineTime) { this.deadlineTime = deadlineTime; }

    public boolean hasTime() { return hasTime; }
    public void setHasTime(boolean hasTime) { this.hasTime = hasTime; }
}
