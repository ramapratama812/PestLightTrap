package com.pebelti.pestlighttrap;

import android.app.TimePickerDialog;
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

    // ESP32-CAM views
    private ImageView ivEspCamCapture;
    private ProgressBar pbCamLoading;
    private ImageView btnRefreshCam;
    private Handler camHandler;
    private Runnable camRunnable;
    private ExecutorService camExecutor;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home_dashboard, container, false);

        btnToggleDevice = view.findViewById(R.id.btnToggleDevice);
        tvStatus = view.findViewById(R.id.tvDeviceStatus);
        statusIndicator = view.findViewById(R.id.statusIndicator);
        tvToggleBadge = view.findViewById(R.id.tvToggleBadge);
        View btnNotifications = view.findViewById(R.id.btnNotifications);

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

        if (trapRef == null)
            return;
        trapRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Integer percent = snapshot.getValue(Integer.class);
                    if (percent != null) {
                        tvTrapPercent.setText(percent + "%");

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
        if (!isAdded() || ivEspCamCapture == null) return;

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
                    if (inputStream != null) inputStream.close();
                } catch (Exception ignored) {}
                if (connection != null) connection.disconnect();
            }

            final Bitmap finalBitmap = bitmap;
            camHandler.post(() -> {
                if (!isAdded()) return;

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
}
