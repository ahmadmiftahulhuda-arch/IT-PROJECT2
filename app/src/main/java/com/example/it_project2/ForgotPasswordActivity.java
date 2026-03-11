package com.example.it_project2;

import androidx.appcompat.app.AppCompatActivity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;

public class ForgotPasswordActivity extends AppCompatActivity {

    private EditText etEmail;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forgot_password);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Inisialisasi Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        ImageView btnBack = findViewById(R.id.btnBack);
        etEmail = findViewById(R.id.etEmail);
        TextView btnKirim = findViewById(R.id.btnKirim);
        TextView tvKembaliLogin = findViewById(R.id.tvKembaliLogin);

        btnBack.setOnClickListener(v -> finish());

        tvKembaliLogin.setOnClickListener(v -> finish());

        btnKirim.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();

            // Validasi email
            if (email.isEmpty()) {
                etEmail.setError("Email tidak boleh kosong");
                etEmail.requestFocus();
                return;
            }
            if (!Patterns.EMAIL_ADDRESS.matcher(email).matches()) {
                etEmail.setError("Format email tidak valid");
                etEmail.requestFocus();
                return;
            }

            // Kirim email reset password via Firebase
            sendPasswordReset(email);
        });
    }

    private void sendPasswordReset(String email) {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Mengirim instruksi...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        mAuth.sendPasswordResetEmail(email)
                .addOnCompleteListener(task -> {
                    progressDialog.dismiss();

                    if (task.isSuccessful()) {
                        Toast.makeText(this,
                                "Instruksi pemulihan telah dikirim ke " + email,
                                Toast.LENGTH_LONG).show();
                        finish();
                    } else {
                        String errorMsg = "Gagal mengirim instruksi";
                        if (task.getException() != null && task.getException().getMessage() != null) {
                            String exMsg = task.getException().getMessage();
                            if (exMsg.contains("no user record")) {
                                errorMsg = "Email tidak terdaftar";
                            } else {
                                errorMsg = "Gagal: " + exMsg;
                            }
                        }
                        Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
    }
}
