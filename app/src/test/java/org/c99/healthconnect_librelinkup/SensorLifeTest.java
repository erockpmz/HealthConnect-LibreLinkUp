package org.c99.healthconnect_librelinkup;

import static org.junit.Assert.assertEquals;

import java.time.Instant;

import org.junit.Test;

public class SensorLifeTest {
    private final long activation = Instant.parse("2026-09-23T00:00:00Z").getEpochSecond();
    private final Instant ends = SensorLife.endsAt(activation, 14);

    @Test
    public void theEndIsTheActivationPlusTheLife() {
        assertEquals(Instant.parse("2026-10-07T00:00:00Z"), ends);
        // Eric's Libre 3 Plus: 15 days, the default.
        assertEquals(15, SensorLife.DEFAULT_LIFE_DAYS);
        assertEquals(Instant.parse("2026-10-08T00:00:00Z"), SensorLife.endsAt(activation, SensorLife.DEFAULT_LIFE_DAYS));
    }

    @Test
    public void theLifeSettingIsKeptWithinReason() {
        assertEquals(7, SensorLife.clamp(1));
        assertEquals(30, SensorLife.clamp(90));
        assertEquals(15, SensorLife.clamp(15));
    }

    @Test
    public void wholeDaysLeftRoundDown() {
        Instant now = Instant.parse("2026-09-30T19:00:00Z"); // 6 days 5 hours to go
        assertEquals(6, SensorLife.daysLeft(ends, now));
        assertEquals("Sensor ends in 6 days", SensorLife.describe(ends, now));
    }

    @Test
    public void theLastDayCountsHours() {
        Instant now = Instant.parse("2026-10-06T19:00:00Z");
        assertEquals(0, SensorLife.daysLeft(ends, now));
        assertEquals("Sensor ends in 5 hours", SensorLife.describe(ends, now));
    }

    @Test
    public void afterTheEndItSaysSo() {
        Instant now = Instant.parse("2026-10-08T00:00:00Z");
        assertEquals(0, SensorLife.daysLeft(ends, now));
        assertEquals("Sensor ended", SensorLife.describe(ends, now));
    }
}
