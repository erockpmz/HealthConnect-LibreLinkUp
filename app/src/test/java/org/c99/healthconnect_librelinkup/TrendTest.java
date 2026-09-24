package org.c99.healthconnect_librelinkup;

import static org.junit.Assert.assertEquals;

import org.junit.Test;

public class TrendTest {
    @Test
    public void theFiveArrowsHaveWordsAndSymbols() {
        assertEquals("falling quickly", Trend.describe(1));
        assertEquals("steady", Trend.describe(3));
        assertEquals("rising quickly", Trend.describe(5));
        assertEquals("\u2192", Trend.symbol(3));
        assertEquals("", Trend.describe(0));
        assertEquals("", Trend.symbol(9));
    }

    @Test
    public void highAndLowFlagsOutrankTheColour() {
        assertEquals("high", Trend.describeColor(1, true, false));
        assertEquals("low", Trend.describeColor(1, false, true));
        assertEquals("in range", Trend.describeColor(1, false, false));
        assertEquals("outside target", Trend.describeColor(2, false, false));
        assertEquals("alarm", Trend.describeColor(3, false, false));
    }
}
