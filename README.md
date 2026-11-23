# AEGIS AFK Watch (mod)

A client-side Fabric mod for BendersMC staff. It watches your own input and tells you when you have gone idle, and it can show how much of your time is real play versus sitting idle.

- **Status pill:** a small pill on the HUD. Outside the AFK server it turns orange at 2:00 idle, red at 3:00, deep red at 5:00, with escalating wording. In the AFK server it shows a calm blue AFK timer. Hidden while you are active.
- **Movable:** open the analysis screen, press **Move pill**, then drag it anywhere. The position is saved as a fraction of the screen, so it survives a GUI-scale change.
- **Analysis screen:** press **Numpad 9** (rebindable under *AEGIS AFK Watch* in Controls). Tabs for this session, today, this week and all time. Shows active time, idle time, idle share, a bar, and a per-gamemode table.
- **Disconnect while idle** is not handled specially in this version.

## What is counted

- **Time in the AFK gamemode is never counted**, anywhere in the analysis.
- Everywhere else on a BendersMC server, time is *active* until you have been idle for the first warning threshold (2:00 by default). At that moment the whole idle stretch, including the two minutes already elapsed, is reclassified as *idle*, and counting continues as idle until you move again.
- Per-gamemode rows use the name from the network's "Sending you to X now..." line. Before the first switch, time is filed under "Unknown".

## How it decides you are active

Read-only polling of state the game already tracks: movement keys, jump, sneak, sprint, attack, use, mouse movement, camera rotation, an open menu or chat (counts for 60 seconds, since typing itself is not observable), and window focus (a quick alt-tab under 15 seconds is forgiven). It never sends input, commands or chat.

## Files and privacy

| File (in the Fabric `config` folder) | Contents |
|---|---|
| `aegis_afk.json` | Settings (below), including the pill position. |
| `aegis_afk_stats.json` | Per day and per gamemode: milliseconds active and idle. Nothing else. |
| `aegis_afk_diagnostics.txt` | A short trail of startup steps and events (see below). Rewritten on each launch, capped at 300 lines. Contains the server address and the list of installed mod ids, so check it before sharing it publicly. |

No network access, no tokens, no usernames, no account data. Use **Reset stats** (it asks you to click twice) to wipe the numbers.

## Settings (`aegis_afk.json`, applied on restart)

| Key | Default | Meaning |
|---|---|---|
| `serverHostFragments` | `["benders"]` | The server address must contain one of these. Add the exact host if needed. |
| `afkServerNames` | `["afk"]` | Gamemode names that count as the AFK server (case-insensitive). |
| `warningStepsSeconds` | `[120, 180, 300]` | Idle seconds at which each warning level starts. The first entry is also the idle threshold for the analysis. |
| `afkServerAfterSeconds` | `15` | Idle seconds before the AFK server turns the pill blue. |
| `screenGraceSeconds` | `60` | How long an open menu or chat counts as activity. |
| `unfocusedGraceSeconds` | `15` | How long an alt-tabbed game still counts as active. |
| `joinNotice` | `true` | One chat line when you join a server: mod version, whether it is tracked, and the address the game reported. |
| `hudEnabled` / `hudShowWhenActive` | `true` / `false` | The pill. |
| `hudX` / `hudY` | `0.5` / `0.03` | Pill position (set by dragging; centre-x and top-y as screen fractions). |

## If nothing shows

0. Open `aegis_afk_diagnostics.txt` in the Fabric `config` folder. It records every startup step (entrypoint reached, settings loaded, HUD element registered, key registered, first tick), every join with the address the game reported, every change of indicator state, and any error with its first few stack lines. No file means the mod's entrypoint never ran. Make sure only one AFK Watch jar is in the mods folder.

1. On joining a server you should see a chat line starting `[AFK Watch <version>]`. If it says **NOT tracking**, copy the address it shows into `serverHostFragments` in `aegis_afk.json` and restart. If you see no line at all, the mod is not the version you think it is, or `joinNotice` is off.
2. The pill is hidden while you are active. To check it works, set `hudShowWhenActive` to `true`, or set `warningStepsSeconds` to `[10, 20, 30]` and stand still for 10 seconds.
3. The analysis screen footer shows whether it is tracking, the current gamemode, and the idle time.

## Limits

- The mod learns the gamemode from the "Sending you to X now..." line. If you connect and land straight in the AFK server without that line, it assumes you are elsewhere until your next switch.
- Idle time before a stretch crosses the threshold is only reclassified once it crosses it; a stretch shorter than the threshold counts as active.

## Building

Push to GitHub. The workflow builds with JDK 25 and uploads the jar as an artifact. Keep `gradle.properties` in step with the quotas mod repo (`minecraft_version`, `loader_version`, `loom_version`, `fabric_version`).

## Per-version checklist

Run on every Minecraft or Fabric bump and record the result in the changelog:

1. It compiles. Game-facing code is limited to `mc/InputProbe` (state reads), `mc/ConnectionHooks` (events), `mc/GameBridge` (keybind, tick), `ui/Pill` (every pill draw call), and the two screens.
2. Join BendersMC: pill hidden while active, orange at 2:00, red by 3:00, blue in the AFK server.
3. Hop gamemodes: the pill keeps the right state, and the analysis table gains a row per gamemode.
4. **Numpad 9** opens the analysis; Escape closes it. Move pill: drag, Done, restart the game, position is kept.
5. Sit in the AFK server for a few minutes: none of that time appears in any tab.
6. Stand idle past the threshold outside the AFK server: the idle figure jumps by the full idle stretch, not just the part after the threshold.
7. Confirm neither JSON file contains anything but settings, gamemode names and durations.

## Layout

```
core/    Pure Java: Session (state machine), Settings, Palette, ServerNames, View, Format. No Minecraft imports.
stats/   Pure Java: Stats, Tracker, StatsStore (Gson). No Minecraft imports.
mc/      Fabric/Minecraft adapters: GameBridge, ConnectionHooks, InputProbe, HudOverlay.
ui/      Pill, PositionScreen, AnalysisScreen.
```
