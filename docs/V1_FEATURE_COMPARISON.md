# Version 1 feature review

Reviewed 2026-11-02 against current upstream main commits (read without changing sibling working
trees): [Ruby 288645b](https://github.com/dragonsec-ai/ArchUnitRuby/tree/288645b75c409a87efa0b09ce5a1db61e950e71e),
[Python 62d620a](https://github.com/LukasNiessen/ArchUnitPython/tree/62d620a7e34a2895d9574aee7f3ee07a9fedef79),
[TypeScript 961ceed](https://github.com/LukasNiessen/ArchUnitTS/tree/961ceeddb7a128e642dd6a1f855b4a7cca264428).
Java starts from the reviewed #85 head `d2ee1a1`; candidate changes are recorded in the linked issues.
This compares documented capabilities and implementation entry points, not identical semantics or
independent certification of the sibling libraries.

| Capability | Ruby / Python / TypeScript | Java v1 candidate | Decision |
| --- | --- | --- | --- |
| Dependency direction, cycles, naming | Present in all three | Present; ordinary-member cycle bug fixed in #85 | Required |
| Empty-selection protection, exclusions, rationale | Present in all three | Present; strict imports retain errors | Required |
| Layers, slices, PlantUML component rules | Present in all three | Present; bounded diagram subset | Required |
| Graph reports | All have DOT, Mermaid, D2, CSV, JSON, HTML | Same six graph formats; bounded/escaped deterministic output | Present |
| Test integration and custom rules | Native frameworks and custom predicates | JUnit assertions/engine and `ArchitectureRules.define` | Present |
| Counts and dependency/distance metrics | Present; formulas and source models vary | Declaration/source-line counts, coupling, instability, abstractness, distance and cumulative metrics | Present |
| Cohesion metrics | Ruby/Python expose eight named variants; TS documents LCOM96a/b | CK LCOM1, LCOM4, Henderson–Sellers; explicit availability policies | Three supported formulas suffice; additional variants later |
| Metric thresholds and extension | Siblings offer broader fluent comparisons/custom metric functions | Inclusive at-most/at-least thresholds over fixed metric IDs; custom rules can evaluate other calculations | Broader metric API later |
| Offline metrics HTML | Present in all three | Graph HTML and structured rule reports; no dedicated metrics dashboard | Later enhancement |
| Logging and readable failure output | Present in all three | Opt-in inspection and console formatting since the unreleased diagnostics change | Present |
| Language-specific model | Ruby require/constants; Python imports/type checking; TS imports/Nx | Bytecode members/calls, annotations, generics, records, sealed types, JPMS, lambdas | Preserve Java semantics rather than copy source-language features |
| Reviewed baselines and advanced Java rules | Not a uniform family feature | Baselines/suppressions, reachability, member access, public interface rules, presets | Already beyond the common feature floor |
| CLI/build use | Ruby upstream lists standalone CLI as roadmap; Python config; TS framework API | Properties/JUnit and CLI; add cycle kind in #89; Maven/Gradle invocation bridges | No standalone build plugins required for v1 |

## Release blockers and work order

1. [#85](https://github.com/dragonsec-ai/ArchUnitJava/pull/85): preserve ordinary cycle evidence;
   merged after 372 local tests and successful upstream CI/CodeQL.
2. [#86](https://github.com/dragonsec-ai/ArchUnitJava/issues/86): iterative cycle traversal; three
   10,000-node regressions first demonstrated stack overflow.
3. [#89](https://github.com/dragonsec-ai/ArchUnitJava/issues/89): declarative type/package cycle rules,
   exercised on compiled Java through all result formats.
4. [#88](https://github.com/dragonsec-ai/ArchUnitJava/issues/88): explicit compatibility scope and
   reviewed API baseline before freezing 1.0.
5. [#87](https://github.com/dragonsec-ai/ArchUnitJava/issues/87): metadata and final candidate gates,
   including upstream Linux/Windows CI and the protected publishing process.

Java 25 remains the supported runtime. A lower runtime floor would require a separately justified
backend project. Full sibling metric parity, metrics HTML, and independently published Maven/Gradle
plugins are useful follow-ups, not reasons to delay a correct, tested and clearly scoped v1.
