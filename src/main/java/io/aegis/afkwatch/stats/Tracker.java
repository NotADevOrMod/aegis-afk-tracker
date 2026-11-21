package io.aegis.afkwatch.stats;

import io.aegis.afkwatch.core.View;
import io.aegis.afkwatch.core.Mode;
import io.aegis.afkwatch.core.Session;
import io.aegis.afkwatch.core.Settings;

import java.time.LocalDate;

/**
 * Turns the session state into statistics, once per client tick.
 *
 * <p>Rules: time in the AFK gamemode is never counted. Anywhere else, time is "active" until the player
 * has been idle for the first warning threshold; at that moment the whole idle stretch (including the
 * threshold already elapsed) is reclassified as idle, and counting continues as idle until they move.
 */
public final class Tracker {
    private static final long MAX_STEP_MS = 1000; // ignore long gaps (pauses, freezes)

    private final Settings settings;
    private final Session session;
    private final Stats lifetime;
    private final Stats thisSession;

    private long lastTick = -1;
    private boolean wasIdle;

    public Tracker(Settings settings, Session session, Stats lifetime, Stats thisSession) {
        this.settings = settings;
        this.session = session;
        this.lifetime = lifetime;
        this.thisSession = thisSession;
    }

    public void tick(long nowMs, LocalDate today) {
        long step = lastTick < 0 ? 0 : Math.min(nowMs - lastTick, MAX_STEP_MS);
        lastTick = nowMs;
        if (step <= 0) {
            return;
        }

        View view = session.view(nowMs);
        boolean counted = (view.mode() == Mode.ACTIVE || view.mode() == Mode.WARNING) && !session.inAfkServer();
        if (!counted) {
            wasIdle = false;
            return;
        }

        String server = session.serverName();
        String label = server.isEmpty() ? "Unknown" : server;

        if (view.mode() == Mode.WARNING) {
            if (!wasIdle) {
                long back = settings.warnStartSeconds() * 1000L;
                lifetime.moveActiveToAfk(today, label, back);
                thisSession.moveActiveToAfk(today, label, back);
                wasIdle = true;
            }
            lifetime.add(today, label, 0, step);
            thisSession.add(today, label, 0, step);
        } else {
            wasIdle = false;
            lifetime.add(today, label, step, 0);
            thisSession.add(today, label, step, 0);
        }
    }
}
