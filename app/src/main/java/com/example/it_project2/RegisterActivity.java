package com.example.it_project2;

import androidx.appcompat.app.AppCompatActivity;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.util.Patterns;
import android.view.View;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserProfileChangeRequest;

public class RegisterActivity extends AppCompatActivity {

    private EditText etNama, etEmail, etPassword, etKonfirmasiPassword;
    private TextView btnDaftar, tvMasuk;
    private View btnBack;
    private FirebaseAuth mAuth;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_register);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Inisialisasi Firebase Auth & SessionManager
        mAuth = FirebaseAuth.getInstance();
        sessionManager = new SessionManager(this);

        etNama = findViewById(R.id.etNama);
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        etKonfirmasiPassword = findViewById(R.id.etKonfirmasiPassword);
        btnDaftar = findViewById(R.id.btnDaftar);
        tvMasuk = findViewById(R.id.tvMasuk);
        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        btnDaftar.setOnClickListener(v -> {
            String nama = etNama.getText().toString().trim();
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();
            String konfirmasi = etKonfirmasiPassword.getText().toString().trim();

            // ===== VALIDASI NAMA =====
            if (nama.isEmpty()) {
                etNama.setError("Nama tidak boleh kosong");
                etNama.requestFocus();
                return;
            }

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

            // ===== VALIDASI KONFIRMASI PASSWORD =====
            if (konfirmasi.isEmpty()) {
                etKonfirmasiPassword.setError("Konfirmasi password tidak boleh kosong");
                etKonfirmasiPassword.requestFocus();
                return;
            }
            if (!password.equals(konfirmasi)) {
                etKonfirmasiPassword.setError("Konfirmasi password tidak cocok");
                etKonfirmasiPassword.requestFocus();
                return;
            }

            // ===== REGISTRASI DENGAN FIREBASE =====
            registerWithFirebase(nama, email, password);
        });

        tvMasuk.setOnClickListener(v -> finish());
    }

    private void registerWithFirebase(String nama, String email, String password) {
        // Tampilkan loading dialog
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Mendaftarkan akun...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        mAuth.createUserWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    if (task.isSuccessful()) {
                        // Set display name di Firebase
                        if (mAuth.getCurrentUser() != null) {
                            UserProfileChangeRequest profileUpdate = new UserProfileChangeRequest.Builder()
                                    .setDisplayName(nama)
                                    .build();

                            mAuth.getCurrentUser().updateProfile(profileUpdate)
                                    .addOnCompleteListener(profileTask -> {
                                        progressDialog.dismiss();

                                        // Simpan nama juga ke lokal
                                        sessionManager.saveUserName(nama);

                                        // Logout setelah register agar user login manual
                                        mAuth.signOut();

                                        Toast.makeText(this, "Pendaftaran berhasil! Silakan login 🎉", Toast.LENGTH_SHORT).show();
                                        finish();
                                    });
                        }
                    } else {
                        progressDialog.dismiss();

                        // Handle error
                        String errorMsg = "Pendaftaran gagal";
                        if (task.getException() != null) {
                            String exMsg = task.getException().getMessage();
                            if (exMsg != null) {
                                if (exMsg.contains("email address is already in use")) {
                                    errorMsg = "Email sudah terdaftar, gunakan email lain";
                                } else if (exMsg.contains("email address is badly formatted")) {
                                    errorMsg = "Format email tidak valid";
                                } else if (exMsg.contains("weak password") || exMsg.contains("at least 6 characters")) {
                                    errorMsg = "Password terlalu lemah, minimal 6 karakter";
                                } else {
                                    errorMsg = "Pendaftaran gagal: " + exMsg;
                                }
                            }
                        }
                        Toast.makeText(this, errorMsg, Toast.LENGTH_LONG).show();
                    }
                });
    }
}
