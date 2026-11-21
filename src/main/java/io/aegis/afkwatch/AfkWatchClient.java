package io.aegis.afkwatch;

import io.aegis.afkwatch.core.Session;
import io.aegis.afkwatch.core.Settings;
import io.aegis.afkwatch.mc.GameBridge;
import io.aegis.afkwatch.stats.Stats;
import io.aegis.afkwatch.stats.StatsStore;
import io.aegis.afkwatch.stats.Tracker;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AfkWatchClient implements ClientModInitializer {
    public static final String MOD_ID = "aegis_afk";
    private static final Logger LOG = LoggerFactory.getLogger("AEGIS AFK Watch");

    @Override
    public void onInitializeClient() {
        var configDir = FabricLoader.getInstance().getConfigDir();

        Settings settings = Settings.load(configDir.resolve("aegis_afk.json"));
        if (settings.loadProblem() != null) {
            LOG.warn(settings.loadProblem());
        }

        Session session = new Session(settings);
        Stats lifetime = new Stats();
        Stats thisSession = new Stats();
        StatsStore store = new StatsStore(configDir.resolve("aegis_afk_stats.json"));
        store.load(lifetime);
        Tracker tracker = new Tracker(settings, session, lifetime, thisSession);

        GameBridge.install(new Context(settings, session, tracker, lifetime, thisSession, store));

        LOG.info("Loaded. Tracking servers matching {}; AFK server names {}.",
                settings.serverHostFragments, settings.afkServerNames);
    }
}
