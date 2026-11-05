package com.dannicholson.climbingfitnessapp;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

/*
 * Describes one row in the climbs table.
 * Room reads and writes these fields.
 */
@Entity(tableName = "climbs")
public class ClimbEntity {

    // Each saved climb gets its own ID, even if its details match another.
    @PrimaryKey(autoGenerate = true)
    public long id;

    public String grade;
    public int attempts;
    public String notes;

    public ClimbEntity(String grade, int attempts, String notes) {
        this.grade = grade;
        this.attempts = attempts;
        this.notes = notes;
    }
}