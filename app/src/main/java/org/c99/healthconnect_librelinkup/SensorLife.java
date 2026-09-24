/*
 * Eric's fork, 2026. Apache License, Version 2.0, as the rest of the app.
 */

package org.c99.healthconnect_librelinkup;

import java.time.Duration;
import java.time.Instant;

/**
 * When the sensor ends. LibreView sends the activation time but no expiry,
 * so the end is the activation plus the sensor's life, a constant per
 * model: 14 days for the Libre 3 Eric wears (15 for the Plus sensors).
 */
public final class SensorLife {
    /** Libre 2 and Libre 3. The Plus sensors run 15. */
    public static final int LIFE_DAYS = 14;

    private SensorLife() {}

    public static Instant endsAt(long activationEpochSeconds) {
        return Instant.ofEpochSecond(activationEpochSeconds).plus(Duration.ofDays(LIFE_DAYS));
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
