package edu.northeastern.mellow.data.util;

import androidx.annotation.Nullable;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.time.temporal.ChronoUnit;

/**
 * Date helpers for streak calculations.
 * All dates are "YYYY-MM-DD" strings in the device's local timezone.
 * Using java.time.LocalDate — requires API 26+, which is our minSdk.
 */
public class DateUtils {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd");

    private DateUtils() {}

    /** Returns today's date as "YYYY-MM-DD". */
    public static String today() {
        return LocalDate.now().format(FORMATTER);
    }

    /** Returns true if date2 is exactly one day after date1. */
    public static boolean isConsecutiveDay(String date1, String date2) {
        LocalDate d1 = parseDate(date1);
        LocalDate d2 = parseDate(date2);
        if (d1 == null || d2 == null) return false;
        return ChronoUnit.DAYS.between(d1, d2) == 1;
    }

    /**
     * Grace period = user missed exactly one day (2 days apart).
     * e.g. last check-in Monday, today is Wednesday → grace applies.
     */
    public static boolean isWithinGracePeriod(String lastDate, String today) {
        LocalDate d1 = parseDate(lastDate);
        LocalDate d2 = parseDate(today);
        if (d1 == null || d2 == null) return false;
        return ChronoUnit.DAYS.between(d1, d2) == 2;
    }

    public static long daysBetween(String date1, String date2) {
        LocalDate d1 = parseDate(date1);
        LocalDate d2 = parseDate(date2);
        if (d1 == null || d2 == null) return -1;
        return ChronoUnit.DAYS.between(d1, d2);
    }

    @Nullable
    public static LocalDate parseDate(String dateStr) {
        if (dateStr == null || dateStr.isEmpty()) return null;
        try {
            return LocalDate.parse(dateStr, FORMATTER);
        } catch (DateTimeParseException e) {
            return null;
        }
    }
}
