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
    // Extras identify an existing record; new entries have no ID.
    public static final String EXTRA_ENTRY_ID = "entryId";
    public static final String EXTRA_DATE = "entryDate";
    private SaveEntryViewModel model;
    private String initialDate;

    // Restore the chosen date, or start with the saved date when editing.
    @Override protected void onCreate(Bundle state) {
        super.onCreate(state);
        initialDate = state != null ? state.getString("loggedDate")
                : isEditing() ? getIntent().getStringExtra(EXTRA_DATE)
                : LocalDate.now().toString();
    }

    // Keep a changed date when Android recreates this screen.
    @Override protected void onSaveInstanceState(Bundle state) {
        state.putString("loggedDate", selectedDate());
        super.onSaveInstanceState(state);
    }

    // Read the date currently shown on the form.
    protected String selectedDate() {
        return LogDateField.value((Button) findViewById(R.id.loggedDateButton));
    }


    // Use the original ID for updates, even after rotation.
    protected long entryId() { return getIntent().getLongExtra(EXTRA_ENTRY_ID, -1); }
    protected boolean isEditing() { return entryId() > 0; }

    // Set up the shared layout, navigation and save feedback.
    protected void setUpForm(int layout) {
        EdgeToEdge.enable(this);
        setContentView(layout);
        if (isEditing()) {
            ((TextView) findViewById(R.id.formTitle)).setText(
                    this instanceof LogClimbActivity ? "Edit climb" : "Edit exercise");
            ((Button) findViewById(R.id.saveButton)).setText("Save changes");
        }
        // Keep the form clear of the keyboard and system bars.
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
        // Stay on the form if saving fails; return to history after success.
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

    // Run either an insert or an update through the same background worker.
    protected void saveEntry(Runnable change) { model.save(change); }
}
