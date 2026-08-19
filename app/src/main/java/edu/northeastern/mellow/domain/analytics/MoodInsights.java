package edu.northeastern.mellow.domain.analytics;

import java.time.LocalDate;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import edu.northeastern.mellow.data.model.JournalEntry;
import edu.northeastern.mellow.data.model.MoodEntry;

/**
 * On-device "mood intelligence" — pure, dependency-free analytics over mood
 * logs and journal moods. No Firebase, no Android, no network. All static so
 * it is trivially unit-testable, exactly like {@link MoodAnalytics}.
 *
 * It merges two mood sources (the mood log and the mood attached to each
 * journal entry) into one daily series, then derives month/year trends,
 * direction vs. the previous period, the strongest weekday, and a simple
 * journaling correlation.
 */
public final class MoodInsights {

    private MoodInsights() {}

    public enum Period { MONTH, YEAR }

    /** Computed, ready-to-render result. */
    public static final class Insights {
        public final float[] series;      // chart values; -1f = no data that slot
        public final String[] labels;     // x-axis labels (may contain "")
        public final float currentAvg;    // average over the window (0 if none)
        public final int loggedCount;     // number of populated slots in the window
        public final float delta;         // currentAvg minus previous window avg
        public final List<String> lines;  // human-readable insight sentences

        Insights(float[] series, String[] labels, float currentAvg,
                 int loggedCount, float delta, List<String> lines) {
            this.series = series;
            this.labels = labels;
            this.currentAvg = currentAvg;
            this.loggedCount = loggedCount;
            this.delta = delta;
            this.lines = lines;
        }
    }

    // --- Public entry point ---

    public static Insights compute(List<MoodEntry> moods,
                                   List<JournalEntry> journals,
                                   Period period,
                                   LocalDate today) {
        Map<LocalDate, float[]> daily = dailyAverages(moods, journals); // date -> {sum,count}

        if (period == Period.YEAR) {
            return computeYear(daily, moods, journals, today);
        }
        return computeMonth(daily, moods, journals, today);
    }

    // --- Windows ---

    private static Insights computeMonth(Map<LocalDate, float[]> daily,
                                         List<MoodEntry> moods,
                                         List<JournalEntry> journals,
                                         LocalDate today) {
        int n = 30;
        float[] series = new float[n];
        String[] labels = new String[n];
        float sum = 0f; int count = 0;

        for (int i = 0; i < n; i++) {
            LocalDate d = today.minusDays(n - 1 - i);
            Float avg = avgFor(daily, d);
            if (avg != null) { series[i] = avg; sum += avg; count++; }
            else series[i] = -1f;
            // sparse labels: start, ~middle, end
            if (i == 0 || i == n - 1 || i == n / 2) {
                labels[i] = d.getDayOfMonth() + " "
                        + d.getMonth().getDisplayName(TextStyle.SHORT, Locale.getDefault());
            } else {
                labels[i] = "";
            }
        }
        float currentAvg = count == 0 ? 0f : sum / count;

        // previous 30-day window for direction
        float prevSum = 0f; int prevCount = 0;
        for (int i = 0; i < n; i++) {
            LocalDate d = today.minusDays(2 * n - 1 - i);
            Float avg = avgFor(daily, d);
            if (avg != null) { prevSum += avg; prevCount++; }
        }
        float delta = (count == 0 || prevCount == 0) ? 0f : currentAvg - (prevSum / prevCount);

        List<String> lines = buildLines(daily, moods, journals, today, count,
                delta, "the last 30 days", "last month");
        return new Insights(series, labels, currentAvg, count, delta, lines);
    }

    private static Insights computeYear(Map<LocalDate, float[]> daily,
                                        List<MoodEntry> moods,
                                        List<JournalEntry> journals,
                                        LocalDate today) {
        int n = 12;
        float[] series = new float[n];
        String[] labels = new String[n];
        float sum = 0f; int count = 0;

        for (int i = 0; i < n; i++) {
            LocalDate month = today.minusMonths(n - 1 - i);
            float mSum = 0f; int mCount = 0;
            for (Map.Entry<LocalDate, float[]> e : daily.entrySet()) {
                LocalDate d = e.getKey();
                if (d.getYear() == month.getYear() && d.getMonthValue() == month.getMonthValue()) {
                    mSum += e.getValue()[0] / e.getValue()[1];
                    mCount++;
                }
            }
            if (mCount > 0) { float a = mSum / mCount; series[i] = a; sum += a; count++; }
            else series[i] = -1f;
            labels[i] = month.getMonth().getDisplayName(TextStyle.SHORT, Locale.getDefault())
                    .substring(0, 1);
        }
        // current month avg vs previous month avg
        float currentAvg = series[n - 1] >= 0 ? series[n - 1] : 0f;
        float delta = (series[n - 1] >= 0 && series[n - 2] >= 0)
                ? series[n - 1] - series[n - 2] : 0f;

        List<String> lines = buildLines(daily, moods, journals, today, countDays(daily, today, 365),
                delta, "this year", "last month");
        // overall window avg for the headline in year mode
        float overall = count == 0 ? currentAvg : sum / count;
        return new Insights(series, labels, overall, countDays(daily, today, 365), delta, lines);
    }

