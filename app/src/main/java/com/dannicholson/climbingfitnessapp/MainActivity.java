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
import android.view.View;
import android.widget.LinearLayout;

import androidx.appcompat.app.AlertDialog;
import android.text.InputType;

public class MainActivity extends AppCompatActivity {

    private EditText gradeInput;
    private EditText attemptsInput;
    private EditText notesInput;
    private TextView summaryText;
    private TextView saveStatus;
    private TextView historyText;
    private LinearLayout historyRows;
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
        historyRows = findViewById(R.id.historyRows);
        saveButton = findViewById(R.id.saveButton);

        Button previewButton = findViewById(R.id.previewButton);

        database = ClimbDatabaseProvider.getDatabase(this);
        repository = new ClimbRepository(database.climbDao());

        previewButton.setOnClickListener(view -> previewClimb());
        saveButton.setOnClickListener(view -> saveClimb());

        // Reload saved climbs whenever this screen opens.
        runDatabaseTask(
                repository::getAll,
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
                    return repository.getAll();
                }),
                "Climb saved.",
                "Could not save the climb. Please try again."
        );
    }

    private void renderHistory(List<ClimbEntity> climbs) {
        // We're rebuilding the list after a load or save.
        // Clear the old buttons so entries don't appear twice.
        historyRows.removeAllViews();

        if (climbs.isEmpty()) {
            historyText.setText("No climbs saved yet.");
            historyText.setVisibility(View.VISIBLE);
            return;
        }

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

    private void showClimbDetails(ClimbEntity climb) {
        new AlertDialog.Builder(this)
                .setTitle("Climb details")
                .setMessage(formatClimb(climb.grade, climb.attempts, climb.notes))
                .setPositiveButton("Close", null)
                .setNeutralButton("Edit", (dialog, which) -> showEditClimbDialog(climb))
                .show();
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
            Callable<List<ClimbEntity>> task,
            String successMessage,
            String failureMessage
    ) {
        saveButton.setEnabled(false);
        databaseWork.increment();

        databaseExecutor.execute(() -> {
            try {
                // Read the saved rows on the background thread.
                List<ClimbEntity> climbs = task.call();
                finishDatabaseTask(climbs, successMessage);
            } catch (Exception exception) {
                Log.e("ClimbDatabase", "Database operation failed", exception);
                finishDatabaseTask(null, failureMessage);
            }
        });
    }

    private void finishDatabaseTask(List<ClimbEntity> climbs, String message) {
        runOnUiThread(() -> {
            try {
                if (!isFinishing() && !isDestroyed()) {
                    // A failed task returns null, so keep the history already shown.
                    if (climbs != null) {
                        renderHistory(climbs);
                    }

                    saveStatus.setText(message);
                    saveButton.setEnabled(true);
                }
            } finally {
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

    private void showEditClimbDialog(ClimbEntity savedClimb) {
        // Wait for any current save or history load to finish.
        if (!saveButton.isEnabled()) {
            return;
        }

        LinearLayout fields = new LinearLayout(this);
        fields.setOrientation(LinearLayout.VERTICAL);

        int padding = Math.round(24 * getResources().getDisplayMetrics().density);
        fields.setPadding(padding, 0, padding, 0);

        EditText grade = new EditText(this);
        grade.setHint("Grade");
        grade.setSingleLine(true);
        grade.setText(savedClimb.grade);
        fields.addView(grade);

        EditText attempts = new EditText(this);
        attempts.setHint("Number of attempts");
        attempts.setInputType(InputType.TYPE_CLASS_NUMBER);
        attempts.setText(String.valueOf(savedClimb.attempts));
        fields.addView(attempts);

        EditText notes = new EditText(this);
        notes.setHint("Notes (optional)");
        notes.setText(savedClimb.notes);
        fields.addView(notes);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("Edit climb")
                .setView(fields)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save changes", null)
                .create();

        dialog.show();

        // Set the listener after show() so we can keep the dialog
        // open when the user needs to correct a field.
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(view -> {
            String gradeText = grade.getText().toString().trim();
            String attemptsText = attempts.getText().toString().trim();

            grade.setError(null);
            attempts.setError(null);

            String gradeError = validator.getGradeError(gradeText);
            if (gradeError != null) {
                grade.setError(gradeError);
                grade.requestFocus();
                return;
            }

            String attemptsError = validator.getAttemptsError(attemptsText);
            if (attemptsError != null) {
                attempts.setError(attemptsError);
                attempts.requestFocus();
                return;
            }

            Climb editedClimb = new Climb(
                    gradeText,
                    Integer.parseInt(attemptsText),
                    notes.getText().toString()
            );

            dialog.dismiss();
            updateClimb(savedClimb.id, editedClimb);
        });
    }

    private void updateClimb(long id, Climb editedClimb) {
        runDatabaseTask(
                () -> database.runInTransaction(() -> {
                    repository.update(id, editedClimb);
                    // Return fresh rows so the history shows the edit.
                    return repository.getAll();
                }),
                "Climb updated.",
                "Could not update the climb. Please try again."
        );
    }
}