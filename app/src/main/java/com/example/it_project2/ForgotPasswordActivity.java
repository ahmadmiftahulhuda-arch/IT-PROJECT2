package com.example.it_project2;

import androidx.appcompat.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

public class ForgotPasswordActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        ImageView btnBack = findViewById(R.id.btnBack);
        TextView btnKirim = findViewById(R.id.btnKirim);
        TextView tvKembaliLogin = findViewById(R.id.tvKembaliLogin);

        btnBack.setOnClickListener(v -> finish());
        
        tvKembaliLogin.setOnClickListener(v -> finish());

        btnKirim.setOnClickListener(v -> {
            Toast.makeText(this, "Instruksi pemulihan telah dikirim ke email Anda", Toast.LENGTH_SHORT).show();
        });
    }
}
