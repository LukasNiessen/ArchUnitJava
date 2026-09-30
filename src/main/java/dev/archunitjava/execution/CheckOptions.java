package dev.archunitjava.execution;

import java.util.Objects;
import dev.archunitjava.diagnostics.AnalysisLogging;

/** Immutable policies applied uniformly by architecture-rule terminals. */
public final class CheckOptions {
    private static final CheckOptions DEFAULTS = new Builder().build();

    private final EmptySelectionPolicy emptySelectionPolicy;
    private final boolean allowIncompleteAnalysis;
    private final AnalysisLogging logging;

    private CheckOptions(Builder builder) {
        emptySelectionPolicy = builder.emptySelectionPolicy;
        allowIncompleteAnalysis = builder.allowIncompleteAnalysis;
        logging = builder.logging;
    }

    /** Returns the deterministic strict defaults used by {@link Checkable#check()}. */
    public static CheckOptions defaults() {
        return DEFAULTS;
    }

    /** Starts a builder initialized with the deterministic strict defaults. */
    public static Builder builder() {
        return new Builder();
    }

    public Builder toBuilder() {
        return new Builder(this);
    }

    public boolean allowEmptySelection() {
        return emptySelectionPolicy == EmptySelectionPolicy.ALLOW;
    }

    public EmptySelectionPolicy emptySelectionPolicy() {
        return emptySelectionPolicy;
    }

    public boolean allowIncompleteAnalysis() {
        return allowIncompleteAnalysis;
    }

    /** Per-check observations, disabled by default and independent of rule-result diagnostics. */
    public AnalysisLogging logging() {
        return logging;
    }

    @Override
    public boolean equals(Object other) {
        return other instanceof CheckOptions options
                && emptySelectionPolicy == options.emptySelectionPolicy
                && allowIncompleteAnalysis == options.allowIncompleteAnalysis
                && logging.equals(options.logging);
    }

    @Override
    public int hashCode() {
        return Objects.hash(emptySelectionPolicy, allowIncompleteAnalysis, logging);
    }

    @Override
    public String toString() {
        return "CheckOptions[emptySelectionPolicy=" + emptySelectionPolicy
                + ", allowIncompleteAnalysis=" + allowIncompleteAnalysis + ']';
    }

    /** Mutable construction state; built options never retain a reference to it. */
    public static final class Builder {
        private EmptySelectionPolicy emptySelectionPolicy = EmptySelectionPolicy.FAIL;
        private boolean allowIncompleteAnalysis;
        private AnalysisLogging logging = AnalysisLogging.disabled();

        private Builder() {}

        private Builder(CheckOptions options) {
            emptySelectionPolicy = options.emptySelectionPolicy;
            allowIncompleteAnalysis = options.allowIncompleteAnalysis;
            logging = options.logging;
        }

        public Builder allowEmptySelection(boolean value) {
            emptySelectionPolicy = value ? EmptySelectionPolicy.ALLOW : EmptySelectionPolicy.FAIL;
            return this;
        }

        public Builder emptySelectionPolicy(EmptySelectionPolicy value) {
            emptySelectionPolicy = Objects.requireNonNull(value, "emptySelectionPolicy");
            return this;
        }

        public Builder allowIncompleteAnalysis(boolean value) {
            allowIncompleteAnalysis = value;
            return this;
        }

        public CheckOptions build() {
            return new CheckOptions(this);
        }

        public Builder logging(AnalysisLogging value) {
            logging = Objects.requireNonNull(value, "logging");
            return this;
        }
    }
}
