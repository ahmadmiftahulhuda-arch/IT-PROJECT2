package com.example.it_project2;

import android.animation.AnimatorSet;
import android.animation.ObjectAnimator;
import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Shader;
import android.util.AttributeSet;
import android.view.MotionEvent;
import android.view.View;
import android.view.animation.DecelerateInterpolator;

/**
 * SliderCaptchaView
 * ─────────────────
 * Custom View: slider CAPTCHA "geser untuk membuktikan Anda bukan robot".
 *
 * Cara pakai di XML:
 *   <com.example.it_project2.SliderCaptchaView
 *       android:id="@+id/sliderCaptcha"
 *       android:layout_width="match_parent"
 *       android:layout_height="56dp" />
 *
 * Cara pakai di Java:
 *   sliderCaptcha.setOnVerifiedListener(() -> { ... }); // dipanggil saat berhasil
 *   sliderCaptcha.reset();  // reset ke posisi awal
 *   sliderCaptcha.isVerified();  // cek status
 */
public class SliderCaptchaView extends View {

    // ── Warna ──────────────────────────────────────────────────────────────
    private static final int COLOR_TRACK_BG     = Color.parseColor("#F1F5F9");
    private static final int COLOR_TRACK_FILL   = Color.parseColor("#DBEAFE");
    private static final int COLOR_TRACK_DONE   = Color.parseColor("#D1FAE5");
    private static final int COLOR_THUMB        = Color.parseColor("#2563EB");
    private static final int COLOR_THUMB_DONE   = Color.parseColor("#16A34A");
    private static final int COLOR_TEXT         = Color.parseColor("#94A3B8");
    private static final int COLOR_TEXT_DONE    = Color.parseColor("#16A34A");
    private static final int COLOR_ARROW        = Color.WHITE;

    // ── Dimensi ─────────────────────────────────────────────────────────────
    private float thumbRadius;
    private float trackHeight;
    private float thumbX;
    private float thumbMinX;
    private float thumbMaxX;
    private float touchStartX;

    // ── State ────────────────────────────────────────────────────────────────
    private boolean isDragging   = false;
    private boolean isVerified   = false;
    private boolean isAnimating  = false;

    // ── Paint ────────────────────────────────────────────────────────────────
    private final Paint paintTrackBg   = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintTrackFill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintThumb     = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintText      = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint paintArrow     = new Paint(Paint.ANTI_ALIAS_FLAG);

    // ── Callback ─────────────────────────────────────────────────────────────
    private OnVerifiedListener listener;

    public interface OnVerifiedListener {
        void onVerified();
    }

    // ── Konstruktor ──────────────────────────────────────────────────────────
    public SliderCaptchaView(Context context) {
        super(context);
        init();
    }

    public SliderCaptchaView(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public SliderCaptchaView(Context context, AttributeSet attrs, int defStyleAttr) {
        super(context, attrs, defStyleAttr);
        init();
    }

    private void init() {
        paintText.setTextAlign(Paint.Align.CENTER);
        paintArrow.setStrokeCap(Paint.Cap.ROUND);
        paintArrow.setStrokeJoin(Paint.Join.ROUND);
        paintArrow.setStyle(Paint.Style.STROKE);
        paintArrow.setColor(COLOR_ARROW);
    }

    // ── Layout ────────────────────────────────────────────────────────────────
    @Override
    protected void onSizeChanged(int w, int h, int oldW, int oldH) {
        super.onSizeChanged(w, h, oldW, oldH);
        trackHeight  = h * 0.85f;
        thumbRadius  = (h / 2f) - 2;
        thumbMinX    = thumbRadius + 4;
        thumbMaxX    = w - thumbRadius - 4;
        thumbX       = thumbMinX;
    }

    // ── Draw ──────────────────────────────────────────────────────────────────
    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);

        int w = getWidth();
        int h = getHeight();
        float cx = w / 2f;
        float cy = h / 2f;
        float r  = trackHeight / 2f;

        // 1. Track background
        paintTrackBg.setColor(isVerified ? COLOR_TRACK_DONE : COLOR_TRACK_BG);
        RectF trackRect = new RectF(0, cy - r, w, cy + r);
        canvas.drawRoundRect(trackRect, r, r, paintTrackBg);

        // 2. Track fill (progress)
        paintTrackFill.setColor(isVerified ? COLOR_TRACK_DONE : COLOR_TRACK_FILL);
        RectF fillRect = new RectF(0, cy - r, thumbX, cy + r);
        canvas.drawRoundRect(fillRect, r, r, paintTrackFill);

        // 3. Label teks di tengah track
        paintText.setTextSize(13f * getResources().getDisplayMetrics().scaledDensity);
        paintText.setColor(isVerified ? COLOR_TEXT_DONE : COLOR_TEXT);
        String label = isVerified ? "✓  Verifikasi berhasil!" : "Geser ke kanan untuk verifikasi";
        canvas.drawText(label, cx, cy + paintText.getTextSize() / 3f, paintText);

        // 4. Thumb (tombol geser)
        paintThumb.setColor(isVerified ? COLOR_THUMB_DONE : COLOR_THUMB);

