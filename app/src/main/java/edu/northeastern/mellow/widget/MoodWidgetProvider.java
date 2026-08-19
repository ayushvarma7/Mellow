package edu.northeastern.mellow.widget;

import android.app.PendingIntent;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.widget.RemoteViews;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;

import dagger.hilt.android.EntryPointAccessors;
import edu.northeastern.mellow.R;
import edu.northeastern.mellow.SplashActivity;
import edu.northeastern.mellow.data.model.MoodEntry;
import edu.northeastern.mellow.data.util.DateUtils;

/**
 * Home-screen widget: tap a face to log that mood for today. Logging reuses the
 * app's MoodRepository via a Hilt EntryPoint, so the one-mood-per-day rule and
 * coin reward behave exactly like they do inside the app.
 */
public class MoodWidgetProvider extends AppWidgetProvider {

    public static final String ACTION_LOG_MOOD = "edu.northeastern.mellow.ACTION_LOG_MOOD";
    public static final String EXTRA_MOOD = "extra_mood";

    private static final int[] FACE_IDS = {R.id.mood1, R.id.mood2, R.id.mood3, R.id.mood4, R.id.mood5};

    @Override
    public void onUpdate(Context context, AppWidgetManager manager, int[] appWidgetIds) {
        for (int id : appWidgetIds) {
            manager.updateAppWidget(id, buildViews(context, "Tap a face to log today"));
        }
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        if (ACTION_LOG_MOOD.equals(intent.getAction())) {
            handleLog(context, intent.getIntExtra(EXTRA_MOOD, 3));
        }
    }

    private void handleLog(Context context, int score) {
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user == null) {
            // Not signed in — open the app so they can sign in first.
            Intent open = new Intent(context, SplashActivity.class)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(open);
            return;
        }

        final Context appCtx = context.getApplicationContext();
        final BroadcastReceiver.PendingResult pending = goAsync(); // keep the process alive for the async write

        MoodWidgetEntryPoint ep = EntryPointAccessors.fromApplication(appCtx, MoodWidgetEntryPoint.class);
        MoodEntry entry = new MoodEntry(
                "", System.currentTimeMillis(), DateUtils.today(),
                Math.max(1, Math.min(5, score)), null, "WIDGET");

        ep.moodRepository().logMood(user.getUid(), entry, result -> {
            String status;
            if (result.isSuccess()) {
                status = "Logged for today 🌿";
            } else if (result.getMessage() != null && result.getMessage().toLowerCase().contains("already")) {
                status = "Already logged today";
            } else {
                status = "Couldn't log — open the app";
            }
            updateAll(appCtx, status);
            pending.finish();
        });
    }

    private void updateAll(Context context, String status) {
        AppWidgetManager manager = AppWidgetManager.getInstance(context);
        int[] ids = manager.getAppWidgetIds(new ComponentName(context, MoodWidgetProvider.class));
        for (int id : ids) {
            manager.updateAppWidget(id, buildViews(context, status));
        }
    }

    private static RemoteViews buildViews(Context context, String status) {
        RemoteViews views = new RemoteViews(context.getPackageName(), R.layout.widget_mood);
        views.setTextViewText(R.id.widgetStatus, status);

        for (int i = 0; i < FACE_IDS.length; i++) {
            int score = i + 1;
            Intent intent = new Intent(context, MoodWidgetProvider.class)
                    .setAction(ACTION_LOG_MOOD)
                    .putExtra(EXTRA_MOOD, score);
            PendingIntent pi = PendingIntent.getBroadcast(context, score, intent,
                    PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
            views.setOnClickPendingIntent(FACE_IDS[i], pi);
        }

        // Tapping the title opens the app.
        PendingIntent openApp = PendingIntent.getActivity(context, 0,
                new Intent(context, SplashActivity.class),
                PendingIntent.FLAG_UPDATE_CURRENT | PendingIntent.FLAG_IMMUTABLE);
        views.setOnClickPendingIntent(R.id.widgetTitle, openApp);

        return views;
    }
}
