package com.dannicholson.climbingfitnessapp;

import androidx.room.Database;
import androidx.room.RoomDatabase;

/*
 * Tells Room which tables belong to this database
 * and provides access to their operations.
 */
@Database(
        entities = {ClimbEntity.class},
        version = 1,
        exportSchema = false
)
public abstract class ClimbDatabase extends RoomDatabase {

    // Room supplies the implementation when the project builds.
    public abstract ClimbDao climbDao();
}