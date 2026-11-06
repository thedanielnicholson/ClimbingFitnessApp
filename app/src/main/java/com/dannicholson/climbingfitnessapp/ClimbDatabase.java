package com.dannicholson.climbingfitnessapp;

import androidx.annotation.NonNull;
import androidx.room.Database;
import androidx.room.RoomDatabase;
import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;

@Database(
        entities = {ClimbEntity.class, WorkoutEntity.class},
        version = 2,
        exportSchema = false
)
public abstract class ClimbDatabase extends RoomDatabase {

    public static final Migration MIGRATION_1_2 = new Migration(1, 2) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {
            // Add the new table alongside the existing climbs.
            database.execSQL(
                    "CREATE TABLE workouts ("
                            + "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                            + "exercise TEXT NOT NULL, "
                            + "sets INTEGER NOT NULL, "
                            + "reps INTEGER NOT NULL, "
                            + "weightKg REAL NOT NULL)"
            );
        }
    };

    public abstract ClimbDao climbDao();
    public abstract WorkoutDao workoutDao();
}