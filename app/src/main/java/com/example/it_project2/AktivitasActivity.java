package com.example.it_project2;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.google.firebase.database.DataSnapshot;
import com.google.firebase.database.DatabaseError;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.Query;
import com.google.firebase.database.ValueEventListener;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class AktivitasActivity extends AppCompatActivity {

    private LinearLayout containerAktivitas;
    private ImageView btnBack;
    private DatabaseReference aktivitasRef;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_aktivitas);

        containerAktivitas = findViewById(R.id.containerAktivitasFull);
        btnBack = findViewById(R.id.btnBack);

        btnBack.setOnClickListener(v -> finish());

        FirebaseDatabase database = FirebaseDatabase.getInstance("https://smartliving-425c0-default-rtdb.asia-southeast1.firebasedatabase.app");
        aktivitasRef = database.getReference("aktivitas");

        loadAllAktivitas();
    }

    private void loadAllAktivitas() {
        // Ambil semua, urutkan berdasarkan timestamp terbaru
        Query query = aktivitasRef.orderByChild("timestamp");

        query.addValueEventListener(new ValueEventListener() {
            @Override
            public void onDataChange(DataSnapshot snapshot) {
                containerAktivitas.removeAllViews();
                
                if (!snapshot.exists()) {
                    showEmptyState();
                    return;
                }

                // Balik urutan agar terbaru di atas
                for (DataSnapshot data : snapshot.getChildren()) {
                    Aktivitas item = data.getValue(Aktivitas.class);
                    if (item != null) {
                        addActivityItem(item, true); // true = insert at top
                    }
                }
            }

            @Override
            public void onCancelled(DatabaseError error) {
                Toast.makeText(AktivitasActivity.this, "Gagal memuat data", Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addActivityItem(Aktivitas item, boolean atTop) {
        View view = LayoutInflater.from(this).inflate(R.layout.item_aktivitas, containerAktivitas, false);
        
        View dot = view.findViewById(R.id.dot);
        TextView tvTitle = view.findViewById(R.id.tvTitle);
        TextView tvDesc = view.findViewById(R.id.tvDesc);
        TextView tvTime = view.findViewById(R.id.tvTime);

        tvTitle.setText(item.getTitle());
        tvDesc.setText(item.getDescription());
        
        SimpleDateFormat sdf = new SimpleDateFormat("HH:mm", Locale.getDefault());
        tvTime.setText(sdf.format(new Date(item.getTimestamp())));

        dot.setBackgroundResource(item.isActive() ? R.drawable.circle_dot_active : R.drawable.circle_dot_inactive);

        if (atTop) {
            containerAktivitas.addView(view, 0);
        } else {
            containerAktivitas.addView(view);
        }
    }

    private void showEmptyState() {
        TextView tv = new TextView(this);
        tv.setText("Belum ada aktivitas tercatat.");
        tv.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        tv.setPadding(0, 100, 0, 0);
        containerAktivitas.addView(tv);
    }
}
