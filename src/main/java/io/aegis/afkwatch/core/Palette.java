package io.aegis.afkwatch.core;

/** Shared colours so the HUD and the window always agree. All values are 0xRRGGBB. */
public final class Palette {
    private Palette() {}

    public static final int BLUE = 0x5AA9E6;     // calm: AFK in the AFK server
    public static final int ORANGE = 0xE8833A;   // first warning
    public static final int RED = 0xE5484D;      // second warning
    public static final int DEEP_RED = 0xC62A2F; // prolonged idle
    public static final int GREEN = 0x6BCB8B;    // active
    public static final int SLATE = 0x9AA3B2;    // connection lost

    public static int accent(View v) {
        return switch (v.mode()) {
            case AFK_SERVER -> BLUE;
            case WARNING -> switch (v.severity()) {
                case 1 -> ORANGE;
                case 2 -> RED;
                default -> DEEP_RED;
            };
            case ACTIVE -> GREEN;
            case DISCONNECTED -> SLATE;
            case INACTIVE -> 0x000000;
        };
    }

    /** 0.7..1.0 pulse for the louder warning levels, steady 1.0 otherwise. */
    public static double pulse(View v, long nowMillis) {
        if (v.mode() == Mode.WARNING && v.severity() >= 2) {
            return 0.85 + 0.15 * Math.sin(nowMillis / 300.0);
        }
        return 1.0;
    }

    public static int withAlpha(int rgb, double alpha) {
        int a = (int) Math.round(Math.max(0, Math.min(1, alpha)) * 255);
        return (a << 24) | (rgb & 0xFFFFFF);
    }
}
