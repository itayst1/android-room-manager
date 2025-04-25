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
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.lang.reflect.Field;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Set;

public class ReserveRoomDialog extends DialogFragment implements TimeSlotAdapter.OnTimeSlotClickListener {

    private Spinner roomSpinner;
    private NumberPicker hourPicker, minutePicker, durationPicker;
    private Button reserveButton, cancelButton, selectDateButton;
    private final Calendar selectedDateTime = Calendar.getInstance();

    private RecyclerView timeSlotsRecycler;
    private TimeSlotAdapter timeSlotAdapter;
    private List<String> timeSlots = new ArrayList<>();
    private List<String> reservedSlots = new ArrayList<>();
    private List<Integer> availableRoom = new ArrayList<>();
    private TimeSlotAdapter adapter;

    private String[] hours = {"07", "08", "09", "10", "11", "12", "13", "14", "15", "16", "17"};
    private String[] minutes = {"00", "15", "30", "45"};
    private String[] durations = {"30 min", "1 hour", "1.5 hours", "2 hours", "2.5 hours", "3 hours", "3.5 hours", "4 hours"};

    private Context context;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        View view = getLayoutInflater().inflate(R.layout.dialog_reserve_room, null);
        dialog.setContentView(view);
        setupDialogWindow(dialog);

        context = getContext();
        FirebaseDatabase database = FirebaseDatabase.getInstance();

        initViews(dialog);
        setupPickers();
        setupDateButton();
        setupRoomSpinner();
        setupTimeSlotsRecycler();

        reserveButton.setOnClickListener(v -> handleReservation(database, context));
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

                int year = Integer.parseInt(selectDateButton.getText().toString().split("/")[2]);
                int month = Integer.parseInt(selectDateButton.getText().toString().split("/")[1]) - 1;
                int day = Integer.parseInt(selectDateButton.getText().toString().split("/")[0]);
                Calendar check = (Calendar) Calendar.getInstance().clone();
                check.set(year, month, day, hour, minute, 0);

                // Add the time range to the timeSlots list
                String timeRange = startTime + " - " + endTime;

