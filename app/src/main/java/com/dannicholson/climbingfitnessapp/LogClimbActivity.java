package com.dannicholson.climbingfitnessapp;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.TextView;

public class LogClimbActivity extends LogEntryActivity {
    private EditText gradeInput, attemptsInput, notesInput;
    private TextView summaryText, saveStatus;
    private final ClimbValidator validator = new ClimbValidator();

    // Connect the fields and fill them once when editing an existing climb.
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setUpForm(R.layout.activity_log_climb);
        gradeInput = findViewById(R.id.gradeInput);
        attemptsInput = findViewById(R.id.attemptsInput);
        notesInput = findViewById(R.id.notesInput);
        summaryText = findViewById(R.id.summaryText);
        saveStatus = findViewById(R.id.saveStatus);
        if (isEditing() && state == null) {
            gradeInput.setText(getIntent().getStringExtra("grade"));
            attemptsInput.setText(String.valueOf(getIntent().getIntExtra("attempts", 1)));
            notesInput.setText(getIntent().getStringExtra("notes"));
        }
        // On rotation, Android restores the user's current field text instead.
        findViewById(R.id.previewButton).setOnClickListener(view -> previewClimb());
        findViewById(R.id.saveButton).setOnClickListener(view -> {
            Climb climb = readClimbFromForm();
            if (climb != null) {
                ClimbRepository repository = new ClimbRepository(
                        ClimbDatabaseProvider.getDatabase(this).climbDao());
                String date = selectedDate();
                long id = entryId();
                ClimbDatabase database = ClimbDatabaseProvider.getDatabase(this);
                // Update values and date together, or insert a new record.
                saveEntry(() -> {
                    if (id > 0) {
                        database.runInTransaction(() -> {
                            repository.update(id, climb);
                            database.climbDao().updateLoggedDate(id, date);
                        });
                    } else {
                        repository.save(climb, date);
                    }
                });
            }
        });
    }

    // Read and check the climb fields before previewing or saving.
    private Climb readClimbFromForm() {
        // Trim spaces from the start and end of each entry.
        String grade = gradeInput.getText().toString().trim();
        String attemptsText = attemptsInput.getText().toString().trim();
        String notes = notesInput.getText().toString().trim();

        // Remove messages left by the previous entry.
        summaryText.setText("");
        saveStatus.setText("");
        gradeInput.setError(null);
        attemptsInput.setError(null);

        // Stop here and highlight the field if the grade is invalid.
        String gradeError = validator.getGradeError(grade);

        if (gradeError != null) {
            gradeInput.setError(gradeError);
            gradeInput.requestFocus();
            return null;
        }

        // Check attempts before converting the text into a number.
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

    // Show the climb details without saving them.
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

    // Turn climb values into text for the screen.
    private String formatClimb(String grade, int attempts, String notes) {
        // Use the same wording for previews and saved history.
        String displayedNotes =
                notes == null || notes.isEmpty() ? "None" : notes;

        return "Grade: " + grade
                + "\nAttempts: " + attempts
                + "\nNotes: " + displayedNotes;
    }

}
