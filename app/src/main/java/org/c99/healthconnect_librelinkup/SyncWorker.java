/*
 * Copyright (c) 2024 Sam Steele
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.c99.healthconnect_librelinkup;

import android.content.Context;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;

import androidx.annotation.NonNull;
import androidx.health.connect.client.HealthConnectClient;
import androidx.health.connect.client.records.BloodGlucoseRecord;
import androidx.health.connect.client.records.metadata.DataOrigin;
import androidx.health.connect.client.records.metadata.Metadata;
import androidx.health.connect.client.response.InsertRecordsResponse;
import androidx.health.connect.client.units.BloodGlucose;
import androidx.work.Worker;
import androidx.work.WorkerParameters;

import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.gms.common.api.AvailabilityException;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.android.gms.wearable.DataClient;
import com.google.android.gms.wearable.PutDataMapRequest;
import com.google.android.gms.wearable.PutDataRequest;
import com.google.android.gms.wearable.Wearable;

import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.ArrayList;
import java.util.concurrent.ExecutionException;

import kotlin.coroutines.Continuation;
import kotlin.coroutines.CoroutineContext;
import kotlin.coroutines.EmptyCoroutineContext;

public class SyncWorker extends Worker {
    private final LibreLinkUp libreLinkUp;
    private final HealthConnectClient healthConnectClient;

    private final String GLUCOSE_KEY = "org.c99.healthconnect_librelinkup.glucose";
    private final String TREND_ARROW_KEY = "org.c99.healthconnect_librelinkup.trendArrow";
    private final String COLOR_KEY = "org.c99.healthconnect_librelinkup.color";
    private final String UNITS_KEY = "org.c99.healthconnect_librelinkup.units";
    private final String TIMESTAMP_KEY = "org.c99.healthconnect_librelinkup.timestamp";

    public SyncWorker(
            @NonNull Context context,
            @NonNull WorkerParameters params) {
        super(context, params);
        libreLinkUp = new LibreLinkUp(context);
        healthConnectClient = HealthConnectClient.getOrCreate(context);
    }

    @NonNull
    @Override
    public Result doWork() {
        try {
            LibreLinkUp.ConnectionsResult result = libreLinkUp.connections();
            libreLinkUp.setAuthTicket(result.ticket);
            LibreLinkUp.Connection connection = result.data.get(0);
            LibreLinkUp.GlucoseMeasurement gm = connection.glucoseMeasurement;
            ZonedDateTime time = parseTime(gm.FactoryTimestamp);

            // Everything else the poll said, for the screen and for HealthView (Eric's 1.5.1).
            SensorStore.save(getApplicationContext(), connection, Instant.from(time), System.currentTimeMillis());

            // The latest reading, as before, but with a stable id so a second
            // poll of the same reading updates it rather than adding a twin.
            List<BloodGlucoseRecord> records = new ArrayList<>();
            records.add(record(time, gm.ValueInMgPerDl));

            // The last twelve hours from the graph endpoint: fills the holes a
            // phone off the network left, at no cost when nothing was missed,
            // since the ids are the readings' times.
            try {
                LibreLinkUp.GraphResult graph = libreLinkUp.graph(connection.patientId);
                if (graph != null && graph.ticket != null) libreLinkUp.setAuthTicket(graph.ticket);
                if (graph != null && graph.data != null && graph.data.graphData != null) {
                    for (LibreLinkUp.GlucoseMeasurement point : graph.data.graphData) {
                        if (point == null || point.ValueInMgPerDl <= 0 || point.FactoryTimestamp == null) continue;
                        ZonedDateTime at = parseTime(point.FactoryTimestamp);
                        if (at.toInstant().equals(time.toInstant())) continue;
                        records.add(record(at, point.ValueInMgPerDl));
                    }
                }
            } catch (Exception e) {
                // The graph is a bonus; the latest reading still goes through.
                e.printStackTrace();
            }

            healthConnectClient.insertRecords(records, new Continuation<InsertRecordsResponse>() {
                @NonNull
                @Override
                public CoroutineContext getContext() {
                    return EmptyCoroutineContext.INSTANCE;
                }

                @Override
                public void resumeWith(@NonNull Object o) {

                }
            });

            if(GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(getApplicationContext()) == com.google.android.gms.common.ConnectionResult.SUCCESS) {
                try {
                    DataClient dc = Wearable.getDataClient(getApplicationContext());
                    Task<Void> t = GoogleApiAvailability.getInstance().checkApiAvailability(dc);
                    Tasks.await(t);

                    PutDataMapRequest putDataMapReq = PutDataMapRequest.create("/glucose");
                    putDataMapReq.getDataMap().putFloat(GLUCOSE_KEY, result.data.get(0).glucoseMeasurement.Value);
                    putDataMapReq.getDataMap().putInt(COLOR_KEY, result.data.get(0).glucoseMeasurement.MeasurementColor);
                    putDataMapReq.getDataMap().putInt(TREND_ARROW_KEY, result.data.get(0).glucoseMeasurement.TrendArrow);
                    putDataMapReq.getDataMap().putInt(UNITS_KEY, result.data.get(0).glucoseMeasurement.GlucoseUnits);
                    putDataMapReq.getDataMap().putString(TIMESTAMP_KEY, result.data.get(0).glucoseMeasurement.FactoryTimestamp);
                    PutDataRequest putDataReq = putDataMapReq.asPutDataRequest();
                    Tasks.await(dc.putDataItem(putDataReq));
                } catch (Exception e) {
                    //Wearable API not available
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
            return Result.failure();
        }

        return Result.success();
    }

    /**
     * LibreView's "M/d/yyyy h:mm:ss a" is UTC; upstream (1.5) then moves it
     * to the phone's zone for the Google Health app's sake. Same instant
     * either way, which is what Health Connect stores. Unchanged from
     * upstream, only pulled out so the graph points go through it too.
     */
    static ZonedDateTime parseTime(String factoryTimestamp) {
        if (factoryTimestamp == null) return ZonedDateTime.now();
        try {
            return ZonedDateTime.parse(factoryTimestamp + " +0000", DateTimeFormatter.ofPattern("M/d/y h:m:s a Z")).withZoneSameInstant(ZoneId.systemDefault());
        } catch (DateTimeParseException e) {
            try {
                return ZonedDateTime.ofInstant(Instant.ofEpochMilli(Long.parseLong(factoryTimestamp)), ZoneId.systemDefault());
            } catch (NumberFormatException nfe) {
                return ZonedDateTime.now();
            }
        }
    }

    /** One reading, keyed on its time so re-sending it updates rather than duplicates. */
    private BloodGlucoseRecord record(ZonedDateTime time, int mgdl) {
        long seconds = time.toEpochSecond();
        return new BloodGlucoseRecord(
                Instant.from(time),
                time.getOffset(),
                BloodGlucose.milligramsPerDeciliter(mgdl),
                BloodGlucoseRecord.SPECIMEN_SOURCE_INTERSTITIAL_FLUID,
                0,
                BloodGlucoseRecord.RELATION_TO_MEAL_UNKNOWN,
                new Metadata("", new DataOrigin(getApplicationContext().getPackageName()), Instant.from(time), "llu-" + seconds, seconds, null, 0)
        );
    }
}
