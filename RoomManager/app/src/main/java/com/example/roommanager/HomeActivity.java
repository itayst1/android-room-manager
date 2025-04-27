package com.example.roommanager;

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

import com.google.firebase.auth.FirebaseAuth;

import java.util.Objects;
import java.util.concurrent.Executors;

public class HomeActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private CredentialManager credentialManager;
    private ProgressBar progressBar;
    private Button reserveButton, myReservationsButton;

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
    }

    private void setupButtonListeners() {
        reserveButton.setOnClickListener(v -> new ReserveRoomDialog().show(getSupportFragmentManager(), "ReserveRoomDialog"));
        myReservationsButton.setOnClickListener(v -> startActivity(new Intent(this, MyReservationsActivity.class)));
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
}
