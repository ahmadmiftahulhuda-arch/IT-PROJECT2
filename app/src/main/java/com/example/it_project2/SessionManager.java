package com.example.it_project2;

import android.content.Context;
import android.content.SharedPreferences;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

/**
 * SessionManager mengelola session login pengguna.
 * Menggunakan Firebase Auth untuk autentikasi
 * dan SharedPreferences untuk menyimpan data tambahan (nama user).
 */
public class SessionManager {

    private static final String PREF_NAME = "SmartLivingSession";
    private static final String KEY_USER_NAME = "userName";

    private final FirebaseAuth firebaseAuth;
    private final SharedPreferences sessionPref;
    private final SharedPreferences.Editor editor;

    public SessionManager(Context context) {
        firebaseAuth = FirebaseAuth.getInstance();
        sessionPref = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        editor = sessionPref.edit();
    }

    // ==================== STATUS LOGIN ====================

    /**
     * Cek apakah user sedang login via Firebase Auth.
     */
    public boolean isLoggedIn() {
        return firebaseAuth.getCurrentUser() != null;
    }

    /**
     * Dapatkan FirebaseUser yang sedang login.
     */
    public FirebaseUser getCurrentUser() {
        return firebaseAuth.getCurrentUser();
    }

    /**
     * Dapatkan FirebaseAuth instance.
     */
    public FirebaseAuth getFirebaseAuth() {
        return firebaseAuth;
    }

    // ==================== DATA USER ====================

    /**
     * Simpan nama user ke SharedPreferences (data tambahan diluar Firebase).
     */
    public void saveUserName(String nama) {
        editor.putString(KEY_USER_NAME, nama);
        editor.apply();
    }

    /**
     * Ambil nama user.
     * Prioritas: SharedPreferences → Firebase displayName → "User"
     */
    public String getUserName() {
        String nama = sessionPref.getString(KEY_USER_NAME, null);
        if (nama != null && !nama.isEmpty()) {
            return nama;
        }

        // Fallback ke Firebase displayName
        FirebaseUser user = firebaseAuth.getCurrentUser();
        if (user != null && user.getDisplayName() != null && !user.getDisplayName().isEmpty()) {
            return user.getDisplayName();
        }

        return "User";
    }

    /**
     * Ambil email user dari Firebase Auth.
     */
    public String getUserEmail() {
        FirebaseUser user = firebaseAuth.getCurrentUser();
        return user != null ? user.getEmail() : "";
    }

    // ==================== LOGOUT ====================

    /**
     * Logout dari Firebase Auth dan hapus data lokal.
     */
    public void logout() {
        firebaseAuth.signOut();
        editor.clear();
        editor.apply();
    }
}
