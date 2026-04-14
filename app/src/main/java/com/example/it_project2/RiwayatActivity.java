package com.example.it_project2;

import androidx.appcompat.app.AppCompatActivity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.animation.Easing;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RiwayatActivity extends AppCompatActivity {

    private LineChart lineChart;
    private TextView tabHariIni, tab7Hari, tab30Hari;
    private TextView tvAvgSuhu, tvMinSuhu, tvMaxSuhu;
    private TextView tvSelectedDate, tvAnalysisContent;
    private TextView tvBannerTitle, tvBannerSub;
    private View btnFilterDate;

    // Firebase
    private DatabaseReference riwayatRef;
    private Calendar calendar = Calendar.getInstance();
    private SimpleDateFormat titleFormatter = new SimpleDateFormat("EEEE, d MMMM yyyy", new Locale("id", "ID"));

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_riwayat);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Bind view
        lineChart         = findViewById(R.id.lineChart);
        tabHariIni        = findViewById(R.id.tabHariIni);
        tab7Hari          = findViewById(R.id.tab7Hari);
        tab30Hari         = findViewById(R.id.tab30Hari);
        tvAvgSuhu         = findViewById(R.id.tvAvgSuhu);
        tvMinSuhu         = findViewById(R.id.tvMinSuhu);
        tvMaxSuhu         = findViewById(R.id.tvMaxSuhu);
        tvSelectedDate    = findViewById(R.id.tvSelectedDate);
        tvAnalysisContent = findViewById(R.id.tvAnalysisContent);
        tvBannerTitle     = findViewById(R.id.tvBannerTitle);
        tvBannerSub       = findViewById(R.id.tvBannerSub);
        btnFilterDate     = findViewById(R.id.btnFilterDate);

        // ===== FIREBASE =====
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://smartliving-425c0-default-rtdb.asia-southeast1.firebasedatabase.app");
        riwayatRef = database.getReference("riwayat_suhu");

        // Load data default (Hari ini)
        updateBanner("Hari Ini", "Melihat data sensor hari ini");
        loadDataRange(0);

        // Tab listener
        tabHariIni.setOnClickListener(v -> {
            setActiveTab(0);
            updateBanner("Hari Ini", "Melihat data sensor hari ini");
            loadDataRange(0);
        });
        tab7Hari.setOnClickListener(v -> {
            setActiveTab(1);
            updateBanner("7 Hari", "Rekap 7 hari terakhir");
            loadDataRange(7);
        });
        tab30Hari.setOnClickListener(v -> {
            setActiveTab(2);
            updateBanner("30 Hari", "Rekap 30 hari terakhir");
            loadDataRange(30);
        });

        // Filter tanggal spesifik
        btnFilterDate.setOnClickListener(v -> showDatePicker());

        // Bottom nav
        setupBottomNav();
    }

    private void showDatePicker() {
        DatePickerDialog datePickerDialog = new DatePickerDialog(
                this,
                (view, year, month, dayOfMonth) -> {
                    calendar.set(Calendar.YEAR, year);
                    calendar.set(Calendar.MONTH, month);
                    calendar.set(Calendar.DAY_OF_MONTH, dayOfMonth);
                    
                    // Set start of day
                    calendar.set(Calendar.HOUR_OF_DAY, 0);
                    calendar.set(Calendar.MINUTE, 0);
                    calendar.set(Calendar.SECOND, 0);
                    long start = calendar.getTimeInMillis();
                    
                    // Set end of day
                    calendar.set(Calendar.HOUR_OF_DAY, 23);
                    calendar.set(Calendar.MINUTE, 59);
                    calendar.set(Calendar.SECOND, 59);
                    long end = calendar.getTimeInMillis();

                    String label = titleFormatter.format(calendar.getTime());
                    tvSelectedDate.setText(label);
                    
                    loadRiwayatByRange(start, end);
                    resetTabs();
                },
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH)
        );
        datePickerDialog.show();
    }

    private void loadDataRange(int days) {
        Calendar cal = Calendar.getInstance();
        long end = cal.getTimeInMillis();
        
        if (days == 0) {
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
            tvSelectedDate.setText("Hari Ini (" + new SimpleDateFormat("d MMM", new Locale("id", "ID")).format(new Date()) + ")");
        } else {
            cal.add(Calendar.DAY_OF_YEAR, -days);
            tvSelectedDate.setText(days + " Hari Terakhir");
        }
        long start = cal.getTimeInMillis();
        
        loadRiwayatByRange(start, end);
    }

    private void loadRiwayatByRange(long start, long end) {
        Query query = riwayatRef.orderByChild("timestamp").startAt(start).endAt(end);
        
        // Update sub-label tanggal jika belum diset
        if (tvSelectedDate.getText().toString().equals("Memuat...")) {
            tvSelectedDate.setText("Mencari data...");
        }

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<Entry> suhuEntries = new ArrayList<>();
                List<String> timeLabels = new ArrayList<>();
                double totalSuhu = 0;
                double minSuhu = Double.MAX_VALUE;
                double maxSuhu = Double.MIN_VALUE;
                int count = 0;

                for (DataSnapshot data : snapshot.getChildren()) {
                    Double suhu = data.child("suhu").getValue(Double.class);
                    Long timestamp = data.child("timestamp").getValue(Long.class);

                    if (suhu != null) {
                        suhuEntries.add(new Entry(count, suhu.floatValue()));
                        totalSuhu += suhu;
                        
                        if (suhu < minSuhu) minSuhu = suhu;
                        if (suhu > maxSuhu) maxSuhu = suhu;

                        if (timestamp != null) {
                            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                            timeLabels.add(sdf.format(new Date(timestamp)));
                        } else {
                            timeLabels.add(String.valueOf(count));
                        }
                        count++;
                    }
                }

                if (count > 0) {
                    double avg = totalSuhu / count;
                    tvAvgSuhu.setText(String.format(Locale.getDefault(), "%.1f°", avg));
                    tvMinSuhu.setText(String.format(Locale.getDefault(), "%.1f°", minSuhu));
                    tvMaxSuhu.setText(String.format(Locale.getDefault(), "%.1f°", maxSuhu));
                    
                    updateAnalysis(avg, maxSuhu);
                    setupChart(suhuEntries, timeLabels);
                } else {
                    tvAvgSuhu.setText("0°");
                    tvMinSuhu.setText("0°");
                    tvMaxSuhu.setText("0°");
                    tvAnalysisContent.setText("Tidak ada data untuk rentang waktu ini.");
                    setupChartDummy();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(RiwayatActivity.this, "Gagal memuat data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void updateBanner(String title, String subtitle) {
        if (tvBannerTitle != null) tvBannerTitle.setText(title);
        if (tvBannerSub != null)   tvBannerSub.setText(subtitle);
    }

    private void updateAnalysis(double avg, double max) {
        String analysis;
        if (max > 40) {
            analysis = "Terdeteksi suhu sangat tinggi (" + String.format("%.1f", max) + "°C). Pastikan blower pendingin bekerja maksimal untuk mencegah overheat.";
            if (tvBannerSub != null) tvBannerSub.setText("⚠ Suhu kritis terdeteksi!");
        } else if (avg < 25) {
            analysis = "Rata-rata suhu cukup rendah. Pemanas mungkin perlu diaktifkan untuk menjaga stabilitas lingkungan.";
            if (tvBannerSub != null) tvBannerSub.setText("Suhu cenderung rendah");
        } else {
            analysis = "Suhu lingkungan stabil di angka " + String.format("%.1f", avg) + "°C. Kondisi ini ideal untuk operasional normal.";
            if (tvBannerSub != null) tvBannerSub.setText("✓ Kondisi suhu normal");
        }
        tvAnalysisContent.setText(analysis);
    }

    private void setupChart(List<Entry> entries, List<String> labels) {
        LineDataSet dataSet = new LineDataSet(entries, "Suhu (°C)");
        
        // Style: Line
        dataSet.setColor(Color.parseColor("#2563EB"));
        dataSet.setLineWidth(3f);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);
        dataSet.setDrawValues(false);
        
        // Style: Circles
        dataSet.setDrawCircles(true);
        dataSet.setCircleColor(Color.parseColor("#2563EB"));
        dataSet.setCircleRadius(4f);
        dataSet.setDrawCircleHole(true);
        dataSet.setCircleHoleColor(Color.WHITE);
        dataSet.setCircleHoleRadius(2f);
        
        // Style: Gradient Fill
        dataSet.setDrawFilled(true);
        GradientDrawable gradient = new GradientDrawable(
                GradientDrawable.Orientation.TOP_BOTTOM,
                new int[]{Color.parseColor("#402563EB"), Color.TRANSPARENT}
        );
        dataSet.setFillDrawable(gradient);

        // Limit Line (Danger)
        YAxis leftAxis = lineChart.getAxisLeft();
        leftAxis.removeAllLimitLines();
        
        LineData lineData = new LineData(dataSet);
        lineChart.setData(lineData);
        
        // General Chart Styling
        lineChart.getDescription().setEnabled(false);
        lineChart.getLegend().setEnabled(false);
        lineChart.setTouchEnabled(true);
        lineChart.setPinchZoom(false);
        lineChart.setDrawGridBackground(false);
        lineChart.setExtraOffsets(10, 10, 10, 10);

        // X-Axis Styling
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawAxisLine(true);
        xAxis.setAxisLineColor(Color.parseColor("#E2E8F0"));
        xAxis.setTextColor(Color.parseColor("#94A3B8"));
        xAxis.setGranularity(1f);
        if (!labels.isEmpty()) {
            xAxis.setValueFormatter(new ValueFormatter() {
                @Override
                public String getFormattedValue(float value) {
                    int idx = (int) value;
                    return (idx >= 0 && idx < labels.size()) ? labels.get(idx) : "";
                }
            });
        }

        // Y-Axis Styling
        leftAxis.setDrawGridLines(true);
        leftAxis.setGridColor(Color.parseColor("#F1F5F9"));
        leftAxis.setDrawAxisLine(false);
        leftAxis.setTextColor(Color.parseColor("#94A3B8"));
        
        lineChart.getAxisRight().setEnabled(false);

        lineChart.animateX(1500, Easing.EaseInOutCubic);
        lineChart.invalidate();
    }

    private void setupChartDummy() {
        List<Entry> entries = new ArrayList<>();
        List<String> labels = new ArrayList<>();
        setupChart(entries, labels);
    }

    private void setActiveTab(int index) {
        resetTabs();
        TextView[] tabs = {tabHariIni, tab7Hari, tab30Hari};
        tabs[index].setBackgroundResource(R.drawable.bg_tab_active);
        tabs[index].setTextColor(Color.WHITE);
    }

    private void resetTabs() {
        tabHariIni.setBackground(null);
        tab7Hari.setBackground(null);
        tab30Hari.setBackground(null);
        tabHariIni.setTextColor(Color.parseColor("#64748B"));
        tab7Hari.setTextColor(Color.parseColor("#64748B"));
        tab30Hari.setTextColor(Color.parseColor("#64748B"));
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_riwayat);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_kontrol) {
                startActivity(new Intent(this, KontrolActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_informasi) {
                startActivity(new Intent(this, InformasiActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                overridePendingTransition(0, 0);
                finish();
                return true;
            }
            return true;
        });
    }
}
