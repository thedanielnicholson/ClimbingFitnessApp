package com.dannicholson.climbingfitnessapp;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.Test;
import org.junit.runner.RunWith;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertNull;

@RunWith(AndroidJUnit4.class)
public class WorkoutMigrationTest {

    @Test
    public void upgrade_keepsClimbsAndAllowsWorkoutLogging() {
        Context context = ApplicationProvider.getApplicationContext();
        String testFile = "workout-migration-test.db";
        context.deleteDatabase(testFile);

        try {
            // Recreate the version 1 table and seed it with a saved climb.
            try (SQLiteDatabase old = context.openOrCreateDatabase(
                    testFile, Context.MODE_PRIVATE, null)) {
                old.execSQL("CREATE TABLE climbs ("
                        + "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, "
                        + "grade TEXT, attempts INTEGER NOT NULL, notes TEXT)");
                old.execSQL("INSERT INTO climbs (id, grade, attempts, notes) "
                        + "VALUES (7, 'V3', 4, 'Overhang')");
                old.setVersion(1);
            }

            ClimbDatabase upgraded = Room.databaseBuilder(
                    context, ClimbDatabase.class, testFile
            ).addMigrations(
                    ClimbDatabase.MIGRATION_1_2,
                    ClimbDatabase.MIGRATION_2_3
            ).build();

            try {
                ClimbEntity climb = upgraded.climbDao().getAll().get(0);
                assertEquals(7L, climb.id);
                assertEquals("V3", climb.grade);
                assertEquals(4, climb.attempts);
                assertEquals("Overhang", climb.notes);
                assertNull(climb.loggedDate); // This climb was saved before dates were recorded.

                long workoutId = upgraded.workoutDao().insert(
                        new WorkoutEntity("Pull-up", 3, 5, 7.5)
                );

                WorkoutEntity workout = upgraded.workoutDao().getAll().get(0);
                assertEquals(workoutId, workout.id);
                assertEquals("Pull-up", workout.exercise);
                assertEquals(3, workout.sets);
                assertEquals(5, workout.reps);
                assertEquals(7.5, workout.weightKg, 0.001);
            } finally {
                upgraded.close();
            }
        } finally {
            context.deleteDatabase(testFile);
        }
    }
}