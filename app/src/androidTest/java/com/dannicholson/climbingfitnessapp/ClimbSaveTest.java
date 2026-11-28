package com.dannicholson.climbingfitnessapp;

import android.content.Context;
import androidx.room.Room;
import androidx.test.core.app.ActivityScenario;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.espresso.Espresso;
import androidx.test.espresso.IdlingRegistry;
import androidx.test.espresso.IdlingResource;
import androidx.test.ext.junit.runners.AndroidJUnit4;
import static androidx.test.espresso.matcher.RootMatchers.isDialog;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import static androidx.test.espresso.Espresso.onView;
import static androidx.test.espresso.action.ViewActions.click;
import static androidx.test.espresso.action.ViewActions.closeSoftKeyboard;
import static androidx.test.espresso.action.ViewActions.replaceText;
import static androidx.test.espresso.action.ViewActions.scrollTo;
import static androidx.test.espresso.assertion.ViewAssertions.matches;
import static androidx.test.espresso.matcher.ViewMatchers.hasErrorText;
import static androidx.test.espresso.matcher.ViewMatchers.withId;
import static androidx.test.espresso.matcher.ViewMatchers.withText;
import static org.junit.Assert.assertTrue;
import static androidx.test.espresso.assertion.ViewAssertions.doesNotExist;
import static androidx.test.espresso.matcher.ViewMatchers.isDisplayed;
import static org.hamcrest.Matchers.containsString;

@RunWith(AndroidJUnit4.class)
public class ClimbSaveTest {

    private static final String TEST_DATABASE = "climb-save-ui-test";

    private Context context;
    private ClimbDatabase database;
    private ActivityScenario<MainActivity> scenario;
    private IdlingResource databaseWork;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();

        // Each test starts with an empty, separate database.
        context.deleteDatabase(TEST_DATABASE);

        database = Room.databaseBuilder(
                context,
                ClimbDatabase.class,
                TEST_DATABASE
        ).build();

