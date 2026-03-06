package com.example.it_project2;


import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.TextView;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class MainActivity extends AppCompatActivity {

    private TextView tvHalo, tvStatus, tvSuhu, tvKelembapan, tvPM25, tvGas;
    private BottomNavigationView bottomNav;

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
        tvSuhu       = findViewById(R.id.tvSuhu);
        tvKelembapan = findViewById(R.id.tvKelembapan);
        tvPM25       = findViewById(R.id.tvPM25);
        tvGas        = findViewById(R.id.tvGas);
        bottomNav    = findViewById(R.id.bottomNav);

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
}
