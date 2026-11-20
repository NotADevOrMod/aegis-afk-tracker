package io.aegis.afkbridge;

import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AfkBridgeClient implements ClientModInitializer {
    private static final Logger LOG = LoggerFactory.getLogger("AEGIS AFK Bridge");

    @Override
    public void onInitializeClient() {
        BridgeState state = new BridgeState();
        BridgeWriter writer = new BridgeWriter(state, LOG);
        if (!writer.available()) {
            LOG.warn("APPDATA is not set; the AFK Watch app cannot be reached. Doing nothing.");
            return;
        }

        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            var data = client.getCurrentServer();
            state.onJoin(data == null ? "" : data.ip, System.currentTimeMillis());
            writer.flush();
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            state.onDisconnect();
            writer.flush();
        });

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) {
                state.onMessage(message.getString(), System.currentTimeMillis());
                writer.flush();
            }
        });

        InputProbe probe = new InputProbe();
        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            long now = System.currentTimeMillis();
            if (probe.sample(mc, now)) {
                state.markActive(now);
            }
        });

        writer.start();
        LOG.info("Publishing AFK state for the AFK Watch app.");
    }
}
