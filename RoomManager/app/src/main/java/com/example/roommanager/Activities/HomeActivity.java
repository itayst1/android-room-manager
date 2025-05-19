package com.example.roommanager.Activities;

import static android.content.ContentValues.TAG;

import android.Manifest;
import android.app.Activity;
import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.os.Build;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.appcompat.widget.Toolbar;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.credentials.ClearCredentialStateRequest;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.exceptions.ClearCredentialException;

import com.example.roommanager.Dialogs.ReportDialog;
import com.example.roommanager.NotificationReceiver;
import com.example.roommanager.R;
import com.example.roommanager.Dialogs.ReserveRoomDialog;
import com.example.roommanager.Reservation;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
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
import java.util.concurrent.Executors;

public class HomeActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private CredentialManager credentialManager;
    private ProgressBar progressBar;
    private ImageButton reserveButton;
    private FloatingActionButton adminButton;
    private LinearLayout myReservationsRecycler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        mAuth = FirebaseAuth.getInstance();
        credentialManager = CredentialManager.create(this);

        setupToolbar();
        initializeUI();
        setupButtonListeners();

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.POST_NOTIFICATIONS}, 1);
            }
        }

        requestNotificationPermission(HomeActivity.this);

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

    private void setupToolbar() {
        Toolbar toolbar = findViewById(R.id.toolbar);
        toolbar.setTitleTextColor(Color.WHITE);
        setSupportActionBar(toolbar);

        String username = (mAuth.getCurrentUser() != null) ? mAuth.getCurrentUser().getDisplayName() : null;
        String greeting = (username != null) ? "Hello, " + username : "Hello";
        Objects.requireNonNull(getSupportActionBar()).setTitle(greeting);
    }

    private void initializeUI() {
        progressBar = findViewById(R.id.progress_bar_sign_out);
        reserveButton = findViewById(R.id.addReservation);
        myReservationsRecycler = findViewById(R.id.myReservationsView);
        myReservationsRecycler.setOrientation(LinearLayout.VERTICAL);
        adminButton = findViewById(R.id.admin_panel);
        checkUserStatus();
    }

    private void setupButtonListeners() {
        reserveButton.setOnClickListener(v -> new ReserveRoomDialog().show(getSupportFragmentManager(), "ReserveRoomDialog"));
        adminButton.setOnClickListener(v -> startActivity(new Intent(this, AdminPanelActivity.class)));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
    }

    public void onSignOutClick(MenuItem item) {
        signOut();
    }

    public void onSignOutClick(View view) {
        signOut();
    }

    public void onReportClick(View view) {
        new ReportDialog().show(getSupportFragmentManager(), "ReserveRoomDialog");
    }

    public void onReportClick(MenuItem item) {
        new ReportDialog().show(getSupportFragmentManager(), "ReserveRoomDialog");
    }

    private void signOut() {
        findViewById(R.id.action_logout).setEnabled(false);
        toggleUI(false);

        mAuth.signOut();
        Log.d(TAG, "Signing out...");

        ClearCredentialStateRequest clearRequest = new ClearCredentialStateRequest();
        credentialManager.clearCredentialStateAsync(
                clearRequest,
                new CancellationSignal(),
                Executors.newSingleThreadExecutor(),
                new CredentialManagerCallback<Void, ClearCredentialException>() {
                    @Override
                    public void onResult(@NonNull Void result) {
                        runOnUiThread(() -> {
                            toggleUI(true);
                            Log.i(TAG, "Sign-out successful");
                            startActivity(new Intent(HomeActivity.this, MainActivity.class));
                            finish();
                        });
                    }

                    @Override
                    public void onError(@NonNull ClearCredentialException e) {
                        Log.e(TAG, "Couldn't clear user credentials: " + e.getLocalizedMessage());
                        runOnUiThread(() -> toggleUI(true));
                    }
                }
        );
    }

    private void toggleUI(boolean enable) {
        progressBar.setVisibility(enable ? View.GONE : View.VISIBLE);
        reserveButton.setEnabled(enable);
        myReservationsRecycler.setEnabled(enable);
        findViewById(R.id.action_logout).setEnabled(enable);
    }

    private void checkUserStatus() {
        String email = Objects.requireNonNull(mAuth.getCurrentUser()).getEmail();
        FirebaseDatabase.getInstance().getReference("settings/admins").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot adminSnapshot : snapshot.getChildren()) {
                    if (Objects.equals(adminSnapshot.getKey().replace("_", "."), email)) {
                        adminButton.setVisibility(View.VISIBLE);
                        adminButton.setSystemUiVisibility(View.VISIBLE);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                adminButton.setVisibility(View.GONE);
                adminButton.setSystemUiVisibility(View.GONE);
            }
        });
    }

    public static void requestNotificationPermission(Activity activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(activity, Manifest.permission.POST_NOTIFICATIONS)
                    != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(
                        activity,
                        new String[]{Manifest.permission.POST_NOTIFICATIONS},
                        1001 // Any unique request code
                );
            }
        }
    }

    private void loadUserReservations() {
        progressBar.setVisibility(View.VISIBLE);
        myReservationsRecycler.removeAllViews();

        DatabaseReference reservationsRef = FirebaseDatabase.getInstance().getReference("reservations");
        reservationsRef.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                myReservationsRecycler.removeAllViews();

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
                    myReservationsRecycler.addView(createTextView("No reservations found"));
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

                        myReservationsRecycler.addView(dateHeader);

                        // Add all reservation buttons under this date
                        for (ReservationWithRef res : reservationsForDate) {
                            View view = createReservationRow(
                                    res.roomName,
                                    res.reservation.getStartTime(),
                                    res.reservation.getEndTime(),
                                    res.ref);
                            myReservationsRecycler.addView(view);
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

    private LinearLayout createReservationRow(String roomName, String startTime, String endTime, DatabaseReference reservationRef) {
        Context context = getBaseContext();

        LinearLayout row = new LinearLayout(context);
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setBackgroundResource(R.drawable.rounded_background_color);
        row.setPadding(30, 40, 30, 40); // top/bottom padding helps vertical alignment
        row.setGravity(Gravity.CENTER_VERTICAL); // ✅ This centers children vertically

        LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        );
        rowParams.setMargins(20, 20, 20, 0);
        row.setLayoutParams(rowParams);

        // Text
        TextView textView = new TextView(context);
        textView.setText("Room: " + roomName + " | " + startTime + "-" + endTime);
        textView.setTextColor(Color.WHITE);
        textView.setTextSize(18);
        textView.setLayoutParams(new LinearLayout.LayoutParams(
                0,
                LinearLayout.LayoutParams.WRAP_CONTENT,
                1 // weight so it expands to fill space
        ));

        // Trash icon
        ImageButton deleteButton = new ImageButton(context);
        deleteButton.setImageResource(R.drawable.ic_trash); // your trash icon
        deleteButton.setBackgroundColor(Color.TRANSPARENT);
        deleteButton.setScaleType(ImageButton.ScaleType.CENTER_INSIDE);
        deleteButton.setPadding(30, 0, 30, 0);
        deleteButton.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));

        deleteButton.setOnClickListener(v -> deleteReservation(reservationRef));

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

    private void deleteReservation(DatabaseReference reservationRef) {
        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.CustomDialogTheme));
        builder.setMessage("Are you sure you want to delete this reservation?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    String requestCode = reservationRef.getKey();
                    reservationRef.removeValue().addOnCompleteListener(task -> {
                        if (task.isSuccessful()) {
                            assert requestCode != null;
                            cancelNotification(getBaseContext(), requestCode);
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

    private void cancelNotification(Context context, String reservationId) {
        int requestCode = reservationId.hashCode();

        Intent intent = new Intent(context, NotificationReceiver.class);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(
                context,
                requestCode,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE
        );

        AlarmManager alarmManager = (AlarmManager) context.getSystemService(Context.ALARM_SERVICE);
        alarmManager.cancel(pendingIntent);
    }
}
