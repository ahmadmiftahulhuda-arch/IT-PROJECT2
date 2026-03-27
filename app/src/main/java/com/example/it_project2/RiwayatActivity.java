package com.example.it_project2;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;
import android.widget.Toast;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.android.material.bottomnavigation.BottomNavigationView;
import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RiwayatActivity extends AppCompatActivity {

    private LineChart lineChart;
    private TextView tabHariIni, tab7Hari, tab30Hari;
    private TextView tvSuhuTerakhir, tvKelembapanTerakhir;

    // Firebase
    private DatabaseReference riwayatRef;
    private DatabaseReference sensorRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_riwayat);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Bind view
        lineChart  = findViewById(R.id.lineChart);
        tabHariIni = findViewById(R.id.tabHariIni);
        tab7Hari   = findViewById(R.id.tab7Hari);
        tab30Hari  = findViewById(R.id.tab30Hari);

        // Tombol back
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // ===== FIREBASE =====
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://smartliving-425c0-default-rtdb.asia-southeast1.firebasedatabase.app");
        riwayatRef = database.getReference("riwayat_suhu");
        sensorRef = database.getReference("sensor");

        // Load data riwayat dari Firebase
        loadRiwayatData();

        // Tab listener
        tabHariIni.setOnClickListener(v -> {
            setActiveTab(0);
            loadRiwayatData();
        });
        tab7Hari.setOnClickListener(v -> {
            setActiveTab(1);
            loadRiwayatData();
        });
        tab30Hari.setOnClickListener(v -> {
            setActiveTab(2);
            loadRiwayatData();
        });

        // Bottom nav
        setupBottomNav();
    }

    /**
     * Load data riwayat suhu dari Firebase Realtime Database.
     * Struktur data di Firebase:
     * riwayat_suhu/
     *   - {pushId}/
     *     - suhu: 26.5
     *     - kelembapan: 55
     *     - timestamp: 1711550400000
     */
    private void loadRiwayatData() {
        // Ambil 24 data terakhir (1 data per jam = 24 jam)
        Query query = riwayatRef.orderByChild("timestamp").limitToLast(24);

        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<Entry> suhuEntries = new ArrayList<>();
                List<String> timeLabels = new ArrayList<>();
                int index = 0;

                for (DataSnapshot data : snapshot.getChildren()) {
                    Double suhu = data.child("suhu").getValue(Double.class);
                    Long timestamp = data.child("timestamp").getValue(Long.class);

                    if (suhu != null) {
                        suhuEntries.add(new Entry(index, suhu.floatValue()));

                        if (timestamp != null) {
                            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
                            timeLabels.add(sdf.format(new Date(timestamp)));
                        } else {
                            timeLabels.add(String.valueOf(index));
                        }
                        index++;
                    }
                }

                if (suhuEntries.isEmpty()) {
                    // Jika belum ada data, tampilkan data dummy
                    setupChartDummy();
                } else {
                    setupChart(suhuEntries, timeLabels);
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(RiwayatActivity.this, "Gagal memuat riwayat", Toast.LENGTH_SHORT).show();
                setupChartDummy();
            }
        });
    }

    private void setupChart(List<Entry> entries, List<String> labels) {
        LineDataSet dataSet = new LineDataSet(entries, "Suhu (°C)");
        dataSet.setColor(Color.parseColor("#2563EB"));
        dataSet.setLineWidth(2.5f);
        dataSet.setCircleColor(Color.parseColor("#2563EB"));
        dataSet.setCircleRadius(4f);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#DBEAFE"));
        dataSet.setValueTextSize(0f);
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER);

        // Garis batas BAHAYA suhu < 20°C (merah putus-putus)
        List<Entry> dangerLine = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            dangerLine.add(new Entry(i, 20f));
        }
        LineDataSet dangerSet = new LineDataSet(dangerLine, "Batas Bahaya (20°C)");
        dangerSet.setColor(Color.parseColor("#DC2626"));
        dangerSet.setLineWidth(1f);
        dangerSet.enableDashedLine(10f, 5f, 0f);
        dangerSet.setDrawCircles(false);
        dangerSet.setValueTextSize(0f);

        // Styling chart
        lineChart.setData(new LineData(dataSet, dangerSet));
        lineChart.getDescription().setEnabled(false);
        lineChart.getLegend().setEnabled(true);
        lineChart.setTouchEnabled(true);
        lineChart.setPinchZoom(false);

        // X Axis
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setGranularity(1f);
        if (!labels.isEmpty()) {
            xAxis.setValueFormatter(new ValueFormatter() {
                @Override
                public String getFormattedValue(float value) {
                    int idx = (int) value;
                    if (idx >= 0 && idx < labels.size()) {
                        return labels.get(idx);
                    }
                    return "";
                }
            });
        }

        // Y Axis
        lineChart.getAxisLeft().setDrawGridLines(true);
        lineChart.getAxisLeft().setGridColor(Color.parseColor("#F1F5F9"));
        lineChart.getAxisRight().setEnabled(false);

        lineChart.animateX(1000);
        lineChart.invalidate();
    }

    /**
     * Setup chart dengan data dummy (jika Firebase belum punya data)
     */
    private void setupChartDummy() {
        List<Entry> entries = new ArrayList<>();
        float[] data = {25f, 24f, 23f, 21f, 19f, 18f, 19f, 21f, 23f, 24f, 25f, 26f};
        for (int i = 0; i < data.length; i++) {
            entries.add(new Entry(i, data[i]));
        }

        List<String> labels = new ArrayList<>();
        String[] hours = {"00:00", "02:00", "04:00", "06:00", "08:00", "10:00",
                         "12:00", "14:00", "16:00", "18:00", "20:00", "22:00"};
        for (String h : hours) labels.add(h);

        setupChart(entries, labels);
    }

    private void setActiveTab(int index) {
        // Reset semua tab
        tabHariIni.setBackground(null);
        tab7Hari.setBackground(null);
        tab30Hari.setBackground(null);
        tabHariIni.setTextColor(Color.parseColor("#94A3B8"));
        tab7Hari.setTextColor(Color.parseColor("#94A3B8"));
        tab30Hari.setTextColor(Color.parseColor("#94A3B8"));

        // Set tab aktif
        TextView[] tabs = {tabHariIni, tab7Hari, tab30Hari};
        tabs[index].setBackgroundResource(R.drawable.bg_tab_active);
        tabs[index].setTextColor(Color.parseColor("#2563EB"));
    }

    private void setupBottomNav() {
        BottomNavigationView bottomNav = findViewById(R.id.bottomNav);
        bottomNav.setSelectedItemId(R.id.nav_riwayat);
        bottomNav.setOnItemSelectedListener(item -> {
            int id = item.getItemId();
            if (id == R.id.nav_home) {
                startActivity(new Intent(this, MainActivity.class));
                finish();
            } else if (id == R.id.nav_kontrol) {
                startActivity(new Intent(this, KontrolActivity.class));
                finish();
            } else if (id == R.id.nav_edukasi) {
                startActivity(new Intent(this, EdukasiActivity.class));
                finish();
            } else if (id == R.id.nav_profile) {
                startActivity(new Intent(this, ProfileActivity.class));
                finish();
            }
            return true;
        });
    }
}
