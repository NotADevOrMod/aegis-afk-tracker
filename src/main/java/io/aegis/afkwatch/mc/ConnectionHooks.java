package io.aegis.afkwatch.mc;

import io.aegis.afkwatch.core.Session;
import net.fabricmc.fabric.api.client.message.v1.ClientReceiveMessageEvents;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;

/** Translates Fabric connection and chat events into {@link Session} calls. */
final class ConnectionHooks {
    private ConnectionHooks() {}

    static void register(Session session) {
        ClientPlayConnectionEvents.JOIN.register((handler, sender, client) -> {
            var data = client.getCurrentServer();
            session.onJoin(data == null ? "" : data.ip, System.currentTimeMillis());
        });

        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) ->
                session.onDisconnect(System.currentTimeMillis()));

        ClientReceiveMessageEvents.GAME.register((message, overlay) -> {
            if (!overlay) {
                session.onMessage(message.getString(), System.currentTimeMillis());
            }
        });
    }
}
