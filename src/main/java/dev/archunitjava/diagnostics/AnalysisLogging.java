package dev.archunitjava.diagnostics;

import dev.archunitjava.report.ConsoleColor;
import dev.archunitjava.report.ConsoleText;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Explicit per-check observer; no global logger, files, timestamps or implicit console writes.
 * Sink runtime failures are isolated so logging cannot replace a rule result or execution error.
 * Callers own sink lifetime, flushing and concurrency. Fatal VM errors are not intercepted.
 */
public final class AnalysisLogging {
    private static final AnalysisLogging DISABLED = new AnalysisLogging(LogLevel.OFF, event -> {});
    private final LogLevel level;
    private final Consumer<AnalysisEvent> sink;

    private AnalysisLogging(LogLevel level, Consumer<AnalysisEvent> sink) {
        this.level = Objects.requireNonNull(level, "level");
        this.sink = Objects.requireNonNull(sink, "sink");
    }

    public static AnalysisLogging disabled() { return DISABLED; }

    public static AnalysisLogging of(LogLevel level, Consumer<AnalysisEvent> sink) {
        return new AnalysisLogging(level, sink);
    }

    /** Plain text is the conservative default for appendable destinations such as files. */
    public static AnalysisLogging text(LogLevel level, Appendable destination) {
        return text(level, destination, ConsoleColor.NEVER);
    }

    public static AnalysisLogging text(LogLevel level, Appendable destination, ConsoleColor color) {
        Objects.requireNonNull(destination, "destination");
        boolean useColor = Objects.requireNonNull(color, "color").enabled();
        return of(level, event -> {
            try {
                destination.append(format(event, useColor));
            } catch (IOException failure) {
                throw new UncheckedIOException(failure);
            }
        });
    }

    public LogLevel level() { return level; }
    public boolean enabled(LogLevel eventLevel) { return level.includes(eventLevel); }

    public void emit(LogLevel eventLevel, String stage, String message, Supplier<Map<String, String>> details) {
        if (!enabled(eventLevel)) return;
        try {
            sink.accept(new AnalysisEvent(eventLevel, stage, message, details.get()));
        } catch (RuntimeException ignored) {
            // Observability is best effort and never alters architecture semantics.
        }
    }

    public static String format(AnalysisEvent event, boolean color) {
        String code = switch (event.level()) {
            case ERROR -> "31";
            case WARN -> "33";
            case INFO -> "36";
            case DEBUG, OFF -> "2";
        };
        return ConsoleText.style("[" + event.level() + "]", code, color) + " "
                + ConsoleText.sanitize(event.stage()) + " | " + ConsoleText.sanitize(event.message())
                + (event.details().isEmpty() ? "" : " " + ConsoleText.sanitize(event.details().toString())) + "\n";
    }
}
