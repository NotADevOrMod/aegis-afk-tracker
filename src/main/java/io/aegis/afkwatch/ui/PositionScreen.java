package io.aegis.afkwatch.ui;

import io.aegis.afkwatch.core.Mode;
import io.aegis.afkwatch.core.Settings;
import io.aegis.afkwatch.core.View;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Drag the pill to where you want it. Dragging is read from the mouse position and the left-button state
 * each frame, so this screen needs no mouse-event overrides.
 */
public final class PositionScreen extends Screen {
    private static final View PREVIEW = new View(Mode.WARNING, 2, 150, "");
    private static final float DEFAULT_X = 0.5f;
    private static final float DEFAULT_Y = 0.03f;

    private final Settings settings;
    private final Screen parent;

    private boolean dragging;
    private boolean wasDown;
    private int grabDx;
    private int grabDy;

    public PositionScreen(Settings settings, Screen parent) {
        super(Component.literal("Move pill"));
        this.settings = settings;
        this.parent = parent;
    }

    @Override
    protected void init() {
        int bw = 100;
        int y = this.height - 30;
        this.addRenderableWidget(Button.builder(Component.literal("Reset position"), b -> {
            settings.hudX = DEFAULT_X;
            settings.hudY = DEFAULT_Y;
            settings.save();
        }).bounds(this.width / 2 - bw - 4, y, bw, 20).build());
        this.addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(this.width / 2 + 4, y, bw, 20).build());
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        int w = Pill.width(this.font, PREVIEW);
        int x = Pill.x(settings, this.width, w);
        int y = Pill.y(settings, this.height);

        boolean down = this.minecraft.mouseHandler.isLeftPressed();
        boolean overPill = mouseX >= x && mouseX < x + w && mouseY >= y && mouseY < y + Pill.HEIGHT;

        if (down && !wasDown && overPill) {
            dragging = true;
            grabDx = mouseX - x;
            grabDy = mouseY - y;
        }
        if (!down && dragging) {
            dragging = false;
            settings.save();
        }
        wasDown = down;

        if (dragging) {
            x = Math.max(0, Math.min(this.width - w, mouseX - grabDx));
            y = Math.max(0, Math.min(this.height - Pill.HEIGHT, mouseY - grabDy));
            settings.hudX = (x + w / 2f) / this.width;
            settings.hudY = y / (float) this.height;
        }

        Pill.draw(graphics, this.font, PREVIEW, x, y, System.currentTimeMillis());

        String hint = "Drag the pill where you want it";
        graphics.text(this.font, hint, (this.width - this.font.width(hint)) / 2, this.height - 46, 0xFFB8C0CC, true);
    }

    @Override
    public void onClose() {
        settings.save();
        this.minecraft.gui.setScreen(parent);
    }
}
