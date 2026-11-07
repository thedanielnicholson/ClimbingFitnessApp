package com.dannicholson.climbingfitnessapp;

import androidx.annotation.NonNull;
import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "workouts")
public class WorkoutEntity {

    @PrimaryKey(autoGenerate = true)
    public long id;

    @NonNull
    public String exercise;
    public int sets;
    public int reps;
    public double weightKg;
    // ISO date, such as 2026-11-07. Older entries have no recorded date.
    public String loggedDate;

    public WorkoutEntity(@NonNull String exercise, int sets, int reps,
                         double weightKg) {
        this.exercise = exercise;
        this.sets = sets;
        this.reps = reps;
        this.weightKg = weightKg;
    }
}