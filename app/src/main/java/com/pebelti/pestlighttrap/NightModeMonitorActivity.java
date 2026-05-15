package com.pebelti.pestlighttrap;

import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class NightModeMonitorActivity extends AppCompatActivity {

    private TextView tvClock;
    private Handler clockHandler;
    private Runnable clockRunnable;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_night_mode);

        tvClock = findViewById(R.id.tvRealTimeClock);
        ImageView btnClose = findViewById(R.id.btnClose);

        // Tutup activity dan kembali ke layar sebelumnya (Home)
        btnClose.setOnClickListener(v -> finish());

        // Implementasi Handler untuk jam real-time berdetak tiap 1 detik
        clockHandler = new Handler(Looper.getMainLooper());
        clockRunnable = new Runnable() {
            @Override
            public void run() {
                updateClock();
                clockHandler.postDelayed(this, 1000);
            }
        };
        clockHandler.post(clockRunnable);
    }

    private void updateClock() {
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        String currentTime = sdf.format(new Date());
        tvClock.setText(currentTime);
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (clockHandler != null) {
            clockHandler.removeCallbacks(clockRunnable);
        }
    }
}
