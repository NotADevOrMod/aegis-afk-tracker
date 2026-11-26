package io.aegis.afkwatch.ui;

import io.aegis.afkwatch.Context;
import io.aegis.afkwatch.audio.CustomSound;
import io.aegis.afkwatch.core.Format;
import io.aegis.afkwatch.core.Settings;
import io.aegis.afkwatch.mc.AlertPlayer;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;

import java.awt.FileDialog;
import java.awt.Frame;
import java.awt.GraphicsEnvironment;
import java.nio.file.Path;

/** The idle alert, plus doors to the server lists, the pill position and resetting statistics. */
public final class SettingsScreen extends Screen {
    private static final int WIDTH = 240;
    private static final int ROW = 24;
    private static final int STEP_SECONDS = 15;
    private static final int[] REPEATS = {0, 30, 60, 120, 300};

    private final Context ctx;
    private final Screen parent;

    private Button alertToggle;
    private Button afterLabel;
    private Button repeatButton;
    private Button soundButton;
    private Button chatButton;
    private Button autoButton;
    private Button volumeButton;
    private String status = "";
    private int statusColor = 0xFF8C96A5;
    private Button resetButton;
    private boolean resetArmed;

    public SettingsScreen(Context ctx, Screen parent) {
        super(Component.literal("AFK Settings"));
        this.ctx = ctx;
        this.parent = parent;
    }

    private Settings s() {
        return ctx.settings();
    }

    private int left() {
        return (this.width - WIDTH) / 2;
    }

    private int top() {
        return Math.max(8, (this.height - 246) / 2);
    }

    @Override
    protected void init() {
        int left = left();
        int y = top() + 26;
        int half = (WIDTH - 4) / 2;

        alertToggle = add("", left, y, WIDTH, b -> {
            s().alertEnabled = !s().alertEnabled;
            changed();
        });
        y += ROW;

        add("-", left, y, 22, b -> {
            s().alertAfterSeconds = Math.max(10, s().alertAfterSeconds - STEP_SECONDS);
            changed();
        });
        afterLabel = add("", left + 24, y, WIDTH - 48, b -> { });
        afterLabel.active = false;
        add("+", left + WIDTH - 22, y, 22, b -> {
            s().alertAfterSeconds = Math.min(900, s().alertAfterSeconds + STEP_SECONDS);
            changed();
        });
        y += ROW;

        repeatButton = add("", left, y, WIDTH, b -> {
            int next = 0;
            for (int i = 0; i < REPEATS.length; i++) {
                if (REPEATS[i] == s().alertRepeatSeconds) {
                    next = REPEATS[(i + 1) % REPEATS.length];
                    break;
                }
            }
            s().alertRepeatSeconds = next;
            changed();
        });
        y += ROW;

        soundButton = add("", left, y, WIDTH - 64, b -> {
            s().alertSound = AlertPlayer.nextSound(s().alertSound, !s().alertSoundFile.isEmpty());
            AlertPlayer.playSound(this.minecraft, s());
            changed();
        });
        add("File...", left + WIDTH - 60, y, 60, b -> chooseFile());
        y += ROW;

        chatButton = add("", left, y, WIDTH, b -> {
            s().alertChat = !s().alertChat;
            changed();
        });
        y += ROW;

        add("Test alert", left, y, half, b -> AlertPlayer.fire(this.minecraft, s(), s().alertAfterSeconds));
        volumeButton = add("", left + half + 4, y, half, b -> {
            s().alertVolume = s().alertVolume <= 25 ? 50 : s().alertVolume <= 50 ? 75 : s().alertVolume <= 75 ? 100 : 25;
            changed();
        });
        y += ROW;

        add("-", left, y, 22, b -> {
            s().autoAfkAfterSeconds = Math.max(60, s().autoAfkAfterSeconds - 60);
            changed();
        });
        autoButton = add("", left + 24, y, WIDTH - 48, b -> {
            s().autoAfkEnabled = !s().autoAfkEnabled;
            changed();
        });
        add("+", left + WIDTH - 22, y, 22, b -> {
            s().autoAfkAfterSeconds = Math.min(3600, s().autoAfkAfterSeconds + 60);
            changed();
        });
        y += ROW + 4;

        add("Servers...", left, y, half, b -> this.minecraft.gui.setScreen(new ServersScreen(ctx, this)));
        add("Move pill", left + half + 4, y, half, b -> this.minecraft.gui.setScreen(new PositionScreen(s(), this)));
        y += ROW;

        resetButton = add("Reset stats", left, y, half, b -> onResetPressed());
        add("Done", left + half + 4, y, half, b -> onClose());

        refreshLabels();
    }

