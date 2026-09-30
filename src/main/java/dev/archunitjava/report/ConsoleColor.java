package dev.archunitjava.report;

import java.util.Map;
import java.util.Objects;

/** ANSI policy. NO_COLOR, CI and TERM=dumb suppress color, including ALWAYS. */
public enum ConsoleColor {
    NEVER, AUTO, ALWAYS;

    public boolean enabled() {
        return enabled(System.console() != null, System.getenv());
    }

    /** Explicit terminal context for embedded applications and deterministic rendering. */
    public boolean enabled(boolean terminal, Map<String, String> environment) {
        Objects.requireNonNull(environment, "environment");
        if (this == NEVER || environment.containsKey("NO_COLOR")
                || environment.containsKey("CI") || "dumb".equals(environment.get("TERM"))) return false;
        return this == ALWAYS || terminal;
    }
}