    // --- Insight sentences ---

    private static List<String> buildLines(Map<LocalDate, float[]> daily,
                                           List<MoodEntry> moods,
                                           List<JournalEntry> journals,
                                           LocalDate today,
                                           int loggedCount,
                                           float delta,
                                           String windowLabel,
                                           String prevLabel) {
        List<String> lines = new ArrayList<>();

        if (loggedCount == 0) {
            lines.add("Log a few moods and your trends will appear here.");
            return lines;
        }

        lines.add("You've tracked your mood on " + loggedCount + " of " + windowLabel + ".");

        if (Math.abs(delta) >= 0.15f) {
            String dir = delta > 0 ? "trending upward" : "dipping";
            lines.add("Your mood is " + dir + " vs " + prevLabel
                    + " (" + formatDelta(delta) + ").");
        } else {
            lines.add("Your mood has held steady vs " + prevLabel + ".");
        }

        String best = bestWeekday(daily);
        if (best != null) lines.add("You tend to feel your best on " + best + "s.");

        String jc = journalingCorrelation(moods, journals);
        if (jc != null) lines.add(jc);

        return lines;
    }

    /** Weekday (e.g. "Saturday") with the highest average, or null if too little data. */
    static String bestWeekday(Map<LocalDate, float[]> daily) {
        float[] sum = new float[7];
        int[] cnt = new int[7];
        for (Map.Entry<LocalDate, float[]> e : daily.entrySet()) {
            int idx = e.getKey().getDayOfWeek().getValue() - 1; // 0=Mon..6=Sun
            sum[idx] += e.getValue()[0] / e.getValue()[1];
            cnt[idx]++;
        }
        int distinct = 0;
        for (int c : cnt) if (c > 0) distinct++;
        if (distinct < 3) return null; // not enough spread to be meaningful

        int best = -1; float bestAvg = -1f;
        for (int i = 0; i < 7; i++) {
            if (cnt[i] == 0) continue;
            float a = sum[i] / cnt[i];
            if (a > bestAvg) { bestAvg = a; best = i; }
        }
        if (best < 0) return null;
        return java.time.DayOfWeek.of(best + 1)
                .getDisplayName(TextStyle.FULL, Locale.getDefault());
    }

    /** "On days you journal, your mood averages +X" or null if not enough data. */
    static String journalingCorrelation(List<MoodEntry> moods, List<JournalEntry> journals) {
        if (moods == null || journals == null || journals.isEmpty()) return null;
        Set<String> journalDates = new HashSet<>();
        for (JournalEntry j : journals) if (j.getDate() != null) journalDates.add(j.getDate());
        if (journalDates.isEmpty()) return null;

        float onSum = 0f; int onCount = 0;
        float offSum = 0f; int offCount = 0;
        for (MoodEntry m : moods) {
            if (m.getMoodScore() < 1 || m.getMoodScore() > 5 || m.getDate() == null) continue;
            if (journalDates.contains(m.getDate())) { onSum += m.getMoodScore(); onCount++; }
            else { offSum += m.getMoodScore(); offCount++; }
        }
        if (onCount < 2 || offCount < 2) return null;
        float diff = (onSum / onCount) - (offSum / offCount);
        if (diff >= 0.3f) {
            return "On days you journal, your mood is " + String.format(Locale.US, "%.1f", diff)
                    + " higher on average.";
        }
        return null;
    }

    // --- Helpers ---

    /** date -> {sum, count} of all mood scores (mood log + journal) that day. */
    static Map<LocalDate, float[]> dailyAverages(List<MoodEntry> moods, List<JournalEntry> journals) {
        Map<LocalDate, float[]> map = new HashMap<>();
        if (moods != null) {
            for (MoodEntry m : moods) add(map, m.getDate(), m.getMoodScore());
        }
        if (journals != null) {
            for (JournalEntry j : journals) add(map, j.getDate(), j.getMoodScore());
        }
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

    private static Float avgFor(Map<LocalDate, float[]> daily, LocalDate d) {
        float[] agg = daily.get(d);
        if (agg == null || agg[1] == 0f) return null;
        return agg[0] / agg[1];
    }

    private static int countDays(Map<LocalDate, float[]> daily, LocalDate today, int withinDays) {
        int c = 0;
        LocalDate from = today.minusDays(withinDays);
        for (LocalDate d : daily.keySet()) {
            if (!d.isBefore(from) && !d.isAfter(today)) c++;
        }
        return c;
    }

    private static String formatDelta(float delta) {
        String sign = delta > 0 ? "+" : "";
        return sign + String.format(Locale.US, "%.1f", delta);
    }
}
