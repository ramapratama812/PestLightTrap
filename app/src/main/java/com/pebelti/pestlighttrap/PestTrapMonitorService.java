package com.pebelti.pestlighttrap;

import android.app.Service;
import android.content.Intent;
import android.os.IBinder;
import android.util.Log;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

/**
 * Service latar belakang yang memantau Firebase Realtime Database
 * dan mengirimkan notifikasi pop-up ke HP pengguna ketika ada perubahan status.
 */
public class PestTrapMonitorService extends Service {

    private static final String TAG = "PestTrapMonitor";

    // ID unik untuk setiap notifikasi (agar tidak duplikat)
    private static final int NOTIF_ID_LAMP = 1001;
    private static final int NOTIF_ID_AUTO_MODE = 1002;
    private static final int NOTIF_ID_BATTERY = 1003;
    private static final int NOTIF_ID_TRAP = 1004;
    private static final int NOTIF_ID_SOLAR = 1005;

    private NotificationHelper notificationHelper;
    private DatabaseReference rootRef;
    private ValueEventListener firebaseListener;

    // Simpan state sebelumnya untuk mendeteksi perubahan
    private Boolean prevLampStatus = null;
    private Boolean prevAutoMode = null;
    private Integer prevBattery = null;
    private Integer prevTrap = null;
    private Integer prevPower = null;
    private boolean isFirstLoad = true;

    @Override
    public void onCreate() {
        super.onCreate();
        notificationHelper = new NotificationHelper(this);
        rootRef = FirebaseDatabase.getInstance().getReference();
        startMonitoring();
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        // START_STICKY agar service berjalan terus di background
        return START_STICKY;
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (rootRef != null && firebaseListener != null) {
            rootRef.removeEventListener(firebaseListener);
        }
    }

    private void startMonitoring() {
        firebaseListener = new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // 1. Monitor status lampu
                Boolean isLampOn = snapshot.child("smart_pest_trap/status").getValue(Boolean.class);
                if (isLampOn != null && !isLampOn.equals(prevLampStatus)) {
                    if (!isFirstLoad) {
                        if (isLampOn) {
                            notificationHelper.showNotification(NOTIF_ID_LAMP,
                                    "Lampu Aktif",
                                    "Lampu perangkap UV sedang menyala dan beroperasi normal.",
                                    "SUCCESS", R.drawable.ic_lightbulb_outline);
                        } else {
                            notificationHelper.showNotification(NOTIF_ID_LAMP,
                                    "Lampu Tidak Aktif",
                                    "Lampu perangkap UV sedang dimatikan.",
                                    "WARNING", R.drawable.ic_lightbulb_outline);
                        }
                    }
                    prevLampStatus = isLampOn;
                }

                // 2. Monitor auto mode (trap diaktifkan/dimatikan)
                Boolean isAuto = snapshot.child("smart_pest_trap/auto_mode").getValue(Boolean.class);
                if (isAuto != null && !isAuto.equals(prevAutoMode)) {
                    if (!isFirstLoad) {
                        if (isAuto) {
                            notificationHelper.showNotification(NOTIF_ID_AUTO_MODE,
                                    "Trap Diaktifkan",
                                    "Mode otomatis aktif. Trap beroperasi sesuai jadwal.",
                                    "INFO", R.drawable.ic_lamp_trap);
                        } else {
                            notificationHelper.showNotification(NOTIF_ID_AUTO_MODE,
                                    "Trap Dimatikan",
                                    "Mode otomatis dinonaktifkan. Trap dalam kendali manual.",
                                    "WARNING", R.drawable.ic_lamp_trap);
                        }
                    }
                    prevAutoMode = isAuto;
                }

                // 3. Monitor baterai
                Integer battery = snapshot.child("smart_pest_trap/battery/percent").getValue(Integer.class);
                if (battery != null) {
                    boolean batteryChanged = !battery.equals(prevBattery);
                    if (batteryChanged && !isFirstLoad) {
                        if (battery <= 20) {
                            notificationHelper.showNotification(NOTIF_ID_BATTERY,
                                    "⚠️ Baterai Lemah",
                                    "Baterai turun ke " + battery + "%. Harap isi daya panel surya.",
                                    "CRITICAL", R.drawable.ic_battery_outline);
                        } else if (battery >= 80 && (prevBattery == null || prevBattery < 80)) {
                            notificationHelper.showNotification(NOTIF_ID_BATTERY,
                                    "✅ Baterai Terisi Penuh",
                                    "Kapasitas baterai " + battery + "%. Daya optimal.",
                                    "SUCCESS", R.drawable.ic_battery_outline);
                        }
                    }
                    prevBattery = battery;
                }

                // 4. Monitor trap fullness
                Integer trap = snapshot.child("smart_pest_trap/trap_fullness").getValue(Integer.class);
                if (trap != null) {
                    boolean trapChanged = !trap.equals(prevTrap);
                    if (trapChanged && !isFirstLoad) {
                        if (trap >= 80) {
                            notificationHelper.showNotification(NOTIF_ID_TRAP,
                                    "🚨 Trap Penuh!",
                                    "Kapasitas trap hama mencapai " + trap + "%. Perlu dikosongkan segera!",
                                    "CRITICAL", R.drawable.ic_bug_report);
                        } else if (trap <= 20 && (prevTrap == null || prevTrap > 20)) {
                            notificationHelper.showNotification(NOTIF_ID_TRAP,
                                    "✅ Trap Kosong",
                                    "Kapasitas trap hama " + trap + "%. Trap bersih dan siap digunakan.",
                                    "SUCCESS", R.drawable.ic_bug_report);
                        }
                    }
                    prevTrap = trap;
                }

                // 5. Monitor efisiensi panel surya
                Integer power = snapshot.child("smart_pest_trap/power_consumption").getValue(Integer.class);
                if (power != null) {
                    boolean powerChanged = !power.equals(prevPower);
                    if (powerChanged && !isFirstLoad) {
                        if (power <= 40 && (prevPower == null || prevPower > 40)) {
                            notificationHelper.showNotification(NOTIF_ID_SOLAR,
                                    "⚠️ Efisiensi Panel Surya Rendah",
                                    "Input surya " + power + "%. Efisiensi berkurang, periksa panel.",
                                    "WARNING", R.drawable.ic_solar_panel);
                        } else if (power >= 80 && (prevPower == null || prevPower < 80)) {
                            notificationHelper.showNotification(NOTIF_ID_SOLAR,
                                    "☀️ Efisiensi Panel Surya Baik",
                                    "Input efisiensi surya " + power + "%. Beroperasi optimal.",
                                    "SUCCESS", R.drawable.ic_solar_panel);
                        }
                    }
                    prevPower = power;
                }

                // Setelah load pertama, aktifkan notifikasi untuk perubahan berikutnya
                isFirstLoad = false;
                Log.d(TAG, "Firebase data updated. Lamp=" + isLampOn + ", Auto=" + isAuto
                        + ", Battery=" + battery + ", Trap=" + trap + ", Power=" + power);
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                Log.e(TAG, "Firebase listener cancelled: " + error.getMessage());
            }
        };

        rootRef.addValueEventListener(firebaseListener);
    }
}
