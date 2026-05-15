package com.pebelti.pestlighttrap;

import android.graphics.Color;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class HomeDashboardFragment extends Fragment {

    private FrameLayout btnToggleDevice;
    private TextView tvStatus;
    private View statusIndicator;
    private TextView tvToggleBadge;
    private boolean isDeviceOn = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home_dashboard, container, false);

        btnToggleDevice = view.findViewById(R.id.btnToggleDevice);
        tvStatus = view.findViewById(R.id.tvDeviceStatus);
        statusIndicator = view.findViewById(R.id.statusIndicator);
        tvToggleBadge = view.findViewById(R.id.tvToggleBadge);
        View btnNotifications = view.findViewById(R.id.btnNotifications);

        btnNotifications.setOnClickListener(v -> {
            getParentFragmentManager().beginTransaction()
                    .replace(R.id.fragment_container, new NotificationsFragment())
                    .addToBackStack(null)
                    .commit();
        });

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

        return view;
    }
}
