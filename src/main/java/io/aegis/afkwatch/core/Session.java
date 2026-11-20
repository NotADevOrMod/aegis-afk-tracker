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
    private boolean onTarget;
    private boolean inAfkServer;
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
        onTarget = target;
        dropped = false;
        inAfkServer = false;
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
            inAfkServer = settings.isAfkServer(name);
            lastActivity = now; // switching gamemode is something you just did
        });
    }

    public synchronized void onActivity(long now) {
        if (connected && onTarget) {
            lastActivity = Math.max(lastActivity, now);
        }
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
        long idle = Math.max(0, (now - lastActivity) / 1000);
        if (inAfkServer) {
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
