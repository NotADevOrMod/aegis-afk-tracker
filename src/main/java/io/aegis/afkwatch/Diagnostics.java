package io.aegis.afkwatch;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;

/**
 * A plain text trail of what the mod did at startup and at key moments, written next to the settings file.
 * Uses only java.io, so it works even when a Minecraft or Fabric call fails. Capped so it cannot grow.
 */
public final class Diagnostics {
    private static final int MAX_LINES = 300;
    private static final int STACK_FRAMES = 8;
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("HH:mm:ss.SSS");

    private final Path file;
    private int lines;

    public Diagnostics(Path file) {
        this.file = file;
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, "", StandardOpenOption.CREATE, StandardOpenOption.TRUNCATE_EXISTING);
        } catch (IOException ignored) {
            // Diagnostics are best-effort.
        }
    }

    public synchronized void log(String message) {
        if (lines >= MAX_LINES) {
            return;
        }
        lines++;
        String line = LocalTime.now().format(TIME) + "  " + message
                + (lines == MAX_LINES ? System.lineSeparator() + "(log full; later events not recorded)" : "")
                + System.lineSeparator();
        try {
            Files.writeString(file, line, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException ignored) {
            // Best-effort.
        }
    }

    public synchronized void error(String where, Throwable t) {
        StringBuilder sb = new StringBuilder("ERROR in ").append(where).append(": ").append(t);
        StackTraceElement[] trace = t.getStackTrace();
        for (int i = 0; i < Math.min(STACK_FRAMES, trace.length); i++) {
            sb.append(System.lineSeparator()).append("      at ").append(trace[i]);
        }
        log(sb.toString());
    }
}
