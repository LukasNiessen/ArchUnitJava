package dev.archunitjava.cli;

import dev.archunitjava.importer.ClassFileInput;
import dev.archunitjava.importer.ClassPathImportResolver;
import dev.archunitjava.importer.ImportResolutionResult;
import dev.archunitjava.importer.InputDiagnosticCode;
import dev.archunitjava.report.ResultReport;
import dev.archunitjava.result.Diagnostic;
import dev.archunitjava.result.RuleResult;
import dev.archunitjava.result.Severity;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

/** Direct Java API for the same bounded import and rule evaluation performed by the CLI. */
public final class CliAnalyzer {
    public CliAnalysisResult analyze(CliConfiguration configuration) {
        CliConfiguration config = Objects.requireNonNull(configuration, "configuration");
        var imports = new ClassPathImportResolver().resolve(config.inputs().stream()
                .map(ClassFileInput::path).toList());
        var graph = CliGraphBuilder.build(imports.model());
        var failures = importFailures(imports);
        var results = CliRuleFactory.createRules(config, imports.model(), graph).stream()
                .map(rule -> rule.check(config.checkOptions()))
                .map(result -> withImportDiagnostics(result, failures,
                        config.checkOptions().allowIncompleteAnalysis()))
                .toList();
        return new CliAnalysisResult(imports, graph, ResultReport.of(results));
    }

    static List<Diagnostic> importFailures(ImportResolutionResult imports) {
        List<Diagnostic> failures = new ArrayList<>();
        imports.assembly().diagnostics().stream()
                .filter(value -> incompleteInput(value.code()))
                .forEach(value -> failures.add(importDiagnostic(
                        "cli.input." + value.code().name(), value.input(), value.context())));
        imports.model().classFileDiagnostics().forEach(value -> failures.add(importDiagnostic(
                "cli.classfile." + value.code().name(), value.resourceName(), value.context())));
        imports.model().diagnostics().forEach(value -> failures.add(importDiagnostic(
                "cli.model." + value.code().name(), value.resourceName(), value.context())));
        return failures.stream().distinct().sorted().toList();
    }

    private static boolean incompleteInput(InputDiagnosticCode code) {
        return switch (code) {
            case ARCHIVE_RESOURCE_REJECTED, DIAGNOSTIC_LIMIT_REACHED, INVALID_IGNORE_RULE,
                    INVALID_RESOURCE_NAME, IO_FAILURE, MISSING_INPUT, RESOURCE_LIMIT_EXCEEDED,
                    UNREADABLE_INPUT, UNSUPPORTED_INPUT -> true;
            // These record the documented classpath selection and exclusion policy.
            case DUPLICATE_INPUT, DUPLICATE_RESOURCE, MANIFEST_CLASS_PATH_REJECTED,
                    MULTI_RELEASE_ENTRY_IGNORED, NESTED_ARCHIVE_REJECTED, RESOURCE_EXCLUDED,
                    SYMLINK_SKIPPED -> false;
        };
    }

    private static Diagnostic importDiagnostic(
            String code, String resource, Map<String, String> context) {
        Map<String, String> details = new TreeMap<>(context);
        details.put("resource", resource);
        return new Diagnostic(code, Severity.ERROR, details);
    }

    private static RuleResult withImportDiagnostics(
            RuleResult result, List<Diagnostic> failures, boolean allowIncomplete) {
        if (failures.isEmpty()) return result;
        List<Diagnostic> diagnostics = new ArrayList<>(result.diagnostics());
        for (Diagnostic failure : failures) {
            diagnostics.add(allowIncomplete
                    ? new Diagnostic(failure.code(), Severity.WARNING, failure.context()) : failure);
        }
        if (!allowIncomplete) {
            return RuleResult.incomplete(result.metadata(), result.violations(), diagnostics);
        }
        return switch (result.status()) {
            case PASSED -> RuleResult.passed(result.metadata(), diagnostics);
            case FAILED -> RuleResult.failed(result.metadata(), result.violations(), diagnostics);
            case SKIPPED -> RuleResult.skipped(result.metadata(), diagnostics);
            case INCOMPLETE -> RuleResult.incomplete(result.metadata(), result.violations(), diagnostics);
        };
    }
}
