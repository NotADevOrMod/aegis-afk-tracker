package io.aegis.afkwatch.mc;

import com.mojang.blaze3d.platform.InputConstants;
import io.aegis.afkwatch.AfkWatchClient;
import io.aegis.afkwatch.Context;
import io.aegis.afkwatch.core.View;
import io.aegis.afkwatch.ui.AnalysisScreen;
import net.fabricmc.fabric.api.client.event.lifecycle.v1.ClientTickEvents;
import net.fabricmc.fabric.api.client.keymapping.v1.KeyMappingHelper;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayConnectionEvents;
import net.fabricmc.loader.api.FabricLoader;
import net.minecraft.client.KeyMapping;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import org.lwjgl.glfw.GLFW;

import java.time.LocalDate;

/** The single entry point from the Minecraft side. Each step is guarded so one failure cannot hide the rest. */
public final class GameBridge {
    private static final long AUTOSAVE_MS = 30_000;
    private static final int MAX_TICK_ERRORS = 5;

    private GameBridge() {}

    public static void install(Context ctx) {
        var diag = ctx.diag();

        try {
            ConnectionHooks.register(ctx.session(), diag);
            diag.log("Connection hooks registered.");
        } catch (Throwable t) {
            diag.error("ConnectionHooks.register", t);
        }

        try {
            HudOverlay.register(ctx);
            diag.log("HUD element registered (hudEnabled=" + ctx.settings().hudEnabled + ").");
        } catch (Throwable t) {
            diag.error("HudOverlay.register", t);
        }

        KeyMapping openAnalysis = null;
        try {
            KeyMapping.Category category = KeyMapping.Category.register(
                    Identifier.fromNamespaceAndPath(AfkWatchClient.MOD_ID, "main"));
            openAnalysis = KeyMappingHelper.registerKeyMapping(new KeyMapping(
                    "key.aegis_afk.open_analysis",
                    InputConstants.Type.KEYSYM,
                    GLFW.GLFW_KEY_KP_9, // numpad 9: unlikely to clash with other mods
                    category));
            diag.log("Key mapping registered: key.aegis_afk.open_analysis (default Numpad 9).");
        } catch (Throwable t) {
            diag.error("key registration", t);
        }
        final KeyMapping openKey = openAnalysis;

        try {
            ClientPlayConnectionEvents.DISCONNECT.register((handler, client) -> ctx.store().saveAsync(ctx.lifetime()));
        } catch (Throwable t) {
            diag.error("disconnect save hook", t);
        }

        String version = FabricLoader.getInstance().getModContainer(AfkWatchClient.MOD_ID)
                .map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("?");

        try {
            InputProbe probe = new InputProbe(ctx.settings());
            long[] lastSave = {System.currentTimeMillis()};
            long[] noticedConnection = {ctx.session().joinCount()};
            int[] tickErrors = {0};
            boolean[] firstTick = {true};
            String[] lastMode = {""};

            ClientTickEvents.END_CLIENT_TICK.register(mc -> {
                try {
                    long now = System.currentTimeMillis();
                    if (firstTick[0]) {
                        firstTick[0] = false;
                        diag.log("First client tick: the mod is running.");
                    }

                    if (probe.sample(mc, now)) {
                        ctx.session().onActivity(now);
                    }
                    ctx.tracker().tick(now, LocalDate.now());

                    View view = ctx.session().view(now);
                    String mode = view.mode() + (view.severity() > 0 ? "/" + view.severity() : "");
                    if (!mode.equals(lastMode[0])) {
                        lastMode[0] = mode;
                        diag.log("Indicator mode -> " + mode + " (idle " + ctx.session().idleSeconds(now) + "s)");
                    }

                    if (ctx.settings().joinNotice && mc.player != null
                            && ctx.session().joinCount() != noticedConnection[0]) {
                        noticedConnection[0] = ctx.session().joinCount();
                        String host = ctx.session().host();
                        if (!host.isEmpty()) {
                            String keyName = openKey == null ? "(key not registered)"
                                    : openKey.getTranslatedKeyMessage().getString();
                            String message = ctx.session().isTracking()
                                    ? "[AFK Watch " + version + "] Tracking " + host + ". Analysis: " + keyName
                                    : "[AFK Watch " + version + "] NOT tracking this server (address: " + host
                                            + "). Add it to serverHostFragments in aegis_afk.json.";
                            mc.player.sendSystemMessage(Component.literal(message));
                            diag.log("Join notice sent: " + message);
                        }
                    }

                    while (openKey != null && openKey.consumeClick()) {
                        diag.log("Analysis key pressed (player=" + (mc.player != null) + ", screenOpen="
                                + (mc.gui.screen() != null) + ").");
                        if (mc.player != null && mc.gui.screen() == null) {
                            mc.gui.setScreen(new AnalysisScreen(ctx, null));
                        }
                    }

                    if (now - lastSave[0] >= AUTOSAVE_MS) {
                        lastSave[0] = now;
                        ctx.store().saveAsync(ctx.lifetime());
                    }
                } catch (Throwable t) {
                    if (tickErrors[0]++ < MAX_TICK_ERRORS) {
                        diag.error("client tick", t);
                    }
                }
            });
            diag.log("Tick handler registered.");
        } catch (Throwable t) {
            diag.error("tick registration", t);
        }
    }
}
