# AEGIS AFK Watch

A small client-side Fabric mod for BendersMC staff. It watches *your own* input and tells you when you have gone idle.

- **Outside the AFK server:** after 2:00 without input the indicator turns orange, then red, and the wording escalates (default steps: 2:00, 3:00, 5:00).
- **Inside the AFK server:** once idle it shows a calm blue AFK timer. No warnings.
- **Connection drop while idle:** the window shows "Disconnected" with how long you had been idle. It does not reconnect for you.
- **Two displays:** an in-game HUD pill, and an optional borderless always-on-top desktop window (drag it, right-click for options).

## What it does and does not do

- Reads input state the game already tracks (movement keys, mouse, camera, open menus, window focus).
- Sends nothing: no keys, no movement, no chat, no commands, no network traffic of its own.
- Stores no credentials, tokens, or personal data. The only file it writes is `config/aegis_afk.json`.
- Not an anti-AFK tool. It never tries to make you look active.

## How it knows where you are

The network prints a "Sending you to X now..." line when you change gamemode. The mod reads that line, exactly as the quotas mod does. If X matches `afkServerNames`, you are in the AFK server.

Known limitation: the mod only learns your server from that line. If you connect and land straight in the AFK server without it being printed, it assumes you are elsewhere until your next switch.

## Config (`config/aegis_afk.json`, created on first launch, applied on restart)

| Key | Default | Meaning |
|---|---|---|
| `serverHostFragments` | `["benders"]` | Server address must contain one of these. Add the exact host if needed. |
| `afkServerNames` | `["afk"]` | Names that count as the AFK server (case-insensitive). |
| `warningStepsSeconds` | `[120, 180, 300]` | Idle seconds at which each warning level starts. First entry = when warnings begin. |
| `afkServerAfterSeconds` | `15` | Idle seconds before the AFK server turns blue. |
| `screenGraceSeconds` | `60` | How long an open menu/chat counts as activity. |
| `unfocusedGraceSeconds` | `15` | How long an alt-tabbed game still counts as active. |
| `disconnectLingerSeconds` | `600` | How long "Disconnected" stays up. |
| `hudEnabled` / `hudShowWhenActive` | `true` / `false` | In-game pill. |
| `windowEnabled` / `windowShowWhenActive` | `true` / `true` | Desktop window. |
| `windowAlwaysOnTop`, `windowOpacity`, `windowX`, `windowY` | | Window appearance; position is saved when you drag it. |
| `beepOnWarning` | `false` | System beep each time the warning level rises. |

The window is disabled on macOS and in headless Java.

## Building

Push to GitHub. The workflow in `.github/workflows/build.yml` builds with JDK 25 and uploads the jar as an artifact. Locally: `gradle build` (JDK 25).

## Per-version checklist

Run this on every Minecraft or Fabric bump, and record the result in the changelog:

1. `gradle.properties`: `minecraft_version`, `loader_version`, `loom_version`, `fabric_version` match the target.
2. It compiles. The Minecraft-facing code is isolated in `mc/`; `HudOverlay` holds every draw call, `InputProbe` every game-state read.
3. Join BendersMC: pill hidden while active, orange at 2:00, red by 3:00, blue in the AFK server.
4. Alt-tab for under 15s: no warning. Over it: counts as idle.
5. Proxy hop between gamemodes keeps the right state (JOIN fires again on a hop).
6. Pull the network cable while idle: window shows "Disconnected".
7. Confirm nothing in the jar or config contains an address, token, or personal data.

## Layout

```
core/     pure Java: Session (state machine), Settings, Palette, ServerNames. No Minecraft imports.
mc/       Fabric/Minecraft adapters: GameBridge, ConnectionHooks, InputProbe, HudOverlay.
window/   Swing desktop window. No Minecraft imports.
```
