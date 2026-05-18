package com.pebelti.pestlighttrap;

import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;

// Import Firebase Database
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class DashboardActivity extends AppCompatActivity {

    // Deklarasi variabel
    private TextView tvKapasitasAir;
    private DatabaseReference databaseReference;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_dashboard); // Sesuaikan dengan layout Anda

        // Hubungkan TextView dari XML (Pastikan Anda punya TextView dengan ID ini di XML)
        tvKapasitasAir = findViewById(R.id.tv_kapasitas_air);

        // 1. Inisialisasi koneksi ke Firebase Database
        FirebaseDatabase database = FirebaseDatabase.getInstance();
        
        // 2. Tunjuk (Reference) spesifik ke nama data yang ingin dibaca dari database IoT
        // Ganti "kapasitas_air" dengan nama key/path yang persis ada di Firebase Console Anda
        databaseReference = database.getReference("kapasitas_air");

        // 3. Baca data secara Real-Time
        databaseReference.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(@NonNull DataSnapshot snapshot) {
                // Fungsi ini akan otomatis terpanggil setiap kali data di Firebase berubah
                if (snapshot.exists()) {
                    // Ambil nilainya (misal nilainya berupa angka/String)
                    String nilaiKapasitas = String.valueOf(snapshot.getValue());
                    
                    // Tampilkan ke layar aplikasi
                    tvKapasitasAir.setText(nilaiKapasitas + " %");
                }
            }

            @Override
            public void onCancelled(@NonNull DatabaseError error) {
                // Fungsi ini terpanggil jika terjadi error (misal koneksi putus atau rules ditolak)
                Toast.makeText(DashboardActivity.this, "Gagal membaca data: " + error.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
}
