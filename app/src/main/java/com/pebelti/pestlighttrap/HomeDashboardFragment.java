package com.pebelti.pestlighttrap;

import android.app.TimePickerDialog;
import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
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

import java.util.Calendar;
import java.util.Locale;

public class HomeDashboardFragment extends Fragment {

    private FrameLayout btnToggleDevice;
    private TextView tvStatus;
    private View statusIndicator;
    private TextView tvToggleBadge;
    private boolean isDeviceOn = true;

    private String startTime = "18:00";
    private String endTime = "06:00";

    // Firebase References
    private DatabaseReference deviceRef;
    private DatabaseReference operationRef;
    private DatabaseReference batteryRef;
    private DatabaseReference trapRef;
    private DatabaseReference powerRef;
    
    private TextView[] days;
    private TextView tvOperationTime;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home_dashboard, container, false);

        btnToggleDevice = view.findViewById(R.id.btnToggleDevice);
        tvStatus = view.findViewById(R.id.tvDeviceStatus);
        statusIndicator = view.findViewById(R.id.statusIndicator);
        tvToggleBadge = view.findViewById(R.id.tvToggleBadge);
        View btnNotifications = view.findViewById(R.id.btnNotifications);

        // Inisialisasi Firebase
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        deviceRef = database.getReference("device_status");
        operationRef = database.getReference("operation_mode");
        batteryRef = database.getReference("battery");
        trapRef = database.getReference("trap_fullness");
        powerRef = database.getReference("power_consumption");

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

        btnNotifications.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new NotificationsFragment())
                    .addToBackStack(null)
                    .commit();
        });

        // Klik tombol toggle -> Update ke Firebase
        btnToggleDevice.setOnClickListener(v -> {
            deviceRef.setValue(!isDeviceOn);
        });

        setupModeOperasi(view);
        setupClock(view);
        loadSolarData(view);
        loadTrapCondition(view);
        loadPowerDraw(view);
        loadOperationMode();

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
                
                java.text.SimpleDateFormat dateFormat = new java.text.SimpleDateFormat("EEEE, dd MMMM yyyy", new java.util.Locale("id", "ID"));
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

    private void setupModeOperasi(View view) {
        tvOperationTime = view.findViewById(R.id.tvOperationTime);
        TextView btnEditOperation = view.findViewById(R.id.btnEditOperation);
        
        days = new TextView[]{
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
        if (operationRef == null) return;
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
        if (operationRef == null) return;
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
            public void onCancelled(@NonNull DatabaseError error) {}
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

        if (batteryRef == null) return;
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
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadTrapCondition(View view) {
        TextView tvTrapPercent = view.findViewById(R.id.tvTrapPercent);
        TextView tvTrapStatus = view.findViewById(R.id.tvTrapStatus);

        if (trapRef == null) return;
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
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }

    private void loadPowerDraw(View view) {
        TextView tvPowerValue = view.findViewById(R.id.tvPowerValue);
        TextView tvPowerStatus = view.findViewById(R.id.tvPowerStatus);

        if (powerRef == null) return;
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
            public void onCancelled(@NonNull DatabaseError error) {}
        });
    }
}
