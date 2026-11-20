package io.aegis.afkwatch.core;

import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/** Reads the server name out of the network's "Sending you to X now..." line. */
public final class ServerNames {
    private ServerNames() {}

    private static final Pattern SWITCH = Pattern.compile(
            "sending you to\\s+\\[?\\s*([^\\]\\n]+?)\\s*\\]?\\s+now",
            Pattern.CASE_INSENSITIVE);

    public static Optional<String> parseSwitch(String chatLine) {
        if (chatLine == null) {
            return Optional.empty();
        }
        String plain = chatLine.replaceAll("§.", "");
        Matcher m = SWITCH.matcher(plain);
        return m.find() ? Optional.of(m.group(1).trim()) : Optional.empty();
    }
}
