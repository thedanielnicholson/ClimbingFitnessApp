package com.dannicholson.climbingfitnessapp;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Delete;
import androidx.room.Update;
import java.util.List;

@Dao
public interface WorkoutDao {

    @Insert
    long insert(WorkoutEntity workout);

    // Show the most recently logged exercises first.
    @Query("SELECT * FROM workouts ORDER BY id DESC")
    List<WorkoutEntity> getAll();

    @Update
    int update(WorkoutEntity workout);

    @Delete
    int delete(WorkoutEntity workout);
}