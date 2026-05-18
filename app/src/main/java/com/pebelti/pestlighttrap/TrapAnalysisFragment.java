package com.pebelti.pestlighttrap;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

// Import Firebase Database
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class TrapAnalysisFragment extends Fragment {

    // Views untuk Trap Section
    private TextView tvDurasiSiklus;
    private TextView tvMulai;
    private TextView tvSelesai;
    private TextView tvDurasi;

    // Views untuk Battery Section
    private ProgressBar pbBattery;
    private TextView tvBatteryPercent;
    private TextView tvVoltage;
    private TextView tvTemp;
    private TextView tvHealth;

    // Firebase References
    private DatabaseReference trapRef;
    private DatabaseReference batteryRef;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_trap_analysis, container, false);

        // 1. Bind Views (Trap)
        tvDurasiSiklus = view.findViewById(R.id.tv_durasi_siklus);
        tvMulai = view.findViewById(R.id.tv_mulai);
        tvSelesai = view.findViewById(R.id.tv_selesai);
        tvDurasi = view.findViewById(R.id.tv_durasi);

        // 2. Bind Views (Battery)
        pbBattery = view.findViewById(R.id.pb_battery);
        tvBatteryPercent = view.findViewById(R.id.tv_battery_percent);
        tvVoltage = view.findViewById(R.id.tv_voltage);
        tvTemp = view.findViewById(R.id.tv_temp);
        tvHealth = view.findViewById(R.id.tv_health);

        // 3. Inisialisasi Firebase Database
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        trapRef = database.getReference("trap_analysis");
        batteryRef = database.getReference("battery");

        // 4. Load Data dari Firebase
        loadTrapData();
        loadBatteryData();

        // Tombol Kembali
        View btnBack = view.findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeDashboardFragment())
                    .commit();
        });

        // Tab Switching Logic (Sudah ada di kode awal Anda)
        setupTabSwitching(view);

        return view;
    }

    private void loadTrapData() {
        trapRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String durasiSiklus = snapshot.child("durasi_siklus").getValue(String.class);
                    String mulai = snapshot.child("mulai").getValue(String.class);
                    String selesai = snapshot.child("selesai").getValue(String.class);
                    String durasi = snapshot.child("durasi").getValue(String.class);

                    if (durasiSiklus != null && tvDurasiSiklus != null) tvDurasiSiklus.setText(durasiSiklus);
                    if (mulai != null && tvMulai != null) tvMulai.setText(mulai);
                    if (selesai != null && tvSelesai != null) tvSelesai.setText(selesai);
                    if (durasi != null && tvDurasi != null) tvDurasi.setText(durasi);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Gagal memuat data Trap: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void loadBatteryData() {
        batteryRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Integer percent = snapshot.child("percent").getValue(Integer.class);
                    String voltage = snapshot.child("voltage").getValue(String.class);
                    String temp = snapshot.child("temp").getValue(String.class);
                    String health = snapshot.child("health").getValue(String.class);

                    if (percent != null) {
                        if (pbBattery != null) pbBattery.setProgress(percent);
                        if (tvBatteryPercent != null) tvBatteryPercent.setText(percent + "%");
                    }
                    if (voltage != null && tvVoltage != null) tvVoltage.setText(voltage);
                    if (temp != null && tvTemp != null) tvTemp.setText(temp);
                    if (health != null && tvHealth != null) tvHealth.setText(health);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Gagal memuat data Baterai: " + error.getMessage(), Toast.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void setupTabSwitching(View view) {
        View tabTrap = view.findViewById(R.id.tabTrap);
        View tabBattery = view.findViewById(R.id.tabBattery);
        View layoutTrapContent = view.findViewById(R.id.layoutTrapContent);
        View layoutBatteryContent = view.findViewById(R.id.layoutBatteryContent);
        
        ImageView ivTabTrap = view.findViewById(R.id.ivTabTrap);
        TextView tvTabTrap = view.findViewById(R.id.tvTabTrap);
        ImageView ivTabBattery = view.findViewById(R.id.ivTabBattery);
        TextView tvTabBattery = view.findViewById(R.id.tvTabBattery);

        tabTrap.setOnClickListener(v -> {
            layoutTrapContent.setVisibility(View.VISIBLE);
            layoutBatteryContent.setVisibility(View.GONE);
            
            tabTrap.setBackgroundResource(R.drawable.bg_tab_active);
            tabBattery.setBackground(null);
            
            ivTabTrap.setColorFilter(Color.WHITE);
            tvTabTrap.setTextColor(Color.WHITE);
            ivTabBattery.setColorFilter(Color.parseColor("#7986CB"));
            tvTabBattery.setTextColor(Color.parseColor("#7986CB"));
        });

        tabBattery.setOnClickListener(v -> {
            layoutTrapContent.setVisibility(View.GONE);
            layoutBatteryContent.setVisibility(View.VISIBLE);
            
            tabBattery.setBackgroundResource(R.drawable.bg_tab_active);
            tabTrap.setBackground(null);
            
            ivTabBattery.setColorFilter(Color.WHITE);
            tvTabBattery.setTextColor(Color.WHITE);
            ivTabTrap.setColorFilter(Color.parseColor("#7986CB"));
            tvTabTrap.setTextColor(Color.parseColor("#7986CB"));
        });
    }
}
