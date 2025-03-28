package com.example.roommanager;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
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

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.util.Objects;

public class MyReservationsActivity extends AppCompatActivity {

    private Context context;
    private LinearLayout buttonContainer;
    private FirebaseDatabase database;
    private String userId;
    private ProgressBar progressBar;

    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_my_reservations);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        context = this;
        buttonContainer = findViewById(R.id.buttonContainer);
        buttonContainer.setOrientation(LinearLayout.VERTICAL);

        userId = Objects.requireNonNull(FirebaseAuth.getInstance().getCurrentUser()).getUid();
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
                for (DataSnapshot roomSnapshot : snapshot.getChildren()) {
                    String roomName = roomSnapshot.getKey();
                    for (DataSnapshot reservationSnapshot : roomSnapshot.getChildren()) {
                        if (Objects.equals(reservationSnapshot.getKey(), userId)) {
                            String startTime = reservationSnapshot.child("startTime").getValue(String.class);
                            String duration = reservationSnapshot.child("duration").getValue(String.class);

                            Button reservationButton = createReservationButton(roomName, startTime, duration, reservationSnapshot.getRef());

                            buttonContainer.addView(reservationButton);
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

    private Button createReservationButton(String roomName, String startTime, String duration, DatabaseReference reservationRef) {
        Button button = new Button(context);
        button.setText(roomName + " - " + startTime + " (" + duration + ")");
        button.setAllCaps(false);
        button.setBackgroundResource(R.drawable.rounded_button);
        button.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#64AECA")));
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
        button.setOnClickListener(view -> showReservationDetails(roomName, startTime, duration, reservationRef));
        return button;
    }

    private void showReservationDetails(String roomName, String startTime, String duration, DatabaseReference reservationRef) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Reservation Details");
        builder.setMessage("Room: " + roomName + "\nStart Time: " + startTime + "\nDuration: " + duration);

        builder.setPositiveButton("Edit", (dialog, which) -> editReservation(reservationRef));
        builder.setNegativeButton("Delete", (dialog, which) -> deleteReservation(reservationRef));
        builder.setNeutralButton("Close", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(R.drawable.rounded_button);
        dialog.show();
    }

    private void editReservation(DatabaseReference reservationRef) {
        // Implement your edit functionality here
        Toast.makeText(this, "Edit feature coming soon", Toast.LENGTH_SHORT).show();
    }

    private void deleteReservation(DatabaseReference reservationRef) {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage("Are you sure you want to delete this reservation?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    reservationRef.removeValue().addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            Toast.makeText(this, "Reservation deleted", Toast.LENGTH_SHORT).show();
                            loadUserReservations(); // Refresh the list
                        } else {
                            Toast.makeText(this, "Failed to delete reservation", Toast.LENGTH_SHORT).show();
                        }
                    });                })
                .setNegativeButton("No", (dialog, which) -> dialog.dismiss());

        AlertDialog dialog = builder.create();

        dialog.show();
    }

    public void onHomeButtonClick(View view) {
        startActivity(new Intent(this, HomeActivity.class));
    }
}
