package com.example.it_project2;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.material.bottomnavigation.BottomNavigationView;

public class ProfileActivity extends AppCompatActivity {

    private SessionManager sessionManager;
    private TextView tvProfileName, tvProfileSub;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile);

        // Inisialisasi SessionManager
        sessionManager = new SessionManager(this);

        // Hubungkan UI
        tvProfileName = findViewById(R.id.tvProfileName);
        tvProfileSub = findViewById(R.id.tvProfileSub);
        View btnLogout = findViewById(R.id.btnLogout);
        View btnBack = findViewById(R.id.btnBack);
        View btnManageFamily = findViewById(R.id.btnManageFamily);
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);

        // Set Data User dari Session
        tvProfileName.setText(sessionManager.getUserName());
        tvProfileSub.setText("Utama"); // Bisa diganti sesuai kebutuhan

        // Tombol Back
        btnBack.setOnClickListener(v -> finish());

        // Tombol Manage Family
        btnManageFamily.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, KelolaKeluargaActivity.class);
            startActivity(intent);
        });

        // Tombol Logout
        btnLogout.setOnClickListener(v -> {
            sessionManager.logout();
            Toast.makeText(ProfileActivity.this, "Berhasil Logout", Toast.LENGTH_SHORT).show();
            
            // Kembali ke LoginActivity dan hapus tumpukan activity
            Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
            startActivity(intent);
            finish();
        });

        // Konfigurasi Bottom Navigation
        bottomNav.setSelectedItemId(R.id.nav_profile);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(ProfileActivity.this, MainActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_riwayat) {
                startActivity(new Intent(ProfileActivity.this, RiwayatActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_kontrol) {
                startActivity(new Intent(ProfileActivity.this, KontrolActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_edukasi) {
                startActivity(new Intent(ProfileActivity.this, EdukasiActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_profile) {
                return true;
            }
            return false;
        });
    }
}
