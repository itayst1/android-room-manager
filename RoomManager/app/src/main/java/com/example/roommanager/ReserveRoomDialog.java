package com.example.roommanager;

import android.app.Dialog;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.ColorInt;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import java.util.Calendar;

public class ReserveRoomDialog extends DialogFragment {

    private Spinner roomSpinner;
    private TimePicker startTimePicker;
    private NumberPicker durationPicker;
    private Button reserveButton, cancelButton;

    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        View view = LayoutInflater.from(getContext()).inflate(R.layout.dialog_reserve_room, null);
        dialog.setContentView(view);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT)); // Remove background corners
        }

        // Initialize UI components
        roomSpinner = view.findViewById(R.id.room_spinner);
        startTimePicker = view.findViewById(R.id.start_time_picker);
        durationPicker = view.findViewById(R.id.duration_picker);
        reserveButton = view.findViewById(R.id.reserve_button);
        cancelButton = view.findViewById(R.id.cancel_button);

        // Populate Spinner with sample room data
        String[] rooms = {"Room 1", "Room 2", "Room 3", "Room 4", "Room 5"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, rooms);
        roomSpinner.setAdapter(adapter);
        roomSpinner.setPopupBackgroundResource(R.color.white);

        // Configure TimePicker for AM/PM mode
        startTimePicker.setIs24HourView(false);

        // Configure NumberPicker for duration (30 min to 4 hours)
        String[] durations = {"30 min", "1 hour", "1.5 hours", "2 hours", "2.5 hours", "3 hours", "3.5 hours", "4 hours"};
        durationPicker.setMinValue(0);
        durationPicker.setMaxValue(durations.length - 1);
        durationPicker.setDisplayedValues(durations);

        // Handle Reserve button click
        reserveButton.setOnClickListener(v -> {
            String selectedRoom = roomSpinner.getSelectedItem().toString();
            int startHour = startTimePicker.getHour();
            int startMinute = startTimePicker.getMinute();
            String selectedDuration = durations[durationPicker.getValue()];

            dismiss();
            Toast.makeText(getContext(), selectedRoom + " reserved from " +
                    formatTime(startHour, startMinute) + " for " + selectedDuration, Toast.LENGTH_LONG).show();
        });

        // Handle Cancel button click
        cancelButton.setOnClickListener(v -> dismiss());

        return dialog;
    }


    // Format time for display (e.g., 2:30 PM)
    private String formatTime(int hour, int minute) {
        String amPm = (hour < 12) ? "AM" : "PM";
        int displayHour = (hour == 0 || hour == 12) ? 12 : hour % 12;
        return String.format("%02d:%02d %s", displayHour, minute, amPm);
    }
}