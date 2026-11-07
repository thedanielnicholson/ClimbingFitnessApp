package com.dannicholson.climbingfitnessapp;

import java.util.List;
import java.time.LocalDate;

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
        entity.loggedDate = LocalDate.now().toString();
        // Room saves the row and returns its new ID.
        return climbDao.insert(entity);
    }

    public List<ClimbEntity> getAll() {
        // Return the saved rows, newest first.
        return climbDao.getAll();
    }

    public void update(long id, Climb climb) {
        int updatedRows = climbDao.update(
                id, climb.getGrade(), climb.getAttempts(), climb.getNotes()
        );

        if (updatedRows != 1) {
            throw new IllegalStateException("The selected climb could not be found.");
        }
    }

    public void delete(long id) {
        int deletedRows = climbDao.deleteById(id);

        if (deletedRows != 1) {
            throw new IllegalStateException("The selected climb could not be found.");
        }
    }
}