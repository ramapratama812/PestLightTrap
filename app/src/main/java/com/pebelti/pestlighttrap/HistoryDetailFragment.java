package com.pebelti.pestlighttrap;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import android.widget.LinearLayout;
import android.app.DatePickerDialog;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class HistoryDetailFragment extends Fragment {

    // ESP32-CAM Configuration (sama dengan HomeDashboardFragment)
    private static final String ESP_CAM_CAPTURE_URL = "http://10.156.99.169/capture";
    private static final int CAM_REFRESH_INTERVAL_MS = 5000;

    private TextView tvHistoryDateBadge;
    private TextView tvHistoryImageDate;
    private ImageView btnBack;
    private LinearLayout btnCalendar;
    private Calendar selectedCalendar = Calendar.getInstance();

    // Overview stats
    private TextView tvDetailDuration;
    private TextView tvDetailStatus;
    private TextView tvDetailPower;
    private TextView tvDetailVoltage;

    // Trap condition
    private TextView tvTrapCapacity;
    private TextView tvTrapCapacityLabel;
    private View dotTrapCapacity;
    private TextView tvTrapDistance;
    private TextView tvTrapDistanceLabel;
    private View dotTrapDistance;

    // Status message
    private View viewStatusBar;
    private TextView tvStatusMessage;

    // ESP32-CAM views
    private ImageView ivEspCamCapture;
    private ProgressBar pbCamLoading;
    private ImageView btnRefreshCam;
    private Handler camHandler;
    private Runnable camRunnable;
    private ExecutorService camExecutor;

    // Firebase references
    private DatabaseReference deviceRef;
    private DatabaseReference batteryRef;
    private DatabaseReference trapRef;
    private DatabaseReference powerRef;
    private DatabaseReference operationRef;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_history_detail, container, false);

        // Bind views
        tvHistoryDateBadge = view.findViewById(R.id.tvHistoryDateBadge);
        tvHistoryImageDate = view.findViewById(R.id.tvHistoryImageDate);
        btnBack = view.findViewById(R.id.btnBack);
        btnCalendar = view.findViewById(R.id.btnCalendar);

        // Overview stats
        tvDetailDuration = view.findViewById(R.id.tvDetailDuration);
        tvDetailStatus = view.findViewById(R.id.tvDetailStatus);
        tvDetailPower = view.findViewById(R.id.tvDetailPower);
        tvDetailVoltage = view.findViewById(R.id.tvDetailVoltage);

        // Trap condition
        tvTrapCapacity = view.findViewById(R.id.tvTrapCapacity);
        tvTrapCapacityLabel = view.findViewById(R.id.tvTrapCapacityLabel);
        dotTrapCapacity = view.findViewById(R.id.dotTrapCapacity);
        tvTrapDistance = view.findViewById(R.id.tvTrapDistance);
        tvTrapDistanceLabel = view.findViewById(R.id.tvTrapDistanceLabel);
        dotTrapDistance = view.findViewById(R.id.dotTrapDistance);

        // Status message
        viewStatusBar = view.findViewById(R.id.viewStatusBar);
        tvStatusMessage = view.findViewById(R.id.tvStatusMessage);

        // Set dynamic date (Default to Today)
        setupHistoryDate();

        // Calendar Click Listener
        btnCalendar.setOnClickListener(v -> showDatePicker());

        // Handle back button click
        btnBack.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new HomeDashboardFragment())
                    .commit();
        });

        // Inisialisasi Firebase
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        deviceRef = database.getReference("smart_pest_trap/status");
        batteryRef = database.getReference("smart_pest_trap/battery");
        trapRef = database.getReference("smart_pest_trap/trap_fullness");
        powerRef = database.getReference("smart_pest_trap/power_consumption");
        operationRef = database.getReference("smart_pest_trap/operation_mode");

        // Load data dari Firebase
        loadDeviceStatus();
        loadBatteryData();
        loadTrapCondition();
        loadPowerConsumption();
        loadOperationDuration();

        // Setup ESP32-CAM live capture
        setupEspCam(view);

        return view;
    }

    // ─── FIREBASE DATA LOADING ─────────────────────────────────────────────

    private void loadDeviceStatus() {
        deviceRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && isAdded()) {
                    Boolean isOn = snapshot.getValue(Boolean.class);
                    if (isOn != null) {
                        if (isOn) {
                            tvDetailStatus.setText("AKTIF");
                            tvDetailStatus.setTextColor(Color.parseColor("#4CAF50"));
                        } else {
                            tvDetailStatus.setText("TIDAK AKTIF");
                            tvDetailStatus.setTextColor(Color.parseColor("#F44336"));
                        }
                        updateStatusMessage();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void loadBatteryData() {
        batteryRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && isAdded()) {
                    // Tegangan
                    Object voltageObj = snapshot.child("voltage").getValue();
                    if (voltageObj != null) {
                        String voltageStr;
                        if (voltageObj instanceof Double) {
                            voltageStr = String.format(Locale.getDefault(), "%.1fV", (Double) voltageObj);
                        } else if (voltageObj instanceof Long) {
                            voltageStr = voltageObj + "V";
                        } else {
                            voltageStr = voltageObj.toString() + "V";
                        }
                        tvDetailVoltage.setText(voltageStr);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void loadTrapCondition() {
        trapRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && isAdded()) {
                    Integer percent = snapshot.getValue(Integer.class);
                    if (percent != null) {
                        tvTrapCapacity.setText(percent + "%");

                        if (percent >= 80) {
                            tvTrapCapacityLabel.setText("PENUH");
                            dotTrapCapacity.setBackgroundResource(R.drawable.dot_red);
                        } else if (percent >= 50) {
                            tvTrapCapacityLabel.setText("SETENGAH");
                            dotTrapCapacity.setBackgroundResource(R.drawable.dot_green);
                        } else {
                            tvTrapCapacityLabel.setText("KOSONG");
                            dotTrapCapacity.setBackgroundResource(R.drawable.dot_green);
                        }

                        // Hitung jarak permukaan berdasarkan persentase (asumsi kedalaman wadah 50cm)
                        int maxDepth = 50;
                        int filledDepth = (int) (maxDepth * percent / 100.0);
                        int remainingDistance = maxDepth - filledDepth;
                        tvTrapDistance.setText(maxDepth + " / " + remainingDistance + " cm");

                        if (remainingDistance <= 10) {
                            tvTrapDistanceLabel.setText("HAMPIR PENUH");
                            dotTrapDistance.setBackgroundResource(R.drawable.dot_red);
                        } else {
                            tvTrapDistanceLabel.setText("TERSEDIA");
                            dotTrapDistance.setBackgroundResource(R.drawable.dot_green);
                        }

                        updateStatusMessage();
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void loadPowerConsumption() {
        powerRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && isAdded()) {
                    Integer value = snapshot.getValue(Integer.class);
                    if (value != null) {
                        tvDetailPower.setText(value + "%");
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void loadOperationDuration() {
        operationRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists() && isAdded()) {
                    String start = snapshot.child("start_time").getValue(String.class);
                    String end = snapshot.child("end_time").getValue(String.class);

                    if (start != null && end != null) {
                        try {
                            String[] s = start.split(":");
                            String[] e = end.split(":");
                            int startH = Integer.parseInt(s[0]);
                            int startM = Integer.parseInt(s[1]);
                            int endH = Integer.parseInt(e[0]);
                            int endM = Integer.parseInt(e[1]);

                            int startMins = startH * 60 + startM;
                            int endMins = endH * 60 + endM;
                            int totalDuration;
                            if (startMins <= endMins) {
                                totalDuration = endMins - startMins;
                            } else {
                                totalDuration = (24 * 60 - startMins) + endMins;
                            }
                            int hours = totalDuration / 60;
                            int mins = totalDuration % 60;
                            tvDetailDuration.setText(hours + "J " + mins + "M");
                        } catch (Exception ignored) {
                            tvDetailDuration.setText("--");
                        }
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });
    }

    private void updateStatusMessage() {
        if (tvStatusMessage == null || viewStatusBar == null || !isAdded()) return;

        String statusText = tvDetailStatus != null ? tvDetailStatus.getText().toString() : "";
        String capacityText = tvTrapCapacityLabel != null ? tvTrapCapacityLabel.getText().toString() : "";

        if ("TIDAK AKTIF".equals(statusText)) {
            viewStatusBar.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#F44336")));
            tvStatusMessage.setText("PERANGKAT SEDANG TIDAK AKTIF. PERIKSA KONEKSI DAN STATUS ALAT.");
            tvStatusMessage.setTextColor(Color.parseColor("#F44336"));
        } else if ("PENUH".equals(capacityText)) {
            viewStatusBar.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#FFA000")));
            tvStatusMessage.setText("PERANGKAP SUDAH PENUH! SEGERA BERSIHKAN WADAH PERANGKAP.");
            tvStatusMessage.setTextColor(Color.parseColor("#FFA000"));
        } else {
            viewStatusBar.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#4CAF50")));
            tvStatusMessage.setText("KONDISI PERANGKAP AMAN DAN BERJALAN OPTIMAL SELAMA SIKLUS BERLANGSUNG.");
            tvStatusMessage.setTextColor(Color.parseColor("#4CAF50"));
        }
    }

    // ─── HISTORY DATE ──────────────────────────────────────────────────────

    private void setupHistoryDate() {
        // Format for badge (e.g., "20 MEI")
        java.text.SimpleDateFormat badgeFormat = new java.text.SimpleDateFormat("d MMM", new Locale("id", "ID"));
        String badgeStr = badgeFormat.format(selectedCalendar.getTime()).toUpperCase();
        if (tvHistoryDateBadge != null) {
            tvHistoryDateBadge.setText(badgeStr);
        }

        // Format for image header (e.g., "• KAMIS, 20 MEI")
        java.text.SimpleDateFormat imageFormat = new java.text.SimpleDateFormat("EEEE, d MMM", new Locale("id", "ID"));
        String imageDateStr = "• " + imageFormat.format(selectedCalendar.getTime()).toUpperCase();
        if (tvHistoryImageDate != null) {
            tvHistoryImageDate.setText(imageDateStr);
        }
    }

    private void showDatePicker() {
        if (getContext() == null) return;

        DatePickerDialog datePickerDialog = new DatePickerDialog(
                getContext(),
                (v, year, month, dayOfMonth) -> {
                    selectedCalendar.set(Calendar.YEAR, year);
                    selectedCalendar.set(Calendar.MONTH, month);
                    selectedCalendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    setupHistoryDate();

                    Toast.makeText(getContext(), "Menampilkan riwayat untuk: " + tvHistoryDateBadge.getText().toString(), Toast.LENGTH_SHORT).show();
                },
                selectedCalendar.get(Calendar.YEAR),
                selectedCalendar.get(Calendar.MONTH),
                selectedCalendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    // ─── ESP32-CAM LIVE CAPTURE ────────────────────────────────────────────

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
                } catch (Exception ignored) {
                }
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
