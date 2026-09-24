/*
 * Eric's fork, 2026. Apache License, Version 2.0, as the rest of the app.
 */

package org.c99.healthconnect_librelinkup;

/** LibreView's trend arrow codes, as words and as arrows. */
public final class Trend {
    private Trend() {}

    /** 1 falling quickly, 2 falling, 3 steady, 4 rising, 5 rising quickly; anything else unknown. */
    public static String describe(int arrow) {
        switch (arrow) {
            case 1: return "falling quickly";
            case 2: return "falling";
            case 3: return "steady";
            case 4: return "rising";
            case 5: return "rising quickly";
            default: return "";
        }
    }

    public static String symbol(int arrow) {
        switch (arrow) {
            case 1: return "\u2193";
            case 2: return "\u2198";
            case 3: return "\u2192";
            case 4: return "\u2197";
            case 5: return "\u2191";
            default: return "";
        }
    }

    /** LibreView's colour code: 1 in range, 2 high or low (outside the target), 3 the alarm range. */
    public static String describeColor(int color, boolean isHigh, boolean isLow) {
        if (isHigh) return "high";
        if (isLow) return "low";
        switch (color) {
            case 1: return "in range";
            case 2: return "outside target";
            case 3: return "alarm";
            default: return "";
        }
    }
}
