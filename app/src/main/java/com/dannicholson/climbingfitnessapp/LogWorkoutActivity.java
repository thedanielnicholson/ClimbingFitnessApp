package com.dannicholson.climbingfitnessapp;

import android.os.Bundle;
import android.widget.EditText;


public class LogWorkoutActivity extends LogEntryActivity {
    private EditText exercise, sets, reps, weight;

    // Connect the fields and show saved values only on the first opening.
    @Override
    protected void onCreate(Bundle state) {
        super.onCreate(state);
        setUpForm(R.layout.activity_log_workout);
        exercise = findViewById(R.id.exerciseInput);
        sets = findViewById(R.id.setsInput);
        reps = findViewById(R.id.repsInput);
        weight = findViewById(R.id.weightInput);
        if (isEditing() && state == null) {
            exercise.setText(getIntent().getStringExtra("exercise"));
            sets.setText(String.valueOf(getIntent().getIntExtra("sets", 1)));
            reps.setText(String.valueOf(getIntent().getIntExtra("reps", 1)));
            weight.setText(String.valueOf(getIntent().getDoubleExtra("weightKg", 0)));
        }
        // Android restores unsaved text itself after rotation.
        findViewById(R.id.saveButton).setOnClickListener(view -> saveWorkout());
    }

    // Check inputs before sending a change to the database.
    private void saveWorkout() {
        exercise.setError(null);
        sets.setError(null);
        reps.setError(null);
        weight.setError(null);
        String name = exercise.getText().toString().trim();

        if (name.isEmpty()) {
            exercise.setError("Enter an exercise name");
            exercise.requestFocus();
            return;
        }

        Integer setCount = readPositiveCount(sets);
        if (setCount == null) {
            return;
        }

        Integer repCount = readPositiveCount(reps);
        if (repCount == null) {
            return;
        }

        // Accept a comma or dot as the decimal separator.
        double weightKg;
        try {
            String weightText = weight.getText().toString().trim().replace(',', '.');
            weightKg = Double.parseDouble(weightText);
        } catch (NumberFormatException exception) {
            // Mark unreadable text as invalid for the check below.
            weightKg = Double.NaN;
        }

        // Reject invalid or negative weights; zero means bodyweight.
        if (!Double.isFinite(weightKg) || weightKg < 0) {
            weight.setError("Enter a weight of 0 kg or more");
            weight.requestFocus();
            return;
        }

        WorkoutEntity entry = new WorkoutEntity(name, setCount, repCount, weightKg);

        entry.loggedDate = selectedDate();
        WorkoutDao dao = ClimbDatabaseProvider.getDatabase(this).workoutDao();
        long id = entryId();
        // Keep the ID on edits so an identical entry is never changed by mistake.
        saveEntry(() -> {
            if (id > 0) {
                entry.id = id;
                if (dao.update(entry) != 1) {
                    throw new IllegalStateException("Exercise no longer exists");
                }
            } else {
                dao.insert(entry);
            }
        });
    }

    // Accept only whole numbers above zero for sets and reps.
    private Integer readPositiveCount(EditText field) {
        try {
            int count = Integer.parseInt(field.getText().toString().trim());
            if (count > 0) {
                return count;
            }
        } catch (NumberFormatException exception) {
            // Blank text and values too large for an int get the same message.
        }

        field.setError("Enter a whole number greater than 0");
        field.requestFocus();
        return null;
    }

}
