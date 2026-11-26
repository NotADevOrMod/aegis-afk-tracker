package io.aegis.afkwatch.ui;

import io.aegis.afkwatch.Context;
import io.aegis.afkwatch.core.ServerRole;
import io.aegis.afkwatch.core.Settings;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.util.List;

/**
 * Gamemode names and what they mean: Include (track only these), Exclude (ignore completely) or AFK
 * (the AFK gamemode). Names must match the network's "Sending you to X now..." line.
 */
public final class ServersScreen extends Screen {
    private static final int WIDTH = 280;
    private static final int ROWS = 5;
    private static final int ROW_H = 22;

    private final Context ctx;
    private final Screen parent;

    private EditBox nameBox;
    private String typed;
    private int page;

    public ServersScreen(Context ctx, Screen parent) {
        super(Component.literal("Servers"));
        this.ctx = ctx;
        this.parent = parent;
        String current = ctx.session().serverName();
        boolean alreadyListed = ctx.settings().serverEntries().stream()
                .anyMatch(e -> e.name().equalsIgnoreCase(current));
        this.typed = alreadyListed ? "" : current; // offer the gamemode you are in right now
    }

    private Settings s() {
        return ctx.settings();
    }

    private int left() {
        return (this.width - WIDTH) / 2;
    }

    private int top() {
        return Math.max(8, (this.height - 230) / 2);
    }

    @Override
    protected void init() {
        int left = left();
        int top = top();

        nameBox = new EditBox(this.font, left, top + 28, WIDTH, 20, Component.literal("Gamemode name"));
        nameBox.setMaxLength(32);
        nameBox.setHint(Component.literal("Gamemode name, e.g. Skyblock"));
        nameBox.setValue(typed);
        this.addRenderableWidget(nameBox);
        this.setInitialFocus(nameBox);

        int third = (WIDTH - 8) / 3;
        int y = top + 52;
        add("Include", left, y, third, b -> addTyped(ServerRole.INCLUDE));
        add("Exclude", left + third + 4, y, third, b -> addTyped(ServerRole.EXCLUDE));
        add("AFK", left + (third + 4) * 2, y, third, b -> addTyped(ServerRole.AFK));

        List<Settings.ServerEntry> entries = s().serverEntries();
        int pages = Math.max(1, (entries.size() + ROWS - 1) / ROWS);
        page = Math.max(0, Math.min(page, pages - 1));

        int rowY = top + 80;
        for (int i = page * ROWS; i < Math.min(entries.size(), (page + 1) * ROWS); i++) {
            Settings.ServerEntry entry = entries.get(i);
            add(entry.role().label(), left + WIDTH - 20 - 4 - 64, rowY, 64, b -> {
                s().setServerRole(entry.name(), entry.role().next());
                s().save();
                rebuild();
            });
            add("x", left + WIDTH - 20, rowY, 20, b -> {
                s().removeServer(entry.name());
                s().save();
                rebuild();
            });
            rowY += ROW_H;
        }

        int by = top + 80 + ROWS * ROW_H + 6;
        Button prev = add("Prev", left, by, 60, b -> {
            page--;
            rebuild();
        });
        prev.active = page > 0;
        add("Done", left + (WIDTH - 80) / 2, by, 80, b -> onClose());
        Button next = add("Next", left + WIDTH - 60, by, 60, b -> {
            page++;
            rebuild();
        });
        next.active = page < pages - 1;
    }

    private Button add(String label, int x, int y, int w, Button.OnPress action) {
        Button b = Button.builder(Component.literal(label), action).bounds(x, y, w, 20).build();
        this.addRenderableWidget(b);
        return b;
    }

    private void addTyped(ServerRole role) {
        String name = nameBox.getValue().trim();
        if (name.isEmpty()) {
            return;
        }
        s().setServerRole(name, role);
        s().save();
        typed = "";
        rebuild();
    }

    private void rebuild() {
        typed = nameBox.getValue();
        this.clearWidgets();
        this.init();
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        int left = left();
        int top = top();
        String title = "SERVERS";
        graphics.text(this.font, title, (this.width - this.font.width(title)) / 2, top + 4, 0xFFFFFFFF, true);
        graphics.fill(left, top + 16, left + WIDTH, top + 17, 0xFF5AA9E6);
        graphics.text(this.font, "Names as in \"Sending you to X now...\"", left, top + 18 + 2, 0xFF8C96A5, false);

        List<Settings.ServerEntry> entries = s().serverEntries();
        int rowY = top + 80;
        for (int i = page * ROWS; i < Math.min(entries.size(), (page + 1) * ROWS); i++) {
            graphics.text(this.font, entries.get(i).name(), left + 2, rowY + 6, 0xFFFFFFFF, false);
            rowY += ROW_H;
        }
        if (entries.isEmpty()) {
            String none = "No gamemodes listed. Everything except the AFK server is tracked.";
            graphics.text(this.font, none, left + 2, top + 86, 0xFF8C96A5, false);
        }

        String legend = "Include: track only these   Exclude: ignore   AFK: the AFK server";
        graphics.text(this.font, legend, left, top + 80 + ROWS * ROW_H + 32, 0xFF8C96A5, false);
    }

    @Override
    public void onClose() {
        s().save();
        this.minecraft.gui.setScreen(parent);
    }
}
