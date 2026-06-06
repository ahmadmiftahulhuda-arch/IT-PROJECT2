package com.example.it_project2;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.util.TypedValue;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

/**
 * PuzzleCaptchaView v2
 * ────────────────────
 * Jigsaw puzzle CAPTCHA interaktif:
 *  - Background bergradient dengan lubang puzzle acak
 *  - Kepingan yang digeser horizontal untuk mengisi lubang
 *  - Toleransi ketat (harus tepat di posisi lubang)
 *  - Animasi gagal: kepingan bergetar merah → puzzle di-reset dengan posisi baru
 *  - Refresh otomatis setelah gagal
 *
 * Listener:
 *   view.setCaptchaListener(new PuzzleCaptchaView.CaptchaListener() {
 *       public void onVerified() { ... }
 *       public void onFailed(String message) { ... }
 *   });
 */
public class PuzzleCaptchaView extends View {

    // ── Listener gabungan ─────────────────────────────────────────────────────
    public interface CaptchaListener {
        void onVerified();
        void onFailed(String message);
    }

    // ── Backward-compat alias ─────────────────────────────────────────────────
    public interface OnVerifiedListener {
        void onVerified();
    }

    // ── Dimensi kepingan ──────────────────────────────────────────────────────
    private float pieceW, pieceH, pegR;

    // ── Zona tampilan ─────────────────────────────────────────────────────────
    private RectF puzzleRect;  // zona atas: gambar + lubang
    private RectF dragRect;    // zona bawah: track geser

    // ── Posisi lubang (target) ────────────────────────────────────────────────
    private float holeX, holeY;

    // ── Posisi kepingan yang digeser ──────────────────────────────────────────
    private float pieceX, pieceMinX, pieceMaxX;

    // ── State ─────────────────────────────────────────────────────────────────
    private boolean verified  = false;
    private boolean dragging  = false;
    private boolean animating = false;
    private boolean failState = false;   // sedang menampilkan animasi gagal
    private float   shakeOffset = 0f;   // offset horizontal saat animasi getar
    private float   touchOffsetX;

    // ── Pesan error yang tampil di drag area ──────────────────────────────────
    private String errorMessage = "";

    // ── Background bitmap ─────────────────────────────────────────────────────
    private Bitmap bgBitmap;

    // ── Paint ─────────────────────────────────────────────────────────────────
    private final Paint paintHoleFill    = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintHoleBorder  = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintPieceFill   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintPieceBorder = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintDragBg      = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintTrackFill   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintTrackBg     = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintLabel       = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintCheck       = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintDivider     = new Paint();

    // ── Callback ──────────────────────────────────────────────────────────────
    private CaptchaListener captchaListener;

    // ── Konstruktor ───────────────────────────────────────────────────────────
    public PuzzleCaptchaView(Context ctx) { super(ctx); init(); }
    public PuzzleCaptchaView(Context ctx, AttributeSet a) { super(ctx, a); init(); }
    public PuzzleCaptchaView(Context ctx, AttributeSet a, int d) { super(ctx, a, d); init(); }

    private void init() {
        pieceW = dp(50);
        pieceH = dp(50);
        pegR   = dp(10);

        paintHoleFill.setColor(Color.BLACK);
        paintHoleFill.setAlpha(115);

        paintHoleBorder.setStyle(Paint.Style.STROKE);
        paintHoleBorder.setColor(Color.WHITE);
        paintHoleBorder.setStrokeWidth(dp(1.8f));
        paintHoleBorder.setAlpha(150);

        paintPieceBorder.setStyle(Paint.Style.STROKE);
        paintPieceBorder.setColor(Color.WHITE);
        paintPieceBorder.setStrokeWidth(dp(1.8f));

        paintTrackBg.setColor(Color.parseColor("#E2E8F0"));

        paintLabel.setTextAlign(Paint.Align.CENTER);
        paintLabel.setAntiAlias(true);

        paintCheck.setColor(Color.WHITE);
        paintCheck.setStrokeWidth(dp(2.5f));
        paintCheck.setStyle(Paint.Style.STROKE);
        paintCheck.setStrokeCap(Paint.Cap.ROUND);
        paintCheck.setStrokeJoin(Paint.Join.ROUND);

        paintDivider.setColor(Color.parseColor("#E2E8F0"));

        setLayerType(LAYER_TYPE_SOFTWARE, null);
    }

