package edu.northeastern.mellow.ui.mood;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.DashPathEffect;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

/**
 * The Mood Overview squiggle — a smooth spline through the period's mood
 * values with dotted gridlines, x labels, and small colour-coded pills
 * anchored at the highest / lowest / latest points.
 */
public class MoodWaveView extends View {

    private float[] values = new float[0];   // 1..5, -1 = no data
    private String[] labels = new String[0];
    private int[] moodColors = new int[5];
    private String[] moodNames = {"Depressed", "Sad", "Neutral", "Happy", "Overjoyed"};

    private final Paint line = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint grid = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint axis = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pill = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint pillText = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint dot = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Path path = new Path();
    private final float d;

    public MoodWaveView(Context c) { this(c, null); }
    public MoodWaveView(Context c, @Nullable AttributeSet a) { this(c, a, 0); }
    public MoodWaveView(Context c, @Nullable AttributeSet a, int s) {
        super(c, a, s);
        d = getResources().getDisplayMetrics().density;

        line.setStyle(Paint.Style.STROKE);
        line.setStrokeWidth(3f * d);
        line.setStrokeCap(Paint.Cap.ROUND);
        line.setStrokeJoin(Paint.Join.ROUND);
        line.setColor(0xFF4A3527);

        grid.setStyle(Paint.Style.STROKE);
        grid.setStrokeWidth(1f * d);
        grid.setColor(0x33A99C8F);
        grid.setPathEffect(new DashPathEffect(new float[]{4 * d, 5 * d}, 0));

        axis.setColor(0xFF8B8177);
        axis.setTextAlign(Paint.Align.CENTER);
        axis.setTextSize(10.5f * d);

        pillText.setColor(0xFFFFFFFF);
        pillText.setTextAlign(Paint.Align.CENTER);
        pillText.setTextSize(9.5f * d);
        pillText.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        dot.setColor(0xFFFFFFFF);
    }

    public void setData(float[] values, String[] labels, int[] moodColors) {
        this.values = values != null ? values : new float[0];
        this.labels = labels != null ? labels : new String[0];
        if (moodColors != null) this.moodColors = moodColors;
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth(), h = getHeight();
        int n = values.length;
        if (w == 0 || h == 0 || n == 0) return;

        float labelH = 20 * d;
        float top = 26 * d;                 // headroom for pills
        float bottom = h - labelH;
        float left = 14 * d, right = w - 14 * d;

        // dotted gridlines at each mood level
        for (int i = 0; i < 5; i++) {
            float y = bottom - (i / 4f) * (bottom - top);
            path.reset();
            path.moveTo(left, y);
            path.lineTo(right, y);
            canvas.drawPath(path, grid);
        }

        float stepX = (right - left) / Math.max(1, n - 1);
        float[] xs = new float[n], ys = new float[n];
        boolean[] has = new boolean[n];
        for (int i = 0; i < n; i++) {
            xs[i] = left + stepX * i;
            has[i] = values[i] >= 1f;
            float v = has[i] ? values[i] : 3f;
            ys[i] = bottom - ((v - 1f) / 4f) * (bottom - top);
        }

        // smooth spline through the points
        path.reset();
        path.moveTo(xs[0], ys[0]);
        for (int i = 0; i < n - 1; i++) {
            float cx = (xs[i] + xs[i + 1]) / 2f;
            path.cubicTo(cx, ys[i], cx, ys[i + 1], xs[i + 1], ys[i + 1]);
        }
        canvas.drawPath(path, line);

        // x labels
        for (int i = 0; i < n && i < labels.length; i++) {
            if (labels[i] != null && !labels[i].isEmpty()) {
                canvas.drawText(labels[i], xs[i], h - 4 * d, axis);
            }
        }

        // pills at the highest and lowest logged points
        int hi = -1, lo = -1;
        for (int i = 0; i < n; i++) {
            if (!has[i]) continue;
            if (hi < 0 || values[i] > values[hi]) hi = i;
            if (lo < 0 || values[i] < values[lo]) lo = i;
        }
        if (hi >= 0) drawPill(canvas, xs[hi], ys[hi], values[hi], w);
        if (lo >= 0 && lo != hi) drawPill(canvas, xs[lo], ys[lo], values[lo], w);
    }

    private void drawPill(Canvas canvas, float x, float y, float value, int w) {
        int idx = Math.max(0, Math.min(4, Math.round(value) - 1));
        String text = moodNames[idx];
        int color = moodColors[idx] != 0 ? moodColors[idx] : 0xFF4A3527;

        float padH = 8 * d, padV = 4.5f * d;
        float tw = pillText.measureText(text);
        float pw = tw + padH * 2, ph = pillText.getTextSize() + padV * 2;
        float px = Math.max(2 * d, Math.min(w - pw - 2 * d, x - pw / 2f));
        float py = y - ph - 9 * d;
        if (py < 0) py = y + 9 * d;

        pill.setColor(color);
        canvas.drawRoundRect(new RectF(px, py, px + pw, py + ph), ph / 2f, ph / 2f, pill);
        float baseline = py + ph / 2f - (pillText.descent() + pillText.ascent()) / 2f;
        canvas.drawText(text, px + pw / 2f, baseline, pillText);

        // marker on the line
        canvas.drawCircle(x, y, 5f * d, dot);
        pill.setColor(color);
        canvas.drawCircle(x, y, 3.2f * d, pill);
    }
}
