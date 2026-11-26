package io.aegis.afkwatch.mc;

import io.aegis.afkwatch.audio.CustomSound;
import io.aegis.afkwatch.core.Format;
import io.aegis.afkwatch.core.Settings;
import net.minecraft.client.Minecraft;
import net.minecraft.client.resources.sounds.SimpleSoundInstance;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;

/** Performs the idle alert: a sound and/or a chat line. All game calls for alerts live here. */
public final class AlertPlayer {
    private AlertPlayer() {}

    public static void fire(Minecraft mc, Settings settings, long idleSeconds) {
        playSound(mc, settings);
        if (settings.alertChat && mc.player != null) {
            mc.player.sendSystemMessage(Component.literal(
                    "[AFK Watch] Idle for " + Format.clock(idleSeconds) + ". Move to stay active."));
        }
    }

    public static void playSound(Minecraft mc, Settings settings) {
        String key = settings.alertSound;
        if ("custom".equals(key)) {
            if (CustomSound.playIfValid(settings.alertSoundFile, settings.alertVolume)) {
                return;
            }
            key = "xp"; // the file is missing or unusable: fall back rather than stay silent
        }
        switch (key) {
            case "xp" -> mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.EXPERIENCE_ORB_PICKUP, 1.0F));
            case "pling" -> mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_PLING, 1.0F));
            case "bell" -> mc.getSoundManager().play(SimpleSoundInstance.forUI(SoundEvents.NOTE_BLOCK_BELL, 1.0F));
            default -> { /* "none" */ }
        }
    }

    public static String soundLabel(Settings settings) {
        return switch (settings.alertSound) {
            case "xp" -> "XP orb";
            case "pling" -> "Pling";
            case "bell" -> "Bell";
            case "custom" -> {
                String name = java.nio.file.Path.of(settings.alertSoundFile.isEmpty() ? "?" : settings.alertSoundFile)
                        .getFileName().toString();
                yield "Custom: " + (name.length() > 18 ? name.substring(0, 17) + "..." : name);
            }
            default -> "None";
        };
    }

    /** Cycle order. "Custom" only appears once a file has been chosen. */
    public static String nextSound(String key, boolean hasFile) {
        return switch (key) {
            case "xp" -> "pling";
            case "pling" -> "bell";
            case "bell" -> hasFile ? "custom" : "none";
            case "custom" -> "none";
            default -> "xp";
        };
    }
}
