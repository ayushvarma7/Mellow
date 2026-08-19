package edu.northeastern.mellow.ui.mood;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

/**
 * Tiny dependency-free bar chart for the mood trend. Values are 0..5
 * (-1 = no data → faint track only). Bars are colour-coded by the mood
 * palette so the chart reads at a glance.
 */
public class MoodChartView extends View {

    private float[] values = new float[0];
    private String[] labels = new String[0];

    private final Paint barPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint trackPaint = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint labelPaint = new Paint(Paint.ANTI_ALIAS_FLAG);

    private static final int[] MOOD_COLORS = {
            0xFFF1885B, // 1 sad  - coral
            0xFFF4A63C, // 2 low  - honey
            0xFFB9AFA4, // 3 okay - grey
            0xFF5EA6D9, // 4 good - sky
            0xFF6FB08A  // 5 great- sage
    };

    public MoodChartView(Context c) { super(c); init(); }
    public MoodChartView(Context c, @Nullable AttributeSet a) { super(c, a); init(); }
    public MoodChartView(Context c, @Nullable AttributeSet a, int d) { super(c, a, d); init(); }

    private void init() {
        trackPaint.setColor(0x33B9AFA4);
        labelPaint.setColor(0xFF8B8177);
        labelPaint.setTextAlign(Paint.Align.CENTER);
        labelPaint.setTypeface(Typeface.DEFAULT);
        labelPaint.setTextSize(sp(10));
    }

    public void setData(float[] values, String[] labels) {
        this.values = values != null ? values : new float[0];
        this.labels = labels != null ? labels : new String[0];
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int n = values.length;
        if (n == 0) return;

        float density = getResources().getDisplayMetrics().density;
        float labelH = 18 * density;
        float top = 6 * density;
        float bottom = getHeight() - labelH;
        float left = 2 * density;
        float right = getWidth() - 2 * density;
        float slot = (right - left) / n;
        float barW = Math.min(slot * 0.62f, 20 * density);
        float radius = barW / 2f;
        float maxVal = 5f;

        for (int i = 0; i < n; i++) {
            float cx = left + slot * i + slot / 2f;
            float bx0 = cx - barW / 2f;
            float bx1 = cx + barW / 2f;

            // faint full-height track
            canvas.drawRoundRect(bx0, top, bx1, bottom, radius, radius, trackPaint);

            float v = values[i];
            if (v >= 0f) {
                float frac = Math.max(0f, Math.min(1f, v / maxVal));
                float minVisible = barW; // keep tiny values visible
                float by0 = bottom - Math.max(minVisible, frac * (bottom - top));
                barPaint.setColor(colorFor(v));
                canvas.drawRoundRect(bx0, by0, bx1, bottom, radius, radius, barPaint);
            }

            if (i < labels.length && labels[i] != null && !labels[i].isEmpty()) {
                canvas.drawText(labels[i], cx, getHeight() - 4 * density, labelPaint);
            }
        }
    }

    private int colorFor(float v) {
        int idx = Math.round(v) - 1;
        if (idx < 0) idx = 0;
        if (idx > 4) idx = 4;
        return MOOD_COLORS[idx];
    }

    private float sp(float sp) {
        return sp * getResources().getDisplayMetrics().scaledDensity;
    }
}
