package com.example.it_project2;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class KeamananActivity extends AppCompatActivity {

    private EditText etCurrentPass, etNewPass, etConfirmPass;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_keamanan);

        ImageView btnBack = findViewById(R.id.btnBack);
        etCurrentPass = findViewById(R.id.etCurrentPass);
        etNewPass = findViewById(R.id.etNewPass);
        etConfirmPass = findViewById(R.id.etConfirmPass);
        android.view.View btnUpdatePass = findViewById(R.id.btnUpdatePass);

        btnBack.setOnClickListener(v -> finish());

        btnUpdatePass.setOnClickListener(v -> {
            String current = etCurrentPass.getText().toString();
            String newP = etNewPass.getText().toString();
            String confirm = etConfirmPass.getText().toString();

            if (current.isEmpty() || newP.isEmpty() || confirm.isEmpty()) {
                Toast.makeText(this, "Mohon isi semua field", Toast.LENGTH_SHORT).show();
                return;
            }

            if (!newP.equals(confirm)) {
                Toast.makeText(this, "Konfirmasi kata sandi tidak cocok", Toast.LENGTH_SHORT).show();
                return;
            }

            com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                user.updatePassword(newP).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Kata sandi berhasil disinkronkan ke Firebase", Toast.LENGTH_SHORT).show();
                        finish();
                    } else {
                        Toast.makeText(this, "Gagal update password (butuh login ulang)", Toast.LENGTH_LONG).show();
                    }
                });
            }
        });
    }
}
