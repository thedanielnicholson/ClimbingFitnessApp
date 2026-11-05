package com.dannicholson.climbingfitnessapp;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertThrows;

public class ClimbTest {

    @Test
    public void validClimb_keepsEnteredDetails() {
        // Create a climb, then check that its details are available.
        Climb climb = new Climb("V3", 4, "Steep overhang");

        assertEquals("V3", climb.getGrade());
        assertEquals(4, climb.getAttempts());
        assertEquals("Steep overhang", climb.getNotes());
    }

    @Test
    public void spacesAroundText_areRemoved() {
        // Extra spaces shouldn't become part of the saved details.
        Climb climb = new Climb(" V3 ", 4, " Good session ");

        assertEquals("V3", climb.getGrade());
        assertEquals("Good session", climb.getNotes());
    }

    @Test
    public void missingNotes_becomeEmptyText() {
        // Notes are optional, including when another part of the app
        // supplies null instead of an empty string.
        Climb climb = new Climb("V1", 1, null);

        assertEquals("", climb.getNotes());
    }

    @Test
    public void blankGrade_isRejected() {
        // Invalid details should stop the object being created.
        assertThrows(
                IllegalArgumentException.class,
                () -> new Climb("   ", 2, "")
        );
    }

    @Test
    public void missingGrade_isRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Climb(null, 2, "")
        );
    }

    @Test
    public void zeroAttempts_isRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Climb("V3", 0, "")
        );
    }

    @Test
    public void negativeAttempts_isRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new Climb("V3", -1, "")
        );
    }
}