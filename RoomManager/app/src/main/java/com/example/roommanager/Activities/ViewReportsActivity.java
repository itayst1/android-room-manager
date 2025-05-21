package com.example.roommanager.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.view.ContextThemeWrapper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roommanager.Adapters.ReportsAdapter;
import com.example.roommanager.R;
import com.example.roommanager.Objects.Report;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class ViewReportsActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private ReportsAdapter reportsAdapter;
    private List<Report> reportList;
    private Button backButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_view_reports);
        getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

        recyclerView = findViewById(R.id.reportsRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        reportList = new ArrayList<>();
        reportsAdapter = new ReportsAdapter(reportList, this::removeReport);
        recyclerView.setAdapter(reportsAdapter);

        backButton = findViewById(R.id.btnBack);
        backButton.setOnClickListener(v -> startActivity(new Intent(ViewReportsActivity.this, AdminPanelActivity.class)));

        // Fetch reports from Firebase
        DatabaseReference reportsRef = FirebaseDatabase.getInstance().getReference("reports");
        reportsRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot dataSnapshot) {
                reportList.clear();
                for (DataSnapshot reportSnapshot : dataSnapshot.getChildren()) {
                    String reportId = reportSnapshot.getKey();
                    String userEmail = reportSnapshot.child("userEmail").getValue(String.class);
                    String message = reportSnapshot.child("message").getValue(String.class);
                    String imageUrl = reportSnapshot.child("imageUrl").getValue(String.class);

                    // Create Report object and add it to the list
                    reportList.add(new Report(reportId, userEmail, message, imageUrl));
                }

                // Notify adapter of the new data
                reportsAdapter.notifyDataSetChanged();
            }

            @Override
            public void onCancelled(DatabaseError databaseError) {
                Log.e("ViewReportsActivity", "Error loading reports: " + databaseError.getMessage());
            }
        });
    }

    private void removeReport(String reportId) {
        AlertDialog.Builder builder = new AlertDialog.Builder(new ContextThemeWrapper(this, R.style.CustomDialogTheme));
        builder.setTitle("Mark as resolved");
        builder.setMessage("Are you sure you want to mark this report as resolved?");
        builder.setPositiveButton("Yes", (dialog, which) -> {
            FirebaseDatabase.getInstance().getReference("reports").child(reportId).removeValue()
                    .addOnSuccessListener(aVoid ->
                            Toast.makeText(this, "Report marked as resolved", Toast.LENGTH_SHORT).show())
                    .addOnFailureListener(e ->
                            Toast.makeText(this, "Failed to mark report as resolved", Toast.LENGTH_SHORT).show());
        });
        builder.setNegativeButton("Cancel", null);

        AlertDialog dialog = builder.create();
        Objects.requireNonNull(dialog.getWindow()).setBackgroundDrawableResource(R.drawable.rounded_background);

        dialog.show();

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
}
