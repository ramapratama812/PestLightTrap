package com.pebelti.pestlighttrap;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.google.android.material.switchmaterial.SwitchMaterial;

public class SettingsFragment extends Fragment {

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        // Account Section
        view.findViewById(R.id.btnChangePhoto).setOnClickListener(v -> 
            Toast.makeText(getContext(), "Buka Galeri untuk mengubah foto...", Toast.LENGTH_SHORT).show());
        
        view.findViewById(R.id.btnEditProfile).setOnClickListener(v -> 
            Toast.makeText(getContext(), "Membuka pengaturan profil...", Toast.LENGTH_SHORT).show());
        
        SwitchMaterial switchNotifications = view.findViewById(R.id.switchNotifications);
        switchNotifications.setOnCheckedChangeListener((buttonView, isChecked) -> {
            String status = isChecked ? "diaktifkan" : "dimatikan";
            Toast.makeText(getContext(), "Notifikasi " + status, Toast.LENGTH_SHORT).show();
        });

        view.findViewById(R.id.btnChangePassword).setOnClickListener(v -> 
            Toast.makeText(getContext(), "Membuka halaman ubah kata sandi...", Toast.LENGTH_SHORT).show());

        // Application Section
        view.findViewById(R.id.btnAbout).setOnClickListener(v -> showAboutDialog());
        
        view.findViewById(R.id.btnPrivacyPolicy).setOnClickListener(v -> 
            Toast.makeText(getContext(), "Membuka Kebijakan Privasi...", Toast.LENGTH_SHORT).show());
        
        view.findViewById(R.id.btnTermsOfUse).setOnClickListener(v -> 
            Toast.makeText(getContext(), "Membuka Syarat Penggunaan...", Toast.LENGTH_SHORT).show());
        
        view.findViewById(R.id.btnHelpSupport).setOnClickListener(v -> showHelpDialog());

        // Logout
        view.findViewById(R.id.btnLogout).setOnClickListener(v -> showLogoutDialog());

        return view;
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Tentang Aplikasi")
                .setMessage("Smart Light Trap v1.0.0\n\nAplikasi monitoring dan kontrol alat perangkap hama pintar untuk pertanian modern.\n\n© 2026 PestLightTrap Team")
                .setPositiveButton("Tutup", null)
                .show();
    }

    private void showHelpDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Bantuan & Dukungan")
                .setMessage("Jika Anda mengalami kendala, silakan hubungi tim dukungan kami di:\n\nEmail: support@pestlighttrap.com\nWhatsApp: +62 812 3456 7890")
                .setPositiveButton("Tutup", null)
                .show();
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Konfirmasi Keluar")
                .setMessage("Apakah Anda yakin ingin keluar dari aplikasi?")
                .setPositiveButton("Ya, Keluar", (dialog, which) -> {
                    // Logic logout - clear session/prefs if needed
                    Toast.makeText(getContext(), "Berhasil keluar", Toast.LENGTH_SHORT).show();
                    // In a real app, redirect to LoginActivity
                    // For now, we'll just show a message since LoginActivity might not be fully ready
                    if (getActivity() != null) {
                        getActivity().finish();
                    }
                })
                .setNegativeButton("Batal", null)
                .show();
    }
}

