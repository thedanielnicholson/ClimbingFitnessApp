package com.dannicholson.climbingfitnessapp;

import org.junit.Test;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNull;

public class ClimbValidatorTest {

    // Use the same validator throughout these tests.
    private final ClimbValidator validator = new ClimbValidator();

    @Test
    public void filledGrade_hasNoError() {
        // A filled grade field should pass our current check.
        assertNull(validator.getGradeError("V3"));
    }

    @Test
    public void blankGrade_returnsError() {
        assertEquals(
                "Enter a grade",
                validator.getGradeError("")
        );
    }

    @Test
    public void spacesOnlyGrade_returnsError() {
        // Spaces shouldn't count as entering a grade.
        assertEquals(
                "Enter a grade",
                validator.getGradeError("   ")
        );
    }

    @Test
    public void oneAttempt_hasNoError() {
        // One is the lowest attempt count we currently allow.
        assertNull(validator.getAttemptsError("1"));
    }

    @Test
    public void blankAttempts_returnsError() {
        assertEquals(
                "Enter a valid whole number",
                validator.getAttemptsError("")
        );
    }

    @Test
    public void zeroAttempts_returnsError() {
        assertEquals(
                "Enter at least 1 attempt",
                validator.getAttemptsError("0")
        );
    }

    @Test
    public void negativeAttempts_returnsError() {
        // Check the rule even if the keyboard normally prevents this.
        assertEquals(
                "Enter at least 1 attempt",
                validator.getAttemptsError("-1")
        );
    }

    @Test
    public void decimalAttempts_returnsError() {
        // An attempt count must be a whole number.
        assertEquals(
                "Enter a valid whole number",
                validator.getAttemptsError("2.5")
        );
    }

    @Test
    public void excessivelyLargeAttempts_returnsError() {
        // This number is too large for Java's int type.
        assertEquals(
                "Enter a valid whole number",
                validator.getAttemptsError("99999999999999999999")
        );
    }

    @Test
    public void spacesAroundAttempts_areIgnored() {
        assertNull(validator.getAttemptsError(" 4 "));
    }
}