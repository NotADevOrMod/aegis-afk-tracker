# Changelog

## [0.3.0]
- **Idle alert** (off by default): set the delay, repeat, sound and chat line in Settings, with a Test button. Only fires in a tracked, non-AFK gamemode.
- **Servers screen**: add gamemodes as Include, Exclude or AFK. Excluded gamemodes are ignored by the pill, alert and statistics; if any are included, only those are tracked.
- **Pill placement reworked**: click to pick up, click to drop, plus arrow nudges. The old press-and-drag relied on a mouse-state call that did not work in screens.
- Reset stats moved into Settings. The analysis screen now has Settings, Move pill and Done.
- Compile-risk notes: `SimpleSoundInstance.forUI(...)` in `mc/AlertPlayer` and the `EditBox` calls in `ui/ServersScreen` are the new game calls most likely to need a one-line fix.

## [0.2.5]
- Renamed `mc/GameBridge` to `mc/ClientSetup`. Everything lives in the mod; nothing bridges to anything else. No behaviour change.
- Upgrade note: delete the old `src/main/java/io/aegis/afkwatch/mc/GameBridge.java` from your repo.

## [0.2.4]
- Build fixes for 26.3: key type is `InputConstants.Type.KEYBOARD`; numpad 9 is given as its key code so no GLFW import is needed.
- Upgrade note: delete the old `window/` source folder (see README).

## [0.2.3]
- Diagnostics: `aegis_afk_diagnostics.txt` records startup steps, joins, mode changes and errors. Every registration step is guarded so one failure cannot hide the rest, and tick and HUD errors are logged instead of ignored.

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
