package dev.archunitjava.rules;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import dev.archunitjava.cli.CliAnalysisResult;
import dev.archunitjava.cli.CliAnalyzer;
import dev.archunitjava.cli.CliConfigurationLoader;
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
}
