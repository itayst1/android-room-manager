package com.example.roommanager;

import static android.content.ContentValues.TAG;

import android.content.Intent;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        mAuth = FirebaseAuth.getInstance();

        credentialManager = CredentialManager.create(getBaseContext());

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        String username = mAuth.getCurrentUser().getDisplayName();
        if(username != null)
            Objects.requireNonNull(getSupportActionBar()).setTitle("Hello, " + mAuth.getCurrentUser().getDisplayName());
        else
            Objects.requireNonNull(getSupportActionBar()).setTitle("Hello");

        progressBar = findViewById(R.id.progress_bar_sign_out);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.toolbar_menu, menu);
        return true;
    }

    public void onSignOutClick(MenuItem item){
        signOut();
    }

    private void signOut() {
        findViewById(R.id.action_logout).setEnabled(false);

        progressBar.setVisibility(View.VISIBLE);

        // Firebase sign out
        mAuth.signOut();
        Log.d("test", "signing out");

        // When a user signs out, clear the current user credential state from all credential providers.
        ClearCredentialStateRequest clearRequest = new ClearCredentialStateRequest();
        credentialManager.clearCredentialStateAsync(
                clearRequest,
                new CancellationSignal(),
                Executors.newSingleThreadExecutor(),
                new CredentialManagerCallback<Void, ClearCredentialException>() {

                    @Override
                    public void onResult(@NonNull Void result) {
                        runOnUiThread(() -> {
                            // Hide ProgressBar after the operation
                            progressBar.setVisibility(View.GONE);
                            // Re-enable the button
                            findViewById(R.id.action_logout).setEnabled(true);
                            // Navigate to the MainActivity
                            Log.i("logOut", "success");
                            startActivity(new Intent(HomeActivity.this, MainActivity.class));
                            finish();  // Finish current activity to prevent returning to it
                        });
                    }

                    @Override
                    public void onError(@NonNull ClearCredentialException e) {
                        Log.e(TAG, "Couldn't clear user credentials: " + e.getLocalizedMessage());
                    }
                });
    }
}
