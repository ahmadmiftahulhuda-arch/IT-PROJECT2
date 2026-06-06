package com.example.it_project2;

import androidx.appcompat.app.AppCompatActivity;
import android.app.Dialog;
import android.app.ProgressDialog;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Patterns;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.UserProfileChangeRequest;

public class RegisterActivity extends AppCompatActivity {

    private EditText etNama, etEmail, etPassword, etKonfirmasiPassword;
    private TextView btnDaftar, tvMasuk, tvPasswordStrength;
    private View btnBack, strengthBar1, strengthBar2, strengthBar3, strengthBar4;
    private ImageView ivTogglePassword, ivToggleKonfirmasi;
    private boolean isPasswordVisible = false;
    private boolean isKonfirmasiVisible = false;
    private boolean isCaptchaVerified = false;  // status verifikasi slider CAPTCHA
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
        tvPasswordStrength = findViewById(R.id.tvPasswordStrength);
        strengthBar1 = findViewById(R.id.strengthBar1);
        strengthBar2 = findViewById(R.id.strengthBar2);
        strengthBar3 = findViewById(R.id.strengthBar3);
        strengthBar4 = findViewById(R.id.strengthBar4);
        ivTogglePassword = findViewById(R.id.ivTogglePassword);
        ivToggleKonfirmasi = findViewById(R.id.ivToggleKonfirmasi);

