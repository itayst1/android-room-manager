package com.example.roommanager.Activities;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;

import com.example.roommanager.Objects.Reservation;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.view.ContextThemeWrapper;

import com.example.roommanager.R;
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
import java.util.TreeMap;

public class AdminViewReservationsActivity extends AppCompatActivity {

    private ProgressBar progressBar;
    private LinearLayout ReservationsRecycler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_view_reservations);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        Button btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            startActivity(new Intent(AdminViewReservationsActivity.this, AdminPanelActivity.class));
        });

        progressBar = findViewById(R.id.progress_bar_sign_out);
        ReservationsRecycler = findViewById(R.id.ReservationsView);
        ReservationsRecycler.setOrientation(LinearLayout.VERTICAL);

        loadUserReservations();
        autoUpdateReservations();
    }

    private void autoUpdateReservations() {
        FirebaseDatabase.getInstance()
                .getReference("reservations/")
                .addValueEventListener(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        loadUserReservations();
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        Log.e("Firebase", "Listener cancelled: " + error.getMessage());
                    }
                });
    }

    private void loadUserReservations() {
        progressBar.setVisibility(View.VISIBLE);
        ReservationsRecycler.removeAllViews();

        DatabaseReference reservationsRef = FirebaseDatabase.getInstance().getReference("reservations");
        reservationsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                ReservationsRecycler.removeAllViews();

                // Map from date to list of reservations for that date
                Map<String, List<ReservationWithRef>> groupedReservations = new TreeMap<>(); // TreeMap to keep dates sorted

                for (DataSnapshot dateSnapshot : snapshot.getChildren()) {
                    String date = dateSnapshot.getKey().replace("-", "/");
                    for (DataSnapshot roomSnapshot : dateSnapshot.getChildren()) {
                        String roomName = roomSnapshot.getKey();
                        for (DataSnapshot reservationSnapshot : roomSnapshot.getChildren()) {
                            Reservation reservation = reservationSnapshot.getValue(Reservation.class);
                            if (reservation != null) {
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
                    ReservationsRecycler.addView(createTextView("No reservations found"));
                } else {
                    // For each date, sort the reservations by startTime and add header + buttons
                    for (String date : groupedReservations.keySet()) {
                        List<ReservationWithRef> reservationsForDate = groupedReservations.get(date);
                        // Sort by start time
                        Collections.sort(reservationsForDate);

                        // Add date header
                        TextView dateHeader = new TextView(getBaseContext());
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

                        ReservationsRecycler.addView(dateHeader);

                        // Add all reservation buttons under this date
                        for (ReservationWithRef res : reservationsForDate) {
                            View view = createReservationRow(res);
                            ReservationsRecycler.addView(view);
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

    public void sendEmailNotification(Context context, String userEmail, String reservationInfo) {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");

        String subject = "Reservation Cancellation Notice";
        String body = "Hello,\n\n" +
                "We wanted to inform you that your reservation has been cancelled.\n\n" +
                "Reservation details:\n" + reservationInfo + "\n\n" +
                "If you have any questions, feel free to contact על\"ה supervisor.\n\n" +
                "Thank you.";

        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{userEmail});
        intent.putExtra(Intent.EXTRA_SUBJECT, subject);
        intent.putExtra(Intent.EXTRA_TEXT, body);

        try {
            context.startActivity(Intent.createChooser(intent, "Send Email"));
        } catch (ActivityNotFoundException e) {
            Toast.makeText(context, "No email app found.", Toast.LENGTH_SHORT).show();
        }
    }


    private LinearLayout createReservationRow(ReservationWithRef reservation) {
        Context context = getBaseContext();

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setBackgroundResource(R.drawable.rounded_button_color);
        row.setPadding(30, 40, 30, 40);
        row.setGravity(Gravity.CENTER_VERTICAL);

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        rowParams.setMargins(20, 20, 20, 0);
        row.setLayoutParams(rowParams);

        // Text
        TextView textView = new TextView(context);
        textView.setText(reservation.reservation.userEmail + "\n" + "Room: " + reservation.roomName + " | " + reservation.reservation.startTime + "-" + reservation.reservation.endTime);
        textView.setTextColor(Color.WHITE);
        textView.setTextSize(18);
        textView.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1 // weight so it expands to fill space
        ));

        // Trash icon
        ImageButton deleteButton = new ImageButton(context);
        deleteButton.setImageResource(R.drawable.ic_trash);
        deleteButton.setBackgroundColor(Color.TRANSPARENT);
        deleteButton.setScaleType(ImageButton.ScaleType.CENTER_INSIDE);
        deleteButton.setPadding(30, 0, 30, 0);
        deleteButton.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        deleteButton.setOnClickListener(v -> deleteReservation(reservation));

        row.addView(textView);
        row.addView(deleteButton);

        return row;
    }


    private TextView createTextView(String text) {
        TextView textView = new TextView(getBaseContext());
        textView.setText(text);
        textView.setTextColor(getColor(R.color.white));
        textView.setTextSize(30);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        params.gravity = Gravity.CENTER_HORIZONTAL; // Centers the TextView inside the parent
        textView.setLayoutParams(params);
        textView.setGravity(Gravity.CENTER);
        textView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER); // Centers text inside TextView

        return textView;
    }

    private void deleteReservation(ReservationWithRef reservation) {
        DatabaseReference reservationRef = reservation.ref;
        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.CustomDialogTheme));
        builder.setMessage("Are you sure you want to delete this reservation?\nPressing \"Yes\" will delete the reservation and prompt you to send a cancellation to the user.")
                .setPositiveButton("Yes", (dialog, which) -> {
                    String requestCode = reservationRef.getKey();
                    reservationRef.removeValue().addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            assert requestCode != null;
                            Toast.makeText(this, "Reservation deleted", Toast.LENGTH_SHORT).show();
                            loadUserReservations(); // Refresh the list
                            sendEmailNotification(this, reservation.reservation.getUserEmail(), "Room: " + reservation.roomName + "\nDate: " + reservation.date + "\nTime: " + reservation.reservation.getStartTime() + "-" + reservation.reservation.getEndTime());
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

    private static class ReservationWithRef implements Comparable<AdminViewReservationsActivity.ReservationWithRef> {
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
        public int compareTo(AdminViewReservationsActivity.ReservationWithRef other) {
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