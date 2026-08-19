package edu.northeastern.mellow.domain.analytics;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import edu.northeastern.mellow.data.model.JournalEntry;
import edu.northeastern.mellow.data.model.MoodEntry;

/**
 * Pure analytics behind the Mood Overview — how often each mood was logged
 * (for the bubble chart) and the value series for a chosen span (for the
 * squiggle chart). No Android, no Firebase; merges mood logs with journal moods.
 */
public final class MoodOverview {

    private MoodOverview() {}

    public enum Span { DAY, WEEK, MONTH, YEAR, ALL }

    public static final class Series {
        public final float[] values;   // 1..5, -1 = no data
        public final String[] labels;
        Series(float[] values, String[] labels) { this.values = values; this.labels = labels; }
    }

    /** counts[0..4] = how many times score 1..5 was logged. */
    public static int[] moodCounts(List<MoodEntry> moods, List<JournalEntry> journals) {
        int[] counts = new int[5];
        if (moods != null) {
            for (MoodEntry m : moods) bump(counts, m.getMoodScore());
        }
        if (journals != null) {
            for (JournalEntry j : journals) bump(counts, j.getMoodScore());
        }
        return counts;
    }

    public static int total(int[] counts) {
        int t = 0;
        for (int c : counts) t += c;
        return t;
    }

    /** Values + x labels for the requested span, ending today. */
    public static Series series(List<MoodEntry> moods, List<JournalEntry> journals,
                                Span span, LocalDate today) {
        Map<LocalDate, float[]> daily = daily(moods, journals);

        switch (span) {
            case DAY:   return byDays(daily, today, 2,  "E");
            case WEEK:  return byDays(daily, today, 7,  "E");
            case MONTH: return byDays(daily, today, 30, "d");
            case YEAR:  return byMonths(daily, today, 12);
            case ALL:
            default:    return byMonths(daily, today, 12);
        }
    }

    // --- windows ---

    private static Series byDays(Map<LocalDate, float[]> daily, LocalDate today,
                                 int n, String labelKind) {
        float[] v = new float[n];
        String[] l = new String[n];
        for (int i = 0; i < n; i++) {
            LocalDate day = today.minusDays(n - 1 - i);
            Float avg = avg(daily, day);
            v[i] = avg == null ? -1f : avg;
            if ("E".equals(labelKind)) {
                l[i] = day.getDayOfWeek().getDisplayName(TextStyle.SHORT, Locale.getDefault());
            } else {
                // sparse day-of-month labels so 30 points stay readable
                l[i] = (i == 0 || i == n - 1 || i == n / 2) ? String.valueOf(day.getDayOfMonth()) : "";
            }
        }
        return new Series(v, l);
    }

    private static Series byMonths(Map<LocalDate, float[]> daily, LocalDate today, int n) {
        float[] v = new float[n];
        String[] l = new String[n];
        for (int i = 0; i < n; i++) {
            LocalDate month = today.minusMonths(n - 1 - i);
            float sum = 0f; int count = 0;
            for (Map.Entry<LocalDate, float[]> e : daily.entrySet()) {
                LocalDate day = e.getKey();
                if (day.getYear() == month.getYear() && day.getMonthValue() == month.getMonthValue()) {
                    sum += e.getValue()[0] / e.getValue()[1];
                    count++;
                }
            }
            v[i] = count == 0 ? -1f : sum / count;
            String m = month.getMonth().getDisplayName(TextStyle.SHORT, Locale.getDefault());
            l[i] = (n <= 6 || i % 2 == 0) ? m.substring(0, 1) : "";
        }
        return new Series(v, l);
    }

    // --- helpers ---

    private static Map<LocalDate, float[]> daily(List<MoodEntry> moods, List<JournalEntry> journals) {
        Map<LocalDate, float[]> map = new HashMap<>();
        if (moods != null) for (MoodEntry m : moods) add(map, m.getDate(), m.getMoodScore());
        if (journals != null) for (JournalEntry j : journals) add(map, j.getDate(), j.getMoodScore());
        return map;
    }

    private static void add(Map<LocalDate, float[]> map, String date, int score) {
        if (date == null || score < 1 || score > 5) return;
        LocalDate d;
        try { d = LocalDate.parse(date); } catch (Exception e) { return; }
        float[] agg = map.get(d);
        if (agg == null) { agg = new float[]{0f, 0f}; map.put(d, agg); }
        agg[0] += score;
        agg[1] += 1f;
    }

    private static Float avg(Map<LocalDate, float[]> daily, LocalDate d) {
        float[] agg = daily.get(d);
        if (agg == null || agg[1] == 0f) return null;
        return agg[0] / agg[1];
    }

    private static void bump(int[] counts, int score) {
        if (score >= 1 && score <= 5) counts[score - 1]++;
    }
}
