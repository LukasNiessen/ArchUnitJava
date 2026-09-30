package dev.archunitjava.diagnostics;

import java.util.Collections;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/** Structured observation, separate from rule results and their diagnostic schema. */
public record AnalysisEvent(LogLevel level, String stage, String message, Map<String, String> details) {
    public AnalysisEvent {
        Objects.requireNonNull(level, "level");
        Objects.requireNonNull(stage, "stage");
        Objects.requireNonNull(message, "message");
        TreeMap<String, String> sorted = new TreeMap<>();
        Objects.requireNonNull(details, "details").forEach((key, value) ->
                sorted.put(Objects.requireNonNull(key), Objects.requireNonNull(value)));
        details = Collections.unmodifiableMap(sorted);
    }
    /** Returns a sorted defensive snapshot; callers never receive the stored map. */
    @Override
    public Map<String, String> details() {
        return Collections.unmodifiableMap(new TreeMap<>(details));
    }
}
