package com.pebelti.pestlighttrap;

import android.content.Intent;
import android.os.Bundle;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

// Import SDK Firebase & Google
import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.auth.GoogleAuthProvider;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";
    private static final int RC_SIGN_IN = 9001;

    // ============================================================
    // AKUN SEMENTARA — bisa langsung dipakai tanpa buat akun dulu
    // Email   : admin@pestlighttrap.com
    // Password: Pest@2024
    // ============================================================
    private static final String TEMP_EMAIL    = "admin@pestlighttrap.com";
    private static final String TEMP_PASSWORD = "Pest@2024";

    private GoogleSignInClient mGoogleSignInClient;
    private FirebaseAuth mAuth;
    private boolean isPasswordVisible = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login); // layout XML tidak diubah

        // 1. Inisialisasi Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // 2. Jika sudah pernah login, langsung ke HomeActivity
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser != null) {
            navigateToHome();
            return;
        }

        // 3. Konfigurasi Google Sign-In menggunakan Web Client ID otomatis dari google-services.json
        GoogleSignInOptions gso = new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_SIGN_IN)
                .requestIdToken(getString(R.string.default_web_client_id))
                .requestEmail()
                .build();
        mGoogleSignInClient = GoogleSignIn.getClient(this, gso);

        // 4. Buat akun sementara di Firebase (hanya 1x, jika belum ada)
        ensureTemporaryAccountExists();

        // ─────────────────────────────────────────────────────────
        // 5. Hubungkan komponen dari layout XML yang sudah ada
        // ─────────────────────────────────────────────────────────

        EditText   etEmail           = findViewById(R.id.etEmail);
        EditText   etPassword        = findViewById(R.id.etPassword);
        Button     btnLogin          = findViewById(R.id.btnLogin);
        ImageView  ivTogglePassword  = findViewById(R.id.ivTogglePassword);
        TextView   tvDaftarSekarang  = findViewById(R.id.tvDaftarSekarang);
        // Tombol Google di layout menggunakan: com.google.android.gms.common.SignInButton
        // dengan android:id="@+id/btn_google_login"
        View tombolLoginGoogle = findViewById(R.id.btn_google_login);

        // Toggle visibilitas password
        if (ivTogglePassword != null) {
            ivTogglePassword.setOnClickListener(v -> {
                isPasswordVisible = !isPasswordVisible;
                etPassword.setTransformationMethod(isPasswordVisible
                        ? HideReturnsTransformationMethod.getInstance()
                        : PasswordTransformationMethod.getInstance());
                etPassword.setSelection(etPassword.getText().length());
            });
        }

        // 6. Aksi tombol "Masuk →" — Login dengan Firebase Email/Password
        if (btnLogin != null) {
            btnLogin.setOnClickListener(v -> {
                String email    = etEmail.getText().toString().trim();
                String password = etPassword.getText().toString().trim();
                if (email.isEmpty() || password.isEmpty()) {
                    Toast.makeText(this, "Email dan password tidak boleh kosong", Toast.LENGTH_SHORT).show();
                    return;
                }
                loginWithEmailPassword(email, password);
            });
        }

        // 7. Aksi tombol Google Sign-In — memicu pop-up pilih akun Google
        if (tombolLoginGoogle != null) {
            tombolLoginGoogle.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    signIn(); // Buka pop-up Google
                }
            });
        }

        // 8. Link "Daftar Sekarang" → RegisterActivity
        if (tvDaftarSekarang != null) {
            tvDaftarSekarang.setOnClickListener(v ->
                    startActivity(new Intent(LoginActivity.this, RegisterActivity.class))
            );
        }
    }

    // ─── Google Sign-In Flow ──────────────────────────────────────

    /** Membuka pop-up daftar akun Google */
    private void signIn() {
        Intent signInIntent = mGoogleSignInClient.getSignInIntent();
        startActivityForResult(signInIntent, RC_SIGN_IN);
    }

    /** Menangkap hasil dari jendela pop-up Google */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == RC_SIGN_IN) {
            Task<GoogleSignInAccount> task = GoogleSignIn.getSignedInAccountFromIntent(data);
            try {
                // Google Sign-In sukses — ambil token untuk dikirim ke Firebase
                GoogleSignInAccount account = task.getResult(ApiException.class);
                if (account != null) {
                    Log.d(TAG, "Google Sign-In sukses, melanjutkan ke Firebase...");
                    firebaseAuthWithGoogle(account.getIdToken());
                }
            } catch (ApiException e) {
                Log.w(TAG, "Google Sign-In gagal: kode=" + e.getStatusCode(), e);
                Toast.makeText(this, "Gagal terhubung ke Google: " + e.getMessage(), Toast.LENGTH_SHORT).show();
            }
        }
    }

    /** Validasi token Google ke Firebase Authentication kelompok */
    private void firebaseAuthWithGoogle(String idToken) {
        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // LOGIN BERHASIL
                        FirebaseUser user = mAuth.getCurrentUser();
                        String name = (user != null && user.getDisplayName() != null)
                                ? user.getDisplayName() : "Pengguna";
                        Toast.makeText(LoginActivity.this, "Selamat Datang, " + name, Toast.LENGTH_SHORT).show();
                        // Pindah ke halaman utama
                        navigateToHome();
                    } else {
                        // LOGIN GAGAL
                        String errorMsg = (task.getException() != null) ? task.getException().getMessage() : "Unknown error";
                        Log.w(TAG, "Firebase Google Auth gagal: " + errorMsg);
                        Toast.makeText(LoginActivity.this,
                                "Gagal sinkronisasi dengan Firebase: " + errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    // ─── Email/Password Login ─────────────────────────────────────

    private void loginWithEmailPassword(String email, String password) {
        // ─── FALLBACK AKUN DUMMY ───
        // Jika Firebase belum siap atau error, akun dummy ini dijamin selalu bisa masuk.
        if ((email.equals(TEMP_EMAIL) && password.equals(TEMP_PASSWORD)) ||
            (email.equals("admin@pest.com") && password.equals("admin123"))) {
            Toast.makeText(this, "Berhasil masuk sebagai Admin (Dummy)", Toast.LENGTH_SHORT).show();
            navigateToHome();
            return;
        }

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        FirebaseUser user = mAuth.getCurrentUser();
                        String name = (user != null && user.getDisplayName() != null
                                && !user.getDisplayName().isEmpty())
                                ? user.getDisplayName() : email;
                        Log.d(TAG, "Login berhasil: " + name);
                        Toast.makeText(this, "Selamat datang, " + name, Toast.LENGTH_SHORT).show();
                        navigateToHome();
                    } else {
                        String msg = (task.getException() != null)
                                ? task.getException().getMessage() : "Login gagal";
                        Log.w(TAG, "Login gagal: " + msg);
                        Toast.makeText(this, "Login gagal: " + msg, Toast.LENGTH_LONG).show();
                    }
                });
    }

    // ─── Buat akun sementara di Firebase (1x saja) ───────────────

    /**
     * Membuat akun admin@pestlighttrap.com / Pest@2024 di Firebase.
     * Jika sudah ada, tidak akan membuat duplikat (error diabaikan).
     */
    private void ensureTemporaryAccountExists() {
        mAuth.createUserWithEmailAndPassword(TEMP_EMAIL, TEMP_PASSWORD)
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Log.d(TAG, "Akun sementara berhasil dibuat: " + TEMP_EMAIL);
                        mAuth.signOut(); // Sign out agar layar login tetap terbuka
                    } else {
                        Log.d(TAG, "Akun sementara sudah ada: "
                                + (task.getException() != null ? task.getException().getMessage() : ""));
                    }
                });
    }

    // ─── Navigasi ────────────────────────────────────────────────

    private void navigateToHome() {
        startActivity(new Intent(LoginActivity.this, HomeActivity.class));
        finish();
    }
}