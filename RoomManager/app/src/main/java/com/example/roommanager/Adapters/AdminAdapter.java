package com.example.roommanager.Adapters;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.example.roommanager.R;

import java.util.List;

public class AdminAdapter extends RecyclerView.Adapter<AdminAdapter.AdminViewHolder> {

    private List<String> admins;
    private OnRemoveClickListener listener;

    public interface OnRemoveClickListener {
        void onRemove(String email);
    }

    public AdminAdapter(List<String> admins, OnRemoveClickListener listener) {
        this.admins = admins;
        this.listener = listener;
    }

    @NonNull
    @Override
    public AdminViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_admin, parent, false);
        return new AdminViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull AdminViewHolder holder, int position) {
        String email = admins.get(position);
        holder.emailText.setText(email);
        holder.removeBtn.setOnClickListener(v -> listener.onRemove(email));
    }

    @Override
    public int getItemCount() {
        return admins.size();
    }

    static class AdminViewHolder extends RecyclerView.ViewHolder {
        TextView emailText;
        ImageButton removeBtn;

        AdminViewHolder(View itemView) {
            super(itemView);
            emailText = itemView.findViewById(R.id.textEmail);
            removeBtn = itemView.findViewById(R.id.btnRemove);
        }
    }
}