    // ── Saat ukuran berubah ───────────────────────────────────────────────────
    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);

        float puzzleH = h * 0.62f;
        puzzleRect = new RectF(0, 0, w, puzzleH);
        dragRect   = new RectF(0, puzzleH, w, h);

        pieceMinX = dp(8);
        // Kepingan bisa digeser sampai nyentuh ujung kanan view
        pieceMaxX = w - pieceW - pegR * 2 - dp(4);
        pieceX    = pieceMinX;

        randomizeHole();
        generateBackground(w, (int) puzzleH);
    }

    /** Acak posisi lubang — antara 30%–80% dari panjang track drag */
    private void randomizeHole() {
        if (puzzleRect == null) return;
        float dragRange = pieceMaxX - pieceMinX;
        // Hole di antara 30% - 80% agar ada ruang drag sebelum dan sesudah
        holeX = pieceMinX + dragRange * 0.30f + (float)(Math.random() * dragRange * 0.50f);
        holeY = (puzzleRect.height() - pieceH) / 2f - dp(4);
    }

    /** Generate latar belakang bergradient dengan elemen dekoratif */
    private void generateBackground(int w, int h) {
        bgBitmap = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas c = new Canvas(bgBitmap);

        // Pilih skema warna secara acak
        int[][] schemes = {
                {Color.parseColor("#1D4ED8"), Color.parseColor("#0284C7"), Color.parseColor("#0F766E")},
                {Color.parseColor("#7C3AED"), Color.parseColor("#DB2777"), Color.parseColor("#EA580C")},
                {Color.parseColor("#0F172A"), Color.parseColor("#1E3A5F"), Color.parseColor("#0369A1")},
                {Color.parseColor("#14532D"), Color.parseColor("#15803D"), Color.parseColor("#0891B2")},
        };
        int[] scheme = schemes[(int)(Math.random() * schemes.length)];

        // Gradient dasar
        LinearGradient baseGrad = new LinearGradient(0, 0, w, h, scheme, null, Shader.TileMode.CLAMP);
        Paint p = new Paint();
        p.setShader(baseGrad);
        c.drawRect(0, 0, w, h, p);

        // Grid garis
        Paint gridPaint = new Paint();
        gridPaint.setColor(Color.WHITE);
        gridPaint.setAlpha(18);
        float step = dp(22);
        for (float x = 0; x < w; x += step) c.drawLine(x, 0, x, h, gridPaint);
        for (float y = 0; y < h; y += step) c.drawLine(0, y, w, y, gridPaint);

        // Lingkaran dekoratif semi-transparan
        Paint cirPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        cirPaint.setColor(Color.WHITE);
        float[][] circles = {
                {w * 0.1f, h * 0.2f, dp(38)},
                {w * 0.35f, h * 0.75f, dp(26)},
                {w * 0.62f, h * 0.3f, dp(33)},
                {w * 0.82f, h * 0.65f, dp(20)},
                {w * 0.5f,  h * 0.5f, dp(48)},
        };
        int[] alphas = {8, 11, 7, 10, 5};
        for (int i = 0; i < circles.length; i++) {
            cirPaint.setAlpha(alphas[i]);
            c.drawCircle(circles[i][0], circles[i][1], circles[i][2], cirPaint);
        }

        // Garis gelombang
        Paint wavePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
        wavePaint.setColor(Color.WHITE);
        wavePaint.setAlpha(14);
        wavePaint.setStyle(Paint.Style.STROKE);
        wavePaint.setStrokeWidth(dp(1));
        for (int row = 0; row <= h / (int) dp(26) + 1; row++) {
            Path wave = new Path();
            float yw = row * dp(26);
            wave.moveTo(0, yw);
            for (int xi = 0; xi < w + dp(30); xi += (int) dp(30)) {
                wave.quadTo(xi + dp(8), yw - dp(6), xi + dp(15), yw);
                wave.quadTo(xi + dp(22), yw + dp(6), xi + dp(30), yw);
            }
            c.drawPath(wave, wavePaint);
        }
    }

    /** Buat Path kepingan jigsaw: badan persegi + peg setengah lingkaran di kanan */
    private Path createPiecePath(float left, float top) {
        Path path = new Path();
        float right  = left + pieceW;
        float bottom = top + pieceH;
        float midY   = top + pieceH / 2f;

        path.moveTo(left, top);
        path.lineTo(right, top);
        path.lineTo(right, midY - pegR);
        path.arcTo(new RectF(right - pegR, midY - pegR, right + pegR, midY + pegR), 270f, 180f);
        path.lineTo(right, bottom);
        path.lineTo(left, bottom);
        path.lineTo(left, top);
        path.close();
        return path;
    }

    // ── Gambar ────────────────────────────────────────────────────────────────
    @Override
    protected void onDraw(Canvas canvas) {
        if (puzzleRect == null) return;

        int w = getWidth();

        // ═══════════════════════════════════════
        //  ZONA 1: Gambar puzzle + lubang
        // ═══════════════════════════════════════
        canvas.save();
        canvas.clipRect(puzzleRect);

        if (bgBitmap != null) canvas.drawBitmap(bgBitmap, 0, 0, null);

        // Lubang (area gelap sesuai bentuk kepingan)
        Path holePath = createPiecePath(holeX, holeY);
        canvas.save();
        canvas.clipPath(holePath);
        if (verified) {
            // Terisi → hijau
            Paint ok = new Paint(Paint.ANTI_ALIAS_FLAG);
            ok.setColor(Color.parseColor("#16A34A"));
            ok.setAlpha(170);
            canvas.drawRect(holeX - pegR, holeY, holeX + pieceW + pegR, holeY + pieceH, ok);
        } else {
            // Kosong → gelap
            canvas.drawRect(holeX - pegR, holeY, holeX + pieceW + pegR, holeY + pieceH, paintHoleFill);
        }
        canvas.restore();

        // Border lubang
        paintHoleBorder.setAlpha(verified ? 220 : 140);
        canvas.drawPath(holePath, paintHoleBorder);

        // Highlight lubang saat kepingan dekat (glow kuning)
        float distToHole = Math.abs(pieceX - holeX);
        if (!verified && !failState && distToHole < dp(30)) {
            Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
            float alpha = (float)(1.0 - distToHole / dp(30));
            glow.setColor(Color.parseColor("#FBBF24"));
            glow.setAlpha((int)(100 * alpha));
            glow.setStyle(Paint.Style.STROKE);
            glow.setStrokeWidth(dp(3f));
            canvas.drawPath(holePath, glow);
        }

        canvas.restore();
        canvas.drawLine(0, puzzleRect.bottom, w, puzzleRect.bottom, paintDivider);

        // ═══════════════════════════════════════
        //  ZONA 2: Track geser kepingan
        // ═══════════════════════════════════════
        // Background drag area
        paintDragBg.setColor(failState   ? Color.parseColor("#FFF5F5")
                           : verified    ? Color.parseColor("#F0FDF4")
                                         : Color.parseColor("#F8FAFC"));
        canvas.drawRect(dragRect, paintDragBg);

        // Track (progress bar)
        float trackY     = dragRect.centerY() - dp(10);
        float trackLeft  = pieceMinX + pieceW / 2f;
        float trackRight = pieceMaxX + pieceW / 2f + pegR;
        float trackH     = dp(5);
        RectF trackFull  = new RectF(trackLeft, trackY - trackH / 2f, trackRight, trackY + trackH / 2f);

        paintTrackBg.setColor(failState ? Color.parseColor("#FED7D7")
                            : verified  ? Color.parseColor("#BBF7D0")
                                        : Color.parseColor("#E2E8F0"));
        canvas.drawRoundRect(trackFull, dp(3), dp(3), paintTrackBg);

        // Bagian track yang sudah dilewati
        float progressRight = Math.min(pieceX + pieceW / 2f + shakeOffset, trackRight);
        if (progressRight > trackLeft) {
            paintTrackFill.setColor(failState ? Color.parseColor("#FC8181")
                                  : verified  ? Color.parseColor("#4ADE80")
                                              : Color.parseColor("#93C5FD"));
            RectF trackDone = new RectF(trackLeft, trackY - trackH / 2f, progressRight, trackY + trackH / 2f);
            canvas.drawRoundRect(trackDone, dp(3), dp(3), paintTrackFill);
        }

        // ── Kepingan puzzle yang digeser ──
        float pieceDrawY  = dragRect.top + (dragRect.height() - pieceH) / 2f - dp(6);
        float pieceDrawX  = pieceX + shakeOffset;   // offset getar saat gagal
        Path  piecePath   = createPiecePath(pieceDrawX, pieceDrawY);

        // Warna kepingan berdasarkan state
        if (verified) {
            paintPieceFill.setShader(null);
            paintPieceFill.setColor(Color.parseColor("#16A34A"));
            paintPieceFill.setShadowLayer(10, 0, 4, Color.parseColor("#5016A34A"));
        } else if (failState) {
            paintPieceFill.setShader(null);
            paintPieceFill.setColor(Color.parseColor("#DC2626"));
            paintPieceFill.setShadowLayer(10, 0, 4, Color.parseColor("#60DC2626"));
        } else {
            LinearGradient grad = new LinearGradient(
                    pieceDrawX, pieceDrawY, pieceDrawX + pieceW, pieceDrawY + pieceH,
                    new int[]{Color.parseColor("#2563EB"), Color.parseColor("#0891B2")},
                    null, Shader.TileMode.CLAMP
            );
            paintPieceFill.setShader(grad);
            paintPieceFill.setShadowLayer(10, 0, 4, Color.parseColor("#602563EB"));
        }
        canvas.drawPath(piecePath, paintPieceFill);
        canvas.drawPath(piecePath, paintPieceBorder);

        // Ikon di dalam kepingan
        float pCx = pieceDrawX + pieceW / 2f;
        float pCy = pieceDrawY + pieceH / 2f;
        if (verified) {
            float cs = dp(9);
            canvas.drawLine(pCx - cs * 0.7f, pCy, pCx, pCy + cs * 0.6f, paintCheck);
            canvas.drawLine(pCx, pCy + cs * 0.6f, pCx + cs, pCy - cs * 0.6f, paintCheck);
        } else {
            // Titik-titik tekstur
            Paint dotPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
            dotPaint.setColor(Color.WHITE);
            dotPaint.setAlpha(failState ? 80 : 120);
            float dotStep = dp(9);
            float dotR    = dp(1.8f);
            for (float dx = pieceDrawX + dotStep; dx < pieceDrawX + pieceW - dotStep / 2; dx += dotStep) {
                for (float dy = pieceDrawY + dotStep; dy < pieceDrawY + pieceH - dotStep / 2; dy += dotStep) {
                    canvas.drawCircle(dx, dy, dotR, dotPaint);
                }
            }
        }

        // Label status di bawah track
        paintLabel.setTextSize(dp(10.5f));
        String labelText;
        int labelColor;
        if (verified) {
            labelText  = "Kepingan cocok! Verifikasi berhasil";
            labelColor = Color.parseColor("#16A34A");
        } else if (failState) {
            labelText  = errorMessage.isEmpty() ? "Tidak tepat, coba lagi..." : errorMessage;
            labelColor = Color.parseColor("#DC2626");
        } else {
            labelText  = "Geser kepingan, tepat di lubang, lalu lepaskan";
            labelColor = Color.parseColor("#94A3B8");
        }
        paintLabel.setColor(labelColor);
        canvas.drawText(labelText, w / 2f, dragRect.bottom - dp(4), paintLabel);
    }

    // ── Sentuhan ──────────────────────────────────────────────────────────────
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (verified || animating) return true;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                if (event.getY() > dragRect.top - dp(4)) {
                    dragging     = true;
                    // Clamp offset agar kepingan tidak loncat jauh saat disentuh
                    float rawOffset = event.getX() - pieceX;
                    touchOffsetX = Math.max(0, Math.min(rawOffset, pieceW + pegR));
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (dragging) {
                    pieceX = Math.max(pieceMinX, Math.min(pieceMaxX, event.getX() - touchOffsetX));
                    invalidate();
                    // Tidak ada snap otomatis saat bergerak.
                    // User bebas menggeser kepingan sampai ujung kanan.
                    // Pengecekan dilakukan hanya saat user melepas (ACTION_UP).
                }
                break;

            case MotionEvent.ACTION_UP:
                if (dragging) {
                    dragging = false;
                    getParent().requestDisallowInterceptTouchEvent(false);
                    if (!verified) {
                        // Toleransi ±10dp saat lepas
                        if (Math.abs(pieceX - holeX) <= dp(10)) {
                            snapToHole();
                        } else {
                            snapToStartWithError("Posisi kurang tepat, coba lagi!");
                        }
                    }
                }
                break;

            case MotionEvent.ACTION_CANCEL:
                if (dragging) {
                    dragging = false;
                    getParent().requestDisallowInterceptTouchEvent(false);
                    if (!verified) snapToStartWithError("Posisi kurang tepat, coba lagi!");
                }
                break;
        }
        return true;
    }

    // ── Animasi: Berhasil ─────────────────────────────────────────────────────

    private void snapToHole() {
        dragging  = false;
        animating = true;
        ValueAnimator anim = ValueAnimator.ofFloat(pieceX, holeX);
        anim.setDuration(180);
        anim.setInterpolator(new DecelerateInterpolator());
        anim.addUpdateListener(a -> { pieceX = (float) a.getAnimatedValue(); invalidate(); });
        anim.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator animation) {
                animating = false;
                verified  = true;
                invalidate();
                if (captchaListener != null) captchaListener.onVerified();
            }
        });
        anim.start();
    }

    // ── Animasi: Gagal ────────────────────────────────────────────────────────

    /**
     * Kepingan dilepas di posisi yang salah:
     * 1. Warna merah + tampilkan pesan error
     * 2. Animasi getar (shake) kiri-kanan
     * 3. Slide kembali ke awal
     * 4. Posisi lubang di-acak ulang (puzzle baru)
     * 5. Panggil onFailed()
     */
    private void snapToStartWithError(String message) {
        animating     = true;
        failState     = true;
        errorMessage  = message;
        invalidate();

        // Animasi getar (sine wave)
        ValueAnimator shake = ValueAnimator.ofFloat(0f, 1f);
        shake.setDuration(600);
        shake.addUpdateListener(a -> {
            float t = (float) a.getAnimatedValue();
            shakeOffset = (float)(Math.sin(t * Math.PI * 5) * dp(7) * (1 - t));
            invalidate();
        });
        shake.addListener(new AnimatorListenerAdapter() {
            @Override public void onAnimationEnd(Animator animation) {
                shakeOffset = 0f;
                // Slide kembali ke awal
                ValueAnimator slide = ValueAnimator.ofFloat(pieceX, pieceMinX);
                slide.setDuration(350);
                slide.setInterpolator(new DecelerateInterpolator());
                slide.addUpdateListener(a -> { pieceX = (float) a.getAnimatedValue(); invalidate(); });
                slide.addListener(new AnimatorListenerAdapter() {
                    @Override public void onAnimationEnd(Animator animation) {
                        // Reset dengan posisi lubang baru
                        failState    = false;
                        errorMessage = "";
                        animating    = false;
                        randomizeHole();
                        generateBackground(getWidth(), (int) puzzleRect.height());
                        invalidate();
                        if (captchaListener != null) captchaListener.onFailed(message);
                    }
                });
                slide.start();
            }
        });
        shake.start();
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public void setCaptchaListener(CaptchaListener listener) {
        this.captchaListener = listener;
    }

    /** Backward-compat: hanya menerima onVerified */
    public void setOnVerifiedListener(OnVerifiedListener listener) {
        this.captchaListener = new CaptchaListener() {
            @Override public void onVerified() { listener.onVerified(); }
            @Override public void onFailed(String msg) { /* no-op */ }
        };
    }

    public boolean isVerified() { return verified; }

    /** Reset manual (juga digunakan tombol Refresh di dialog) */
    public void refresh() {
        verified     = false;
        failState    = false;
        animating    = false;
        dragging     = false;
        shakeOffset  = 0f;
        errorMessage = "";
        pieceX       = pieceMinX;
        randomizeHole();
        if (getWidth() > 0 && puzzleRect != null) {
            generateBackground(getWidth(), (int) puzzleRect.height());
        }
        invalidate();
    }

    public void reset() { refresh(); }

    // ── Helper ────────────────────────────────────────────────────────────────
    private float dp(float v) {
        return TypedValue.applyDimension(TypedValue.COMPLEX_UNIT_DIP, v, getResources().getDisplayMetrics());
    }
}
