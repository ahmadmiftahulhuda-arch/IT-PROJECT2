package com.example.it_project2;

/**
 * CaptchaGuard
 * ─────────────
 * Menyimpan state cooldown setelah CAPTCHA gagal terlalu banyak.
 *
 * Cooldown berlaku GLOBAL — jika user gagal di Login, Register
 * juga ikut diblokir selama cooldown berlangsung (dan sebaliknya).
 *
 * Cara pakai di Activity:
 *
 *   // Sebelum tampilkan dialog CAPTCHA:
 *   if (CaptchaGuard.isBlocked()) {
 *       long sisa = CaptchaGuard.remainingSeconds();
 *       Toast.makeText(this, "Coba lagi dalam " + sisa + " detik", Toast.LENGTH_LONG).show();
 *       return;
 *   }
 *
 *   // Saat 3x gagal:
 *   CaptchaGuard.block();
 *   dialog.dismiss();
 */
public class CaptchaGuard {

    /** Durasi cooldown setelah diblokir (30 detik) */
    private static final long COOLDOWN_MS = 30_000L;

    /** Waktu terakhir diblokir (0 = belum pernah diblokir) */
    private static long lastBlockTime = 0L;

    /** Batas kegagalan sebelum diblokir */
    public static final int MAX_FAILURES = 3;

    /** Durasi timeout dialog CAPTCHA (60 detik) */
    public static final long DIALOG_TIMEOUT_MS = 60_000L;

    // ── Prevent instantiation ──────────────────────────────────────────────
    private CaptchaGuard() {}

    /**
     * Apakah user sedang dalam masa cooldown (diblokir)?
     * @return true jika masih diblokir
     */
    public static boolean isBlocked() {
        if (lastBlockTime == 0L) return false;
        return (System.currentTimeMillis() - lastBlockTime) < COOLDOWN_MS;
    }

    /**
     * Sisa waktu cooldown dalam detik.
     * @return sisa detik, atau 0 jika tidak diblokir
     */
    public static long remainingSeconds() {
        if (!isBlocked()) return 0L;
        return (COOLDOWN_MS - (System.currentTimeMillis() - lastBlockTime)) / 1000L;
    }

    /**
     * Blokir user sekarang (mulai cooldown 30 detik).
     * Dipanggil saat batas kegagalan tercapai.
     */
    public static void block() {
        lastBlockTime = System.currentTimeMillis();
    }

    /**
     * Reset blokir secara manual (opsional, untuk testing).
     */
    public static void reset() {
        lastBlockTime = 0L;
    }
}
