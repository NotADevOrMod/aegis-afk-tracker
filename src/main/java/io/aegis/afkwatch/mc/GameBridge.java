package io.aegis.afkwatch.mc;

import com.mojang.blaze3d.platform.InputConstants;
import io.aegis.afkwatch.AfkWatchClient;
import io.aegis.afkwatch.Context;
import io.aegis.afkwatch.ui.AnalysisScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.minecraft.client.KeyMapping;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.time.LocalDate;

/** The single entry point from the Minecraft side. */
public final class GameBridge {
    private static final long AUTOSAVE_MS = 30_000;

    private GameBridge() {}

    public static void install(Context ctx) {
        ConnectionHooks.register(ctx.session());
        HudOverlay.register(ctx);

        KeyMapping.Category category = KeyMapping.Category.register(
                Identifier.fromNamespaceAndPath(AfkWatchClient.MOD_ID, "main"));
        KeyMapping openAnalysis = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                "key.aegis_afk.open_analysis",
                InputConstants.Type.KEYSYM,
                GLFW.GLFW_KEY_KP_9, // numpad 9: unlikely to clash with other mods
                category));

        // Make sure a session's numbers reach disk when the player leaves a server.
        ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ctx.store().saveAsync(ctx.lifetime()));

        InputProbe probe = new InputProbe(ctx.settings());
        long[] lastSave = {System.currentTimeMillis()};
        long[] noticedConnection = {ctx.session().joinCount()};
        String version = FabricLoader.getInstance().getModContainer(AfkWatchClient.MOD_ID)
                .map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("?");

        ClientTickEvents.END_CLIENT_TICK.register(mc -> {
            long now = System.currentTimeMillis();

            if (probe.sample(mc, now)) {
                ctx.session().onActivity(now);
            }
            ctx.tracker().tick(now, LocalDate.now());

            if (ctx.settings().joinNotice && mc.player != null && ctx.session().joinCount() != noticedConnection[0]) {
                noticedConnection[0] = ctx.session().joinCount();
                String host = ctx.session().host();
                if (!host.isEmpty()) {
                    String message = ctx.session().isTracking()
                            ? "[AFK Watch " + version + "] Tracking " + host + ". Analysis: "
                                    + openAnalysis.getTranslatedKeyMessage().getString()
                            : "[AFK Watch " + version + "] NOT tracking this server (address: " + host
                                    + "). Add it to serverHostFragments in aegis_afk.json.";
                    mc.player.sendSystemMessage(Component.literal(message));
                }
            }

            while (openAnalysis.consumeClick()) {
                if (mc.player != null && mc.gui.screen() == null) {
                    mc.gui.setScreen(new AnalysisScreen(ctx, null));
                }
            }

            if (now - lastSave[0] >= AUTOSAVE_MS) {
                lastSave[0] = now;
                ctx.store().saveAsync(ctx.lifetime());
            }
        });
    }
}
