package io.aegis.afkwatch.core;

public final class Format {
    private Format() {}

    public static String clock(long totalSeconds) {
        long s = Math.max(0, totalSeconds);
        long h = s / 3600;
        long m = (s % 3600) / 60;
        long sec = s % 60;
        return h > 0
                ? String.format("%d:%02d:%02d", h, m, sec)
                : String.format("%d:%02d", m, sec);
    }

    /** Compact duration for summaries: "1h 12m", "14m", "45s". */
    public static String duration(long millis) {
        long s = Math.max(0, millis / 1000);
        long h = s / 3600;
        long m = (s % 3600) / 60;
        if (h > 0) {
            return h + "h " + String.format("%02d", m) + "m";
        }
        if (m > 0) {
            return m + "m";
        }
        return s + "s";
    }

    public static String percent(long part, long whole) {
        return whole <= 0 ? "-" : Math.round(100.0 * part / whole) + "%";
    }
}
