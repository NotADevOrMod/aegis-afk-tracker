package io.aegis.afkwatch.stats;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonParseException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Saves and loads {@link Stats} as JSON. Writes happen on a background thread. */
public final class StatsStore {
    /** On-disk shape. Durations and gamemode names only. */
    static final class FileModel {
        int v = 1;
        Map<String, Map<String, Stats.Bucket>> days = new TreeMap<>();
    }

    private static final Gson GSON = new GsonBuilder().setPrettyPrinting().create();

    private final Path file;
    private final ExecutorService io = Executors.newSingleThreadExecutor(task -> {
        Thread t = new Thread(task, "aegis-afk-stats");
        t.setDaemon(true);
        return t;
    });

    public StatsStore(Path file) {
        this.file = file;
    }

    /** Fills {@code into} from disk. A missing file is normal; a damaged one is set aside, not overwritten. */
    public void load(Stats into) {
        if (!Files.exists(file)) {
            return;
        }
        try {
            FileModel model = GSON.fromJson(Files.readString(file), FileModel.class);
            if (model != null) {
                into.load(model.days);
            }
        } catch (IOException | JsonParseException e) {
            try {
                Files.move(file, file.resolveSibling(file.getFileName() + ".bad"), StandardCopyOption.REPLACE_EXISTING);
            } catch (IOException ignored) {
                // Nothing more we can do; start empty.
            }
        }
    }

    public void saveAsync(Stats from) {
        Map<String, Map<String, Stats.Bucket>> snapshot = from.snapshot();
        io.execute(() -> write(snapshot));
    }

    private void write(Map<String, Map<String, Stats.Bucket>> days) {
        FileModel model = new FileModel();
        model.days = days;
        try {
            Files.createDirectories(file.getParent());
            Path temp = file.resolveSibling(file.getFileName() + ".tmp");
            Files.writeString(temp, GSON.toJson(model));
            Files.move(temp, file, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ignored) {
            // Statistics are a convenience; failing to save must never affect the game.
        }
    }
}
