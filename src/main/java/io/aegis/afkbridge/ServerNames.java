package io.aegis.afkbridge;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reads the server name out of the network's "Sending you to X now..." line. */
final class ServerNames {
    private ServerNames() {}

    // Anchored on purpose: if a server relays player chat as a system message, it arrives with the
    // player's name in front and so can never match.
    private static final Pattern SWITCH = Pattern.compile(
            "^\\s*sending you to\\s+\\[?\\s*([^\\]\\n]+?)\\s*\\]?\\s+now",
            Pattern.CASE_INSENSITIVE);

    static Optional<String> parseSwitch(String chatLine) {
        if (chatLine == null) {
            return Optional.empty();
        }
        Matcher m = SWITCH.matcher(chatLine.replaceAll("§.", ""));
        return m.find() ? Optional.of(m.group(1).trim()) : Optional.empty();
    }
}
