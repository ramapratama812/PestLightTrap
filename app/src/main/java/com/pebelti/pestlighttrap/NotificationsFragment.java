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
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class NotificationsFragment extends Fragment {

    private RecyclerView rvNotifications;
    private TextView tvEmptyState;
    private NotificationAdapter adapter;
    private List<NotificationItem> notificationList;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notifications, container, false);

        rvNotifications = view.findViewById(R.id.rvNotifications);
        tvEmptyState = view.findViewById(R.id.tvEmptyState);

        rvNotifications.setLayoutManager(new LinearLayoutManager(getContext()));
        notificationList = new ArrayList<>();
        adapter = new NotificationAdapter(notificationList);
        rvNotifications.setAdapter(adapter);

        // Fetch data from Firebase to generate dynamic notifications
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        rootRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                notificationList.clear();

                // 1. Lampu aktif dan tidak aktif
                Boolean isLampOn = snapshot.child("smart_pest_trap/status").getValue(Boolean.class);
                if (isLampOn != null) {
                    if (isLampOn) {
                        notificationList.add(new NotificationItem("Lampu Aktif",
                                "Lampu perangkap UV sedang menyala dan beroperasi normal.", "Saat ini", "SUCCESS",
                                R.drawable.ic_lightbulb_outline));
                    } else {
                        notificationList
                                .add(new NotificationItem("Lampu Tidak Aktif", "Lampu perangkap UV sedang dimatikan.",
                                        "Saat ini", "WARNING", R.drawable.ic_lightbulb_outline));
                    }
                }

                // 2. Trap diaktifkan dan dimatikan (Auto Mode)
                Boolean isAuto = snapshot.child("smart_pest_trap/auto_mode").getValue(Boolean.class);
                if (isAuto != null) {
                    if (isAuto) {
                        notificationList.add(new NotificationItem("Trap Diaktifkan",
                                "Mode otomatis aktif. Trap beroperasi sesuai jadwal.", "Saat ini", "INFO",
                                R.drawable.ic_lamp_trap));
                    } else {
                        notificationList.add(new NotificationItem("Trap Dimatikan",
                                "Mode otomatis dinonaktifkan. Trap dalam kendali manual.", "Saat ini", "WARNING",
                                R.drawable.ic_lamp_trap));
                    }
                }

                // 3. Baterai lemah dan terisi penuh
                Integer battery = snapshot.child("smart_pest_trap/battery/percent").getValue(Integer.class);
                if (battery != null) {
                    if (battery <= 20) {
                        notificationList.add(new NotificationItem("Baterai Lemah",
                                "Baterai turun ke " + battery + "%. Harap isi daya panel surya.", "Saat ini",
                                "CRITICAL", R.drawable.ic_battery_outline));
                    } else if (battery >= 80) {
                        notificationList.add(new NotificationItem("Baterai Terisi Penuh",
                                "Kapasitas baterai " + battery + "%. Daya optimal.", "Saat ini", "SUCCESS",
                                R.drawable.ic_battery_outline));
                    }
                }

                // 4. Trap penuh dan kosong
                Integer trap = snapshot.child("smart_pest_trap/trap_fullness").getValue(Integer.class);
                if (trap != null) {
                    if (trap >= 80) {
                        notificationList.add(new NotificationItem("Trap Penuh",
                                "Kapasitas trap hama mencapai " + trap + "%. Perlu dikosongkan.", "Saat ini",
                                "CRITICAL", R.drawable.ic_bug_report));
                    } else if (trap <= 20) {
                        notificationList.add(new NotificationItem("Trap Kosong",
                                "Kapasitas trap hama " + trap + "%. Trap bersih dan siap digunakan.", "Saat ini",
                                "SUCCESS", R.drawable.ic_bug_report));
                    } else {
                        notificationList.add(new NotificationItem("Status Trap Normal",
                                "Kapasitas trap hama " + trap + "%. Berjalan normal.", "Saat ini", "INFO",
                                R.drawable.ic_bug_report));
                    }
                }

                // 5. Efisiensi panel surya
                Integer power = snapshot.child("smart_pest_trap/power_consumption").getValue(Integer.class);
                if (power != null) {
                    if (power >= 80) {
                        notificationList.add(new NotificationItem("Efisiensi Panel Surya Baik",
                                "Input efisiensi surya " + power + "%. Beroperasi optimal.", "Saat ini", "SUCCESS",
                                R.drawable.ic_solar_panel));
                    } else if (power <= 40) {
                        notificationList.add(new NotificationItem("Efisiensi Panel Surya Rendah",
                                "Input surya " + power + "%. Efisiensi berkurang, periksa panel.", "Saat ini",
                                "WARNING", R.drawable.ic_solar_panel));
                    }
                }

                if (notificationList.isEmpty()) {
                    tvEmptyState.setVisibility(View.VISIBLE);
                    rvNotifications.setVisibility(View.GONE);
                } else {
                    tvEmptyState.setVisibility(View.GONE);
                    rvNotifications.setVisibility(View.VISIBLE);
                    adapter.notifyDataSetChanged();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

        return view;
    }
}
