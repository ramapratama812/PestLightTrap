package com.pebelti.pestlighttrap;

import android.app.TimePickerDialog;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
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
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Calendar;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HomeDashboardFragment extends Fragment {

    // ESP32-CAM Configuration
    private static final String ESP_CAM_CAPTURE_URL = "http://10.156.99.169/capture";
    private static final int CAM_REFRESH_INTERVAL_MS = 5000; // Refresh setiap 5 detik

    private FrameLayout btnToggleDevice;
    private TextView tvStatus;
    private View statusIndicator;
    private TextView tvToggleBadge;
    private View viewNotificationDot;
    private boolean isDeviceOn = true;
    private boolean isAutoMode = true;

    private String startTime = "18:00";
    private String endTime = "06:00";

    // Firebase References
    private DatabaseReference deviceRef;
    private DatabaseReference operationRef;
    private DatabaseReference batteryRef;
    private DatabaseReference trapRef;
    private DatabaseReference powerRef;
    private DatabaseReference autoModeRef;

    private TextView[] days;
    private TextView tvOperationTime;

    private ImageView ivProfilePicture;
    private TextView tvProfileName;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private DatabaseReference userRef;

    // ESP32-CAM views
    private ImageView ivEspCamCapture;
    private ProgressBar pbCamLoading;
    private ImageView btnRefreshCam;
    private Handler camHandler;
    private Runnable camRunnable;
    private ExecutorService camExecutor;

    // Activity tracker views
    private TextView tvActivityTimeRemaining;
    private TextView tvActivityPeriodLabel;
    private View viewActivityProgressFill;
    private TextView tvActivityStartTime;
    private TextView tvActivityEndTime;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home_dashboard, container, false);

        btnToggleDevice = view.findViewById(R.id.btnToggleDevice);
        tvStatus = view.findViewById(R.id.tvDeviceStatus);
        statusIndicator = view.findViewById(R.id.statusIndicator);
        tvToggleBadge = view.findViewById(R.id.tvToggleBadge);
        viewNotificationDot = view.findViewById(R.id.viewNotificationDot);
        View btnNotifications = view.findViewById(R.id.btnNotifications);

        // Tampilkan/sembunyikan dot merah bell sesuai status baca
        refreshNotificationDot();

        ivProfilePicture = view.findViewById(R.id.ivProfilePicture);
        tvProfileName = view.findViewById(R.id.tvProfileName);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            updateProfileUI();
            userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUser.getUid());
            loadProfilePicture();
        }

        // Inisialisasi Firebase
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        deviceRef = database.getReference("smart_pest_trap/status");
        operationRef = database.getReference("smart_pest_trap/operation_mode");
        batteryRef = database.getReference("smart_pest_trap/battery");
        trapRef = database.getReference("smart_pest_trap/trap_fullness");
        powerRef = database.getReference("smart_pest_trap/power_consumption");
        autoModeRef = database.getReference("smart_pest_trap/auto_mode"); // nyesuain referensi realtime database

        // Baca status dari Firebase
        deviceRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Boolean isOn = snapshot.getValue(Boolean.class);
                    if (isOn != null) {
                        isDeviceOn = isOn;
                        updateDeviceUI(isDeviceOn);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                if (getContext() != null) {
                    Toast.makeText(getContext(), "Gagal membaca status alat", Toast.LENGTH_SHORT).show();
                }
            }
        });

        // Baca mode otomatis dari Firebase
        autoModeRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Boolean isAuto = snapshot.getValue(Boolean.class);
                    if (isAuto != null) {
                        isAutoMode = isAuto;
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

        btnNotifications.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new NotificationsFragment())
                    .addToBackStack(null)
                    .commit();
        });

        // Set today's date dynamically for the history card
        TextView tvHistoryItemDate = view.findViewById(R.id.tvHistoryItemDate);
        TextView tvHistoryBadge = view.findViewById(R.id.tvHistoryBadge);
        Calendar historyCalendar = Calendar.getInstance();

        if (tvHistoryItemDate != null) {
            java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("EEEE, d MMM",
                    new java.util.Locale("id", "ID"));
            String todayStr = "• " + dateFormat.format(historyCalendar.getTime()).toUpperCase();
            tvHistoryItemDate.setText(todayStr);
        }
        if (tvHistoryBadge != null) {
            java.text.SimpleDateFormat badgeFormat = new java.text.SimpleDateFormat("d MMM",
                    new java.util.Locale("id", "ID"));
            String badgeStr = badgeFormat.format(historyCalendar.getTime()).toUpperCase();
            tvHistoryBadge.setText(badgeStr);
        }

        // Click listener for Selengkapnya button and Card
        View btnMoreHistory = view.findViewById(R.id.btnMoreHistory);
        if (btnMoreHistory != null) {
            btnMoreHistory.setOnClickListener(v -> {
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new HistoryDetailFragment())
                        .addToBackStack(null)
                        .commit();
            });
        }

        View cardTrapHistory = view.findViewById(R.id.cardTrapHistory);
        if (cardTrapHistory != null) {
            cardTrapHistory.setOnClickListener(v -> {
                getParentFragmentManager().beginTransaction()
                        .replace(R.id.fragment_container, new HistoryDetailFragment())
                        .addToBackStack(null)
                        .commit();
            });
        }

        // Klik tombol toggle -> Update ke Firebase
        btnToggleDevice.setOnClickListener(v -> {
            if (isAutoMode) {
                if (getContext() != null) {
                    Toast.makeText(getContext(),
                            "MODE OTOMATIS AKTIF! SILAKAN NONAKTIFKAN DI PENGATURAN UNTUK KONTROL MANUAL.",
                            Toast.LENGTH_LONG).show();
                }
            } else {
                deviceRef.setValue(!isDeviceOn);
            }
        });

        setupModeOperasi(view);
        setupClock(view);
        loadSolarData(view);
        loadTrapCondition(view);
        loadPowerDraw(view);
        loadOperationMode();
        setupEspCam(view);
        setupActivityTracker(view);

        return view;
    }

    private void setupClock(View view) {
        TextView tvDate = view.findViewById(R.id.tvDate);
        TextView tvTime = view.findViewById(R.id.tvTime);
        TextView tvAmPm = view.findViewById(R.id.tvAmPm);
        android.widget.ImageView ivDayNight = view.findViewById(R.id.ivDayNight);
        View layoutDayNight = view.findViewById(R.id.layoutDayNight);

        android.os.Handler handler = new android.os.Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                Calendar calendar = Calendar.getInstance();

                java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("EEEE, dd MMMM yyyy",
                        new java.util.Locale("id", "ID"));
                String dateStr = dateFormat.format(calendar.getTime()).toUpperCase();
                tvDate.setText(dateStr);

                int hour = calendar.get(Calendar.HOUR_OF_DAY);
                int minute = calendar.get(Calendar.MINUTE);
                String timeStr = String.format(Locale.getDefault(), "%02d:%02d", hour, minute);
                tvTime.setText(timeStr);

                String amPm = calendar.get(Calendar.AM_PM) == Calendar.AM ? "AM" : "PM";
                tvAmPm.setText(amPm);

                if (hour >= 6 && hour < 18) {
                    ivDayNight.setImageResource(R.drawable.ic_sun);
                    ivDayNight.setColorFilter(Color.parseColor("#FFD54F"));
                    layoutDayNight.getBackground().mutate().setTint(Color.parseColor("#FFF3E0"));
                } else {
                    ivDayNight.setImageResource(R.drawable.ic_moon);
                    ivDayNight.setColorFilter(Color.parseColor("#FFD54F"));
                    layoutDayNight.getBackground().mutate().setTint(Color.parseColor("#191970"));
                }

                // Terapkan logika mode otomatis
                checkAndApplyAutoMode(hour, minute, calendar.get(Calendar.DAY_OF_WEEK));

                // Update activity tracker
                updateActivityTracker(hour, minute);

                handler.postDelayed(this, 10000);
            }
        };
        handler.post(runnable);
    }

    private void updateDeviceUI(boolean isOn) {
        if (isOn) {
            tvStatus.setText("AKTIF");
            tvStatus.setTextColor(Color.parseColor("#11df11")); // Bright Green
            statusIndicator.setBackgroundResource(R.drawable.dot_green); // Green dot
            tvToggleBadge.setText("NYALA");
            tvToggleBadge.setBackgroundResource(R.drawable.bg_pill_soft); // Purple Background
            tvToggleBadge.setTextColor(Color.WHITE); // White Font
        } else {
            tvStatus.setText("TIDAK AKTIF");
            tvStatus.setTextColor(Color.parseColor("#F44336")); // Red
            statusIndicator.setBackgroundResource(R.drawable.dot_red);
            tvToggleBadge.setText("MATI");
            tvToggleBadge.setBackgroundResource(R.drawable.bg_badge_red);
            tvToggleBadge.setTextColor(Color.parseColor("#F44336"));
        }
    }

    private void checkAndApplyAutoMode(int currentHour, int currentMinute, int currentDayOfWeek) {
        if (!isAutoMode || days == null)
            return;

        // Konversi currentDayOfWeek (Calendar.SUNDAY=1, MONDAY=2, dst) ke index 0-6
        // (Senin=0, Minggu=6)
        int dayIndex = currentDayOfWeek - 2;
        if (dayIndex < 0)
            dayIndex = 6;

        boolean isDayActive = false;
        if (days[dayIndex] != null && days[dayIndex].getTag() != null) {
            isDayActive = (boolean) days[dayIndex].getTag();
        }

        if (!isDayActive) {
            // Jika hari ini tidak aktif dalam jadwal, pastikan perangkat mati
            if (isDeviceOn)
                deviceRef.setValue(false);
            return;
        }

        // Parse startTime dan endTime
        int startH = 18, startM = 0, endH = 6, endM = 0;
        try {
            String[] s = startTime.split(":");
            startH = Integer.parseInt(s[0]);
            startM = Integer.parseInt(s[1]);

            String[] e = endTime.split(":");
            endH = Integer.parseInt(e[0]);
            endM = Integer.parseInt(e[1]);
        } catch (Exception ignored) {
        }

        int currentMins = currentHour * 60 + currentMinute;
        int startMins = startH * 60 + startM;
        int endMins = endH * 60 + endM;

        boolean shouldBeOn = false;
        if (startMins <= endMins) {
            // Contoh jadwal: 08:00 ke 17:00
            shouldBeOn = currentMins >= startMins && currentMins < endMins;
        } else {
            // Contoh jadwal melewati tengah malam: 18:00 ke 06:00
            shouldBeOn = currentMins >= startMins || currentMins < endMins;
        }

        if (isDeviceOn != shouldBeOn) {
            deviceRef.setValue(shouldBeOn);
        }
    }

    private void setupModeOperasi(View view) {
        tvOperationTime = view.findViewById(R.id.tvOperationTime);
        TextView btnEditOperation = view.findViewById(R.id.btnEditOperation);

        days = new TextView[] {
                view.findViewById(R.id.daySenin),
                view.findViewById(R.id.daySelasa),
                view.findViewById(R.id.dayRabu),
                view.findViewById(R.id.dayKamis),
                view.findViewById(R.id.dayJumat),
                view.findViewById(R.id.daySabtu),
                view.findViewById(R.id.dayMinggu)
        };

        // Initialize tags and listeners for the days
        for (int i = 0; i < days.length; i++) {
            final TextView day = days[i];
            day.setTag(i < 5); // Default Mon-Fri active

            day.setOnClickListener(v -> {
                boolean isActive = (boolean) day.getTag();
                if (isActive) {
                    day.setBackgroundResource(R.drawable.bg_circle_soft);
                    day.setTag(false);
                } else {
                    day.setBackgroundResource(R.drawable.bg_power_button);
                    day.setTag(true);
                }
                saveOperationMode();
            });
        }

        btnEditOperation.setOnClickListener(v -> {
            showTimeRangePicker(tvOperationTime);
        });
    }

    private void saveOperationMode() {
        if (operationRef == null)
            return;
        StringBuilder daysStr = new StringBuilder();
        for (int i = 0; i < days.length; i++) {
            if ((boolean) days[i].getTag()) {
                daysStr.append(i).append(",");
            }
        }
        operationRef.child("days").setValue(daysStr.toString());
        operationRef.child("start_time").setValue(startTime);
        operationRef.child("end_time").setValue(endTime);
    }

    private void loadOperationMode() {
        if (operationRef == null)
            return;
        operationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    String daysStr = snapshot.child("days").getValue(String.class);
                    String start = snapshot.child("start_time").getValue(String.class);
                    String end = snapshot.child("end_time").getValue(String.class);

                    if (daysStr != null) {
                        for (int i = 0; i < days.length; i++) {
                            if (daysStr.contains(String.valueOf(i))) {
                                days[i].setBackgroundResource(R.drawable.bg_power_button);
                                days[i].setTag(true);
                            } else {
                                days[i].setBackgroundResource(R.drawable.bg_circle_soft);
                                days[i].setTag(false);
                            }
                        }
                    }
                    if (start != null && end != null) {
                        startTime = start;
                        endTime = end;
                        tvOperationTime.setText(startTime + " — " + endTime);

                        // Update activity tracker labels
                        if (tvActivityStartTime != null)
                            tvActivityStartTime.setText(startTime);
                        if (tvActivityEndTime != null)
                            tvActivityEndTime.setText(endTime);

                        // Refresh tracker immediately
                        Calendar cal = Calendar.getInstance();
                        updateActivityTracker(cal.get(Calendar.HOUR_OF_DAY), cal.get(Calendar.MINUTE));
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void showTimeRangePicker(TextView tvTime) {
        // Pick Start Time
        TimePickerDialog startTimePicker = new TimePickerDialog(getContext(), (view, hourOfDay, minute) -> {
            startTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay, minute);

            // Pick End Time after Start Time is selected
            TimePickerDialog endTimePicker = new TimePickerDialog(getContext(), (view1, hourOfDay1, minute1) -> {
                endTime = String.format(Locale.getDefault(), "%02d:%02d", hourOfDay1, minute1);
                tvTime.setText(startTime + " — " + endTime);
                saveOperationMode();
            }, 6, 0, true);
            endTimePicker.setTitle("Pilih Jam Selesai");
            endTimePicker.show();

        }, 18, 0, true);
        startTimePicker.setTitle("Pilih Jam Mulai");
        startTimePicker.show();
    }

    private void loadSolarData(View view) {
        TextView tvSolarStatus = view.findViewById(R.id.tvSolarStatus);
        TextView tvSolarPercent = view.findViewById(R.id.tvSolarPercent);

        if (batteryRef == null)
            return;
        batteryRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Integer percent = snapshot.child("percent").getValue(Integer.class);
                    if (percent != null) {
                        tvSolarPercent.setText(percent + "%");

                        if (percent >= 80) {
                            tvSolarStatus.setText("DAYA PENUH");
                            tvSolarStatus.setTextColor(Color.parseColor("#4CAF50")); // Green
                        } else if (percent >= 40) {
                            tvSolarStatus.setText("DAYA SETENGAH");
                            tvSolarStatus.setTextColor(Color.parseColor("#FFA000")); // Yellow/Amber
                        } else {
                            tvSolarStatus.setText("DAYA HABIS");
                            tvSolarStatus.setTextColor(Color.parseColor("#F44336")); // Red
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void loadTrapCondition(View view) {
        TextView tvTrapPercent = view.findViewById(R.id.tvTrapPercent);
        TextView tvTrapStatus = view.findViewById(R.id.tvTrapStatus);
        ProgressBar pbTrapCircular = view.findViewById(R.id.pbTrapCircular);

        if (trapRef == null)
            return;
        trapRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Integer percent = snapshot.getValue(Integer.class);
                    if (percent != null) {
                        tvTrapPercent.setText(percent + "%");

                        // Animasi circular progress bar
                        if (pbTrapCircular != null) {
                            android.animation.ObjectAnimator animator = android.animation.ObjectAnimator.ofInt(
                                    pbTrapCircular, "progress", pbTrapCircular.getProgress(), percent);
                            animator.setDuration(800);
                            animator.setInterpolator(new android.view.animation.DecelerateInterpolator());
                            animator.start();
                        }

                        if (percent >= 80) {
                            tvTrapStatus.setText("PENUH");
                            tvTrapStatus.setTextColor(Color.parseColor("#F44336")); // Red
                        } else if (percent >= 50) {
                            tvTrapStatus.setText("AMAN");
                            tvTrapStatus.setTextColor(Color.parseColor("#FFA000")); // Yellow
                        } else {
                            tvTrapStatus.setText("KOSONG");
                            tvTrapStatus.setTextColor(Color.parseColor("#4CAF50")); // Green
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void loadPowerDraw(View view) {
        TextView tvPowerValue = view.findViewById(R.id.tvPowerValue);
        TextView tvPowerStatus = view.findViewById(R.id.tvPowerStatus);

        if (powerRef == null)
            return;
        powerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Integer value = snapshot.getValue(Integer.class);
                    if (value != null) {
                        tvPowerValue.setText(String.valueOf(value));

                        if (value >= 60) {
                            tvPowerStatus.setText("KONSUMSI DAYA NORMAL");
                            tvPowerStatus.setTextColor(Color.parseColor("#4CAF50")); // Green
                        } else {
                            tvPowerStatus.setText("KONSUMSI DAYA RENDAH");
                            tvPowerStatus.setTextColor(Color.parseColor("#7986CB")); // Light Blue/Indigo
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    // ─── ACTIVITY TRACKER (WAKTU OPERASI) ──────────────────────────────────

    private void setupActivityTracker(View view) {
        tvActivityTimeRemaining = view.findViewById(R.id.tvActivityTimeRemaining);
        tvActivityPeriodLabel = view.findViewById(R.id.tvActivityPeriodLabel);
        viewActivityProgressFill = view.findViewById(R.id.viewActivityProgressFill);
        tvActivityStartTime = view.findViewById(R.id.tvActivityStartTime);
        tvActivityEndTime = view.findViewById(R.id.tvActivityEndTime);

        // Set initial labels
        if (tvActivityStartTime != null)
            tvActivityStartTime.setText(startTime);
        if (tvActivityEndTime != null)
            tvActivityEndTime.setText(endTime);
    }

    private void updateActivityTracker(int currentHour, int currentMinute) {
        if (tvActivityTimeRemaining == null || tvActivityPeriodLabel == null || viewActivityProgressFill == null)
            return;

        // Parse startTime dan endTime
        int startH = 18, startM = 0, endH = 6, endM = 0;
        try {
            String[] s = startTime.split(":");
            startH = Integer.parseInt(s[0]);
            startM = Integer.parseInt(s[1]);

            String[] e = endTime.split(":");
            endH = Integer.parseInt(e[0]);
            endM = Integer.parseInt(e[1]);
        } catch (Exception ignored) {
        }

        int startMins = startH * 60 + startM;
        int endMins = endH * 60 + endM;
        int currentMins = currentHour * 60 + currentMinute;

        // Hitung total durasi operasi (dalam menit)
        int totalDuration;
        if (startMins <= endMins) {
            // Jadwal dalam hari yang sama, misal 06:00 — 18:00
            totalDuration = endMins - startMins;
        } else {
            // Jadwal melewati tengah malam, misal 18:00 — 06:00
            totalDuration = (24 * 60 - startMins) + endMins;
        }

        if (totalDuration <= 0)
            totalDuration = 1; // Hindari division by zero

        // Cek apakah sekarang berada dalam jadwal operasi
        boolean isInSchedule;
        if (startMins <= endMins) {
            isInSchedule = currentMins >= startMins && currentMins < endMins;
        } else {
            isInSchedule = currentMins >= startMins || currentMins < endMins;
        }

        int elapsedMins;
        int remainingMins;

        if (isInSchedule) {
            // Hitung waktu yang sudah berjalan
            if (startMins <= endMins) {
                elapsedMins = currentMins - startMins;
            } else {
                if (currentMins >= startMins) {
                    elapsedMins = currentMins - startMins;
                } else {
                    elapsedMins = (24 * 60 - startMins) + currentMins;
                }
            }
            remainingMins = totalDuration - elapsedMins;
        } else {
            // Di luar jadwal - hitung waktu sampai jadwal dimulai
            if (currentMins < startMins) {
                remainingMins = startMins - currentMins;
            } else {
                remainingMins = (24 * 60 - currentMins) + startMins;
            }
            elapsedMins = 0;
        }

        // Format sisa waktu
        int hours = remainingMins / 60;
        int mins = remainingMins % 60;
        String timeStr;
        if (isInSchedule) {
            timeStr = hours + "h " + mins + "m";
        } else {
            timeStr = "MULAI " + hours + "h " + mins + "m";
        }
        tvActivityTimeRemaining.setText(timeStr);

        // Set label MALAM INI / SIANG INI berdasarkan jam mulai
        if (startH >= 18 || startH < 6) {
            tvActivityPeriodLabel.setText("MALAM INI");
        } else {
            tvActivityPeriodLabel.setText("SIANG INI");
        }

        // Update label jam
        if (tvActivityStartTime != null)
            tvActivityStartTime.setText(startTime);
        if (tvActivityEndTime != null)
            tvActivityEndTime.setText(endTime);

        // Animasi progress bar
        float progress = isInSchedule ? (float) elapsedMins / totalDuration : 0f;
        progress = Math.max(0f, Math.min(1f, progress)); // Clamp 0-1

        final float finalProgress = progress;
        viewActivityProgressFill.post(() -> {
            if (viewActivityProgressFill.getParent() == null)
                return;
            int parentWidth = ((View) viewActivityProgressFill.getParent()).getWidth();
            if (parentWidth <= 0)
                return;

            int targetWidth = (int) (parentWidth * finalProgress);
            if (targetWidth < 1 && finalProgress > 0)
                targetWidth = 1;

            ViewGroup.LayoutParams params = viewActivityProgressFill.getLayoutParams();
            int currentWidth = viewActivityProgressFill.getWidth();

            // Animasi smooth dari current ke target
            android.animation.ValueAnimator animator = android.animation.ValueAnimator.ofInt(currentWidth, targetWidth);
            animator.setDuration(500);
            animator.setInterpolator(new android.view.animation.DecelerateInterpolator());
            animator.addUpdateListener(animation -> {
                if (viewActivityProgressFill == null)
                    return;
                int animatedValue = (int) animation.getAnimatedValue();
                ViewGroup.LayoutParams p = viewActivityProgressFill.getLayoutParams();
                p.width = animatedValue;
                viewActivityProgressFill.setLayoutParams(p);
            });
            animator.start();
        });
    }

    // ─── ESP32-CAM LIVE CAPTURE ───────────────────────────────────────────

    private void setupEspCam(View view) {
        ivEspCamCapture = view.findViewById(R.id.ivEspCamCapture);
        pbCamLoading = view.findViewById(R.id.pbCamLoading);
        btnRefreshCam = view.findViewById(R.id.btnRefreshCam);
        camExecutor = Executors.newSingleThreadExecutor();
        camHandler = new Handler(Looper.getMainLooper());

        // Tombol refresh manual
        if (btnRefreshCam != null) {
            btnRefreshCam.setOnClickListener(v -> loadEspCamImage());
        }

        // Mulai auto-refresh
        camRunnable = new Runnable() {
            @Override
            public void run() {
                loadEspCamImage();
                camHandler.postDelayed(this, CAM_REFRESH_INTERVAL_MS);
            }
        };
        camHandler.post(camRunnable);
    }

    private void loadEspCamImage() {
        if (!isAdded() || ivEspCamCapture == null)
            return;

        // Tampilkan loading indicator
        if (pbCamLoading != null) {
            pbCamLoading.setVisibility(View.VISIBLE);
        }

        camExecutor.execute(() -> {
            Bitmap bitmap = null;
            HttpURLConnection connection = null;
            InputStream inputStream = null;
            try {
                URL url = new URL(ESP_CAM_CAPTURE_URL);
                connection = (HttpURLConnection) url.openConnection();
                connection.setConnectTimeout(5000);
                connection.setReadTimeout(5000);
                connection.setRequestMethod("GET");
                connection.setDoInput(true);
                connection.connect();

                int responseCode = connection.getResponseCode();
                if (responseCode == HttpURLConnection.HTTP_OK) {
                    inputStream = connection.getInputStream();
                    bitmap = BitmapFactory.decodeStream(inputStream);
                }
            } catch (Exception e) {
                e.printStackTrace();
            } finally {
                try {
                    if (inputStream != null)
                        inputStream.close();
                } catch (Exception ignored) {
                }
                if (connection != null)
                    connection.disconnect();
            }

            final Bitmap finalBitmap = bitmap;
            camHandler.post(() -> {
                if (!isAdded())
                    return;

                // Sembunyikan loading indicator
                if (pbCamLoading != null) {
                    pbCamLoading.setVisibility(View.GONE);
                }

                if (finalBitmap != null && ivEspCamCapture != null) {
                    ivEspCamCapture.setImageBitmap(finalBitmap);
                }
                // Jika gagal, tetap tampilkan gambar sebelumnya (atau default)
            });
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        // Refresh dot saat kembali dari halaman notifikasi
        refreshNotificationDot();
        if (mAuth != null && mAuth.getCurrentUser() != null) {
            currentUser = mAuth.getCurrentUser();
            currentUser.reload().addOnCompleteListener(task -> {
                if (task.isSuccessful() && isAdded()) {
                    currentUser = mAuth.getCurrentUser();
                    updateProfileUI();
                }
            });
        }
    }

    private void updateProfileUI() {
        if (currentUser != null) {
            String displayName = currentUser.getDisplayName();
            if (displayName != null && !displayName.isEmpty()) {
                if (tvProfileName != null) {
                    tvProfileName.setText(displayName.toUpperCase());
                }
            } else {
                // Fallback ke email prefix jika displayName belum diatur
                String email = currentUser.getEmail();
                if (email != null && email.contains("@")) {
                    String namePart = email.split("@")[0];
                    if (tvProfileName != null) {
                        tvProfileName.setText(namePart.toUpperCase());
                    }
                }
            }
        }
    }

    private void loadProfilePicture() {
        if (userRef != null) {
            userRef.child("profilePicture").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists() && isAdded()) {
                        String base64Image = snapshot.getValue(String.class);
                        if (base64Image != null && !base64Image.isEmpty()) {
                            try {
                                byte[] decodedString = android.util.Base64.decode(base64Image,
                                        android.util.Base64.DEFAULT);
                                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0,
                                        decodedString.length);
                                if (ivProfilePicture != null) {
                                    ivProfilePicture.setImageBitmap(decodedByte);
                                    ivProfilePicture.setPadding(0, 0, 0, 0);
                                    ivProfilePicture.setBackground(null);
                                }
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                }
            });
        }
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        // Hentikan auto-refresh saat fragment dihancurkan
        if (camHandler != null && camRunnable != null) {
            camHandler.removeCallbacks(camRunnable);
        }
        if (camExecutor != null && !camExecutor.isShutdown()) {
            camExecutor.shutdownNow();
        }
    }

    // ─── NOTIFICATION DOT HELPER ─────────────────────────────────────────────

    /** Key SharedPreferences untuk status notifikasi belum dibaca */
    static final String PREFS_NAME = "notif_prefs";
    static final String KEY_HAS_UNREAD = "has_unread_notif";

    /**
     * Simpan status "ada notifikasi belum dibaca" ke SharedPreferences.
     * Dipanggil saat Firebase mendeteksi ada notifikasi baru, atau dihapus
     * saat semua notifikasi sudah ditandai dibaca.
     */
    static void setHasUnreadNotification(Context context, boolean hasUnread) {
        SharedPreferences prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        prefs.edit().putBoolean(KEY_HAS_UNREAD, hasUnread).apply();
    }

    /** Baca SharedPreferences dan tampilkan/sembunyikan dot merah di bell. */
    private void refreshNotificationDot() {
        if (viewNotificationDot == null || getContext() == null) return;
        SharedPreferences prefs = requireContext().getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        boolean hasUnread = prefs.getBoolean(KEY_HAS_UNREAD, false);
        viewNotificationDot.setVisibility(hasUnread ? View.VISIBLE : View.GONE);
    }
}
