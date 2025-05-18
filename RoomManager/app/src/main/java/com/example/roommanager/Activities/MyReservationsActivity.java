package com.example.roommanager.Activities;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ContextThemeWrapper;

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
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

public class MyReservationsActivity extends AppCompatActivity {

    private Context context;
    private LinearLayout buttonContainer;
    private FirebaseDatabase database;
    private ProgressBar progressBar;
    Context style;

    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_reservations);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        context = this;
        buttonContainer = findViewById(R.id.buttonContainer);
        buttonContainer.setOrientation(LinearLayout.VERTICAL);

        style = new ContextThemeWrapper(this, R.style.CustomDialogTheme);

        database = FirebaseDatabase.getInstance();

        progressBar = findViewById(R.id.progress_bar_load_reservations);

        loadUserReservations();
    }

    private void loadUserReservations() {
        progressBar.setVisibility(View.VISIBLE);
        buttonContainer.removeAllViews();

        DatabaseReference reservationsRef = database.getReference("reservations");
        reservationsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                buttonContainer.removeAllViews();

                // Map from date to list of reservations for that date
                Map<String, List<ReservationWithRef>> groupedReservations = new TreeMap<>(); // TreeMap to keep dates sorted

                for (DataSnapshot dateSnapshot : snapshot.getChildren()) {
                    String date = dateSnapshot.getKey().replace("-", "/");
                    for (DataSnapshot roomSnapshot : dateSnapshot.getChildren()) {
                        String roomName = roomSnapshot.getKey();
                        for (DataSnapshot reservationSnapshot : roomSnapshot.getChildren()) {
                            Reservation reservation = reservationSnapshot.getValue(Reservation.class);
                            if (reservation != null &&
                                    reservation.getUserEmail() != null &&
                                    reservation.getUserEmail().equals(FirebaseAuth.getInstance().getCurrentUser().getEmail())) {

                                ReservationWithRef resWithRef = new ReservationWithRef(date, roomName, reservation, reservationSnapshot.getRef());

                                if (!groupedReservations.containsKey(date)) {
                                    groupedReservations.put(date, new ArrayList<>());
                                }
                                groupedReservations.get(date).add(resWithRef);
                            }
                        }
                    }
                }

                if (groupedReservations.isEmpty()) {
                    buttonContainer.addView(createTextView("No reservations found"));
                } else {
                    // For each date, sort the reservations by startTime and add header + buttons
                    for (String date : groupedReservations.keySet()) {
                        List<ReservationWithRef> reservationsForDate = groupedReservations.get(date);
                        // Sort by start time
                        Collections.sort(reservationsForDate);

                        // Add date header
                        TextView dateHeader = new TextView(context);
                        dateHeader.setText(date);
                        dateHeader.setTextSize(24);
                        dateHeader.setTextColor(Color.WHITE);
                        dateHeader.setAllCaps(true);
                        dateHeader.setPadding(20, 40, 20, 20);

                        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                                LinearLayout.LayoutParams.MATCH_PARENT,
                                LinearLayout.LayoutParams.WRAP_CONTENT
                        );
                        params.gravity = Gravity.CENTER_HORIZONTAL;
                        dateHeader.setLayoutParams(params);

                        dateHeader.setGravity(Gravity.CENTER);

                        buttonContainer.addView(dateHeader);

                        // Add all reservation buttons under this date
                        for (ReservationWithRef res : reservationsForDate) {
                            Button btn = createReservationButton(
                                    res.date,
                                    res.roomName,
                                    res.reservation.getStartTime(),
                                    res.reservation.getEndTime(),
                                    res.ref);
                            buttonContainer.addView(btn);
                        }
                    }
                }
                progressBar.setVisibility(View.GONE);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                progressBar.setVisibility(View.GONE);
                Log.e("Reservation", "Failed to fetch reservations: " + error.getMessage());
            }
        });
    }


    private Button createReservationButton(String date, String roomName, String startTime, String endTime, DatabaseReference reservationRef) {
        Button button = new Button(context);
        button.setText(date + " | Room: " + roomName + " | " + startTime + "-" + endTime);
        button.setAllCaps(false);
        button.setBackgroundResource(R.drawable.rounded_button_color);
        button.setTextColor(Color.WHITE);
        button.setTextSize(18);
        button.setPadding(10, 50, 10, 50);
        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.setMargins(20, 20, 20, 20); // Left, Top, Right, Bottom margins
        button.setLayoutParams(params);
        button.setOnClickListener(view -> {
        });
        button.setOnClickListener(view -> showReservationDetails(date, roomName, startTime, endTime, reservationRef));
        return button;
    }

    private TextView createTextView(String text) {
        TextView textView = new TextView(context);
        textView.setText(text);
        textView.setTextColor(getColor(R.color.white));
        textView.setTextSize(30);
        textView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        return textView;
    }

    private void showReservationDetails(String date, String roomName, String startTime, String endTime, DatabaseReference reservationRef) {
        AlertDialog.Builder builder = new AlertDialog.Builder(style);
        builder.setTitle("Reservation Details");
        builder.setMessage("Date: " + date + "\nRoom: " + roomName + "\nReserved time: " + startTime + "-" + endTime);

        builder.setNegativeButton("Delete", (dialog, which) -> deleteReservation(reservationRef));
        builder.setNeutralButton("Close", (dialog, which) -> dialog.dismiss());


        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(R.drawable.rounded_background);

        dialog.show();

        // Manually change the button text color
        Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        Button neutralButton = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);

        // Set button text color to black
        if (negativeButton != null) {
            negativeButton.setTextColor(0xFF000000);
        }
        if (neutralButton != null) {
            neutralButton.setTextColor(0xFF000000);
        }
    }

    private void deleteReservation(DatabaseReference reservationRef) {
        AlertDialog.Builder builder = new AlertDialog.Builder(style);
        builder.setMessage("Are you sure you want to delete this reservation?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    reservationRef.removeValue().addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Reservation deleted", Toast.LENGTH_SHORT).show();
                            loadUserReservations(); // Refresh the list
                        } else {
                            Toast.makeText(this, "Failed to delete reservation", Toast.LENGTH_SHORT).show();
                        }
                    });
                })
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(R.drawable.rounded_background);
        dialog.show();

        // Manually change the button text color
        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);

        // Set button text color to black
        if (positiveButton != null) {
            positiveButton.setTextColor(0xFF000000);
        }
        if (negativeButton != null) {
            negativeButton.setTextColor(0xFF000000);
        }
    }

    public void onHomeButtonClick(View view) {
        startActivity(new Intent(this, HomeActivity.class));
    }

    private static class ReservationWithRef implements Comparable<ReservationWithRef> {
        public String date; // "dd/MM/yyyy"
        public String roomName;
        public Reservation reservation;
        public DatabaseReference ref;

        public ReservationWithRef(String date, String roomName, Reservation reservation, DatabaseReference ref) {
            this.date = date;
            this.roomName = roomName;
            this.reservation = reservation;
            this.ref = ref;
        }

        @Override
        public int compareTo(ReservationWithRef other) {
            try {
                SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy HH:mm");
                Date thisDateTime = sdf.parse(this.date + " " + this.reservation.getStartTime());
                Date otherDateTime = sdf.parse(other.date + " " + other.reservation.getStartTime());
                return thisDateTime.compareTo(otherDateTime);
            } catch (ParseException e) {
                return 0;
            }
        }
    }

}
