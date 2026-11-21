package io.aegis.afkwatch.stats;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;

/**
 * Time spent actively playing versus idle, per day and per gamemode. Time spent in the AFK gamemode is
 * never recorded here. Thread-safe; holds no Minecraft types.
 */
public final class Stats {

    /** Milliseconds, as plain public fields so Gson can read and write it directly. */
    public static final class Bucket {
        public long activeMs;
        public long afkMs;

        Bucket copy() {
            Bucket b = new Bucket();
            b.activeMs = activeMs;
            b.afkMs = afkMs;
            return b;
        }
    }

    public record Totals(long activeMs, long afkMs, Map<String, Bucket> byServer) {
        public long totalMs() {
            return activeMs + afkMs;
        }
    }

    private final Map<String, Map<String, Bucket>> days = new TreeMap<>();

    public synchronized void add(LocalDate date, String server, long activeMs, long afkMs) {
        Bucket b = days.computeIfAbsent(date.toString(), d -> new TreeMap<>())
                .computeIfAbsent(server, s -> new Bucket());
        b.activeMs += activeMs;
        b.afkMs += afkMs;
    }

    /** Reclassifies already-counted active time as idle, never going below zero. */
    public synchronized void moveActiveToAfk(LocalDate date, String server, long ms) {
        Map<String, Bucket> day = days.get(date.toString());
        Bucket b = day == null ? null : day.get(server);
        if (b == null) {
            return;
        }
        long moved = Math.min(ms, b.activeMs);
        b.activeMs -= moved;
        b.afkMs += moved;
    }

    /** Everything from {@code from} (inclusive) onwards. */
    public synchronized Totals totalsSince(LocalDate from) {
        String fromKey = from.toString();
        long active = 0;
        long afk = 0;
        Map<String, Bucket> byServer = new HashMap<>();
        for (var day : days.entrySet()) {
            if (day.getKey().compareTo(fromKey) < 0) {
                continue;
            }
            for (var e : day.getValue().entrySet()) {
                active += e.getValue().activeMs;
                afk += e.getValue().afkMs;
                Bucket sum = byServer.computeIfAbsent(e.getKey(), s -> new Bucket());
                sum.activeMs += e.getValue().activeMs;
                sum.afkMs += e.getValue().afkMs;
            }
        }
        return new Totals(active, afk, byServer);
    }

    public synchronized void clear() {
        days.clear();
    }

    /** Deep copy for persistence. */
    public synchronized Map<String, Map<String, Bucket>> snapshot() {
        Map<String, Map<String, Bucket>> copy = new TreeMap<>();
        for (var day : days.entrySet()) {
            Map<String, Bucket> inner = new TreeMap<>();
            for (var e : day.getValue().entrySet()) {
                inner.put(e.getKey(), e.getValue().copy());
            }
            copy.put(day.getKey(), inner);
        }
        return copy;
    }

    public synchronized void load(Map<String, Map<String, Bucket>> data) {
        days.clear();
        if (data == null) {
            return;
        }
        for (var day : data.entrySet()) {
            if (day.getValue() == null) {
                continue;
            }
            Map<String, Bucket> inner = new TreeMap<>();
            for (var e : day.getValue().entrySet()) {
                if (e.getValue() != null) {
                    inner.put(e.getKey(), e.getValue().copy());
                }
            }
            days.put(day.getKey(), inner);
        }
    }
}
