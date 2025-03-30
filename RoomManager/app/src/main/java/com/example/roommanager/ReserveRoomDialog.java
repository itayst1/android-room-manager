package com.example.roommanager;

import android.app.DatePickerDialog;
import android.app.Dialog;
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
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public class ReserveRoomDialog extends DialogFragment {

    private Spinner roomSpinner;
    private NumberPicker hourPicker, minutePicker, durationPicker;
    private Button reserveButton, cancelButton, selectDateButton;
    private final Calendar selectedDateTime = Calendar.getInstance();

    @NonNull
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        View view = getLayoutInflater().inflate(R.layout.dialog_reserve_room, null);
        dialog.setContentView(view);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        String userId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();

        // Find views
        selectDateButton = dialog.findViewById(R.id.date_button);
        hourPicker = dialog.findViewById(R.id.hour_picker);
        minutePicker = dialog.findViewById(R.id.minute_picker);
        durationPicker = dialog.findViewById(R.id.duration_picker);
        roomSpinner = dialog.findViewById(R.id.room_spinner);
        reserveButton = dialog.findViewById(R.id.reserve_button);
        cancelButton = dialog.findViewById(R.id.cancel_button);

        reserveButton.setEnabled(false);

        FirebaseDatabase database = FirebaseDatabase.getInstance();

        Context context = getContext();

        final Calendar selectedDateTime = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String defaultDate = dateFormat.format(selectedDateTime.getTime());
        selectDateButton.setText(defaultDate); // Set initial date text

        // Handle Select Date button
        selectDateButton.setOnClickListener(v -> {
            int year = selectedDateTime.get(Calendar.YEAR);
            int month = selectedDateTime.get(Calendar.MONTH);
            int day = selectedDateTime.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(), (datePicker, selectedYear, selectedMonth, selectedDay) -> {
                selectedDateTime.set(selectedYear, selectedMonth, selectedDay);
                String formattedDate = selectedDay + "/" + (selectedMonth + 1) + "/" + selectedYear;
                selectDateButton.setText(formattedDate);
                reserveButton.setEnabled(true);
            }, year, month, day);

            datePickerDialog.show();
        });

        // Configure Hour Picker (7 AM - 5 PM)
        String[] hours = {"07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17"};
        hourPicker.setMinValue(0);
        hourPicker.setMaxValue(hours.length - 1);
        hourPicker.setDisplayedValues(hours);

        // Configure Minute Picker (0, 15, 30, 45)
        String[] minutes = {"00", "15", "30", "45"};
        minutePicker.setMinValue(0);
        minutePicker.setMaxValue(minutes.length - 1);
        minutePicker.setDisplayedValues(minutes);

        // Configure Duration Picker (30 min to 4 hours)
        String[] durations = {"30 min", "1 hour", "1.5 hours", "2 hours", "2.5 hours", "3 hours", "3.5 hours", "4 hours"};
        durationPicker.setMinValue(0);
        durationPicker.setMaxValue(durations.length - 1);
        durationPicker.setDisplayedValues(durations);

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

        // Handle Reserve button click
        reserveButton.setOnClickListener(v -> {
            String userEmail = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getEmail();
            String selectedRoom = roomSpinner.getSelectedItem().toString();
            String selectedDate = selectDateButton.getText().toString();
            String selectedHour = hours[hourPicker.getValue()];
            String selectedMinute = minutes[minutePicker.getValue()];
            String selectedTime = selectedHour + ":" + selectedMinute;
            String selectedDuration = durations[durationPicker.getValue()];

            // Create a new reservation object
            Reservation reservation = new Reservation(userEmail, selectedTime, selectedDuration);
            dismiss();

            database.getReference("reservations/" + selectedDate.replace("/", "-") + "/" + selectedRoom + "/" + userId).setValue(reservation)
                    .addOnSuccessListener(aVoid ->
                            Toast.makeText(context, selectedRoom + " reserved on " + selectedDate + " at " + selectedTime + " for " + selectedDuration, Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e ->
                            Toast.makeText(context, "Failed to reserve room", Toast.LENGTH_SHORT).show());
        });

        // Handle Cancel button click
        cancelButton.setOnClickListener(v -> dismiss());

        return dialog;
    }

    private int getDurationInMinutes(String selectedDuration) {
        // Convert the duration string to minutes
        switch (selectedDuration) {
            case "30 min":
                return 30;
            case "1 hour":
                return 60;
            case "1.5 hours":
                return 90;
            case "2 hours":
                return 120;
            case "2.5 hours":
                return 150;
            case "3 hours":
                return 180;
            case "3.5 hours":
                return 210;
            case "4 hours":
                return 240;
            default:
                return 0; // Default case
        }
    }
}
