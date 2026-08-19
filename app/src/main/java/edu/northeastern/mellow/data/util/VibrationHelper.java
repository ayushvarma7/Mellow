package edu.northeastern.mellow.data.util;

import android.content.Context;
import android.os.VibrationEffect;
import android.os.Vibrator;

/**
 * Haptic patterns for the Nudge feature.
 * VIBRATE is a normal permission — no runtime request needed.
 */
public class VibrationHelper {

    // Light single tick on finger-down — "I feel your touch"
    private static final long PRESS_MS = 30;

    // Rising pattern on send — "nudge sent!"
    private static final long[] SENT_PATTERN = {0, 60, 60, 120};

    // Double-pulse on receive — "someone nudged you!"
    private static final long[] RECEIVE_PATTERN = {0, 150, 80, 150};

    /** Call on ACTION_DOWN — immediate light confirmation. */
    public static void onPress(Context context) {
        Vibrator v = vibrator(context);
        if (v == null) return;
        v.vibrate(VibrationEffect.createOneShot(PRESS_MS, VibrationEffect.DEFAULT_AMPLITUDE));
    }

    /** Call when long-press threshold is reached and nudge is sent. */
    public static void onSent(Context context) {
        Vibrator v = vibrator(context);
        if (v == null) return;
        v.vibrate(VibrationEffect.createWaveform(SENT_PATTERN, -1));
    }

    /** Call on the receiver's device when a new nudge arrives. */
    public static void nudge(Context context) {
        Vibrator v = vibrator(context);
        if (v == null) return;
        v.vibrate(VibrationEffect.createWaveform(RECEIVE_PATTERN, -1));
    }

    private static Vibrator vibrator(Context context) {
        Vibrator v = (Vibrator) context.getSystemService(Context.VIBRATOR_SERVICE);
        return (v != null && v.hasVibrator()) ? v : null;
    }
}
