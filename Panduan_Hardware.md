# Panduan Integrasi ESP32 + DHT22 + Firebase Realtime Database

Dokumen ini berisi panduan teknis untuk merakit hardware dan memprogram ESP32 agar dapat mengirim data suhu dan kelembapan ke Firebase Realtime Database.

## 1. Skema Wiring (Kabel)

Berdasarkan foto hardware yang digunakan (NodeMCU ESP-32S V1.1):

| Komponen DHT22 | ESP32 Pin | Warna Kabel (Contoh Foto) |
| :--- | :--- | :--- |
| **+ (VCC)** | **3V3** | Cokelat |
| **Out (Data)** | **P15 (GPIO 15)** | Merah |
| **- (GND)** | **GND** | Putih |

> [!CAUTION]
> Pastikan kabel **Cokelat (+)** tidak tertukar dengan kabel **Putih (-)** untuk menghindari kerusakan pada sensor.

## 2. Persiapan Software (Arduino IDE)

1.  **Boards Manager**: Tambahkan URL ini di Preferences: 
    `https://raw.githubusercontent.com/espressif/arduino-esp32/gh-pages/package_esp32_index.json`
2.  **Library Manager**: Install library berikut:
    *   `Firebase ESP32 Client` (oleh Mobizt)
    *   `DHT sensor library` (oleh Adafruit)
    *   `Adafruit Unified Sensor` (oleh Adafruit)

## 3. Konfigurasi Firebase

*   **Firebase Host**: `smartliving-425c0-default-rtdb.asia-southeast1.firebasedatabase.app`
*   **Database Secret**: Ambil dari *Project Settings* > *Service Accounts* > *Database Secrets*.

## 4. Kode Arduino (main.ino)

```cpp
#include <WiFi.h>
#include <FirebaseESP32.h>
#include <DHT.h>

// ISI DENGAN KREDENSIAL WIFI
#define WIFI_SSID "NAMA_WIFI_KAMU"
#define WIFI_PASSWORD "PASSWORD_WIFI_KAMU"

// ISI DENGAN DETAIL FIREBASE
#define FIREBASE_HOST "smartliving-425c0-default-rtdb.asia-southeast1.firebasedatabase.app"
#define FIREBASE_AUTH "DATABASE_SECRET_KAMU"

// SETTING PIN SESUAI HARDWARE
#define DHTPIN 15 
#define DHTTYPE DHT22
DHT dht(DHTPIN, DHTTYPE);

FirebaseData firebaseData;
FirebaseConfig config;
FirebaseAuth auth;

void setup() {
  Serial.begin(115200);
  dht.begin();

  WiFi.begin(WIFI_SSID, WIFI_PASSWORD);
  Serial.print("Menghubungkan WiFi");
  while (WiFi.status() != WL_CONNECTED) {
    Serial.print(".");
    delay(500);
  }
  Serial.println("\nWiFi Terhubung!");

  config.host = FIREBASE_HOST;
  config.signer.tokens.legacy_token = FIREBASE_AUTH;

  Firebase.begin(&config, &auth);
  Firebase.reconnectWiFi(true);
}

void loop() {
  delay(2000); 

  float t = dht.readTemperature();
  float h = dht.readHumidity();

  if (isnan(t) || isnan(h)) {
    Serial.println("Gagal baca sensor! Cek kabel.");
    return;
  }

  Serial.printf("Suhu: %.1f°C | Lembap: %.1f%%\n", t, h);

  // Kirim data ke path yang dibaca aplikasi Android
  if (Firebase.setDouble(firebaseData, "/sensor/suhu", t)) {
    Serial.println(">>> Data Terkirim!");
  } else {
    Serial.println("Gagal kirim: " + firebaseData.errorReason());
  }

  Firebase.setDouble(firebaseData, "/sensor/kelembapan", h);
}
```

## 5. Troubleshooting
*   **Failed to Read Sensor**: Periksa apakah pin data sudah benar di P15. Pastikan sensor mendapatkan daya 3.3V.
*   **Firebase Connection Failed**: Periksa apakah Database Secret (Auth) sudah benar dan Rule Database di Firebase diset ke `true`.
*   **Library Error**: Pastikan menggunakan library `Firebase ESP32 Client` oleh Mobizt, bukan library lain yang serupa.
