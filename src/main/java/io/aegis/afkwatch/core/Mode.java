package io.aegis.afkwatch.core;

/** What the indicator should currently communicate. */
public enum Mode {
    /** Not on a tracked server, or nothing worth showing. */
    INACTIVE,
    /** On a tracked server and recently active. */
    ACTIVE,
    /** Idle outside the AFK server. Escalates through {@link View#severity()}. */
    WARNING,
    /** Idle inside the AFK server. Expected, so shown calmly. */
    AFK_SERVER,
    /** The connection dropped while the player was idle. */
    DISCONNECTED
}
