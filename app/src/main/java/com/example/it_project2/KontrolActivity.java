package com.example.it_project2;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.SwitchCompat;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

public class KontrolActivity extends AppCompatActivity {

    private TextView tabOtomatis, tabManual, tvTabDesc, tvSuhuValue, btnBack;
    private TextView tvPemanahStatus;
    private SeekBar seekBarSuhu;
    private SwitchCompat switchPemanas, switchPembersih;
    private BottomNavigationView bottomNav;

    // Firebase
    private DatabaseReference heaterRef;
    private DatabaseReference sensorRef;
    private boolean isOtomatisMode = false;

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
        tvPemanahStatus = findViewById(R.id.tvPemanahStatus);
        seekBarSuhu = findViewById(R.id.seekBarSuhu);
        switchPemanas = findViewById(R.id.switchPemanas);
        switchPembersih = findViewById(R.id.switchPembersih);
        bottomNav = findViewById(R.id.bottomNav);

        // ===== FIREBASE =====
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://smartliving-425c0-default-rtdb.asia-southeast1.firebasedatabase.app");
        heaterRef = database.getReference("heater");
        sensorRef = database.getReference("sensor");

        // Listener status heater dari Firebase
        heaterRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Boolean isOn = snapshot.child("status").getValue(Boolean.class);
                Long threshold = snapshot.child("threshold").getValue(Long.class);
                String mode = snapshot.child("mode").getValue(String.class);

                if (isOn != null) {
                    // Update switch tanpa trigger listener
                    switchPemanas.setOnCheckedChangeListener(null);
                    switchPemanas.setChecked(isOn);
                    tvPemanahStatus.setText(isOn ? "● Aktif" : "● Nonaktif");
                    tvPemanahStatus.setTextColor(isOn ? 0xFF16A34A : 0xFF94A3B8);
                    switchPemanas.setOnCheckedChangeListener((buttonView, isChecked) -> {
                        heaterRef.child("status").setValue(isChecked);
                    });
                }

                if (threshold != null) {
                    int progress = (int) (threshold - 15);
                    seekBarSuhu.setProgress(Math.max(0, Math.min(20, progress)));
                    tvSuhuValue.setText(threshold + "°C");
                }

                if (mode != null) {
                    isOtomatisMode = mode.equals("otomatis");
                    selectTab(isOtomatisMode);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(KontrolActivity.this, "Gagal membaca data heater", Toast.LENGTH_SHORT).show();
            }
        });

        // Tombol Kembali
        btnBack.setOnClickListener(v -> finish());

        // Logika Perpindahan Tab
        tabOtomatis.setOnClickListener(v -> {
            isOtomatisMode = true;
            selectTab(true);
            heaterRef.child("mode").setValue("otomatis");
        });
        tabManual.setOnClickListener(v -> {
            isOtomatisMode = false;
            selectTab(false);
            heaterRef.child("mode").setValue("manual");
        });

        // Switch Pemanas → kirim ke Firebase
        switchPemanas.setOnCheckedChangeListener((buttonView, isChecked) -> {
            heaterRef.child("status").setValue(isChecked);
        });

        // Logika SeekBar Suhu (ambang batas)
        seekBarSuhu.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                int suhu = 15 + progress;
                tvSuhuValue.setText(suhu + "°C");
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {}

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Simpan threshold ke Firebase saat user selesai geser
                int threshold = 15 + seekBar.getProgress();
                heaterRef.child("threshold").setValue(threshold);
                Toast.makeText(KontrolActivity.this,
                        "Ambang batas diset ke " + threshold + "°C", Toast.LENGTH_SHORT).show();
            }
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

            // Disable kontrol manual
            switchPemanas.setEnabled(false);
            seekBarSuhu.setEnabled(true);
        } else {
            tabManual.setBackgroundResource(R.drawable.bg_tab_selected);
            tabManual.setTextColor(getResources().getColor(R.color.blue_primary));
            tabOtomatis.setBackground(null);
            tabOtomatis.setTextColor(getResources().getColor(R.color.text_gray));
            tvTabDesc.setText("Mode Manual aktif. Kendali penuh.");

            // Enable kontrol manual
            switchPemanas.setEnabled(true);
            seekBarSuhu.setEnabled(true);
        }
    }
}
