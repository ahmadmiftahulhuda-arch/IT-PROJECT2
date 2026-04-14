package com.example.it_project2;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import android.app.ProgressDialog;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import com.google.firebase.auth.FirebaseAuth;

public class ResetPasswordActivity extends AppCompatActivity {

    private EditText etNewPassword, etKonfirmasiPassword;
    private TextView btnPerbarui, tvPasswordStrength;
    private View strengthBar1, strengthBar2, strengthBar3, strengthBar4;
    private ImageView ivTogglePassword, ivToggleKonfirmasi;
    private boolean isPasswordVisible = false;
    private boolean isKonfirmasiVisible = false;

    private FirebaseAuth mAuth;
    private String oobCode = null;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_reset_password);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        mAuth = FirebaseAuth.getInstance();

        // Ambil data intent dari Deep Link email
        Intent intent = getIntent();
        if (intent != null && Intent.ACTION_VIEW.equals(intent.getAction())) {
            Uri data = intent.getData();
            if (data != null) {
                oobCode = data.getQueryParameter("oobCode");
            }
        }

        if (oobCode == null) {
            Toast.makeText(this, "Link tidak valid atau sudah kadaluarsa", Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        etNewPassword = findViewById(R.id.etNewPassword);
        etKonfirmasiPassword = findViewById(R.id.etKonfirmasiPassword);
        btnPerbarui = findViewById(R.id.btnPerbarui);
        tvPasswordStrength = findViewById(R.id.tvPasswordStrength);
        strengthBar1 = findViewById(R.id.strengthBar1);
        strengthBar2 = findViewById(R.id.strengthBar2);
        strengthBar3 = findViewById(R.id.strengthBar3);
        strengthBar4 = findViewById(R.id.strengthBar4);
        ivTogglePassword = findViewById(R.id.ivTogglePassword);
        ivToggleKonfirmasi = findViewById(R.id.ivToggleKonfirmasi);
        ImageView btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        // Keyboard responsif layout
        ScrollView scrollViewReset = findViewById(R.id.scrollViewReset);
        ViewCompat.setOnApplyWindowInsetsListener(scrollViewReset, (v, insets) -> {
            int imeHeight = insets.getInsets(WindowInsetsCompat.Type.ime()).bottom;
            int navHeight = insets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom;
            v.setPadding(v.getPaddingLeft(), v.getPaddingTop(), v.getPaddingRight(),
                    Math.max(imeHeight, navHeight));
            return insets;
        });

        // Toggle Mata Sandi
        ivTogglePassword.setOnClickListener(v -> {
            if (isPasswordVisible) {
                etNewPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_PASSWORD);
                ivTogglePassword.setImageResource(R.drawable.ic_visibility_off);
                isPasswordVisible = false;
            } else {
                etNewPassword.setInputType(android.text.InputType.TYPE_CLASS_TEXT | android.text.InputType.TYPE_TEXT_VARIATION_VISIBLE_PASSWORD);
                ivTogglePassword.setImageResource(R.drawable.ic_visibility);
                isPasswordVisible = true;
            }
            etNewPassword.setSelection(etNewPassword.getText().length());
        });

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

        etNewPassword.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {}
            @Override public void afterTextChanged(Editable s) {
                updatePasswordStrength(s.toString());
            }
        });

        btnPerbarui.setOnClickListener(v -> {
            String password = etNewPassword.getText().toString().trim();
            String konfirmasi = etKonfirmasiPassword.getText().toString().trim();

            if (password.isEmpty()) {
                etNewPassword.setError("Password tidak boleh kosong");
                etNewPassword.requestFocus();
                return;
            }
            if (password.length() < 8) {
                etNewPassword.setError("Password minimal 8 karakter");
                etNewPassword.requestFocus();
                return;
            }
            if (!password.matches(".*[A-Z].*")) {
                etNewPassword.setError("Minimal 1 huruf kapital (A-Z)");
                etNewPassword.requestFocus();
                return;
            }
            if (!password.matches(".*[0-9].*")) {
                etNewPassword.setError("Minimal 1 angka (0-9)");
                etNewPassword.requestFocus();
                return;
            }
            if (!password.matches(".*[@#$%^&+=!*()_\\-].*")) {
                etNewPassword.setError("Minimal 1 simbol (@#$%^&+=!*()_-)");
                etNewPassword.requestFocus();
                return;
            }
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

            perbaruiPasswordFirebase(password);
        });
    }

    private void perbaruiPasswordFirebase(String newPassword) {
        ProgressDialog progressDialog = new ProgressDialog(this);
        progressDialog.setMessage("Menyimpan password baru...");
        progressDialog.setCancelable(false);
        progressDialog.show();

        // Mengecek / memverifikasi kode terlebih dahulu (opsional, tapi disarankan)
        mAuth.verifyPasswordResetCode(oobCode).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                // Email valid, sekarang proses reset password
                mAuth.confirmPasswordReset(oobCode, newPassword).addOnCompleteListener(resetTask -> {
                    progressDialog.dismiss();
                    if (resetTask.isSuccessful()) {
                        Toast.makeText(ResetPasswordActivity.this, "Password berhasil diperbarui!", Toast.LENGTH_LONG).show();
                        // Alihkan ke halaman utama Login dan tutup tumpukan reset sandi
                        Intent intent = new Intent(ResetPasswordActivity.this, LoginActivity.class);
                        intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_NEW_TASK);
                        startActivity(intent);
                        finish();
                    } else {
                        Toast.makeText(ResetPasswordActivity.this, "Gagal memperbarui password", Toast.LENGTH_LONG).show();
                    }
                });
            } else {
                progressDialog.dismiss();
                Toast.makeText(this, "Sesi link sudah kadaluarsa. Silakan minta link baru.", Toast.LENGTH_LONG).show();
            }
        });
    }

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
