package com.example.roommanager.Adapters;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roommanager.R;

import java.util.List;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roommanager.R;

import java.util.List;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roommanager.R;

import java.util.List;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roommanager.R;

import java.util.List;

public class TimeSlotAdapter extends RecyclerView.Adapter<TimeSlotAdapter.TimeSlotViewHolder> {

    private List<String> timeSlots;
    private OnTimeSlotClickListener listener;
    private final List<String> reservedSlots; // List to track if a slot is reserved
    private int selectedPosition = -1; // Track the selected item position

    public interface OnTimeSlotClickListener {
        void onTimeSlotClick(String timeSlot);
    }

    public TimeSlotAdapter(List<String> timeSlots, OnTimeSlotClickListener listener, List<String> reservedSlots) {
        this.timeSlots = timeSlots;
        this.listener = listener;
        this.reservedSlots = reservedSlots;
    }

    @NonNull
    @Override
    public TimeSlotViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_time_slot, parent, false);
        return new TimeSlotViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TimeSlotViewHolder holder, int position) {
        // Use holder.getAdapterPosition() instead of position
        final int adapterPosition = holder.getAdapterPosition();

        if (reservedSlots.isEmpty() || timeSlots.isEmpty() || position == RecyclerView.NO_POSITION) {
            return;
        }

        String timeSlot = timeSlots.get(position);
        holder.timeSlotTextView.setText(timeSlot);
        holder.timeSlotTextView.setTextColor(Color.WHITE);

        // Check if the slot is reserved, available, or past
        if (reservedSlots.get(position).equals("Reserved")) {
            holder.timeSlotStatus.setText("Reserved");
            holder.timeSlotStatus.setBackgroundResource(R.drawable.rounded_red);
        } else if (reservedSlots.get(position).equals("Available")) {
            holder.timeSlotStatus.setText("Available");
            holder.timeSlotStatus.setBackgroundResource(R.drawable.rounded_green);
        } else {
            holder.timeSlotStatus.setText("Time passed");
            holder.timeSlotStatus.setBackgroundResource(R.drawable.rounded_red);
        }

        if (holder.timeSlotStatus.getText().equals("Available")) {
            // Change background color of the selected item
            if (selectedPosition == adapterPosition) {
                holder.timeSlot.setBackgroundResource(R.drawable.rounded_background_color); // Highlight selected item
            } else {
                holder.timeSlot.setBackgroundColor(Color.TRANSPARENT); // Default background
            }
        } else {
            holder.timeSlot.setBackgroundColor(Color.TRANSPARENT); // Default background
            selectedPosition = -1;
        }

        // Set a click listener on the time slot
        holder.timeSlot.setOnClickListener(v -> {
            selectedPosition = adapterPosition; // Update the selected item
            notifyDataSetChanged(); // Notify the adapter that an item has been selected

            if (listener != null) {
                listener.onTimeSlotClick(timeSlot);
            }

            // Optional: Update the selected time somewhere else (outside of RecyclerView)
            updateSelectedTime(timeSlot);
        });
    }

    @Override
    public int getItemCount() {
        return timeSlots.size();
    }

    public static class TimeSlotViewHolder extends RecyclerView.ViewHolder {
        TextView timeSlotTextView, timeSlotStatus;
        LinearLayout timeSlot;

        public TimeSlotViewHolder(@NonNull View itemView) {
            super(itemView);
            timeSlotTextView = itemView.findViewById(R.id.time_slot_text_view);
            timeSlotStatus = itemView.findViewById(R.id.time_slot_status);
            timeSlot = itemView.findViewById(R.id.time_slot);
        }
    }

    // Method to update the selected time outside of RecyclerView
    private void updateSelectedTime(String timeSlot) {
        // Example: You can call a method in your activity or fragment to update the UI with the selected start time
        String selectedStartTime = timeSlot.split("-")[0];
        // Assuming you have a TextView in your activity/fragment to display the selected start time
        // selectedTimeTextView.setText("Selected Start Time: " + selectedStartTime);
    }
}
