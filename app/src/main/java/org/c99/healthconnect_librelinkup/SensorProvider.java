/*
 * Eric's fork, 2026. Apache License, Version 2.0, as the rest of the app.
 */

package org.c99.healthconnect_librelinkup;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.SharedPreferences;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.MatrixCursor;
import android.net.Uri;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

/**
 * What the last poll said, for HealthView.
 *
 * Read-only, guarded by a signature permission, so only an app signed with
 * the same key (Eric's HealthView) can read it. Health Connect carries the
 * glucose itself but has no record type for a sensor's life, a trend arrow
 * or a target range, which is why this exists. Two paths under
 * content://org.c99.healthconnect_librelinkup.sensor/:
 *
 *  - `sensor`: one row, or none until LibreView has reported a sensor.
 *    serial (text), activated_at_epoch_millis, ends_at_epoch_millis,
 *    life_days, warmup_minutes, product_type, last_sync_epoch_millis.
 *  - `reading`: one row, or none before the first poll. reading_at_epoch_millis,
 *    mgdl, trend_arrow (1 falling quickly .. 5 rising quickly), trend (text),
 *    color (LibreView's 1 in range, 2 outside target, 3 alarm), is_high,
 *    is_low (0/1), target_low_mgdl, target_high_mgdl, alarm_high_mgdl,
 *    alarm_low_mgdl, alarm_fixed_low_mgdl, unit (1 mg/dL, 0 mmol/L),
 *    libre_app_version (text), patient (text), last_sync_epoch_millis.
 */
public class SensorProvider extends ContentProvider {
    public static final String AUTHORITY = "org.c99.healthconnect_librelinkup.sensor";
    public static final Uri SENSOR_URI = Uri.parse("content://" + AUTHORITY + "/sensor");
    public static final Uri READING_URI = Uri.parse("content://" + AUTHORITY + "/reading");
    /** Kept for anything that used the first name. */
    public static final Uri CONTENT_URI = SENSOR_URI;

    private static final int SENSOR = 1;
    private static final int READING = 2;
    private static final UriMatcher MATCHER = new UriMatcher(UriMatcher.NO_MATCH);
    static {
        MATCHER.addURI(AUTHORITY, "sensor", SENSOR);
        MATCHER.addURI(AUTHORITY, "reading", READING);
    }

    public static final String[] SENSOR_COLUMNS = {
            "serial", "activated_at_epoch_millis", "ends_at_epoch_millis", "life_days",
            "warmup_minutes", "product_type", "last_sync_epoch_millis",
    };
    public static final String[] READING_COLUMNS = {
            "reading_at_epoch_millis", "mgdl", "trend_arrow", "trend", "color", "is_high", "is_low",
            "target_low_mgdl", "target_high_mgdl", "alarm_high_mgdl", "alarm_low_mgdl", "alarm_fixed_low_mgdl",
            "unit", "libre_app_version", "patient", "last_sync_epoch_millis",
    };

    @Override
    public boolean onCreate() {
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection, @Nullable String selection,
                        @Nullable String[] selectionArgs, @Nullable String sortOrder) {
        SharedPreferences p = SensorStore.prefs(getContext());
        MatrixCursor cursor;
        switch (MATCHER.match(uri)) {
            case SENSOR:
                cursor = new MatrixCursor(SENSOR_COLUMNS);
                if (p.contains(SensorStore.KEY_ENDS_AT)) {
                    cursor.addRow(new Object[] {
                            p.getString(SensorStore.KEY_SERIAL, ""),
                            p.getLong(SensorStore.KEY_ACTIVATED_AT, 0),
                            p.getLong(SensorStore.KEY_ENDS_AT, 0),
                            p.getInt(SensorStore.KEY_LIFE_DAYS_USED, SensorLife.DEFAULT_LIFE_DAYS),
                            p.getInt(SensorStore.KEY_WARMUP_MINUTES, 0),
                            p.getInt(SensorStore.KEY_PRODUCT_TYPE, 0),
                            p.getLong(SensorStore.KEY_LAST_SYNC, 0),
                    });
                }
                break;
            case READING:
                cursor = new MatrixCursor(READING_COLUMNS);
                if (p.contains(SensorStore.KEY_READING_AT)) {
                    int arrow = p.getInt(SensorStore.KEY_TREND_ARROW, 0);
                    cursor.addRow(new Object[] {
                            p.getLong(SensorStore.KEY_READING_AT, 0),
                            p.getInt(SensorStore.KEY_READING_MGDL, 0),
                            arrow,
                            Trend.describe(arrow),
                            p.getInt(SensorStore.KEY_COLOR, 0),
                            p.getBoolean(SensorStore.KEY_IS_HIGH, false) ? 1 : 0,
                            p.getBoolean(SensorStore.KEY_IS_LOW, false) ? 1 : 0,
                            p.getInt(SensorStore.KEY_TARGET_LOW, 0),
                            p.getInt(SensorStore.KEY_TARGET_HIGH, 0),
                            p.getInt(SensorStore.KEY_ALARM_HIGH, 0),
                            p.getInt(SensorStore.KEY_ALARM_LOW, 0),
                            p.getInt(SensorStore.KEY_ALARM_FIXED_LOW, 0),
                            p.getInt(SensorStore.KEY_UNIT, 1),
                            p.getString(SensorStore.KEY_APP_VERSION, ""),
                            p.getString(SensorStore.KEY_PATIENT, ""),
                            p.getLong(SensorStore.KEY_LAST_SYNC, 0),
                    });
                }
                break;
            default:
                throw new IllegalArgumentException("Unknown uri " + uri);
        }
        cursor.setNotificationUri(getContext().getContentResolver(), uri);
        return cursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        switch (MATCHER.match(uri)) {
            case SENSOR: return "vnd.android.cursor.item/vnd.org.c99.healthconnect_librelinkup.sensor";
            case READING: return "vnd.android.cursor.item/vnd.org.c99.healthconnect_librelinkup.reading";
            default: return null;
        }
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues values) {
        throw new UnsupportedOperationException("read-only");
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        throw new UnsupportedOperationException("read-only");
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues values, @Nullable String selection,
                      @Nullable String[] selectionArgs) {
        throw new UnsupportedOperationException("read-only");
    }
}
