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

public class StatsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        // Menghubungkan ke layout fragment_stats.xml
        View view = inflater.inflate(R.layout.fragment_stats, container, false);

        // --- 1. FITUR TOMBOL LONCENG ---
        ImageView btnBell = view.findViewById(R.id.btn_bell);
        if (btnBell != null) {
            btnBell.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).loadFragment(new NotificationFragment());
                }
            });
        }

        // --- FITUR TOMBOL PROFIL ---
        ImageView btnProfile = view.findViewById(R.id.btn_profile);
        if (btnProfile != null) {
            btnProfile.setOnClickListener(v -> {
                if (getActivity() instanceof MainActivity) {
                    ((MainActivity) getActivity()).loadFragment(new SettingsFragment());
                }
            });
        }

        // --- 2. MENGHUBUNGKAN ID TOMBOL DAN HALAMAN ---
        TextView tabPerangkap = view.findViewById(R.id.tab_perangkap);
        TextView tabBaterai = view.findViewById(R.id.tab_baterai);
        LinearLayout layoutPerangkap = view.findViewById(R.id.layout_perangkap);
        LinearLayout layoutBaterai = view.findViewById(R.id.layout_baterai);

        // Pastikan variabel tidak null sebelum memberikan perintah klik
        if (tabPerangkap != null && tabBaterai != null && layoutPerangkap != null && layoutBaterai != null) {

            // --- 3. LOGIKA KLIK TOMBOL PERANGKAP ---
            tabPerangkap.setOnClickListener(v -> {
                // Tampilkan halaman perangkap, sembunyikan halaman baterai
                layoutPerangkap.setVisibility(View.VISIBLE);
                layoutBaterai.setVisibility(View.GONE);

                // Pindahkan latar biru ke tombol Perangkap
                tabPerangkap.setBackgroundResource(R.drawable.bg_toggle_active);
                tabBaterai.setBackgroundResource(0); // Angka 0 menghapus background
            });

            // --- 4. LOGIKA KLIK TOMBOL BATERAI ---
            tabBaterai.setOnClickListener(v -> {
                // Tampilkan halaman baterai, sembunyikan halaman perangkap
                layoutBaterai.setVisibility(View.VISIBLE);
                layoutPerangkap.setVisibility(View.GONE);

                // Pindahkan latar biru ke tombol Baterai
                tabBaterai.setBackgroundResource(R.drawable.bg_toggle_active);
                tabPerangkap.setBackgroundResource(0);
            });
        }

        return view;
    }
}