    private Button add(String label, int x, int y, int w, Button.OnPress action) {
        Button b = Button.builder(Component.literal(label), action).bounds(x, y, w, 20).build();
        this.addRenderableWidget(b);
        return b;
    }

    private void changed() {
        s().save();
        refreshLabels();
    }

    private void refreshLabels() {
        Settings s = s();
        alertToggle.setMessage(Component.literal("Idle alert: " + (s.alertEnabled ? "ON" : "OFF")));
        afterLabel.setMessage(Component.literal("Alert after: " + Format.clock(s.alertAfterSeconds) + " idle"));
        repeatButton.setMessage(Component.literal(s.alertRepeatSeconds == 0
                ? "Repeat: once" : "Repeat: every " + Format.clock(s.alertRepeatSeconds)));
        soundButton.setMessage(Component.literal("Sound: " + AlertPlayer.soundLabel(s)));
        volumeButton.setMessage(Component.literal("File volume: " + s.alertVolume + "%"));
        autoButton.setMessage(Component.literal("Auto-join AFK after " + Format.clock(s.autoAfkAfterSeconds)
                + ": " + (s.autoAfkEnabled ? "ON" : "OFF")));
        chatButton.setMessage(Component.literal("Chat message: " + (s.alertChat ? "ON" : "OFF")));
    }

    /** Opens the system file picker on its own thread, so the game keeps running while it is open. */
    private void chooseFile() {
        if (GraphicsEnvironment.isHeadless()) {
            setStatus("No file dialog here: set alertSoundFile in aegis_afk.json", false);
            return;
        }
        Thread thread = new Thread(() -> {
            FileDialog dialog = new FileDialog((Frame) null, "Choose an alert sound (WAV)", FileDialog.LOAD);
            dialog.setFile("*.wav");
            dialog.setVisible(true); // blocks this thread only
            String name = dialog.getFile();
            String folder = dialog.getDirectory();
            if (name != null && folder != null) {
                Path chosen = Path.of(folder, name);
                this.minecraft.execute(() -> useFile(chosen));
            }
        }, "aegis-afk-file-dialog");
        thread.setDaemon(true);
        thread.start();
    }

    private void useFile(Path chosen) {
        String problem = CustomSound.validate(chosen.toString());
        if (problem != null) {
            setStatus(problem, false);
            return;
        }
        s().alertSoundFile = chosen.toString();
        s().alertSound = "custom";
        changed();
        AlertPlayer.playSound(this.minecraft, s());
        setStatus("Using " + chosen.getFileName(), true);
    }

    private void setStatus(String message, boolean good) {
        status = message;
        statusColor = good ? 0xFF6BCB8B : 0xFFE8833A;
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

    @Override
    public void extractRenderState(GuiGraphicsExtractor graphics, int mouseX, int mouseY, float delta) {
        super.extractRenderState(graphics, mouseX, mouseY, delta);

        int left = left();
        int top = top();
        String title = "AFK SETTINGS";
        graphics.text(this.font, title, (this.width - this.font.width(title)) / 2, top + 4, 0xFFFFFFFF, true);
        graphics.fill(left, top + 16, left + WIDTH, top + 17, 0xFF5AA9E6);
        if (!status.isEmpty()) {
            graphics.text(this.font, status, (this.width - this.font.width(status)) / 2, top + 18, statusColor, false);
        }
    }

    @Override
    public void onClose() {
        ctx.settings().save();
        this.minecraft.gui.setScreen(parent);
    }
}
