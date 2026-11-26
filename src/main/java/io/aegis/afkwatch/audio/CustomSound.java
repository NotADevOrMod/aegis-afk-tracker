package io.aegis.afkwatch.audio;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sound.sampled.AudioFileFormat;
import javax.sound.sampled.AudioInputStream;
import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Clip;
import javax.sound.sampled.FloatControl;
import javax.sound.sampled.LineEvent;
import javax.sound.sampled.UnsupportedAudioFileException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * Plays the player's own alert sound with Java's built-in audio, independent of the game's sound system
 * (which only plays sounds registered through resource packs). Java reads uncompressed WAV, AIFF and AU;
 * MP3 and OGG need converting first.
 */
public final class CustomSound {
    private static final Logger LOG = LoggerFactory.getLogger("AEGIS AFK Watch");
    private static final long MAX_BYTES = 10L * 1024 * 1024;
    private static final double MAX_SECONDS = 30.0;

    private CustomSound() {}

    /** @return null if the file can be played, otherwise a short reason suitable for showing to the player */
    public static String validate(String path) {
        if (path == null || path.isBlank()) {
            return "No file chosen";
        }
        Path file;
        try {
            file = Path.of(path);
        } catch (RuntimeException e) {
            return "Not a valid path";
        }
        if (!Files.isRegularFile(file)) {
            return "File not found";
        }
        try {
            if (Files.size(file) > MAX_BYTES) {
                return "File is over 10 MB";
            }
            AudioFileFormat format = AudioSystem.getAudioFileFormat(file.toFile());
            float rate = format.getFormat().getFrameRate();
            if (format.getFrameLength() > 0 && rate > 0 && format.getFrameLength() / rate > MAX_SECONDS) {
                return "Longer than 30 seconds";
            }
            return null;
        } catch (UnsupportedAudioFileException e) {
            return "Unsupported format: use an uncompressed WAV (convert MP3/OGG first)";
        } catch (IOException e) {
            return "Could not read the file";
        }
    }

    /** Plays the file in the background. Returns false, playing nothing, if it cannot be played. */
    public static boolean playIfValid(String path, int volumePercent) {
        if (validate(path) != null) {
            return false;
        }
        Path file = Path.of(path);
        Thread thread = new Thread(() -> play(file, volumePercent), "aegis-afk-sound");
        thread.setDaemon(true);
        thread.start();
        return true;
    }

    private static void play(Path file, int volumePercent) {
        try (AudioInputStream stream = AudioSystem.getAudioInputStream(file.toFile())) {
            Clip clip = AudioSystem.getClip();
            clip.open(stream);
            applyVolume(clip, volumePercent);
            clip.addLineListener(event -> {
                if (event.getType() == LineEvent.Type.STOP) {
                    clip.close();
                }
            });
            clip.start();
        } catch (Exception e) { // no audio device, unsupported encoding inside a WAV, and so on
            LOG.warn("Could not play {}: {}", file.getFileName(), e.toString());
        }
    }

    private static void applyVolume(Clip clip, int volumePercent) {
        if (!clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
            return;
        }
        FloatControl gain = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
        double linear = Math.max(0.01, Math.min(1.0, volumePercent / 100.0));
        float decibels = (float) (20.0 * Math.log10(linear));
        gain.setValue(Math.max(gain.getMinimum(), Math.min(gain.getMaximum(), decibels)));
    }
}
