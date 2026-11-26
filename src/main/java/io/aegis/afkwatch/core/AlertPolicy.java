package io.aegis.afkwatch.core;

/**
 * Decides when the idle alert should go off. Pure logic: the caller supplies idle time and whether the
 * player is somewhere an alert makes sense, and does the actual beeping.
 */
public final class AlertPolicy {
    private final Settings settings;
    private long nextAt = -1;

    public AlertPolicy(Settings settings) {
        this.settings = settings;
    }

    /**
     * @param idleSeconds seconds since the player last did something
     * @param eligible    true when idle here should alert (tracked, not the AFK server, not excluded)
     * @return true exactly when an alert should fire now
     */
    public boolean shouldFire(long idleSeconds, boolean eligible) {
        if (!settings.alertEnabled || !eligible || idleSeconds < settings.alertAfterSeconds) {
            nextAt = -1; // activity (or leaving) re-arms the alert
            return false;
        }
        if (nextAt < 0) {
            nextAt = settings.alertAfterSeconds;
        }
        if (idleSeconds < nextAt) {
            return false;
        }
        nextAt = settings.alertRepeatSeconds > 0 ? idleSeconds + settings.alertRepeatSeconds : Long.MAX_VALUE;
        return true;
    }
}
