package com.example.roommanager;

import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

public class ReportViewHolder extends RecyclerView.ViewHolder {
    public TextView emailTextView;
    public TextView messageTextView;
    public ImageView imageView;

    public ReportViewHolder(@NonNull View itemView) {
        super(itemView);
        emailTextView = itemView.findViewById(R.id.reportEmail);
        messageTextView = itemView.findViewById(R.id.reportMessage);
        imageView = itemView.findViewById(R.id.reportImage);
    }
}
