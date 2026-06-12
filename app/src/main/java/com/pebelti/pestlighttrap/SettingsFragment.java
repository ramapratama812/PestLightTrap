package com.pebelti.pestlighttrap;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Base64;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.fragment.app.Fragment;
import com.google.android.material.switchmaterial.SwitchMaterial;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class SettingsFragment extends Fragment {

    private ImageView ivProfilePicture;
    private TextView tvProfileName;
    private TextView tvProfileEmail;
    private FirebaseAuth mAuth;
    private FirebaseUser currentUser;
    private DatabaseReference userRef;

    private final ActivityResultLauncher<Intent> pickImageLauncher = registerForActivityResult(
            new ActivityResultContracts.StartActivityForResult(),
            result -> {
                if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                    Uri imageUri = result.getData().getData();
                    uploadImageToDatabase(imageUri);
                }
            });

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_settings, container, false);

        ivProfilePicture = view.findViewById(R.id.ivProfilePicture);
        tvProfileName = view.findViewById(R.id.tvProfileName);
        tvProfileEmail = view.findViewById(R.id.tvProfileEmail);

        mAuth = FirebaseAuth.getInstance();
        currentUser = mAuth.getCurrentUser();

        if (currentUser != null) {
            updateProfileUI();
            userRef = FirebaseDatabase.getInstance().getReference("users").child(currentUser.getUid());
            loadProfilePicture();
        }

        // Account Section
        view.findViewById(R.id.btnChangePhoto).setOnClickListener(v -> openGallery());

        view.findViewById(R.id.btnEditProfile).setOnClickListener(v -> {
            startActivity(new Intent(getContext(), EditProfileActivity.class));
        });

        SwitchMaterial switchAutoMode = view.findViewById(R.id.switchAutoMode);
        DatabaseReference autoModeRef = FirebaseDatabase.getInstance().getReference("smart_pest_trap/auto_mode"); // ubah
                                                                                                                  // refensinya
                                                                                                                  // supaya
                                                                                                                  // ngikutin
                                                                                                                  // yg
                                                                                                                  // di
                                                                                                                  // realtime
                                                                                                                  // database

        autoModeRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    Boolean isAuto = snapshot.getValue(Boolean.class);
                    if (isAuto != null) {
                        switchAutoMode.setChecked(isAuto);
                    }
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
            }
        });

        switchAutoMode.setOnCheckedChangeListener((buttonView, isChecked) -> {
            autoModeRef.setValue(isChecked);
            String status = isChecked ? "OTOMATIS" : "MANUAL";
            Toast.makeText(getContext(), "Mode Alat diubah menjadi " + status, Toast.LENGTH_SHORT).show();
        });

        // Application Section
        view.findViewById(R.id.btnAbout).setOnClickListener(v -> showAboutDialog());
        view.findViewById(R.id.btnPrivacyPolicy).setOnClickListener(
                v -> Toast.makeText(getContext(), "Membuka Kebijakan Privasi...", Toast.LENGTH_SHORT).show());
        view.findViewById(R.id.btnTermsOfUse).setOnClickListener(
                v -> Toast.makeText(getContext(), "Membuka Syarat Penggunaan...", Toast.LENGTH_SHORT).show());
        view.findViewById(R.id.btnHelpSupport).setOnClickListener(v -> showHelpDialog());

        // Logout
        view.findViewById(R.id.btnLogout).setOnClickListener(v -> showLogoutDialog());

        return view;
    }

    @Override
    public void onResume() {
        super.onResume();
        if (mAuth != null && mAuth.getCurrentUser() != null) {
            currentUser = mAuth.getCurrentUser();
            currentUser.reload().addOnCompleteListener(task -> {
                if (task.isSuccessful() && isAdded()) {
                    updateProfileUI();
                }
            });
        }
    }

    private void updateProfileUI() {
        if (currentUser != null) {
            String email = currentUser.getEmail();
            if (email != null) {
                tvProfileEmail.setText(email);
                if (email.contains("@")) {
                    String namePart = email.split("@")[0];
                    tvProfileName.setText(namePart.toUpperCase());
                }
            }
        }
    }

    private void openGallery() {
        Intent intent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        pickImageLauncher.launch(intent);
    }

    private void uploadImageToDatabase(Uri imageUri) {
        try {
            InputStream inputStream = requireContext().getContentResolver().openInputStream(imageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

            int width = bitmap.getWidth();
            int height = bitmap.getHeight();
            float maxDim = 300.0f; // Scale down for Base64 storage
            if (width > maxDim || height > maxDim) {
                float ratio = Math.min(maxDim / width, maxDim / height);
                width = Math.round(ratio * width);
                height = Math.round(ratio * height);
                bitmap = Bitmap.createScaledBitmap(bitmap, width, height, true);
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            bitmap.compress(Bitmap.CompressFormat.JPEG, 70, baos);
            byte[] imageBytes = baos.toByteArray();
            String base64Image = Base64.encodeToString(imageBytes, Base64.DEFAULT);

            if (userRef != null) {
                userRef.child("profilePicture").setValue(base64Image).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(getContext(), "Foto profil berhasil diperbarui", Toast.LENGTH_SHORT).show();
                    } else {
                        Toast.makeText(getContext(), "Gagal mengunggah foto", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        } catch (Exception e) {
            e.printStackTrace();
            Toast.makeText(getContext(), "Error memproses gambar", Toast.LENGTH_SHORT).show();
        }
    }

    private void loadProfilePicture() {
        if (userRef != null) {
            userRef.child("profilePicture").addValueEventListener(new ValueEventListener() {
                @Override
                public void onDataChange(@NonNull DataSnapshot snapshot) {
                    if (snapshot.exists() && isAdded()) {
                        String base64Image = snapshot.getValue(String.class);
                        if (base64Image != null && !base64Image.isEmpty()) {
                            try {
                                byte[] decodedString = Base64.decode(base64Image, Base64.DEFAULT);
                                Bitmap decodedByte = BitmapFactory.decodeByteArray(decodedString, 0,
                                        decodedString.length);
                                ivProfilePicture.setImageBitmap(decodedByte);
                            } catch (Exception e) {
                                e.printStackTrace();
                            }
                        }
                    }
                }

                @Override
                public void onCancelled(@NonNull DatabaseError error) {
                }
            });
        }
    }

    private void showAboutDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Tentang Aplikasi")
                .setMessage(
                        "Smart Light Trap v1.0.0\n\nAplikasi monitoring dan kontrol alat perangkap hama pintar untuk pertanian modern.\n\n© 2026 PestLightTrap Team")
                .setPositiveButton("Tutup", null)
                .show();
    }

    private void showHelpDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Bantuan & Dukungan")
                .setMessage(
                        "Jika Anda mengalami kendala, silakan hubungi tim dukungan kami di:\n\nEmail: support@pestlighttrap.com\nWhatsApp: +62 812 3456 7890")
                .setPositiveButton("Tutup", null)
                .show();
    }

    private void showLogoutDialog() {
        new AlertDialog.Builder(requireContext())
                .setTitle("Konfirmasi Keluar")
                .setMessage("Apakah Anda yakin ingin keluar dari aplikasi?")
                .setPositiveButton("Ya, Keluar", (dialog, which) -> {
                    if (mAuth != null) {
                        mAuth.signOut();
                    }
                    Toast.makeText(getContext(), "Berhasil keluar", Toast.LENGTH_SHORT).show();
                    if (getActivity() != null) {
                        startActivity(new Intent(getActivity(), LoginActivity.class));
                        getActivity().finish();
                    }
                })
                .setNegativeButton("Batal", null)
                .show();
    }
}