                if (Calendar.getInstance().before(check)) {
                    reservedSlots.add("Available");
                } else {
                    reservedSlots.add("Past");
                }
                availableRoom.add(0);
                timeSlots.add(timeRange);
            }
        }

        if (!timeSlots.isEmpty()) {
            // Fetch reserved slots from Firebase
            FirebaseDatabase.getInstance().getReference("reservations/" + selectDateButton.getText().toString().replace("/", "-") + "/" + roomSpinner.getSelectedItem().toString())
                    .get()
                    .addOnSuccessListener(snapshot -> {
                        for (DataSnapshot roomSnapshot : snapshot.getChildren()) {
                            for(DataSnapshot reservation : roomSnapshot.getChildren()) {
                                String reservedTime = reservation.child("startTime").getValue(String.class);
                                int year = Integer.parseInt(selectDateButton.getText().toString().split("/")[2]);
                                int month = Integer.parseInt(selectDateButton.getText().toString().split("/")[1]) - 1;
                                int day = Integer.parseInt(selectDateButton.getText().toString().split("/")[0]);
                                int reservedHour = Integer.parseInt(reservedTime.split(":")[0]);
                                int reservedMinute = Integer.parseInt(reservedTime.split(":")[1]);
                                String duration = reservation.child("duration").getValue(String.class);
                                Calendar reservedStart = (Calendar) Calendar.getInstance().clone();
                                reservedStart.set(year, month, day, reservedHour, reservedMinute, 0);
                                Calendar reservedEnd = (Calendar) reservedStart.clone();
                                reservedEnd.add(Calendar.MINUTE, getDurationInMinutes(duration));
                                reservedEnd.set(Calendar.SECOND, 29);
                                Calendar current = (Calendar) reservedStart.clone();
                                current.set(Calendar.SECOND, 30);
                                for (String ts : timeSlots) {
                                    String tsSplit = ts.split(" - ")[0];
                                    current.set(Calendar.HOUR_OF_DAY, Integer.parseInt(tsSplit.split(":")[0]));
                                    current.set(Calendar.MINUTE, Integer.parseInt(tsSplit.split(":")[1]));
                                    if ((reservedStart.before(current) && reservedEnd.after(current))) {
                                        reservedSlots.set(timeSlots.indexOf(ts), "Reserved");
                                    }
                                    else {
                                        availableRoom.set(timeSlots.indexOf(ts), Integer.valueOf(Objects.requireNonNull(roomSnapshot.getKey())));
                                    }
                                }
                            }
                        }
                        // Update UI
                        adapter.notifyDataSetChanged();
                    })
                    .addOnFailureListener(e -> Log.e("Firebase", "Failed to fetch reservations"));
        }
    }

    private boolean checkAvailability(String reservationTime) {
        Calendar currentReservations = (Calendar) Calendar.getInstance().clone();
        int year = Integer.parseInt(selectDateButton.getText().toString().split("/")[2]);
        int month = Integer.parseInt(selectDateButton.getText().toString().split("/")[1]) - 1;
        int day = Integer.parseInt(selectDateButton.getText().toString().split("/")[0]);
        int reserveHour = Integer.parseInt(reservationTime.split(":")[0]);
        int reserveMinute = Integer.parseInt(reservationTime.split(":")[1]);
        currentReservations.set(year, month, day, reserveHour, reserveMinute, 0);
        Calendar reservationEnd = (Calendar) currentReservations.clone();
        reservationEnd.add(Calendar.MINUTE, getDurationInMinutes(durations[durationPicker.getValue()]));
        reservationEnd.set(Calendar.SECOND, 59);
        if (Calendar.getInstance().after(currentReservations)) {
            Toast.makeText(getContext(), "Time slot is before now", Toast.LENGTH_SHORT).show();
            return false;
        }
        Calendar checking = (Calendar) currentReservations.clone();
        checking.set(Calendar.SECOND, 30);
        for (String ts : timeSlots) {
            String tsSplit = ts.split(" - ")[0];
            checking.set(Calendar.HOUR_OF_DAY, Integer.parseInt(tsSplit.split(":")[0]));
            checking.set(Calendar.MINUTE, Integer.parseInt(tsSplit.split(":")[1]));
            if (reservedSlots.get(timeSlots.indexOf(ts)).equals("Reserved")) {
                if (checking.after(currentReservations) && checking.before(reservationEnd)) {
                    Toast.makeText(getContext(), "Time slot already reserved", Toast.LENGTH_SHORT).show();
                    return false;
                }
            }
        }
        return true;
    }

    private List<String> getTimeSlots() {
        List<String> timeSlots = new ArrayList<>();
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
            }
        }
        return timeSlots;
    }

    @Override
    public void onTimeSlotClick(String timeSlot) {
        if (reservedSlots.get(timeSlots.indexOf(timeSlot)).equals("Reserved") || reservedSlots.get(timeSlots.indexOf(timeSlot)).equals("Past"))
            return;
        // Parse the clicked time slot
        String[] times = timeSlot.split(" - ");
        String startTime = times[0];

        // Extract hour and minute from the start time
        String[] timeParts = startTime.split(":");
        int hour = Integer.parseInt(timeParts[0]);
        int minute = Integer.parseInt(timeParts[1]);

        // Set the time pickers to the clicked time
        hourPicker.setValue(hour - 7);
        minutePicker.setValue(minute / 15); // For 15-minute intervals, multiply by 15
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

    private void setupDialogWindow(Dialog dialog) {
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    private void initViews(Dialog dialog) {
        selectDateButton = dialog.findViewById(R.id.date_button);
        hourPicker = dialog.findViewById(R.id.hour_picker);
        minutePicker = dialog.findViewById(R.id.minute_picker);
        durationPicker = dialog.findViewById(R.id.duration_picker);
        roomSpinner = dialog.findViewById(R.id.room_spinner);
        reserveButton = dialog.findViewById(R.id.reserve_button);
        cancelButton = dialog.findViewById(R.id.cancel_button);
        timeSlotsRecycler = dialog.findViewById(R.id.time_slots_recycler);
    }

    private void setupPickers() {
        hourPicker.setMinValue(0);
        hourPicker.setMaxValue(hours.length - 1);
        hourPicker.setDisplayedValues(hours);

        minutePicker.setMinValue(0);
        minutePicker.setMaxValue(minutes.length - 1);
        minutePicker.setDisplayedValues(minutes);

        durationPicker.setMinValue(0);
        durationPicker.setMaxValue(durations.length - 1);
        durationPicker.setDisplayedValues(durations);
    }

    private void setupDateButton() {
        final Calendar selectedDateTime = Calendar.getInstance();
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd/M/yyyy", Locale.getDefault());
        selectDateButton.setText(dateFormat.format(selectedDateTime.getTime()));

        selectDateButton.setOnClickListener(v -> {
            int year = selectedDateTime.get(Calendar.YEAR);
            int month = selectedDateTime.get(Calendar.MONTH);
            int day = selectedDateTime.get(Calendar.DAY_OF_MONTH);

            DatePickerDialog datePickerDialog = new DatePickerDialog(requireContext(), (view, y, m, d) -> {
                selectedDateTime.set(y, m, d);
                selectDateButton.setText(d + "/" + (m + 1) + "/" + y);
                fetchReservedTimes();
            }, year, month, day);

            datePickerDialog.getDatePicker().setMinDate(Calendar.getInstance().getTimeInMillis());
            datePickerDialog.show();
        });
    }

    private void setupRoomSpinner() {
        DatabaseReference roomsRef = FirebaseDatabase.getInstance().getReference("settings/rooms");
        ArrayAdapter<String> adapter = new ArrayAdapter<>(requireContext(), android.R.layout.simple_spinner_dropdown_item);
        roomSpinner.setAdapter(adapter);

        roomsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                List<String> roomTypes = new ArrayList<>();

                for (DataSnapshot typeSnapshot : snapshot.getChildren()) {
                    String roomType = typeSnapshot.getKey();
                    if (roomType != null) {
                        roomTypes.add(roomType);
                    }
                }
                 Collections.reverse(roomTypes);

                adapter.clear();
                adapter.addAll(roomTypes);
                adapter.notifyDataSetChanged();

                // Optional: select the first item and update UI
                if (!roomTypes.isEmpty()) {
                    roomSpinner.setSelection(0);
                    fetchReservedTimes();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(getContext(), "Failed to load room types", Toast.LENGTH_SHORT).show();
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
            public void onNothingSelected(AdapterView<?> parent) {
            }
        });

        roomSpinner.post(() -> {
            View selectedView = roomSpinner.getSelectedView();
            if (selectedView instanceof TextView) {
                ((TextView) selectedView).setTextColor(Color.WHITE);
            }
        });
    }


    private void setupTimeSlotsRecycler() {
        timeSlotsRecycler.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new TimeSlotAdapter(getTimeSlots(), this, reservedSlots);
        timeSlotsRecycler.setAdapter(adapter);

        timeSlotsRecycler.addItemDecoration(new RecyclerView.ItemDecoration() {
            @Override
            public void onDraw(@NonNull Canvas c, @NonNull RecyclerView parent, @NonNull RecyclerView.State state) {
                int height = parent.getHeight();
                float fadeHeight = height * 0.03f;

                for (int i = 0; i < parent.getChildCount(); i++) {
                    View child = parent.getChildAt(i);
                    float itemY = child.getY(), itemBottom = itemY + child.getHeight(), alpha = 1f;

                    if (itemY < fadeHeight)
                        alpha = Math.max(0f, itemY / fadeHeight);
                    else if (itemBottom > height - fadeHeight)
                        alpha = Math.max(0f, (height - itemBottom) / fadeHeight);

                    child.setAlpha(alpha);
                }
            }
        });
    }

    private void handleReservation(FirebaseDatabase database, Context context) {
        String email = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getEmail();
        String roomType = roomSpinner.getSelectedItem().toString();
        String date = selectDateButton.getText().toString();
        String hour = hours[hourPicker.getValue()];
        String minute = minutes[minutePicker.getValue()];
        String time = hour + ":" + minute;
        String duration = durations[durationPicker.getValue()];

        if (checkAvailability(time)) {
            Reservation reservation = new Reservation(email, time, duration);
            dismiss();

            DatabaseReference roomsRef = database.getReference("settings/rooms/" + roomType);
            roomsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    List<String> availableRooms = new ArrayList<>();
                    for (DataSnapshot roomSnapshot : snapshot.getChildren()) {
                        availableRooms.add(roomSnapshot.getKey()); // room number (e.g., "1", "2", etc.)
                    }

                    checkAvailableRoomAndReserve(availableRooms);
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                    Toast.makeText(context, "Failed to load room list", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void checkAvailableRoomAndReserve(List<String> roomNumbers) {
        String selectedDate = selectDateButton.getText().toString().replace("/", "-");
        String selectedTime = hours[hourPicker.getValue()] + ":" + minutes[minutePicker.getValue()];
        String selectedDuration = durations[durationPicker.getValue()];
        String userEmail = FirebaseAuth.getInstance().getCurrentUser().getEmail();

        for (String room : roomNumbers) {
            DatabaseReference roomReservationsRef = FirebaseDatabase.getInstance().getReference("reservations/" + selectedDate + "/" + room);
            roomReservationsRef.addListenerForSingleValueEvent(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (!isTimeConflict(snapshot, selectedTime, selectedDuration)) {
                        // Reserve this room
                        Reservation reservation = new Reservation(userEmail, selectedTime, selectedDuration);
                        roomReservationsRef.push().setValue(reservation)
                                .addOnSuccessListener(aVoid -> {
                                    Toast.makeText(context, "Room " + room + " reserved!", Toast.LENGTH_SHORT).show();
                                    dismiss();
                                })
                                .addOnFailureListener(e ->
                                        Toast.makeText(context, "Reservation failed", Toast.LENGTH_SHORT).show());
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                }
            });
            break; // Only try to reserve one room (first available)
        }
    }

    private boolean isTimeConflict(DataSnapshot snapshot, String newStartTime, String newDuration) {
        int newStart = timeToMinutes(newStartTime);
        int newEnd = newStart + parseDuration(newDuration);

        for (DataSnapshot reservationSnapshot : snapshot.getChildren()) {
            String reservedStartTime = reservationSnapshot.child("time").getValue(String.class);
            String reservedDuration = reservationSnapshot.child("duration").getValue(String.class);

            if (reservedStartTime == null || reservedDuration == null) continue;

            int reservedStart = timeToMinutes(reservedStartTime);
            int reservedEnd = reservedStart + parseDuration(reservedDuration);

            // Check for time overlap
            if (newStart < reservedEnd && newEnd > reservedStart) {
                return true; // Conflict found
            }
        }
        return false; // No conflicts
    }

    private int timeToMinutes(String time) {
        String[] parts = time.split(":");
        int hour = Integer.parseInt(parts[0]);
        int minute = Integer.parseInt(parts[1]);
        return hour * 60 + minute;
    }

    private int parseDuration(String durationStr) {
        if (durationStr.contains("hour")) {
            double hours = Double.parseDouble(durationStr.replace(" hours", "").replace(" hour", ""));
            return (int) (hours * 60);
        } else if (durationStr.contains("minute")) {
            return Integer.parseInt(durationStr.replace(" minutes", "").replace(" minute", ""));
        }
        return 0;
    }

}
