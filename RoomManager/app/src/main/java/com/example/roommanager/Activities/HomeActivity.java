package com.example.roommanager.Activities;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.credentials.ClearCredentialStateRequest;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.exceptions.ClearCredentialException;

import com.example.roommanager.Dialogs.ReportDialog;
import com.example.roommanager.R;
import com.example.roommanager.Dialogs.ReserveRoomDialog;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.Objects;
import java.util.concurrent.Executors;

public class HomeActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private CredentialManager credentialManager;
    private ProgressBar progressBar;
    private Button reserveButton, myReservationsButton, adminButton;

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
        reserveButton = findViewById(R.id.reserve);
        myReservationsButton = findViewById(R.id.myReservations);
        adminButton = findViewById(R.id.admin_panel);
        checkUserStatus();
    }

    private void setupButtonListeners() {
        reserveButton.setOnClickListener(v -> new ReserveRoomDialog().show(getSupportFragmentManager(), "ReserveRoomDialog"));
        myReservationsButton.setOnClickListener(v -> startActivity(new Intent(this, MyReservationsActivity.class)));
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

    public void onReportClick(View view){
        new ReportDialog().show(getSupportFragmentManager(), "ReserveRoomDialog");
    }

    public void onReportClick(MenuItem item){
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
        myReservationsButton.setEnabled(enable);
        findViewById(R.id.action_logout).setEnabled(enable);
    }

    private void checkUserStatus(){
        String email = Objects.requireNonNull(mAuth.getCurrentUser()).getEmail();
        FirebaseDatabase.getInstance().getReference("settings/admins").addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                for (DataSnapshot adminSnapshot : snapshot.getChildren()) {
                    if(Objects.equals(adminSnapshot.getKey().replace("_", "."), email)){
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
}