        // Shadow thumb
        paintThumb.setShadowLayer(8f, 0, 3f, Color.parseColor("#40000000"));
        setLayerType(LAYER_TYPE_SOFTWARE, null);

        canvas.drawCircle(thumbX, cy, thumbRadius, paintThumb);

        // 5. Ikon panah (»») di dalam thumb
        paintArrow.setStrokeWidth(2.2f * getResources().getDisplayMetrics().density);
        float ax = thumbX;
        float arrowSize = thumbRadius * 0.38f;
        if (!isVerified) {
            // Panah pertama
            drawArrow(canvas, ax - arrowSize * 0.6f, cy, arrowSize);
            // Panah kedua (sedikit ke kanan)
            drawArrow(canvas, ax + arrowSize * 0.4f, cy, arrowSize);
        } else {
            // Centang saat verified
            paintArrow.setColor(COLOR_ARROW);
            paintArrow.setStrokeWidth(3f * getResources().getDisplayMetrics().density);
            float ck = thumbRadius * 0.45f;
            // Garis ceklis
            canvas.drawLine(ax - ck * 0.6f, cy, ax - ck * 0.1f, cy + ck * 0.5f, paintArrow);
            canvas.drawLine(ax - ck * 0.1f, cy + ck * 0.5f, ax + ck * 0.7f, cy - ck * 0.5f, paintArrow);
        }
    }

    /** Gambar ikon panah (chevron >) di posisi (x, y) */
    private void drawArrow(Canvas canvas, float x, float cy, float size) {
        paintArrow.setColor(COLOR_ARROW);
        canvas.drawLine(x - size * 0.5f, cy - size, x + size * 0.5f, cy, paintArrow);
        canvas.drawLine(x + size * 0.5f, cy, x - size * 0.5f, cy + size, paintArrow);
    }

    // ── Touch ─────────────────────────────────────────────────────────────────
    @Override
    public boolean onTouchEvent(MotionEvent event) {
        if (isVerified || isAnimating) return true;

        float x = event.getX();
        float cy = getHeight() / 2f;

        switch (event.getAction()) {
            case MotionEvent.ACTION_DOWN:
                // Cek apakah user menyentuh thumb
                if (Math.abs(x - thumbX) <= thumbRadius * 1.5f) {
                    isDragging   = true;
                    touchStartX  = x - thumbX;
                    getParent().requestDisallowInterceptTouchEvent(true);
                }
                break;

            case MotionEvent.ACTION_MOVE:
                if (isDragging) {
                    thumbX = Math.max(thumbMinX, Math.min(thumbMaxX, x - touchStartX));
                    invalidate();

                    // Cek apakah sudah sampai ujung (threshold 95%)
                    float progress = (thumbX - thumbMinX) / (thumbMaxX - thumbMinX);
                    if (progress >= 0.95f) {
                        snapToEnd();
                    }
                }
                break;

            case MotionEvent.ACTION_UP:
            case MotionEvent.ACTION_CANCEL:
                if (isDragging) {
                    isDragging = false;
                    getParent().requestDisallowInterceptTouchEvent(false);

                    float progress = (thumbX - thumbMinX) / (thumbMaxX - thumbMinX);
                    if (progress < 0.95f) {
                        // Belum sampai ujung — snap kembali ke awal
                        snapToStart();
                    }
                }
                break;
        }
        return true;
    }

    // ── Animasi ───────────────────────────────────────────────────────────────

    /** Slider berhasil mencapai ujung → animasi verified */
    private void snapToEnd() {
        isAnimating = true;
        ValueAnimator anim = ValueAnimator.ofFloat(thumbX, thumbMaxX);
        anim.setDuration(180);
        anim.setInterpolator(new DecelerateInterpolator());
        anim.addUpdateListener(a -> {
            thumbX = (float) a.getAnimatedValue();
            invalidate();
        });
        anim.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                isVerified  = true;
                isAnimating = false;
                invalidate();
                if (listener != null) listener.onVerified();
            }
        });
        anim.start();
    }

    /** Slider dilepas sebelum ujung → kembali ke posisi awal */
    private void snapToStart() {
        isAnimating = true;
        ValueAnimator anim = ValueAnimator.ofFloat(thumbX, thumbMinX);
        anim.setDuration(300);
        anim.setInterpolator(new DecelerateInterpolator());
        anim.addUpdateListener(a -> {
            thumbX = (float) a.getAnimatedValue();
            invalidate();
        });
        anim.addListener(new android.animation.AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(android.animation.Animator animation) {
                isAnimating = false;
            }
        });
        anim.start();
    }

    // ── Public API ────────────────────────────────────────────────────────────

    public void setOnVerifiedListener(OnVerifiedListener listener) {
        this.listener = listener;
    }

    public boolean isVerified() {
        return isVerified;
    }

    /** Reset slider ke posisi awal (belum diverifikasi) */
    public void reset() {
        isVerified  = false;
        isAnimating = false;
        isDragging  = false;
        thumbX      = thumbMinX;
        invalidate();
    }
}
