package io.aegis.afkwatch.mc;

import io.aegis.afkwatch.Context;
import io.aegis.afkwatch.core.View;
import io.aegis.afkwatch.ui.Pill;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

/** Registers the status pill on the in-game HUD. Drawing itself lives in {@link Pill}. */
final class HudOverlay {
    private HudOverlay() {}

    static void register(Context ctx) {
        if (!ctx.settings().hudEnabled) {
            return;
        }
        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("aegis_afk", "status"),
                (graphics, deltaTracker) -> {
                    long now = System.currentTimeMillis();
                    View view = ctx.session().view(now);
                    if (!view.visibleOnHud(ctx.settings().hudShowWhenActive)) {
                        return;
                    }
                    Minecraft mc = Minecraft.getInstance();
                    int screenW = mc.getWindow().getGuiScaledWidth();
                    int screenH = mc.getWindow().getGuiScaledHeight();
                    int w = Pill.width(mc.font, view);
                    Pill.draw(graphics, mc.font, view,
                            Pill.x(ctx.settings(), screenW, w), Pill.y(ctx.settings(), screenH), now);
                });
    }
}
