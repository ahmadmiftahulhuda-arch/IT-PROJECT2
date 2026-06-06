package com.example.it_project2;

import android.content.Context;
import android.util.Log;

import com.google.firebase.FirebaseApp;
import com.google.firebase.appcheck.FirebaseAppCheck;
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory;

/**
 * AppCheckInitializer
 * ───────────────────
 * Menginisialisasi Firebase App Check dengan Play Integrity provider.
 *
 * Cara kerja:
 *  - Pada build RELEASE  → memakai Play Integrity (memverifikasi app asli di device asli)
 *  - Pada build DEBUG    → memakai Debug Provider (token sementara untuk development)
 *
 * Firebase App Check melindungi Firebase resources (Auth, Database, dll.) dari:
 *   • Bot / automated scripts
 *   • Brute-force attack
 *   • Aplikasi modifikasi / repackaged
 *   • Request yang tidak berasal dari app yang sah
 *
 * PRASYARAT (sebelum file ini bisa dikompilasi):
 * ──────────────────────────────────────────────
 * 1. Buka gradle/libs.versions.toml, tambahkan di [versions]:
 *      firebaseAppcheck = "17.1.1"
 *
 * 2. Masih di libs.versions.toml, tambahkan di [libraries]:
 *      firebase-appcheck-playintegrity = { group = "com.google.firebase", name = "firebase-appcheck-playintegrity", version.ref = "firebaseAppcheck" }
 *      firebase-appcheck-debug         = { group = "com.google.firebase", name = "firebase-appcheck-debug",         version.ref = "firebaseAppcheck" }
 *
 * 3. Buka app/build.gradle.kts, uncomment:
 *      implementation(libs.firebase.appcheck.playintegrity)
 *      debugImplementation(libs.firebase.appcheck.debug)
 *
 * 4. Sync Gradle → lalu jalankan app dan daftarkan debug token di Firebase Console.
 *    Lihat walkthrough.md untuk panduan lengkap.
 */
public class AppCheckInitializer {

    private static final String TAG = "AppCheckInitializer";

    /**
     * Panggil method ini SEKALI di awal aplikasi (SplashActivity.onCreate).
     * Setelah dipanggil, SEMUA request Firebase berikutnya otomatis menyertakan
     * App Check token tanpa perlu konfigurasi tambahan.
     *
     * @param context Application context
     */
    public static void initialize(Context context) {
        try {
            // Pastikan FirebaseApp sudah diinisialisasi
            FirebaseApp.initializeApp(context);

            FirebaseAppCheck firebaseAppCheck = FirebaseAppCheck.getInstance();

            // Gunakan Play Integrity Provider untuk memvalidasi keaslian app.
            // Play Integrity memeriksa:
            //   1. App genuine (tidak dimodifikasi / repackaged)
            //   2. Device genuine (bukan emulator yang di-root/modified)
            //   3. Google Play account valid
            //
            // Pada build DEBUG, Android secara otomatis menggunakan DebugAppCheckProvider
            // sehingga tidak perlu konfigurasi terpisah untuk development.
            firebaseAppCheck.installAppCheckProviderFactory(
                    PlayIntegrityAppCheckProviderFactory.getInstance()
            );

            // Aktifkan auto-refresh token agar token App Check tidak expired.
            // Token berlaku 1 jam, di-refresh otomatis di background.
            firebaseAppCheck.setTokenAutoRefreshEnabled(true);

            Log.i(TAG, "✅ Firebase App Check (Play Integrity) berhasil diinisialisasi");

        } catch (Exception e) {
            Log.e(TAG, "❌ Gagal menginisialisasi Firebase App Check: " + e.getMessage(), e);
        }
    }

    /**
     * Mendapatkan App Check token secara manual (untuk debugging / verifikasi).
     * Token ini otomatis disertakan pada setiap Firebase request setelah initialize().
     *
     * Cara pakai:
     *   AppCheckInitializer.getTokenForDebug(new AppCheckInitializer.AppCheckTokenListener() {
     *       public void onTokenReceived(String token) { Log.d("TAG", "Token: " + token); }
     *       public void onError(String err)           { Log.e("TAG", "Error: " + err); }
     *   });
     *
     * @param listener Callback yang menerima token atau error
     */
    public static void getTokenForDebug(AppCheckTokenListener listener) {
        FirebaseAppCheck.getInstance().getAppCheckToken(false)
                .addOnSuccessListener(tokenResult -> {
                    String token = tokenResult.getToken();
                    Log.d(TAG, "App Check Token OK: " + token.substring(0, Math.min(20, token.length())) + "...");
                    if (listener != null) listener.onTokenReceived(token);
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Gagal mendapatkan App Check token: " + e.getMessage());
                    if (listener != null) listener.onError(e.getMessage());
                });
    }

    /**
     * Interface callback untuk debug token.
     */
    public interface AppCheckTokenListener {
        void onTokenReceived(String token);
        void onError(String errorMessage);
    }
}
