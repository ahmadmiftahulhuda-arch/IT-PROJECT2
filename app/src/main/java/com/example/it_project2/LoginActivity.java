package com.example.it_project2;

import androidx.appcompat.app.AppCompatActivity;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.util.Patterns;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;

public class LoginActivity extends AppCompatActivity {

    private EditText etEmail, etPassword;
    private TextView btnMasuk, tvDaftar, tvLupaPassword;
    private CheckBox cbIngatSaya;
    private FirebaseAuth mAuth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Sembunyikan action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Inisialisasi Firebase Auth
        mAuth = FirebaseAuth.getInstance();

        // Hubungkan ke XML
        etEmail          = findViewById(R.id.etEmail);
        etPassword       = findViewById(R.id.etPassword);
        btnMasuk         = findViewById(R.id.btnMasuk);
        tvDaftar         = findViewById(R.id.tvDaftar);
        tvLupaPassword   = findViewById(R.id.tvLupaPassword);
        cbIngatSaya      = findViewById(R.id.cbIngatSaya);

        // Tombol Masuk diklik
        btnMasuk.setOnClickListener(v -> {
            String email    = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            // ===== VALIDASI EMAIL =====
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

            // ===== VALIDASI PASSWORD =====
            if (password.isEmpty()) {
                etPassword.setError("Password tidak boleh kosong");
                etPassword.requestFocus();
                return;
            }
            if (password.length() < 6) {
                etPassword.setError("Password minimal 6 karakter");
                etPassword.requestFocus();
                return;
            }

            // ===== LOGIN DENGAN FIREBASE =====
            loginWithFirebase(email, password);
        });

        // Link Daftar diklik
        tvDaftar.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, RegisterActivity.class);
            startActivity(intent);
        });

        // Lupa password diklik
        tvLupaPassword.setOnClickListener(v -> {
            Intent intent = new Intent(LoginActivity.this, ForgotPasswordActivity.class);
            startActivity(intent);
        });
    }

    private void loginWithFirebase(String email, String password) {
        // Tampilkan loading dialog
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Sedang masuk...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    progressDialog.dismiss();

                    if (task.isSuccessful()) {
                        // Login berhasil
                        Toast.makeText(this, "Login berhasil! Selamat datang 👋", Toast.LENGTH_SHORT).show();

                        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        // Login gagal
                        String errorMsg = "Login gagal";
                        if (task.getException() != null) {
                            String exMsg = task.getException().getMessage();
                            if (exMsg != null) {
                                if (exMsg.contains("no user record") || exMsg.contains("INVALID_LOGIN_CREDENTIALS")) {
                                    errorMsg = "Email atau password salah";
                                } else if (exMsg.contains("password is invalid")) {
                                    errorMsg = "Password salah";
                                } else if (exMsg.contains("blocked")) {
                                    errorMsg = "Akun diblokir sementara, coba lagi nanti";
                                } else {
                                    errorMsg = "Login gagal: " + exMsg;
                                }
                            }
                        }
                        Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
    }
}
