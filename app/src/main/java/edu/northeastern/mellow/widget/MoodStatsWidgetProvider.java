package edu.northeastern.mellow.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.widget.RemoteViews;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import dagger.hilt.android.EntryPointAccessors;
import edu.northeastern.mellow.R;
import edu.northeastern.mellow.SplashActivity;
import edu.northeastern.mellow.data.model.MoodEntry;

/**
 * Larger home-screen widget: a positive/negative bar chart of the last two
 * weeks plus the last 7 days as faces. Reads through the app's MoodRepository,
 * and because RemoteViews can't host custom views the chart is drawn to a
 * bitmap and set on an ImageView.
 */
public class MoodStatsWidgetProvider extends AppWidgetProvider {

    private static final int BARS = 14;

    private static final int[] DAY_FACE_IDS = {
            R.id.day1Face, R.id.day2Face, R.id.day3Face, R.id.day4Face,
            R.id.day5Face, R.id.day6Face, R.id.day7Face};
    private static final int[] DAY_LABEL_IDS = {
            R.id.day1Label, R.id.day2Label, R.id.day3Label, R.id.day4Label,
            R.id.day5Label, R.id.day6Label, R.id.day7Label};

    private static final int[] FACE_DRAWABLE = {
            R.drawable.mood_face_depressed, R.drawable.mood_face_sad, R.drawable.mood_face_neutral,
            R.drawable.mood_face_happy, R.drawable.mood_face_overjoyed};

    // mood palette
    private static final int C_OVERJOYED = 0xFF9BB068;
    private static final int C_SAD       = 0xFFED7E1C;
    private static final int C_NEUTRAL   = 0xFFAEA194;
    private static final int C_EMPTY     = 0xFFE8E0D6;

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        refresh(context.getApplicationContext());
    }

    /** Pulls the latest moods and repaints every instance of this widget. */
    static void refresh(Context appCtx) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            pushViews(appCtx, new HashMap<>());
            return;
        }
        MoodWidgetEntryPoint ep = EntryPointAccessors.fromApplication(appCtx, MoodWidgetEntryPoint.class);
        ep.moodRepository().getMoodHistory(user.getUid(), 120, result -> {
            Map<LocalDate, Integer> byDay = new HashMap<>();
            if (result.isSuccess() && result.getData() != null) {
                List<MoodEntry> moods = result.getData();
                for (MoodEntry m : moods) {
                    if (m.getDate() == null || m.getMoodScore() < 1 || m.getMoodScore() > 5) continue;
                    try {
                        LocalDate d = LocalDate.parse(m.getDate());
                        // history is newest-first, so keep the first hit per day
                        if (!byDay.containsKey(d)) byDay.put(d, m.getMoodScore());
                    } catch (Exception ignored) {}
                }
            }
            pushViews(appCtx, byDay);
        });
    }

    private static void pushViews(Context ctx, Map<LocalDate, Integer> byDay) {
        AppWidgetManager manager = AppWidgetManager.getInstance(ctx);
        int[] ids = manager.getAppWidgetIds(new ComponentName(ctx, MoodStatsWidgetProvider.class));
        if (ids == null || ids.length == 0) return;
        RemoteViews views = buildViews(ctx, byDay);
        for (int id : ids) manager.updateAppWidget(id, views);
    }

    private static RemoteViews buildViews(Context ctx, Map<LocalDate, Integer> byDay) {
        RemoteViews views = new RemoteViews(ctx.getPackageName(), R.layout.widget_mood_stats);
        LocalDate today = LocalDate.now();

        views.setImageViewBitmap(R.id.statsChart, drawChart(ctx, byDay, today));

        // last 7 days, oldest → today
        for (int i = 0; i < 7; i++) {
            LocalDate day = today.minusDays(6 - i);
            Integer score = byDay.get(day);
            views.setImageViewResource(DAY_FACE_IDS[i],
                    score == null ? R.drawable.mood_face_neutral : FACE_DRAWABLE[score - 1]);
            views.setInt(DAY_FACE_IDS[i], "setImageAlpha", score == null ? 70 : 255);
            views.setTextViewText(DAY_LABEL_IDS[i],
                    day.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.getDefault()));
        }

        PendingIntent openApp = PendingIntent.getActivity(ctx, 0,
                new Intent(ctx, SplashActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.statsHeader, openApp);
        views.setOnClickPendingIntent(R.id.statsChart, openApp);
        return views;
    }

    /** Rounded track + fill per day, coloured by whether the day was positive or negative. */
    private static Bitmap drawChart(Context ctx, Map<LocalDate, Integer> byDay, LocalDate today) {
        float density = ctx.getResources().getDisplayMetrics().density;
        int w = Math.round(320 * density);
        int h = Math.round(150 * density);

        Bitmap bmp = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        canvas.drawColor(Color.TRANSPARENT);

        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);
        Paint label = new Paint(Paint.ANTI_ALIAS_FLAG);
        label.setColor(0xFF8B8177);
        label.setTextAlign(Paint.Align.CENTER);
        label.setTextSize(11 * density);
        label.setTypeface(Typeface.create(Typeface.DEFAULT, Typeface.BOLD));

        float labelH = 18 * density;
        float top = 4 * density;
        float bottom = h - labelH;
        float slot = w / (float) BARS;
        float barW = Math.min(slot * 0.52f, 16 * density);
        float radius = barW / 2f;

        for (int i = 0; i < BARS; i++) {
            LocalDate day = today.minusDays(BARS - 1 - i);
            Integer score = byDay.get(day);

            float cx = slot * i + slot / 2f;
            float x0 = cx - barW / 2f, x1 = cx + barW / 2f;

            int fill = score == null ? C_EMPTY
                    : score >= 4 ? C_OVERJOYED
                    : score <= 2 ? C_SAD : C_NEUTRAL;

            // faint full-height track tinted to match the day
            paint.setColor(score == null ? 0x22AEA194 : (fill & 0x00FFFFFF) | 0x22000000);
            canvas.drawRoundRect(new RectF(x0, top, x1, bottom), radius, radius, paint);

            // fill grows with the score; empty days keep a small stub
            float frac = score == null ? 0f : score / 5f;
            float minStub = barW;
            float y0 = bottom - Math.max(minStub, frac * (bottom - top));
            paint.setColor(fill);
            canvas.drawRoundRect(new RectF(x0, y0, x1, bottom), radius, radius, paint);

            // weekday initial under every other bar so it stays readable
            if (i % 2 == 0) {
                String d = day.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.getDefault());
                canvas.drawText(d.substring(0, 1), cx, h - 4 * density, label);
            }
        }
        return bmp;
    }
}
