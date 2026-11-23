package io.aegis.afkwatch.mc;

import io.aegis.afkwatch.Diagnostics;
import io.aegis.afkwatch.core.Session;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

/** Translates Fabric connection and chat events into {@link Session} calls. */
final class ConnectionHooks {
    private ConnectionHooks() {}

    static void register(Session session, Diagnostics diag) {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            var data = client.getCurrentServer();
            String host = data == null ? "" : data.ip;
            session.onJoin(host, System.currentTimeMillis());
            diag.log("JOIN event. host=\"" + host + "\" tracking=" + session.isTracking());
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> {
            session.onDisconnect(System.currentTimeMillis());
            diag.log("DISCONNECT event.");
        });

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) {
                String text = message.getString();
                session.onMessage(text, System.currentTimeMillis());
                if (text.toLowerCase().contains("sending you to")) {
                    diag.log("Server-switch line seen. gamemode=\"" + session.serverName() + "\" afkServer="
                            + session.inAfkServer());
                }
            }
        });
    }
}
