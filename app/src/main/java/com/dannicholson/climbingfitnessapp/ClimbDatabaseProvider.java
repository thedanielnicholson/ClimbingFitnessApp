package com.dannicholson.climbingfitnessapp;

import android.content.Context;

import androidx.room.Room;

/*
 * Shares one app database instead of opening a new connection
 * every time the screen is created.
 */
public final class ClimbDatabaseProvider {

    private static ClimbDatabase appDatabase;
    private static ClimbDatabase testDatabase;

    private ClimbDatabaseProvider() {
        // Other classes use the static methods below.
    }

    public static synchronized ClimbDatabase getDatabase(Context context) {
        // UI tests can supply their own separate database.
        if (testDatabase != null) {
            return testDatabase;
        }

        if (appDatabase == null) {
            // Use the application context so we don't retain a screen.
            appDatabase = Room.databaseBuilder(
                    context.getApplicationContext(),
                    ClimbDatabase.class,
                    "climbing-fitness.db"
            ).build();
        }

        return appDatabase;
    }

    // Used by our tests. Passing null restores normal app behaviour.
    static synchronized void setTestDatabase(ClimbDatabase database) {
        testDatabase = database;
    }
}