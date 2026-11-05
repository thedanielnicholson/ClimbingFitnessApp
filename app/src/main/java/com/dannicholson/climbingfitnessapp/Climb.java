package com.dannicholson.climbingfitnessapp;

/*
 * Holds the details of one climb.
 * This class doesn't draw anything on screen or save to a database.
 */
public class Climb {

    // Keep the details private so other classes use the methods below.
    // final means these values can't be reassigned after construction.
    private final String grade;
    private final int attempts;
    private final String notes;

    public Climb(String grade, int attempts, String notes) {
        /*
         * Check the details here as well as on the form.
         * Later, climbs might also come from imports or other screens.
         * They should follow the same rules.
         */
        ClimbValidator validator = new ClimbValidator();

        // Treat a missing grade as blank so the validator can check it.
        String gradeText = grade == null ? "" : grade;
        String gradeError = validator.getGradeError(gradeText);

        if (gradeError != null) {
            throw new IllegalArgumentException(gradeError);
        }

        // Reuse the existing attempt-count rule.
        String attemptsError =
                validator.getAttemptsError(Integer.toString(attempts));

        if (attemptsError != null) {
            throw new IllegalArgumentException(attemptsError);
        }

        // Store the checked details, removing spaces from the text.
        this.grade = gradeText.trim();
        this.attempts = attempts;

        // Notes are optional. Use empty text when none were supplied.
        this.notes = notes == null ? "" : notes.trim();
    }

    // These methods let other classes read this climb's details.
    public String getGrade() {
        return grade;
    }

    public int getAttempts() {
        return attempts;
    }

    public String getNotes() {
        return notes;
    }
}