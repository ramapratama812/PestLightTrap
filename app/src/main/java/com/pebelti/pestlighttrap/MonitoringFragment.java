package com.pebelti.pestlighttrap;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

public class MonitoringFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Menginflate layout fragment_monitoring.xml
        View view = inflater.inflate(R.layout.fragment_monitoring, container, false);

        // 1. Inisialisasi Widget dari XML
        ImageView btnBell = view.findViewById(R.id.btn_bell);
        TextView btnLampu = view.findViewById(R.id.btn_lampu_stream);

        // 2. Logika Klik Tombol Lampu di Siaran Langsung
        if (btnLampu != null) {
            btnLampu.setOnClickListener(v -> {
                // Menampilkan notifikasi singkat saat diklik
                Toast.makeText(getContext(), "Perintah Lampu Dikirim ke ESP32", Toast.LENGTH_SHORT).show();
            });
        }

        // 3. Logika Navigasi Notifikasi (Tombol Bell)
        if (btnBell != null) {
            btnBell.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).loadFragment(new NotificationFragment());
                }
            });
        }

        return view;
    }
}