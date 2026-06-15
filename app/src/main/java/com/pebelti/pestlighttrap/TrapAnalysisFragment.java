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
import android.widget.LinearLayout;
import android.app.DatePickerDialog;
import java.util.Calendar;
import java.util.Locale;
import java.text.SimpleDateFormat;
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
    private TextView tvHealth;

    // Day/Night State Views
    private View dotNight;
    private TextView tvNightStatus;
    private View borderNight;

    private View dotDay;
    private TextView tvDayStatus;
    private View borderDay;

    // Calendar Views & Variables
    private LinearLayout btnCalendar;
    private TextView tvCalendarDate;
    private Calendar selectedCalendar = Calendar.getInstance();

    // Firebase References
    private DatabaseReference trapRef;
    private DatabaseReference batteryRef;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_trap_monitoring, container, false);

        // 1. Bind Views (Trap)
        tvDurasiSiklus = view.findViewById(R.id.tv_durasi_siklus);
        tvMulai = view.findViewById(R.id.tv_mulai);
        tvSelesai = view.findViewById(R.id.tv_selesai);
        tvDurasi = view.findViewById(R.id.tv_durasi);

        // 2. Bind Views (Battery)
        pbBattery = view.findViewById(R.id.pb_battery);
        tvBatteryPercent = view.findViewById(R.id.tv_battery_percent);
        tvVoltage = view.findViewById(R.id.tv_voltage);
        tvHealth = view.findViewById(R.id.tv_health);

        // 3. Inisialisasi Firebase Database
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        trapRef = database.getReference("smart_pest_trap/trap_analysis");
        batteryRef = database.getReference("smart_pest_trap/battery");

        // 4. Load Data dari Firebase
        loadTrapData();
        loadBatteryData();

        // 5. Setup Kalender
        btnCalendar = view.findViewById(R.id.btnCalendar);
        tvCalendarDate = view.findViewById(R.id.tvCalendarDate);
        updateCalendarText();
        btnCalendar.setOnClickListener(v -> showDatePicker());

        // 6. Setup Status Siang/Malam
        dotNight = view.findViewById(R.id.dotNight);
        tvNightStatus = view.findViewById(R.id.tvNightStatus);
        borderNight = view.findViewById(R.id.borderNight);
        dotDay = view.findViewById(R.id.dotDay);
        tvDayStatus = view.findViewById(R.id.tvDayStatus);
        borderDay = view.findViewById(R.id.borderDay);
        setupClockBasedStatus();

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

                    if (durasiSiklus != null && tvDurasiSiklus != null)
                        tvDurasiSiklus.setText(durasiSiklus);
                    if (mulai != null && tvMulai != null)
                        tvMulai.setText(mulai);
                    if (selesai != null && tvSelesai != null)
                        tvSelesai.setText(selesai);
                    if (durasi != null && tvDurasi != null)
                        tvDurasi.setText(durasi);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Gagal memuat data Trap: " + error.getMessage(), Toast.LENGTH_SHORT)
                            .show();
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
                    String health = snapshot.child("health").getValue(String.class);

                    if (percent != null) {
                        if (pbBattery != null)
                            pbBattery.setProgress(percent);
                        if (tvBatteryPercent != null)
                            tvBatteryPercent.setText(percent + "%");
                    }
                    if (voltage != null && tvVoltage != null)
                        tvVoltage.setText(voltage);
                    if (health != null && tvHealth != null)
                        tvHealth.setText(health);
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Gagal memuat data Baterai: " + error.getMessage(), Toast.LENGTH_SHORT)
                            .show();
                }
            }
        });
    }

    private void showDatePicker() {
        if (getContext() == null)
            return;

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                getContext(),
                (view, year, month, dayOfMonth) -> {
                    selectedCalendar.set(Calendar.YEAR, year);
                    selectedCalendar.set(Calendar.MONTH, month);
                    selectedCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    updateCalendarText();

                    // TODO: Tambahkan query Firebase berdasarkan tanggal (jika struktur database
                    // mendukung history harian)
                    // Contoh:
                    // trapRef.orderByChild("tanggal").equalTo("2026-07-15").addListenerForSingleValueEvent(...)

                    Toast.makeText(getContext(), "Menampilkan data untuk: " + tvCalendarDate.getText().toString(),
                            Toast.LENGTH_SHORT).show();
                },
                selectedCalendar.get(Calendar.YEAR),
                selectedCalendar.get(Calendar.MONTH),
                selectedCalendar.get(Calendar.DAY_OF_MONTH));
        datePickerDialog.show();
    }

    private void updateCalendarText() {
        SimpleDateFormat sdf = new SimpleDateFormat("EEE, d MMM", new Locale("id", "ID"));
        String dateStr = sdf.format(selectedCalendar.getTime());
        if (tvCalendarDate != null) {
            tvCalendarDate.setText(dateStr);
        }
    }

    private void setupClockBasedStatus() {
        android.os.Handler handler = new android.os.Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                Calendar calendar = Calendar.getInstance();
                int hour = calendar.get(Calendar.HOUR_OF_DAY);

                if (hour >= 6 && hour < 18) {
                    // Siang (Day) -> Night Card Inactive
                    if (dotNight != null) {
                        dotNight.setBackgroundResource(R.drawable.bg_circle_soft);
                        dotNight.setBackgroundTintList(
                                android.content.res.ColorStateList.valueOf(Color.parseColor("#9FA8DA")));
                    }
                    if (tvNightStatus != null) {
                        tvNightStatus.setText("PERANGKAP TIDAK AKTIF");
                        tvNightStatus.setTextColor(Color.parseColor("#7986CB"));
                    }
                    if (borderNight != null)
                        borderNight.setBackgroundColor(Color.TRANSPARENT);

                    // Day Card Active (Tapi status perangkap mati di siang hari)
                    if (dotDay != null) {
                        dotDay.setBackgroundResource(R.drawable.dot_green);
                        dotDay.setBackgroundTintList(null);
                    }
                    if (tvDayStatus != null) {
                        tvDayStatus.setText("PERANGKAP TIDAK AKTIF");
                        tvDayStatus.setTextColor(Color.parseColor("#5C6BC0"));
                    }
                    if (borderDay != null)
                        borderDay.setBackgroundColor(Color.parseColor("#5C6BC0"));

                } else {
                    // Malam (Night) -> Night Card Active
                    if (dotNight != null) {
                        dotNight.setBackgroundResource(R.drawable.dot_green);
                        dotNight.setBackgroundTintList(null);
                    }
                    if (tvNightStatus != null) {
                        tvNightStatus.setText("PERANGKAP AKTIF");
                        tvNightStatus.setTextColor(Color.parseColor("#5C6BC0"));
                    }
                    if (borderNight != null)
                        borderNight.setBackgroundColor(Color.parseColor("#5C6BC0"));

                    // Day Card Inactive
                    if (dotDay != null) {
                        dotDay.setBackgroundResource(R.drawable.bg_circle_soft);
                        dotDay.setBackgroundTintList(
                                android.content.res.ColorStateList.valueOf(Color.parseColor("#9FA8DA")));
                    }
                    if (tvDayStatus != null) {
                        tvDayStatus.setText("PERANGKAP TIDAK AKTIF");
                        tvDayStatus.setTextColor(Color.parseColor("#7986CB"));
                    }
                    if (borderDay != null)
                        borderDay.setBackgroundColor(Color.TRANSPARENT);
                }

                handler.postDelayed(this, 10000); // Cek setiap 10 detik
            }
        };
        handler.post(runnable);
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
            android.transition.TransitionManager.beginDelayedTransition((android.view.ViewGroup) view);
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
            android.transition.TransitionManager.beginDelayedTransition((android.view.ViewGroup) view);
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
