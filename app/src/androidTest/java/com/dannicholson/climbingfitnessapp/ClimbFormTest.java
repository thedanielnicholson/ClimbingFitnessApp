package com.dannicholson.climbingfitnessapp;

// Lets a test launch and close an Android activity.
import androidx.test.core.app.ActivityScenario;

// Runs these tests using Android's JUnit test runner.
import androidx.test.ext.junit.runners.AndroidJUnit4;

// JUnit annotations identify tests and setup/cleanup methods.
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

// Espresso finds screen components using onView().
import static androidx.test.espresso.Espresso.onView;

// Actions simulate what a user does on the screen.
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;

// Assertions check whether the screen matches our expectations.
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasErrorText;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;

/*
 * Automated UI tests for the climb-entry form.
 *
 * Each test opens the activity, interacts with the form, and checks
 * either the displayed summary or an input-validation error.
 *
 * These tests run on an emulator or Android device.
 */
@RunWith(AndroidJUnit4.class)
public class ClimbFormTest {

    // Holds the running activity so we can close it after each test.
    private ActivityScenario<MainActivity> scenario;

    @Before
    public void openApp() {
        /*
         * Runs before EVERY test.
         * Launching a fresh activity prevents text and errors from a
         * previous test affecting the next one.
         *
         * This does not clear a database or other permanent storage.
         */
        scenario = ActivityScenario.launch(MainActivity.class);
    }

    @After
    public void closeApp() {
        // Runs after each test, including tests whose assertions fail.
        // The null check handles cases where launching did not complete.
        if (scenario != null) {
            scenario.close();
        }
    }

    @Test
    public void validClimb_displaysSummary() {
        // Act: enter a complete, valid climb and press Preview.
        enterClimb("V3", "4", "Steep overhang");

        /*
         * Assert: the summary contains exactly the expected information.
         * \n represents a new line in the displayed text.
         */
        onView(withId(R.id.summaryText))
                .check(matches(withText(
                        "Grade: V3\nAttempts: 4\nNotes: Steep overhang"
                )));
    }

    @Test
    public void blankGrade_displaysError() {
        // An empty string represents leaving the grade field blank.
        enterClimb("", "4", "");

        // Check that the error belongs to the grade field.
        onView(withId(R.id.gradeInput))
                .check(matches(hasErrorText("Enter a grade")));

        // Invalid input must not produce a climb summary.
        checkSummaryIsEmpty();
    }

    @Test
    public void blankAttempts_displaysError() {
        // A grade is provided, but the required attempt count is missing.
        enterClimb("V3", "", "");

        // Check that the form explains the problem on the correct field.
        onView(withId(R.id.attemptsInput))
                .check(matches(hasErrorText(
                        "Enter a valid whole number"
                )));

        checkSummaryIsEmpty();
    }

    @Test
    public void zeroAttempts_displaysError() {
        /*
         * Zero can be converted into an integer, but it breaks our
         * current rule that a logged climb needs at least one attempt.
         *
         * This checks a business rule rather than number conversion.
         */
        enterClimb("V3", "0", "");

        onView(withId(R.id.attemptsInput))
                .check(matches(hasErrorText(
                        "Enter at least 1 attempt"
                )));

        checkSummaryIsEmpty();
    }

    @Test
    public void blankNotes_areAllowed() {
        // Notes are optional, so leaving them blank should still work.
        // One attempt also checks the lowest permitted attempt count.
        enterClimb("V1", "1", "");

        // The summary should show "None" for the missing notes.
        onView(withId(R.id.summaryText))
                .check(matches(withText(
                        "Grade: V1\nAttempts: 1\nNotes: None"
                )));
    }

    @Test
    public void excessivelyLargeAttempts_displaysError() {
        /*
         * This contains digits, but is too large for Java's int type.
         * The app should show an error instead of crashing when parsing it.
         */
        enterClimb("V3", "99999999999999999999", "");

        onView(withId(R.id.attemptsInput))
                .check(matches(hasErrorText(
                        "Enter a valid whole number"
                )));

        checkSummaryIsEmpty();
    }

    /*
     * Shared helper for filling in the form.
     *
     * Keeping these actions in one method avoids repeating the same
     * interaction code in every test. Each test supplies its own values.
     */
    private void enterClimb(
            String grade,
            String attempts,
            String notes
    ) {
        /*
         * withId() identifies the component by its XML ID.
         * scrollTo() brings it into view within the ScrollView.
         * replaceText() replaces the field's contents with our test value.
         * closeSoftKeyboard() hides the keyboard before the next action.
         */
        onView(withId(R.id.gradeInput))
                .perform(scrollTo(), replaceText(grade),
                        closeSoftKeyboard());

        onView(withId(R.id.attemptsInput))
                .perform(scrollTo(), replaceText(attempts),
                        closeSoftKeyboard());

        onView(withId(R.id.notesInput))
                .perform(scrollTo(), replaceText(notes),
                        closeSoftKeyboard());

        // Submit the form just as a user would by pressing the button.
        onView(withId(R.id.previewButton))
                .perform(scrollTo(), click());
    }

    private void checkSummaryIsEmpty() {
        // Shared assertion used when invalid input should block a preview.
        onView(withId(R.id.summaryText))
                .check(matches(withText("")));
    }
}