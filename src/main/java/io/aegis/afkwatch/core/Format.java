package io.aegis.afkwatch.core;

final class Format {
    private Format() {}

    static String clock(long totalSeconds) {
        long s = Math.max(0, totalSeconds);
        long h = s / 3600;
        long m = (s % 3600) / 60;
        long sec = s % 60;
        return h > 0
                ? String.format("%d:%02d:%02d", h, m, sec)
                : String.format("%d:%02d", m, sec);
    }
}
