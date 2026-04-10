package com.example.it_project2;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.Gravity;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationServices;
import com.google.android.gms.location.Priority;
import com.google.android.gms.tasks.CancellationTokenSource;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.android.material.card.MaterialCardView;

import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class InformasiActivity extends AppCompatActivity {

    // ===== API KEY OPENWEATHERMAP =====
    private static final String API_KEY = "2cfb11165aef08cf723950125e1f9ae0";
    private static final String BASE_URL = "https://api.openweathermap.org/data/2.5/";

    private static final int PERMISSION_REQUEST_CODE = 1001;

    // Views
    private TextView tvUserName;
    private TextView tvCity, tvDate, tvTime;
    private TextView tvTemperature, tvCondition, tvHumidity, tvWind;
    private ImageView ivWeatherIcon;
    private TextView tvAQI, tvAQIBadge, tvAQIDescription;
    private TextView tvHealthAdvice;
    private View aqiIndicator;
    private View aqiRingContainer;
    private LinearLayout llHourlyForecast;

    private FusedLocationProviderClient fusedLocationClient;
    private final ExecutorService executor = Executors.newFixedThreadPool(3);
    private final Handler mainHandler = new Handler(Looper.getMainLooper());

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_informasi);

        if (getSupportActionBar() != null) getSupportActionBar().hide();

        // Bind views
        tvUserName      = findViewById(R.id.tvUserName);
        tvCity          = findViewById(R.id.tvCity);
        tvDate          = findViewById(R.id.tvDate);
        tvTime          = findViewById(R.id.tvTime);
        tvTemperature   = findViewById(R.id.tvTemperature);
        tvCondition     = findViewById(R.id.tvCondition);
        tvHumidity      = findViewById(R.id.tvHumidity);
        tvWind          = findViewById(R.id.tvWind);
        ivWeatherIcon   = findViewById(R.id.ivWeatherIcon);
        tvAQI           = findViewById(R.id.tvAQI);
        tvAQIBadge      = findViewById(R.id.tvAQIBadge);
        tvAQIDescription = findViewById(R.id.tvAQIDescription);
        tvHealthAdvice  = findViewById(R.id.tvHealthAdvice);
        aqiIndicator    = findViewById(R.id.aqiIndicator);
        aqiRingContainer = findViewById(R.id.aqiRingContainer);
        llHourlyForecast = findViewById(R.id.llHourlyForecast);

        // Tampilkan username
        SessionManager sessionManager = new SessionManager(this);
        String name = sessionManager.getUserName();
        if (name != null && !name.isEmpty() && tvUserName != null) {
            tvUserName.setText(name);
        }

        // Tampilkan tanggal & jam sekarang
        updateDateTime();

        // Inisialisasi Location Client
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Minta izin lokasi
        if (hasLocationPermission()) {
            fetchLocation();
        } else {
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                                 Manifest.permission.ACCESS_COARSE_LOCATION},
                    PERMISSION_REQUEST_CODE);
        }

        // Bottom Navigation
        setupBottomNav();
    }

    // ===== DATE & TIME =====
    private void updateDateTime() {
        Date now = new Date();

        // Format tanggal: "Senin, 14 April"
        SimpleDateFormat dateFormat = new SimpleDateFormat("EEEE, d MMMM", new Locale("id", "ID"));
        tvDate.setText(dateFormat.format(now));

        // Format jam: "10:45 AM"
        SimpleDateFormat timeFormat = new SimpleDateFormat("hh:mm a", Locale.getDefault());
        tvTime.setText(timeFormat.format(now));
    }

    // ===== PERMISSION =====
    private boolean hasLocationPermission() {
        return ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions,
                                           @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                fetchLocation();
            } else {
                Toast.makeText(this, "Izin lokasi diperlukan untuk menampilkan cuaca.",
                        Toast.LENGTH_LONG).show();
                // Load dengan lokasi default (Banjarmasin / Kalimantan Selatan)
                loadWeatherData(-3.3194, 114.5905);
            }
        }
    }

    // ===== GET GPS LOCATION =====
    @SuppressWarnings("MissingPermission")
    private void fetchLocation() {
        CancellationTokenSource cts = new CancellationTokenSource();
        fusedLocationClient.getCurrentLocation(Priority.PRIORITY_HIGH_ACCURACY, cts.getToken())
                .addOnSuccessListener(this, location -> {
                    if (location != null) {
                        double lat = location.getLatitude();
                        double lon = location.getLongitude();
                        getCityName(lat, lon);
                        loadWeatherData(lat, lon);
                    } else {
                        // Fallback ke last known location
                        fusedLocationClient.getLastLocation()
                                .addOnSuccessListener(lastLoc -> {
                                    if (lastLoc != null) {
                                        getCityName(lastLoc.getLatitude(), lastLoc.getLongitude());
                                        loadWeatherData(lastLoc.getLatitude(), lastLoc.getLongitude());
                                    } else {
                                        tvCity.setText("LOKASI TIDAK TERSEDIA");
                                        loadWeatherData(-3.3194, 114.5905); // Default Banjarmasin
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    tvCity.setText("GAGAL AMBIL LOKASI");
                    loadWeatherData(-3.3194, 114.5905);
                });
    }

    // ===== GEOCODER: Koordinat → Nama Kota =====
    private void getCityName(double lat, double lon) {
        executor.execute(() -> {
            try {
                Geocoder geocoder = new Geocoder(this, new Locale("id", "ID"));
                List<Address> addresses = geocoder.getFromLocation(lat, lon, 1);
                if (addresses != null && !addresses.isEmpty()) {
                    Address addr = addresses.get(0);
                    // Prioritas: subLocality (kecamatan) → locality (kota) → adminArea (provinsi)
                    String city = addr.getSubLocality();
                    if (city == null || city.isEmpty()) city = addr.getLocality();
                    if (city == null || city.isEmpty()) city = addr.getSubAdminArea();
                    if (city == null || city.isEmpty()) city = addr.getAdminArea();
                    final String cityName = (city != null) ? city.toUpperCase() : "LOKASI TIDAK DIKETAHUI";
                    mainHandler.post(() -> tvCity.setText(cityName));
                }
            } catch (Exception e) {
                mainHandler.post(() -> tvCity.setText("LOKASI TIDAK TERSEDIA"));
            }
        });
    }

    // ===== LOAD SEMUA DATA CUACA =====
    private void loadWeatherData(double lat, double lon) {
        fetchCurrentWeather(lat, lon);
        fetchAirQuality(lat, lon);
        fetchHourlyForecast(lat, lon);
    }

    // ===== API 1: CUACA SEKARANG =====
    private void fetchCurrentWeather(double lat, double lon) {
        String urlStr = BASE_URL + "weather?lat=" + lat + "&lon=" + lon
                + "&appid=" + API_KEY + "&units=metric&lang=id";

        executor.execute(() -> {
            try {
                String response = httpGet(urlStr);
                JSONObject json = new JSONObject(response);

                double temp = json.getJSONObject("main").getDouble("temp");
                int humidity = json.getJSONObject("main").getInt("humidity");
                double windSpeed = json.getJSONObject("wind").getDouble("speed");
                String conditionId = json.getJSONArray("weather")
                        .getJSONObject(0).getString("description");
                int weatherCode = json.getJSONArray("weather")
                        .getJSONObject(0).getInt("id");

                // Kapitalisasi pertama
                String condition = conditionId.substring(0, 1).toUpperCase()
                        + conditionId.substring(1);

                String tempStr = String.format(Locale.getDefault(), "%.0f°C", temp);
                String humStr = humidity + "%";
                String windStr = String.format(Locale.getDefault(), "%.0f km/h", windSpeed * 3.6);
                int iconRes = getWeatherIconRes(weatherCode);

                mainHandler.post(() -> {
                    tvTemperature.setText(tempStr);
                    tvCondition.setText(condition);
                    tvHumidity.setText(humStr);
                    tvWind.setText(windStr);
                    ivWeatherIcon.setImageResource(iconRes);
                });

            } catch (Exception e) {
                mainHandler.post(() -> tvCondition.setText("Gagal memuat cuaca"));
            }
        });
    }

    // ===== API 2: KUALITAS UDARA (AQI) =====
    private void fetchAirQuality(double lat, double lon) {
        String urlStr = BASE_URL + "air_pollution?lat=" + lat + "&lon=" + lon
                + "&appid=" + API_KEY;

        executor.execute(() -> {
            try {
                String response = httpGet(urlStr);
                JSONObject json = new JSONObject(response);
                JSONObject components = json.getJSONArray("list")
                        .getJSONObject(0).getJSONObject("components");

                double pm25 = components.getDouble("pm2_5");
                int aqiIndex = (int) Math.round(pm25);

                // Tentukan kategori AQI
                String badge, desc, advice;
                int badgeColor;
                float indicatorFraction; // 0.0 - 1.0

                if (pm25 <= 12) {
                    badge = "BAIK"; desc = "Kualitas udara baik. Tidak ada risiko kesehatan.";
                    advice = "Udara bersih dan segar. Aktivitas luar ruangan sangat aman dilakukan.";
                    badgeColor = 0xFF16A34A; indicatorFraction = 0.1f;
                } else if (pm25 <= 35) {
                    badge = "MODERATE"; desc = "Kualitas udara sedang. Penderita asma disarankan berhati-hati.";
                    advice = "Penderita asma atau alergi sebaiknya mengurangi aktivitas luar ruangan yang intens.";
                    badgeColor = 0xFFF59E0B; indicatorFraction = 0.4f;
                } else if (pm25 <= 55) {
                    badge = "TIDAK SEHAT"; desc = "Konsentrasi PM2.5 tinggi. Berbahaya bagi kelompok rentan.";
                    advice = "Gunakan masker saat beraktivitas di luar ruangan. Penderita asma wajib menghindari area luar.";
                    badgeColor = 0xFFEF4444; indicatorFraction = 0.65f;
                } else {
                    badge = "BERBAHAYA"; desc = "Kualitas udara sangat buruk. Semua orang berisiko terdampak.";
                    advice = "Tetap di dalam ruangan. Gunakan pembersih udara. Jangan beraktivitas di luar.";
                    badgeColor = 0xFF7C3AED; indicatorFraction = 0.9f;
                }

                final String finalBadge = badge, finalDesc = desc, finalAdvice = advice;
                final int finalColor = badgeColor;
                final float finalFraction = indicatorFraction;
                final int aqiFinal = aqiIndex;

                mainHandler.post(() -> {
                    tvAQI.setText(String.valueOf(aqiFinal));
                    tvAQIBadge.setText(finalBadge);
                    tvAQIBadge.setTextColor(finalColor);

                    // Dynamic badge background (15% opacity of the actual color)
                    android.graphics.drawable.GradientDrawable badgeBg = new android.graphics.drawable.GradientDrawable();
                    badgeBg.setColor(android.graphics.Color.argb(38, android.graphics.Color.red(finalColor), android.graphics.Color.green(finalColor), android.graphics.Color.blue(finalColor)));
                    badgeBg.setCornerRadius(dpToPx(6));
                    tvAQIBadge.setBackground(badgeBg);

                    // Dynamic ring color
                    if (aqiRingContainer != null) {
                        try {
                            android.graphics.drawable.GradientDrawable ring = (android.graphics.drawable.GradientDrawable) androidx.core.content.ContextCompat.getDrawable(InformasiActivity.this, R.drawable.bg_aqi_ring).mutate();
                            ring.setStroke(dpToPx(4), finalColor);
                            aqiRingContainer.setBackground(ring);
                        } catch (Exception ignored) {}
                    }

                    tvAQIDescription.setText(finalDesc);
                    tvHealthAdvice.setText(finalAdvice);

                    // Posisi indikator dot di bar gradient
                    aqiIndicator.post(() -> {
                        int barWidth = ((View) aqiIndicator.getParent()).getWidth();
                        int dotWidth = aqiIndicator.getWidth();
                        int margin = (int) (finalFraction * barWidth) - dotWidth / 2;
                        if (margin < 0) margin = 0;
                        if (margin > barWidth - dotWidth) margin = barWidth - dotWidth;
                        android.widget.FrameLayout.LayoutParams params = new android.widget.FrameLayout.LayoutParams(
                                dotWidth, aqiIndicator.getHeight());
                        params.leftMargin = margin;
                        params.gravity = android.view.Gravity.CENTER_VERTICAL;
                        aqiIndicator.setLayoutParams(params);
                    });
                });

            } catch (Exception e) {
                mainHandler.post(() -> {
                    tvAQI.setText("--");
                    tvAQIDescription.setText("Gagal memuat data kualitas udara.");
                });
            }
        });
    }

    // ===== API 3: PRAKIRAAN PER JAM =====
    private void fetchHourlyForecast(double lat, double lon) {
        String urlStr = BASE_URL + "forecast?lat=" + lat + "&lon=" + lon
                + "&appid=" + API_KEY + "&units=metric&lang=id&cnt=8";

        executor.execute(() -> {
            try {
                String response = httpGet(urlStr);
                JSONObject json = new JSONObject(response);
                JSONArray list = json.getJSONArray("list");

                mainHandler.post(() -> llHourlyForecast.removeAllViews());

                for (int i = 0; i < list.length(); i++) {
                    JSONObject item = list.getJSONObject(i);
                    long dt = item.getLong("dt") * 1000;
                    double temp = item.getJSONObject("main").getDouble("temp");
                    int weatherCode = item.getJSONArray("weather")
                            .getJSONObject(0).getInt("id");

                    SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                    String timeStr = sdf.format(new Date(dt));
                    String tempStr = String.format(Locale.getDefault(), "%.0f°", temp);
                    int iconRes = getWeatherIconRes(weatherCode);
                    boolean isFirst = (i == 0);

                    mainHandler.post(() -> addHourlyCard(timeStr, iconRes, tempStr, isFirst));
                }

            } catch (Exception e) {
                mainHandler.post(() -> {
                    // Tampilkan placeholder jika gagal
                });
            }
        });
    }

    // ===== Tambah card prakiraan ke HorizontalScrollView =====
    private void addHourlyCard(String time, int iconRes, String temp, boolean isActive) {
        // Container card
        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setGravity(Gravity.CENTER);

        int widthDp = 72;
        int heightDp = LinearLayout.LayoutParams.WRAP_CONTENT;
        int marginPx = dpToPx(8);
        int paddingPx = dpToPx(12);

        LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(dpToPx(widthDp), heightDp);
        params.setMargins(0, 0, marginPx, 0);
        card.setLayoutParams(params);
        card.setPadding(paddingPx, dpToPx(14), paddingPx, dpToPx(14));

        if (isActive) {
            card.setBackgroundResource(android.R.color.transparent);
            // Buat MaterialCardView untuk card aktif (biru gelap)
            MaterialCardView mcv = new MaterialCardView(this);
            LinearLayout.LayoutParams mcvParams = new LinearLayout.LayoutParams(dpToPx(widthDp), heightDp);
            mcvParams.setMargins(0, 0, marginPx, 0);
            mcv.setLayoutParams(mcvParams);
            mcv.setRadius(dpToPx(14));
            mcv.setCardElevation(0);
            mcv.setCardBackgroundColor(0xFF2563EB);
            mcv.setStrokeWidth(0);

            LinearLayout inner = new LinearLayout(this);
            inner.setOrientation(LinearLayout.VERTICAL);
            inner.setGravity(Gravity.CENTER);
            inner.setPadding(paddingPx, dpToPx(14), paddingPx, dpToPx(14));

            TextView tvTime = new TextView(this);
            tvTime.setText(time);
            tvTime.setTextSize(11);
            tvTime.setTextColor(0xFFFFFFFF);
            tvTime.setGravity(Gravity.CENTER);

            ImageView ivIcon = new ImageView(this);
            ivIcon.setImageResource(iconRes);
            ivIcon.setColorFilter(0xFFFFFFFF); // White icon for active card
            LinearLayout.LayoutParams iconParams = new LinearLayout.LayoutParams(dpToPx(28), dpToPx(28));
            iconParams.setMargins(0, dpToPx(8), 0, dpToPx(8));
            ivIcon.setLayoutParams(iconParams);

            TextView tvTemp = new TextView(this);
            tvTemp.setText(temp);
            tvTemp.setTextSize(14);
            tvTemp.setTextColor(0xFFFFFFFF);
            tvTemp.setTypeface(null, android.graphics.Typeface.BOLD);
            tvTemp.setGravity(Gravity.CENTER);

            inner.addView(tvTime);
            inner.addView(ivIcon);
            inner.addView(tvTemp);
            mcv.addView(inner);
            llHourlyForecast.addView(mcv);
            return;
        }

        // Card non-aktif (putih dengan border)
        MaterialCardView mcv = new MaterialCardView(this);
        LinearLayout.LayoutParams mcvParams = new LinearLayout.LayoutParams(dpToPx(widthDp), heightDp);
        mcvParams.setMargins(0, 0, marginPx, 0);
        mcv.setLayoutParams(mcvParams);
        mcv.setRadius(dpToPx(14));
        mcv.setCardElevation(0);
        mcv.setCardBackgroundColor(0xFFFFFFFF);
        mcv.setStrokeColor(0xFFEDEFF2);
        mcv.setStrokeWidth(dpToPx(1));

        LinearLayout inner = new LinearLayout(this);
        inner.setOrientation(LinearLayout.VERTICAL);
        inner.setGravity(Gravity.CENTER);
        inner.setPadding(paddingPx, dpToPx(14), paddingPx, dpToPx(14));

        TextView tvTime2 = new TextView(this);
        tvTime2.setText(time);
        tvTime2.setTextSize(11);
        tvTime2.setTextColor(0xFF94A3B8);
        tvTime2.setGravity(Gravity.CENTER);

        ImageView ivIcon2 = new ImageView(this);
        ivIcon2.setImageResource(iconRes);
        LinearLayout.LayoutParams iconParams2 = new LinearLayout.LayoutParams(dpToPx(28), dpToPx(28));
        iconParams2.setMargins(0, dpToPx(8), 0, dpToPx(8));
        ivIcon2.setLayoutParams(iconParams2);

        TextView tvTemp2 = new TextView(this);
        tvTemp2.setText(temp);
        tvTemp2.setTextSize(14);
        tvTemp2.setTextColor(0xFF1E293B);
        tvTemp2.setTypeface(null, android.graphics.Typeface.BOLD);
        tvTemp2.setGravity(Gravity.CENTER);

        inner.addView(tvTime2);
        inner.addView(ivIcon2);
        inner.addView(tvTemp2);
        mcv.addView(inner);
        llHourlyForecast.addView(mcv);
    }

    // ===== HELPER: Kode cuaca → Resource Drawable =====
    private int getWeatherIconRes(int code) {
        if (code >= 200 && code < 300) return R.drawable.ic_weather_thunder;
        if (code >= 300 && code < 400) return R.drawable.ic_weather_rain;
        if (code >= 500 && code < 510) return R.drawable.ic_weather_rain;
        if (code == 511) return R.drawable.ic_weather_snow;
        if (code >= 520 && code < 600) return R.drawable.ic_weather_rain;
        if (code >= 600 && code < 700) return R.drawable.ic_weather_snow;
        if (code >= 700 && code < 800) return R.drawable.ic_weather_cloudy; // fog/dust
        if (code == 800) return R.drawable.ic_weather_sunny;
        if (code == 801) return R.drawable.ic_weather_sunny; // few clouds
        if (code == 802) return R.drawable.ic_weather_cloudy;
        if (code == 803 || code == 804) return R.drawable.ic_weather_cloudy;
        return R.drawable.ic_weather_sunny;
    }

    // ===== HELPER: HTTP GET =====
    private String httpGet(String urlStr) throws Exception {
        URL url = new URL(urlStr);
        HttpURLConnection conn = (HttpURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10000);
        conn.setReadTimeout(10000);
        conn.connect();

        BufferedReader reader = new BufferedReader(
                new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        reader.close();
        conn.disconnect();
        return sb.toString();
    }

    // ===== HELPER: dp → px =====
    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    // ===== BOTTOM NAV =====
    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_informasi);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_riwayat) {
                startActivity(new Intent(this, RiwayatActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_kontrol) {
                startActivity(new Intent(this, KontrolActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_informasi) {
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return false;
        });
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        executor.shutdown();
    }
}
