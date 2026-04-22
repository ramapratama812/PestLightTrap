package com.pebelti.pestlighttrap;

import android.content.Intent;
import android.os.Bundle;
import android.text.InputType;
import android.view.MotionEvent;
import android.widget.EditText;
import android.widget.RelativeLayout;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;

public class LoginActivity extends AppCompatActivity {

    private EditText etUsername, etPassword;
    private RelativeLayout btnLogin;

    // Variabel untuk menyimpan status sandi (terlihat atau tidak)
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Menghubungkan ID dari XML
        etUsername = findViewById(R.id.et_username);
        etPassword = findViewById(R.id.et_password);
        btnLogin = findViewById(R.id.btn_login);

        // --- FITUR TAMPILKAN / SEMBUNYIKAN KATA SANDI ---
        etPassword.setOnTouchListener((v, event) -> {
            // Memeriksa apakah aksi yang dilakukan adalah mengangkat jari (selesai mengetuk)
            if (event.getAction() == MotionEvent.ACTION_UP) {
                // Mengecek apakah area yang diketuk berada di bagian kanan (tempat ikon mata berada)
                if (event.getX() >= (etPassword.getWidth() - etPassword.getCompoundPaddingRight())) {

                    if (isPasswordVisible) {
                        // Jika sedang terlihat, maka SEMBUNYIKAN sandi (ubah jadi titik-titik)
                        etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                        isPasswordVisible = false;
                    } else {
                        // Jika sedang tersembunyi, maka TAMPILKAN sandi (ubah jadi teks biasa)
                        etPassword.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                        isPasswordVisible = true;
                    }

                    // Mengembalikan bentuk font (karena biasanya font keriset saat InputType diubah)
                    etPassword.setTypeface(etUsername.getTypeface());

                    // Memastikan kursor pengetikan tetap berada di ujung paling kanan
                    etPassword.setSelection(etPassword.getText().length());

                    return true;
                }
            }
            return false;
        });

        // --- FITUR LOGIN (HARDCODE) ---
        btnLogin.setOnClickListener(v -> {
            String inputUsername = etUsername.getText().toString().trim();
            String inputPassword = etPassword.getText().toString().trim();

            // Logika Hardcode (Username: admin, Password: 12345)
            if (inputUsername.equals("admin") && inputPassword.equals("admin123")) {
                // Pindah ke Halaman Utama
                Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                startActivity(intent);
                finish(); // Tutup halaman login agar tidak bisa di-back
            } else {
                Toast.makeText(LoginActivity.this, "Email atau Kata Sandi salah!", Toast.LENGTH_SHORT).show();
            }
        });
    }
}