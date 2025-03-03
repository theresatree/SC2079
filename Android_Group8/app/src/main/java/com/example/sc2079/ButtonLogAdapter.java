package com.example.sc2079;

import android.graphics.Color;
import android.graphics.Typeface;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import java.util.ArrayList;

public class ButtonLogAdapter extends RecyclerView.Adapter<ButtonLogAdapter.ViewHolder> {
    private ArrayList<String> logList;
    private RecyclerView recyclerView;

    public ButtonLogAdapter(ArrayList<String> logList, RecyclerView recyclerView) {
        this.logList = logList;
        this.recyclerView = recyclerView;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        // Inflate the custom layout (log.xml)
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.log, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        holder.textView.setText(logList.get(position));
        holder.textView.setTextSize(TypedValue.COMPLEX_UNIT_SP, 14); // Set text size to 11sp
        holder.textView.setTextColor(Color.WHITE); // Correct way to set text color
        holder.textView.setTypeface(null, Typeface.NORMAL); // Set text to normal (non-bold)
    }

    @Override
    public int getItemCount() {
        return logList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView textView;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            // Reference the TextView in your custom layout (log.xml)
            textView = itemView.findViewById(R.id.logTextView);
            textView.setPadding(15, 0, 0, 0);  // Optional: Reset padding if needed
        }
    }

    public void addLog(String log) {
        if (logList.size() >= 30) {
            logList.remove(0);
            notifyItemRemoved(0);
        }
        logList.add(log);
        notifyItemInserted(logList.size() - 1);

        // Ensure recyclerView is not null before scrolling
        if (recyclerView != null) {
            recyclerView.post(() -> recyclerView.smoothScrollToPosition(logList.size() - 1));
        }
    }
}