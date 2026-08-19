package edu.northeastern.mellow.ui.checkin;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;
import android.util.AttributeSet;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.LinearInterpolator;

import androidx.annotation.Nullable;

/**
 * A calm "fluid fill" — two soft translucent waves that fill from the bottom
 * to a level (0..1). The horizontal phase drifts continuously so the surface
 * always ripples; call {@link #animateLevel} on inhale/exhale to raise and
 * settle the water with the breath. Pure Canvas, no dependencies.
 */
public class WaveView extends View {

    private static final int COLOR_BACK  = 0x335EA6D9; // soft sky, ~20% alpha
    private static final int COLOR_FRONT = 0x4487CFC4; // soft mint, ~27% alpha

    private float level = 0f;   // 0 = empty, 1 = full
    private float phase = 0f;   // radians

    private final Paint backPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint frontPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final float density;

    private ValueAnimator phaseAnim;
    private ValueAnimator levelAnim;

    public WaveView(Context c) { this(c, null); }
    public WaveView(Context c, @Nullable AttributeSet a) { this(c, a, 0); }
    public WaveView(Context c, @Nullable AttributeSet a, int d) {
        super(c, a, d);
        density = getResources().getDisplayMetrics().density;
        backPaint.setColor(COLOR_BACK);
        frontPaint.setColor(COLOR_FRONT);
    }

    /** Raise/settle the water to [target] (0..1) over [durationMs]. */
    public void animateLevel(float target, long durationMs) {
        target = Math.max(0f, Math.min(1f, target));
        if (levelAnim != null) levelAnim.cancel();
        levelAnim = ValueAnimator.ofFloat(level, target);
        levelAnim.setDuration(durationMs);
        levelAnim.setInterpolator(new AccelerateDecelerateInterpolator());
        levelAnim.addUpdateListener(a -> {
            level = (float) a.getAnimatedValue();
            invalidate();
        });
        levelAnim.start();
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        phaseAnim = ValueAnimator.ofFloat(0f, (float) (2 * Math.PI));
        phaseAnim.setDuration(5200);
        phaseAnim.setRepeatCount(ValueAnimator.INFINITE);
        phaseAnim.setInterpolator(new LinearInterpolator());
        phaseAnim.addUpdateListener(a -> {
            phase = (float) a.getAnimatedValue();
            invalidate();
        });
        phaseAnim.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        if (phaseAnim != null) phaseAnim.cancel();
        if (levelAnim != null) levelAnim.cancel();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth();
        int h = getHeight();
        if (w == 0 || h == 0) return;

        // back wave (bigger, slower-looking) then front wave (crisper, offset)
        drawWave(canvas, backPaint, 13f * density, phase, w * 0.85f, 0f);
        drawWave(canvas, frontPaint, 9f * density, phase + 1.6f, w * 1.15f, 6f * density);
    }

    private void drawWave(Canvas canvas, Paint paint, float amp, float phaseShift,
                          float wavelength, float yOffset) {
        int w = getWidth();
        int h = getHeight();
        float base = h * (1f - level) + yOffset;

        path.reset();
        path.moveTo(0, base);
        for (int x = 0; x <= w; x += 10) {
            float y = base + amp * (float) Math.sin((x / wavelength) * 2 * Math.PI + phaseShift);
            path.lineTo(x, y);
        }
        path.lineTo(w, h);
        path.lineTo(0, h);
        path.close();
        canvas.drawPath(path, paint);
    }
}
