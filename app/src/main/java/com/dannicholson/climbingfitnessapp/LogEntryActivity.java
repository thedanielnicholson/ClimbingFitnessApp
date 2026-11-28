package com.dannicholson.climbingfitnessapp;

import android.widget.TextView;
import android.widget.Button;
import android.os.Bundle;
import java.time.LocalDate;
import androidx.activity.EdgeToEdge;
import androidx.activity.OnBackPressedCallback;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.lifecycle.ViewModelProvider;

// Shared navigation and saving behaviour for both logging screens.
public abstract class LogEntryActivity extends AppCompatActivity {
    private SaveEntryViewModel model;
    private String initialDate;

    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        initialDate = state == null ? LocalDate.now().toString() : state.getString("loggedDate");
    }

    @Override protected void onSaveInstanceState(Bundle state) {
        state.putString("loggedDate", selectedDate());
        super.onSaveInstanceState(state);
    }

    protected String selectedDate() {
        return LogDateField.value((Button) findViewById(R.id.loggedDateButton));
    }


    protected void setUpForm(int layout) {
        EdgeToEdge.enable(this);
        setContentView(layout);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (view, insets) -> {
            Insets bars = insets.getInsets(WindowInsetsCompat.Type.systemBars()
                    | WindowInsetsCompat.Type.ime());
            view.setPadding(bars.left, bars.top, bars.right, bars.bottom);
            return insets;
        });
        LogDateField.bind(findViewById(R.id.loggedDateButton), initialDate);
        model = new ViewModelProvider(this).get(SaveEntryViewModel.class);
        findViewById(R.id.backButton).setOnClickListener(view -> leaveForm());
        findViewById(R.id.cancelButton).setOnClickListener(view -> leaveForm());
        getOnBackPressedDispatcher().addCallback(this, new OnBackPressedCallback(true) {
            @Override public void handleOnBackPressed() { leaveForm(); }
        });
        model.state.observe(this, state -> {
            boolean saving = state == SaveEntryViewModel.State.SAVING;
            findViewById(R.id.saveButton).setEnabled(!saving);
            findViewById(R.id.cancelButton).setEnabled(!saving);
            findViewById(R.id.backButton).setEnabled(!saving);
            TextView status = findViewById(R.id.saveStatus);
            status.setText(saving ? "Saving…" : state == SaveEntryViewModel.State.FAILED
                    ? "Could not save. Please try again." : "");
            if (state == SaveEntryViewModel.State.SAVED) {
                setResult(RESULT_OK);
                finish();
            }
        });
    }

    private void leaveForm() {
        // Wait for an active save so Back cannot race a database write.
        if (model.state.getValue() != SaveEntryViewModel.State.SAVING) finish();
    }

    protected void saveEntry(Runnable insert) { model.save(insert); }
}
