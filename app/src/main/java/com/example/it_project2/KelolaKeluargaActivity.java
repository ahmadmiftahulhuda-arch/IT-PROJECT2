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

        // Setup Container List Dinamis
        android.widget.LinearLayout llFamilyMembers = findViewById(R.id.llFamilyMembers);
        com.google.firebase.database.DatabaseReference familyRef = com.google.firebase.database.FirebaseDatabase.getInstance("https://smartliving-425c0-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("family_members");

        // Listener untuk membaca daftar keluarga secara realtime dari Firebase
        familyRef.addValueEventListener(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(@androidx.annotation.NonNull com.google.firebase.database.DataSnapshot snapshot) {
                llFamilyMembers.removeAllViews();
                for (com.google.firebase.database.DataSnapshot memberSnap : snapshot.getChildren()) {
                    String email = memberSnap.child("email").getValue(String.class);
                    String access = memberSnap.child("access").getValue(String.class);
                    if (email == null) continue;

                    // Buat view untuk setiap member
                    View memberView = getLayoutInflater().inflate(R.layout.item_family_member, llFamilyMembers, false);
                    android.widget.TextView tvEmail = memberView.findViewById(R.id.tvMemberEmail);
                    RadioGroup rgAkses = memberView.findViewById(R.id.rgAksesMember);
                    android.widget.ImageView btnRemove = memberView.findViewById(R.id.btnRemoveMember);

                    tvEmail.setText(email);
                    if ("FULL".equals(access)) {
                        rgAkses.check(R.id.rbAksesFull);
                    } else {
                        rgAkses.check(R.id.rbAksesMonitor);
                    }

                    // Aksi ketika akses diubah
                    rgAkses.setOnCheckedChangeListener((group, checkedId) -> {
                        String newAccess = (checkedId == R.id.rbAksesFull) ? "FULL" : "MONITOR";
                        memberSnap.getRef().child("access").setValue(newAccess);
                        Toast.makeText(KelolaKeluargaActivity.this, "Akses " + email + " diubah jadi " + newAccess, Toast.LENGTH_SHORT).show();
                    });

                    // Aksi untuk hapus (Edit icon diganti fungsi hapus sementara)
                    btnRemove.setOnClickListener(v -> {
                        memberSnap.getRef().removeValue();
                        Toast.makeText(KelolaKeluargaActivity.this, email + " dihapus", Toast.LENGTH_SHORT).show();
                    });

                    llFamilyMembers.addView(memberView);
                }
            }

            @Override
            public void onCancelled(@androidx.annotation.NonNull com.google.firebase.database.DatabaseError error) {
                Toast.makeText(KelolaKeluargaActivity.this, "Gagal memuat daftar keluarga", Toast.LENGTH_SHORT).show();
            }
        });

        // Logika Undang via Email (Simpan ke Firebase)
        android.widget.EditText etInviteEmail = findViewById(R.id.etInviteEmail);
        com.google.android.material.button.MaterialButton btnKirimUndangan = findViewById(R.id.btnKirimUndangan);
        
        btnKirimUndangan.setOnClickListener(v -> {
            String email = etInviteEmail.getText().toString().trim();
            if (email.isEmpty()) {
                Toast.makeText(this, "Silakan masukkan alamat email", Toast.LENGTH_SHORT).show();
                return;
            }
            if (!android.util.Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                Toast.makeText(this, "Format email tidak valid", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // Generate valid Firebase Key
            String key = email.replace(".", ",");
            familyRef.child(key).child("email").setValue(email);
            familyRef.child(key).child("access").setValue("MONITOR");

            Toast.makeText(this, "Berhasil mengundang " + email + " !", Toast.LENGTH_SHORT).show();
            etInviteEmail.setText("");
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
