package com.example.roommanager;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.List;

public class TimeSlotAdapter extends RecyclerView.Adapter<TimeSlotAdapter.TimeSlotViewHolder> {

    private List<String> timeSlots;
    private OnTimeSlotClickListener listener;
    private final List<String> reservedSlots; // List to track if a slot is reserved
    private List<Integer> room;

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
        if (reservedSlots.isEmpty() || timeSlots.isEmpty()) {
            return;
        }
        String timeSlot = timeSlots.get(position);
        holder.timeSlotTextView.setText(timeSlots.get(position));
        holder.timeSlotTextView.setTextColor(Color.WHITE);
        // Check if slot is reserved
        if (reservedSlots.get(position).equals("Reserved")) {
            holder.timeSlotStatus.setText("Reserved");
            holder.timeSlotStatus.setBackgroundResource(R.drawable.rounded_red);
        } else if (reservedSlots.get(position).equals("Available")) {
            holder.timeSlotStatus.setText("Available");
            holder.timeSlotStatus.setBackgroundResource(R.drawable.rounded_green);
        } else {
            holder.timeSlotStatus.setText("Time past");
            holder.timeSlotStatus.setBackgroundResource(R.drawable.rounded_red);
        }
        // Set a click listener on the time slot
        holder.timeSlot.setOnClickListener(v -> {
            if (listener != null) {
                listener.onTimeSlotClick(timeSlot);
            }
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
}
