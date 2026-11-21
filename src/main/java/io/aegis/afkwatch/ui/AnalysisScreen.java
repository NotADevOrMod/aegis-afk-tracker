package io.aegis.afkwatch.ui;

import io.aegis.afkwatch.Context;
import io.aegis.afkwatch.core.Format;
import io.aegis.afkwatch.stats.Stats;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.temporal.TemporalAdjusters;
import java.util.List;
import java.util.Map;

/**
 * Active play versus idle time, per gamemode. Time spent in the AFK gamemode is never counted, so this
 * only compares real playing against sitting idle elsewhere.
 */
public final class AnalysisScreen extends Screen {
    private static final String[] TABS = {"Session", "Today", "This week", "All time"};
    private static final int MAX_ROWS = 5;
    private static final LocalDate EPOCH = LocalDate.of(1970, 1, 1);

    private static final int BORDER = 0xFF5AA9E6;
    private static final int PANEL = 0xE0101418;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int MUTED = 0xFF8C96A5;
    private static final int GREEN = 0xFF6BCB8B;
    private static final int RED = 0xFFE5484D;
    private static final int TRACK = 0xFF2A323B;

    private final Context ctx;
    private final Screen parent;
    private final Button[] tabButtons = new Button[TABS.length];
    private Button resetButton;
    private boolean resetArmed;
    private int range = 1; // opens on "Today"

    public AnalysisScreen(Context ctx, Screen parent) {
        super(Component.literal("AFK Analysis"));
        this.ctx = ctx;
        this.parent = parent;
    }

    private int panelWidth() {
        return Math.min(360, this.width - 20);
    }

    private int panelHeight() {
        return 236;
    }

    private int left() {
        return (this.width - panelWidth()) / 2;
    }

    private int top() {
        return Math.max(10, (this.height - panelHeight()) / 2);
    }

    @Override
    protected void init() {
        int left = left();
        int top = top();
        int inner = panelWidth() - 24;

        int gap = 4;
        int tabW = (inner - gap * (TABS.length - 1)) / TABS.length;
        for (int i = 0; i < TABS.length; i++) {
            final int index = i;
            tabButtons[i] = Button.builder(Component.literal(TABS[i]), b -> range = index)
                    .bounds(left + 12 + i * (tabW + gap), top + 28, tabW, 20).build();
            this.addRenderableWidget(tabButtons[i]);
        }

        int bw = (inner - gap * 2) / 3;
        int by = top + panelHeight() - 28;
        this.addRenderableWidget(Button.builder(Component.literal("Move pill"),
                        b -> this.minecraft.gui.setScreen(new PositionScreen(ctx.settings(), this)))
                .bounds(left + 12, by, bw, 20).build());

        resetButton = Button.builder(Component.literal("Reset stats"), b -> onResetPressed())
                .bounds(left + 12 + bw + gap, by, bw, 20).build();
        this.addRenderableWidget(resetButton);

        this.addRenderableWidget(Button.builder(Component.literal("Done"), b -> onClose())
                .bounds(left + 12 + (bw + gap) * 2, by, bw, 20).build());
    }

    private void onResetPressed() {
        if (!resetArmed) {
            resetArmed = true;
            resetButton.setMessage(Component.literal("Click to confirm"));
            return;
        }
        ctx.lifetime().clear();
        ctx.thisSession().clear();
        ctx.store().saveAsync(ctx.lifetime());
        resetArmed = false;
        resetButton.setMessage(Component.literal("Reset stats"));
    }

