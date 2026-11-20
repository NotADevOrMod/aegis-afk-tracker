package io.aegis.afkwatch.mc;

import io.aegis.afkwatch.core.Session;
import io.aegis.afkwatch.core.Settings;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;

/** The single entry point from the Minecraft side. Everything else here is package-private. */
public final class GameBridge {
    private GameBridge() {}

    public static void install(Session session, Settings settings) {
        ConnectionHooks.register(session);
        HudOverlay.register(session, settings);

        InputProbe probe = new InputProbe(settings);
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            long now = System.currentTimeMillis();
            if (probe.sample(mc, now)) {
                session.onActivity(now);
            }
        });
    }
}
