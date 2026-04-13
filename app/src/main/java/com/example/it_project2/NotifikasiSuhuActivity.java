package com.example.it_project2;

import android.os.Bundle;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;

import com.google.android.material.button.MaterialButton;

public class NotifikasiSuhuActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_notifikasi_suhu);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        TextView btnTutup = findViewById(R.id.btnTutup);
        MaterialButton btnAction = findViewById(R.id.btnAction);
        MaterialButton btnHubungi = findViewById(R.id.btnHubungi);

        btnTutup.setOnClickListener(v -> finish());
        
        btnAction.setOnClickListener(v -> {
            // Logika aksi jika diperlukan
            finish();
        });

        btnHubungi.setOnClickListener(v -> {
            // Logika hubungi keluarga jika diperlukan
            finish();
        });
    }
}
