package com.example.it_project2;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Switch;
import android.widget.TextView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextView tvHalo, tvStatus, tvStatusDesc, tvSuhu, tvKelembapan, tvPM25, tvGas;
    private Switch switchHeater;
    private BottomNavigationView bottomNav;

    // Firebase Realtime Database
    private DatabaseReference sensorRef;
    private DatabaseReference heaterRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Bind semua view
        tvHalo       = findViewById(R.id.tvHalo);
        tvStatus     = findViewById(R.id.tvStatus);
        tvStatusDesc = findViewById(R.id.tvStatusDesc);
        tvSuhu       = findViewById(R.id.tvSuhu);
        tvKelembapan = findViewById(R.id.tvKelembapan);
        tvPM25       = findViewById(R.id.tvPM25);
        tvGas        = findViewById(R.id.tvGas);
        switchHeater = findViewById(R.id.switchHeater);
        bottomNav    = findViewById(R.id.bottomNav);

        // Tampilkan nama user yang login
        SessionManager sessionManager = new SessionManager(this);
        String userName = sessionManager.getUserName();
        if (!userName.isEmpty()) {
            tvHalo.setText("Halo, " + userName + " 👋");
        }

        // ===== FIREBASE REALTIME DATABASE =====
        // URL dari Firebase Console → Realtime Database (bagian atas halaman)
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://smartliving-425c0-default-rtdb.asia-southeast1.firebasedatabase.app");
        sensorRef = database.getReference("sensor");
        heaterRef = database.getReference("heater");

        // Listener untuk data sensor (real-time)
        sensorRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                // Ambil data suhu
                Double suhu = snapshot.child("suhu").getValue(Double.class);
                Double kelembapan = snapshot.child("kelembapan").getValue(Double.class);

                if (suhu != null) {
                    tvSuhu.setText(String.format(Locale.getDefault(), "%.1f°C", suhu));
                    updateStatusCard(suhu);
                }

                if (kelembapan != null) {
                    tvKelembapan.setText(String.format(Locale.getDefault(), "%.0f%%", kelembapan));
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                tvStatus.setText("ERROR");
                tvStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                tvStatusDesc.setText("Gagal membaca data sensor");
            }
        });

        // Listener untuk status heater
        heaterRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Boolean isOn = snapshot.child("status").getValue(Boolean.class);
                if (isOn != null) {
                    // Update switch tanpa trigger listener
                    switchHeater.setOnCheckedChangeListener(null);
                    switchHeater.setChecked(isOn);
                    switchHeater.setOnCheckedChangeListener((buttonView, isChecked) -> {
                        heaterRef.child("status").setValue(isChecked);
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Handle error
            }
        });

        // Switch heater manual toggle
        switchHeater.setOnCheckedChangeListener((buttonView, isChecked) -> {
            heaterRef.child("status").setValue(isChecked);
        });

        // Set active tab
        bottomNav.setSelectedItemId(R.id.nav_home);

        // Navigasi bottom nav
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_riwayat) {
                startActivity(new Intent(this, RiwayatActivity.class));
            } else if (id == R.id.nav_kontrol) {
                startActivity(new Intent(this, KontrolActivity.class));
            } else if (id == R.id.nav_edukasi) {
                startActivity(new Intent(this, EdukasiActivity.class));
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
            }
            return true;
        });
    }

    /**
     * Update card status berdasarkan suhu.
     * - Hijau (AMAN): suhu >= 24°C
     * - Kuning (WASPADA): suhu 20-23°C
     * - Merah (BAHAYA): suhu < 20°C
     */
    private void updateStatusCard(double suhu) {
        if (suhu >= 24) {
            tvStatus.setText("AMAN");
            tvStatus.setTextColor(0xFF16A34A); // Hijau
            tvStatusDesc.setText("Suhu ruangan nyaman");
        } else if (suhu >= 20) {
            tvStatus.setText("WASPADA");
            tvStatus.setTextColor(0xFFF59E0B); // Kuning
            tvStatusDesc.setText("Suhu mulai menurun, perhatikan kondisi");
        } else {
            tvStatus.setText("BAHAYA");
            tvStatus.setTextColor(0xFFDC2626); // Merah
            tvStatusDesc.setText("Suhu terlalu dingin! Risiko serangan asma");
        }
    }
}
