package edu.northeastern.mellow.ui.mood;

import android.content.Context;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Typeface;
import android.util.AttributeSet;
import android.view.View;

import androidx.annotation.Nullable;

/**
 * Packed bubble chart of mood counts — one circle per mood, area proportional
 * to how often it was logged. The largest sits in the middle and the rest
 * cluster around it, so the dominant mood reads instantly.
 */
public class MoodBubbleView extends View {

    /** counts[0..4] = Depressed, Sad, Neutral, Happy, Overjoyed */
    private int[] counts = new int[5];
    private int[] colors = new int[5];

    private final Paint bubble = new Paint(Paint.ANTI_ALIAS_FLAG);
    private final Paint label = new Paint(Paint.ANTI_ALIAS_FLAG);

    public MoodBubbleView(Context c) { this(c, null); }
    public MoodBubbleView(Context c, @Nullable AttributeSet a) { this(c, a, 0); }
    public MoodBubbleView(Context c, @Nullable AttributeSet a, int d) {
        super(c, a, d);
        label.setColor(0xFFFFFFFF);
        label.setTextAlign(Paint.Align.CENTER);
        label.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));
    }

    public void setData(int[] counts, int[] colors) {
        this.counts = counts != null ? counts : new int[5];
        this.colors = colors != null ? colors : new int[5];
        invalidate();
    }

    @Override
    protected void onDraw(Canvas canvas) {
        super.onDraw(canvas);
        int w = getWidth(), h = getHeight();
        if (w == 0 || h == 0) return;

        int total = 0;
        for (int c : counts) total += c;
        if (total == 0) return;

        // biggest first
        Integer[] idx = {0, 1, 2, 3, 4};
        java.util.Arrays.sort(idx, (a, b) -> counts[b] - counts[a]);

        int n = 0;
        for (int c : counts) if (c > 0) n++;

        float d = getResources().getDisplayMetrics().density;
        float gap = 3f * d;
        float maxCount = Math.max(1, counts[idx[0]]);

        // radius ∝ sqrt(count) so AREA reflects how often the mood was logged
        float base = Math.min(w, h) * 0.27f;
        float[] r = new float[5];
        float[] cxs = new float[5], cys = new float[5];
        for (int i = 0; i < 5; i++) {
            r[i] = counts[i] <= 0 ? 0f
                    : Math.max(16f * d, base * (float) Math.sqrt(counts[i] / maxCount));
        }

        // hub-and-ring layout: largest in the middle, the rest just touching it
        int hub = idx[0];
        cxs[hub] = 0f; cys[hub] = 0f;
        double[] angles = {-1.05, 0.55, 2.15, 3.75};   // evenly spread, tuned start
        int k = 0;
        for (int j = 1; j < idx.length; j++) {
            int i = idx[j];
            if (counts[i] <= 0) continue;
            double ang = angles[Math.min(k, angles.length - 1)];
            k++;
            float dist = r[hub] + r[i] + gap;          // touch, never overlap
            cxs[i] = (float) Math.cos(ang) * dist;
            cys[i] = (float) Math.sin(ang) * dist;
        }

        // fit the whole cluster inside the view
        float minX = 0, maxX = 0, minY = 0, maxY = 0;
        for (int i = 0; i < 5; i++) {
            if (counts[i] <= 0) continue;
            minX = Math.min(minX, cxs[i] - r[i]);
            maxX = Math.max(maxX, cxs[i] + r[i]);
            minY = Math.min(minY, cys[i] - r[i]);
            maxY = Math.max(maxY, cys[i] + r[i]);
        }
        float clusterW = Math.max(1f, maxX - minX);
        float clusterH = Math.max(1f, maxY - minY);
        float scale = Math.min((w - 4 * d) / clusterW, (h - 4 * d) / clusterH);
        if (scale > 1f) scale = 1f;

        float offX = w / 2f - ((minX + maxX) / 2f) * scale;
        float offY = h / 2f - ((minY + maxY) / 2f) * scale;

        for (int j = 0; j < idx.length; j++) {
            int i = idx[j];
            if (counts[i] <= 0) continue;
            float rr = r[i] * scale;
            float x = cxs[i] * scale + offX;
            float y = cys[i] * scale + offY;

            bubble.setColor(colors[i]);
            canvas.drawCircle(x, y, rr, bubble);

            label.setTextSize(Math.max(11f, rr * 0.68f));
            float baseline = y - (label.descent() + label.ascent()) / 2f;
            canvas.drawText(String.valueOf(counts[i]), x, baseline, label);
        }
    }
}
