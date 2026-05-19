package com.example.it_project2;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class AktivitasLoginActivity extends AppCompatActivity {

    private LinearLayout containerLogin;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aktivitas_login);

        ImageView btnBack = findViewById(R.id.btnBack);
        containerLogin = findViewById(R.id.containerLogin);

        btnBack.setOnClickListener(v -> finish());

        com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            String uid = user.getUid();
            com.google.firebase.database.DatabaseReference historyRef = com.google.firebase.database.FirebaseDatabase.getInstance("https://smartliving-425c0-default-rtdb.asia-southeast1.firebasedatabase.app")
                    .getReference("login_history").child(uid);

            historyRef.limitToLast(10).addValueEventListener(new com.google.firebase.database.ValueEventListener() {
                @Override
                public void onDataChange(com.google.firebase.database.DataSnapshot snapshot) {
                    containerLogin.removeAllViews();
                    if (!snapshot.exists()) {
                        addLoginItem("Perangkat Saat Ini", "Belum ada riwayat login", "Aktif", true);
                        return;
                    }

                    long count = snapshot.getChildrenCount();
                    long i = 0;
                    for (com.google.firebase.database.DataSnapshot ds : snapshot.getChildren()) {
                        i++;
                        String device = ds.child("device").getValue(String.class);
                        String location = ds.child("location").getValue(String.class);
                        String time = ds.child("time").getValue(String.class);
                        
                        // Item terakhir dianggap session saat ini
                        boolean isCurrent = (i == count);
                        
                        addLoginItem(device, location, time, isCurrent);
                    }
                }

                @Override
                public void onCancelled(com.google.firebase.database.DatabaseError error) {
                    Toast.makeText(AktivitasLoginActivity.this, "Gagal memuat history", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }

    private void addLoginItem(String device, String loc, String time, boolean isActive) {
        View view = LayoutInflater.from(this).inflate(R.layout.item_aktivitas_login, containerLogin, false);
        
        TextView tvDevice = view.findViewById(R.id.tvDeviceName);
        TextView tvLocTime = view.findViewById(R.id.tvLocTime);
        TextView tvStatus = view.findViewById(R.id.tvStatus);

        tvDevice.setText(device);
        tvLocTime.setText(loc + " • " + time);
        
        if (isActive) {
            tvStatus.setText("Aktif Sekarang");
            tvStatus.setTextColor(0xFF16A34A);
        } else {
            tvStatus.setText("Terakhir aktif: " + time);
            tvStatus.setTextColor(0xFF94A3B8);
        }

        containerLogin.addView(view);
    }
}
