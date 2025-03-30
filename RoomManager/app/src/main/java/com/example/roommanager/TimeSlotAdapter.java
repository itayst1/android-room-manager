package com.example.roommanager;

import android.content.Context;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class TimeSlotAdapter extends RecyclerView.Adapter<TimeSlotAdapter.ViewHolder> {

    private final List<String> timeSlots; // List of time slots
    private final List<Boolean> reservedSlots; // List to track if a slot is reserved
    private final Context context;

    public TimeSlotAdapter(Context context, List<String> timeSlots, List<Boolean> reservedSlots) {
        this.context = context;
        this.timeSlots = timeSlots;
        this.reservedSlots = reservedSlots;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_time_slot, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.tvTimeSlot.setText(timeSlots.get(position));
        holder.tvTimeSlot.setTextColor(Color.WHITE);
        holder.tvStatus.setBackgroundResource(R.drawable.rounded_background);
        // Check if slot is reserved
        if (reservedSlots.get(position)) {
            holder.tvStatus.setText("Reserved");
            holder.tvStatus.setBackgroundColor(Color.RED);
        } else {
            holder.tvStatus.setText("Available");
            holder.tvStatus.setBackgroundColor(Color.GREEN);
        }
    }

    @Override
    public int getItemCount() {
        return timeSlots.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvTimeSlot, tvStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTimeSlot = itemView.findViewById(R.id.tv_time_slot);
            tvStatus = itemView.findViewById(R.id.tv_status);
        }
    }
}
