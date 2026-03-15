package com.example.it_project2;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class KelolaKeluargaActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_kelola_keluarga);

        ImageView btnBack = findViewById(R.id.btnBack);
        btnBack.setOnClickListener(v -> finish());

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
            } else if (id == R.id.nav_edukasi) {
                startActivity(new Intent(this, EdukasiActivity.class));
                return true;
            } else if (id == R.id.nav_profile) {
                finish(); // Kembali ke profile jika sedang di sub-halaman
                return true;
            }
            return false;
        });
    }
}
