package io.aegis.afkwatch;

import io.aegis.afkwatch.core.Session;
import io.aegis.afkwatch.core.Settings;
import io.aegis.afkwatch.mc.GameBridge;
import io.aegis.afkwatch.stats.Stats;
import io.aegis.afkwatch.stats.StatsStore;
import io.aegis.afkwatch.stats.Tracker;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import net.fabricmc.loader.api.ModContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.stream.Collectors;

public final class AfkWatchClient implements ClientModInitializer {
    public static final String MOD_ID = "aegis_afk";
    private static final Logger LOG = LoggerFactory.getLogger("AEGIS AFK Watch");

    @Override
    public void onInitializeClient() {
        var loader = FabricLoader.getInstance();
        var configDir = loader.getConfigDir();

        Diagnostics diag = new Diagnostics(configDir.resolve("aegis_afk_diagnostics.txt"));
        try {
            String version = loader.getModContainer(MOD_ID)
                    .map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("?");
            diag.log("Entrypoint reached. AEGIS AFK Watch " + version);
            diag.log("Java " + System.getProperty("java.version") + ", Minecraft "
                    + loader.getModContainer("minecraft").map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("?")
                    + ", Fabric Loader " + loader.getModContainer("fabricloader")
                            .map(c -> c.getMetadata().getVersion().getFriendlyString()).orElse("?"));
            diag.log("Mods loaded: " + loader.getAllMods().stream()
                    .map(ModContainer::getMetadata).map(m -> m.getId()).sorted().collect(Collectors.joining(", ")));

            Settings settings = Settings.load(configDir.resolve("aegis_afk.json"));
            if (settings.loadProblem() != null) {
                LOG.warn(settings.loadProblem());
                diag.log("Settings problem: " + settings.loadProblem());
            }
            diag.log("Settings loaded. Tracking hosts containing " + settings.serverHostFragments
                    + "; AFK server names " + settings.afkServerNames + "; hudEnabled=" + settings.hudEnabled);

            Session session = new Session(settings);
            Stats lifetime = new Stats();
            Stats thisSession = new Stats();
            StatsStore store = new StatsStore(configDir.resolve("aegis_afk_stats.json"));
            store.load(lifetime);
            Tracker tracker = new Tracker(settings, session, lifetime, thisSession);

            GameBridge.install(new Context(settings, session, tracker, lifetime, thisSession, store, diag));
            diag.log("Startup finished.");
        } catch (Throwable t) {
            diag.error("onInitializeClient", t);
            LOG.error("AEGIS AFK Watch failed to start", t);
        }
    }
}