    private Stats.Totals totals() {
        LocalDate today = LocalDate.now();
        return switch (range) {
            case 0 -> ctx.thisSession().totalsSince(EPOCH);
            case 1 -> ctx.lifetime().totalsSince(today);
            case 2 -> ctx.lifetime().totalsSince(today.with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY)));
            default -> ctx.lifetime().totalsSince(EPOCH);
        };
    }

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        for (int i = 0; i < tabButtons.length; i++) {
            tabButtons[i].active = i != range;
        }

        int left = left();
        int top = top();
        int right = left + panelWidth() - 14;
        int pw = panelWidth();

        drawPanel(graphics, left, top, pw);
        centered(graphics, "AFK ANALYSIS", left + pw / 2, top + 10, WHITE);

        Stats.Totals t = totals();
        long total = t.totalMs();
        int y = top + 58;

        row(graphics, "Active", Format.duration(t.activeMs()), left + 14, right, y, GREEN);
        row(graphics, "Idle (not in AFK server)", Format.duration(t.afkMs()), left + 14, right, y + 12, RED);
        row(graphics, "Idle share", Format.percent(t.afkMs(), total), left + 14, right, y + 24, WHITE);

        int barX = left + 14;
        int barY = y + 40;
        int barW = right - barX;
        graphics.fill(barX, barY, barX + barW, barY + 6, TRACK);
        if (total > 0) {
            int green = (int) Math.round(barW * (t.activeMs() / (double) total));
            graphics.fill(barX, barY, barX + green, barY + 6, GREEN);
            graphics.fill(barX + green, barY, barX + barW, barY + 6, RED);
        }

        int headY = y + 56;
        text(graphics, "GAMEMODE", left + 14, headY, MUTED);
        rightText(graphics, "Active", right - 100, headY, MUTED);
        rightText(graphics, "Idle", right - 50, headY, MUTED);
        rightText(graphics, "Idle %", right, headY, MUTED);

        List<Map.Entry<String, Stats.Bucket>> rows = t.byServer().entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue().activeMs + b.getValue().afkMs,
                        a.getValue().activeMs + a.getValue().afkMs))
                .limit(MAX_ROWS)
                .toList();

        if (rows.isEmpty()) {
            centered(graphics, "No data yet for this range.", left + pw / 2, headY + 24, MUTED);
        }
        int rowY = headY + 12;
        for (Map.Entry<String, Stats.Bucket> e : rows) {
            Stats.Bucket b = e.getValue();
            text(graphics, e.getKey(), left + 14, rowY, WHITE);
            rightText(graphics, Format.duration(b.activeMs), right - 100, rowY, GREEN);
            rightText(graphics, Format.duration(b.afkMs), right - 50, rowY, RED);
            rightText(graphics, Format.percent(b.afkMs, b.activeMs + b.afkMs), right, rowY, MUTED);
            rowY += 11;
        }

        var session = ctx.session();
        String status;
        int statusColor;
        if (session.isTracking()) {
            String mode = session.inAfkServer() ? "AFK server"
                    : session.serverName().isEmpty() ? "gamemode unknown" : session.serverName();
            status = "Tracking " + session.host() + " | " + mode + " | idle "
                    + Format.clock(session.idleSeconds(System.currentTimeMillis()));
            statusColor = GREEN;
        } else {
            status = session.host().isEmpty() ? "Not connected to a server"
                    : "Not tracking " + session.host() + " (see serverHostFragments)";
            statusColor = 0xFFE8833A;
        }
        centered(graphics, status, left + pw / 2, top + panelHeight() - 52, statusColor);

        centered(graphics, "Time in the AFK server is not counted.",
                left + pw / 2, top + panelHeight() - 42, MUTED);
    }

    /**
     * The widgets are drawn by the superclass before this runs, so the panel is filled in the gaps around
     * them (title, side margins, content area, bottom edge) rather than over them.
     */
    private void drawPanel(GuiGraphicsExtractor g, int left, int top, int pw) {
        int ph = panelHeight();
        int buttonsTop = top + ph - 28;

        g.fill(left - 1, top - 1, left + pw + 1, top, BORDER);
        g.fill(left - 1, top + ph, left + pw + 1, top + ph + 1, BORDER);
        g.fill(left - 1, top, left, top + ph, BORDER);
        g.fill(left + pw, top, left + pw + 1, top + ph, BORDER);

        g.fill(left, top, left + pw, top + 28, PANEL);                              // title
        g.fill(left, top + 28, left + 12, top + ph, PANEL);                         // left margin
        g.fill(left + pw - 12, top + 28, left + pw, top + ph, PANEL);               // right margin
        g.fill(left + 12, top + 48, left + pw - 12, buttonsTop, PANEL);             // content
        g.fill(left + 12, buttonsTop + 20, left + pw - 12, top + ph, PANEL);        // bottom edge
    }

    private void row(GuiGraphicsExtractor g, String label, String value, int leftX, int rightX, int y, int color) {
        text(g, label, leftX, y, color);
        rightText(g, value, rightX, y, color);
    }

    private void text(GuiGraphicsExtractor g, String s, int x, int y, int color) {
        g.text(this.font, s, x, y, color, false);
    }

    private void rightText(GuiGraphicsExtractor g, String s, int rightX, int y, int color) {
        g.text(this.font, s, rightX - this.font.width(s), y, color, false);
    }

    private void centered(GuiGraphicsExtractor g, String s, int centerX, int y, int color) {
        g.text(this.font, s, centerX - this.font.width(s) / 2, y, color, false);
    }

    @Override
    public void onClose() {
        this.minecraft.gui.setScreen(parent);
    }
}
