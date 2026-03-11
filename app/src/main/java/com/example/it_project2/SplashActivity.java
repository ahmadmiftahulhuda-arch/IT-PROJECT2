package com.example.it_project2;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.widget.ProgressBar;

public class SplashActivity extends AppCompatActivity {

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
                    // Progress 100% → pindah ke LoginActivity
                    handler.postDelayed(() -> {
                        Intent intent = new Intent(SplashActivity.this, LoginActivity.class);
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