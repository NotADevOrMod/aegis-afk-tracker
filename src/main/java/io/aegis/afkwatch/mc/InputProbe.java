package io.aegis.afkwatch.mc;

import io.aegis.afkwatch.core.Settings;
import net.minecraft.client.Minecraft;

/**
 * Decides, once per client tick, whether the player did anything. Read-only:
 * it polls state the game already tracks and never sends input of any kind.
 */
final class InputProbe {

    private static final float ROTATION_EPSILON = 0.01f;

    private final Settings settings;

    private double lastMouseX = Double.NaN;
    private double lastMouseY;
    private boolean rotationKnown;
    private float lastYaw;
    private float lastPitch;
    private Object lastScreen;
    private long screenSince;
    private long unfocusedSince;

    InputProbe(Settings settings) {
        this.settings = settings;
    }

    boolean sample(Minecraft mc, long now) {
        boolean active = false;

        var o = mc.options;
        if (o.keyUp.isDown() || o.keyDown.isDown() || o.keyLeft.isDown() || o.keyRight.isDown()
                || o.keyJump.isDown() || o.keyShift.isDown() || o.keySprint.isDown()
                || o.keyAttack.isDown() || o.keyUse.isDown()) {
            active = true;
        }

        var mouse = mc.mouseHandler;
        double mx = mouse.xpos();
        double my = mouse.ypos();
        if (!Double.isNaN(lastMouseX) && (mx != lastMouseX || my != lastMouseY)) {
            active = true;
        }
        lastMouseX = mx;
        lastMouseY = my;

        var player = mc.player;
        if (player != null) {
            float yaw = player.getYRot();
            float pitch = player.getXRot();
            if (rotationKnown && (Math.abs(yaw - lastYaw) > ROTATION_EPSILON
                    || Math.abs(pitch - lastPitch) > ROTATION_EPSILON)) {
                active = true;
            }
            lastYaw = yaw;
            lastPitch = pitch;
            rotationKnown = true;
        } else {
            rotationKnown = false;
        }

        // An open menu or chat counts for a limited time; typing itself is not observable here.
        Object screen = mc.gui.screen(); // 26.x moved the current screen from Minecraft to Gui
        if (screen != lastScreen) {
            lastScreen = screen;
            screenSince = now;
        }
        if (screen != null && now - screenSince < settings.screenGraceSeconds * 1000L) {
            active = true;
        }

        // A quick alt-tab is forgiven; a long one is not.
        if (!mc.isWindowActive()) {
            if (unfocusedSince == 0) {
                unfocusedSince = now;
            }
            if (now - unfocusedSince < settings.unfocusedGraceSeconds * 1000L) {
                active = true;
            }
        } else {
            unfocusedSince = 0;
        }

        return active;
    }
}
