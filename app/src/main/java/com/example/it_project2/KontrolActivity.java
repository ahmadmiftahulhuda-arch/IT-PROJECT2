package com.example.it_project2;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.widget.SwitchCompat;
import androidx.core.content.ContextCompat;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;
import android.widget.LinearLayout;

public class KontrolActivity extends AppCompatActivity {

    private TextView tabOtomatis, tabManual, tvTabDesc, tvSuhuValue;
    private ImageView btnBack, btnSettings;
    private TextView tvPemanahStatus;
    private SeekBar seekBarSuhu;
    private SwitchCompat switchPemanas;
    private BottomNavigationView bottomNav;
    private LinearLayout containerAktivitas;
    private View btnLihatSemua;

    // Firebase
    private DatabaseReference heaterRef;
    private boolean isOtomatisMode = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_kontrol);

        // Inisialisasi Views
        tabOtomatis = findViewById(R.id.tabOtomatis);
        tabManual = findViewById(R.id.tabManual);
        tvTabDesc = findViewById(R.id.tvTabDesc);
        tvSuhuValue = findViewById(R.id.tvSuhuValue);
        tvPemanahStatus = findViewById(R.id.tvPemanahStatus);
        seekBarSuhu = findViewById(R.id.seekBarSuhu);
        switchPemanas = findViewById(R.id.switchPemanas);
        bottomNav = findViewById(R.id.bottomNav);
        btnSettings = findViewById(R.id.btnSettings);
        containerAktivitas = findViewById(R.id.containerAktivitas);
        btnLihatSemua = findViewById(R.id.btnLihatSemua);
        
        // Tombol Pengaturan
        btnSettings.setOnClickListener(v -> {
            startActivity(new Intent(this, ProfileActivity.class));
        });

        // ===== FIREBASE =====
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://smartliving-425c0-default-rtdb.asia-southeast1.firebasedatabase.app");
        heaterRef = database.getReference("heater");

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
                    tvPemanahStatus.setTextColor(ContextCompat.getColor(KontrolActivity.this, 
                        isOn ? R.color.green_safe : R.color.text_gray));
                    
                    switchPemanas.setOnCheckedChangeListener((buttonView, isChecked) -> {
                        heaterRef.child("status").setValue(isChecked);
                    });
                }

                if (threshold != null) {
                    int progress = (int) (threshold - 15);
                    seekBarSuhu.setProgress(Math.max(0, Math.min(15, progress)));
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
            logAktivitas(isChecked ? "Pemanas Dinyalakan" : "Pemanas Dimatikan", 
                "Pengguna mengubah status secara manual", isChecked);
        });

        // Tombol Lihat Semua
        btnLihatSemua.setOnClickListener(v -> {
            startActivity(new Intent(this, AktivitasActivity.class));
        });

        // Load Aktivitas Terkini
        loadAktivitasTerkini();

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
                logAktivitas("Ambang Batas Diubah", 
                    "Target suhu diatur ke " + threshold + "°C", true);
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
            } else if (id == R.id.nav_informasi) {
                startActivity(new Intent(this, InformasiActivity.class));
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                return true;
            }
            return false;
        });

        // ===== ENFORCEMENT AKSES =====
        SessionManager sessionManager = new SessionManager(this);
        if (sessionManager.getUserAccess().equals(SessionManager.ACCESS_MONITOR)) {
            // Disable tabs
            tabOtomatis.setEnabled(false);
            tabManual.setEnabled(false);
            
            // Disable switches
            switchPemanas.setEnabled(false);
            
            // Disable seekbar
            seekBarSuhu.setEnabled(false);
            
            tvTabDesc.setText(tvTabDesc.getText() + "\n(Batas Akses: Monitoring Saja)");
        }
    }

    private void selectTab(boolean isOtomatis) {
        SessionManager sessionManager = new SessionManager(this);
        boolean isMonitor = sessionManager.getUserAccess().equals(SessionManager.ACCESS_MONITOR);

        if (isOtomatis) {
            tabOtomatis.setBackgroundResource(R.drawable.bg_tab_selected);
            tabOtomatis.setTextColor(ContextCompat.getColor(this, R.color.blue_primary));
            tabManual.setBackground(null);
            tabManual.setTextColor(ContextCompat.getColor(this, R.color.text_gray));
            tvTabDesc.setText("Mode Otomatis aktif. Sistem mengatur segalanya.");

            // Disable kontrol manual di mode otomatis
            switchPemanas.setEnabled(false);
            seekBarSuhu.setEnabled(false);
        } else {
            tabManual.setBackgroundResource(R.drawable.bg_tab_selected);
            tabManual.setTextColor(ContextCompat.getColor(this, R.color.blue_primary));
            tabOtomatis.setBackground(null);
            tabOtomatis.setTextColor(ContextCompat.getColor(this, R.color.text_gray));
            tvTabDesc.setText("Mode Manual aktif. Kendali penuh.");

            // Enable kontrol manual jika bukan monitor
            switchPemanas.setEnabled(!isMonitor);
            seekBarSuhu.setEnabled(!isMonitor);
        }

        if (isMonitor) {
            tvTabDesc.setText(tvTabDesc.getText() + "\n(Terbatas: Monitoring Saja)");
            tabOtomatis.setEnabled(false);
            tabManual.setEnabled(false);
        }
    }
    private void logAktivitas(String title, String desc, boolean isActive) {
        DatabaseReference logRef = FirebaseDatabase.getInstance("https://smartliving-425c0-default-rtdb.asia-southeast1.firebasedatabase.app")
            .getReference("aktivitas");
        String id = logRef.push().getKey();
        Aktivitas log = new Aktivitas(title, desc, System.currentTimeMillis(), isActive);
        if (id != null) logRef.child(id).setValue(log);
    }

    private void loadAktivitasTerkini() {
        DatabaseReference logRef = FirebaseDatabase.getInstance("https://smartliving-425c0-default-rtdb.asia-southeast1.firebasedatabase.app")
            .getReference("aktivitas");
        
        // Ambil 3 terbaru
        Query query = logRef.orderByChild("timestamp").limitToLast(3);
        query.addValueEventListener(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(com.google.firebase.database.DataSnapshot snapshot) {
                containerAktivitas.removeAllViews();
                if (!snapshot.exists()) {
                    TextView tv = new TextView(KontrolActivity.this);
                    tv.setText("Belum ada aktivitas.");
                    tv.setGravity(android.view.Gravity.CENTER);
                    containerAktivitas.addView(tv);
                    return;
                }

                for (com.google.firebase.database.DataSnapshot data : snapshot.getChildren()) {
                    Aktivitas item = data.getValue(Aktivitas.class);
                    if (item != null) {
                        addActivityItem(item);
                    }
                }
            }

            @Override
            public void onCancelled(com.google.firebase.database.DatabaseError error) {}
        });
    }

    private void addActivityItem(Aktivitas item) {
        android.view.View view = android.view.LayoutInflater.from(this).inflate(R.layout.item_aktivitas, containerAktivitas, false);
        
        android.view.View dot = view.findViewById(R.id.dot);
        TextView tvTitle = view.findViewById(R.id.tvTitle);
        TextView tvDesc = view.findViewById(R.id.tvDesc);
        TextView tvTime = view.findViewById(R.id.tvTime);

        tvTitle.setText(item.getTitle());
        tvDesc.setText(item.getDescription());
        
        java.text.SimpleDateFormat sdf = new java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault());
        tvTime.setText(sdf.format(new java.util.Date(item.getTimestamp())));

        dot.setBackgroundResource(item.isActive() ? R.drawable.circle_dot_active : R.drawable.circle_dot_inactive);

        // Tambahkan di paling atas agar yang terbaru di atas
        containerAktivitas.addView(view, 0);
    }
}