        // ===== KEYBOARD RESPONSIF =====
        ScrollView scrollViewRegister = findViewById(R.id.scrollViewRegister);
        ViewCompat.setOnApplyWindowInsetsListener(scrollViewRegister, (v, insets) -> {
            int imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
            int navHeight = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(),
                    Math.max(imeHeight, navHeight));
            return insets;
        });

        // ===== TOGGLE MATA — KATA SANDI =====
        ivTogglePassword.setOnClickListener(v -> {
            if (isPasswordVisible) {
                etPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
                ivTogglePassword.setImageResource(R.drawable.ic_visibility_off);
                isPasswordVisible = false;
            } else {
                etPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                ivTogglePassword.setImageResource(R.drawable.ic_visibility);
                isPasswordVisible = true;
            }
            etPassword.setSelection(etPassword.getText().length());
        });

        // ===== TOGGLE MATA — KONFIRMASI KATA SANDI =====
        ivToggleKonfirmasi.setOnClickListener(v -> {
            if (isKonfirmasiVisible) {
                etKonfirmasiPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
                ivToggleKonfirmasi.setImageResource(R.drawable.ic_visibility_off);
                isKonfirmasiVisible = false;
            } else {
                etKonfirmasiPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                ivToggleKonfirmasi.setImageResource(R.drawable.ic_visibility);
                isKonfirmasiVisible = true;
            }
            etKonfirmasiPassword.setSelection(etKonfirmasiPassword.getText().length());
        });

        // ===== PASSWORD STRENGTH INDICATOR =====
        etPassword.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override
            public void afterTextChanged(Editable s) {
                updatePasswordStrength(s.toString());
            }
        });

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
            if (password.length() < 8) {
                etPassword.setError("Password minimal 8 karakter");
                etPassword.requestFocus();
                return;
            }
            if (!password.matches(".*[A-Z].*")) {
                etPassword.setError("Password harus mengandung minimal 1 huruf kapital (A-Z)");
                etPassword.requestFocus();
                return;
            }
            if (!password.matches(".*[0-9].*")) {
                etPassword.setError("Password harus mengandung minimal 1 angka (0-9)");
                etPassword.requestFocus();
                return;
            }
            if (!password.matches(".*[@#$%^&+=!*()_\\-].*")) {
                etPassword.setError("Password harus mengandung minimal 1 simbol (@#$%^&+=!*()_-)");
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

            // ===== TAMPILKAN CAPTCHA SEBELUM REGISTRASI =====
            // Semua validasi form sudah lolos → tampilkan dialog CAPTCHA.
            // Registrasi Firebase hanya berjalan setelah CAPTCHA berhasil digeser.
            showCaptchaDialog(nama, email, password);
        });

        tvMasuk.setOnClickListener(v -> finish());
    }

    // ==================== DIALOG CAPTCHA ====================

    /**
     * Menampilkan dialog Slider CAPTCHA.
     * Setelah slider berhasil digeser hingga ujung, tombol "Lanjutkan"
     * akan aktif dan menekannya akan memulai registrasi Firebase.
     *
     * @param nama     nama lengkap user
     * @param email    email user
     * @param password password user
     */
    private void showCaptchaDialog(String nama, String email, String password) {
        // ── Cek cooldown global sebelum tampilkan dialog ──
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

        PuzzleCaptchaView puzzleView      = dialog.findViewById(R.id.sliderCaptcha);
        TextView btnLanjut                = dialog.findViewById(R.id.btnCaptchaLanjut);
        TextView btnBatal                 = dialog.findViewById(R.id.btnCaptchaBatal);
        android.widget.LinearLayout layoutStatus = dialog.findViewById(R.id.layoutCaptchaStatus);
        android.widget.TextView tvStatus  = dialog.findViewById(R.id.tvCaptchaStatus);
        android.widget.TextView tvAttempt = dialog.findViewById(R.id.tvAttemptCount);
        android.widget.LinearLayout btnRefresh = dialog.findViewById(R.id.btnRefreshPuzzle);
        android.widget.TextView tvTimer   = dialog.findViewById(R.id.tvCaptchaTimer);
        android.widget.ImageView ivTimerIcon  = dialog.findViewById(R.id.ivCaptchaTimerIcon);
        android.widget.ImageView ivAttemptIcon= dialog.findViewById(R.id.ivAttemptsIcon);

        final int[] attempts = {0};
        isCaptchaVerified = false;
        puzzleView.refresh();

        // ── CountDownTimer 60 detik ──
        CountDownTimer[] timerHolder = {null};
        timerHolder[0] = new CountDownTimer(CaptchaGuard.DIALOG_TIMEOUT_MS, 1000) {
            @Override
            public void onTick(long millisLeft) {
                long sec = millisLeft / 1000;
                tvTimer.setText(sec + " detik");
                int color = sec > 30 ? 0xFF16A34A : sec > 10 ? 0xFFF59E0B : 0xFFDC2626;
                tvTimer.setTextColor(color);
                ivTimerIcon.setColorFilter(color, android.graphics.PorterDuff.Mode.SRC_IN);
            }
            @Override
            public void onFinish() {
                if (dialog.isShowing()) {
                    dialog.dismiss();
                    Toast.makeText(RegisterActivity.this,
                            "Waktu verifikasi habis. Silakan coba lagi.",
                            Toast.LENGTH_LONG).show();
                }
            }
        };
        timerHolder[0].start();

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
                btnLanjut.setText("Lanjutkan Pendaftaran");
            }

            @Override
            public void onFailed(String message) {
                attempts[0]++;
                tvAttempt.setText(attempts[0] + " / " + CaptchaGuard.MAX_FAILURES + " kesempatan");

                if (attempts[0] >= CaptchaGuard.MAX_FAILURES) {
                    if (timerHolder[0] != null) timerHolder[0].cancel();
                    CaptchaGuard.block();
                    dialog.dismiss();
                    Toast.makeText(RegisterActivity.this,
                            "Pendaftaran dibatalkan. Terlalu banyak percobaan.\nCoba lagi dalam 30 detik.",
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
                        Toast.makeText(RegisterActivity.this,
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
                registerWithFirebase(nama, email, password);
            }
        });

        // ── Tombol Batal ──
        btnBatal.setOnClickListener(v -> {
            isCaptchaVerified = false;
            dialog.dismiss();
            Toast.makeText(RegisterActivity.this, "Pendaftaran dibatalkan", Toast.LENGTH_SHORT).show();
        });

        dialog.show();
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

    // ===== PASSWORD STRENGTH INDICATOR =====
    private void updatePasswordStrength(String password) {
        int score = 0;

        boolean hasLength  = password.length() >= 8;
        boolean hasUpper   = password.matches(".*[A-Z].*");
        boolean hasNumber  = password.matches(".*[0-9].*");
        boolean hasSymbol  = password.matches(".*[@#$%^&+=!*()_\\-].*");

        if (hasLength)  score++;
        if (hasUpper)   score++;
        if (hasNumber)  score++;
        if (hasSymbol)  score++;

        // Reset semua bar ke abu-abu
        int grey = 0xFFE2E8F0;
        strengthBar1.setBackgroundColor(grey);
        strengthBar2.setBackgroundColor(grey);
        strengthBar3.setBackgroundColor(grey);
        strengthBar4.setBackgroundColor(grey);

        switch (score) {
            case 0:
                tvPasswordStrength.setText("Masukkan password");
                tvPasswordStrength.setTextColor(0xFF94A3B8);
                break;
            case 1:
                strengthBar1.setBackgroundColor(0xFFEF4444); // merah
                tvPasswordStrength.setText("⚠ Sangat Lemah");
                tvPasswordStrength.setTextColor(0xFFEF4444);
                break;
            case 2:
                strengthBar1.setBackgroundColor(0xFFF97316); // oranye
                strengthBar2.setBackgroundColor(0xFFF97316);
                tvPasswordStrength.setText("● Lemah — tambahkan huruf kapital, angka, atau simbol");
                tvPasswordStrength.setTextColor(0xFFF97316);
                break;
            case 3:
                strengthBar1.setBackgroundColor(0xFFFACC15); // kuning
                strengthBar2.setBackgroundColor(0xFFFACC15);
                strengthBar3.setBackgroundColor(0xFFFACC15);
                tvPasswordStrength.setText("◑ Cukup Kuat");
                tvPasswordStrength.setTextColor(0xFFF59E0B);
                break;
            case 4:
                strengthBar1.setBackgroundColor(0xFF16A34A); // hijau
                strengthBar2.setBackgroundColor(0xFF16A34A);
                strengthBar3.setBackgroundColor(0xFF16A34A);
                strengthBar4.setBackgroundColor(0xFF16A34A);
                tvPasswordStrength.setText("✓ Sangat Kuat");
                tvPasswordStrength.setTextColor(0xFF16A34A);
                break;
        }
    }
}
