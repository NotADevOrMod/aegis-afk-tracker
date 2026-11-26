package io.aegis.afkwatch.ui;

import io.aegis.afkwatch.core.Mode;
import io.aegis.afkwatch.core.Settings;
import io.aegis.afkwatch.core.View;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

/**
 * Place the pill. Click it to pick it up, move the mouse, click again to drop it. The arrow buttons
 * nudge it by small steps. Everything is driven by button presses plus the mouse position the screen is
 * given each frame, so there are no mouse-event overrides to break between game versions.
 */
public final class PositionScreen extends Screen {
    private static final View PREVIEW = new View(Mode.WARNING, 2, 150, "");
    private static final float DEFAULT_X = 0.5f;
    private static final float DEFAULT_Y = 0.03f;
    private static final float NUDGE = 0.01f;

    private final Settings settings;
    private final Screen parent;

    private Button grab;
    private final Button[] controls = new Button[6];
    private boolean carrying;
    private int grabDx;
    private int grabDy;
    private int lastMouseX;
    private int lastMouseY;

    public PositionScreen(Settings settings, Screen parent) {
        super(Component.literal("Move pill"));
        this.settings = settings;
        this.parent = parent;
    }

    @Override
    protected void init() {
        int w = Pill.width(this.font, PREVIEW);
        grab = Button.builder(Component.empty(), b -> toggleCarry())
                .bounds(Pill.x(settings, this.width, w), Pill.y(settings, this.height), w, Pill.HEIGHT).build();
        this.addRenderableWidget(grab);

        int y = this.height - 30;
        int total = 92 + 4 + (20 + 2) * 4 + 2 + 60;
        int x = (this.width - total) / 2;

        controls[0] = add("Reset", x, y, 92, b -> {
            settings.hudX = DEFAULT_X;
            settings.hudY = DEFAULT_Y;
            settings.save();
        });
        x += 92 + 4;
        controls[1] = add("<", x, y, 20, b -> nudge(-NUDGE, 0));
        x += 22;
        controls[2] = add("^", x, y, 20, b -> nudge(0, -NUDGE));
        x += 22;
        controls[3] = add("v", x, y, 20, b -> nudge(0, NUDGE));
        x += 22;
        controls[4] = add(">", x, y, 20, b -> nudge(NUDGE, 0));
        x += 22 + 2;
        controls[5] = add("Done", x, y, 60, b -> onClose());
    }

    private Button add(String label, int x, int y, int w, Button.OnPress action) {
        Button b = Button.builder(Component.literal(label), action).bounds(x, y, w, 20).build();
        this.addRenderableWidget(b);
        return b;
    }

    private void toggleCarry() {
        carrying = !carrying;
        if (carrying) {
            grabDx = lastMouseX - grab.getX();
            grabDy = lastMouseY - grab.getY();
        } else {
            settings.save();
        }
        for (Button b : controls) {
            b.active = !carrying; // so the drop click can never land on another button
        }
    }

    private void nudge(float dx, float dy) {
        settings.hudX = Math.max(0f, Math.min(1f, settings.hudX + dx));
        settings.hudY = Math.max(0f, Math.min(1f, settings.hudY + dy));
        settings.save();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        lastMouseX = mouseX;
        lastMouseY = mouseY;
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        int w = Pill.width(this.font, PREVIEW);
        int x;
        int y;
        if (carrying) {
            x = Math.max(0, Math.min(this.width - w, mouseX - grabDx));
            y = Math.max(0, Math.min(this.height - Pill.HEIGHT, mouseY - grabDy));
            settings.hudX = (x + w / 2f) / this.width;
            settings.hudY = y / (float) this.height;
        } else {
            x = Pill.x(settings, this.width, w);
            y = Pill.y(settings, this.height);
        }
        grab.setX(x); // keep the click target under the pill
        grab.setY(y);

        Pill.drawSolid(graphics, this.font, PREVIEW, x, y, System.currentTimeMillis());

        String hint = carrying ? "Click again to drop it here" : "Click the pill to pick it up, or use the arrows";
        graphics.text(this.font, hint, (this.width - this.font.width(hint)) / 2, this.height - 46, 0xFFB8C0CC, true);
    }

    @Override
    public void onClose() {
        settings.save();
        this.minecraft.gui.setScreen(parent);
    }
}
