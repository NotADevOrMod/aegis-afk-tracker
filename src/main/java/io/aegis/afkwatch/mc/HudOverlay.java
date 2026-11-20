package io.aegis.afkwatch.mc;

import io.aegis.afkwatch.core.Palette;
import io.aegis.afkwatch.core.Session;
import io.aegis.afkwatch.core.Settings;
import io.aegis.afkwatch.core.View;
import net.fabricmc.fabric.api.client.rendering.v1.hud.HudElementRegistry;
import net.fabricmc.fabric.api.client.rendering.v1.hud.VanillaHudElements;
import net.minecraft.client.Minecraft;
import net.minecraft.resources.Identifier;

/**
 * Small status pill at the top of the screen. All Minecraft drawing calls live
 * in this one method so a future rendering change touches a single place.
 */
final class HudOverlay {
    private static final int PAD_X = 8;
    private static final int HEIGHT = 26;
    private static final int TOP = 6;
    private static final int PANEL = 0xCC101418;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int SUBTEXT = 0xFFB8C0CC;

    private HudOverlay() {}

    static void register(Session session, Settings settings) {
        if (!settings.hudEnabled) {
            return;
        }
        HudElementRegistry.attachElementBefore(
                VanillaHudElements.CHAT,
                Identifier.fromNamespaceAndPath("aegis_afk", "status"),
                (graphics, deltaTracker) -> {
                    long now = System.currentTimeMillis();
                    View view = session.view(now);
                    if (!view.visibleOnHud(settings.hudShowWhenActive)) {
                        return;
                    }

                    Minecraft mc = Minecraft.getInstance();
                    var font = mc.font;
                    String headline = view.headline();
                    String detail = view.detail();

                    int width = Math.max(font.width(headline), font.width(detail)) + PAD_X * 2 + 4;
                    int x = (mc.getWindow().getGuiScaledWidth() - width) / 2;
                    int y = TOP;
                    int accent = Palette.withAlpha(Palette.accent(view), Palette.pulse(view, now));

                    graphics.fill(x - 1, y - 1, x + width + 1, y + HEIGHT + 1, accent);
                    graphics.fill(x, y, x + width, y + HEIGHT, PANEL);
                    graphics.fill(x, y, x + 3, y + HEIGHT, accent);
                    graphics.text(font, headline, x + PAD_X + 2, y + 4, TEXT, true);
                    graphics.text(font, detail, x + PAD_X + 2, y + 14, SUBTEXT, false);
                });
    }
}
