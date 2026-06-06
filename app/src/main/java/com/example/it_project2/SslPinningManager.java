package com.example.it_project2;

import android.util.Base64;
import android.util.Log;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URL;
import java.security.KeyStore;
import java.security.MessageDigest;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.net.ssl.HttpsURLConnection;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * SslPinningManager — Lapisan 2 SSL Pinning
 * ───────────────────────────────────────────
 * Validasi SPKI (SubjectPublicKeyInfo) SHA-256 hash secara PROGRAMATIK
 * untuk koneksi HttpsURLConnection ke OpenWeatherMap API.
 *
 * Lapisan 1 (Network Security Config XML) sudah aktif otomatis untuk semua
 * koneksi. Class ini menambahkan layer validasi tambahan khusus untuk
 * pemanggilan HTTP manual di InformasiActivity dan MainActivity.
 *
 * Cara pakai:
 *   String json = SslPinningManager.httpsGet("https://api.openweathermap.org/...");
 */
public class SslPinningManager {

    private static final String TAG = "SslPinningManager";

    /**
     * SPKI SHA-256 pins untuk OpenWeatherMap (Let's Encrypt / ISRG Root).
     * Sama persis dengan yang ada di network_security_config.xml.
     * Ekspirasi: 2027-01-01
     */
    private static final Set<String> OWM_PINS = new HashSet<>(Arrays.asList(
            "Douxi77vs4G+Ib/BogbTFymEYq0QSFXwSgVCaZcI09Q=",  // Sectigo Root R46
            "KqkYYX5LYAYP7XGemqzbtPPIA8x7BS/BbOIcAXf3j2k=",  // Sectigo CA OV R36
            "2rABlvP8a/45fRdYlmvSYEWrgBZyNampT8AqVpcPMtk=",  // openweathermap.org Leaf
            "C5+lpZ7tcVwmwQIMcRtPbsQtWLABXhQzejna0wHFr8M=",  // ISRG Root X1
            "diGVwiVYbubAI3RW4hB9xU8e/CH2GnkuvXFuIckypH4="   // ISRG Root X2
    ));

    /**
     * SPKI SHA-256 pins untuk Firebase / Google Trust Services.
     * Ekspirasi: 2027-01-01
     */
    private static final Set<String> FIREBASE_PINS = new HashSet<>(Arrays.asList(
            "hxqRlPTu1bMS/0DITB1SSu0vd4u/8l8TjPgfaAp63Gc=",  // GTS Root R1
            "Vfd95BwDeSZs+0YRPcQKrHjl3VqL1jj/7eGDjmpRN6U=",  // GTS Root R2
            "cGuxAXyFXFkWm61cF4HPWX8S0srS9j0aSqN0k4AP+4A=",  // GTS Root R3
            "ZMRmQs0DcaFMEYrUb5kVK2rnNlX0DqxKp0+LRj09Jnw="   // GTS Root R4
    ));

    // ── Prevent instantiation ────────────────────────────────────────────────
    private SslPinningManager() {}

    /**
     * Lakukan HTTPS GET dengan validasi SSL Pinning.
     *
     * @param urlStr URL lengkap (harus https://)
     * @return Respons body sebagai String
     * @throws Exception jika koneksi gagal, timeout, atau pin tidak cocok
     */
    public static String httpsGet(String urlStr) throws Exception {
        // Tentukan set pin berdasarkan domain
        Set<String> pins = urlStr.contains("openweathermap") ? OWM_PINS : FIREBASE_PINS;

        URL url = new URL(urlStr);
        String hostname = url.getHost();
        HttpsURLConnection conn = (HttpsURLConnection) url.openConnection();
        conn.setRequestMethod("GET");
        conn.setConnectTimeout(10_000);
        conn.setReadTimeout(10_000);

        // Pasang custom SSLSocketFactory dengan pinned TrustManager (hostname aware)
        SSLContext sslContext = SSLContext.getInstance("TLS");
        sslContext.init(null, new TrustManager[]{new PinnedTrustManager(hostname, pins)}, null);
        conn.setSSLSocketFactory(sslContext.getSocketFactory());

        conn.connect();

        int responseCode = conn.getResponseCode();
        if (responseCode != HttpsURLConnection.HTTP_OK) {
            conn.disconnect();
            throw new Exception("HTTP error: " + responseCode);
        }

        BufferedReader reader = new BufferedReader(new InputStreamReader(conn.getInputStream()));
        StringBuilder sb = new StringBuilder();
        String line;
        while ((line = reader.readLine()) != null) sb.append(line);
        reader.close();
        conn.disconnect();

        return sb.toString();
    }

    // ── Custom X509TrustManager dengan pin validation ────────────────────────

    private static class PinnedTrustManager implements X509TrustManager {

        private final String hostname;
        private final Set<String> pins;
        private final X509TrustManager systemTrustManager;
        private final android.net.http.X509TrustManagerExtensions trustManagerExtensions;

        PinnedTrustManager(String hostname, Set<String> pins) throws Exception {
            this.hostname = hostname;
            this.pins = pins;
            // Inisialisasi system trust manager untuk validasi chain standar
            TrustManagerFactory tmf = TrustManagerFactory.getInstance(
                    TrustManagerFactory.getDefaultAlgorithm());
            tmf.init((KeyStore) null);
            X509TrustManager found = null;
            for (TrustManager tm : tmf.getTrustManagers()) {
                if (tm instanceof X509TrustManager) {
                    found = (X509TrustManager) tm;
                    break;
                }
            }
            if (found == null) throw new IllegalStateException("No X509TrustManager found");
            this.systemTrustManager = found;
            this.trustManagerExtensions = new android.net.http.X509TrustManagerExtensions(found);
        }

        @Override
        public void checkServerTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
            // Langkah 1: Validasi chain dengan system trust store (hostname-aware)
            java.util.List<X509Certificate> cleanChain = trustManagerExtensions.checkServerTrusted(chain, authType, hostname);

            // Langkah 2: Validasi SPKI SHA-256 pin
            // Cukup satu sertifikat dalam chain yang cocok
            for (X509Certificate cert : cleanChain) {
                try {
                    // Hitung SHA-256 dari SubjectPublicKeyInfo (SPKI)
                    byte[] spki = cert.getPublicKey().getEncoded();
                    MessageDigest md = MessageDigest.getInstance("SHA-256");
                    byte[] hash = md.digest(spki);
                    String pin = Base64.encodeToString(hash, Base64.NO_WRAP);

                    if (pins.contains(pin)) {
                        Log.d(TAG, "SSL pin match: " + pin);
                        return; // Pin cocok → koneksi aman
                    }
                } catch (Exception e) {
                    Log.w(TAG, "Error computing pin: " + e.getMessage());
                }
            }

            // Tidak ada pin yang cocok → tolak koneksi
            Log.e(TAG, "SSL Pinning FAILED — tidak ada pin yang cocok!");
            throw new CertificateException(
                    "SSL Pinning gagal: Sertifikat server tidak dikenali. " +
                    "Kemungkinan serangan Man-in-the-Middle terdeteksi.");
        }

        @Override
        public void checkClientTrusted(X509Certificate[] chain, String authType)
                throws CertificateException {
            systemTrustManager.checkClientTrusted(chain, authType);
        }

        @Override
        public X509Certificate[] getAcceptedIssuers() {
            return systemTrustManager.getAcceptedIssuers();
        }
    }
}
