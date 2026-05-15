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
        notificationList.add(new NotificationItem("Baterai Lemah", "Baterai turun ke 15%. Harap isi daya panel surya.", "5 menit lalu", "CRITICAL", R.drawable.ic_battery_outline));
        notificationList.add(new NotificationItem("Trap Penuh", "Kapasitas trap hama mencapai 100%. Perlu dikosongkan.", "32 menit lalu", "WARNING", R.drawable.ic_bug_report));
        notificationList.add(new NotificationItem("Lampu Rusak", "Lampu UV tidak merespons. Periksa koneksi.", "1 jam lalu", "CRITICAL", R.drawable.ic_lightbulb_outline));
        notificationList.add(new NotificationItem("Sistem Normal", "Semua sensor beroperasi dalam parameter normal.", "3 jam lalu", "INFO", R.drawable.ic_check_circle));
        notificationList.add(new NotificationItem("Input Surya Rendah", "Efisiensi panel surya 45% karena cuaca mendung.", "Kemarin", "WARNING", R.drawable.ic_solar_panel));
        notificationList.add(new NotificationItem("Trap Diaktifkan", "Mode malam dimulai pukul 18:00. Trap sekarang aktif.", "Kemarin", "INFO", R.drawable.ic_lamp_trap));

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
