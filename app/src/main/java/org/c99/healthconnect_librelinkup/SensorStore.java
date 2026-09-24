/*
 * Eric's fork, 2026. Apache License, Version 2.0, as the rest of the app.
 */

package org.c99.healthconnect_librelinkup;

import android.content.Context;
import android.content.SharedPreferences;

import java.time.Instant;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.time.format.FormatStyle;

/**
 * What the last poll said, beyond the glucose that goes to Health Connect:
 * the sensor, the latest reading with its trend, the target range and the
 * alarms. Kept so the screen and the content provider can answer without
 * a network call. Written by the sync worker on every successful poll.
 */
public final class SensorStore {
    private static final String PREFS = "sensor";
    static final String KEY_SERIAL = "serial";
    static final String KEY_ACTIVATED_AT = "activatedAtEpochMillis";
    static final String KEY_ENDS_AT = "endsAtEpochMillis";
    static final String KEY_WARMUP_MINUTES = "warmupMinutes";
    static final String KEY_PRODUCT_TYPE = "productType";
    static final String KEY_LAST_SYNC = "lastSyncEpochMillis";

    static final String KEY_READING_AT = "readingAtEpochMillis";
    static final String KEY_READING_MGDL = "readingMgdl";
    static final String KEY_TREND_ARROW = "trendArrow";
    static final String KEY_COLOR = "color";
    static final String KEY_IS_HIGH = "isHigh";
    static final String KEY_IS_LOW = "isLow";
    static final String KEY_TARGET_LOW = "targetLow";
    static final String KEY_TARGET_HIGH = "targetHigh";
    static final String KEY_ALARM_HIGH = "alarmHigh";
    static final String KEY_ALARM_LOW = "alarmLow";
    static final String KEY_ALARM_FIXED_LOW = "alarmFixedLow";
    static final String KEY_UNIT = "unit";
    static final String KEY_APP_VERSION = "libreAppVersion";
    static final String KEY_PATIENT = "patient";

    private SensorStore() {}

    static SharedPreferences prefs(Context context) {
        return context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
    }

    /** Records everything the connection said and tells anyone watching the provider. */
    public static void save(Context context, LibreLinkUp.Connection c, Instant readingAt, long lastSyncEpochMillis) {
        if (c == null) return;
        SharedPreferences.Editor e = prefs(context).edit();
        LibreLinkUp.Sensor sensor = c.sensor;
        if (sensor != null && sensor.a > 0) {
            e.putString(KEY_SERIAL, sensor.sn == null ? "" : sensor.sn)
                    .putLong(KEY_ACTIVATED_AT, Instant.ofEpochSecond(sensor.a).toEpochMilli())
                    .putLong(KEY_ENDS_AT, SensorLife.endsAt(sensor.a).toEpochMilli())
                    .putInt(KEY_WARMUP_MINUTES, sensor.w)
                    .putInt(KEY_PRODUCT_TYPE, sensor.pt);
        }
        LibreLinkUp.GlucoseMeasurement gm = c.glucoseMeasurement;
        if (gm != null && readingAt != null) {
            e.putLong(KEY_READING_AT, readingAt.toEpochMilli())
                    .putInt(KEY_READING_MGDL, gm.ValueInMgPerDl)
                    .putInt(KEY_TREND_ARROW, gm.TrendArrow)
                    .putInt(KEY_COLOR, gm.MeasurementColor)
                    .putBoolean(KEY_IS_HIGH, gm.isHigh)
                    .putBoolean(KEY_IS_LOW, gm.isLow);
        }
        e.putInt(KEY_TARGET_LOW, c.targetLow)
                .putInt(KEY_TARGET_HIGH, c.targetHigh)
                .putInt(KEY_UNIT, c.uom);
        if (c.alarmRules != null) {
            if (c.alarmRules.h != null) e.putInt(KEY_ALARM_HIGH, c.alarmRules.h.th);
            if (c.alarmRules.l != null) e.putInt(KEY_ALARM_LOW, c.alarmRules.l.th);
            if (c.alarmRules.f != null) e.putInt(KEY_ALARM_FIXED_LOW, c.alarmRules.f.th);
        }
        if (c.patientDevice != null && c.patientDevice.v != null) e.putString(KEY_APP_VERSION, c.patientDevice.v);
        String patient = ((c.firstName == null ? "" : c.firstName) + " " + (c.lastName == null ? "" : c.lastName)).trim();
        e.putString(KEY_PATIENT, patient);
        e.putLong(KEY_LAST_SYNC, lastSyncEpochMillis);
        e.apply();
        context.getContentResolver().notifyChange(SensorProvider.SENSOR_URI, null);
        context.getContentResolver().notifyChange(SensorProvider.READING_URI, null);
    }

    public static boolean hasSensor(Context context) {
        return prefs(context).contains(KEY_ENDS_AT);
    }

    /** A line for the screen: "Sensor ends in 6 days (3 Oct 2026)", or nothing known yet. */
    public static String describeSensor(Context context, Instant now) {
        SharedPreferences p = prefs(context);
        if (!p.contains(KEY_ENDS_AT)) return "Sensor: not reported yet";
        Instant ends = Instant.ofEpochMilli(p.getLong(KEY_ENDS_AT, 0));
        String date = DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM)
                .format(ends.atZone(ZoneId.systemDefault()));
        return SensorLife.describe(ends, now) + " (" + date + ")";
    }

    /** "112 mg/dL rising, in range, 7:45 PM. Target 70 to 180." */
    public static String describeReading(Context context) {
        SharedPreferences p = prefs(context);
        if (!p.contains(KEY_READING_AT)) return "";
        Instant at = Instant.ofEpochMilli(p.getLong(KEY_READING_AT, 0));
        String time = DateTimeFormatter.ofLocalizedTime(FormatStyle.SHORT).format(at.atZone(ZoneId.systemDefault()));
        int arrow = p.getInt(KEY_TREND_ARROW, 0);
        String trend = Trend.describe(arrow);
        String state = Trend.describeColor(p.getInt(KEY_COLOR, 0), p.getBoolean(KEY_IS_HIGH, false), p.getBoolean(KEY_IS_LOW, false));
        StringBuilder b = new StringBuilder();
        b.append("Last reading ").append(p.getInt(KEY_READING_MGDL, 0)).append(" mg/dL");
        if (!trend.isEmpty()) b.append(" ").append(Trend.symbol(arrow)).append(" ").append(trend);
        if (!state.isEmpty()) b.append(", ").append(state);
        b.append(", ").append(time).append(".");
        int low = p.getInt(KEY_TARGET_LOW, 0), high = p.getInt(KEY_TARGET_HIGH, 0);
        if (low > 0 && high > 0) b.append(" Target ").append(low).append(" to ").append(high).append(".");
        return b.toString();
    }
}
