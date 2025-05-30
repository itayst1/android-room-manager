package com.example.roommanager.Activities;

import static com.google.android.libraries.identity.googleid.GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL;

import android.content.Intent;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.util.Log;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.credentials.Credential;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;

import com.example.roommanager.R;
import com.example.roommanager.creds;
import com.google.android.gms.common.SignInButton;
import com.google.android.libraries.identity.googleid.GetSignInWithGoogleOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.concurrent.Executors;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;

    private CredentialManager credentialManager;

    private SignInButton signIn;

    private ProgressBar progressBar;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        mAuth = FirebaseAuth.getInstance();

        credentialManager = CredentialManager.create(getBaseContext());

        signIn = findViewById(R.id.sign_in);
        signIn.setOnClickListener(this::onSignInClick);

        progressBar = findViewById(R.id.progress_bar_sign_in);
    }

    public void onSignInClick(View view) {
        launchCredentialManager();
    }

    private void launchCredentialManager() {
        // [START create_credential_manager_request]
        // Instantiate a Google sign-in request
        GetSignInWithGoogleOption googleIdOption = new GetSignInWithGoogleOption.Builder(com.example.roommanager.creds.default_web_client_id).build();

        // Create the Credential Manager request
        GetCredentialRequest request = new GetCredentialRequest.Builder().addCredentialOption(googleIdOption).build();
        // [END create_credential_manager_request]

        runOnUiThread(() -> {
            progressBar.setVisibility(View.VISIBLE);
        });

        // Launch Credential Manager UI
        credentialManager.getCredentialAsync(
                getBaseContext(),
                request,
                new CancellationSignal(),
                Executors.newSingleThreadExecutor(),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {

                    @Override
                    public void onResult(GetCredentialResponse result) {
                        // Extract credential from the result returned by Credential Manager
                        handleSignIn(result.getCredential());
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                        });
                    }

                    @Override
                    public void onError(GetCredentialException e) {
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                            Toast.makeText(MainActivity.this, "Sign in failed. Please try again or use a different Email.", Toast.LENGTH_LONG).show();
                        });
                        mAuth.signOut();
                        Log.e("sign in", "Couldn't retrieve user's credentials: " + e.getLocalizedMessage());
                    }
                }
        );
    }

    private void handleSignIn(Credential credential) {
        // Check if credential is of type Google ID
        if (credential instanceof CustomCredential
                && credential.getType().equals(TYPE_GOOGLE_ID_TOKEN_CREDENTIAL)) {
            // Create Google ID Token
            Bundle credentialData = ((CustomCredential) credential).getData();
            GoogleIdTokenCredential googleIdTokenCredential = GoogleIdTokenCredential.createFrom(credentialData);

            // Sign in to Firebase with using the token
            firebaseAuthWithGoogle(googleIdTokenCredential.getIdToken());
        } else {
            runOnUiThread(() -> {
                progressBar.setVisibility(View.GONE);
            });
            Log.w("sign in", "Credential is not of type Google ID!");
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Sign in success, update UI with the signed-in user's information
                        Log.d("sign in", "signInWithCredential:success");
                        FirebaseUser user = mAuth.getCurrentUser();
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                        });
                        startActivity(new Intent(this, HomeActivity.class));
                    } else {
                        // If sign in fails, display a message to the user
                        runOnUiThread(() -> {
                            progressBar.setVisibility(View.GONE);
                        });
                        Log.w("sign in", "signInWithCredential:failure", task.getException());
                    }
                });
    }

    @Override
    public void onStart() {
        super.onStart();
        // Check if user is signed in (non-null) and update UI accordingly.
        FirebaseUser currentUser = mAuth.getCurrentUser();
        try {
            if (currentUser.getEmail() != null)
                startActivity(new Intent(this, HomeActivity.class));
        } catch (Exception e) {
            mAuth.signOut();
            e.printStackTrace();
        }
    }
}