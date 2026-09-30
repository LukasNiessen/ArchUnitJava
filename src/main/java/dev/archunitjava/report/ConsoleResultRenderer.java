package dev.archunitjava.report;

import dev.archunitjava.result.Diagnostic;
import dev.archunitjava.result.RuleResult;
import dev.archunitjava.result.Violation;
import java.util.Objects;
import java.util.List;
import java.util.Map;

/** Bounded, deterministic plain-text output for people and terminal CI logs. */
public final class ConsoleResultRenderer {
    private ConsoleResultRenderer() {}

    public static String render(ResultReport report) {
        return render(report, ResultRenderLimits.defaults());
    }

    public static String render(ResultReport report, ResultRenderLimits limits) {
        ResultReport value = Objects.requireNonNull(report, "report");
        ResultRenderLimits bounds = Objects.requireNonNull(limits, "limits");
        StringBuilder out = new StringBuilder();
        for (RuleResult result : value.results()) {
            out.append(marker(result)).append(' ').append(sanitize(result.metadata().displayName()))
                    .append(" (").append(sanitize(result.ruleId())).append(")\n");
            if (result.status() == dev.archunitjava.result.RuleStatus.FAILED
                    || result.status() == dev.archunitjava.result.RuleStatus.INCOMPLETE) {
                violations(out, result, bounds);
            }
            diagnostics(out, result, bounds);
        }
        return out.toString();
    }

    /** Opt-in formatted report; the existing render overloads retain their plain-text contract. */
    public static String renderPretty(ResultReport report, ConsoleColor color) {
        return renderPretty(report, ResultRenderLimits.defaults(), color, System.console() != null, System.getenv());
    }

    /** Renders with explicit limits and terminal context; labels are always escaped before styling. */
    public static String renderPretty(ResultReport report, ResultRenderLimits limits,
            ConsoleColor color, boolean terminal, Map<String, String> environment) {
        Objects.requireNonNull(report, "report");
        Objects.requireNonNull(limits, "limits");
        boolean useColor = Objects.requireNonNull(color, "color").enabled(terminal, environment);
        StringBuilder out = new StringBuilder(ConsoleText.style("ARCHUNITJAVA | Architecture report", "1;36", useColor))
                .append('\n').append("=====================================\n");
        for (RuleResult result : report.results()) {
            String plain = render(ResultReport.of(List.of(result)), limits);
            int newline = plain.indexOf('\n');
            String code = switch (result.status()) {
                case PASSED -> "1;32";
                case FAILED, INCOMPLETE -> "1;31";
                case SKIPPED -> "1;33";
            };
            out.append(ConsoleText.style(ConsoleText.sanitize(plain.substring(0, newline)), code, useColor))
                    .append('\n');
            plain.substring(newline + 1).lines().forEach(line ->
                    out.append(ConsoleText.sanitize(line)).append('\n'));
            result.metadata().rationale().ifPresent(reason -> out.append("  Because: ")
                    .append(ConsoleText.sanitize(reason)).append('\n'));
            out.append('\n');
        }
        out.append("Summary:");
        for (var status : dev.archunitjava.result.RuleStatus.values()) {
            out.append(' ').append(status).append('=')
                    .append(report.results().stream().filter(result -> result.status() == status).count());
        }
        return out.append('\n').toString();
    }

    private static String marker(RuleResult result) {
        return switch (result.status()) {
            case PASSED -> "[PASS]";
            case FAILED -> "[FAIL:POLICY]";
            case SKIPPED -> "[SKIP]";
            case INCOMPLETE -> "[ERROR:ANALYSIS]";
        };
    }

    private static void violations(
            StringBuilder out, RuleResult result, ResultRenderLimits limits) {
        int count = Math.min(result.violations().size(), limits.maxViolationsPerRule());
        for (int index = 0; index < count; index++) {
            Violation violation = result.violations().get(index);
            out.append("  - ").append(sanitize(violation.id().value())).append(" [")
                    .append(violation.severity()).append("] ").append(sanitize(violation.code()))
                    .append('\n');
            int evidence = Math.min(violation.evidence().size(), limits.maxEvidencePerViolation());
            for (int item = 0; item < evidence; item++) {
                out.append("      at ").append(sanitize(
                        violation.evidence().get(item).location().resourcePath()));
                violation.evidence().get(item).lineNumber().ifPresent(line -> out.append(':').append(line));
                out.append('\n');
            }
            omitted(out, "evidence", violation.evidence().size() - evidence, 6);
        }
        omitted(out, "violations", result.violations().size() - count, 2);
    }

    private static void diagnostics(
            StringBuilder out, RuleResult result, ResultRenderLimits limits) {
        int count = Math.min(result.diagnostics().size(), limits.maxDiagnosticsPerRule());
        for (int index = 0; index < count; index++) {
            Diagnostic diagnostic = result.diagnostics().get(index);
            out.append("  ! ").append(sanitize(diagnostic.code())).append(" [")
                    .append(diagnostic.severity()).append("] ")
                    .append(sanitize(diagnostic.context().toString())).append('\n');
        }
        omitted(out, "diagnostics", result.diagnostics().size() - count, 2);
    }

    private static void omitted(StringBuilder out, String kind, int count, int indent) {
        if (count > 0) out.append(" ".repeat(indent)).append("... ").append(count)
                .append(" more ").append(kind).append('\n');
    }

    private static String sanitize(String value) {
        StringBuilder out = new StringBuilder(value.length());
        value.codePoints().forEach(codePoint -> {
            if (codePoint == '\n') out.append("\\n");
            else if (codePoint == '\r') out.append("\\r");
            else if (codePoint == '\t') out.append("\\t");
            else if (Character.isISOControl(codePoint)) out.append('?');
            else out.appendCodePoint(codePoint);
        });
        return out.toString();
    }
}
