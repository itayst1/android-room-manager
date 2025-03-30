package com.example.roommanager;

import android.app.DatePickerDialog;
import android.app.Dialog;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.NumberPicker;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;

import java.lang.reflect.Field;
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

    private RecyclerView timeSlotsRecycler;
    private TimeSlotAdapter timeSlotAdapter;
    private List<String> timeSlots = new ArrayList<>();
    private List<Boolean> reservedSlots = new ArrayList<>();

    private String[] hours = {"07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17"};
    private String[] minutes = {"00", "15", "30", "45"};
    private String[] durations = {"30 min", "1 hour", "1.5 hours", "2 hours", "2.5 hours", "3 hours", "3.5 hours", "4 hours"};
    private String[] rooms = {"Room 1", "Room 2", "Room 3", "Room 4", "Room 5"};

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

        timeSlotsRecycler = view.findViewById(R.id.time_slots_recycler);
        timeSlotsRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        timeSlotAdapter = new TimeSlotAdapter(getContext(), timeSlots, reservedSlots);
        timeSlotsRecycler.setAdapter(timeSlotAdapter);

        FirebaseDatabase database = FirebaseDatabase.getInstance();

        Context context = getContext();

        final Calendar selectedDateTime = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
        String defaultDate = dateFormat.format(selectedDateTime.getTime());
        if(defaultDate.split("/")[1].charAt(0) == '0'){
            defaultDate = defaultDate.substring(0, 3) + defaultDate.substring(4);
        }
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
                fetchReservedTimes();
            }, year, month, day);

            datePickerDialog.show();
        });

        // Configure Hour Picker (7 AM - 5 PM)
        hourPicker.setMinValue(0);
        hourPicker.setMaxValue(hours.length - 1);
        hourPicker.setDisplayedValues(hours);

        // Configure Minute Picker (0, 15, 30, 45)
        minutePicker.setMinValue(0);
        minutePicker.setMaxValue(minutes.length - 1);
        minutePicker.setDisplayedValues(minutes);

        // Configure Duration Picker (30 min to 4 hours)
        durationPicker.setMinValue(0);
        durationPicker.setMaxValue(durations.length - 1);
        durationPicker.setDisplayedValues(durations);

        // Populate Spinner with sample room data
        ArrayAdapter<String> adapter = new ArrayAdapter<>(getContext(), android.R.layout.simple_spinner_dropdown_item, rooms);
        roomSpinner.setAdapter(adapter);
        roomSpinner.post(() -> {
            View selectedView = roomSpinner.getSelectedView();
            if (selectedView instanceof TextView) {
                ((TextView) selectedView).setTextColor(Color.WHITE);
            }
        });

        roomSpinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                if (view instanceof TextView) {
                    ((TextView) view).setTextColor(Color.WHITE);
                }
                fetchReservedTimes();
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });

        fetchReservedTimes();

        timeSlotsRecycler.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void onDraw(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                super.onDraw(c, parent, state);

                int childCount = parent.getChildCount();
                int height = parent.getHeight();
                float fadeHeight = height * 0.03f; // 15% fade zone at top & bottom (adjustable)

                // Ensure that the first and last items are always visible
                for (int i = 0; i < childCount; i++) {
                    View child = parent.getChildAt(i);
                    float itemY = child.getY();
                    float itemBottom = itemY + child.getHeight();
                    float alpha = 1.0f;

                    // Apply fade effect to the top of the list
                    if (itemY < fadeHeight) {
                        alpha = Math.max(0.0f, itemY / fadeHeight);
                    }
                    // Apply fade effect to the bottom of the list
                    else if (itemBottom > height - fadeHeight) {
                        alpha = Math.max(0.0f, (height - itemBottom) / fadeHeight);
                    }

                    // Apply alpha to the item view
                    child.setAlpha(alpha);
                }
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

            if(checkAvailability(selectedTime)){
                // Create a new reservation object
                Reservation reservation = new Reservation(userEmail, selectedTime, selectedDuration);
                dismiss();

                database.getReference("reservations/" + selectedDate.replace("/", "-") + "/" + selectedRoom + "/" + userId).setValue(reservation)
                        .addOnSuccessListener(aVoid ->
                                Toast.makeText(context, selectedRoom + " reserved on " + selectedDate + " at " + selectedTime + " for " + selectedDuration, Toast.LENGTH_SHORT).show())
                        .addOnFailureListener(e ->
                                Toast.makeText(context, "Failed to reserve room", Toast.LENGTH_SHORT).show());
            }
        });

        // Handle Cancel button click
        cancelButton.setOnClickListener(v -> dismiss());

        return dialog;
    }

    private void fetchReservedTimes() {
        // Clear existing data
        timeSlots.clear();
        reservedSlots.clear();

        // Populate the time slots (7:00 AM - 5:00 PM in 30-min intervals)
        for (int hour = 7; hour < 17; hour++) {
            for (int minute = 0; minute < 60; minute += 15) {
                int nextMinute = minute + 15;

                // Handle the next hour transition (e.g., 07:45 -> 08:00)
                int endHour = hour;
                int endMinute = nextMinute;
                if (endMinute == 60) {
                    endMinute = 0;
                    endHour++;
                }

                String startTime = String.format("%02d:%02d", hour, minute);
                String endTime = String.format("%02d:%02d", endHour, endMinute);

                // Add the time range to the timeSlots list
                String timeRange = startTime + " - " + endTime;
                timeSlots.add(timeRange);

                reservedSlots.add(false); // Initially all are available
            }
        }

        // Fetch reserved slots from Firebase
        Log.d("test", "reservations/" + selectDateButton.getText().toString().replace("/", "-") + "/" + roomSpinner.getSelectedItem().toString());
        FirebaseDatabase.getInstance().getReference("reservations/" + selectDateButton.getText().toString().replace("/", "-") + "/" + roomSpinner.getSelectedItem().toString())
                .get()
                .addOnSuccessListener(snapshot -> {
                    for (DataSnapshot reservation : snapshot.getChildren()) {
                        Calendar reservedStart = Calendar.getInstance();
                        String reservedTime = reservation.child("startTime").getValue(String.class);
                        String duration = reservation.child("duration").getValue(String.class);
                        reservedStart.set(Calendar.HOUR_OF_DAY, Integer.parseInt(reservedTime.split(":")[0]));
                        reservedStart.set(Calendar.MINUTE, Integer.parseInt(reservedTime.split(":")[1]));
                        reservedStart.set(Calendar.SECOND, 0);
                        Calendar reservedEnd = (Calendar) reservedStart.clone();
                        reservedEnd.add(Calendar.MINUTE, getDurationInMinutes(duration));
                        reservedEnd.set(Calendar.SECOND, 29);
                        Calendar current = (Calendar) reservedStart.clone();
                        current.set(Calendar.SECOND, 30);

                        for(String ts: timeSlots){
                            String tsSplit = ts.split(" - ")[0];
                            current.set(Calendar.HOUR_OF_DAY, Integer.parseInt(tsSplit.split(":")[0]));
                            current.set(Calendar.MINUTE, Integer.parseInt(tsSplit.split(":")[1]));
                            if(reservedStart.before(current) && reservedEnd.after(current)){
                                reservedSlots.set(timeSlots.indexOf(ts), true);
                            }
                        }
                    }
                    // Update UI
                    timeSlotAdapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> Log.e("Firebase", "Failed to fetch reservations"));
    }

    private boolean checkAvailability(String reservationTime) {
        Calendar current = Calendar.getInstance();
        current.set(Calendar.HOUR_OF_DAY, Integer.parseInt(reservationTime.split(":")[0]));
        current.set(Calendar.MINUTE, Integer.parseInt(reservationTime.split(":")[1]));
        current.set(Calendar.SECOND, 0);
        Calendar end = (Calendar) current.clone();
        end.add(Calendar.MINUTE, getDurationInMinutes(durations[durationPicker.getValue()]));
        end.set(Calendar.SECOND, 59);
        Calendar checking = (Calendar) current.clone();
        for(String ts: timeSlots){
            String tsSplit = ts.split(" - ")[0];
            checking.set(Calendar.HOUR_OF_DAY, Integer.parseInt(tsSplit.split(":")[0]));
            checking.set(Calendar.MINUTE, Integer.parseInt(tsSplit.split(":")[1]));
            checking.set(Calendar.SECOND, 30);
            if(reservedSlots.get(timeSlots.indexOf(ts))){
                if(checking.after(current) && checking.before(end)){
                    Toast.makeText(getContext(), "Time slot already reserved", Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
        }
        return true;
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
