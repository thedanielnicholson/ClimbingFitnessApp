package com.dannicholson.climbingfitnessapp;

import android.os.Bundle;
import android.content.Intent;
import android.util.Log;
import android.widget.Button;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.test.espresso.idling.CountingIdlingResource;

import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AlertDialog;
import android.widget.Toast;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;
import java.time.DayOfWeek;
import java.time.temporal.TemporalAdjusters;

public class MainActivity extends AppCompatActivity {

    private TextView saveStatus;
    private TextView historyText;
    private LinearLayout historyRows;
    private Button logClimbButton;
    private Button logWorkoutButton;
    private TextView workoutHistoryText;
    // The database stores records; the repository handles climb operations.
    private ClimbDatabase database;
    private ClimbRepository repository;
    // Run database jobs one at a time, away from the screen thread.
    private final ExecutorService databaseExecutor =
            Executors.newSingleThreadExecutor();
    // Lets Espresso know when database work and its screen update are done.
    private final CountingIdlingResource databaseWork =
            SaveEntryViewModel.DATABASE_WORK;
    private LinearLayout workoutRows;

    // Set up the screen and load the saved records when it opens.
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load the layout and allow it to extend behind the system bars.
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Keep the content clear of the status and navigation bars.
        ViewCompat.setOnApplyWindowInsetsListener(
                findViewById(R.id.main),
                (view, windowInsets) -> {
                    Insets bars = windowInsets.getInsets(
                            WindowInsetsCompat.Type.systemBars()
                    );

                    view.setPadding(
                            bars.left, bars.top, bars.right, bars.bottom
                    );

                    return windowInsets;
                }
        );

        saveStatus = findViewById(R.id.saveStatus);
        historyText = findViewById(R.id.historyText);
        historyRows = findViewById(R.id.historyRows);
        logClimbButton = findViewById(R.id.logClimbButton);


        database = ClimbDatabaseProvider.getDatabase(this);
        repository = new ClimbRepository(database.climbDao());
        findViewById(R.id.weeklySummaryButton)
                .setOnClickListener(this::showWeeklySummary);

        // Open a blank climb form when logging a new entry.
        logClimbButton.setOnClickListener(view ->
                startActivity(new Intent(this, LogClimbActivity.class)));

        logWorkoutButton = findViewById(R.id.logWorkoutButton);
        workoutHistoryText = findViewById(R.id.workoutHistoryText);
        workoutRows = findViewById(R.id.workoutRows);

