package com.ismailmushraf.bujo.models;

public class WorkoutSet {
    private int id;
    private String dateStr;
    private String exercise;
    private double weight;
    private int reps;
    private String note;
    private int setNumber; // Calculated on the fly for UI

    public WorkoutSet() {}

    public WorkoutSet(String dateStr, String exercise, double weight, int reps, String note) {
        this.dateStr = dateStr;
        this.exercise = exercise;
        this.weight = weight;
        this.reps = reps;
        this.note = note;
    }

    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    public String getDateStr() { return dateStr; }
    public void setDateStr(String dateStr) { this.dateStr = dateStr; }
    public String getExercise() { return exercise; }
    public void setExercise(String exercise) { this.exercise = exercise; }
    public double getWeight() { return weight; }
    public void setWeight(double weight) { this.weight = weight; }
    public int getReps() { return reps; }
    public void setReps(int reps) { this.reps = reps; }
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    public int getSetNumber() { return setNumber; }
    public void setSetNumber(int setNumber) { this.setNumber = setNumber; }
}