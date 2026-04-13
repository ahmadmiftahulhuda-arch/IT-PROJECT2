package com.example.it_project2;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.Toast;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.button.MaterialButton;

import android.content.SharedPreferences;

public class KelolaKeluargaActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_kelola_keluarga);

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

        SessionManager sessionManager = new SessionManager(this);

        // Inisialisasi RadioGroup Siti Aminah
        RadioGroup rgAksesSiti = findViewById(R.id.rgAksesSiti);
        SharedPreferences prefs = getSharedPreferences("FamilyPermissions", MODE_PRIVATE);
        
        // Load status Siti
        boolean isSitiMonitor = prefs.getBoolean("Siti_Monitor", false);
        rgAksesSiti.check(isSitiMonitor ? R.id.rbAksesSitiMonitor : R.id.rbAksesSitiFull);

        rgAksesSiti.setOnCheckedChangeListener((group, checkedId) -> {
            boolean isMonitor = (checkedId == R.id.rbAksesSitiMonitor);
            prefs.edit().putBoolean("Siti_Monitor", isMonitor).apply();
            Toast.makeText(this, "Akses Siti Aminah: " + (isMonitor ? "Monitoring" : "Kontrol Penuh"), Toast.LENGTH_SHORT).show();
        });

        // Inisialisasi RadioGroup Rizky Santoso
        RadioGroup rgAksesRizky = findViewById(R.id.rgAksesRizky);
        
        // Load status Rizky
        boolean isRizkyMonitor = prefs.getBoolean("Rizky_Monitor", false);
        rgAksesRizky.check(isRizkyMonitor ? R.id.rbAksesRizkyMonitor : R.id.rbAksesRizkyFull);

        rgAksesRizky.setOnCheckedChangeListener((group, checkedId) -> {
            boolean isMonitor = (checkedId == R.id.rbAksesRizkyMonitor);
            prefs.edit().putBoolean("Rizky_Monitor", isMonitor).apply();
            Toast.makeText(this, "Akses Rizky Santoso: " + (isMonitor ? "Monitoring" : "Kontrol Penuh"), Toast.LENGTH_SHORT).show();
        });

        // Tombol Simulasi Mode
        MaterialButton btnSimulasi = findViewById(R.id.btnSimulasiMonitoring);
        boolean isCurrentMonitor = sessionManager.getUserAccess().equals(SessionManager.ACCESS_MONITOR);
        if (isCurrentMonitor) {
            btnSimulasi.setText("Hentikan Simulasi (Kembali ke Full Control)");
            btnSimulasi.setTextColor(0xFF16A34A); // Hijau
        }

        btnSimulasi.setOnClickListener(v -> {
            if (sessionManager.getUserAccess().equals(SessionManager.ACCESS_MONITOR)) {
                sessionManager.saveUserAccess(SessionManager.ACCESS_FULL);
                Toast.makeText(this, "Mode Full Control Aktif", Toast.LENGTH_SHORT).show();
                btnSimulasi.setText("Simulasi: Mode Monitoring Saja");
                btnSimulasi.setTextColor(0xFFEF4444);
            } else {
                sessionManager.saveUserAccess(SessionManager.ACCESS_MONITOR);
                Toast.makeText(this, "Mode Monitoring Aktif (Kontrol dinonaktifkan)", Toast.LENGTH_SHORT).show();
                btnSimulasi.setText("Hentikan Simulasi (Kembali ke Full Control)");
                btnSimulasi.setTextColor(0xFF16A34A);
            }
        });

        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        
        // Atur agar tidak ada item yang terpilih secara default jika ini halaman sub-profil
        // Atau tetap arahkan ke nav_profile
        bottomNav.setSelectedItemId(R.id.nav_profile);

        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
                return true;
            } else if (id == R.id.nav_riwayat) {
                startActivity(new Intent(this, RiwayatActivity.class));
                return true;
            } else if (id == R.id.nav_kontrol) {
                startActivity(new Intent(this, KontrolActivity.class));
                return true;
            } else if (id == R.id.nav_informasi) {
                startActivity(new Intent(this, InformasiActivity.class));
                return true;
            } else if (id == R.id.nav_profile) {
                finish(); // Kembali ke profile jika sedang di sub-halaman
                return true;
            }
            return false;
        });
    }
}
