package io.aegis.afkbridge;

import org.slf4j.Logger;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

/**
 * Writes the current {@link BridgeState.Snapshot} to %APPDATA%\AegisAfkWatch\bridge.json once a second
 * and immediately on a change. The heartbeat doubles as "the game is still alive": if it stops, the
 * app knows the game closed or crashed. Writes go through a background thread so the game never waits
 * on the disk.
 */
final class BridgeWriter {
    private final BridgeState state;
    private final Logger log;
    private final Path file;
    private final Path temp;
    private final ScheduledExecutorService executor = Executors.newSingleThreadScheduledExecutor(task -> {
        Thread t = new Thread(task, "aegis-afk-bridge");
        t.setDaemon(true);
        return t;
    });
    private boolean warned;

    BridgeWriter(BridgeState state, Logger log) {
        this.state = state;
        this.log = log;
        String appData = System.getenv("APPDATA");
        Path dir = appData == null ? null : Path.of(appData, "AegisAfkWatch");
        this.file = dir == null ? null : dir.resolve("bridge.json");
        this.temp = dir == null ? null : dir.resolve("bridge.json.tmp");
    }

    boolean available() {
        return file != null;
    }

    void start() {
        executor.scheduleWithFixedDelay(this::write, 0, 1, TimeUnit.SECONDS);
    }

    /** Publish right now (used when something just changed). */
    void flush() {
        executor.execute(this::write);
    }

    private void write() {
        try {
            Files.createDirectories(file.getParent());
            String json = state.snapshot(System.currentTimeMillis()).toJson();
            Files.writeString(temp, json);
            try {
                Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING, StandardCopyOption.ATOMIC_MOVE);
            } catch (IOException atomicFailed) {
                Files.writeString(file, json); // reader will simply skip a half-written read
            }
        } catch (IOException | RuntimeException e) {
            if (!warned) {
                warned = true;
                log.warn("Could not write {}: {}", file, e.toString());
            }
        }
    }
}
