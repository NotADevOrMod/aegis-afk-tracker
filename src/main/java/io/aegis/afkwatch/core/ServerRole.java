package io.aegis.afkwatch.core;

/** What a gamemode name means to the mod. */
public enum ServerRole {
    /** Track only these gamemodes (when any are listed). */
    INCLUDE("Include"),
    /** Ignore this gamemode completely: no pill, no alerts, no statistics. */
    EXCLUDE("Exclude"),
    /** This is the AFK gamemode: calm blue pill, never counted in the analysis. */
    AFK("AFK");

    private final String label;

    ServerRole(String label) {
        this.label = label;
    }

    public String label() {
        return label;
    }

    public ServerRole next() {
        ServerRole[] all = values();
        return all[(ordinal() + 1) % all.length];
    }
}
