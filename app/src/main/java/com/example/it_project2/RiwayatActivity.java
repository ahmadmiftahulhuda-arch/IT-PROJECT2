package com.example.it_project2;

import androidx.appcompat.app.AppCompatActivity;
import android.app.DatePickerDialog;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
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
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class RiwayatActivity extends AppCompatActivity {

    private LineChart lineChart;
    private TextView tabHariIni, tab7Hari, tab30Hari;
    private TextView tvAvgPM25;
    private ImageView btnFilterDate;

    // Firebase
    private DatabaseReference riwayatRef;
    private Calendar calendar = Calendar.getInstance();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_riwayat);

        if (getSupportActionBar() != null) {
            getSupportActionBar().hide();
        }

        // Bind view
        lineChart   = findViewById(R.id.lineChart);
        tabHariIni  = findViewById(R.id.tabHariIni);
        tab7Hari    = findViewById(R.id.tab7Hari);
        tab30Hari   = findViewById(R.id.tab30Hari);
        tvAvgPM25   = findViewById(R.id.tvAvgPM25);
        btnFilterDate = findViewById(R.id.btnFilterDate);

        // Tombol back
        findViewById(R.id.btnBack).setOnClickListener(v -> finish());

        // ===== FIREBASE =====
        FirebaseDatabase database = FirebaseDatabase.getInstance("https://smartliving-425c0-default-rtdb.asia-southeast1.firebasedatabase.app");
        riwayatRef = database.getReference("riwayat_suhu");

        // Load data default (Hari ini)
        loadDataRange(0);

        // Tab listener
        tabHariIni.setOnClickListener(v -> {
            setActiveTab(0);
            loadDataRange(0);
        });
        tab7Hari.setOnClickListener(v -> {
            setActiveTab(1);
            loadDataRange(7);
        });
        tab30Hari.setOnClickListener(v -> {
            setActiveTab(2);
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

                    loadRiwayatByRange(start, end, "Filter: " + dayOfMonth + "/" + (month + 1));
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
            // Hari ini (mulai jam 00:00)
            cal.set(Calendar.HOUR_OF_DAY, 0);
            cal.set(Calendar.MINUTE, 0);
            cal.set(Calendar.SECOND, 0);
        } else {
            cal.add(Calendar.DAY_OF_YEAR, -days);
        }
        long start = cal.getTimeInMillis();
        
        loadRiwayatByRange(start, end, null);
    }

    private void loadRiwayatByRange(long start, long end, String label) {
        Query query = riwayatRef.orderByChild("timestamp").startAt(start).endAt(end);

        query.addListenerForSingleValueEvent(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                List<Entry> suhuEntries = new ArrayList<>();
                List<String> timeLabels = new ArrayList<>();
                double totalSuhu = 0;
                int count = 0;

                for (DataSnapshot data : snapshot.getChildren()) {
                    Double suhu = data.child("suhu").getValue(Double.class);
                    Long timestamp = data.child("timestamp").getValue(Long.class);

                    if (suhu != null) {
                        suhuEntries.add(new Entry(count, suhu.floatValue()));
                        totalSuhu += suhu;

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
                    tvAvgPM25.setText(String.format(Locale.getDefault(), "%.1f", (totalSuhu / count)));
                    setupChart(suhuEntries, timeLabels);
                } else {
                    tvAvgPM25.setText("0");
                    setupChartDummy();
                    Toast.makeText(RiwayatActivity.this, "Tidak ada data untuk rentang ini", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(RiwayatActivity.this, "Gagal memuat data", Toast.LENGTH_SHORT).show();
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

        // Garis batas BAHAYA suhu > 50 (seperti di XML)
        List<Entry> dangerLine = new ArrayList<>();
        for (int i = 0; i < entries.size(); i++) {
            dangerLine.add(new Entry(i, 50f));
        }
        LineDataSet dangerSet = new LineDataSet(dangerLine, "Batas Bahaya (50)");
        dangerSet.setColor(Color.parseColor("#DC2626"));
        dangerSet.setLineWidth(1f);
        dangerSet.enableDashedLine(10f, 5f, 0f);
        dangerSet.setDrawCircles(false);
        dangerSet.setValueTextSize(0f);

        lineChart.setData(new LineData(dataSet, dangerSet));
        lineChart.getDescription().setEnabled(false);
        lineChart.getLegend().setEnabled(true);
        lineChart.setTouchEnabled(true);
        lineChart.setPinchZoom(false);

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

        lineChart.getAxisLeft().setDrawGridLines(true);
        lineChart.getAxisLeft().setGridColor(Color.parseColor("#F1F5F9"));
        lineChart.getAxisRight().setEnabled(false);

        lineChart.animateX(1000);
        lineChart.invalidate();
    }

    private void setupChartDummy() {
        List<Entry> entries = new ArrayList<>();
        for (int i = 0; i < 12; i++) entries.add(new Entry(i, 0f));
        List<String> labels = new ArrayList<>();
        for (int i = 0; i < 12; i++) labels.add("--:--");
        setupChart(entries, labels);
    }

    private void setActiveTab(int index) {
        resetTabs();
        TextView[] tabs = {tabHariIni, tab7Hari, tab30Hari};
        tabs[index].setBackgroundResource(R.drawable.bg_tab_active);
        tabs[index].setTextColor(Color.parseColor("#2563EB"));
    }

    private void resetTabs() {
        tabHariIni.setBackground(null);
        tab7Hari.setBackground(null);
        tab30Hari.setBackground(null);
        tabHariIni.setTextColor(Color.parseColor("#94A3B8"));
        tab7Hari.setTextColor(Color.parseColor("#94A3B8"));
        tab30Hari.setTextColor(Color.parseColor("#94A3B8"));
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
