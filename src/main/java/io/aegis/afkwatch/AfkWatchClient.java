package io.aegis.afkwatch;

import io.aegis.afkwatch.core.Session;
import io.aegis.afkwatch.core.Settings;
import io.aegis.afkwatch.mc.GameBridge;
import io.aegis.afkwatch.window.AfkWindow;
import net.fabricmc.api.ClientModInitializer;
import net.fabricmc.loader.api.FabricLoader;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class AfkWatchClient implements ClientModInitializer {
    public static final String MOD_ID = "aegis_afk";
    private static final Logger LOG = LoggerFactory.getLogger("AEGIS AFK Watch");

    @Override
    public void onInitializeClient() {
        Settings settings = Settings.load(FabricLoader.getInstance().getConfigDir().resolve("aegis_afk.json"));
        if (settings.loadProblem() != null) {
            LOG.warn(settings.loadProblem());
        }

        Session session = new Session(settings);
        GameBridge.install(session, settings);
        AfkWindow.install(session, settings, LOG::warn);

        LOG.info("Loaded. Tracking servers matching {}; AFK server names {}.",
                settings.serverHostFragments, settings.afkServerNames);
    }
}
