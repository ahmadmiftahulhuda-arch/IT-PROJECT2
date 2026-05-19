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
        View btnManageFamily = findViewById(R.id.btnManageFamily);
        View separatorFamily = findViewById(R.id.separatorFamily);
        View btnDetailAkun = findViewById(R.id.btnDetailAkun);
        View btnKeamanan = findViewById(R.id.btnKeamanan);
        View btnAktivitasLogin = findViewById(R.id.btnAktivitasLogin);
        View btnPusatBantuan = findViewById(R.id.btnPusatBantuan);
        TextView tvAvatarInitials = findViewById(R.id.tvAvatarInitials);
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);

        // Set Data User dari Session
        String userName = sessionManager.getUserName();
        tvProfileName.setText(userName);
        
        // Atur Inisial Avatar
        if (userName != null && !userName.isEmpty()) {
            tvAvatarInitials.setText(String.valueOf(userName.charAt(0)).toUpperCase());
        }

        // Atur Role Akses Dinamis
        String accessRole = sessionManager.getUserAccess();
        if (SessionManager.ACCESS_MONITOR.equals(accessRole)) {
            tvProfileSub.setText("Anggota (Monitoring Only)");
            btnManageFamily.setVisibility(View.GONE);
            separatorFamily.setVisibility(View.GONE);
        } else {
            tvProfileSub.setText("Admin Utama (Full Control)");
            btnManageFamily.setVisibility(View.VISIBLE);
            separatorFamily.setVisibility(View.VISIBLE);
        }

        // Click listeners untuk menu baru
        btnDetailAkun.setOnClickListener(v -> startActivity(new Intent(this, DetailAkunActivity.class)));
        btnKeamanan.setOnClickListener(v -> startActivity(new Intent(this, KeamananActivity.class)));
        btnAktivitasLogin.setOnClickListener(v -> startActivity(new Intent(this, AktivitasLoginActivity.class)));
        btnPusatBantuan.setOnClickListener(v -> startActivity(new Intent(this, PusatBantuanActivity.class)));

        // Tombol Manage Family
        btnManageFamily.setOnClickListener(v -> {
            Intent intent = new Intent(ProfileActivity.this, KelolaKeluargaActivity.class);
            startActivity(intent);
        });

        // Tombol Logout dengan Konfirmasi Dialog
        btnLogout.setOnClickListener(v -> {
            new androidx.appcompat.app.AlertDialog.Builder(ProfileActivity.this)
                .setTitle("Konfirmasi Logout")
                .setMessage("Apakah Anda yakin ingin keluar dari akun ini?")
                .setPositiveButton("Ya, Keluar", (dialog, which) -> {
                    sessionManager.logout();
                    Toast.makeText(ProfileActivity.this, "Berhasil Logout", Toast.LENGTH_SHORT).show();
                    
                    // Kembali ke LoginActivity dan hapus tumpukan activity
                    Intent intent = new Intent(ProfileActivity.this, LoginActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    startActivity(intent);
                    finish();
                })
                .setNegativeButton("Batal", null)
                .show();
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
            } else if (id == R.id.nav_informasi) {
                startActivity(new Intent(ProfileActivity.this, InformasiActivity.class));
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
