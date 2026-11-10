package com.dannicholson.climbingfitnessapp;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

/*
 * Lists the operations we can perform on saved climbs.
 * Room generates the code that carries them out.
 */
@Dao
public interface ClimbDao {

    // Save a row and return its generated ID.
    @Insert
    long insert(ClimbEntity climb);

    // Read every saved climb, with the most recently inserted first.
    @Query("SELECT * FROM climbs ORDER BY id DESC")
    List<ClimbEntity> getAll();

    // Update only the row with the selected climb's ID.
    @Query("UPDATE climbs SET grade = :grade, attempts = :attempts, "
            + "notes = :notes WHERE id = :id")
    int update(long id, String grade, int attempts, String notes);

    // Delete only the row with the selected climb's ID.
    @Query("DELETE FROM climbs WHERE id = :id")
    int deleteById(long id);

    @Query("SELECT COUNT(*) FROM climbs "
            + "WHERE loggedDate >= :startDate AND loggedDate < :endDate")
    int countLoggedBetween(String startDate, String endDate);
}