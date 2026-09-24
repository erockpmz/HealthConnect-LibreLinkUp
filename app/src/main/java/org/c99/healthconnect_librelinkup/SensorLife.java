/*
 * Eric's fork, 2026. Apache License, Version 2.0, as the rest of the app.
 */

package org.c99.healthconnect_librelinkup;

import android.content.Context;

import java.time.Duration;
import java.time.Instant;

/**
 * When the sensor ends. LibreView sends the activation time but no expiry,
 * so the end is the activation plus the sensor's life, which is a setting
 * on the app's screen: 15 days, the Libre 3 Plus Eric wears (the original
 * Libre 3 and the Libre 2 ran 14). A wrong figure here is off by a day,
 * not dangerous, and the setting means a sensor change never needs a rebuild.
 */
public final class SensorLife {
    public static final int DEFAULT_LIFE_DAYS = 15;
    public static final int MIN_LIFE_DAYS = 7;
    public static final int MAX_LIFE_DAYS = 30;
    static final String KEY_LIFE_DAYS = "lifeDays";

    private SensorLife() {}

    /** The life in days from the setting, the default when unset. */
    public static int lifeDays(Context context) {
        int days = SensorStore.prefs(context).getInt(KEY_LIFE_DAYS, DEFAULT_LIFE_DAYS);
        return clamp(days);
    }

    public static void setLifeDays(Context context, int days) {
        SensorStore.prefs(context).edit().putInt(KEY_LIFE_DAYS, clamp(days)).apply();
        SensorStore.recomputeEnd(context);
    }

    public static int clamp(int days) {
        return Math.max(MIN_LIFE_DAYS, Math.min(MAX_LIFE_DAYS, days));
    }

    public static Instant endsAt(long activationEpochSeconds, int lifeDays) {
        return Instant.ofEpochSecond(activationEpochSeconds).plus(Duration.ofDays(lifeDays));
    }

    /** Whole days left, never below zero: 6 with 6 days 5 hours to go, 0 on the last day. */
    public static long daysLeft(Instant endsAt, Instant now) {
        Duration left = Duration.between(now, endsAt);
        return left.isNegative() ? 0 : left.toDays();
    }

    /** Hours left, for the last day, never below zero. */
    public static long hoursLeft(Instant endsAt, Instant now) {
        Duration left = Duration.between(now, endsAt);
        return left.isNegative() ? 0 : left.toHours();
    }

    /** "Sensor ends in 6 days", "Sensor ends in 5 hours", or "Sensor ended". */
    public static String describe(Instant endsAt, Instant now) {
        if (!endsAt.isAfter(now)) return "Sensor ended";
        long days = daysLeft(endsAt, now);
        if (days >= 1) return "Sensor ends in " + days + (days == 1 ? " day" : " days");
        long hours = hoursLeft(endsAt, now);
        return "Sensor ends in " + hours + (hours == 1 ? " hour" : " hours");
    }
}
