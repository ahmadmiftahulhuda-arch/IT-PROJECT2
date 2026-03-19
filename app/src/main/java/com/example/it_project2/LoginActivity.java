package com.example.it_project2;

import androidx.appcompat.app.AppCompatActivity;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;

import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.widget.CheckBox;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.libraries.identity.googleid.GetGoogleIdOption;
import com.google.android.libraries.identity.googleid.GoogleIdTokenCredential;
import com.google.firebase.auth.AuthCredential;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.GoogleAuthProvider;

import java.util.concurrent.Executors;

public class LoginActivity extends AppCompatActivity {

    private static final String TAG = "LoginActivity";

    private EditText etEmail, etPassword;
    private TextView btnMasuk, tvDaftar, tvLupaPassword;
    private View btnGoogle;
    private CheckBox cbIngatSaya;
    private FirebaseAuth mAuth;
    private CredentialManager credentialManager;

    // Web Client ID dari Firebase Console → Authentication → Sign-in method →
    // Google
    // PENTING: Ganti dengan Web Client ID kamu sendiri!
    private static final String WEB_CLIENT_ID = "621146485046-c6d2mvgea7v7h2is59csnuk9gq5ij3b3.apps.googleusercontent.com";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_login);

        // Sembunyikan action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Inisialisasi Firebase Auth & Credential Manager
        mAuth = FirebaseAuth.getInstance();
        credentialManager = CredentialManager.create(this);

        // Hubungkan ke XML
        etEmail = findViewById(R.id.etEmail);
        etPassword = findViewById(R.id.etPassword);
        btnMasuk = findViewById(R.id.btnMasuk);
        tvDaftar = findViewById(R.id.tvDaftar);
        tvLupaPassword = findViewById(R.id.tvLupaPassword);
        cbIngatSaya = findViewById(R.id.cbIngatSaya);
        btnGoogle = findViewById(R.id.btnGoogle);

        // ===== TOMBOL MASUK (Email + Password) =====
        btnMasuk.setOnClickListener(v -> {
            String email = etEmail.getText().toString().trim();
            String password = etPassword.getText().toString().trim();

            // Validasi Email
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

            // Validasi Password
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

            // Login dengan Firebase
            loginWithFirebase(email, password);
        });

        // ===== TOMBOL GOOGLE SSO =====
        btnGoogle.setOnClickListener(v -> signInWithGoogle());

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

    // ==================== LOGIN EMAIL + PASSWORD ====================

    private void loginWithFirebase(String email, String password) {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Sedang masuk...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        mAuth.signInWithEmailAndPassword(email, password)
                .addOnCompleteListener(this, task -> {
                    progressDialog.dismiss();

                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Login berhasil! Selamat datang 👋", Toast.LENGTH_SHORT).show();
                        navigateToMain();
                    } else {
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

    // ==================== LOGIN GOOGLE SSO ====================

    private void signInWithGoogle() {
        // Buat Google ID option
        GetGoogleIdOption googleIdOption = new GetGoogleIdOption.Builder()
                .setFilterByAuthorizedAccounts(false) // Tampilkan semua akun Google
                .setServerClientId(WEB_CLIENT_ID)
                .setAutoSelectEnabled(true)
                .build();

        // Buat credential request
        GetCredentialRequest request = new GetCredentialRequest.Builder()
                .addCredentialOption(googleIdOption)
                .build();

        // Tampilkan loading
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Menghubungkan ke Google...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Minta credential
        credentialManager.getCredentialAsync(
                this,
                request,
                new CancellationSignal(),
                Executors.newSingleThreadExecutor(),
                new CredentialManagerCallback<GetCredentialResponse, GetCredentialException>() {
                    @Override
                    public void onResult(GetCredentialResponse result) {
                        runOnUiThread(() -> {
                            progressDialog.dismiss();
                            handleGoogleSignInResult(result);
                        });
                    }

                    @Override
                    public void onError(GetCredentialException e) {
                        runOnUiThread(() -> {
                            progressDialog.dismiss();
                            Log.e(TAG, "Google Sign-In gagal", e);
                            Toast.makeText(LoginActivity.this,
                                    "Login Google dibatalkan atau gagal",
                                    Toast.LENGTH_SHORT).show();
                        });
                    }
                });
    }

    private void handleGoogleSignInResult(GetCredentialResponse response) {
        // Cek apakah credential yang diterima adalah Google ID Token
        if (response.getCredential() instanceof CustomCredential) {
            CustomCredential credential = (CustomCredential) response.getCredential();

            if (GoogleIdTokenCredential.TYPE_GOOGLE_ID_TOKEN_CREDENTIAL.equals(credential.getType())) {
                // Ambil Google ID Token
                GoogleIdTokenCredential googleCredential = GoogleIdTokenCredential.createFrom(credential.getData());
                String idToken = googleCredential.getIdToken();

                // Autentikasi ke Firebase dengan Google credential
                firebaseAuthWithGoogle(idToken);
            } else {
                Toast.makeText(this, "Tipe credential tidak didukung", Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(this, "Tipe credential tidak didukung", Toast.LENGTH_SHORT).show();
        }
    }

    private void firebaseAuthWithGoogle(String idToken) {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Sedang masuk dengan Google...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        AuthCredential credential = GoogleAuthProvider.getCredential(idToken, null);
        mAuth.signInWithCredential(credential)
                .addOnCompleteListener(this, task -> {
                    progressDialog.dismiss();

                    if (task.isSuccessful()) {
                        Toast.makeText(this, "Login Google berhasil! 🎉", Toast.LENGTH_SHORT).show();
                        navigateToMain();
                    } else {
                        Log.e(TAG, "Firebase auth dengan Google gagal", task.getException());
                        Toast.makeText(this, "Login Google gagal, coba lagi",
                                Toast.LENGTH_LONG).show();
                    }
                });
    }

    // ==================== NAVIGASI ====================

    private void navigateToMain() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
