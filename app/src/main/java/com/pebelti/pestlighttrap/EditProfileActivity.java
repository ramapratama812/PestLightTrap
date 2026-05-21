package com.pebelti.pestlighttrap;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.EmailAuthProvider;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

public class EditProfileActivity extends AppCompatActivity {

    private EditText etCurrentPassword, etNewEmail, etNewPassword;
    private Button btnSaveProfile;
    private ImageView btnBack;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_profile);

        mAuth = FirebaseAuth.getInstance();
        FirebaseUser user = mAuth.getCurrentUser();

        etCurrentPassword = findViewById(R.id.etCurrentPassword);
        etNewEmail = findViewById(R.id.etNewEmail);
        etNewPassword = findViewById(R.id.etNewPassword);
        btnSaveProfile = findViewById(R.id.btnSaveProfile);
        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());
        
        if (user != null) {
            etNewEmail.setText(user.getEmail());
        }

        btnSaveProfile.setOnClickListener(v -> {
            String currentPassword = etCurrentPassword.getText().toString().trim();
            String newEmail = etNewEmail.getText().toString().trim();
            String newPassword = etNewPassword.getText().toString().trim();

            if (TextUtils.isEmpty(currentPassword)) {
                etCurrentPassword.setError("Sandi saat ini wajib diisi untuk verifikasi");
                return;
            }

            if (user != null && user.getEmail() != null) {
                AuthCredential credential = EmailAuthProvider.getCredential(user.getEmail(), currentPassword);
                user.reauthenticate(credential).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        updateProfile(user, newEmail, newPassword);
                    } else {
                        Toast.makeText(EditProfileActivity.this, "Sandi saat ini salah. Gagal memverifikasi.", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }

    private void updateProfile(FirebaseUser user, String newEmail, String newPassword) {
        boolean emailChanged = !newEmail.equals(user.getEmail()) && !TextUtils.isEmpty(newEmail);
        boolean passwordChanged = !TextUtils.isEmpty(newPassword);

        if (emailChanged) {
            user.updateEmail(newEmail).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(EditProfileActivity.this, "Email berhasil diperbarui", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(EditProfileActivity.this, "Gagal memperbarui email", Toast.LENGTH_SHORT).show();
                }
            });
        }

        if (passwordChanged) {
            user.updatePassword(newPassword).addOnCompleteListener(task -> {
                if (task.isSuccessful()) {
                    Toast.makeText(EditProfileActivity.this, "Kata sandi berhasil diperbarui", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(EditProfileActivity.this, "Gagal memperbarui kata sandi", Toast.LENGTH_SHORT).show();
                }
            });
        }
        
        if (!emailChanged && !passwordChanged) {
            Toast.makeText(EditProfileActivity.this, "Tidak ada data yang diubah", Toast.LENGTH_SHORT).show();
        } else {
            finish();
        }
    }
}
