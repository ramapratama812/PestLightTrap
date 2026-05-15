package com.pebelti.pestlighttrap;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import java.util.ArrayList;
import java.util.List;

public class NotificationsFragment extends Fragment {

    private RecyclerView rvNotifications;
    private TextView tvEmptyState;
    private NotificationAdapter adapter;
    private List<NotificationItem> notificationList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notifications, container, false);

        rvNotifications = view.findViewById(R.id.rvNotifications);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);

        rvNotifications.setLayoutManager(new LinearLayoutManager(getContext()));

        // Data dummy notifikasi sesuai gambar
        notificationList = new ArrayList<>();
        notificationList.add(new NotificationItem("Low Battery", "Battery dropped to 15%. Please charge solar panel.", "5 mins ago", "CRITICAL", R.drawable.ic_battery_outline));
        notificationList.add(new NotificationItem("Trap Full", "Insect trap capacity reached 100%. Empty required.", "32 mins ago", "WARNING", R.drawable.ic_bug_report));
        notificationList.add(new NotificationItem("Bulb Failure", "UV light bulb not responding. Check connection.", "1 hr ago", "CRITICAL", R.drawable.ic_lightbulb_outline));
        notificationList.add(new NotificationItem("System Normal", "All sensors operating within normal parameters.", "3 hrs ago", "INFO", R.drawable.ic_check_circle));
        notificationList.add(new NotificationItem("Low Solar Input", "Solar panel efficiency at 45% due to cloudy weather.", "Yesterday", "WARNING", R.drawable.ic_solar_panel));
        notificationList.add(new NotificationItem("Trap Activated", "Night mode started at 18:00. Trap is now active.", "Yesterday", "INFO", R.drawable.ic_lamp_trap));

        if (notificationList.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            rvNotifications.setVisibility(View.GONE);
        } else {
            tvEmptyState.setVisibility(View.GONE);
            rvNotifications.setVisibility(View.VISIBLE);
            adapter = new NotificationAdapter(notificationList);
            rvNotifications.setAdapter(adapter);
        }

        return view;
    }
}
