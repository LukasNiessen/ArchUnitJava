package dev.archunitjava.rules;

import dev.archunitjava.execution.CheckOptions;
import dev.archunitjava.diagnostics.AnalysisInspection;
import dev.archunitjava.diagnostics.LogLevel;
import dev.archunitjava.result.RuleMetadata;
import dev.archunitjava.result.RuleResult;
import dev.archunitjava.result.Severity;
import java.util.Collection;
import java.util.Objects;
import java.util.Map;

/** Factory for immutable rule values; later rule DSLs share this metadata behavior. */
public final class ArchitectureRules {
    private ArchitectureRules() {}

    public static ArchitectureRule define(
            String semanticIdentity, String description, RuleEvaluation evaluation) {
        return new ImmutableArchitectureRule(
                RuleMetadata.of(semanticIdentity, description),
                Objects.requireNonNull(evaluation, "evaluation"));
    }

    private record ImmutableArchitectureRule(RuleMetadata metadata, RuleEvaluation evaluation)
            implements ArchitectureRule {
        private ImmutableArchitectureRule {
            Objects.requireNonNull(metadata, "metadata");
            Objects.requireNonNull(evaluation, "evaluation");
        }

        @Override
        public RuleResult check(CheckOptions options) {
            Objects.requireNonNull(options, "options");
            var log = options.logging();
            log.emit(LogLevel.INFO, "ASSERT", "Rule started", () -> Map.of(
                    "rule", metadata.semanticIdentity(), "description", metadata.displayName()));
            log.emit(LogLevel.DEBUG, "ASSERT", "Check policies", () -> Map.of(
                    "emptySelection", options.emptySelectionPolicy().name(),
                    "allowIncompleteAnalysis", "" + options.allowIncompleteAnalysis()));
            try {
                RuleResult result = Objects.requireNonNull(evaluation.evaluate(metadata, options), "rule result");
                if (!result.metadata().equals(metadata)) {
                    throw new IllegalStateException(
                            "Rule evaluation returned metadata other than the supplied immutable value");
                }
                AnalysisInspection.result(result, log);
                return result;
            } catch (RuntimeException failure) {
                log.emit(LogLevel.ERROR, "ASSERT", "Rule execution failed", () -> Map.of(
                        "rule", metadata.semanticIdentity(), "exception", failure.getClass().getName()));
                throw failure;
            }
        }

        @Override
        public ArchitectureRule as(String displayName) {
            return new ImmutableArchitectureRule(metadata.as(displayName), evaluation);
        }

        @Override
        public ArchitectureRule because(String rationale) {
            return new ImmutableArchitectureRule(metadata.because(rationale), evaluation);
        }

        @Override
        public ArchitectureRule tagged(String... tags) {
            return new ImmutableArchitectureRule(metadata.tagged(tags), evaluation);
        }

        @Override
        public ArchitectureRule tagged(Collection<String> tags) {
            return new ImmutableArchitectureRule(metadata.tagged(tags), evaluation);
        }

        @Override
        public ArchitectureRule withSeverity(Severity severity) {
            return new ImmutableArchitectureRule(metadata.withSeverity(severity), evaluation);
        }
    }
}
