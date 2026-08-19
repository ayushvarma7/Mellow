package edu.northeastern.mellow.domain.analytics;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.TextStyle;
import java.util.List;
import java.util.Locale;

import edu.northeastern.mellow.data.model.MoodEntry;
import edu.northeastern.mellow.data.model.MoodSummary;

/**
 * Pure static analytics — zero Firebase, zero Android dependencies.
 * Takes a list of mood entries and returns computed summaries.
 */
public class MoodAnalytics {

    private MoodAnalytics() {}

    /**
     * Computes a MoodSummary for `currentWeek` entries, with trend calculated
     * against `previousWeek` entries.
     *
     * Both lists should already be filtered to their respective 7-day windows
     * before calling this — the caller (repository) handles the filtering.
     */
    public static MoodSummary computeWeeklySummary(
            List<MoodEntry> currentWeek,
            List<MoodEntry> previousWeek,
            String weekLabel) {

        if (currentWeek.isEmpty()) {
            return new MoodSummary(weekLabel, 0f, 0, 0, 0, 0f);
        }

        float currentAvg = average(currentWeek);
        float previousAvg = previousWeek.isEmpty() ? 0f : average(previousWeek);
        float trend = previousWeek.isEmpty() ? 0f : currentAvg - previousAvg;

        int highest = currentWeek.stream().mapToInt(MoodEntry::getMoodScore).max().orElse(0);
        int lowest  = currentWeek.stream().mapToInt(MoodEntry::getMoodScore).min().orElse(0);

        return new MoodSummary(weekLabel, currentAvg, currentWeek.size(), highest, lowest, trend);
    }

    /**
     * Builds a human-readable week label for the 7-day window ending on `endDate`.
     * Example: "Mar 20 – 26"
     */
    public static String buildWeekLabel(LocalDate endDate) {
        LocalDate startDate = endDate.minusDays(6);
        DateTimeFormatter dayFmt = DateTimeFormatter.ofPattern("d");
        String month = startDate.getMonth().getDisplayName(TextStyle.SHORT, Locale.getDefault());
        return month + " " + startDate.format(dayFmt) + " – " + endDate.format(dayFmt);
    }

    /**
     * Returns true if `entry` falls within the 7-day window ending on `windowEnd` (inclusive).
     */
    public static boolean isInWindow(MoodEntry entry, LocalDate windowEnd) {
        LocalDate windowStart = windowEnd.minusDays(6);
        try {
            LocalDate entryDate = LocalDate.parse(entry.getDate());
            return !entryDate.isBefore(windowStart) && !entryDate.isAfter(windowEnd);
        } catch (Exception e) {
            return false;
        }
    }

    private static float average(List<MoodEntry> entries) {
        int sum = entries.stream().mapToInt(MoodEntry::getMoodScore).sum();
        return (float) sum / entries.size();
    }
}
