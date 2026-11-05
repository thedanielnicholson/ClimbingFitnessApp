package com.dannicholson.climbingfitnessapp;

import java.util.List;

/*
 * Handles saving and retrieving climbs.
 * The screen can use this class without building database rows itself.
 *
 * These methods must be called on a background thread.
 */
public class ClimbRepository {

    private final ClimbDao climbDao;

    public ClimbRepository(ClimbDao climbDao) {
        // Accept the DAO rather than opening a database here.
        // This lets the app and tests supply their own database.
        this.climbDao = climbDao;
    }

    public long save(Climb climb) {
        // Copy the checked climb details into a database row.
        ClimbEntity entity = new ClimbEntity(
                climb.getGrade(),
                climb.getAttempts(),
                climb.getNotes()
        );

        // Room saves the row and returns its new ID.
        return climbDao.insert(entity);
    }

    public List<ClimbEntity> getAll() {
        // Return the saved rows, newest first.
        return climbDao.getAll();
    }
}