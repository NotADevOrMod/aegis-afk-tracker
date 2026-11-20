package io.aegis.afkwatch.core;

/**
 * Immutable snapshot of what to display. Produced by {@link Session#view(long)}
 * and consumed by both the in-game HUD and the desktop window.
 *
 * @param seconds idle seconds, or for {@link Mode#DISCONNECTED} the idle time at the moment of the drop
 */
public record View(Mode mode, int severity, long seconds, String server) {

    public static final View INACTIVE = new View(Mode.INACTIVE, 0, 0, "");

    public boolean visibleOnHud(boolean showWhenActive) {
        return switch (mode) {
            case WARNING, AFK_SERVER -> true;
            case ACTIVE -> showWhenActive;
            case INACTIVE, DISCONNECTED -> false; // no world to draw on after a drop
        };
    }

    public String headline() {
        String clock = Format.clock(seconds);
        return switch (mode) {
            case ACTIVE -> "Active";
            case WARNING -> switch (severity) {
                case 1 -> "Idle · " + clock;
                case 2 -> "AFK warning · " + clock;
                default -> "Still idle · " + clock;
            };
            case AFK_SERVER -> "AFK · " + clock;
            case DISCONNECTED -> "Disconnected";
            case INACTIVE -> "";
        };
    }

    public String detail() {
        return switch (mode) {
            case ACTIVE -> "Idle " + Format.clock(seconds);
            case WARNING -> switch (severity) {
                case 1 -> "Not in the AFK server";
                case 2 -> "Move, or head to the AFK server";
                default -> "Idle for a while: join the AFK server";
            };
            case AFK_SERVER -> server.isEmpty() ? "In the AFK server" : "In " + server;
            case DISCONNECTED -> "Lost while idle · " + Format.clock(seconds);
            case INACTIVE -> "";
        };
    }
}
