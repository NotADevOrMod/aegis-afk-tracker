package io.aegis.afkwatch.ui;

import io.aegis.afkwatch.core.Palette;
import io.aegis.afkwatch.core.Settings;
import io.aegis.afkwatch.core.View;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphicsExtractor;

/**
 * The status pill: size, placement and drawing in one place, shared by the HUD and the position editor.
 * Every GUI draw call for the pill is here, so a rendering API change touches a single file.
 */
public final class Pill {
    public static final int HEIGHT = 26;

    private static final int PAD_X = 8;
    private static final int PANEL = 0xCC101418;
    private static final int TEXT = 0xFFFFFFFF;
    private static final int SUBTEXT = 0xFFB8C0CC;

    private Pill() {}

    public static int width(Font font, View view) {
        return Math.max(font.width(view.headline()), font.width(view.detail())) + PAD_X * 2 + 4;
    }

    /** Left edge for a pill of width {@code w}; the saved position is the pill's horizontal centre. */
    public static int x(Settings settings, int screenW, int w) {
        return clamp(Math.round(settings.hudX * screenW - w / 2f), 0, Math.max(0, screenW - w));
    }

    public static int y(Settings settings, int screenH) {
        return clamp(Math.round(settings.hudY * screenH), 0, Math.max(0, screenH - HEIGHT));
    }

    public static void draw(GuiGraphicsExtractor graphics, Font font, View view, int x, int y, long nowMs) {
        int w = width(font, view);
        int accent = Palette.withAlpha(Palette.accent(view), Palette.pulse(view, nowMs));

        graphics.fill(x - 1, y - 1, x + w + 1, y + HEIGHT + 1, accent);
        graphics.fill(x, y, x + w, y + HEIGHT, PANEL);
        graphics.fill(x, y, x + 3, y + HEIGHT, accent);
        graphics.text(font, view.headline(), x + PAD_X + 2, y + 4, TEXT, true);
        graphics.text(font, view.detail(), x + PAD_X + 2, y + 14, SUBTEXT, false);
    }

    private static int clamp(int v, int lo, int hi) {
        return Math.max(lo, Math.min(hi, v));
    }
}
