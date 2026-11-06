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
        onView(withId(R.id.saveStatus))
                .check(matches(withText("Climb saved.")));

        // Close and open the screen to exercise its history-loading path.
        closeScreen();
        openScreen();

        // After reopening, the saved climb should have a tappable history row.
        onView(withText("V3 · 4 attempts"))
                .perform(scrollTo(), click());

        // Check that tapping the row shows the details we saved.
        onView(withText("Grade: V3\nAttempts: 4\nNotes: Steep overhang"))
                .inRoot(isDialog())
                .check(matches(withText("Grade: V3\nAttempts: 4\nNotes: Steep overhang")));
    }

    @Test
    public void invalidClimb_isNotSaved() {
        // Leave the required fields blank and press Save.
        onView(withId(R.id.saveButton))
                .perform(scrollTo(), click());

        onView(withId(R.id.gradeInput))
                .check(matches(hasErrorText("Enter a grade")));

        onView(withId(R.id.historyText))
                .check(matches(withText("No climbs saved yet.")));

        // Check the database too, rather than only trusting the screen.
        assertTrue(database.climbDao().getAll().isEmpty());
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