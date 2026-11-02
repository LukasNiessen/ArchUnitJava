package dev.archunitjava.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.archunitjava.cli.CliAnalysisResult;
import dev.archunitjava.cli.CliAnalyzer;
import dev.archunitjava.cli.CliConfigurationLoader;
import dev.archunitjava.cli.CliRunner;
import dev.archunitjava.graph.DependencyEvidence;
import dev.archunitjava.graph.DependencyKind;
import dev.archunitjava.graph.MemberId;
import dev.archunitjava.graph.TypeId;
import dev.archunitjava.model.JavaMemberModifier;
import dev.archunitjava.pattern.JavaPattern;
import dev.archunitjava.pattern.PatternDomain;
import dev.archunitjava.result.RuleStatus;
import dev.archunitjava.selector.PackageSelector;
import dev.archunitjava.selector.TypeSelector;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class CycleRulesBytecodeTest {
    private static final String PREFIX = "dev.archunitjava.rules.fixtures.cycles.";
    private static final TypeId LEFT = TypeId.ofBinaryName(PREFIX + "left.CycleLeft");
    private static final TypeId RIGHT = TypeId.ofBinaryName(PREFIX + "right.CycleRight");

    @TempDir Path temporaryDirectory;
    private CliAnalysisResult analysis;

    @BeforeEach
    void importJavacFixturesWithoutLoadingTargetClasses() throws IOException {
        for (String name : List.of(LEFT.binaryName(), RIGHT.binaryName(),
                LEFT.binaryName() + "$OneWay", RIGHT.binaryName() + "$Leaf")) {
            String resource = name.replace('.', '/') + ".class";
            Path target = temporaryDirectory.resolve("classes").resolve(resource);
            Files.createDirectories(target.getParent());
            try (var input = CycleRulesBytecodeTest.class.getResourceAsStream("/" + resource)) {
                assertNotNull(input, resource);
                Files.copy(input, target);
            }
        }
        Path configuration = temporaryDirectory.resolve("archunitjava.properties");
        Files.writeString(configuration, """
                schema=archunitjava.cli.v1
                inputs=classes
                rules=fixture
                rule.fixture.domain=types
                rule.fixture.mode=only
                rule.fixture.origins=glob:dev.archunitjava.rules.fixtures.cycles.**
                rule.fixture.targets=glob:dev.archunitjava.rules.fixtures.cycles.**
                rule.fixture.external=ignore
                """);
        analysis = new CliAnalyzer().analyze(
                CliConfigurationLoader.load(configuration, temporaryDirectory));
        assertTrue(analysis.imports().model().classFileDiagnostics().isEmpty());
        assertEquals(4, analysis.imports().model().types().size());
    }

    @Test
    void defaultRulesDetectJavacTypeAndPackageCycles() {
        TypeSelector selected = types(LEFT.binaryName(), RIGHT.binaryName());
        assertEquals(RuleStatus.FAILED, CycleRules.typesAreAcyclic(
                analysis.imports().model(), analysis.graph(), selected).check().status());
        assertEquals(RuleStatus.FAILED, CycleRules.packagesAreAcyclic(
                analysis.imports().model(), analysis.graph(), PackageSelector.all()).check().status());
    }

    @Test
    void javacMethodCyclesAreDetectedUnderEverySyntheticPolicy() {
        assertCyclesUnderEverySyntheticPolicy(DependencyKind.METHOD_CALL);
    }

    @Test
    void javacFieldCyclesRespectTheIndependentTypeUsePolicy() {
        assertCyclesUnderEverySyntheticPolicy(DependencyKind.FIELD_TYPE);
        var noTypeUses = CycleRuleOptions.defaults().includingOnly(Set.of(DependencyKind.FIELD_TYPE))
                .withTypeUseDependencies(TypeUseDependencyPolicy.IGNORE);
        assertEquals(RuleStatus.PASSED, CycleRules.typesAreAcyclic(
                analysis.imports().model(), analysis.graph(), types(LEFT.binaryName(), RIGHT.binaryName()),
                noTypeUses).check().status());
    }

    @Test
    void javacLambdaEvidenceIsFilteredWithoutRemovingOrdinaryCallsOnTheSameEdge() {
        Set<MemberId> syntheticMembers = analysis.imports().model().types().stream()
                .flatMap(type -> type.declaredMembers().stream()
                        .filter(member -> member.modifiers().contains(JavaMemberModifier.SYNTHETIC))
                        .map(member -> MemberId.of(TypeId.ofBinaryName(type.binaryName()),
                                member.name(), member.descriptor())))
                .collect(Collectors.toSet());
        assertFalse(syntheticMembers.isEmpty(), "javac must generate a lambda implementation member");
        var calls = analysis.graph().edges().stream()
                .filter(edge -> edge.origin().equals(LEFT) && edge.target().equals(RIGHT)
                        && edge.kind() == DependencyKind.METHOD_CALL)
                .flatMap(edge -> edge.evidence().stream()).toList();
        assertTrue(calls.stream().anyMatch(evidence -> syntheticMembers.contains(
                evidence.ownerMember().orElseThrow())));
        assertTrue(calls.stream().anyMatch(evidence -> !syntheticMembers.contains(
                evidence.ownerMember().orElseThrow())));

        for (SyntheticEdgePolicy policy : SyntheticEdgePolicy.values()) {
            var result = CycleRules.typesAreAcyclic(analysis.imports().model(), analysis.graph(),
                    types(LEFT.binaryName(), RIGHT.binaryName()), CycleRuleOptions.defaults()
                            .includingOnly(Set.of(DependencyKind.METHOD_CALL))
                            .withSyntheticEdges(policy)).check();
            assertEquals(RuleStatus.FAILED, result.status());
            Set<MemberId> owners = result.violations().getFirst().evidence().stream()
                    .map(DependencyEvidence::ownerMember).map(owner -> owner.orElseThrow())
                    .collect(Collectors.toSet());
            assertTrue(owners.contains(MemberId.of(LEFT, "work", "()V")));
            assertTrue(owners.contains(MemberId.of(RIGHT, "work", "()V")));
            assertEquals(policy == SyntheticEdgePolicy.INCLUDE,
                    owners.stream().anyMatch(syntheticMembers::contains));
        }
    }

    @Test
    void javacOneWayDependenciesRemainAcyclicUnderEverySyntheticPolicy() {
        TypeSelector selected = types(LEFT.binaryName() + "$OneWay", RIGHT.binaryName() + "$Leaf");
        assertEquals(2, selected.selectFrom(analysis.imports().model()).selected().size());
        Set<DependencyKind> importedKinds = analysis.graph().edges().stream()
                .filter(edge -> edge.origin().equals(TypeId.ofBinaryName(LEFT.binaryName() + "$OneWay"))
                        && edge.target().equals(TypeId.ofBinaryName(RIGHT.binaryName() + "$Leaf")))
                .map(edge -> edge.kind()).collect(Collectors.toSet());
        assertTrue(importedKinds.containsAll(Set.of(DependencyKind.METHOD_CALL, DependencyKind.FIELD_TYPE)));
        assertEquals(RuleStatus.PASSED, CycleRules.typesAreAcyclic(
                analysis.imports().model(), analysis.graph(), selected).check().status());
        for (SyntheticEdgePolicy policy : SyntheticEdgePolicy.values()) {
            var result = CycleRules.typesAreAcyclic(analysis.imports().model(), analysis.graph(),
                    selected, CycleRuleOptions.defaults().withSyntheticEdges(policy)).check();
            assertEquals(RuleStatus.PASSED, result.status(), policy.name());
            assertTrue(result.violations().isEmpty());
        }
    }

    private void assertCyclesUnderEverySyntheticPolicy(DependencyKind kind) {
        var edges = analysis.graph().edges().stream()
                .filter(edge -> edge.kind() == kind)
                .filter(edge -> Set.of(LEFT, RIGHT).contains(edge.origin())
                        && Set.of(LEFT, RIGHT).contains(edge.target()))
                .toList();
        assertEquals(2, edges.size(), kind.name());
        assertTrue(edges.stream().flatMap(edge -> edge.evidence().stream())
                .allMatch(evidence -> evidence.ownerMember().isPresent()));
        for (SyntheticEdgePolicy policy : SyntheticEdgePolicy.values()) {
            var options = CycleRuleOptions.defaults().includingOnly(Set.of(kind)).withSyntheticEdges(policy);
            var result = CycleRules.typesAreAcyclic(analysis.imports().model(), analysis.graph(),
                    types(LEFT.binaryName(), RIGHT.binaryName()), options).check();
            assertEquals(RuleStatus.FAILED, result.status(), kind + "/" + policy);
            assertEquals(1, result.violations().size());
            assertFalse(result.violations().getFirst().evidence().isEmpty());
            var packages = CycleRules.packagesAreAcyclic(analysis.imports().model(),
                    analysis.graph(), PackageSelector.all(), options).check();
            assertEquals(RuleStatus.FAILED, packages.status(), kind + "/" + policy);
            assertEquals(1, packages.violations().size());
        }
    }

    private static TypeSelector types(String first, String second) {
        return TypeSelector.anyOf(
                TypeSelector.binaryName(JavaPattern.exact(PatternDomain.QUALIFIED_NAME, first)),
                TypeSelector.binaryName(JavaPattern.exact(PatternDomain.QUALIFIED_NAME, second)));
    }

    @Test
    void declarativeCyclesReachTheSameRulesAndAllResultRenderers() throws IOException {
        for (String domain : List.of("types", "packages")) {
            Path configuration = cycleConfiguration(domain, "glob:" + PREFIX + "**", "");
            for (String format : List.of("console", "json", "sarif", "junit-xml")) {
                var output = new StringBuilder();
                var errors = new StringBuilder();
                assertEquals(5, new CliRunner().run(new String[] {"check", "--config",
                        configuration.toString(), "--root", temporaryDirectory.toString(),
                        "--result-format", format}, output, errors), domain + "/" + format);
                assertTrue(output.toString().contains("cycle.detected"));
                assertTrue(errors.isEmpty());
            }
        }
    }

    @Test
    void declarativeCycleSelectionsAreInducedAndEmptySelectionsFail() throws IOException {
        assertEquals(0, runCycle("types", "exact:" + LEFT.binaryName(), "check", "").exit());
        assertEquals(0, runCycle("packages", "exact:" + PREFIX + "left", "check", "").exit());
        assertEquals(4, runCycle("types", "exact:missing.Type", "check", "").exit());
        var explanation = runCycle("types", "glob:" + PREFIX + "**", "explain", "");
        assertEquals(0, explanation.exit());
        assertTrue(explanation.output().contains("kind=CYCLES"));
        assertTrue(explanation.output().contains("synthetic=IGNORE typeUses=INCLUDE"));
        assertFalse(explanation.output().contains("targets="));
    }

    @Test
    void declarativeCyclesRejectIrrelevantFieldsAndRetainImportFailures() throws IOException {
        for (String extra : List.of("mode=no", "targets=glob:**", "self=ignore", "external=ignore",
                "kind=unknown", "synthetic=include")) {
            assertEquals(3, runCycle("types", "glob:" + PREFIX + "**", "validate-config",
                    "rule.fixture." + extra + "\n").exit(), extra);
        }
        Files.write(temporaryDirectory.resolve("classes/Broken.class"), new byte[] {0, 1, 2});
        var result = runCycle("types", "glob:" + PREFIX + "**", "check", "");
        assertEquals(4, result.exit());
        assertTrue(result.output().contains("INCOMPLETE"));
        assertTrue(result.output().contains("cycle.detected"));
    }

    private Path cycleConfiguration(String domain, String selection, String extra) throws IOException {
        Path configuration = Files.createTempFile(temporaryDirectory, "cycles", ".properties");
        Files.writeString(configuration, """
                schema=archunitjava.cli.v1
                inputs=classes
                rules=fixture
                resultFormat=json
                rule.fixture.kind=cycles
                rule.fixture.domain=%s
                rule.fixture.origins=%s
                rule.fixture.displayName=Cycle policy
                rule.fixture.rationale=Keep the selected graph acyclic
                rule.fixture.tags=cycles
                """.formatted(domain, selection) + extra);
        return configuration;
    }

    private Invocation runCycle(String domain, String selection, String command, String extra)
            throws IOException {
        Path configuration = cycleConfiguration(domain, selection, extra);
        var output = new StringBuilder();
        var errors = new StringBuilder();
        int exit = new CliRunner().run(new String[] {command, "--config", configuration.toString(),
                "--root", temporaryDirectory.toString()}, output, errors);
        return new Invocation(exit, output.toString(), errors.toString());
    }

    private record Invocation(int exit, String output, String errors) {}

}
