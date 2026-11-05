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
        String grade = gradeInput.getText().toString().trim();
        String attemptsText = attemptsInput.getText().toString().trim();
        String notes = notesInput.getText().toString().trim();

        // Remove the previous preview and validation messages.
        summaryText.setText("");
        gradeInput.setError(null);
        attemptsInput.setError(null);

        if (grade.isEmpty()) {
            gradeInput.setError("Enter a grade");
            gradeInput.requestFocus();
            return;
        }

        int attempts;

        try {
            attempts = Integer.parseInt(attemptsText);
        } catch (NumberFormatException exception) {
            attemptsInput.setError("Enter a valid whole number");
            attemptsInput.requestFocus();
            return;
        }

        if (attempts < 1) {
            attemptsInput.setError("Enter at least 1 attempt");
            attemptsInput.requestFocus();
            return;
        }

        String summary = "Grade: " + grade
                + "\nAttempts: " + attempts
                + "\nNotes: " + (notes.isEmpty() ? "None" : notes);

        summaryText.setText(summary);
    }
}