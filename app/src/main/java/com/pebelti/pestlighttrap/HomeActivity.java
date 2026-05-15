package com.pebelti.pestlighttrap;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class HomeActivity extends AppCompatActivity {

    private FrameLayout btnToggleDevice;
    private TextView tvStatus;
    private View statusIndicator;
    private TextView tvToggleBadge;
    private boolean isDeviceOn = true;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        btnToggleDevice = findViewById(R.id.btnToggleDevice);
        tvStatus = findViewById(R.id.tvDeviceStatus);
        statusIndicator = findViewById(R.id.statusIndicator);
        tvToggleBadge = findViewById(R.id.tvToggleBadge);

        btnToggleDevice.setOnClickListener(v -> {
            isDeviceOn = !isDeviceOn;
            if (isDeviceOn) {
                tvStatus.setText("ACTIVE");
                tvStatus.setTextColor(Color.parseColor("#4CAF50")); // Hijau
                statusIndicator.setBackgroundResource(R.drawable.dot_green);
                tvToggleBadge.setText("ON");
                tvToggleBadge.setBackgroundResource(R.drawable.bg_badge_green);
                tvToggleBadge.setTextColor(Color.parseColor("#4CAF50"));
            } else {
                tvStatus.setText("INACTIVE");
                tvStatus.setTextColor(Color.parseColor("#F44336")); // Merah
                statusIndicator.setBackgroundResource(R.drawable.dot_red);
                tvToggleBadge.setText("OFF");
                tvToggleBadge.setBackgroundResource(R.drawable.bg_badge_red);
                tvToggleBadge.setTextColor(Color.parseColor("#F44336"));
            }
        });

        BottomNavigationView bottomNav = findViewById(R.id.bottomNavigation);
        bottomNav.setOnItemSelectedListener(item -> {
            // TODO: Implementasi pergantian Fragment
            return true;
        });
    }
}
