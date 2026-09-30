package dev.archunitjava.diagnostics;

/** Per-operation verbosity. DEBUG includes every emitted event; OFF emits nothing. */
public enum LogLevel {
    OFF, ERROR, WARN, INFO, DEBUG;

    public boolean includes(LogLevel eventLevel) {
        return eventLevel != OFF && ordinal() >= eventLevel.ordinal();
    }
}
