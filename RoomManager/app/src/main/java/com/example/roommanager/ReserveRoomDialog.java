package com.example.roommanager;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.TimePickerDialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.TimePicker;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.FirebaseDatabase;

import java.util.Calendar;
import java.util.Objects;

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

        String userId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        // Find views
        Button selectDateButton = dialog.findViewById(R.id.date_button);
        Button selectTimeButton = dialog.findViewById(R.id.time_button);

// Variables to store selected date and time
        final Calendar selectedDateTime = Calendar.getInstance();

// Handle Select Date button
        selectDateButton.setOnClickListener(v -> {
            int year = selectedDateTime.get(Calendar.YEAR);
            int month = selectedDateTime.get(Calendar.MONTH);
            int day = selectedDateTime.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(), (datePicker, selectedYear, selectedMonth, selectedDay) -> {
                selectedDateTime.set(selectedYear, selectedMonth, selectedDay);
                String formattedDate = selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear;
                selectDateButton.setText(formattedDate); // Update button text
            }, year, month, day);

            datePickerDialog.show();
        });

// Handle Select Time button
        selectTimeButton.setOnClickListener(v -> {
            int hour = selectedDateTime.get(Calendar.HOUR_OF_DAY);
            int minute = selectedDateTime.get(Calendar.MINUTE);

            TimePickerDialog timePickerDialog = new TimePickerDialog(requireContext(), (timePicker, selectedHour, selectedMinute) -> {
                selectedDateTime.set(Calendar.HOUR_OF_DAY, selectedHour);
                selectedDateTime.set(Calendar.MINUTE, selectedMinute);

                String formattedTime = String.format("%02d:%02d", selectedHour, selectedMinute);
                selectTimeButton.setText(formattedTime); // Update button text
            }, hour, minute, false);

            timePickerDialog.show();
        });

        // Initialize UI components
        roomSpinner = view.findViewById(R.id.room_spinner);
        durationPicker = view.findViewById(R.id.duration_picker);
        reserveButton = view.findViewById(R.id.reserve_button);
        cancelButton = view.findViewById(R.id.cancel_button);

        FirebaseDatabase database = FirebaseDatabase.getInstance();

        Context context = getContext();

        // Populate Spinner with sample room data
        String[] rooms = {"Room 1", "Room 2", "Room 3", "Room 4", "Room 5"};
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, rooms);
        roomSpinner.setAdapter(adapter);
        roomSpinner.setPopupBackgroundResource(R.color.white);
        roomSpinner.post(() -> {
            View selectedView = roomSpinner.getSelectedView();
            if (selectedView instanceof TextView) {
                ((TextView) selectedView).setTextColor(getResources().getColor(R.color.black));
            }
        });

        roomSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (view instanceof TextView) {
                    ((TextView) view).setTextColor(getResources().getColor(R.color.black));
                }
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                // No action needed
            }
        });

        // Configure NumberPicker for duration (30 min to 4 hours)
        String[] durations = {"30 min", "1 hour", "1.5 hours", "2 hours", "2.5 hours", "3 hours", "3.5 hours", "4 hours"};
        durationPicker.setMinValue(0);
        durationPicker.setMaxValue(durations.length - 1);
        durationPicker.setDisplayedValues(durations);

        // Handle Reserve button click
        reserveButton.setOnClickListener(v -> {
            String userEmail = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getEmail(); // Get current user ID
            String selectedRoom = roomSpinner.getSelectedItem().toString();
            String startTime = selectTimeButton.getText().toString();
            String selectedDate = selectDateButton.getText().toString();
            String selectedDuration = durations[durationPicker.getValue()];

            // Create a new reservation object
            Reservation reservation = new Reservation(userEmail, startTime, selectedDuration);

            dismiss();

            database.getReference("reservations/" + selectedDate.replace("/", "-") + "/" + selectedRoom + "/" + userId).setValue(reservation)
                    .addOnSuccessListener(aVoid -> {
                        Toast.makeText(context, selectedRoom + " reserved on " + selectedDate + " at " + startTime + " for " + selectedDuration, Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        Toast.makeText(context, "Failed to reserve room", Toast.LENGTH_SHORT).show();
                    });
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