package com.pebelti.pestlighttrap;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import android.app.DatePickerDialog;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class TrapAnalysisFragment extends Fragment {

    // ─── VIEWS: Trap Section ────────────────────────────────────────────
    private TextView tvDurasiSiklus;
    private TextView tvMulai;
    private TextView tvSelesai;
    private TextView tvDurasi;
    private TextView tvTimelineStart;
    private TextView tvTimelineMid;
    private TextView tvTimelineEnd;
    private LinearLayout layoutDailyCycleItems;
    private TextView tvDailyCycleCount;

    // ─── VIEWS: Battery Section ─────────────────────────────────────────
    private ProgressBar pbBattery;
    private TextView tvBatteryPercent;
    private TextView tvVoltage;
    private TextView tvHealth;

    // ─── VIEWS: Day/Night Status ────────────────────────────────────────
    private View dotNight;
    private TextView tvNightStatus;
    private View borderNight;
    private View dotDay;
    private TextView tvDayStatus;
    private View borderDay;

    // ─── VIEWS: Calendar ────────────────────────────────────────────────
    private LinearLayout btnCalendar;
    private TextView tvCalendarDate;
    private Calendar selectedCalendar = Calendar.getInstance();

    // ─── FIREBASE REFERENCES ────────────────────────────────────────────
    private DatabaseReference trapAnalysisRef;
    private DatabaseReference batteryRef;
    private DatabaseReference operationRef;
    private DatabaseReference statusRef;
    private DatabaseReference dailyLogsRef;

    // ─── FIREBASE LISTENERS (untuk cleanup) ─────────────────────────────
    private ValueEventListener trapAnalysisListener;
    private ValueEventListener batteryListener;
    private ValueEventListener operationListener;
    private ValueEventListener statusListener;
    private ValueEventListener dailyLogsListener;

    // ─── HANDLER: Akumulasi waktu aktif ─────────────────────────────────
    private Handler activeTimeHandler;
    private Runnable activeTimeRunnable;
    private boolean isDeviceActive = false;

    // ─── HANDLER: Clock-based status ────────────────────────────────────
    private Handler clockHandler;
    private Runnable clockRunnable;

    // ─── CACHED DATA ────────────────────────────────────────────────────
    private String cachedStartTime = "18:00";
    private String cachedEndTime = "06:00";

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
        tvTimelineStart = view.findViewById(R.id.tvTimelineStart);
        tvTimelineMid = view.findViewById(R.id.tvTimelineMid);
        tvTimelineEnd = view.findViewById(R.id.tvTimelineEnd);
        layoutDailyCycleItems = view.findViewById(R.id.layoutDailyCycleItems);
        tvDailyCycleCount = view.findViewById(R.id.tvDailyCycleCount);

        // 2. Bind Views (Battery)
        pbBattery = view.findViewById(R.id.pb_battery);
        tvBatteryPercent = view.findViewById(R.id.tv_battery_percent);
        tvVoltage = view.findViewById(R.id.tv_voltage);
        tvHealth = view.findViewById(R.id.tv_health);

        // 3. Bind Views (Day/Night)
        dotNight = view.findViewById(R.id.dotNight);
        tvNightStatus = view.findViewById(R.id.tvNightStatus);
        borderNight = view.findViewById(R.id.borderNight);
        dotDay = view.findViewById(R.id.dotDay);
        tvDayStatus = view.findViewById(R.id.tvDayStatus);
        borderDay = view.findViewById(R.id.borderDay);

        // 4. Bind Views (Calendar)
        btnCalendar = view.findViewById(R.id.btnCalendar);
        tvCalendarDate = view.findViewById(R.id.tvCalendarDate);

        // 5. Inisialisasi Firebase
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        trapAnalysisRef = database.getReference("smart_pest_trap/trap_analysis");
        batteryRef = database.getReference("smart_pest_trap/battery");
        operationRef = database.getReference("smart_pest_trap/operation_mode");
        statusRef = database.getReference("smart_pest_trap/status");
        dailyLogsRef = database.getReference("smart_pest_trap/daily_logs");

        // 6. Load data real-time
        loadTrapAnalysisData();
        loadBatteryData();
        syncTrapAnalysisFromOperationMode();
        setupDeviceStatusListener();
        loadDailyCycleReport();

        // 7. Setup UI
        updateCalendarText();
        btnCalendar.setOnClickListener(v -> showDatePicker());
        setupClockBasedStatus();
        setupTabSwitching(view);

        // 8. Tombol Kembali
        View btnBack = view.findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeDashboardFragment())
                    .commit();
        });

        return view;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // FIREBASE LISTENERS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Membaca data trap_analysis (durasi_siklus, mulai, selesai, durasi).
     * Data ini diupdate oleh syncTrapAnalysisFromOperationMode().
     */
    private void loadTrapAnalysisData() {
        trapAnalysisListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || !snapshot.exists())
                    return;

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

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getContext() != null)
                    Toast.makeText(getContext(), "Gagal memuat data Trap", Toast.LENGTH_SHORT).show();
            }
        };
        trapAnalysisRef.addValueEventListener(trapAnalysisListener);
    }

    /**
     * Membaca data baterai (percent, voltage, health).
     */
    private void loadBatteryData() {
        batteryListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || !snapshot.exists())
                    return;

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

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getContext() != null)
                    Toast.makeText(getContext(), "Gagal memuat data Baterai", Toast.LENGTH_SHORT).show();
            }
        };
        batteryRef.addValueEventListener(batteryListener);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // SINKRONISASI TRAP_ANALYSIS ↔ OPERATION_MODE
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Listener pada operation_mode.
     * Ketika start_time/end_time berubah, otomatis update trap_analysis dan
     * timeline.
     */
    private void syncTrapAnalysisFromOperationMode() {
        operationListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || !snapshot.exists())
                    return;

                String start = snapshot.child("start_time").getValue(String.class);
                String end = snapshot.child("end_time").getValue(String.class);

                if (start == null || end == null)
                    return;

                cachedStartTime = start;
                cachedEndTime = end;

                // Hitung durasi dari start_time dan end_time
                int durasiMenit = calculateDurationMinutes(start, end);
                int jam = durasiMenit / 60;
                int menit = durasiMenit % 60;

                String durasiSiklus = jam + "J " + menit + "M";
                String durasiShort = jam + "." + (menit * 10 / 60) + "j";

                // Update trap_analysis di Firebase
                trapAnalysisRef.child("mulai").setValue(start);
                trapAnalysisRef.child("selesai").setValue(end);
                trapAnalysisRef.child("durasi_siklus").setValue(durasiSiklus);
                trapAnalysisRef.child("durasi").setValue(durasiShort);

                // Update timeline labels
                if (tvTimelineStart != null)
                    tvTimelineStart.setText("00:00");
                if (tvTimelineMid != null)
                    tvTimelineMid.setText(start);
                if (tvTimelineEnd != null)
                    tvTimelineEnd.setText(end);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Silent fail
            }
        };
        operationRef.addValueEventListener(operationListener);
    }

    /**
     * Menghitung durasi dalam menit antara dua waktu (format "HH:mm").
     * Mendukung rentang melewati tengah malam (misal 18:00 → 06:00 = 12 jam).
     */
    private int calculateDurationMinutes(String startTime, String endTime) {
        try {
            String[] startParts = startTime.split(":");
            String[] endParts = endTime.split(":");

            int startMins = Integer.parseInt(startParts[0]) * 60 + Integer.parseInt(startParts[1]);
            int endMins = Integer.parseInt(endParts[0]) * 60 + Integer.parseInt(endParts[1]);

            if (endMins > startMins) {
                return endMins - startMins;
            } else {
                // Melewati tengah malam
                return (24 * 60 - startMins) + endMins;
            }
        } catch (Exception e) {
            return 0;
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // AKUMULASI WAKTU AKTIF HARIAN
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Listener pada smart_pest_trap/status.
     * Jika status = true, jalankan Handler yang menambah active_minutes setiap 60
     * detik.
     * Jika status = false, hentikan Handler.
     * Juga update tampilan siang/malam.
     */
    private void setupDeviceStatusListener() {
        activeTimeHandler = new Handler(Looper.getMainLooper());

        statusListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded())
                    return;

                Boolean status = snapshot.getValue(Boolean.class);
                isDeviceActive = status != null && status;

                // Update status siang/malam berdasarkan status perangkat
                updateDayNightFromDeviceStatus();

                // Mulai/hentikan akumulasi waktu aktif
                if (isDeviceActive) {
                    startActiveTimeAccumulator();
                } else {
                    stopActiveTimeAccumulator();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Silent fail
            }
        };
        statusRef.addValueEventListener(statusListener);
    }

    /**
     * Menjalankan Handler yang menambah active_minutes setiap 60 detik.
     */
    private void startActiveTimeAccumulator() {
        // Hentikan yang sedang berjalan terlebih dahulu
        stopActiveTimeAccumulator();

        activeTimeRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isAdded() || !isDeviceActive)
                    return;

                String todayKey = getTodayDateKey();
                DatabaseReference todayMinutesRef = dailyLogsRef.child(todayKey).child("active_minutes");

                // Increment active_minutes
                todayMinutesRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot snapshot) {
                        Long currentMinutes = snapshot.getValue(Long.class);
                        long newMinutes = (currentMinutes != null ? currentMinutes : 0) + 1;
                        todayMinutesRef.setValue(newMinutes);

                        // Update timestamp
                        dailyLogsRef.child(todayKey).child("timestamp")
                                .setValue(System.currentTimeMillis() / 1000);
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {
                        // Silent fail
                    }
                });

                // Schedule next tick (60 detik)
                activeTimeHandler.postDelayed(this, 60000);
            }
        };

        // Mulai segera
        activeTimeHandler.post(activeTimeRunnable);
    }

    /**
     * Hentikan akumulasi waktu aktif.
     */
    private void stopActiveTimeAccumulator() {
        if (activeTimeHandler != null && activeTimeRunnable != null) {
            activeTimeHandler.removeCallbacks(activeTimeRunnable);
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // LAPORAN HARIAN (DAILY CYCLE REPORT)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Membaca daily_logs 7 hari terakhir dan mengisi container laporan harian.
     * Setiap item menampilkan: tanggal, akumulasi jam aktif, badge status.
     */
    private void loadDailyCycleReport() {
        List<String> dateKeys = getLast7DaysKeys();

        dailyLogsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded())
                    return;

                List<DailyCycleItem> items = new ArrayList<>();

                for (String dateKey : dateKeys) {
                    DataSnapshot daySnapshot = snapshot.child(dateKey);
                    if (daySnapshot.exists()) {
                        Long activeMinutes = daySnapshot.child("active_minutes").getValue(Long.class);
                        if (activeMinutes != null && activeMinutes > 0) {
                            items.add(new DailyCycleItem(dateKey, activeMinutes.intValue()));
                        }
                    }
                }

                // Update count label
                if (tvDailyCycleCount != null) {
                    tvDailyCycleCount.setText("☰ " + items.size() + " LAPORAN");
                }

                // Populate items (terbaru duluan, sudah diurutkan dari getLast7DaysKeys)
                populateDailyCycleItems(items);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getContext() != null)
                    Toast.makeText(getContext(), "Gagal memuat laporan harian", Toast.LENGTH_SHORT).show();
            }
        };
        dailyLogsRef.addValueEventListener(dailyLogsListener);
    }

    /**
     * Model data untuk item laporan harian.
     */
    private static class DailyCycleItem {
        String dateKey; // yyyy-MM-dd
        int activeMinutes;

        DailyCycleItem(String dateKey, int activeMinutes) {
            this.dateKey = dateKey;
            this.activeMinutes = activeMinutes;
        }
    }

    /**
     * Mengisi container laporan harian secara programmatic.
     */
    private void populateDailyCycleItems(List<DailyCycleItem> items) {
        if (layoutDailyCycleItems == null || getContext() == null)
            return;
        layoutDailyCycleItems.removeAllViews();

        if (items.isEmpty()) {
            TextView emptyText = new TextView(getContext());
            emptyText.setText("Belum ada data harian");
            emptyText.setTextColor(Color.parseColor("#9FA8DA"));
            emptyText.setTextSize(14f);
            emptyText.setGravity(Gravity.CENTER);
            emptyText.setPadding(0, dpToPx(24), 0, dpToPx(24));
            layoutDailyCycleItems.addView(emptyText);
            return;
        }

        // Urutkan terbalik (terbaru duluan)
        List<DailyCycleItem> reversed = new ArrayList<>(items);
        java.util.Collections.reverse(reversed);

        for (int i = 0; i < reversed.size(); i++) {
            DailyCycleItem item = reversed.get(i);

            // Separator sebelum item (kecuali yang pertama)
            if (i > 0) {
                View divider = new View(getContext());
                divider.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(1)));
                divider.setBackgroundColor(Color.parseColor("#E8EAF6"));
                LinearLayout.LayoutParams divParams = (LinearLayout.LayoutParams) divider.getLayoutParams();
                divParams.topMargin = dpToPx(4);
                divParams.bottomMargin = dpToPx(4);
                layoutDailyCycleItems.addView(divider);
            }

            View itemView = createDailyCycleItemView(item);
            layoutDailyCycleItems.addView(itemView);
        }
    }

    /**
     * Membuat View item laporan harian (icon, tanggal, durasi, badge status).
     */
    private View createDailyCycleItemView(DailyCycleItem item) {
        // Container utama
        RelativeLayout container = new RelativeLayout(getContext());
        container.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));
        container.setPadding(0, dpToPx(12), 0, dpToPx(12));

        // Icon bulan
        ImageView icon = new ImageView(getContext());
        int iconId = View.generateViewId();
        icon.setId(iconId);
        RelativeLayout.LayoutParams iconParams = new RelativeLayout.LayoutParams(dpToPx(40), dpToPx(40));
        icon.setLayoutParams(iconParams);
        icon.setImageResource(R.drawable.ic_moon);
        icon.setColorFilter(Color.WHITE);
        icon.setBackgroundResource(R.drawable.bg_circle_soft);
        icon.setPadding(dpToPx(8), dpToPx(8), dpToPx(8), dpToPx(8));
        container.addView(icon);

        // Content (tanggal + durasi)
        LinearLayout content = new LinearLayout(getContext());
        content.setOrientation(LinearLayout.VERTICAL);
        RelativeLayout.LayoutParams contentParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        contentParams.addRule(RelativeLayout.END_OF, iconId);
        contentParams.setMarginStart(dpToPx(16));
        content.setLayoutParams(contentParams);

        // Tanggal
        TextView tvDate = new TextView(getContext());
        tvDate.setText(formatDateKeyToDisplay(item.dateKey));
        tvDate.setTextColor(Color.parseColor("#191970"));
        tvDate.setTextSize(16f);
        tvDate.setTypeface(null, Typeface.BOLD);
        content.addView(tvDate);

        // Durasi
        TextView tvDuration = new TextView(getContext());
        int jam = item.activeMinutes / 60;
        int menit = item.activeMinutes % 60;
        tvDuration.setText(jam + "J " + menit + "M");
        tvDuration.setTextColor(Color.parseColor("#7986CB"));
        tvDuration.setTextSize(14f);
        content.addView(tvDuration);

        container.addView(content);

        // Badge status
        TextView badge = new TextView(getContext());
        int scheduledMinutes = calculateDurationMinutes(cachedStartTime, cachedEndTime);
        // Tentukan status berdasarkan persentase aktif vs jadwal
        float ratio = scheduledMinutes > 0 ? (float) item.activeMinutes / scheduledMinutes : 0;
        String badgeText;
        String badgeColor;

        if (ratio >= 0.8f) {
            badgeText = "OK";
            badgeColor = "#5C6BC0";
        } else if (ratio >= 0.5f) {
            badgeText = "SEDANG";
            badgeColor = "#FFA000";
        } else {
            badgeText = "RENDAH";
            badgeColor = "#F44336";
        }

        badge.setText(badgeText);
        badge.setTextColor(Color.parseColor(badgeColor));
        badge.setTextSize(12f);
        badge.setTypeface(null, Typeface.BOLD);
        badge.setBackgroundResource(R.drawable.bg_badge_green);
        badge.setPadding(dpToPx(16), dpToPx(6), dpToPx(16), dpToPx(6));

        RelativeLayout.LayoutParams badgeParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        badgeParams.addRule(RelativeLayout.ALIGN_PARENT_END);
        badgeParams.addRule(RelativeLayout.CENTER_VERTICAL);
        badge.setLayoutParams(badgeParams);
        container.addView(badge);

        return container;
    }

    // ═══════════════════════════════════════════════════════════════════════
    // STATUS SIANG/MALAM (BERDASARKAN JAM & STATUS PERANGKAT)
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Setup clock-based status checker yang berjalan periodik.
     */
    private void setupClockBasedStatus() {
        clockHandler = new Handler(Looper.getMainLooper());
        clockRunnable = new Runnable() {
            @Override
            public void run() {
                if (!isAdded())
                    return;
                updateDayNightFromDeviceStatus();
                clockHandler.postDelayed(this, 10000); // Cek setiap 10 detik
            }
        };
        clockHandler.post(clockRunnable);
    }

    /**
     * Update tampilan card Siang/Malam berdasarkan waktu lokal dan status
     * perangkat.
     */
    private void updateDayNightFromDeviceStatus() {
        Calendar calendar = Calendar.getInstance();
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        boolean isNightTime = (hour >= 18 || hour < 6);

        if (isNightTime) {
            // Malam — Night Card Active, status berdasarkan isDeviceActive
            if (dotNight != null) {
                dotNight.setBackgroundResource(isDeviceActive ? R.drawable.dot_green : R.drawable.dot_red);
                dotNight.setBackgroundTintList(null);
            }
            if (tvNightStatus != null) {
                tvNightStatus.setText(isDeviceActive ? "PERANGKAP AKTIF" : "PERANGKAP TIDAK AKTIF");
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

        } else {
            // Siang — Day Card Active
            if (dotDay != null) {
                dotDay.setBackgroundResource(isDeviceActive ? R.drawable.dot_green : R.drawable.dot_red);
                dotDay.setBackgroundTintList(null);
            }
            if (tvDayStatus != null) {
                tvDayStatus.setText(isDeviceActive ? "PERANGKAP AKTIF" : "PERANGKAP TIDAK AKTIF");
                tvDayStatus.setTextColor(Color.parseColor("#5C6BC0"));
            }
            if (borderDay != null)
                borderDay.setBackgroundColor(Color.parseColor("#5C6BC0"));

            // Night Card Inactive
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
        }
    }

    // ═══════════════════════════════════════════════════════════════════════
    // TAB SWITCHING
    // ═══════════════════════════════════════════════════════════════════════

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

    // ═══════════════════════════════════════════════════════════════════════
    // CALENDAR / DATE PICKER
    // ═══════════════════════════════════════════════════════════════════════

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

                    // Muat data untuk tanggal yang dipilih
                    loadDataForSelectedDate();
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

    /**
     * Memuat data daily_logs untuk tanggal yang dipilih dari kalender.
     * Menampilkan durasi aktivitas pada hari tersebut.
     */
    private void loadDataForSelectedDate() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String dateKey = sdf.format(selectedCalendar.getTime());

        dailyLogsRef.child(dateKey).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded())
                    return;

                if (snapshot.exists()) {
                    Long activeMinutes = snapshot.child("active_minutes").getValue(Long.class);
                    if (activeMinutes != null && tvDurasiSiklus != null) {
                        int jam = (int) (activeMinutes / 60);
                        int menit = (int) (activeMinutes % 60);
                        tvDurasiSiklus.setText(jam + "J " + menit + "M");
                    }
                } else {
                    if (tvDurasiSiklus != null)
                        tvDurasiSiklus.setText("0J 0M");
                    if (getContext() != null)
                        Toast.makeText(getContext(), "Tidak ada data untuk tanggal ini", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Silent fail
            }
        });
    }

    // ═══════════════════════════════════════════════════════════════════════
    // HELPER METHODS
    // ═══════════════════════════════════════════════════════════════════════

    /**
     * Key tanggal hari ini (format: yyyy-MM-dd).
     */
    private String getTodayDateKey() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(Calendar.getInstance().getTime());
    }

    /**
     * Daftar key tanggal 7 hari terakhir (termasuk hari ini).
     */
    private List<String> getLast7DaysKeys() {
        List<String> keys = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -6);

        for (int i = 0; i < 7; i++) {
            keys.add(sdf.format(cal.getTime()));
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
        return keys;
    }

    /**
     * Format "yyyy-MM-dd" → "SENIN, 18 JUN" (tampilan laporan harian).
     */
    private String formatDateKeyToDisplay(String dateKey) {
        try {
            SimpleDateFormat inputFormat = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
            SimpleDateFormat outputFormat = new SimpleDateFormat("EEEE, d MMM", new Locale("id", "ID"));
            java.util.Date date = inputFormat.parse(dateKey);
            if (date != null) {
                return outputFormat.format(date).toUpperCase();
            }
        } catch (Exception e) {
            // Fallback
        }
        return dateKey;
    }

    /**
     * Konversi dp ke pixel.
     */
    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    // ═══════════════════════════════════════════════════════════════════════
    // LIFECYCLE CLEANUP
    // ═══════════════════════════════════════════════════════════════════════

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        // Hapus Firebase listeners
        if (trapAnalysisRef != null && trapAnalysisListener != null)
            trapAnalysisRef.removeEventListener(trapAnalysisListener);
        if (batteryRef != null && batteryListener != null)
            batteryRef.removeEventListener(batteryListener);
        if (operationRef != null && operationListener != null)
            operationRef.removeEventListener(operationListener);
        if (statusRef != null && statusListener != null)
            statusRef.removeEventListener(statusListener);
        if (dailyLogsRef != null && dailyLogsListener != null)
            dailyLogsRef.removeEventListener(dailyLogsListener);

        // Hentikan Handlers
        stopActiveTimeAccumulator();
        if (clockHandler != null && clockRunnable != null) {
            clockHandler.removeCallbacks(clockRunnable);
        }
    }
}
