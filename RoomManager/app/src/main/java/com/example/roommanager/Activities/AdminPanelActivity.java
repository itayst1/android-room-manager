package com.example.roommanager.Activities;

import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.InputType;
import android.view.View;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roommanager.Adapters.AdminAdapter;
import com.example.roommanager.R;
import com.google.firebase.database.*;

import java.util.ArrayList;
import java.util.List;

public class AdminPanelActivity extends AppCompatActivity {

    private RecyclerView adminRecyclerView;
    private AdminAdapter adminAdapter;
    private List<String> adminList = new ArrayList<>();
    private DatabaseReference adminsRef;
    private Context style;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_admin_panel);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        // UI Elements
        Button btnAddAdmin = findViewById(R.id.btnAddAdmin);
        Button btnViewReports = findViewById(R.id.btnViewReports);
        Button btnBack = findViewById(R.id.btnBack);
        adminRecyclerView = findViewById(R.id.adminRecyclerView);

        // Firebase Reference
        adminsRef = FirebaseDatabase.getInstance().getReference("settings/admins");

        // RecyclerView setup
        adminAdapter = new AdminAdapter(adminList, this::removeAdmin);
        adminRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        adminRecyclerView.setAdapter(adminAdapter);

        style = new ContextThemeWrapper(this, R.style.CustomDialogTheme);


        // Load data
        loadAdmins();

        // Button actions
        btnAddAdmin.setOnClickListener(v -> showAddAdminDialog());

        btnViewReports.setOnClickListener(v -> {startActivity(new Intent(AdminPanelActivity.this, ViewReportsActivity.class));});

        btnBack.setOnClickListener(v -> startActivity(new Intent(AdminPanelActivity.this, HomeActivity.class)));
    }

    private void loadAdmins() {
        adminsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                adminList.clear();
                for (DataSnapshot child : snapshot.getChildren()) {
                    String emailKey = child.getKey();
                    if (emailKey != null) {
                        String email = decodeEmailKey(emailKey);
                        adminList.add(email);
                    }
                }
                adminAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Toast.makeText(AdminPanelActivity.this, "Failed to load admins", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void showAddAdminDialog() {
        EditText input = new EditText(this);
        input.setInputType(InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);

        AlertDialog.Builder builder = new AlertDialog.Builder(style);
        builder.setTitle("Add Admin");
        builder.setMessage("Enter admin email:");
        builder.setView(input);
        builder.setPositiveButton("Add", (dialog, which) -> {
            String email = input.getText().toString().trim();
            if (!email.isEmpty()) {
                String key = encodeEmailKey(email);
                adminsRef.child(key).setValue(true);
            }
        });
        builder.setNegativeButton("Cancel", null);

        AlertDialog dialog = builder.create();
        dialog.getWindow().setBackgroundDrawableResource(R.drawable.rounded_background);
        dialog.show();

        // Set EditText text color programmatically
        input.setTextColor(Color.BLACK);  // Set the text color to black
        input.setHintTextColor(Color.GRAY);  // Set the hint text color to gray

        // Manually change the button text color
        Button positiveButton = dialog.getButton(AlertDialog.BUTTON_POSITIVE);
        Button negativeButton = dialog.getButton(AlertDialog.BUTTON_NEGATIVE);
        Button neutralButton = dialog.getButton(AlertDialog.BUTTON_NEUTRAL);

        // Set button text color to black
        if (positiveButton != null) {
            positiveButton.setTextColor(0xFF000000);
        }
        if (negativeButton != null) {
            negativeButton.setTextColor(0xFF000000);
        }
        if (neutralButton != null) {
            neutralButton.setTextColor(0xFF000000);
        }
    }



    private void removeAdmin(String email) {
        new AlertDialog.Builder(this)
                .setTitle("Remove Admin")
                .setMessage("Are you sure you want to remove admin:\n" + email + "?")
                .setPositiveButton("Yes", (dialog, which) -> {
                    String key = encodeEmailKey(email);
                    adminsRef.child(key).removeValue()
                            .addOnSuccessListener(aVoid ->
                                    Toast.makeText(this, "Admin removed", Toast.LENGTH_SHORT).show())
                            .addOnFailureListener(e ->
                                    Toast.makeText(this, "Failed to remove admin", Toast.LENGTH_SHORT).show());
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private String encodeEmailKey(String email) {
        return email.replace(".", "_");
    }

    private String decodeEmailKey(String key) {
        return key.replace("_", ".");
    }
}
