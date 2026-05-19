package com.example.it_project2;

import androidx.appcompat.app.AppCompatActivity;
import android.graphics.Color;
import android.content.Intent;
import android.os.Bundle;
import android.widget.Switch;
import android.widget.TextView;

import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private TextView tvHalo, tvStatus, tvStatusDesc, tvSuhu, tvKelembapan;
    private android.view.View btnNotifikasi; // Menambahkan FrameLayout notifikasi
    private Switch switchHeater;
    private BottomNavigationView bottomNav;

    // View Kondisi Luar Ruangan
    private TextView tvHomeAqiBadge, tvHomeOuterTemp, tvHomeCondition, tvHomeLocation, tvHomeTime;
    private android.widget.ImageView ivHomeWeatherIcon;

    // Firebase Realtime Database
    private DatabaseReference sensorRef;
    private DatabaseReference heaterRef;
    private double currentThreshold = 20.0;
    private boolean isNotificationShown = false;
    private long lastSensorUpdate = 0;
    private android.os.Handler statusHandler = new android.os.Handler(android.os.Looper.getMainLooper());
    private Runnable statusChecker;

    // OpenWeatherMap API untuk Kondisi Luar Ruangan
    private static final String API_KEY = "2cfb11165aef08cf723950125e1f9ae0";
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/";
    private static final int PERMISSION_REQUEST_CODE = 1001;

    private com.google.android.gms.location.FusedLocationProviderClient fusedLocationClient;
    private final java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(2);
    private final android.os.Handler mainHandler = new android.os.Handler(android.os.Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Bind semua view
        tvHalo       = findViewById(R.id.tvHalo);
        tvStatus     = findViewById(R.id.tvStatus);
        tvStatusDesc = findViewById(R.id.tvStatusDesc);
        tvSuhu       = findViewById(R.id.tvSuhu);
        tvKelembapan = findViewById(R.id.tvKelembapan);
        switchHeater = findViewById(R.id.switchHeater);
        bottomNav    = findViewById(R.id.bottomNav);
        btnNotifikasi = findViewById(R.id.btnNotifikasi);

        // Bind Kondisi Luar Ruangan
        tvHomeAqiBadge = findViewById(R.id.tvHomeAqiBadge);
        tvHomeOuterTemp = findViewById(R.id.tvHomeOuterTemp);
        tvHomeCondition = findViewById(R.id.tvHomeCondition);
        tvHomeLocation = findViewById(R.id.tvHomeLocation);
        tvHomeTime = findViewById(R.id.tvHomeTime);
        ivHomeWeatherIcon = findViewById(R.id.ivHomeWeatherIcon);

        // Update Waktu Standby
        updateHomeTime();

        // Listener Notifikasi
        btnNotifikasi.setOnClickListener(v -> {
            startActivity(new Intent(this, NotifikasiSuhuActivity.class));
        });

        // Tampilkan nama user + Sapaan Dinamis
        SessionManager sessionManager = new SessionManager(this);
        String userName = sessionManager.getUserName();
        
        // Logika Sapaan
        String greeting;
        int hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY);
        if (hour >= 5 && hour < 11) greeting = "Selamat Pagi";
        else if (hour >= 11 && hour < 15) greeting = "Selamat Siang";
        else if (hour >= 15 && hour < 18) greeting = "Selamat Sore";
        else greeting = "Selamat Malam";

        if (!userName.isEmpty()) {
            tvHalo.setText(greeting + ",\n" + userName);
        } else {
            tvHalo.setText(greeting + ",\nUser");
        }

        // ===== FIREBASE REALTIME DATABASE =====
        // URL dari Firebase Console → Realtime Database (bagian atas halaman)
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://smartliving-425c0-default-rtdb.asia-southeast1.firebasedatabase.app");
        sensorRef = database.getReference("sensor");
        heaterRef = database.getReference("heater");

        // Listener untuk data sensor (real-time)
        sensorRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                lastSensorUpdate = System.currentTimeMillis();
                // Ambil data suhu
                Double suhu = snapshot.child("suhu").getValue(Double.class);
                Double kelembapan = snapshot.child("kelembapan").getValue(Double.class);

                if (suhu != null) {
                    tvSuhu.setText(String.format(Locale.getDefault(), "%.1f°C", suhu));
                    updateStatusCard(suhu);
                    
                    if (suhu <= currentThreshold) {
                        if (!isNotificationShown) {
                            isNotificationShown = true;
                            Intent intent = new Intent(MainActivity.this, NotifikasiSuhuActivity.class);
                            intent.putExtra("suhu", suhu);
                            startActivity(intent);
                            logAktivitas("Peringatan Suhu", 
                                "Suhu " + suhu + "°C menyentuh ambang batas (" + currentThreshold + "°C)", true);
                        }
                    } else if (suhu > currentThreshold + 1.0) {
                        // Reset status notifikasi jika suhu sudah kembali normal (dengan hysteresis 1°C)
                        isNotificationShown = false;
                    }
                }

                if (kelembapan != null) {
                    tvKelembapan.setText(String.format(Locale.getDefault(), "%.0f%%", kelembapan));
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                tvStatus.setText("ERROR");
                tvStatus.setTextColor(getResources().getColor(android.R.color.holo_red_dark));
                tvStatusDesc.setText("Gagal membaca data sensor");
            }
        });

        // Listener untuk status heater
        heaterRef.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                Boolean isOn = snapshot.child("status").getValue(Boolean.class);
                Double threshold = snapshot.child("threshold").getValue(Double.class);
                String mode = snapshot.child("mode").getValue(String.class);
                
                if (threshold != null) {
                    currentThreshold = threshold;
                }

                if (isOn != null) {
                    // Update UI Heater
                    TextView tvHeaterMode = findViewById(R.id.tvHeaterMode);
                    TextView tvHeaterStatusText = findViewById(R.id.tvHeaterStatusText);
                    
                    if (tvHeaterMode != null) tvHeaterMode.setText("Mode " + (mode != null ? mode : "Manual"));
                    if (tvHeaterStatusText != null) {
                        tvHeaterStatusText.setText(isOn ? "ON" : "OFF");
                        tvHeaterStatusText.setTextColor(isOn ? 0xFF16A34A : 0xFFEF4444);
                    }

                    // Update switch tanpa trigger listener
                    switchHeater.setOnCheckedChangeListener(null);
                    switchHeater.setChecked(isOn);
                    
                    // Jika mode otomatis, matikan interaksi manual
                    SessionManager sessionManager = new SessionManager(MainActivity.this);
                    boolean isMonitor = sessionManager.getUserAccess().equals(SessionManager.ACCESS_MONITOR);
                    boolean isOtomatis = "otomatis".equals(mode);
                    
                    switchHeater.setEnabled(!isMonitor && !isOtomatis);

                    switchHeater.setOnCheckedChangeListener((buttonView, isChecked) -> {
                        heaterRef.child("status").setValue(isChecked);
                        logAktivitas(isChecked ? "Pemanas Aktif" : "Pemanas Standby", 
                            "Status diperbarui oleh sistem/pengguna", isChecked);
                    });
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                // Handle error
            }
        });

        // Switch heater manual toggle
        switchHeater.setOnCheckedChangeListener((buttonView, isChecked) -> {
            heaterRef.child("status").setValue(isChecked);
        });

        // Set active tab
        bottomNav.setSelectedItemId(R.id.nav_home);

        // Navigasi bottom nav
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_riwayat) {
                startActivity(new Intent(this, RiwayatActivity.class));
            } else if (id == R.id.nav_kontrol) {
                startActivity(new Intent(this, KontrolActivity.class));
            } else if (id == R.id.nav_informasi) {
                startActivity(new Intent(this, InformasiActivity.class));
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
            }
            return true;
        });

        // ===== ENFORCEMENT AKSES =====
        if (sessionManager.getUserAccess().equals(SessionManager.ACCESS_MONITOR)) {
            switchHeater.setEnabled(false);
            tvStatusDesc.setText(tvStatusDesc.getText() + " (Mode Monitoring)");
        }
        
        // ===== STATUS CHECKER (Heartbeat) =====
        statusChecker = new Runnable() {
            @Override
            public void run() {
                long now = System.currentTimeMillis();
                TextView tvSensorDhtStatus = findViewById(R.id.tvSensorDhtStatus);
                TextView tvSensorDhtConn = findViewById(R.id.tvSensorDhtConn);

                if (lastSensorUpdate == 0 || now - lastSensorUpdate > 10000) {
                    // Jika belum pernah update atau lebih dari 10 detik tidak ada update data
                    tvStatus.setText("SENSOR OFFLINE");
                    tvStatus.setTextColor(android.graphics.Color.WHITE);
                    tvStatusDesc.setText("Perangkat tidak terdeteksi / Mati");
                    androidx.cardview.widget.CardView cardStatusSystem = findViewById(R.id.cardStatusSystem);
                    if (cardStatusSystem != null) cardStatusSystem.setCardBackgroundColor(android.graphics.Color.parseColor("#64748B"));
                    
                    // Sembunyikan Icon Shield jika offline
                    android.widget.ImageView ivStatusIcon = findViewById(R.id.ivStatusIcon);
                    if (ivStatusIcon != null) ivStatusIcon.setVisibility(android.view.View.GONE);
                    
                    // Update Label Kecil DHT22
                    if (tvSensorDhtStatus != null) {
                        tvSensorDhtStatus.setText("Mati");
                        tvSensorDhtStatus.setTextColor(0xFFEF4444); // Merah
                    }
                    if (tvSensorDhtConn != null) tvSensorDhtConn.setText("Terputus");
                } else {
                    // Sensor Aktif
                    android.widget.ImageView ivStatusIcon = findViewById(R.id.ivStatusIcon);
                    if (ivStatusIcon != null) ivStatusIcon.setVisibility(android.view.View.VISIBLE);

                    if (tvSensorDhtStatus != null) {
                        tvSensorDhtStatus.setText("Aktif");
                        tvSensorDhtStatus.setTextColor(0xFF10B981); // Hijau
                    }
                    if (tvSensorDhtConn != null) tvSensorDhtConn.setText("Terhubung");
                }
                statusHandler.postDelayed(this, 5000);
            }
        };
        statusHandler.post(statusChecker);

        // ===== LOKASI & CUACA LUAR RUANGAN =====
        fusedLocationClient = com.google.android.gms.location.LocationServices.getFusedLocationProviderClient(this);

        if (androidx.core.app.ActivityCompat.checkSelfPermission(this, android.Manifest.permission.ACCESS_FINE_LOCATION) == android.content.pm.PackageManager.PERMISSION_GRANTED) {
            fetchLocation();
        } else {
            androidx.core.app.ActivityCompat.requestPermissions(this,
                    new String[]{android.Manifest.permission.ACCESS_FINE_LOCATION,
                                 android.Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSION_REQUEST_CODE);
        }
    }

    private void updateHomeTime() {
        java.text.SimpleDateFormat timeFormat = new java.text.SimpleDateFormat("HH:mm 'WIB'", Locale.getDefault());
        tvHomeTime.setText(timeFormat.format(new java.util.Date()));
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @androidx.annotation.NonNull String[] permissions, @androidx.annotation.NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == android.content.pm.PackageManager.PERMISSION_GRANTED) {
                fetchLocation();
            } else {
                tvHomeLocation.setText("BANJARMASIN");
                tvHomeOuterTemp.setText("33°C");
                tvHomeCondition.setText("Akses Lokasi Ditolak");
            }
        }
    }

    @SuppressWarnings("MissingPermission")
    private void fetchLocation() {
        fusedLocationClient.getLastLocation().addOnSuccessListener(lastLoc -> {
            if (lastLoc != null) {
                double lat = lastLoc.getLatitude();
                double lon = lastLoc.getLongitude();
                getCityName(lat, lon);
                fetchCurrentWeather(lat, lon);
                fetchAirQuality(lat, lon);
            } else {
                tvHomeLocation.setText("BANJARMASIN");
                tvHomeOuterTemp.setText("33°C");
            }
        });
    }

    private void getCityName(double lat, double lon) {
        executor.execute(() -> {
            try {
                android.location.Geocoder geocoder = new android.location.Geocoder(this, new Locale("id", "ID"));
                java.util.List<android.location.Address> addresses = geocoder.getFromLocation(lat, lon, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    android.location.Address addr = addresses.get(0);
                    String city = addr.getSubLocality();
                    if (city == null || city.isEmpty()) city = addr.getLocality();
                    if (city == null || city.isEmpty()) city = addr.getSubAdminArea();
                    final String cityName = (city != null) ? city.toUpperCase(Locale.ROOT) : "LOKASI ANDA";
                    mainHandler.post(() -> tvHomeLocation.setText(cityName));
                }
            } catch (Exception e) {
                mainHandler.post(() -> tvHomeLocation.setText("LOKASI ANDA"));
            }
        });
    }

    private void fetchCurrentWeather(double lat, double lon) {
        String urlStr = BASE_URL + "weather?lat=" + lat + "&lon=" + lon + "&appid=" + API_KEY + "&units=metric&lang=id";
        executor.execute(() -> {
            try {
                String response = httpGet(urlStr);
                org.json.JSONObject json = new org.json.JSONObject(response);

                double temp = json.getJSONObject("main").getDouble("temp");
                String conditionId = json.getJSONArray("weather").getJSONObject(0).getString("description");
                int weatherCode = json.getJSONArray("weather").getJSONObject(0).getInt("id");

                String condition = conditionId.substring(0, 1).toUpperCase(Locale.ROOT) + conditionId.substring(1);
                String tempStr = String.format(Locale.getDefault(), "%.0f°C", temp);
                int iconRes = getWeatherIconRes(weatherCode);

                mainHandler.post(() -> {
                    tvHomeOuterTemp.setText(tempStr);
                    tvHomeCondition.setText(condition);
                    ivHomeWeatherIcon.setImageResource(iconRes);
                    updateHomeTime();
                });
            } catch (Exception e) {
                mainHandler.post(() -> tvHomeCondition.setText("Gagal"));
            }
        });
    }

    private void fetchAirQuality(double lat, double lon) {
        String urlStr = BASE_URL + "air_pollution?lat=" + lat + "&lon=" + lon + "&appid=" + API_KEY;
        executor.execute(() -> {
            try {
                String response = httpGet(urlStr);
                org.json.JSONObject json = new org.json.JSONObject(response);
                org.json.JSONObject components = json.getJSONArray("list").getJSONObject(0).getJSONObject("components");

                double pm25 = components.getDouble("pm2_5");
                int aqiIndex = (int) Math.round(pm25);

                String badgeText;
                int badgeColor;

                if (pm25 <= 12) {
                    badgeText = "BAIK"; badgeColor = 0xFF16A34A;
                } else if (pm25 <= 35) {
                    badgeText = "MODERATE"; badgeColor = 0xFFF59E0B;
                } else if (pm25 <= 55) {
                    badgeText = "TIDAK SEHAT"; badgeColor = 0xFFEF4444;
                } else {
                    badgeText = "BERBAHAYA"; badgeColor = 0xFF7C3AED;
                }

                mainHandler.post(() -> {
                    tvHomeAqiBadge.setText("AQI: " + aqiIndex + " " + badgeText);
                    tvHomeAqiBadge.setTextColor(badgeColor);
                    android.graphics.drawable.GradientDrawable badgeBg = new android.graphics.drawable.GradientDrawable();
                    badgeBg.setColor(android.graphics.Color.argb(38, android.graphics.Color.red(badgeColor), android.graphics.Color.green(badgeColor), android.graphics.Color.blue(badgeColor)));
                    // dpToPx manual for 6dp
                    badgeBg.setCornerRadius(6 * getResources().getDisplayMetrics().density);
                    tvHomeAqiBadge.setBackground(badgeBg);
                });
            } catch (Exception e) {
                mainHandler.post(() -> tvHomeAqiBadge.setText("AQI: --"));
            }
        });
    }

    private int getWeatherIconRes(int code) {
        if (code >= 200 && code < 300) return R.drawable.ic_weather_thunder;
        if (code >= 300 && code < 600) return R.drawable.ic_weather_rain;
        if (code >= 600 && code < 700) return R.drawable.ic_weather_snow;
        if (code >= 700 && code < 800) return R.drawable.ic_weather_cloudy; 
        if (code == 800) return R.drawable.ic_weather_sunny;
        return R.drawable.ic_weather_cloudy;
    }

    private String httpGet(String urlStr) throws Exception {
        java.net.URL url = new java.net.URL(urlStr);
        java.net.HttpURLConnection conn = (java.net.HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        conn.connect();

        java.io.BufferedReader reader = new java.io.BufferedReader(new java.io.InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        reader.close();
        conn.disconnect();
        return sb.toString();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }

    /**
     * Update card status berdasarkan suhu.
     * - Hijau (AMAN): suhu >= 24°C
     * - Kuning (WASPADA): suhu 20-23°C
     * - Merah (BAHAYA): suhu < 20°C
     */
    private void updateStatusCard(double suhu) {
        androidx.cardview.widget.CardView cardStatusSystem = findViewById(R.id.cardStatusSystem);
        android.widget.ImageView ivStatusIcon = findViewById(R.id.ivStatusIcon);
        if (cardStatusSystem == null || ivStatusIcon == null) return;
        
        if (suhu >= 24) {
            tvStatus.setText("AMAN");
            cardStatusSystem.setCardBackgroundColor(android.graphics.Color.parseColor("#10B981"));
            ivStatusIcon.setColorFilter(android.graphics.Color.parseColor("#34D399"));
            tvStatusDesc.setText("Semua parameter normal");
        } else if (suhu >= 20) {
            tvStatus.setText("WASPADA");
            cardStatusSystem.setCardBackgroundColor(android.graphics.Color.parseColor("#F59E0B"));
            ivStatusIcon.setColorFilter(android.graphics.Color.parseColor("#FBBF24"));
            tvStatusDesc.setText("Suhu menurun berbahaya");
        } else {
            tvStatus.setText("BAHAYA");
            cardStatusSystem.setCardBackgroundColor(android.graphics.Color.parseColor("#EF4444"));
            ivStatusIcon.setColorFilter(android.graphics.Color.parseColor("#F87171"));
            tvStatusDesc.setText("Risiko kesehatan meningkat");
        }
    }
    private void logAktivitas(String title, String desc, boolean isActive) {
        DatabaseReference logRef = FirebaseDatabase.getInstance("https://smartliving-425c0-default-rtdb.asia-southeast1.firebasedatabase.app")
            .getReference("aktivitas");
        String id = logRef.push().getKey();
        Aktivitas log = new Aktivitas(title, desc, System.currentTimeMillis(), isActive);
        if (id != null) logRef.child(id).setValue(log);
    }
}
