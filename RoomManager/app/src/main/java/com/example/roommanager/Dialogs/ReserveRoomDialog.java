package com.example.roommanager.Dialogs;

import android.app.AlarmManager;
import android.app.DatePickerDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.net.ConnectivityManager;
import android.net.Network;
import android.net.NetworkCapabilities;
import android.os.Bundle;
import android.util.Log;
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
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.fragment.app.DialogFragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roommanager.Activities.HomeActivity;
import com.example.roommanager.Adapters.TimeSlotAdapter;
import com.example.roommanager.NotificationReceiver;
import com.example.roommanager.R;
import com.example.roommanager.Reservation;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;

public class ReserveRoomDialog extends DialogFragment implements TimeSlotAdapter.OnTimeSlotClickListener {

    private Spinner roomSpinner;
    private NumberPicker durationPicker;
    private Button reserveButton, cancelButton, selectDateButton;
    private TextView selectedTime;

    private RecyclerView timeSlotsRecycler;
    private List<String> timeSlots = new ArrayList<>();
    private List<String> reservedSlots = new ArrayList<>();
    private List<List<String>> availableRooms = new ArrayList<>();
    private TimeSlotAdapter adapter;

    private List<String> hours = new ArrayList<>();
    private List<String> minutes = new ArrayList<>();
    private String[] durations = {"1 lesson", "2 lessons", "3 lessons", "4 lessons", "5 lessons"};

    private Context context;

    @NonNull
    @Override
    public Dialog onCreateDialog(@Nullable Bundle savedInstanceState) {
        Dialog dialog = super.onCreateDialog(savedInstanceState);
        View view = getLayoutInflater().inflate(R.layout.dialog_reserve_room, null);
        dialog.setContentView(view);
        setupDialogWindow(dialog);

        context = getContext();
        initViews(dialog);

        cancelButton.setOnClickListener(v -> dismiss());

        return dialog;
    }

