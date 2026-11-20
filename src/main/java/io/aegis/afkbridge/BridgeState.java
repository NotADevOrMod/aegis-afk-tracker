package io.aegis.afkbridge;

/**
 * What the mod knows, nothing more: whether we are connected, to which host, which gamemode the
 * network last sent us to, and when the player last did something. Thread-safe.
 */
final class BridgeState {

    /** One immutable reading, serialised to the status file. */
    record Snapshot(long updatedAtMs, boolean connected, String host, String server, long idleMs, long session) {

        static final int PROTOCOL_VERSION = 1;

        String toJson() {
            return "{\"v\":" + PROTOCOL_VERSION
                    + ",\"updatedAtMs\":" + updatedAtMs
                    + ",\"connected\":" + connected
                    + ",\"host\":" + quote(host)
                    + ",\"server\":" + quote(server)
                    + ",\"idleMs\":" + idleMs
                    + ",\"session\":" + session + "}";
        }

        private static String quote(String s) {
            StringBuilder out = new StringBuilder("\"");
            for (char c : s.toCharArray()) {
                switch (c) {
                    case '"' -> out.append("\\\"");
                    case '\\' -> out.append("\\\\");
                    default -> {
                        if (c < 0x20) {
                            out.append(String.format("\\u%04x", (int) c));
                        } else {
                            out.append(c);
                        }
                    }
                }
            }
            return out.append('"').toString();
        }
    }

    private boolean connected;
    private String host = "";
    private String server = "";
    private long lastActivity = System.currentTimeMillis();
    private long session;

    synchronized void onJoin(String newHost, long now) {
        if (newHost == null || newHost.isEmpty()) {
            connected = false; // singleplayer or unknown: nothing to report
            return;
        }
        if (connected && newHost.equals(host)) {
            lastActivity = now; // proxy hop between gamemodes on the same connection
            return;
        }
        connected = true;
        host = newHost;
        server = "";
        lastActivity = now;
        session++;
    }

    synchronized void onDisconnect() {
        connected = false;
    }

    synchronized void onMessage(String text, long now) {
        if (!connected) {
            return;
        }
        ServerNames.parseSwitch(text).ifPresent(name -> {
            server = name;
            lastActivity = now; // switching gamemode is something the player just did
        });
    }

    synchronized void markActive(long now) {
        if (connected) {
            lastActivity = Math.max(lastActivity, now);
        }
    }

    synchronized Snapshot snapshot(long now) {
        return connected
                ? new Snapshot(now, true, host, server, Math.max(0, now - lastActivity), session)
                : new Snapshot(now, false, "", "", 0, session);
    }
}
