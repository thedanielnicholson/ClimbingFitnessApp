package com.dannicholson.climbingfitnessapp;

import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    private EditText gradeInput;
    private EditText attemptsInput;
    private EditText notesInput;
    private TextView summaryText;

    // Keep the input checks in a separate class so we can test them easily.
    private final ClimbValidator validator = new ClimbValidator();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        // Keep the form clear of the status and navigation bars.
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

        // Connect Java variables to components in the XML layout.
        gradeInput = findViewById(R.id.gradeInput);
        attemptsInput = findViewById(R.id.attemptsInput);
        notesInput = findViewById(R.id.notesInput);
        summaryText = findViewById(R.id.summaryText);

        Button previewButton = findViewById(R.id.previewButton);

        // Run previewClimb() when the button is pressed.
        previewButton.setOnClickListener(view -> previewClimb());
    }

    private void previewClimb() {
        // Read the form and remove spaces from the ends of each value.
        String grade = gradeInput.getText().toString().trim();
        String attemptsText = attemptsInput.getText().toString().trim();
        String notes = notesInput.getText().toString().trim();

        // Clear the previous result before checking this entry.
        summaryText.setText("");
        gradeInput.setError(null);
        attemptsInput.setError(null);

        // Ask the validator to check the grade, then show any error.
        String gradeError = validator.getGradeError(grade);

        if (gradeError != null) {
            gradeInput.setError(gradeError);
            gradeInput.requestFocus();
            return;
        }

        // Check the attempt count before trying to use it.
        String attemptsError = validator.getAttemptsError(attemptsText);

        if (attemptsError != null) {
            attemptsInput.setError(attemptsError);
            attemptsInput.requestFocus();
            return;
        }

        // The validator has checked that this text fits into an int.
        int attempts = Integer.parseInt(attemptsText);

        // Show a preview. Permanent saving will come later.
        String summary = "Grade: " + grade
                + "\nAttempts: " + attempts
                + "\nNotes: " + (notes.isEmpty() ? "None" : notes);

        summaryText.setText(summary);
    }
}