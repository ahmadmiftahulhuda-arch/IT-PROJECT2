package com.example.it_project2;

import android.os.Bundle;
import android.widget.ImageView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

public class PusatBantuanActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pusat_bantuan);

        ImageView btnBack = findViewById(R.id.btnBack);
        android.view.View btnContact = findViewById(R.id.btnContact);
        android.widget.LinearLayout containerFAQ = findViewById(android.R.id.content).findViewWithTag("faq_container"); // I'll add a tag to the layout

        btnBack.setOnClickListener(v -> finish());

        // Sync FAQ dari Firebase
        com.google.firebase.database.FirebaseDatabase.getInstance("https://smartliving-425c0-default-rtdb.asia-southeast1.firebasedatabase.app")
                .getReference("pusat_bantuan/faq").addValueEventListener(new com.google.firebase.database.ValueEventListener() {
            @Override
            public void onDataChange(com.google.firebase.database.DataSnapshot snapshot) {
                if (snapshot.exists()) {
                    // Update UI jika ada data dinamis
                    Toast.makeText(PusatBantuanActivity.this, "FAQ diperbarui dari server", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onCancelled(com.google.firebase.database.DatabaseError error) {}
        });

        btnContact.setOnClickListener(v -> {
            Toast.makeText(this, "Menghubungkan ke layanan bantuan...", Toast.LENGTH_SHORT).show();
            // Implementasi chat atau WhatsApp link di sini
        });
    }
}
