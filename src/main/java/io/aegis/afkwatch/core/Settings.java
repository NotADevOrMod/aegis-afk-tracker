package io.aegis.afkwatch.core;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.List;
import java.util.Locale;

/** User-editable settings, stored as JSON in the Fabric config directory. */
public final class Settings {

    /** A server counts as BendersMC if its address contains any of these (case-insensitive). */
    public List<String> serverHostFragments = new ArrayList<>(List.of("benders"));
    /** Server names (from the "Sending you to ..." line) that count as the AFK server. */
    public List<String> afkServerNames = new ArrayList<>(List.of("afk"));
    /** Gamemodes to track exclusively. Empty means "track everything that is not excluded". */
    public List<String> includedServers = new ArrayList<>();
    /** Gamemodes to ignore completely: no pill, no alerts, no statistics. */
    public List<String> excludedServers = new ArrayList<>();

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

    /** The idle alert: fires when idle this long in a tracked, non-AFK gamemode. */
    public boolean alertEnabled = false;
    public int alertAfterSeconds = 60;
    /** Seconds between repeats while still idle; 0 means alert once. */
    public int alertRepeatSeconds = 60;
    /** One of: xp, pling, bell, custom, none. */
    public String alertSound = "xp";
    /** Full path to your own alert sound (uncompressed WAV, up to 30 seconds), used when alertSound is "custom". */
    public String alertSoundFile = "";
    /** Volume of the custom sound, 0 to 100. The built-in sounds follow the game volume. */
    public int alertVolume = 100;
    public boolean alertChat = true;

    /** Join the AFK queue by itself after being idle this long in a tracked, non-AFK gamemode. */
    public boolean autoAfkEnabled = true;
    public int autoAfkAfterSeconds = 300;
    /** The command to send, without the leading slash. */
    public String autoAfkCommand = "joinq AFK";

    /** Print one chat line on joining a server saying whether it is being tracked. */
    public boolean joinNotice = true;

    public boolean hudEnabled = true;
    public boolean hudShowWhenActive = false;
    /** Pill centre as a fraction of the screen width, so it survives a GUI-scale change. */
    public float hudX = 0.5f;
    /** Pill top edge as a fraction of the screen height. */
    public float hudY = 0.03f;


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

    /** A gamemode and the role the player gave it. */
    public record ServerEntry(String name, ServerRole role) {}

    /** Every listed gamemode with its role, sorted by name. A name in several lists takes the strongest role. */
    public List<ServerEntry> serverEntries() {
        java.util.Map<String, ServerEntry> byName = new java.util.TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (ServerRole role : new ServerRole[] {ServerRole.INCLUDE, ServerRole.EXCLUDE, ServerRole.AFK}) {
            for (String name : listFor(role)) {
                byName.put(name, new ServerEntry(name, role)); // later roles (AFK) override earlier ones
            }
        }
        return new ArrayList<>(byName.values());
    }

    /** Gives a gamemode exactly one role, replacing any it had. */
    public void setServerRole(String name, ServerRole role) {
        String clean = name == null ? "" : name.trim();
        if (clean.isEmpty()) {
            return;
        }
        removeServer(clean);
        listFor(role).add(clean);
    }

    public void removeServer(String name) {
        String clean = name == null ? "" : name.trim();
        for (ServerRole role : ServerRole.values()) {
            listFor(role).removeIf(n -> n.equalsIgnoreCase(clean));
        }
    }

    private List<String> listFor(ServerRole role) {
        return switch (role) {
            case INCLUDE -> includedServers;
            case EXCLUDE -> excludedServers;
            case AFK -> afkServerNames;
        };
    }

    /**
     * True when this gamemode should be left alone entirely. An unknown gamemode is never ignored, and
     * the AFK gamemode has its own handling.
     */
    public boolean isIgnoredServer(String name) {
        if (name == null || name.isBlank() || isAfkServer(name)) {
            return false;
        }
        if (containsName(excludedServers, name)) {
            return true;
        }
        return !includedServers.isEmpty() && !containsName(includedServers, name);
    }

    private static boolean containsName(List<String> list, String name) {
        return list.stream().anyMatch(n -> n.equalsIgnoreCase(name.trim()));
    }

    private static List<String> cleanNames(List<String> in) {
        List<String> out = new CopyOnWriteArrayList<>();
        if (in != null) {
            for (String n : in) {
                if (n != null && !n.isBlank() && !containsName(out, n)) {
                    out.add(n.trim());
                }
            }
        }
        return out;
    }

    public int warnStartSeconds() {
        return warningStepsSeconds.get(0);
    }

    public boolean matchesHost(String host) {
        String h = host.toLowerCase(Locale.ROOT);
        return serverHostFragments.stream().anyMatch(f -> !f.isBlank() && h.contains(f.toLowerCase(Locale.ROOT)));
    }

    public boolean isAfkServer(String name) {
        return afkServerNames.stream().anyMatch(n -> n.equalsIgnoreCase(name.trim()));
    }

    private void normalize() {
        afkServerNames = afkServerNames == null ? new ArrayList<>(List.of("afk")) : afkServerNames;
        afkServerNames = cleanNames(afkServerNames);
        includedServers = cleanNames(includedServers);
        excludedServers = cleanNames(excludedServers);
        alertAfterSeconds = Math.max(10, Math.min(900, alertAfterSeconds));
        alertRepeatSeconds = Math.max(0, Math.min(900, alertRepeatSeconds));
        autoAfkAfterSeconds = Math.max(60, Math.min(3600, autoAfkAfterSeconds));
        autoAfkCommand = autoAfkCommand == null ? "" : autoAfkCommand.replaceAll("[\\r\\n]", " ").trim();
        while (autoAfkCommand.startsWith("/")) {
            autoAfkCommand = autoAfkCommand.substring(1).trim();
        }
        if (autoAfkCommand.isEmpty() || autoAfkCommand.length() > 100) {
            autoAfkCommand = "joinq AFK";
        }
        alertSoundFile = alertSoundFile == null ? "" : alertSoundFile.trim();
        alertVolume = Math.max(0, Math.min(100, alertVolume));
        if (alertSound == null || !List.of("xp", "pling", "bell", "custom", "none").contains(alertSound)) {
            alertSound = "xp";
        }
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
        hudX = Math.max(0f, Math.min(1f, hudX));
        hudY = Math.max(0f, Math.min(1f, hudY));
    }
}
