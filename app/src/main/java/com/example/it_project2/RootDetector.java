package com.example.it_project2;

import java.io.File;

/**
 * RootDetector — Deteksi Perangkat Rooted (OWASP Mobile M8 / MASVS Resilience)
 * ─────────────────────────────────────────────────────────────────────────────
 * Memeriksa apakah perangkat Android telah di-root dengan mendeteksi file
 * biner su dan build tags 'test-keys'.
 */
public class RootDetector {

    /**
     * Memeriksa apakah perangkat telah di-root.
     * @return true jika perangkat terdeteksi root, false sebaliknya.
     */
    public static boolean isDeviceRooted() {
        return checkBuildTags() || checkSuBinary();
    }

    private static boolean checkBuildTags() {
        String buildTags = android.os.Build.TAGS;
        return buildTags != null && buildTags.contains("test-keys");
    }

    private static boolean checkSuBinary() {
        String[] paths = {
            "/system/app/Superuser.apk",
            "/sbin/su",
            "/system/bin/su",
            "/system/xbin/su",
            "/data/local/xbin/su",
            "/data/local/bin/su",
            "/system/sd/xbin/su",
            "/system/bin/failsafe/su",
            "/data/local/su"
        };
        for (String path : paths) {
            if (new File(path).exists()) {
                return true;
            }
        }
        return false;
    }
}
