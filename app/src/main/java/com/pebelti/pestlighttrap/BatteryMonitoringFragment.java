package com.pebelti.pestlighttrap;

import android.graphics.Color;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.IndexAxisValueFormatter;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import android.widget.ImageView;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class BatteryMonitoringFragment extends Fragment {

    private PieChart pieChart;
    private BarChart barChartHistory;
    private BarChart barChartPest;

    // Flag untuk lazy-loading chart baterai (hanya setup sekali)
    private boolean batteryChartsInitialized = false;

    // Label hari yang dipakai bersama oleh kedua bar chart
    private static final String[] DAYS = {"SEN", "SEL", "RAB", "KAM", "JUM", "SAB", "MIN"};

    // Nama hari lengkap untuk laporan harian
    private static final String[] DAYS_FULL = {"SENIN", "SELASA", "RABU", "KAMIS", "JUMAT", "SABTU", "MINGGU"};

    // Firebase References
    private DatabaseReference batteryRef;
    private DatabaseReference powerRef;
    private DatabaseReference trapRef;
    private DatabaseReference dailyLogsRef;

    // Firebase Listeners (untuk cleanup di onDestroyView)
    private ValueEventListener batteryListener;
    private ValueEventListener powerListener;
    private ValueEventListener trapListener;
    private ValueEventListener dailyLogsListener;

    // Views - Tab Perangkap
    private TextView tvWeeklyDate;
    private TextView tvTrapCapacityPercent;
    private TextView tvTrapCapacityStatus;
    private View dotTrapCapacity;
    private TextView tvTrapSurfaceDistance;
    private TextView tvTrapSurfaceStatus;
    private View dotTrapSurface;
    private LinearLayout layoutDailyItems;

    // Views - Tab Baterai
    private TextView tvBattVoltage;
    private TextView tvBattHealthBadge;
    private TextView tvBattLifetime;
    private TextView tvPanelEfficiency;
    private View viewPanelProgressFill;
    private TextView tvPanelPeakTime;
    private TextView tvBattCyclePercent;
    private View viewBattProgressFill;
    private TextView tvBattStatusTime;

    // Data cache untuk perhitungan
    private int currentBatteryPercent = 0;
    private int currentPowerConsumption = 0;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_trap_analysis, container, false);

        // Bind chart views
        pieChart = view.findViewById(R.id.pieChartBattery);
        barChartHistory = view.findViewById(R.id.barChartHistory);
        barChartPest = view.findViewById(R.id.barChartPest);

        // Bind views - Tab Perangkap
        tvWeeklyDate = view.findViewById(R.id.tvWeeklyDate);
        tvTrapCapacityPercent = view.findViewById(R.id.tvTrapCapacityPercent);
        tvTrapCapacityStatus = view.findViewById(R.id.tvTrapCapacityStatus);
        dotTrapCapacity = view.findViewById(R.id.dotTrapCapacity);
        tvTrapSurfaceDistance = view.findViewById(R.id.tvTrapSurfaceDistance);
        tvTrapSurfaceStatus = view.findViewById(R.id.tvTrapSurfaceStatus);
        dotTrapSurface = view.findViewById(R.id.dotTrapSurface);
        layoutDailyItems = view.findViewById(R.id.layoutDailyItems);

        // Bind views - Tab Baterai
        tvBattVoltage = view.findViewById(R.id.tvBattVoltage);
        tvBattHealthBadge = view.findViewById(R.id.tvBattHealthBadge);
        tvBattLifetime = view.findViewById(R.id.tvBattLifetime);
        tvPanelEfficiency = view.findViewById(R.id.tvPanelEfficiency);
        viewPanelProgressFill = view.findViewById(R.id.viewPanelProgressFill);
        tvPanelPeakTime = view.findViewById(R.id.tvPanelPeakTime);
        tvBattCyclePercent = view.findViewById(R.id.tvBattCyclePercent);
        viewBattProgressFill = view.findViewById(R.id.viewBattProgressFill);
        tvBattStatusTime = view.findViewById(R.id.tvBattStatusTime);

        // Inisialisasi Firebase References
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        batteryRef = database.getReference("smart_pest_trap/battery");
        powerRef = database.getReference("smart_pest_trap/power_consumption");
        trapRef = database.getReference("smart_pest_trap/trap_fullness");
        dailyLogsRef = database.getReference("smart_pest_trap/daily_logs");

        // Update tanggal mingguan
        updateWeeklyDateLabel();

        // Setup chart kosong dulu (tab Perangkap aktif by default)
        setupBarChartPest(new float[]{0, 0, 0, 0, 0, 0, 0});

        // Load data real-time dari Firebase
        loadRealtimeTrapData();
        loadRealtimeBatteryData();
        loadRealtimePowerData();

        // Load data mingguan & harian dari daily_logs
        loadWeeklyData();

        // Simpan snapshot harian
        saveDailySnapshot();

        // Setup tab switching & tombol kembali
        setupTabSwitching(view);
        setupBackButton(view);

        return view;
    }

    // ─── FIREBASE REAL-TIME LISTENERS ───────────────────────────────────

    /**
     * Listener real-time untuk data trap_fullness.
     * Update card Status Kapasitas & Jarak Permukaan di tab Perangkap.
     */
    private void loadRealtimeTrapData() {
        trapListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || snapshot == null) return;
                if (snapshot.exists()) {
                    Integer percent = snapshot.getValue(Integer.class);
                    if (percent != null) {
                        // Update Status Kapasitas
                        if (tvTrapCapacityPercent != null)
                            tvTrapCapacityPercent.setText(percent + "%");

                        // Hitung jarak permukaan simulasi (semakin penuh semakin dekat)
                        int maxDistance = 50; // cm
                        int currentDistance = maxDistance - (int)(maxDistance * percent / 100.0);
                        if (tvTrapSurfaceDistance != null)
                            tvTrapSurfaceDistance.setText(maxDistance + " / " + currentDistance + " cm");

                        // Update status & warna indikator
                        if (percent >= 80) {
                            setTrapStatus("PENUH", "#F44336", true);
                        } else if (percent >= 50) {
                            setTrapStatus("SETENGAH", "#FFA000", false);
                        } else {
                            setTrapStatus("KOSONG", "#4CAF50", false);
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Gagal memuat data Perangkap", Toast.LENGTH_SHORT).show();
                }
            }
        };
        trapRef.addValueEventListener(trapListener);
    }

    /**
     * Helper untuk mengupdate status trap dan warna indikator.
     */
    private void setTrapStatus(String status, String color, boolean isFull) {
        if (tvTrapCapacityStatus != null) {
            tvTrapCapacityStatus.setText(status);
        }
        if (dotTrapCapacity != null) {
            if (isFull) {
                dotTrapCapacity.setBackgroundResource(R.drawable.dot_red);
            } else {
                dotTrapCapacity.setBackgroundResource(R.drawable.dot_green);
            }
        }

        // Surface status mengikuti
        if (tvTrapSurfaceStatus != null) {
            if (isFull) {
                tvTrapSurfaceStatus.setText("PENUH");
            } else {
                tvTrapSurfaceStatus.setText("TERSEDIA");
            }
        }
        if (dotTrapSurface != null) {
            if (isFull) {
                dotTrapSurface.setBackgroundResource(R.drawable.dot_red);
            } else {
                dotTrapSurface.setBackgroundResource(R.drawable.dot_green);
            }
        }
    }

    /**
     * Listener real-time untuk data baterai.
     * Update PieChart, tegangan, badge kesehatan, estimasi masa pakai di tab Baterai.
     */
    private void loadRealtimeBatteryData() {
        batteryListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || snapshot == null) return;
                if (snapshot.exists()) {
                    Integer percent = snapshot.child("percent").getValue(Integer.class);
                    String voltage = snapshot.child("voltage").getValue(String.class);
                    String health = snapshot.child("health").getValue(String.class);

                    if (percent != null) {
                        currentBatteryPercent = percent;

                        // Update PieChart jika tab baterai sudah pernah diakses
                        if (batteryChartsInitialized) {
                            setupPieChart(percent);
                        }

                        // Update Baterai % card
                        if (tvBattCyclePercent != null)
                            tvBattCyclePercent.setText(percent + "%");

                        // Update progress bar di card Baterai %
                        updateCustomProgressBar(viewBattProgressFill, percent);

                        // Update status time
                        updateBattLifetimeEstimate();

                        // Update battery status time label
                        if (tvBattStatusTime != null) {
                            float hours = calculateLifetimeHours();
                            tvBattStatusTime.setText(String.format(Locale.getDefault(), "STATUS: %.1f JAM", hours));
                        }
                    }

                    if (voltage != null && tvBattVoltage != null) {
                        tvBattVoltage.setText(voltage);
                    }

                    if (health != null && tvBattHealthBadge != null) {
                        tvBattHealthBadge.setText(health.toUpperCase());
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Gagal memuat data Baterai", Toast.LENGTH_SHORT).show();
                }
            }
        };
        batteryRef.addValueEventListener(batteryListener);
    }

    /**
     * Listener real-time untuk data power_consumption.
     * Update card Analisis Panel di tab Baterai.
     */
    private void loadRealtimePowerData() {
        powerListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded() || snapshot == null) return;
                if (snapshot.exists()) {
                    Integer value = snapshot.getValue(Integer.class);
                    if (value != null) {
                        currentPowerConsumption = value;

                        // Efisiensi panel = 100 - power_consumption (semakin rendah konsumsi = efisiensi tinggi)
                        int efficiency = Math.max(0, Math.min(100, 100 - value));
                        if (tvPanelEfficiency != null)
                            tvPanelEfficiency.setText(efficiency + "%");

                        // Update progress bar panel
                        updateCustomProgressBar(viewPanelProgressFill, efficiency);

                        // Update peak time (simulasi berdasarkan konsumsi)
                        if (tvPanelPeakTime != null) {
                            if (value >= 60) {
                                tvPanelPeakTime.setText("PUNCAK: 12:00");
                            } else if (value >= 30) {
                                tvPanelPeakTime.setText("PUNCAK: 14:00");
                            } else {
                                tvPanelPeakTime.setText("DAYA RENDAH");
                            }
                        }

                        // Recalculate battery lifetime
                        updateBattLifetimeEstimate();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Gagal memuat data Konsumsi Daya", Toast.LENGTH_SHORT).show();
                }
            }
        };
        powerRef.addValueEventListener(powerListener);
    }

    // ─── ESTIMASI MASA PAKAI ────────────────────────────────────────────

    /**
     * Menghitung estimasi masa pakai baterai dalam jam.
     * Rumus: (battery_percent / max(power_consumption, 1)) * 12
     */
    private float calculateLifetimeHours() {
        int power = Math.max(currentPowerConsumption, 1);
        return ((float) currentBatteryPercent / power) * 12f;
    }

    /**
     * Update tampilan estimasi masa pakai.
     */
    private void updateBattLifetimeEstimate() {
        if (tvBattLifetime == null) return;
        float hours = calculateLifetimeHours();
        if (hours >= 1f) {
            tvBattLifetime.setText(String.format(Locale.getDefault(), "%.0fH", hours));
        } else {
            int minutes = (int)(hours * 60);
            tvBattLifetime.setText(minutes + "M");
        }
    }

    // ─── CUSTOM PROGRESS BAR UPDATE ─────────────────────────────────────

    /**
     * Update custom progress bar (View dengan layout_marginEnd).
     * Menghitung margin berdasarkan persentase.
     */
    private void updateCustomProgressBar(View progressFill, int percent) {
        if (progressFill == null) return;
        progressFill.post(() -> {
            if (progressFill.getParent() == null) return;
            View parent = (View) progressFill.getParent();
            int parentWidth = parent.getWidth();
            if (parentWidth <= 0) return;

            int marginEnd = (int)(parentWidth * (100 - percent) / 100.0);
            ViewGroup.MarginLayoutParams params = (ViewGroup.MarginLayoutParams) progressFill.getLayoutParams();
            params.setMarginEnd(marginEnd);
            progressFill.setLayoutParams(params);
        });
    }

    // ─── DAILY SNAPSHOT ─────────────────────────────────────────────────

    /**
     * Menyimpan snapshot data hari ini ke Firebase daily_logs.
     * Hanya menulis jika belum ada data untuk hari ini.
     */
    private void saveDailySnapshot() {
        String todayKey = getTodayDateKey();
        dailyLogsRef.child(todayKey).addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Selalu update data hari ini dengan nilai terbaru
                batteryRef.addListenerForSingleValueEvent(new ValueEventListener() {
                    @Override
                    public void onDataChange(@NonNull DataSnapshot battSnapshot) {
                        powerRef.addListenerForSingleValueEvent(new ValueEventListener() {
                            @Override
                            public void onDataChange(@NonNull DataSnapshot powerSnapshot) {
                                trapRef.addListenerForSingleValueEvent(new ValueEventListener() {
                                    @Override
                                    public void onDataChange(@NonNull DataSnapshot trapSnapshot) {
                                        Map<String, Object> dailyData = new HashMap<>();

                                        // Battery data
                                        if (battSnapshot.exists()) {
                                            Integer percent = battSnapshot.child("percent").getValue(Integer.class);
                                            String voltage = battSnapshot.child("voltage").getValue(String.class);
                                            String health = battSnapshot.child("health").getValue(String.class);
                                            if (percent != null) dailyData.put("battery_percent", percent);
                                            if (voltage != null) dailyData.put("battery_voltage", voltage);
                                            if (health != null) dailyData.put("battery_health", health);
                                        }

                                        // Power consumption
                                        if (powerSnapshot.exists()) {
                                            Integer power = powerSnapshot.getValue(Integer.class);
                                            if (power != null) dailyData.put("power_consumption", power);
                                        }

                                        // Trap fullness
                                        if (trapSnapshot.exists()) {
                                            Integer trap = trapSnapshot.getValue(Integer.class);
                                            if (trap != null) dailyData.put("trap_fullness", trap);
                                        }

                                        // Timestamp
                                        dailyData.put("timestamp", System.currentTimeMillis() / 1000);

                                        if (!dailyData.isEmpty()) {
                                            dailyLogsRef.child(todayKey).setValue(dailyData);
                                        }
                                    }

                                    @Override
                                    public void onCancelled(@NonNull DatabaseError error) {}
                                });
                            }

                            @Override
                            public void onCancelled(@NonNull DatabaseError error) {}
                        });
                    }

                    @Override
                    public void onCancelled(@NonNull DatabaseError error) {}
                });
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    // ─── WEEKLY DATA (BAR CHARTS + DAILY REPORT) ────────────────────────

    /**
     * Load data 7 hari terakhir dari daily_logs.
     * Mengisi barChartPest (trap fullness per hari) dan barChartHistory (battery percent per hari).
     * Juga mengisi laporan harian.
     */
    private void loadWeeklyData() {
        // Dapatkan key tanggal 7 hari terakhir
        List<String> dateKeys = getLast7DaysKeys();

        dailyLogsListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (!isAdded()) return;

                float[] trapData = new float[7];
                float[] batteryData = new float[7];
                String[] dayLabels = new String[7];
                List<DailyReportItem> reportItems = new ArrayList<>();

                Calendar cal = Calendar.getInstance();
                cal.add(Calendar.DAY_OF_YEAR, -6); // 6 hari lalu

                for (int i = 0; i < 7; i++) {
                    String dateKey = dateKeys.get(i);
                    int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
                    // Convert Calendar.DAY_OF_WEEK (Sunday=1) ke index 0=Senin
                    int dayIndex = dayOfWeek - 2;
                    if (dayIndex < 0) dayIndex = 6;
                    dayLabels[i] = DAYS[dayIndex];

                    DataSnapshot daySnapshot = snapshot.child(dateKey);
                    if (daySnapshot.exists()) {
                        Integer trapFullness = daySnapshot.child("trap_fullness").getValue(Integer.class);
                        Integer battPercent = daySnapshot.child("battery_percent").getValue(Integer.class);

                        trapData[i] = trapFullness != null ? trapFullness : 0f;
                        batteryData[i] = battPercent != null ? battPercent : 0f;

                        reportItems.add(new DailyReportItem(
                                DAYS_FULL[dayIndex],
                                (int) trapData[i],
                                dateKey
                        ));
                    } else {
                        trapData[i] = 0f;
                        batteryData[i] = 0f;
                    }

                    cal.add(Calendar.DAY_OF_YEAR, 1);
                }

                // Update Bar Chart Perangkap (tab Perangkap)
                setupBarChartPest(trapData, dayLabels);

                // Update Bar Chart History (tab Baterai) jika sudah diinisialisasi
                if (batteryChartsInitialized) {
                    setupBarChartHistory(batteryData, dayLabels);
                }

                // Simpan data untuk lazy-load tab baterai
                cachedBatteryData = batteryData;
                cachedDayLabels = dayLabels;

                // Populate laporan harian
                populateDailyReport(reportItems);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Gagal memuat data mingguan", Toast.LENGTH_SHORT).show();
                }
            }
        };
        dailyLogsRef.addValueEventListener(dailyLogsListener);
    }

    // Cache untuk lazy-load tab baterai
    private float[] cachedBatteryData;
    private String[] cachedDayLabels;

    // ─── DAILY REPORT UI BUILDER ────────────────────────────────────────

    /**
     * Model data untuk item laporan harian.
     */
    private static class DailyReportItem {
        String dayName;
        int trapFullnessPercent;
        String dateKey;

        DailyReportItem(String dayName, int trapFullnessPercent, String dateKey) {
            this.dayName = dayName;
            this.trapFullnessPercent = trapFullnessPercent;
            this.dateKey = dateKey;
        }
    }

    /**
     * Mengisi container laporan harian secara programmatic.
     * Item diurutkan dari persentase tertinggi ke terendah.
     */
    private void populateDailyReport(List<DailyReportItem> items) {
        if (layoutDailyItems == null || getContext() == null) return;
        layoutDailyItems.removeAllViews();

        // Urutkan dari tertinggi ke terendah
        Collections.sort(items, (a, b) -> Integer.compare(b.trapFullnessPercent, a.trapFullnessPercent));

        // Maksimal tampilkan 7 item
        int maxItems = Math.min(items.size(), 7);

        // Cari nilai maksimal untuk normalisasi progress bar
        int maxValue = 1;
        for (DailyReportItem item : items) {
            if (item.trapFullnessPercent > maxValue) maxValue = item.trapFullnessPercent;
        }

        for (int i = 0; i < maxItems; i++) {
            DailyReportItem item = items.get(i);
            View itemView = createDailyReportItemView(i + 1, item, maxValue);
            layoutDailyItems.addView(itemView);
        }

        // Jika tidak ada data, tampilkan placeholder
        if (maxItems == 0) {
            TextView emptyText = new TextView(getContext());
            emptyText.setText("Belum ada data harian");
            emptyText.setTextColor(Color.parseColor("#9FA8DA"));
            emptyText.setTextSize(14f);
            emptyText.setGravity(Gravity.CENTER);
            emptyText.setPadding(0, dpToPx(24), 0, dpToPx(24));
            layoutDailyItems.addView(emptyText);
        }
    }

    /**
     * Membuat View item laporan harian (ranking badge, nama hari, persentase, progress bar).
     */
    private View createDailyReportItemView(int rank, DailyReportItem item, int maxValue) {
        // Container horizontal
        LinearLayout container = new LinearLayout(getContext());
        container.setOrientation(LinearLayout.HORIZONTAL);
        container.setGravity(Gravity.CENTER_VERTICAL);
        container.setPadding(0, dpToPx(12), 0, dpToPx(12));
        container.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        // Ranking badge (lingkaran dengan angka)
        TextView tvRank = new TextView(getContext());
        LinearLayout.LayoutParams rankParams = new LinearLayout.LayoutParams(dpToPx(32), dpToPx(32));
        tvRank.setLayoutParams(rankParams);
        tvRank.setBackgroundResource(R.drawable.bg_circle_soft);
        tvRank.setText(String.valueOf(rank));
        tvRank.setGravity(Gravity.CENTER);
        tvRank.setTypeface(null, android.graphics.Typeface.BOLD);

        if (rank == 1) {
            tvRank.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#3F51B5")));
            tvRank.setTextColor(Color.WHITE);
        } else {
            tvRank.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#E8EAF6")));
            tvRank.setTextColor(Color.parseColor("#191970"));
        }
        container.addView(tvRank);

        // Content (nama hari + progress bar)
        LinearLayout content = new LinearLayout(getContext());
        content.setOrientation(LinearLayout.VERTICAL);
        LinearLayout.LayoutParams contentParams = new LinearLayout.LayoutParams(0, LinearLayout.LayoutParams.WRAP_CONTENT, 1f);
        contentParams.setMarginStart(dpToPx(16));
        content.setLayoutParams(contentParams);

        // Row: nama hari + persentase
        RelativeLayout row = new RelativeLayout(getContext());
        row.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT));

        TextView tvDay = new TextView(getContext());
        tvDay.setText(item.dayName);
        tvDay.setTextColor(Color.parseColor("#191970"));
        tvDay.setTextSize(16f);
        tvDay.setTypeface(null, android.graphics.Typeface.BOLD);
        RelativeLayout.LayoutParams dayParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        tvDay.setLayoutParams(dayParams);
        row.addView(tvDay);

        TextView tvPercent = new TextView(getContext());
        tvPercent.setText(item.trapFullnessPercent + "%");
        tvPercent.setTextColor(Color.parseColor("#191970"));
        tvPercent.setTextSize(16f);
        tvPercent.setTypeface(null, android.graphics.Typeface.BOLD);
        RelativeLayout.LayoutParams percentParams = new RelativeLayout.LayoutParams(
                RelativeLayout.LayoutParams.WRAP_CONTENT,
                RelativeLayout.LayoutParams.WRAP_CONTENT);
        percentParams.addRule(RelativeLayout.ALIGN_PARENT_END);
        tvPercent.setLayoutParams(percentParams);
        row.addView(tvPercent);

        content.addView(row);

        // Progress bar
        ProgressBar progressBar = new ProgressBar(getContext(), null, android.R.attr.progressBarStyleHorizontal);
        LinearLayout.LayoutParams pbParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT, dpToPx(8));
        pbParams.topMargin = dpToPx(8);
        progressBar.setLayoutParams(pbParams);
        progressBar.setProgressDrawable(getResources().getDrawable(R.drawable.bg_progress_fill_blue, null));
        progressBar.setMax(maxValue);
        progressBar.setProgress(item.trapFullnessPercent);

        // Alpha berdasarkan ranking (item pertama paling terang)
        float alpha = 1.0f - (rank - 1) * 0.12f;
        progressBar.setAlpha(Math.max(0.3f, alpha));

        content.addView(progressBar);
        container.addView(content);

        return container;
    }

    // ─── DATE HELPERS ───────────────────────────────────────────────────

    /**
     * Mendapatkan key tanggal hari ini (format: yyyy-MM-dd).
     */
    private String getTodayDateKey() {
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        return sdf.format(Calendar.getInstance().getTime());
    }

    /**
     * Mendapatkan daftar key tanggal 7 hari terakhir (termasuk hari ini).
     */
    private List<String> getLast7DaysKeys() {
        List<String> keys = new ArrayList<>();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_YEAR, -6); // Mulai dari 6 hari lalu

        for (int i = 0; i < 7; i++) {
            keys.add(sdf.format(cal.getTime()));
            cal.add(Calendar.DAY_OF_YEAR, 1);
        }
        return keys;
    }

    /**
     * Update label tanggal pada header "LAPORAN MINGGUAN".
     */
    private void updateWeeklyDateLabel() {
        if (tvWeeklyDate == null) return;
        SimpleDateFormat sdf = new SimpleDateFormat("EEEE, d MMMM yyyy", new Locale("id", "ID"));
        String dateStr = sdf.format(Calendar.getInstance().getTime()).toUpperCase();
        tvWeeklyDate.setText(dateStr);
    }

    // ─── CHART SETUP ────────────────────────────────────────────────────

    /**
     * Setup bar chart tangkapan hama mingguan (tab Perangkap) — dengan data dinamis.
     */
    private void setupBarChartPest(float[] data) {
        setupBarChartPest(data, DAYS);
    }

    private void setupBarChartPest(float[] data, String[] labels) {
        ArrayList<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < data.length; i++) {
            entries.add(new BarEntry(i, data[i]));
        }

        BarDataSet dataSet = new BarDataSet(entries, "Weekly Catch");
        dataSet.setColor(Color.parseColor("#5C6BC0"));
        dataSet.setDrawValues(true);
        dataSet.setValueTextColor(Color.parseColor("#191970"));
        dataSet.setValueTextSize(10f);
        dataSet.setValueFormatter(new com.github.mikephil.charting.formatter.ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return (int) value + "%";
            }
        });

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.5f);
        barChartPest.setData(barData);

        styleBarChart(barChartPest, labels);
    }

    /**
     * Setup pie chart kapasitas baterai (tab Baterai) — dengan data dinamis.
     */
    private void setupPieChart(int batteryPercent) {
        ArrayList<PieEntry> entries = new ArrayList<>();

        // Bagian terisi (baterai)
        float filled = batteryPercent;
        float empty = 100 - batteryPercent;

        // Membagi lingkaran menjadi beberapa bagian putus-putus
        int segments = 4;
        float filledPerSegment = filled / segments;
        float emptyPerSegment = empty / segments;

        for (int i = 0; i < segments; i++) {
            entries.add(new PieEntry(filledPerSegment, ""));
        }

        PieDataSet dataSet = new PieDataSet(entries, "");

        // Warna berdasarkan level baterai
        int sliceColor;
        if (batteryPercent >= 60) {
            sliceColor = Color.parseColor("#5C6BC0"); // Indigo
        } else if (batteryPercent >= 30) {
            sliceColor = Color.parseColor("#FFA000"); // Amber
        } else {
            sliceColor = Color.parseColor("#F44336"); // Red
        }

        int[] colors = new int[segments];
        for (int i = 0; i < segments; i++) {
            colors[i] = sliceColor;
        }
        dataSet.setColors(colors);
        dataSet.setSliceSpace(8f);
        dataSet.setDrawValues(false);

        PieData data = new PieData(dataSet);

        pieChart.setData(data);
        pieChart.setHoleRadius(75f);
        pieChart.setTransparentCircleRadius(80f);
        pieChart.setTransparentCircleColor(Color.parseColor("#E8F5E9"));
        pieChart.setTransparentCircleAlpha(110);
        pieChart.setCenterText(batteryPercent + "%\nTerisi");
        pieChart.setCenterTextSize(28f);
        pieChart.setCenterTextColor(Color.parseColor("#191970"));
        pieChart.setDrawEntryLabels(false);
        pieChart.getDescription().setEnabled(false);
        pieChart.getLegend().setEnabled(false);
        pieChart.setTouchEnabled(false);
        pieChart.animateY(1000);
        pieChart.invalidate();
    }

    /**
     * Setup bar chart riwayat pengisian baterai (tab Baterai) — dengan data dinamis.
     */
    private void setupBarChartHistory(float[] data, String[] labels) {
        ArrayList<BarEntry> entries = new ArrayList<>();
        for (int i = 0; i < data.length; i++) {
            entries.add(new BarEntry(i, data[i]));
        }

        BarDataSet dataSet = new BarDataSet(entries, "History");
        dataSet.setColor(Color.parseColor("#5C6BC0"));
        dataSet.setDrawValues(true);
        dataSet.setValueTextColor(Color.parseColor("#191970"));
        dataSet.setValueTextSize(10f);
        dataSet.setValueFormatter(new com.github.mikephil.charting.formatter.ValueFormatter() {
            @Override
            public String getFormattedValue(float value) {
                return (int) value + "%";
            }
        });

        BarData barData = new BarData(dataSet);
        barData.setBarWidth(0.5f);
        barChartHistory.setData(barData);

        styleBarChart(barChartHistory, labels);
    }

    /**
     * Konfigurasi styling bersama untuk bar chart (X-axis, Y-axis, legend, grid, animasi).
     * Menghilangkan duplikasi kode antara setupBarChartPest() dan setupBarChartHistory().
     */
    private void styleBarChart(BarChart chart) {
        styleBarChart(chart, DAYS);
    }

    private void styleBarChart(BarChart chart, String[] labels) {
        // Styling X-Axis
        XAxis xAxis = chart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(false);
        xAxis.setTextColor(Color.parseColor("#7986CB"));
        xAxis.setTextSize(9f);
        xAxis.setValueFormatter(new IndexAxisValueFormatter(labels));
        xAxis.setLabelCount(7);
        xAxis.setYOffset(10f);

        // Styling Y-Axis Kiri
        YAxis leftAxis = chart.getAxisLeft();
        leftAxis.setDrawAxisLine(false);
        leftAxis.setGridColor(Color.parseColor("#E8EAF6"));
        leftAxis.setGridLineWidth(1f);
        leftAxis.enableGridDashedLine(10f, 10f, 0f);
        leftAxis.setTextColor(Color.parseColor("#9FA8DA"));
        leftAxis.setTextSize(9f);
        leftAxis.setAxisMinimum(0f);
        leftAxis.setAxisMaximum(120f);
        leftAxis.setLabelCount(5);

        // Sembunyikan Y-Axis Kanan & dekorasi lainnya
        chart.getAxisRight().setEnabled(false);
        chart.getDescription().setEnabled(false);
        chart.getLegend().setEnabled(false);
        chart.setDrawGridBackground(false);
        chart.setExtraBottomOffset(20f);
        chart.animateY(1000);
        chart.invalidate();
    }

    // ─── TAB SWITCHING ──────────────────────────────────────────────────

    /**
     * Setup tab switching dengan transisi animasi.
     */
    private void setupTabSwitching(View view) {
        View tabTrap = view.findViewById(R.id.tabTrapAnalytics);
        View tabBattery = view.findViewById(R.id.tabBatteryAnalytics);
        View layoutTrapContent = view.findViewById(R.id.layoutTrapAnalyticsContent);
        View layoutBatteryContent = view.findViewById(R.id.layoutBatteryAnalyticsContent);

        ImageView ivTabTrap = view.findViewById(R.id.ivTabTrapAnal);
        TextView tvTabTrap = view.findViewById(R.id.tvTabTrapAnal);
        ImageView ivTabBatt = view.findViewById(R.id.ivTabBattAnal);
        TextView tvTabBatt = view.findViewById(R.id.tvTabBattAnal);

        TextView tvTitle = view.findViewById(R.id.tvAnalyticsTitle);

        tabTrap.setOnClickListener(v -> {
            android.transition.TransitionManager.beginDelayedTransition((ViewGroup) view);
            tvTitle.setText("ANALISIS PERANGKAP");
            layoutTrapContent.setVisibility(View.VISIBLE);
            layoutBatteryContent.setVisibility(View.GONE);

            tabTrap.setBackgroundResource(R.drawable.bg_tab_active);
            tabBattery.setBackground(null);
            ivTabTrap.setColorFilter(Color.WHITE);
            tvTabTrap.setTextColor(Color.WHITE);
            ivTabBatt.setColorFilter(Color.parseColor("#191970"));
            tvTabBatt.setTextColor(Color.parseColor("#191970"));
        });

        tabBattery.setOnClickListener(v -> {
            android.transition.TransitionManager.beginDelayedTransition((ViewGroup) view);
            tvTitle.setText("ANALISIS BATERAI");
            layoutTrapContent.setVisibility(View.GONE);
            layoutBatteryContent.setVisibility(View.VISIBLE);

            tabBattery.setBackgroundResource(R.drawable.bg_tab_active);
            tabTrap.setBackground(null);
            ivTabBatt.setColorFilter(Color.WHITE);
            tvTabBatt.setTextColor(Color.WHITE);
            ivTabTrap.setColorFilter(Color.parseColor("#191970"));
            tvTabTrap.setTextColor(Color.parseColor("#191970"));

            // Lazy-load: setup chart baterai hanya saat pertama kali tab diklik
            if (!batteryChartsInitialized) {
                setupPieChart(currentBatteryPercent);
                if (cachedBatteryData != null && cachedDayLabels != null) {
                    setupBarChartHistory(cachedBatteryData, cachedDayLabels);
                } else {
                    setupBarChartHistory(new float[]{0, 0, 0, 0, 0, 0, 0}, DAYS);
                }
                batteryChartsInitialized = true;
            }
        });
    }

    // ─── TOMBOL KEMBALI ─────────────────────────────────────────────────

    /**
     * Setup tombol kembali ke home.
     */
    private void setupBackButton(View view) {
        View btnBack = view.findViewById(R.id.btnBackAnalytics);
        btnBack.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeDashboardFragment())
                    .commit();
        });
    }

    // ─── UTILITY ────────────────────────────────────────────────────────

    /**
     * Konversi dp ke pixel.
     */
    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    // ─── LIFECYCLE CLEANUP ──────────────────────────────────────────────

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Hapus semua Firebase listeners untuk menghindari memory leak
        if (batteryRef != null && batteryListener != null) {
            batteryRef.removeEventListener(batteryListener);
        }
        if (powerRef != null && powerListener != null) {
            powerRef.removeEventListener(powerListener);
        }
        if (trapRef != null && trapListener != null) {
            trapRef.removeEventListener(trapListener);
        }
        if (dailyLogsRef != null && dailyLogsListener != null) {
            dailyLogsRef.removeEventListener(dailyLogsListener);
        }
    }
}
