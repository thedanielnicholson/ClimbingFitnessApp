package com.dannicholson.climbingfitnessapp;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface WorkoutDao {

    @Insert
    long insert(WorkoutEntity workout);

    // Show the most recently logged exercises first.
    @Query("SELECT * FROM workouts ORDER BY id DESC")
    List<WorkoutEntity> getAll();
}