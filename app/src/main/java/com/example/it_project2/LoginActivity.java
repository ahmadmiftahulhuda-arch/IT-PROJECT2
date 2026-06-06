package com.example.it_project2;

import androidx.appcompat.app.AppCompatActivity;
import androidx.credentials.CredentialManager;
import androidx.credentials.CredentialManagerCallback;
import androidx.credentials.CustomCredential;
import androidx.credentials.GetCredentialRequest;
import androidx.credentials.GetCredentialResponse;
import androidx.credentials.exceptions.GetCredentialException;

import android.app.Dialog;
import android.app.ProgressDialog;
import android.content.Intent;
import android.os.Bundle;
import android.os.CancellationSignal;
import android.os.CountDownTimer;
import android.util.Log;
import android.util.Patterns;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.CheckBox;
import android.widget.ScrollView;
import android.widget.Toast;

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
    private boolean isCaptchaVerified = false; // status verifikasi slider CAPTCHA
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

            // ===== TAMPILKAN CAPTCHA SEBELUM LOGIN =====
            // Validasi form sudah lolos → tampilkan dialog CAPTCHA.
            // Login Firebase hanya berjalan setelah CAPTCHA berhasil digeser.
            showCaptchaDialog(email, password);
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

    // ==================== DIALOG CAPTCHA ====================

    /**
     * Menampilkan dialog Slider CAPTCHA sebelum proses login.
     * Login Firebase hanya dijalankan setelah slider berhasil digeser penuh.
     *
     * @param email    email yang dimasukkan user
     * @param password password yang dimasukkan user
     */
    private void showCaptchaDialog(String email, String password) {
        // ── Cek cooldown sebelum tampilkan dialog ──
        if (CaptchaGuard.isBlocked()) {
            long sisa = CaptchaGuard.remainingSeconds();
            Toast.makeText(this,
                    "Terlalu banyak percobaan. Coba lagi dalam " + sisa + " detik.",
                    Toast.LENGTH_LONG).show();
            return;
        }

        Dialog dialog = new Dialog(this);
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        dialog.setContentView(R.layout.dialog_captcha);
        dialog.setCancelable(false);

        if (dialog.getWindow() != null) {
            dialog.getWindow().setBackgroundDrawableResource(android.R.color.transparent);
            WindowManager.LayoutParams params = dialog.getWindow().getAttributes();
            params.width = (int) (getResources().getDisplayMetrics().widthPixels * 0.90f);
            params.height = WindowManager.LayoutParams.WRAP_CONTENT;
            dialog.getWindow().setAttributes(params);
        }

        PuzzleCaptchaView puzzleView          = dialog.findViewById(R.id.sliderCaptcha);
        android.widget.TextView btnLanjut     = dialog.findViewById(R.id.btnCaptchaLanjut);
        android.widget.TextView btnBatal      = dialog.findViewById(R.id.btnCaptchaBatal);
        android.widget.LinearLayout layoutStatus = dialog.findViewById(R.id.layoutCaptchaStatus);
        android.widget.TextView tvStatus      = dialog.findViewById(R.id.tvCaptchaStatus);
        android.widget.TextView tvAttempt     = dialog.findViewById(R.id.tvAttemptCount);
        android.widget.LinearLayout btnRefresh= dialog.findViewById(R.id.btnRefreshPuzzle);
        android.widget.TextView tvTimer       = dialog.findViewById(R.id.tvCaptchaTimer);
        android.widget.ImageView ivTimerIcon  = dialog.findViewById(R.id.ivCaptchaTimerIcon);
        android.widget.ImageView ivAttemptIcon= dialog.findViewById(R.id.ivAttemptsIcon);

        final int[] attempts   = {0};
        isCaptchaVerified = false;
        puzzleView.refresh();

        // ── CountDownTimer 60 detik ──
        CountDownTimer[] timerHolder = {null};
        timerHolder[0] = new CountDownTimer(CaptchaGuard.DIALOG_TIMEOUT_MS, 1000) {
            @Override
            public void onTick(long millisLeft) {
                long sec = millisLeft / 1000;
                tvTimer.setText(sec + " detik");
                int color;
                if (sec > 30)      color = 0xFF16A34A;
                else if (sec > 10) color = 0xFFF59E0B;
                else               color = 0xFFDC2626;
                tvTimer.setTextColor(color);
                ivTimerIcon.setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_IN);
            }
            @Override
            public void onFinish() {
                if (dialog.isShowing()) {
                    dialog.dismiss();
                    Toast.makeText(LoginActivity.this,
                            "Waktu verifikasi habis. Silakan coba lagi.",
                            Toast.LENGTH_LONG).show();
                }
            }
        };
        timerHolder[0].start();

        // Batalkan timer saat dialog ditutup
        dialog.setOnDismissListener(d -> {
            if (timerHolder[0] != null) timerHolder[0].cancel();
        });

        // ── Listener puzzle CAPTCHA ──
        puzzleView.setCaptchaListener(new PuzzleCaptchaView.CaptchaListener() {
            @Override
            public void onVerified() {
                isCaptchaVerified = true;
                layoutStatus.setVisibility(View.GONE);
                btnLanjut.setEnabled(true);
                btnLanjut.setBackgroundResource(R.drawable.captcha_btn_active);
                btnLanjut.setText("Lanjutkan Masuk");
            }

            @Override
            public void onFailed(String message) {
                attempts[0]++;
                tvAttempt.setText(attempts[0] + " / " + CaptchaGuard.MAX_FAILURES + " kesempatan");

                if (attempts[0] >= CaptchaGuard.MAX_FAILURES) {
                    if (timerHolder[0] != null) timerHolder[0].cancel();
                    CaptchaGuard.block();
                    dialog.dismiss();
                    Toast.makeText(LoginActivity.this,
                            "Login dibatalkan. Terlalu banyak percobaan gagal.\nCoba lagi dalam 30 detik.",
                            Toast.LENGTH_LONG).show();
                    return;
                }

                if (attempts[0] == CaptchaGuard.MAX_FAILURES - 1) {
                    tvAttempt.setTextColor(0xFFDC2626);
                    ivAttemptIcon.setColorFilter(0xFFDC2626, android.graphics.PorterDuff.Mode.SRC_IN);
                }

                layoutStatus.setVisibility(View.VISIBLE);
                tvStatus.setText("❌ Posisi kurang tepat. Percobaan ke-" + attempts[0]
                        + " dari " + CaptchaGuard.MAX_FAILURES);
            }
        });

        // ── Tombol Refresh ──
        btnRefresh.setOnClickListener(v -> {
            isCaptchaVerified = false;
            attempts[0] = 0;
            tvAttempt.setText("0 / " + CaptchaGuard.MAX_FAILURES + " kesempatan");
            tvAttempt.setTextColor(0xFF64748B);
            layoutStatus.setVisibility(View.GONE);
            btnLanjut.setEnabled(false);
            btnLanjut.setBackgroundResource(R.drawable.captcha_btn_disabled);
            btnLanjut.setText("Selesaikan puzzle terlebih dahulu");
            puzzleView.refresh();
            // Reset timer
            if (timerHolder[0] != null) timerHolder[0].cancel();
            timerHolder[0] = new CountDownTimer(CaptchaGuard.DIALOG_TIMEOUT_MS, 1000) {
                @Override public void onTick(long ms) {
                    long s = ms / 1000;
                    tvTimer.setText(s + " detik");
                    int c = s > 30 ? 0xFF16A34A : s > 10 ? 0xFFF59E0B : 0xFFDC2626;
                    tvTimer.setTextColor(c);
                    ivTimerIcon.setColorFilter(c, android.graphics.PorterDuff.Mode.SRC_IN);
                }
                @Override public void onFinish() {
                    if (dialog.isShowing()) {
                        dialog.dismiss();
                        Toast.makeText(LoginActivity.this,
                                "Waktu verifikasi habis. Silakan coba lagi.",
                                Toast.LENGTH_LONG).show();
                    }
                }
            };
            timerHolder[0].start();
        });

        // ── Tombol Lanjutkan ──
        btnLanjut.setOnClickListener(v -> {
            if (isCaptchaVerified) {
                dialog.dismiss();
                loginWithFirebase(email, password);
            }
        });

        // ── Tombol Batal ──
        btnBatal.setOnClickListener(v -> {
            isCaptchaVerified = false;
            dialog.dismiss();
        });

        dialog.show();
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
        recordLoginHistory();
        Intent intent = new Intent(LoginActivity.this, MainActivity.class);
        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
        startActivity(intent);
        finish();
    }

    private void recordLoginHistory() {
        if (mAuth.getCurrentUser() == null) return;
        
        String uid = mAuth.getCurrentUser().getUid();
        com.google.firebase.database.DatabaseReference historyRef = 
            com.google.firebase.database.FirebaseDatabase.getInstance("https://smartliving-425c0-default-rtdb.asia-southeast1.firebasedatabase.app")
            .getReference("login_history").child(uid);

        String device = android.os.Build.MANUFACTURER + " " + android.os.Build.MODEL;
        String time = new java.text.SimpleDateFormat("dd MMM yyyy, HH:mm", java.util.Locale.getDefault()).format(new java.util.Date());
        
        // Cek Izin Lokasi & Ambil Lokasi Riil
        com.google.android.gms.location.FusedLocationProviderClient fusedLocationClient = 
            com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(this);

        if (androidx.core.app.ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            // Gunakan getCurrentLocation agar lebih akurat dibanding getLastLocation
            fusedLocationClient.getCurrentLocation(com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY, null)
                .addOnSuccessListener(this, location -> {
                    String finalLoc = "Lokasi tidak terdeteksi";
                    if (location != null) {
                        try {
                            android.location.Geocoder geocoder = new android.location.Geocoder(this, java.util.Locale.getDefault());
                            java.util.List<android.location.Address> addresses = geocoder.getFromLocation(location.getLatitude(), location.getLongitude(), 1);
                            if (addresses != null && !addresses.isEmpty()) {
                                String city = addresses.get(0).getLocality();
                                String country = addresses.get(0).getCountryName();
                                finalLoc = (city != null ? city : "Kota tidak dikenal") + ", " + (country != null ? country : "");
                            }
                        } catch (Exception e) {
                            finalLoc = "Error: " + e.getMessage();
                        }
                    }
                    saveLoginToDB(historyRef, device, finalLoc, time);
                })
                .addOnFailureListener(e -> {
                    saveLoginToDB(historyRef, device, "GPS mati / Error lokasi", time);
                });
        } else {
            saveLoginToDB(historyRef, device, "Izin lokasi ditolak", time);
        }
    }

    private void saveLoginToDB(com.google.firebase.database.DatabaseReference historyRef, String device, String loc, String time) {
        java.util.Map<String, Object> history = new java.util.HashMap<>();
        history.put("device", device);
        history.put("location", loc);
        history.put("time", time);
        historyRef.push().setValue(history);
    }
}
