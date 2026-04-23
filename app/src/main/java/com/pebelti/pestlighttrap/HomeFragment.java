package com.pebelti.pestlighttrap;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class HomeFragment extends Fragment {

    private boolean isOn = true;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        View view = inflater.inflate(R.layout.fragment_home, container, false);

        ImageView btnBell = view.findViewById(R.id.btn_bell);
        ImageView btnProfile = view.findViewById(R.id.btn_profile);
        ImageView btnPower = view.findViewById(R.id.btn_power);
        TextView txtStatus = view.findViewById(R.id.txt_status);
        LinearLayout toggle = view.findViewById(R.id.toggle_container);

        // NOTIF
        if (btnBell != null) {
            btnBell.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).loadFragment(new NotificationFragment());
                }
            });
        }

        // PROFILE
        if (btnProfile != null) {
            btnProfile.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    // Diperbaiki: Menggunakan SettingsFragment (dengan 's') sesuai nama file yang ada
                    ((MainActivity) getActivity()).loadFragment(new SettingsFragment());
                }
            });
        }

        // TOGGLE ON/OFF
        if (btnPower != null && txtStatus != null && toggle != null) {
            btnPower.setOnClickListener(v -> {
                isOn = !isOn;

                if (isOn) {
                    txtStatus.setText("ON");
                    txtStatus.setTextColor(getResources().getColor(android.R.color.white));
                    toggle.setBackgroundResource(R.drawable.bg_toggle_on);
                } else {
                    txtStatus.setText("OFF");
                    txtStatus.setTextColor(getResources().getColor(android.R.color.black));
                    toggle.setBackgroundResource(R.drawable.bg_toggle_off);
                }
            });
        }

        return view;
    }
}