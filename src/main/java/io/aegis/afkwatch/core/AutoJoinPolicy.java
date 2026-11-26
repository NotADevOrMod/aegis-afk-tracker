package io.aegis.afkwatch.core;

/**
 * Decides when to send the "join the AFK queue" command: once, when the player has been idle long enough
 * somewhere an AFK queue makes sense. It does not repeat, because sending the command again could drop a
 * place in the queue. It re-arms when the player moves, or once they are in the AFK gamemode.
 */
public final class AutoJoinPolicy {
    private final Settings settings;
    private boolean fired;

    public AutoJoinPolicy(Settings settings) {
        this.settings = settings;
    }

    /** @return true exactly when the command should be sent now */
    public boolean shouldFire(long idleSeconds, boolean eligible) {
        if (!settings.autoAfkEnabled || !eligible || idleSeconds < settings.autoAfkAfterSeconds) {
            fired = false;
            return false;
        }
        if (fired) {
            return false;
        }
        fired = true;
        return true;
    }
}
