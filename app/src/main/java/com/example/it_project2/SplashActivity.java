package com.example.it_project2;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.util.Log;
import android.widget.ProgressBar;

import com.google.firebase.auth.FirebaseAuth;

public class SplashActivity extends AppCompatActivity {

    private static final String TAG = "SplashActivity";

    private ProgressBar progressBar;
    private int progressStatus = 0;
    private final Handler handler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);

        // Sembunyikan action bar
        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        progressBar = findViewById(R.id.progressBar);
        progressBar.setProgress(0);

        // ===== FIREBASE APP CHECK (PLAY INTEGRITY) =====
        // Inisialisasi App Check PERTAMA sebelum Firebase calls lainnya.
        // Memastikan SEMUA request ke Firebase Auth & Database dilindungi
        // oleh App Check token secara otomatis.
        //
        // App Check memverifikasi:
        //   1. Request berasal dari app asli (tidak dimodifikasi)
        //   2. App berjalan di device yang sah
        //   3. Hanya app terdaftar yang bisa mengakses Firebase resources
        //
        // Catatan development: Debug provider aktif otomatis pada build DEBUG.
        // Lihat debug token di Logcat → filter "DebugAppCheckProvider"
        // lalu daftarkan di Firebase Console → App Check.
        AppCheckInitializer.initialize(getApplicationContext());
        Log.i(TAG, "Firebase App Check diinisialisasi");

        // Jalankan animasi loading
        startLoading();
    }

    private void startLoading() {
        // Update progress setiap 25ms
        // Total: 100 step x 25ms = 2500ms = 2.5 detik
        handler.post(new Runnable() {
            @Override
            public void run() {
                if (progressStatus < 100) {
                    progressStatus += 1;
                    progressBar.setProgress(progressStatus);

                    // Efek loading tidak merata — terasa lebih natural
                    // Awal cepat, tengah lambat, akhir cepat lagi
                    int delay;
                    if (progressStatus < 30) {
                        delay = 15;  // awal cepat
                    } else if (progressStatus < 70) {
                        delay = 35;  // tengah lambat
                    } else {
                        delay = 20;  // akhir agak cepat
                    }

                    handler.postDelayed(this, delay);

                } else {
                    // Progress 100% → cek login Firebase lalu pindah
                    handler.postDelayed(() -> {
                        Intent intent;

                        // Cek apakah user sudah login di Firebase
                        if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                            // Sudah login → langsung ke MainActivity
                            intent = new Intent(SplashActivity.this, MainActivity.class);
                        } else {
                            // Belum login → ke LoginActivity
                            intent = new Intent(SplashActivity.this, LoginActivity.class);
                        }

                        intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TASK);
                        startActivity(intent);
                        finish();
                    }, 300); // jeda 0.3 detik setelah 100%
                }
            }
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Hentikan handler saat activity ditutup
        // supaya tidak memory leak
        handler.removeCallbacksAndMessages(null);
    }
}