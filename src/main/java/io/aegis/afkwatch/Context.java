package io.aegis.afkwatch;

import io.aegis.afkwatch.core.Session;
import io.aegis.afkwatch.core.Settings;
import io.aegis.afkwatch.stats.Stats;
import io.aegis.afkwatch.stats.StatsStore;
import io.aegis.afkwatch.stats.Tracker;

/** Everything the screens and the HUD share, built once at startup. */
public record Context(
        Settings settings,
        Session session,
        Tracker tracker,
        Stats lifetime,
        Stats thisSession,
        StatsStore store,
        Diagnostics diag) {
}