    @Override
    public void onStart() {
        super.onStart();

        if (isInternetAvailable(context)) {
            loadTimeSlotsFromDatabase();
            setupDateButton();
            setupRoomSpinner();
            autoUpdateReservations();

            reserveButton.setOnClickListener(v -> handleReservation());
        } else {
            AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(context, R.style.CustomDialogTheme));
            builder.setTitle("No internet connection.");
            builder.setMessage("Please check your internet connection and try again later.");

            builder.setPositiveButton("ok", (retry, which) -> startActivity(new Intent(context, HomeActivity.class)));

            AlertDialog retry = builder.create();
            retry.getWindow().setBackgroundDrawableResource(R.drawable.rounded_background);

            retry.show();

            // Manually change the button text color
            Button positiveButton = retry.getButton(AlertDialog.BUTTON_POSITIVE);
            // Set button text color to black
            if (positiveButton != null) {
                positiveButton.setTextColor(0xFF000000);
            }
        }
    }

    private void autoUpdateReservations() {
        String selectedDate = selectDateButton.getText().toString().replace("/", "-");
        if (selectedDate.isEmpty()) return;

        FirebaseDatabase.getInstance()
                .getReference("reservations/")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        fetchReservedTimes();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("Firebase", "Listener cancelled: " + error.getMessage());
                    }
                });
    }

    private void fetchReservedTimes() {
        if (roomSpinner.getSelectedItem() == null) {
            Log.e("fetchReservedTimes", "No room type selected yet.");
            return;
        }
        // Clear existing data
        reservedSlots.clear();
        availableRooms.clear();

        // Populate the time slots (7:00 AM - 5:00 PM in 30-min intervals)
        for (String timeRange : timeSlots) {

            String endTime = timeRange.split("-")[1];

            int year = Integer.parseInt(selectDateButton.getText().toString().split("/")[2]);
            int month = Integer.parseInt(selectDateButton.getText().toString().split("/")[1]) - 1;
            int day = Integer.parseInt(selectDateButton.getText().toString().split("/")[0]);
            Calendar check = (Calendar) Calendar.getInstance().clone();
            check.set(year, month, day, Integer.parseInt(endTime.split(":")[0]), Integer.parseInt(endTime.split(":")[1]), 0);

            // Add the time range to the timeSlots list

            if (Calendar.getInstance().before(check)) {
                reservedSlots.add("Available");
            } else {
                reservedSlots.add("Past");
            }
            availableRooms.add(new ArrayList<>());
        }
        FirebaseDatabase.getInstance().getReference("settings/rooms/" + roomSpinner.getSelectedItem().toString()).get().addOnSuccessListener(dataSnapshot -> {
            for (DataSnapshot roomsSnapshot : dataSnapshot.getChildren()) {
                for (String ts : timeSlots) {
                    availableRooms.get(timeSlots.indexOf(ts)).add(Objects.requireNonNull(roomsSnapshot.getKey()));
                }
            }
        });
        if (!timeSlots.isEmpty()) {
            // Fetch reserved slots from Firebase
            FirebaseDatabase.getInstance().getReference("settings/rooms/" + roomSpinner.getSelectedItem().toString()).get().addOnSuccessListener(dataSnapshot -> {
                for (DataSnapshot roomsSnapshot : dataSnapshot.getChildren()) {
                    FirebaseDatabase.getInstance().getReference("reservations/" + selectDateButton.getText().toString().replace("/", "-") + "/" + roomsSnapshot.getKey())
                            .get().addOnSuccessListener(snapshot -> {
                                for (DataSnapshot reservation : snapshot.getChildren()) {
                                    String reservedTime = reservation.child("startTime").getValue(String.class);
                                    int year = Integer.parseInt(selectDateButton.getText().toString().split("/")[2]);
                                    int month = Integer.parseInt(selectDateButton.getText().toString().split("/")[1]) - 1;
                                    int day = Integer.parseInt(selectDateButton.getText().toString().split("/")[0]);
                                    int reservedHour = Integer.parseInt(reservedTime.split(":")[0]);
                                    int reservedMinute = Integer.parseInt(reservedTime.split(":")[1]);
                                    String endTime = reservation.child("endTime").getValue(String.class);
                                    Calendar reservedStart = (Calendar) Calendar.getInstance().clone();
                                    reservedStart.set(year, month, day, reservedHour, reservedMinute, 0);
                                    Calendar reservedEnd = (Calendar) reservedStart.clone();
                                    reservedEnd.set(Calendar.HOUR_OF_DAY, Integer.parseInt(endTime.split(":")[0]));
                                    reservedEnd.set(Calendar.MINUTE, Integer.parseInt(endTime.split(":")[1]));
                                    reservedEnd.set(Calendar.SECOND, 29);
                                    Calendar current = (Calendar) reservedStart.clone();
                                    current.set(Calendar.SECOND, 30);
                                    for (String ts : timeSlots) {
                                        String tsSplit = ts.split("-")[0];
                                        current.set(Calendar.HOUR_OF_DAY, Integer.parseInt(tsSplit.split(":")[0]));
                                        current.set(Calendar.MINUTE, Integer.parseInt(tsSplit.split(":")[1]));
                                        if ((reservedStart.before(current) && reservedEnd.after(current))) {
                                            availableRooms.get(timeSlots.indexOf(ts)).remove(Objects.requireNonNull(roomsSnapshot.getKey()));
                                            if (availableRooms.get(timeSlots.indexOf(ts)).isEmpty()) {
                                                reservedSlots.set(timeSlots.indexOf(ts), "Reserved");
                                            }
                                        } else {
                                        }
                                    }
                                }
                                // Update UI
                                adapter.notifyDataSetChanged();
                            })
                            .addOnFailureListener(e -> {
                                Log.e("Firebase", "Failed to fetch reservations");
                            });
                }
            });
        }
    }

    private boolean checkAvailability(String reservationTime, String roomNum) {
        Calendar currentReservations = (Calendar) Calendar.getInstance().clone();
        int year = Integer.parseInt(selectDateButton.getText().toString().split("/")[2]);
        int month = Integer.parseInt(selectDateButton.getText().toString().split("/")[1]) - 1;
        int day = Integer.parseInt(selectDateButton.getText().toString().split("/")[0]);
        int reserveHour = Integer.parseInt(reservationTime.split(":")[0]);
        int reserveMinute = Integer.parseInt(reservationTime.split(":")[1]);

        currentReservations.set(year, month, day, reserveHour, reserveMinute, 0);
        Calendar reservationEnd = (Calendar) currentReservations.clone();
        String endTime = Objects.requireNonNull(getEndTimeFromStart(reservationTime, durations[durationPicker.getValue()]));
        reservationEnd.set(Calendar.HOUR_OF_DAY, Integer.parseInt(endTime.split(":")[0]));
        reservationEnd.set(Calendar.MINUTE, Integer.parseInt(endTime.split(":")[1]));
        reservationEnd.set(Calendar.SECOND, 29);

        if (Calendar.getInstance().after(currentReservations)) {
            return false;
        }

        Calendar checking = (Calendar) currentReservations.clone();
        checking.set(Calendar.SECOND, 30);
        for (String ts : timeSlots) {
            String tsSplit = ts.split("-")[0];
            checking.set(Calendar.HOUR_OF_DAY, Integer.parseInt(tsSplit.split(":")[0]));
            checking.set(Calendar.MINUTE, Integer.parseInt(tsSplit.split(":")[1]));
            if (checking.after(currentReservations) && checking.before(reservationEnd) && !availableRooms.get(timeSlots.indexOf(ts)).contains(roomNum)) {
                return false;
            }
        }
        return true;
    }

    private void loadTimeSlotsFromDatabase() {
        FirebaseDatabase.getInstance().getReference("settings/lessons")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        timeSlots.clear();
                        for (DataSnapshot lessonSnapshot : snapshot.getChildren()) {
                            String lessonTime = lessonSnapshot.getValue(String.class);
                            if (lessonTime != null) {
                                timeSlots.add(lessonTime);
                                String startTime = lessonTime.split("-")[0];
                                hours.add(startTime.split(":")[0]);
                                minutes.add(startTime.split(":")[1]);
                            }
                        }

                        // Setup pickers ONLY AFTER timeSlots is filled
                        setupDurationPicker();
                        setupTimeSlotsRecycler();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("Firebase", "Failed to load lesson times", error.toException());
                    }
                });
    }

    public boolean isInternetAvailable(Context context) {
        ConnectivityManager connectivityManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (connectivityManager == null) return false;

        Network network = connectivityManager.getActiveNetwork();
        if (network == null) return false;

        NetworkCapabilities capabilities = connectivityManager.getNetworkCapabilities(network);
        return capabilities != null && (
                capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                        capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        );
    }


    @Override
    public void onTimeSlotClick(String timeSlot) {
        if (reservedSlots.get(timeSlots.indexOf(timeSlot)).equals("Reserved") || reservedSlots.get(timeSlots.indexOf(timeSlot)).equals("Past"))
            return;
        String startTime = timeSlot.split("-")[0];
        String duration = String.valueOf(durations[durationPicker.getValue()].charAt(0));
        String endTime = getEndTimeFromStart(startTime, duration);
        selectedTime.setText(startTime + "-" + endTime);
    }

    private void setupDialogWindow(Dialog dialog) {
        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            dialog.getWindow().setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }
    }

    private void initViews(Dialog dialog) {
        selectDateButton = dialog.findViewById(R.id.date_button);
        durationPicker = dialog.findViewById(R.id.duration_picker);
        roomSpinner = dialog.findViewById(R.id.room_spinner);
        reserveButton = dialog.findViewById(R.id.reserve_button);
        cancelButton = dialog.findViewById(R.id.cancel_button);
        timeSlotsRecycler = dialog.findViewById(R.id.time_slots_recycler);
        selectedTime = dialog.findViewById(R.id.selected_time);
    }

    private void setupDurationPicker() {
        durationPicker.setMinValue(0);
        durationPicker.setMaxValue(durations.length - 1);
        durationPicker.setDisplayedValues(durations);
        durationPicker.postInvalidate();  // Force redraw
        durationPicker.requestLayout();

        durationPicker.setOnValueChangedListener(new NumberPicker.OnValueChangeListener() {
            @Override
            public void onValueChange(NumberPicker numberPicker, int i, int i1) {
                if (!selectedTime.getText().toString().isEmpty()) {
                    String startTime = selectedTime.getText().toString().split("-")[0];
                    String duration = String.valueOf(durations[durationPicker.getValue()].charAt(0));
                    String endTime = getEndTimeFromStart(startTime, duration);
                    selectedTime.setText(startTime + "-" + endTime);
                }
            }
        });
    }

    private void setupDateButton() {
        final Calendar selectedDateTime = (Calendar) Calendar.getInstance().clone();
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
                selectedTime.setText("");
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
                Collections.sort(roomTypes);

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
                selectedTime.setText("");
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
        adapter = new TimeSlotAdapter(timeSlots, this, reservedSlots);
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

    private void handleReservation() {
        if (selectedTime.getText().toString().isEmpty()) {
            Toast.makeText(context, "Select a time slot", Toast.LENGTH_SHORT).show();
            return;
        }
        if(selectedTime.getText().toString().split("-")[1].equals("Out of bounds!")){
            Toast.makeText(context, "Selected time is out of bounds!", Toast.LENGTH_SHORT).show();
            return;
        }
        String email = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getEmail();
        String roomType = roomSpinner.getSelectedItem().toString();
        String duration = durations[durationPicker.getValue()];
        String date = selectDateButton.getText().toString().replace("/", "-");
        String time = selectedTime.getText().toString().split("-")[0];
        String hour = time.split(":")[0];
        String minute = time.split(":")[1];
        Calendar endTimeCal = (Calendar) Calendar.getInstance().clone();
        endTimeCal.set(Calendar.HOUR_OF_DAY, Integer.parseInt(hour));
        endTimeCal.set(Calendar.MINUTE, Integer.parseInt(minute));
        String endTime = selectedTime.getText().toString().split("-")[1];
        if (availableRooms.get(timeSlots.indexOf(time + "-" + getEndTimeFromStart(time, "1"))).isEmpty()) {
            Toast.makeText(context, "No available rooms", Toast.LENGTH_SHORT).show();
            return;
        }
        boolean didReserve = false;
        for (String room : availableRooms.get(timeSlots.indexOf(time + "-" + getEndTimeFromStart(time, "1")))) {
            if (checkAvailability(time, room)) {
                didReserve = true;
                Reservation reservation = new Reservation(email, time, endTime);
                DatabaseReference reservationRef = FirebaseDatabase.getInstance()
                        .getReference("reservations/" + date + "/" + room).push();
                String reservationId = reservationRef.getKey();
                reservationRef.setValue(reservation)
                        .addOnSuccessListener(aVoid -> {
                            scheduleNotification(context, selectDateButton.getText().toString(), time, room, reservationId);
                            Toast.makeText(context, "Room " + room + " reserved!", Toast.LENGTH_SHORT).show();
                            dismiss();
                        })
                        .addOnFailureListener(e ->
                                Toast.makeText(context, "Reservation failed", Toast.LENGTH_SHORT).show());
                break;
            }
        }
        if (!didReserve) {
            Toast.makeText(context, "Reservation failed", Toast.LENGTH_SHORT).show();
        }
    }

    private String getEndTimeFromStart(String time, String duration) {
        // Parse how many lessons (e.g., "2 lessons" → 2)
        int durationLessons = Character.getNumericValue(duration.charAt(0));

        // Find the index of the time slot that starts with the given time
        int startIndex = -1;
        for (int i = 0; i < timeSlots.size(); i++) {
            String slot = timeSlots.get(i);
            if (slot.startsWith(time)) {
                startIndex = i;
                break;
            }
        }

        if (startIndex == -1) {
            return null; // Invalid time or out of bounds
        }

        if (startIndex + durationLessons - 1 >= timeSlots.size()) {
            return "Out of bounds!";
        }

        // Get the end time of the last lesson in the range
        String lastSlot = timeSlots.get(startIndex + durationLessons - 1);
        return lastSlot.split("-")[1];
    }

    public void scheduleNotification(Context context, String date, String startTime, String room, String reservationId) {
        SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault());
        try {
            String dateTimeStr = date + " " + startTime;
            Calendar reservationTime = (Calendar) Calendar.getInstance().clone();
            reservationTime.setTime(Objects.requireNonNull(sdf.parse(dateTimeStr)));

            // Subtract 1 hour for the notification trigger
            reservationTime.add(Calendar.HOUR_OF_DAY, -1);

            long triggerAtMillis = reservationTime.getTimeInMillis();

            if (triggerAtMillis > System.currentTimeMillis()) {
                Intent intent = new Intent(context, NotificationReceiver.class);
                String message = "Room: " + room + " at " + startTime + " on " + date;
                intent.putExtra("reservationDetails", message);
                intent.putExtra("room", room);
                intent.putExtra("date", date);
                intent.putExtra("time", startTime);

                PendingIntent pendingIntent = PendingIntent.getBroadcast(
                        context,
                        reservationId.hashCode(), // unique request code
                        intent,
                        PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
                );
                AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, triggerAtMillis, pendingIntent);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
    }
}
