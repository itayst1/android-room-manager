package com.example.roommanager.Activities;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roommanager.Adapters.ReportsAdapter;
import com.example.roommanager.R;
import com.example.roommanager.Report;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.util.ArrayList;
import java.util.List;

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
        reportsAdapter = new ReportsAdapter(reportList);
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
                    String userEmail = reportSnapshot.child("userEmail").getValue(String.class);
                    String message = reportSnapshot.child("message").getValue(String.class);
                    String imageUrl = reportSnapshot.child("imageUrl").getValue(String.class);

                    // Create Report object and add it to the list
                    reportList.add(new Report(userEmail, message, imageUrl));
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
}
