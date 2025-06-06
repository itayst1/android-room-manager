package com.example.roommanager.Adapters;

import android.graphics.Color;
import android.util.Log;
<<<<<<< HEAD
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
=======
>>>>>>> 3a9fccc (bug fix)
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

        Log.d("debug", selectedPosition + "");
        Log.d("debug", holder.timeSlotStatus.getText() + "");
        Log.d("debug", holder.getAdapterPosition() + "");
        if (holder.timeSlotStatus.getText().toString().equals("Available")) {
            // Change background color of the selected item
            if (selectedPosition == holder.getAdapterPosition()) {
                holder.timeSlot.setBackgroundResource(R.drawable.rounded_background_color); // Highlight selected item
            } else {
                holder.timeSlot.setBackgroundColor(Color.TRANSPARENT); // Default background
            }
        } else if (adapterPosition == timeSlots.size()) {
            holder.timeSlot.setBackgroundColor(Color.TRANSPARENT); // Default background
        }

        // Set a click listener on the time slot
        holder.timeSlot.setOnClickListener(v -> {
            selectedPosition = holder.getAdapterPosition();
            notifyDataSetChanged();

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
