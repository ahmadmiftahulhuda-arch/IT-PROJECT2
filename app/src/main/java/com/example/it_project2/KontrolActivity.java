package com.example.it_project2;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.SeekBar;
import androidx.appcompat.widget.SwitchCompat;
import android.widget.TextView;
import com.google.android.material.bottomnavigation.BottomNavigationView;

public class KontrolActivity extends AppCompatActivity {

    private TextView tabOtomatis, tabManual, tvTabDesc, tvSuhuValue, btnBack;
    private SeekBar seekBarSuhu;
    private SwitchCompat switchPemanas, switchPembersih;
    private BottomNavigationView bottomNav;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_kontrol);

        // Inisialisasi Views
        btnBack = findViewById(R.id.btnBack);
        tabOtomatis = findViewById(R.id.tabOtomatis);
        tabManual = findViewById(R.id.tabManual);
        tvTabDesc = findViewById(R.id.tvTabDesc);
        tvSuhuValue = findViewById(R.id.tvSuhuValue);
        seekBarSuhu = findViewById(R.id.seekBarSuhu);
        switchPemanas = findViewById(R.id.switchPemanas);
        switchPembersih = findViewById(R.id.switchPembersih);
        bottomNav = findViewById(R.id.bottomNav);

        // Tombol Kembali
        btnBack.setOnClickListener(v -> finish());

        // Logika Perpindahan Tab
        tabOtomatis.setOnClickListener(v -> selectTab(true));
        tabManual.setOnClickListener(v -> selectTab(false));

        // Logika SeekBar Suhu
        seekBarSuhu.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int suhu = 15 + progress; // Berdasarkan rentang 15-35 (max 20)
                tvSuhuValue.setText(suhu + "°C");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {}
        });

        // Bottom Navigation
        bottomNav.setSelectedItemId(R.id.nav_kontrol);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
                return true;
            } else if (id == R.id.nav_riwayat) {
                startActivity(new Intent(this, RiwayatActivity.class));
                return true;
            } else if (id == R.id.nav_kontrol) {
                return true;
            } else if (id == R.id.nav_edukasi) {
                startActivity(new Intent(this, EdukasiActivity.class));
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                return true;
            }
            return false;
        });
    }

    private void selectTab(boolean isOtomatis) {
        if (isOtomatis) {
            tabOtomatis.setBackgroundResource(R.drawable.bg_tab_selected);
            tabOtomatis.setTextColor(getResources().getColor(R.color.blue_primary));
            tabManual.setBackground(null);
            tabManual.setTextColor(getResources().getColor(R.color.text_gray));
            tvTabDesc.setText("Mode Otomatis aktif. Sistem mengatur segalanya.");
        } else {
            tabManual.setBackgroundResource(R.drawable.bg_tab_selected);
            tabManual.setTextColor(getResources().getColor(R.color.blue_primary));
            tabOtomatis.setBackground(null);
            tabOtomatis.setTextColor(getResources().getColor(R.color.text_gray));
            tvTabDesc.setText("Mode Manual aktif. Kendali penuh.");
        }
    }
}
