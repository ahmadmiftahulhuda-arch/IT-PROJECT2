package com.example.it_project2;

import androidx.appcompat.app.AppCompatActivity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.widget.TextView;

import com.github.mikephil.charting.charts.LineChart;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.google.android.material.bottomnavigation.BottomNavigationView;

import java.util.ArrayList;
import java.util.List;

public class RiwayatActivity extends AppCompatActivity {

    private LineChart lineChart;
    private TextView tabHariIni, tab7Hari, tab30Hari;

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

        // Setup grafik dengan data dummy
        setupChart();

        // Tab listener
        tabHariIni.setOnClickListener(v -> setActiveTab(0));
        tab7Hari.setOnClickListener(v -> setActiveTab(1));
        tab30Hari.setOnClickListener(v -> setActiveTab(2));

        // Bottom nav
        setupBottomNav();
    }

    private void setupChart() {
        // Data dummy PM2.5 per jam — nanti diganti data Firestore
        List<Entry> entries = new ArrayList<>();
        float[] data = {15f, 20f, 45f, 65f, 55f, 40f, 30f, 25f, 20f, 18f, 15f, 12f};
        for (int i = 0; i < data.length; i++) {
            entries.add(new Entry(i, data[i]));
        }

        LineDataSet dataSet = new LineDataSet(entries, "PM2.5");
        dataSet.setColor(Color.parseColor("#2563EB"));
        dataSet.setLineWidth(2.5f);
        dataSet.setCircleColor(Color.parseColor("#2563EB"));
        dataSet.setCircleRadius(4f);
        dataSet.setDrawFilled(true);
        dataSet.setFillColor(Color.parseColor("#DBEAFE"));
        dataSet.setValueTextSize(0f); // Sembunyikan value label
        dataSet.setMode(LineDataSet.Mode.CUBIC_BEZIER); // Garis melengkung

        // Garis batas BAHAYA (merah putus-putus)
        List<Entry> dangerLine = new ArrayList<>();
        for (int i = 0; i < data.length; i++) {
            dangerLine.add(new Entry(i, 50f));
        }
        LineDataSet dangerSet = new LineDataSet(dangerLine, "Bahaya");
        dangerSet.setColor(Color.parseColor("#DC2626"));
        dangerSet.setLineWidth(1f);
        dangerSet.enableDashedLine(10f, 5f, 0f);
        dangerSet.setDrawCircles(false);
        dangerSet.setValueTextSize(0f);

        // Styling chart
        lineChart.setData(new LineData(dataSet, dangerSet));
        lineChart.getDescription().setEnabled(false);
        lineChart.getLegend().setEnabled(false);
        lineChart.setTouchEnabled(true);
        lineChart.setPinchZoom(false);

        // X Axis
        XAxis xAxis = lineChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setDrawGridLines(false);
        xAxis.setDrawLabels(false);

        // Y Axis
        lineChart.getAxisLeft().setDrawGridLines(true);
        lineChart.getAxisLeft().setGridColor(Color.parseColor("#F1F5F9"));
        lineChart.getAxisRight().setEnabled(false);

        lineChart.animateX(1000);
        lineChart.invalidate();
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
