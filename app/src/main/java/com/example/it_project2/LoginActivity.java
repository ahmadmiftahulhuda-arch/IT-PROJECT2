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
import android.widget.ScrollView;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import android.widget.EditText;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView;

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
    private ImageView ivTogglePassword;
    private boolean isPasswordVisible = false;
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

        // ===== KEYBOARD RESPONSIF (push form above keyboard) =====
        // Material3 edge-to-edge membuat adjustResize tidak bekerja di API 30+,
        // sehingga kita handle secara manual menggunakan WindowInsetsCompat.
        ScrollView scrollViewLogin = findViewById(R.id.scrollViewLogin);
        ViewCompat.setOnApplyWindowInsetsListener(scrollViewLogin, (v, insets) -> {
            int imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
            int navHeight = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
            // Saat keyboard muncul, tambah padding bawah agar konten bisa discroll ke atas keyboard
            v.setPadding(
                v.getPaddingLeft(),
                v.getPaddingTop(),
                v.getPaddingRight(),
                Math.max(imeHeight, navHeight)
            );
            return insets;
        });

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
        ivTogglePassword = findViewById(R.id.ivTogglePassword);

        // ===== TOGGLE PASSWORD VISIBILITY =====
        ivTogglePassword.setOnClickListener(v -> {
            if (isPasswordVisible) {
                // Sembunyikan password
                etPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
                ivTogglePassword.setImageResource(R.drawable.ic_visibility_off);
                isPasswordVisible = false;
            } else {
                // Tampilkan password
                etPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                ivTogglePassword.setImageResource(R.drawable.ic_visibility);
                isPasswordVisible = true;
            }
            // Kembalikan kursor ke posisi akhir teks agar tidak melompat ke depan
            etPassword.setSelection(etPassword.getText().length());
        });

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

    // ==================== NAVIGASI & SINKRONISASI HAK AKSES ====================

    private void navigateToMain() {
        if (mAuth.getCurrentUser() != null && mAuth.getCurrentUser().getEmail() != null) {
            String email = mAuth.getCurrentUser().getEmail();
            String key = email.replace(".", ",");

            ProgressDialog pd = new ProgressDialog(this);
            pd.setMessage("Memeriksa Hak Akses...");
            pd.setCancelable(false);
            pd.show();

            com.google.firebase.database.DatabaseReference familyRef = 
                com.google.firebase.database.FirebaseDatabase.getInstance("https://smartliving-425c0-default-rtdb.asia-southeast1.firebasedatabase.app").getReference("family_members").child(key);
            
            // Tambahkan sistem Timeout 8 Detik agar tidak stuck
            boolean[] isProcessed = {false};
            android.os.Handler timeoutHandler = new android.os.Handler(android.os.Looper.getMainLooper());
            Runnable timeoutRunnable = () -> {
                if (!isProcessed[0]) {
                    isProcessed[0] = true;
                    pd.dismiss();
                    // Jika timeout, default jadikan full access agar pengguna bisa masuk
                    SessionManager sessionManager = new SessionManager(LoginActivity.this);
                    sessionManager.saveUserAccess(SessionManager.ACCESS_FULL);
                    Toast.makeText(LoginActivity.this, "Gagal sinkron akses keluarga (Jaringan Lambat / Firebase Offline)", Toast.LENGTH_LONG).show();
                    proceedToHome();
                }
            };
            timeoutHandler.postDelayed(timeoutRunnable, 8000); // Batas 8 detik tunggu

            familyRef.get().addOnCompleteListener(task -> {
                if (isProcessed[0]) return; // Jika sudah timeout, batalkan listener
                isProcessed[0] = true;
                timeoutHandler.removeCallbacks(timeoutRunnable);
                pd.dismiss();

                SessionManager sessionManager = new SessionManager(LoginActivity.this);
                if (task.isSuccessful() && task.getResult() != null && task.getResult().exists()) {
                    String access = task.getResult().child("access").getValue(String.class);
                    if ("MONITOR".equals(access)) {
                        sessionManager.saveUserAccess(SessionManager.ACCESS_MONITOR);
                        Toast.makeText(LoginActivity.this, "Masuk sebagai Anggota Keluarga (Monitoring)", Toast.LENGTH_SHORT).show();
                    } else {
                        sessionManager.saveUserAccess(SessionManager.ACCESS_FULL);
                    }
                } else {
                    // Default jika data tidak ada di whitelist
                    sessionManager.saveUserAccess(SessionManager.ACCESS_FULL);
                }
                proceedToHome();
            });
        } else {
            proceedToHome();
        }
    }

    private void proceedToHome() {
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }
}