        logWorkoutButton.setOnClickListener(view ->
                startActivity(new Intent(this, LogWorkoutActivity.class)));


    }

    // Refresh both histories when returning from a logging screen.
    @Override
    protected void onResume() {
        super.onResume();
        runDatabaseTask(repository::getAll, "", "Could not load climbs.");
        runWorkoutTask(() -> {}, "Could not load saved exercises.");
    }

    // Rebuild the saved-climb list after a database change.
    private void renderHistory(List<ClimbEntity> climbs) {
        // We're rebuilding the list after a load or save.
        // Clear the old buttons so entries don't appear twice.
        historyRows.removeAllViews();

        if (climbs.isEmpty()) {
            historyText.setText("No climbs saved yet.");
            historyText.setVisibility(View.VISIBLE);
            return;
        }

        // Hide the empty-history message when there are climbs to display.
        historyText.setVisibility(View.GONE);

        for (ClimbEntity climb : climbs) {
            Button row = new Button(this);
            String attemptLabel = climb.attempts == 1 ? " attempt" : " attempts";

            row.setAllCaps(false);
            row.setText(climb.grade + " · " + climb.attempts + attemptLabel);

            // This button opens the row it was created for, even if
            // another climb has the same grade and number of attempts.
            row.setOnClickListener(view -> showClimbDetails(climb));
            historyRows.addView(row);
        }
    }

    // Show the selected climb with edit and delete options.
    private void showClimbDetails(ClimbEntity climb) {
        new AlertDialog.Builder(this)
                .setTitle("Climb details")
                .setMessage(formatClimb(climb.grade, climb.attempts, climb.notes)
                        + "\nLogged: " + formatLoggedDate(climb.loggedDate))
                .setPositiveButton("Close", null)
                .setNeutralButton("Edit", (dialog, which) -> editClimb(climb))
                .setNegativeButton("Delete", (dialog, which) -> confirmDeleteClimb(climb))
                .show();
    }

    // Turn climb values into text for the screen.
    private String formatClimb(String grade, int attempts, String notes) {
        // Use the same wording for previews and saved history.
        String displayedNotes =
                notes == null || notes.isEmpty() ? "None" : notes;

        return "Grade: " + grade
                + "\nAttempts: " + attempts
                + "\nNotes: " + displayedNotes;
    }

    // Run a climb database job without freezing the screen.
    private void runDatabaseTask(
            Callable<List<ClimbEntity>> task,
            String successMessage,
            String failureMessage
    ) {
        // Pause climb actions until the history update finishes.
        logClimbButton.setEnabled(false);
        databaseWork.increment();

        databaseExecutor.execute(() -> {
            try {
                // The supplied job may load, save, edit or delete a climb.
                List<ClimbEntity> climbs = task.call();
                finishDatabaseTask(climbs, successMessage);
            } catch (Exception exception) {
                Log.e("ClimbDatabase", "Database operation failed", exception);
                finishDatabaseTask(null, failureMessage);
            }
        });
    }

    // Show the result on the screen after the database job finishes.
    private void finishDatabaseTask(List<ClimbEntity> climbs, String message) {
        // Android screen controls must be updated on the UI thread.
        runOnUiThread(() -> {
            try {
                if (!isFinishing() && !isDestroyed()) {
                    // A failed task returns null, so keep the history already shown.
                    if (climbs != null) {
                        renderHistory(climbs);
                    }

                    saveStatus.setText(message);
                    logClimbButton.setEnabled(true);
                }
            } finally {
                // Always balance the counter, even if the screen has closed.
                databaseWork.decrement();
            }
        });
    }

    // Give UI tests access to the database work counter.
    public CountingIdlingResource getDatabaseWork() {
        // UI tests register this so Espresso waits for database jobs.
        return databaseWork;
    }

    // Clean up the worker when Android destroys this screen.
    @Override
    protected void onDestroy() {
        // Finish queued jobs, then stop this screen's worker thread.
        databaseExecutor.shutdown();

        // The shared database belongs to the app, so leave it open.
        super.onDestroy();
    }

    // Send the selected climb's ID and values to the full-screen form.
    private void editClimb(ClimbEntity climb) {
        if (!logClimbButton.isEnabled()) return;
        Intent intent = new Intent(this, LogClimbActivity.class);
        intent.putExtra(LogEntryActivity.EXTRA_ENTRY_ID, climb.id);
        intent.putExtra(LogEntryActivity.EXTRA_DATE, climb.loggedDate);
        intent.putExtra("grade", climb.grade);
        intent.putExtra("attempts", climb.attempts);
        intent.putExtra("notes", climb.notes);
        startActivity(intent);
    }

    // Ask before deleting the selected climb.
    private void confirmDeleteClimb(ClimbEntity climb) {
        new AlertDialog.Builder(this)
                .setTitle("Delete climb?")
                .setMessage(formatClimb(climb.grade, climb.attempts, climb.notes))
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) -> deleteClimb(climb.id))
                .show();
    }

    // Delete the selected climb and reload the remaining entries.
    private void deleteClimb(long id) {
        // Wait for the current climb task to finish.
        if (!logClimbButton.isEnabled()) {
            return;
        }

        runDatabaseTask(
                () -> database.runInTransaction(() -> {
                    repository.delete(id);
                    // Read the remaining rows so the screen can rebuild the list.
                    return repository.getAll();
                }),
                "Climb deleted.",
                "Could not delete the climb. Please try again."
        );
    }

    // Open the exercise form with the selected record ready to edit.
    private void editWorkout(WorkoutEntity entry) {
        if (!logWorkoutButton.isEnabled()) return;
        Intent intent = new Intent(this, LogWorkoutActivity.class);
        intent.putExtra(LogEntryActivity.EXTRA_ENTRY_ID, entry.id);
        intent.putExtra(LogEntryActivity.EXTRA_DATE, entry.loggedDate);
        intent.putExtra("exercise", entry.exercise);
        intent.putExtra("sets", entry.sets);
        intent.putExtra("reps", entry.reps);
        intent.putExtra("weightKg", entry.weightKg);
        startActivity(intent);
    }

    // Apply an exercise change and reload its history in the background.
    private void runWorkoutTask(Runnable change, String failureMessage) {
        // Prevent another Log exercise tap until the history has refreshed.
        logWorkoutButton.setEnabled(false);
        databaseWork.increment();

        databaseExecutor.execute(() -> {
            try {
                List<WorkoutEntity> entries = database.runInTransaction(() -> {
                    // Apply the change, then read the updated history.
                    change.run();
                    return database.workoutDao().getAll();
                });

                finishWorkoutTask(entries, null);
            } catch (Exception exception) {
                Log.e("WorkoutDatabase", "Workout operation failed", exception);
                finishWorkoutTask(null, failureMessage);
            }
        });
    }

    // Update the exercise list or show an error on the screen.
    private void finishWorkoutTask(List<WorkoutEntity> entries, String error) {
        // Android screen controls must be updated on the UI thread.
        runOnUiThread(() -> {
            try {
                // Skip screen updates if this activity has already closed.
                if (isFinishing() || isDestroyed()) {
                    return;
                }

                if (error == null) {
                    renderWorkoutHistory(entries);
                } else {
                    // Keep any existing history visible if the request fails.
                    if (workoutRows.getChildCount() == 0) {
                        workoutHistoryText.setText(error);
                    }
                    Toast.makeText(this, error, Toast.LENGTH_SHORT).show();
                }

                logWorkoutButton.setEnabled(true);
            } finally {
                // Espresso can continue once the screen has handled the result.
                databaseWork.decrement();
            }
        });
    }

    // Build a clickable button for each saved exercise.
    private void renderWorkoutHistory(List<WorkoutEntity> entries) {
        // Rebuild the buttons from the latest database rows.
        workoutRows.removeAllViews();

        if (entries.isEmpty()) {
            workoutHistoryText.setText("No exercises saved yet.");
            workoutHistoryText.setVisibility(View.VISIBLE);
            return;
        }

        workoutHistoryText.setVisibility(View.GONE);

        for (WorkoutEntity entry : entries) {
            Button row = new Button(this);
            row.setAllCaps(false);
            row.setText(formatWorkout(entry));
            row.setOnClickListener(view -> showWorkoutDetails(entry));
            workoutRows.addView(row);
        }
    }

    // Show the exercise name, sets, reps and weight on one line.
    private String formatWorkout(WorkoutEntity entry) {
        return entry.exercise + " · " + entry.sets + " × " + entry.reps
                + " · " + entry.weightKg + " kg";
    }

    // Show the selected exercise with edit and delete options.
    private void showWorkoutDetails(WorkoutEntity entry) {
        // Wait for the history refresh before opening an entry.
        if (!logWorkoutButton.isEnabled()) {
            return;
        }

        new AlertDialog.Builder(this)
                .setTitle("Exercise details")
                .setMessage(formatWorkout(entry)
                        + "\nLogged: " + formatLoggedDate(entry.loggedDate))
                .setNeutralButton("Edit", (dialog, which) -> editWorkout(entry))
                .setNegativeButton("Delete", (dialog, which) -> confirmDeleteWorkout(entry))
                .setPositiveButton("Close", null)
                .show();
    }

    // Ask before deleting the selected exercise.
    private void confirmDeleteWorkout(WorkoutEntity entry) {
        new AlertDialog.Builder(this)
                .setTitle("Delete exercise?")
                .setMessage(formatWorkout(entry))
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Delete", (dialog, which) ->
                        runWorkoutTask(() -> {
                            if (database.workoutDao().delete(entry) != 1) {
                                throw new IllegalStateException("Exercise no longer exists");
                            }
                        }, "Could not delete the exercise. Please try again."))
                .show();
    }

    // Turn a stored date into the phone’s local date format.
    private String formatLoggedDate(String loggedDate) {
        // Older records may have been saved before dates were added.
        if (loggedDate == null) {
            return "Date unknown";
        }

        DateTimeFormatter formatter = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM);
        return LocalDate.parse(loggedDate).format(formatter);
    }

    // Load this week’s totals and show them in a dialog.
    private void showWeeklySummary(View button) {
        // Avoid opening the summary twice while its totals are loading.
        button.setEnabled(false);
        databaseWork.increment();

        databaseExecutor.execute(() -> {
            String message;

            try {
                message = buildWeeklySummary();
            } catch (Exception exception) {
                Log.e("TrainingSummary", "Could not load weekly totals", exception);
                message = "Could not load this week's totals. Please try again.";
            }

            // Keep a fixed copy for the screen-update callback.
            String dialogMessage = message;

            runOnUiThread(() -> {
                try {
                    // Skip screen updates if this activity has already closed.
                if (isFinishing() || isDestroyed()) {
                        return;
                    }

                    button.setEnabled(true);
                    new AlertDialog.Builder(this)
                            .setTitle("This week")
                            .setMessage(dialogMessage)
                            .setPositiveButton("Close", null)
                            .show();
                } finally {
                    // Let Espresso continue after the dialog has opened.
                    databaseWork.decrement();
                }
            });
        });
    }

    // Count climbs and exercises logged during the current week.
    private String buildWeeklySummary() {
        // Sunday still belongs to the week that started the previous Monday.
        LocalDate monday = LocalDate.now().with(
                TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)
        );
        LocalDate nextMonday = monday.plusWeeks(1);

        // Include this Monday and exclude next Monday. Undated rows are not counted.
        int climbCount = database.climbDao().countLoggedBetween(
                monday.toString(), nextMonday.toString()
        );
        // Count exercise entries, not the number of sets or reps.
        int exerciseCount = database.workoutDao().countLoggedBetween(
                monday.toString(), nextMonday.toString()
        );

        String firstDay = formatLoggedDate(monday.toString());
        String lastDay = formatLoggedDate(nextMonday.minusDays(1).toString());

        return firstDay + " to " + lastDay
                + "\n\nClimbs logged: " + climbCount
                + "\nExercises logged: " + exerciseCount;
    }
}
