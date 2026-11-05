package com.dannicholson.climbingfitnessapp;

/*
 * Checks the text entered into the climb form.
 * This class doesn't need access to the screen or Android.
 */
public class ClimbValidator {

    public String getGradeError(String grade) {
        // Ignore spaces at the beginning and end.
        if (grade.trim().isEmpty()) {
            return "Enter a grade";
        }

        // No error. We'll check specific grading systems later.
        return null;
    }

    public String getAttemptsError(String attemptsText) {
        int attempts;

        try {
            // Convert the entered text into a whole number.
            attempts = Integer.parseInt(attemptsText.trim());
        } catch (NumberFormatException exception) {
            // Covers blank text, decimals and numbers too large for an int.
            return "Enter a valid whole number";
        }

        // A number can be valid Java input but still break our app's rule.
        if (attempts < 1) {
            return "Enter at least 1 attempt";
        }

        return null;
    }
}