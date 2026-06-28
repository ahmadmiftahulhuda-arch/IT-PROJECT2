package com.example.it_project2;

import java.io.File;
import java.io.BufferedReader;
import java.io.InputStreamReader;

/**
 * RootDetector — Deteksi Perangkat Rooted (OWASP Mobile M8 / MASVS Resilience)
 * ─────────────────────────────────────────────────────────────────────────────
 * Memeriksa apakah perangkat Android telah di-root dengan mendeteksi file
 * biner su, build tags 'test-keys', eksekusi runtime 'su', dan biner BusyBox.
 */
public class RootDetector {

    /**
     * Memeriksa apakah perangkat telah di-root.
     * @return true jika perangkat terdeteksi root, false sebaliknya.
     */
    public static boolean isDeviceRooted() {
        return checkBuildTags() || checkSuBinary() || checkSuExecution() || checkBusyBox();
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

    private static boolean checkSuExecution() {
        Process process = null;
        try {
            process = Runtime.getRuntime().exec(new String[] { "which", "su" });
            BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()));
            return in.readLine() != null;
        } catch (Throwable t) {
            return false;
        } finally {
            if (process != null) {
                process.destroy();
            }
        }
    }

    private static boolean checkBusyBox() {
        String[] paths = {
            "/system/xbin/busybox",
            "/system/bin/busybox",
            "/sbin/busybox"
        };
        for (String path : paths) {
            if (new File(path).exists()) {
                return true;
            }
        }
        return false;
    }
}
