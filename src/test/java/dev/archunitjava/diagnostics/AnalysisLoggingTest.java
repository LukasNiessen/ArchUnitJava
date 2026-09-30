package dev.archunitjava.diagnostics;

import static org.junit.jupiter.api.Assertions.*;

import dev.archunitjava.execution.CheckOptions;
import dev.archunitjava.graph.TypeId;
import dev.archunitjava.metrics.MetricAmount;
import dev.archunitjava.metrics.MetricName;
import dev.archunitjava.metrics.MetricSample;
import dev.archunitjava.metrics.MetricThreshold;
import dev.archunitjava.metrics.MetricThresholdRules;
import dev.archunitjava.report.ConsoleColor;
import dev.archunitjava.report.ResultJsonRenderer;
import dev.archunitjava.report.ResultReport;
import dev.archunitjava.result.RuleResult;
import dev.archunitjava.rules.ArchitectureRules;
import dev.archunitjava.rules.RuleTerminal;
import dev.archunitjava.selector.SelectorDescription;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class AnalysisLoggingTest {
    @Test
    void levelsFilterBeforeEvaluatingLazyDetails() {
        for (LogLevel level : LogLevel.values()) {
            List<AnalysisEvent> events = new ArrayList<>();
            var log = AnalysisLogging.of(level, events::add);
            for (LogLevel emitted : LogLevel.values()) {
                log.emit(emitted, "ASSERT", "event", () -> {
                    assertTrue(level.includes(emitted));
                    return Map.of();
                });
            }
            assertEquals(level.ordinal(), events.size(), level.name());
            assertTrue(events.stream().allMatch(event -> level.includes(event.level())));
        }
        AnalysisLogging.disabled().emit(LogLevel.DEBUG, "ASSERT", "disabled",
                () -> { throw new AssertionError("Disabled logging evaluated a supplier"); });
    }

    @Test
    void loggingPreservesRuleResultsAndTheOriginalExecutionException() {
        var rule = ArchitectureRules.define("selection", "Inspect selection", (metadata, options) ->
                RuleTerminal.evaluate(metadata, options, new SelectorDescription("nothing"), 0,
                        diagnostics -> RuleResult.passed(metadata, diagnostics)));
        String expected = ResultJsonRenderer.render(ResultReport.of(List.of(rule.check())));
        for (LogLevel level : LogLevel.values()) {
            List<AnalysisEvent> events = new ArrayList<>();
            var options = CheckOptions.builder().logging(AnalysisLogging.of(level, events::add)).build();
            assertEquals(expected, ResultJsonRenderer.render(ResultReport.of(List.of(rule.check(options)))));
            assertSame(options.logging(), options.toBuilder().build().logging());
            if (level == LogLevel.DEBUG) {
                assertTrue(events.stream().anyMatch(event -> event.message().equals("Selection")
                        && event.details().get("selected").equals("0")));
                assertTrue(events.stream().anyMatch(event -> event.level() == LogLevel.ERROR));
            }
        }
        var failingSink = CheckOptions.builder().logging(AnalysisLogging.of(LogLevel.DEBUG,
                event -> { throw new IllegalStateException("closed log"); })).build();
        assertEquals(rule.check(), rule.check(failingSink));
        var original = new IllegalStateException("evaluation failed");
        var failingRule = ArchitectureRules.define("failure", "Fails to execute",
                (metadata, options) -> { throw original; });
        assertSame(original, assertThrows(IllegalStateException.class, () -> failingRule.check(failingSink)));
    }

    @Test
    void metricInspectionIncludesPassingAndFailingSamplesWithoutChangingViolations() {
        MetricName metric = MetricName.values()[0];
        List<MetricSample> samples = List.of(
                new MetricSample(TypeId.ofBinaryName("example.Pass"), metric, MetricAmount.of(1, metric.unit())),
                new MetricSample(TypeId.ofBinaryName("example.Fail"), metric, MetricAmount.of(3, metric.unit())));
        var rule = MetricThresholdRules.enforce(samples,
                MetricThreshold.atMost(metric, MetricAmount.of(2, metric.unit())));
        List<AnalysisEvent> events = new ArrayList<>();
        var actual = rule.check(CheckOptions.builder().logging(AnalysisLogging.of(LogLevel.DEBUG, events::add)).build());
        assertEquals(rule.check(), actual);
        assertEquals(1, actual.violations().size());
        assertEquals(2, events.stream().filter(event -> event.message().equals("Metric sample")).count());
        assertTrue(events.stream().anyMatch(event -> event.message().equals("Metric threshold")));
    }

    @Test
    void structuredEventsAreImmutableAndTextCannotInjectTerminalControlSequences() {
        Map<String, String> details = new HashMap<>(Map.of("z", "last", "a", "first\n\u001b[31m\u202e"));
        var event = new AnalysisEvent(LogLevel.WARN, "EXTRACT", "bad\rlabel", details);
        details.clear();
        assertEquals(List.of("a", "z"), new ArrayList<>(event.details().keySet()));
        assertThrows(UnsupportedOperationException.class, () -> event.details().put("x", "y"));
        String plain = AnalysisLogging.format(event, false);
        assertTrue(plain.contains("bad\\rlabel"));
        assertTrue(plain.contains("first\\n?[31m?"));
        assertFalse(plain.contains("\u001b"));
        assertEquals(1, plain.chars().filter(c -> c == '\n').count());
        String colored = AnalysisLogging.format(event, true);
        assertTrue(colored.startsWith("\u001b[33m[WARN]\u001b[0m"));
        assertEquals(plain, colored.replaceAll("\u001b\\[[0-9;]*m", ""));
    }

    @Test
    void appendableFailureCannotChangeACheckAndTextDefaultsToPlain() {
        var writer = new java.io.Writer() {
            @Override public void write(char[] buffer, int offset, int length) throws java.io.IOException {
                throw new java.io.IOException("disk full");
            }
            @Override public void flush() {}
            @Override public void close() {}
        };
        var rule = ArchitectureRules.define("pass", "Pass", (metadata, options) -> RuleResult.passed(metadata));
        assertEquals(rule.check(), rule.check(CheckOptions.builder()
                .logging(AnalysisLogging.text(LogLevel.DEBUG, writer)).build()));
        StringBuilder out = new StringBuilder();
        rule.check(CheckOptions.builder().logging(AnalysisLogging.text(LogLevel.INFO, out)).build());
        assertTrue(out.toString().contains("[INFO] ASSERT | Rule started"));
        assertFalse(out.toString().contains("\u001b"));
    }

    @Test
    void colorHonorsTerminalAndExplicitEnvironmentOptOuts() {
        assertFalse(ConsoleColor.AUTO.enabled(false, Map.of()));
        assertTrue(ConsoleColor.AUTO.enabled(true, Map.of()));
        assertTrue(ConsoleColor.ALWAYS.enabled(false, Map.of()));
        assertFalse(ConsoleColor.NEVER.enabled(true, Map.of()));
        for (ConsoleColor color : ConsoleColor.values()) {
            assertFalse(color.enabled(true, Map.of("NO_COLOR", "")));
            assertFalse(color.enabled(true, Map.of("CI", "true")));
            assertFalse(color.enabled(true, Map.of("TERM", "dumb")));
        }
    }
}