        ClimbDatabaseProvider.setTestDatabase(database);
        openScreen();
    }

    @After
    public void tearDown() {
        try {
            closeScreen();
        } finally {
            // Restore normal database selection and remove test records.
            ClimbDatabaseProvider.setTestDatabase(null);

            if (database != null) {
                database.close();
            }

            if (context != null) {
                context.deleteDatabase(TEST_DATABASE);
            }
        }
    }

    @Test
    public void savedClimb_appearsAfterReopeningScreen() {
        onView(withId(R.id.logClimbButton)).perform(scrollTo(), click());
        onView(withId(R.id.gradeInput))
                .perform(scrollTo(), replaceText("V3"),
                        closeSoftKeyboard());

        onView(withId(R.id.attemptsInput))
                .perform(scrollTo(), replaceText("4"),
                        closeSoftKeyboard());

        onView(withId(R.id.notesInput))
                .perform(scrollTo(), replaceText("Steep overhang"),
                        closeSoftKeyboard());

        onView(withId(R.id.saveButton))
                .perform(scrollTo(), click());

        // Espresso waits for the save and screen update to finish.
        onView(withText("V3 · 4 attempts")).perform(scrollTo()).check(matches(isDisplayed()));

        // Close and open the screen to exercise its history-loading path.
        closeScreen();
        openScreen();

        // After reopening, the saved climb should have a tappable history row.
        onView(withText("V3 · 4 attempts"))
                .perform(scrollTo(), click());

        // Check that tapping the row shows the details we saved.
        onView(withText(containsString(
                "Grade: V3\nAttempts: 4\nNotes: Steep overhang\nLogged: "
        )))
                .inRoot(isDialog())
                .check(matches(isDisplayed()));

        onView(withText(containsString("Logged: Date unknown")))
                .inRoot(isDialog())
                .check(doesNotExist());
    }

    @Test
    public void invalidClimb_isNotSaved() {
        onView(withId(R.id.logClimbButton)).perform(scrollTo(), click());
        // Leave the required fields blank and press Save.
        onView(withId(R.id.saveButton))
                .perform(scrollTo(), click());

        onView(withId(R.id.gradeInput))
                .check(matches(hasErrorText("Enter a grade")));

        Espresso.pressBack();
        onView(withId(R.id.historyText))
                .check(matches(withText("No climbs saved yet.")));

        // Check the database too, rather than only trusting the screen.
        assertTrue(database.climbDao().getAll().isEmpty());
    }

    @Test
    public void workoutSave_returnsToHistoryAndPersists() {
        onView(withId(R.id.logWorkoutButton)).perform(scrollTo(), click());
        onView(withId(R.id.exerciseInput)).perform(replaceText("Pull-up"), closeSoftKeyboard());
        onView(withId(R.id.setsInput)).perform(replaceText("3"), closeSoftKeyboard());
        onView(withId(R.id.repsInput)).perform(replaceText("5"), closeSoftKeyboard());
        onView(withId(R.id.weightInput)).perform(replaceText("7,5"), closeSoftKeyboard());
        onView(withId(R.id.saveButton)).perform(scrollTo(), click());
        onView(withText("Pull-up · 3 × 5 · 7.5 kg")).perform(scrollTo())
                .check(matches(isDisplayed()));
        closeScreen();
        openScreen();
        onView(withText("Pull-up · 3 × 5 · 7.5 kg")).perform(scrollTo())
                .check(matches(isDisplayed()));
        org.junit.Assert.assertEquals(1, database.workoutDao().getAll().size());
        org.junit.Assert.assertEquals(java.time.LocalDate.now().toString(),
                database.workoutDao().getAll().get(0).loggedDate);
    }

    @Test
    public void cancelClimb_discardsEntry() {
        onView(withId(R.id.logClimbButton)).perform(scrollTo(), click());
        onView(withId(R.id.gradeInput)).perform(replaceText("V4"), closeSoftKeyboard());
        onView(withId(R.id.cancelButton)).perform(scrollTo(), click());
        onView(withId(R.id.logClimbButton)).check(matches(isDisplayed()));
        assertTrue(database.climbDao().getAll().isEmpty());
    }

    @Test
    public void invalidWorkout_staysOpenAndBackDiscardsIt() {
        onView(withId(R.id.logWorkoutButton)).perform(scrollTo(), click());
        onView(withId(R.id.exerciseInput)).perform(replaceText("Pull-up"), closeSoftKeyboard());
        onView(withId(R.id.setsInput)).perform(replaceText("0"), closeSoftKeyboard());
        onView(withId(R.id.saveButton)).perform(scrollTo(), click());
        onView(withId(R.id.setsInput)).check(matches(
                hasErrorText("Enter a whole number greater than 0")));
        onView(withId(R.id.setsInput)).perform(closeSoftKeyboard());
        Espresso.pressBack();
        onView(withId(R.id.logWorkoutButton)).check(matches(isDisplayed()));
        assertTrue(database.workoutDao().getAll().isEmpty());
    }

    @Test
    public void selectedClimbDate_isSaved() {
        java.time.LocalDate yesterday = java.time.LocalDate.now().minusDays(1);
        onView(withId(R.id.logClimbButton)).perform(scrollTo(), click());
        onView(withId(R.id.gradeInput)).perform(replaceText("V2"), closeSoftKeyboard());
        onView(withId(R.id.attemptsInput)).perform(replaceText("2"), closeSoftKeyboard());
        onView(withId(R.id.loggedDateButton)).perform(scrollTo(), click());
        onView(androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom(
                android.widget.DatePicker.class)).perform(new androidx.test.espresso.ViewAction() {
            @Override public org.hamcrest.Matcher<android.view.View> getConstraints() {
                return androidx.test.espresso.matcher.ViewMatchers.isAssignableFrom(
                        android.widget.DatePicker.class);
            }
            @Override public String getDescription() { return "Choose yesterday"; }
            @Override public void perform(androidx.test.espresso.UiController controller,
                    android.view.View view) {
                ((android.widget.DatePicker) view).updateDate(yesterday.getYear(),
                        yesterday.getMonthValue() - 1, yesterday.getDayOfMonth());
                controller.loopMainThreadUntilIdle();
            }
        });
        onView(withId(android.R.id.button1)).inRoot(isDialog()).perform(click());
        onView(withId(R.id.saveButton)).perform(scrollTo(), click());
        onView(withText("V2 · 2 attempts")).perform(scrollTo()).check(matches(isDisplayed()));
        org.junit.Assert.assertEquals(yesterday.toString(),
                database.climbDao().getAll().get(0).loggedDate);
    }

    private void openScreen() {
        scenario = ActivityScenario.launch(MainActivity.class);

        scenario.onActivity(activity -> {
            databaseWork = activity.getDatabaseWork();
            IdlingRegistry.getInstance().register(databaseWork);
        });
    }

    private void closeScreen() {
        if (scenario != null) {
            // Finish pending work before closing or changing databases.
            Espresso.onIdle();

            IdlingRegistry.getInstance().unregister(databaseWork);
            scenario.close();
            scenario = null;
        }
    }
}