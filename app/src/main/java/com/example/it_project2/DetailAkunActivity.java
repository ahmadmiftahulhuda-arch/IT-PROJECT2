package com.example.it_project2;

import android.os.Bundle;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class DetailAkunActivity extends AppCompatActivity {

    private EditText etName, etEmail, etPhone;
    private SessionManager sessionManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail_akun);

        sessionManager = new SessionManager(this);

        ImageView btnBack = findViewById(R.id.btnBack);
        etName = findViewById(R.id.etName);
        etEmail = findViewById(R.id.etEmail);
        etPhone = findViewById(R.id.etPhone);
        android.view.View btnSave = findViewById(R.id.btnSave);

        btnBack.setOnClickListener(v -> finish());

        // Load data dari session
        String currentName = sessionManager.getUserName();
        String currentEmail = sessionManager.getUserEmail();
        etName.setText(currentName);
        etEmail.setText(currentEmail);

        btnSave.setOnClickListener(v -> {
            String newName = etName.getText().toString().trim();
            String newEmail = etEmail.getText().toString().trim();
            String newPhone = etPhone.getText().toString().trim();

            if (newName.isEmpty()) {
                etName.setError("Nama tidak boleh kosong");
                return;
            }

            // 1. Update Firebase Auth Profile (Name)
            com.google.firebase.auth.FirebaseUser user = com.google.firebase.auth.FirebaseAuth.getInstance().getCurrentUser();
            if (user != null) {
                com.google.firebase.auth.UserProfileChangeRequest profileUpdates = new com.google.firebase.auth.UserProfileChangeRequest.Builder()
                        .setDisplayName(newName)
                        .build();

                user.updateProfile(profileUpdates).addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        // 2. Simpan ke Realtime Database
                        String uid = user.getUid();
                        com.google.firebase.database.DatabaseReference userRef = com.google.firebase.database.FirebaseDatabase.getInstance("https://smartliving-425c0-default-rtdb.asia-southeast1.firebasedatabase.app")
                                .getReference("users").child(uid);

                        java.util.Map<String, Object> userData = new java.util.HashMap<>();
                        userData.put("name", newName);
                        userData.put("email", newEmail);
                        userData.put("phone", newPhone);

                        userRef.setValue(userData).addOnCompleteListener(dbTask -> {
                            if (dbTask.isSuccessful()) {
                                sessionManager.saveUserName(newName);
                                Toast.makeText(this, "Profil berhasil disinkronkan ke Database", Toast.LENGTH_SHORT).show();
                                finish();
                            } else {
                                Toast.makeText(this, "Gagal sinkron database", Toast.LENGTH_SHORT).show();
                            }
                        });
                    } else {
                        Toast.makeText(this, "Gagal update profil", Toast.LENGTH_SHORT).show();
                    }
                });
            }
        });
    }
}
