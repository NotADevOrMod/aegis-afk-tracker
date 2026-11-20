package io.aegis.afkwatch.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

/** User-editable settings, stored as JSON in the Fabric config directory. */
public final class Settings {

    /** A server counts as BendersMC if its address contains any of these (case-insensitive). */
    public List<String> serverHostFragments = new ArrayList<>(List.of("benders"));
    /** Server names (from the "Sending you to ..." line) that count as the AFK server. */
    public List<String> afkServerNames = new ArrayList<>(List.of("afk"));
    /** Idle seconds at which each warning level begins. The first entry is when warnings start. */
    public List<Integer> warningStepsSeconds = new ArrayList<>(List.of(120, 180, 300));
    /** Idle seconds before the AFK server turns the indicator blue. */
    public int afkServerAfterSeconds = 15;
    /** How long an open menu or chat screen counts as activity. */
    public int screenGraceSeconds = 60;
    /** How long a defocused game window still counts as activity (quick alt-tab). */
    public int unfocusedGraceSeconds = 15;
    /** How long the "Disconnected" state stays up after a drop. */
    public int disconnectLingerSeconds = 600;

    public boolean hudEnabled = true;
    public boolean hudShowWhenActive = false;

    public boolean windowEnabled = true;
    public boolean windowShowWhenActive = true;
    public boolean windowAlwaysOnTop = true;
    public float windowOpacity = 0.92f;
    public int windowX = -1;
    public int windowY = -1;

    public boolean beepOnWarning = false;

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private transient Path file;
    private transient String loadProblem;

    public static Settings load(Path file) {
        Settings s = new Settings();
        if (Files.exists(file)) {
            try {
                Settings read = GSON.fromJson(Files.readString(file), Settings.class);
                if (read != null) {
                    s = read;
                }
            } catch (IOException | JsonParseException e) {
                s = new Settings();
                s.loadProblem = "Could not read " + file.getFileName() + ", using defaults: " + e.getMessage();
                s.file = file;
                s.normalize();
                return s; // leave the broken file alone so it can be fixed by hand
            }
        }
        s.file = file;
        s.normalize();
        s.save();
        return s;
    }

    public synchronized void save() {
        if (file == null) {
            return;
        }
        try {
            Files.createDirectories(file.getParent());
            Files.writeString(file, GSON.toJson(this));
        } catch (IOException ignored) {
            // Settings are a convenience; failing to persist must never affect the game.
        }
    }

    public String loadProblem() {
        return loadProblem;
    }

    public boolean matchesHost(String host) {
        String h = host.toLowerCase(Locale.ROOT);
        return serverHostFragments.stream().anyMatch(f -> !f.isBlank() && h.contains(f.toLowerCase(Locale.ROOT)));
    }

    public boolean isAfkServer(String name) {
        return afkServerNames.stream().anyMatch(n -> n.equalsIgnoreCase(name.trim()));
    }

    private void normalize() {
        if (serverHostFragments == null) serverHostFragments = new ArrayList<>(List.of("benders"));
        if (afkServerNames == null) afkServerNames = new ArrayList<>(List.of("afk"));
        if (warningStepsSeconds == null) warningStepsSeconds = new ArrayList<>();
        warningStepsSeconds = new ArrayList<>(warningStepsSeconds.stream()
                .filter(v -> v != null && v >= 10).distinct().sorted().toList());
        if (warningStepsSeconds.isEmpty()) warningStepsSeconds = new ArrayList<>(List.of(120, 180, 300));
        afkServerAfterSeconds = Math.max(5, afkServerAfterSeconds);
        screenGraceSeconds = Math.max(0, screenGraceSeconds);
        unfocusedGraceSeconds = Math.max(0, unfocusedGraceSeconds);
        disconnectLingerSeconds = Math.max(30, disconnectLingerSeconds);
        windowOpacity = Math.max(0.3f, Math.min(1f, windowOpacity));
    }
}
