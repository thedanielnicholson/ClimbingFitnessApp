package com.dannicholson.climbingfitnessapp;

import android.os.Bundle;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
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

public class MainActivity extends AppCompatActivity {

    private EditText gradeInput;
    private EditText attemptsInput;
    private EditText notesInput;
    private TextView summaryText;
    private TextView saveStatus;
    private TextView historyText;
    private Button saveButton;

    private ClimbDatabase database;
    private ClimbRepository repository;

    private final ClimbValidator validator = new ClimbValidator();

    // Run database jobs one at a time, away from the screen thread.
    private final ExecutorService databaseExecutor =
            Executors.newSingleThreadExecutor();

    // Lets Espresso know when database work and its screen update are done.
    private final CountingIdlingResource databaseWork =
            new CountingIdlingResource("Climb database work");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

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

        gradeInput = findViewById(R.id.gradeInput);
        attemptsInput = findViewById(R.id.attemptsInput);
        notesInput = findViewById(R.id.notesInput);
        summaryText = findViewById(R.id.summaryText);
        saveStatus = findViewById(R.id.saveStatus);
        historyText = findViewById(R.id.historyText);
        saveButton = findViewById(R.id.saveButton);

        Button previewButton = findViewById(R.id.previewButton);

        database = ClimbDatabaseProvider.getDatabase(this);
        repository = new ClimbRepository(database.climbDao());

        previewButton.setOnClickListener(view -> previewClimb());
        saveButton.setOnClickListener(view -> saveClimb());

        // Reload saved records whenever this screen is created.
        runDatabaseTask(
                () -> buildHistoryText(),
                "",
                "Could not load climbs. Reopen the screen to try again."
        );
    }

    private Climb readClimbFromForm() {
        String grade = gradeInput.getText().toString().trim();
        String attemptsText = attemptsInput.getText().toString().trim();
        String notes = notesInput.getText().toString().trim();

        // Remove messages left by the previous entry.
        summaryText.setText("");
        saveStatus.setText("");
        gradeInput.setError(null);
        attemptsInput.setError(null);

        String gradeError = validator.getGradeError(grade);

        if (gradeError != null) {
            gradeInput.setError(gradeError);
            gradeInput.requestFocus();
            return null;
        }

        String attemptsError = validator.getAttemptsError(attemptsText);

        if (attemptsError != null) {
            attemptsInput.setError(attemptsError);
            attemptsInput.requestFocus();
            return null;
        }

        int attempts = Integer.parseInt(attemptsText);

        // Both buttons use the same checks and create the same object.
        return new Climb(grade, attempts, notes);
    }

    private void previewClimb() {
        Climb climb = readClimbFromForm();

        // A null result means an input error was already shown.
        if (climb == null) {
            return;
        }

        summaryText.setText(formatClimb(
                climb.getGrade(),
                climb.getAttempts(),
                climb.getNotes()
        ));
    }

    private void saveClimb() {
        Climb climb = readClimbFromForm();

        if (climb == null) {
            return;
        }

        runDatabaseTask(
                () -> database.runInTransaction(() -> {
                    /*
                     * Save and read the updated history in one transaction.
                     * If either operation fails, the transaction rolls back.
                     */
                    repository.save(climb);
                    return buildHistoryText();
                }),
                "Climb saved.",
                "Could not save the climb. Please try again."
        );
    }

    private String buildHistoryText() {
        List<ClimbEntity> climbs = repository.getAll();

        if (climbs.isEmpty()) {
            return "No climbs saved yet.";
        }

        StringBuilder history = new StringBuilder();

        for (ClimbEntity climb : climbs) {
            // Leave a blank line between saved entries.
            if (history.length() > 0) {
                history.append("\n\n");
            }

            history.append(formatClimb(
                    climb.grade,
                    climb.attempts,
                    climb.notes
            ));
        }

        return history.toString();
    }

    private String formatClimb(String grade, int attempts, String notes) {
        // Use the same wording for previews and saved history.
        String displayedNotes =
                notes == null || notes.isEmpty() ? "None" : notes;

        return "Grade: " + grade
                + "\nAttempts: " + attempts
                + "\nNotes: " + displayedNotes;
    }

    private void runDatabaseTask(
            Callable<String> task,
            String successMessage,
            String failureMessage
    ) {
        // Prevent another save while a database job is running.
        saveButton.setEnabled(false);
        databaseWork.increment();

        databaseExecutor.execute(() -> {
            try {
                // This part runs on the background thread.
                String history = task.call();

                finishDatabaseTask(history, successMessage);
            } catch (Exception exception) {
                // Keep the technical details in Logcat for debugging.
                Log.e("ClimbDatabase", "Database operation failed", exception);

                finishDatabaseTask(null, failureMessage);
            }
        });
    }

    private void finishDatabaseTask(String history, String message) {
        // Android screen components must be updated on the main thread.
        runOnUiThread(() -> {
            try {
                // The user may have closed this screen while the job ran.
                if (!isFinishing() && !isDestroyed()) {
                    if (history != null) {
                        historyText.setText(history);
                    }

                    saveStatus.setText(message);
                    saveButton.setEnabled(true);
                }
            } finally {
                // Mark the job complete even if the screen has closed.
                databaseWork.decrement();
            }
        });
    }

    public CountingIdlingResource getDatabaseWork() {
        // UI tests register this so Espresso waits for database jobs.
        return databaseWork;
    }

    @Override
    protected void onDestroy() {
        // Finish queued jobs, then stop this screen's worker thread.
        databaseExecutor.shutdown();

        // The shared database belongs to the app, so leave it open.
        super.onDestroy();
    }
}