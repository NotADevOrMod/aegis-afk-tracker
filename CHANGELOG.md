# Changelog

## [0.2.2]
- Default analysis key is now Numpad 9. Still rebindable in Controls.

## [0.2.1]
- Default analysis key changed from K to ] (K clashed with the quotas mod). Still rebindable in Controls.
- One-line chat notice on joining a server: mod version, tracked or not, and the address the game reports (`joinNotice` setting).
- Analysis screen footer shows tracking status, gamemode and idle time.

## [0.2.0]
- Removed the desktop window; this is a mod only.
- Movable status pill (drag it in the Move pill screen; position saved as screen fractions).
- Analysis screen on **K**: active versus idle time per session, day, week and all time, with a per-gamemode table. Time in the AFK gamemode is never counted.
- New files: `aegis_afk_stats.json` (durations and gamemode names only).
- Compile-risk notes: `mc.gui.screen()` and `MouseHandler.isLeftPressed()` are the two game calls most likely to need a one-line fix on a new version.

## [0.1.0]
- First build: input-based AFK detection, BendersMC-only, HUD pill, red escalating warnings outside the AFK server, calm blue inside it.
