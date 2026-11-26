package io.aegis.afkwatch.core;

/**
 * Pure state machine: knows nothing about Minecraft. Fed with events, answers
 * "what should the indicator show right now?".
 *
 * <p>Methods are synchronized because events arrive from the game thread and
 * the network thread while the window reads from the Swing thread.
 */
public final class Session {

    private final Settings settings;

    private boolean connected;
    private String host = "";
    private boolean onTarget;
    private boolean afkServer;
    private String server = "";
    private long lastActivity;
    private long joinCount;

    private boolean dropped;
    private long droppedAt;
    private long idleAtDrop;

    public Session(Settings settings) {
        this.settings = settings;
    }

    public synchronized void onJoin(String host, long now) {
        boolean target = host != null && settings.matchesHost(host);
        if (connected && onTarget && target) {
            // Proxy hop between gamemodes: same connection, keep the server we already learned.
            lastActivity = now;
            return;
        }
        connected = true;
        this.host = host == null ? "" : host;
        onTarget = target;
        dropped = false;
        afkServer = false;
        server = "";
        lastActivity = now;
        joinCount++;
    }

    public synchronized void onDisconnect(long now) {
        if (connected && onTarget) {
            long idle = (now - lastActivity) / 1000;
            if (idle >= settings.afkServerAfterSeconds) {
                dropped = true;
                droppedAt = now;
                idleAtDrop = idle;
            } else {
                onTarget = false; // a deliberate leave while active is not worth flagging
            }
        }
        connected = false;
    }

    public synchronized void onMessage(String line, long now) {
        if (!connected || !onTarget) {
            return;
        }
        ServerNames.parseSwitch(line).ifPresent(name -> {
            server = name;
            afkServer = settings.isAfkServer(name);
            lastActivity = now; // switching gamemode is something you just did
        });
    }

    public synchronized void onActivity(long now) {
        if (connected && onTarget) {
            lastActivity = Math.max(lastActivity, now);
        }
    }

    /** True while connected to a server that matches {@code serverHostFragments}. */
    public synchronized boolean isTracking() {
        return connected && onTarget;
    }

    /** The address the game reported for the current connection (empty in singleplayer). */
    public synchronized String host() {
        return host;
    }

    /** Seconds since the last detected activity. */
    public synchronized long idleSeconds(long now) {
        return Math.max(0, (now - lastActivity) / 1000);
    }

    /** True while connected to a tracked server and sitting in its AFK gamemode. */
    public synchronized boolean inAfkServer() {
        return connected && onTarget && afkServer;
    }

    /** The gamemode the network last sent us to, or an empty string if unknown. */
    public synchronized String serverName() {
        return server;
    }

    /** Increments on every genuinely new connection; lets the window reset "hide for this session". */
    public synchronized long joinCount() {
        return joinCount;
    }

    public synchronized View view(long now) {
        if (!onTarget) {
            return View.INACTIVE;
        }
        if (!connected) {
            boolean fresh = dropped && now - droppedAt <= settings.disconnectLingerSeconds * 1000L;
            return fresh ? new View(Mode.DISCONNECTED, 0, idleAtDrop, server) : View.INACTIVE;
        }
        if (!afkServer && settings.isIgnoredServer(server)) {
            return View.INACTIVE; // excluded gamemode: nothing to show, count or alert
        }
        long idle = Math.max(0, (now - lastActivity) / 1000);
        if (afkServer) {
            return idle >= settings.afkServerAfterSeconds
                    ? new View(Mode.AFK_SERVER, 0, idle, server)
                    : new View(Mode.ACTIVE, 0, idle, server);
        }
        int severity = 0;
        for (int step : settings.warningStepsSeconds) {
            if (idle >= step) {
                severity++;
            }
        }
        return severity > 0
                ? new View(Mode.WARNING, severity, idle, server)
                : new View(Mode.ACTIVE, 0, idle, server);
    }
}
