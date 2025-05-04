package com.example.roommanager.Adapters;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.roommanager.R;
import com.example.roommanager.Report;
import com.example.roommanager.ReportViewHolder;

import java.util.List;

public class ReportsAdapter extends RecyclerView.Adapter<ReportViewHolder> {

    private List<Report> reportList;

    // Constructor
    public ReportsAdapter(List<Report> reportList) {
        this.reportList = reportList;
    }

    @NonNull
    @Override
    public ReportViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_report, parent, false);
        return new ReportViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ReportViewHolder holder, int position) {
        Report report = reportList.get(position);
        holder.emailTextView.setText(report.getUserEmail());

        // Bind message if exists
        if (report.getMessage() != null && !report.getMessage().isEmpty()) {
            holder.messageTextView.setText(report.getMessage());
            holder.messageTextView.setVisibility(View.VISIBLE);
        } else {
            holder.messageTextView.setVisibility(View.GONE);
        }

        // Decode Base64 to Bitmap and set it to the ImageView
        if (report.getImageUrl() != null && !report.getImageUrl().isEmpty()) {
            holder.imageView.setVisibility(View.VISIBLE);

            // Decode Base64 string to Bitmap
            Bitmap bitmap = decodeBase64(report.getImageUrl());

            if (bitmap != null) {
                Glide.with(holder.imageView.getContext())
                        .load(bitmap) // Load Bitmap into ImageView
                        .into(holder.imageView);
            } else {
                holder.imageView.setVisibility(View.GONE);
            }
        } else {
            holder.imageView.setVisibility(View.GONE);
        }

        holder.itemView.setOnLongClickListener(v -> {
            if (longClickListener != null) {
                longClickListener.onReportLongClick(reportList.get(position));
            }
            return true;
        });

    }

    @Override
    public int getItemCount() {
        return reportList.size();
    }

    // Utility method to decode Base64 string into Bitmap
    private Bitmap decodeBase64(String base64String) {
        try {
            byte[] decodedString = Base64.decode(base64String, Base64.DEFAULT);
            return BitmapFactory.decodeByteArray(decodedString, 0, decodedString.length);
        } catch (IllegalArgumentException e) {
            e.printStackTrace();
            return null;
        }
    }

    public interface OnReportLongClickListener {
        void onReportLongClick(Report report);
    }

    private OnReportLongClickListener longClickListener;

    public void setOnReportLongClickListener(OnReportLongClickListener listener) {
        this.longClickListener = listener;
    }

}
