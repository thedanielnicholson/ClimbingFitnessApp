package com.dannicholson.climbingfitnessapp;

import android.content.Context;

import androidx.room.Room;
import androidx.test.core.app.ApplicationProvider;
import androidx.test.ext.junit.runners.AndroidJUnit4;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

@RunWith(AndroidJUnit4.class)
public class ClimbDatabaseTest {

    // Use a separate database so tests never change real app records.
    private static final String TEST_DATABASE = "climb-database-test";

    private Context context;
    private ClimbDatabase database;

    @Before
    public void setUp() {
        context = ApplicationProvider.getApplicationContext();

        // Remove anything left by a previous test run.
        context.deleteDatabase(TEST_DATABASE);
        openDatabase();
    }

    @After
    public void tearDown() {
        // Close the connection before removing the test database.
        if (database != null) {
            database.close();
        }

        context.deleteDatabase(TEST_DATABASE);
    }

    @Test
    public void newDatabase_hasNoClimbs() {
        assertTrue(database.climbDao().getAll().isEmpty());
    }

    @Test
    public void savedClimb_isAvailableAfterReopeningDatabase() {
        // Save one climb and keep the ID assigned by the database.
        long savedId = database.climbDao().insert(
                new ClimbEntity("V3", 4, "Steep overhang")
        );

        // Reopen the file to check that the record was saved to disk.
        database.close();
        openDatabase();

        List<ClimbEntity> climbs = database.climbDao().getAll();

        assertEquals(1, climbs.size());

        ClimbEntity savedClimb = climbs.get(0);

        assertTrue(savedId > 0);
        assertEquals(savedId, savedClimb.id);
        assertEquals("V3", savedClimb.grade);
        assertEquals(4, savedClimb.attempts);
        assertEquals("Steep overhang", savedClimb.notes);
    }

    private void openDatabase() {
        database = Room.databaseBuilder(
                context,
                ClimbDatabase.class,
                TEST_DATABASE
        ).build();
    }

    @Test
    public void repository_savesCheckedClimbDetails() {
        // Use the separate test database opened by setUp().
        ClimbRepository repository =
                new ClimbRepository(database.climbDao());

        // Climb removes extra spaces and treats missing notes as empty text.
        Climb climb = new Climb(" V3 ", 4, null);

        long savedId = repository.save(climb);

        // Check that the repository saved the cleaned details correctly.
        List<ClimbEntity> climbs = repository.getAll();

        assertEquals(1, climbs.size());

        ClimbEntity savedClimb = climbs.get(0);

        assertTrue(savedId > 0);
        assertEquals(savedId, savedClimb.id);
        assertEquals("V3", savedClimb.grade);
        assertEquals(4, savedClimb.attempts);
        assertEquals("", savedClimb.notes);
    }

    @Test
    public void update_changesOnlyTheSelectedClimb() {
        ClimbRepository repository = new ClimbRepository(database.climbDao());

        // Save two matching climbs so the test proves the ID selects the row.
        long selectedId = repository.save(new Climb("V3", 4, "Overhang"));
        long otherId = repository.save(new Climb("V3", 4, "Overhang"));

        repository.update(selectedId, new Climb("V4", 2, "Edited notes"));

        List<ClimbEntity> climbs = repository.getAll();
        assertEquals(2, climbs.size());

        // History is ordered by ID, newest first.
        ClimbEntity other = climbs.get(0);
        assertEquals(otherId, other.id);
        assertEquals("V3", other.grade);
        assertEquals(4, other.attempts);
        assertEquals("Overhang", other.notes);

        ClimbEntity edited = climbs.get(1);
        assertEquals(selectedId, edited.id);
        assertEquals("V4", edited.grade);
        assertEquals(2, edited.attempts);
        assertEquals("Edited notes", edited.notes);
    }
}