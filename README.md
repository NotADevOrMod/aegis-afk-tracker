# AEGIS AFK Bridge (mod)

A tiny client-side Fabric mod. It does one thing: every second it writes a small status file for the **AEGIS AFK Watch** app to read. It draws nothing, changes nothing in the game, and sends nothing over the network.

## What it publishes

`%APPDATA%\AegisAfkWatch\bridge.json`:

```json
{"v":1,"updatedAtMs":1767225600000,"connected":true,"host":"play.example.com","server":"AFK","idleMs":4000,"session":1}
```

| Field | Meaning |
|---|---|
| `connected` / `host` | Whether you are on a multiplayer server, and its address. |
| `server` | The gamemode the network last sent you to ("Sending you to X now..."). Empty until the first switch. |
| `idleMs` | How long since you last did something in the game. |
| `session` | Increments on every new connection (not on gamemode hops), so a quick reconnect is never missed. |
| `updatedAtMs` | Wall-clock time of this write; the app treats a stale value as "the game is gone". |

No tokens, usernames or account data are read or written.

## How it decides you are active

Read-only polling of state the game already tracks: movement keys, jump, sneak, sprint, attack, use, mouse movement, camera rotation, an open menu or chat (counts for 60 seconds, since typing itself is not observable), and window focus (a quick alt-tab under 15 seconds is forgiven). It never sends input.

## Building

Push to GitHub. The workflow builds with JDK 25 and uploads the jar as an artifact. Keep `gradle.properties` in step with the quotas mod repo (`minecraft_version`, `loader_version`, `loom_version`, `fabric_version`).

## Per-version checklist

1. It compiles. All game-facing code is in `InputProbe` (state reads) and `AfkBridgeClient` (events).
2. Join BendersMC: `bridge.json` appears and `updatedAtMs` advances about once a second.
3. Hop gamemodes: `server` changes and `session` does not. Disconnect: `connected` becomes false.
4. Stand still: `idleMs` grows. Move: it drops back near zero.
5. Confirm the file contains no token or personal data.
