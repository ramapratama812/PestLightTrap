package com.pebelti.pestlighttrap;

import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import java.util.List;

public class NotificationAdapter extends RecyclerView.Adapter<NotificationAdapter.ViewHolder> {

    private List<NotificationItem> notificationList;

    public NotificationAdapter(List<NotificationItem> notificationList) {
        this.notificationList = notificationList;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_notification, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        NotificationItem item = notificationList.get(position);
        holder.tvTitle.setText(item.getTitle());
        holder.tvDescription.setText(item.getDescription());
        holder.tvTime.setText(item.getTime());
        holder.tvBadge.setText(item.getType());
        holder.ivIcon.setImageResource(item.getIconResId());

        int color;
        switch (item.getType()) {
            case "CRITICAL":
                color = Color.parseColor("#F44336"); // Merah
                break;
            case "WARNING":
                color = Color.parseColor("#FF9800"); // Kuning
                break;
            case "SUCCESS":
                color = Color.parseColor("#4CAF50"); // Hijau
                break;
            case "INFO":
            default:
                color = Color.parseColor("#5C6BC0"); // Biru
                break;
        }

        holder.colorIndicator.setBackgroundColor(color);
        holder.iconBg.getBackground().mutate().setTint(color);
        holder.tvBadge.getBackground().mutate().setTint(color);
    }

    @Override
    public int getItemCount() {
        return notificationList.size();
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        View colorIndicator;
        View iconBg;
        ImageView ivIcon;
        TextView tvTitle;
        TextView tvDescription;
        TextView tvTime;
        TextView tvBadge;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            colorIndicator = itemView.findViewById(R.id.colorIndicator);
            iconBg = itemView.findViewById(R.id.iconBg);
            ivIcon = itemView.findViewById(R.id.ivNotificationIcon);
            tvTitle = itemView.findViewById(R.id.tvNotificationTitle);
            tvDescription = itemView.findViewById(R.id.tvNotificationDesc);
            tvTime = itemView.findViewById(R.id.tvNotificationTime);
            tvBadge = itemView.findViewById(R.id.tvNotificationBadge);
        }
    }
}
