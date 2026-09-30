package dev.archunitjava.diagnostics;

import dev.archunitjava.graph.DependencyGraph;
import dev.archunitjava.importer.ImportResolutionResult;
import dev.archunitjava.metrics.MetricSample;
import dev.archunitjava.result.RuleResult;
import java.util.Collection;
import java.util.Map;

/** Read-only inspection of existing immutable data; never parses, loads or reevaluates target code. */
public final class AnalysisInspection {
    private AnalysisInspection() {}

    public static void imports(ImportResolutionResult imports, AnalysisLogging log) {
        if (log.level() == LogLevel.OFF) return;
        log.emit(LogLevel.INFO, "EXTRACT", "Import complete", () -> Map.of(
                "types", "" + imports.model().types().size(), "externalTypes", "" + imports.externalTypes().size(),
                "resources", "" + imports.assembly().selections().size()));
        if (log.enabled(LogLevel.DEBUG)) {
            imports.assembly().selections().forEach(selection -> log.emit(LogLevel.DEBUG, "EXTRACT",
                    "Selected resource", () -> Map.of("resource", selection.winner().name(),
                            "precedence", "" + selection.winner().precedence())));
            imports.importedTypes().forEach(type -> log.emit(LogLevel.DEBUG, "EXTRACT", "Imported type",
                    () -> Map.of("type", type.winner().binaryName(), "scope", type.lookupScope(),
                            "shadowedDefinitions", "" + type.shadowedDefinitions().size())));
            imports.externalTypes().forEach(type -> log.emit(LogLevel.DEBUG, "EXTRACT", "External type",
                    () -> Map.of("type", type.name().binaryName())));
        }
        imports.assembly().diagnostics().forEach(diagnostic -> log.emit(switch (diagnostic.code()) {
            case DUPLICATE_INPUT, DUPLICATE_RESOURCE, MULTI_RELEASE_ENTRY_IGNORED,
                    RESOURCE_EXCLUDED, SYMLINK_SKIPPED -> LogLevel.DEBUG;
            default -> LogLevel.WARN;
        }, "EXTRACT",
                "Input selection diagnostic", () -> Map.of("code", diagnostic.code().name(),
                        "input", diagnostic.input(), "context", diagnostic.context().toString())));
        imports.diagnostics().forEach(diagnostic -> log.emit(LogLevel.WARN, "EXTRACT",
                "Import resolution diagnostic", () -> Map.of("diagnostic", diagnostic.toString())));
        imports.model().classFileDiagnostics().forEach(diagnostic -> log.emit(LogLevel.ERROR, "EXTRACT",
                "Class file diagnostic", () -> Map.of("diagnostic", diagnostic.toString())));
        imports.model().diagnostics().forEach(diagnostic -> log.emit(LogLevel.WARN, "EXTRACT",
                "Model diagnostic", () -> Map.of("diagnostic", diagnostic.toString())));
    }

    public static void graph(DependencyGraph graph, AnalysisLogging log) {
        log.emit(LogLevel.INFO, "PROJECT", "Graph ready", () -> Map.of(
                "nodes", "" + graph.nodes().size(), "edges", "" + graph.edges().size()));
        if (!log.enabled(LogLevel.DEBUG)) return;
        graph.nodes().forEach(node -> log.emit(LogLevel.DEBUG, "PROJECT", "Graph node",
                () -> Map.of("node", node.id().stableKey())));
        graph.edges().forEach(edge -> {
            log.emit(LogLevel.DEBUG, "PROJECT", "Dependency edge", () -> Map.of(
                    "origin", edge.origin().stableKey(), "target", edge.target().stableKey(),
                    "kind", edge.kind().name(), "evidence", "" + edge.evidence().size()));
            edge.evidence().forEach(evidence -> log.emit(LogLevel.DEBUG, "PROJECT", "Dependency evidence",
                    () -> Map.of("origin", edge.origin().stableKey(), "target", edge.target().stableKey(),
                            "evidence", evidence.toString())));
        });
    }

    public static void metrics(Collection<MetricSample> samples, AnalysisLogging log) {
        if (!log.enabled(LogLevel.DEBUG)) return;
        samples.stream().sorted().forEach(sample -> log.emit(LogLevel.DEBUG, "ASSERT", "Metric sample",
                () -> Map.of("subject", sample.subject().stableKey(), "metric", sample.metric().name(),
                        "value", sample.amount().stableValue(), "unit", sample.amount().unit().name())));
    }

    public static void result(RuleResult result, AnalysisLogging log) {
        if (log.level() == LogLevel.OFF) return;
        LogLevel level = switch (result.status()) {
            case PASSED -> LogLevel.INFO;
            case FAILED, INCOMPLETE -> LogLevel.ERROR;
            case SKIPPED -> LogLevel.WARN;
        };
        log.emit(level, "REPORT", "Rule complete", () -> Map.of("rule", result.ruleId(),
                "status", result.status().name(), "violations", "" + result.violations().size()));
        result.diagnostics().forEach(diagnostic -> log.emit(
                switch (diagnostic.severity()) {
                    case ERROR -> LogLevel.ERROR;
                    case WARNING -> LogLevel.WARN;
                    case INFO -> LogLevel.INFO;
                },
                "REPORT", "Rule diagnostic", () -> Map.of("rule", result.ruleId(),
                        "code", diagnostic.code(), "context", diagnostic.context().toString())));
        if (!log.enabled(LogLevel.DEBUG)) return;
        result.violations().forEach(violation -> {
            log.emit(LogLevel.DEBUG, "REPORT", "Violation", () -> Map.of("rule", result.ruleId(),
                    "id", violation.id().value(), "code", violation.code(),
                    "subjects", violation.subjects().toString(), "attributes", violation.attributes().toString()));
            violation.evidence().forEach(evidence -> log.emit(LogLevel.DEBUG, "REPORT", "Violation evidence",
                    () -> Map.of("id", violation.id().value(), "evidence", evidence.toString())));
        });
    }
}
