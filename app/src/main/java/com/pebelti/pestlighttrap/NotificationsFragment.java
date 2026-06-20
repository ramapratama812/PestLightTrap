package com.pebelti.pestlighttrap;

import android.content.Context;
import android.content.SharedPreferences;
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

    /** Key fingerprint di SharedPreferences (beda dari KEY_HAS_UNREAD) */
    private static final String KEY_READ_FINGERPRINT = "read_fingerprint";

    private RecyclerView rvNotifications;
    private TextView tvEmptyState;
    private TextView tvMarkAsRead;
    private NotificationAdapter adapter;
    private List<NotificationItem> notificationList;

    /**
     * Fingerprint dari data Firebase pada saat terakhir Firebase mengirim data.
     * Digunakan saat tombol "Tandai Dibaca" diklik — disimpan ke SharedPreferences
     * sebagai penanda "data ini sudah dibaca".
     */
    private String lastFirebaseFingerprint = "";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_notifications, container, false);

        rvNotifications = view.findViewById(R.id.rvNotifications);
        tvEmptyState    = view.findViewById(R.id.tvEmptyState);
        tvMarkAsRead    = view.findViewById(R.id.tvMarkAsRead);

        rvNotifications.setLayoutManager(new LinearLayoutManager(getContext()));
        notificationList = new ArrayList<>();
        adapter = new NotificationAdapter(notificationList);
        rvNotifications.setAdapter(adapter);

        // Tombol "Tandai Dibaca":
        //  1. Simpan fingerprint data saat ini ke SharedPreferences
        //  2. Bersihkan list
        //  3. Sembunyikan dot merah bell
        tvMarkAsRead.setOnClickListener(v -> {
            SharedPreferences prefs = requireContext()
                    .getSharedPreferences(HomeDashboardFragment.PREFS_NAME, Context.MODE_PRIVATE);
            // Simpan fingerprint terakhir sebagai "sudah dibaca"
            prefs.edit().putString(KEY_READ_FINGERPRINT, lastFirebaseFingerprint).apply();

            notificationList.clear();
            adapter.notifyDataSetChanged();
            HomeDashboardFragment.setHasUnreadNotification(requireContext(), false);
            updateEmptyState();
        });

        // Dengarkan data Firebase secara real-time
        DatabaseReference rootRef = FirebaseDatabase.getInstance().getReference();
        rootRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (getContext() == null) return;

                // --- Bangun fingerprint dari data Firebase saat ini ---
                String currentFingerprint = buildFingerprint(snapshot);
                lastFirebaseFingerprint = currentFingerprint;

                // --- Baca fingerprint yang sudah ditandai dibaca ---
                SharedPreferences prefs = requireContext()
                        .getSharedPreferences(HomeDashboardFragment.PREFS_NAME, Context.MODE_PRIVATE);
                String readFingerprint = prefs.getString(KEY_READ_FINGERPRINT, "");

                // Jika fingerprint sama → data tidak berubah sejak terakhir dibaca
                boolean alreadyRead = currentFingerprint.equals(readFingerprint);

                // --- Bangun daftar notifikasi ---
                List<NotificationItem> tempList = buildNotificationList(snapshot);
                boolean hasData = !tempList.isEmpty();

                // --- Update dot bell ---
                // Dot muncul hanya jika ada data DAN data berubah sejak terakhir dibaca
                HomeDashboardFragment.setHasUnreadNotification(getContext(), hasData && !alreadyRead);

                // --- Update tampilan list ---
                if (!alreadyRead) {
                    // Data baru / belum pernah dibaca → tampilkan
                    notificationList.clear();
                    notificationList.addAll(tempList);
                    adapter.notifyDataSetChanged();
                    updateEmptyState();
                } else {
                    // Data sama dengan terakhir dibaca → pertahankan list kosong
                    if (!notificationList.isEmpty()) {
                        notificationList.clear();
                        adapter.notifyDataSetChanged();
                    }
                    updateEmptyState();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

        return view;
    }

    /**
     * Bangun fingerprint string dari nilai-nilai kunci Firebase.
     * Jika salah satu nilai berubah, fingerprint akan berbeda → notifikasi muncul lagi.
     */
    private String buildFingerprint(DataSnapshot snapshot) {
        Boolean status  = snapshot.child("smart_pest_trap/status").getValue(Boolean.class);
        Boolean auto    = snapshot.child("smart_pest_trap/auto_mode").getValue(Boolean.class);
        Integer battery = snapshot.child("smart_pest_trap/battery/percent").getValue(Integer.class);
        Integer trap    = snapshot.child("smart_pest_trap/trap_fullness").getValue(Integer.class);
        Integer power   = snapshot.child("smart_pest_trap/power_consumption").getValue(Integer.class);
        return status + "|" + auto + "|" + battery + "|" + trap + "|" + power;
    }

    /**
     * Bangun daftar NotificationItem dari snapshot Firebase.
     */
    private List<NotificationItem> buildNotificationList(DataSnapshot snapshot) {
        List<NotificationItem> list = new ArrayList<>();

        // 1. Status lampu
        Boolean isLampOn = snapshot.child("smart_pest_trap/status").getValue(Boolean.class);
        if (isLampOn != null) {
            if (isLampOn) {
                list.add(new NotificationItem("Lampu Aktif",
                        "Lampu perangkap UV sedang menyala dan beroperasi normal.",
                        "Saat ini", "SUCCESS", R.drawable.ic_lightbulb_outline));
            } else {
                list.add(new NotificationItem("Lampu Tidak Aktif",
                        "Lampu perangkap UV sedang dimatikan.",
                        "Saat ini", "WARNING", R.drawable.ic_lightbulb_outline));
            }
        }

        // 2. Mode otomatis / manual
        Boolean isAuto = snapshot.child("smart_pest_trap/auto_mode").getValue(Boolean.class);
        if (isAuto != null) {
            if (isAuto) {
                list.add(new NotificationItem("Trap Diaktifkan",
                        "Mode otomatis aktif. Trap beroperasi sesuai jadwal.",
                        "Saat ini", "INFO", R.drawable.ic_lamp_trap));
            } else {
                list.add(new NotificationItem("Trap Dimatikan",
                        "Mode otomatis dinonaktifkan. Trap dalam kendali manual.",
                        "Saat ini", "WARNING", R.drawable.ic_lamp_trap));
            }
        }

        // 3. Level baterai
        Integer battery = snapshot.child("smart_pest_trap/battery/percent").getValue(Integer.class);
        if (battery != null) {
            if (battery <= 20) {
                list.add(new NotificationItem("Baterai Lemah",
                        "Baterai turun ke " + battery + "%. Harap isi daya panel surya.",
                        "Saat ini", "CRITICAL", R.drawable.ic_battery_outline));
            } else if (battery >= 80) {
                list.add(new NotificationItem("Baterai Terisi Penuh",
                        "Kapasitas baterai " + battery + "%. Daya optimal.",
                        "Saat ini", "SUCCESS", R.drawable.ic_battery_outline));
            }
        }

        // 4. Kapasitas trap
        Integer trap = snapshot.child("smart_pest_trap/trap_fullness").getValue(Integer.class);
        if (trap != null) {
            if (trap >= 80) {
                list.add(new NotificationItem("Trap Penuh",
                        "Kapasitas trap hama mencapai " + trap + "%. Perlu dikosongkan.",
                        "Saat ini", "CRITICAL", R.drawable.ic_bug_report));
            } else if (trap <= 20) {
                list.add(new NotificationItem("Trap Kosong",
                        "Kapasitas trap hama " + trap + "%. Trap bersih dan siap digunakan.",
                        "Saat ini", "SUCCESS", R.drawable.ic_bug_report));
            } else {
                list.add(new NotificationItem("Status Trap Normal",
                        "Kapasitas trap hama " + trap + "%. Berjalan normal.",
                        "Saat ini", "INFO", R.drawable.ic_bug_report));
            }
        }

        // 5. Efisiensi panel surya
        Integer power = snapshot.child("smart_pest_trap/power_consumption").getValue(Integer.class);
        if (power != null) {
            if (power >= 80) {
                list.add(new NotificationItem("Efisiensi Panel Surya Baik",
                        "Input efisiensi surya " + power + "%. Beroperasi optimal.",
                        "Saat ini", "SUCCESS", R.drawable.ic_solar_panel));
            } else if (power <= 40) {
                list.add(new NotificationItem("Efisiensi Panel Surya Rendah",
                        "Input surya " + power + "%. Efisiensi berkurang, periksa panel.",
                        "Saat ini", "WARNING", R.drawable.ic_solar_panel));
            }
        }

        // --- CONTOH NOTIFIKASI UNTUK TES JIKA FIREBASE KOSONG ---
        if (list.isEmpty()) {
            list.add(new NotificationItem("Contoh Notifikasi (Tes)",
                    "Ini adalah contoh notifikasi karena database Firebase Anda belum memiliki data sensor. Tekan 'Tandai Dibaca' untuk menyembunyikannya.",
                    "Saat ini", "INFO", R.drawable.ic_notifications));
        }

        return list;
    }

    /** Sinkronkan visibilitas RecyclerView, empty-state, dan tombol mark-as-read. */
    private void updateEmptyState() {
        if (notificationList.isEmpty()) {
            tvEmptyState.setVisibility(View.VISIBLE);
            rvNotifications.setVisibility(View.GONE);
            tvMarkAsRead.setVisibility(View.GONE);   // sembunyikan tombol kalau tidak ada notif
            
            tvEmptyState.setText("Tidak ada kejadian terbaru");
        } else {
            tvEmptyState.setVisibility(View.GONE);
            rvNotifications.setVisibility(View.VISIBLE);
            tvMarkAsRead.setVisibility(View.VISIBLE); // tampilkan tombol kalau ada notif
        }
    }
}
