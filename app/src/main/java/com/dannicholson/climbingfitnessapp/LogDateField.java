package com.dannicholson.climbingfitnessapp;

import android.app.DatePickerDialog;
import android.widget.Button;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

// Keeps the displayed date and stored ISO date together.
final class LogDateField {
    private LogDateField() { }

    static void bind(Button button, String date) {
        button.setTag(date);
        button.setAllCaps(false);
        button.setText(date == null ? "Date unknown — choose date"
                : LocalDate.parse(date).format(
                        DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)));
        button.setOnClickListener(view -> {
            String saved = value(button);
            LocalDate initial = saved == null ? LocalDate.now() : LocalDate.parse(saved);
            new DatePickerDialog(button.getContext(), (picker, year, month, day) ->
                    bind(button, LocalDate.of(year, month + 1, day).toString()),
                    initial.getYear(), initial.getMonthValue() - 1,
                    initial.getDayOfMonth()).show();
        });
    }

    static String value(Button button) { return (String) button.getTag(); }
}
