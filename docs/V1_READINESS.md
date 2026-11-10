# Version 1 release evidence

Status: **PUBLISHED 1.0.0 on 2026-11-10**. The working line is `1.0.1-SNAPSHOT`; the immutable
`0.1.0` beta is unchanged. Feature scope is defined in [the family review](V1_FEATURE_COMPARISON.md)
and [the compatibility contract](COMPATIBILITY.md).

## Required evidence

- [x] #85 reviewed and merged; full original candidate suite passed (372 tests, zero failures/errors,
  two platform/opt-in skips) and upstream CI/CodeQL passed.
- [x] #86 iterative SCC/enumeration, including 10,000-node chains/rings, passes.
- [x] #89 properties cycle rules and real-bytecode CLI regressions pass.
- [x] Clean full suite: 378 tests, zero failures/errors, two expected platform/opt-in skips;
  source and Javadoc candidate artifacts pass doclint.
- [x] Opt-in digest-pinned performance and semantic snapshots pass (383 combined imported types,
  4,832 dependency edges, zero diagnostics; semantic digest unchanged).
- [x] Reviewed SpotBugs baseline matches exactly: 161 notices, fingerprint
  `fc7970533be9e41f9961b7303fa77d0170a4f441c31ea6e85057effcdfc2f555`, zero analysis errors/missing classes.
- [x] API baseline matches 478 public/protected types; an incompatible baseline is rejected.
- [x] Independently compiled basic example (one test) and RAG consumer (five tests) pass.
- [x] Primary artifact is byte-for-byte reproducible across clean local builds (SHA-256
  `b91178bc2c400674245720fe19e11cce1fce11607e88ea5cf15c339eb9e4cdd8`);
  docs site assembles with valid local links.
- [x] Final candidate CI passes on Linux/Windows, including both independent RAG consumers and
  release dry runs; CodeQL passes. Evidence for reviewed head
  `6b0be56e3f304ca3b844366a2984e1266f1836cc`:
  [CI](https://github.com/dragonsec-ai/ArchUnitJava/actions/runs/41524443955),
  [push CI](https://github.com/dragonsec-ai/ArchUnitJava/actions/runs/41524435589),
  [CodeQL](https://github.com/dragonsec-ai/ArchUnitJava/actions/runs/41524444110).
  [PR #90](https://github.com/dragonsec-ai/ArchUnitJava/pull/90) merged this tree to main as
  `89ba4d7932c84f1404714519e673f55273aca8bc`.
- [x] Signed exact-commit tag and protected Central staging validated the `1.0.0` bundle; all
  sixteen checksums and four signatures passed review before publication. Central reports
  `PUBLISHED`, and an independent consumer passed five tests using public `1.0.0` and a fresh cache.
  See [the release record](releases/1.0.0.json) for exact hashes, deployment and workflow IDs.

Publisher membership is separate from repository ownership. Previously the `maven-central`
environment required Tristan's approval; a read-only check on 2026-11-02 confirms that reviewer
still applies. Lukas's namespace access was requested using `lks.niessen@gmail.com` but has not been
independently confirmed. A successful signed staging run is the authoritative credential check.

## Reproduce locally

Use JDK 25 and the checked-in Maven wrapper. Supply `-Dmaven.repo.local=...` when relocating the
repository; also supply `-Darchunitjava.performance.repository=...` for the performance test.

```shell
./mvnw --batch-mode --no-transfer-progress -Prelease-candidate clean verify
pwsh -File scripts/check-public-api.ps1
./mvnw -Dtest=PerformanceBaselineTest -Darchunitjava.performance=true test
./mvnw -Prelease-candidate -DskipTests install
./mvnw -f examples/basic/pom.xml test
```

Run the independent RAG repository with `-Darchunitjava.version=1.0.1-SNAPSHOT` for the development
candidate, or its default `1.0.0` for the published artifact. Build the JAR twice
from clean sources to compare SHA-256 values. Use `scripts/build-pages.ps1` after generating site
Javadocs. CI additionally exercises disposable signing and a Central bundle against an unreachable
loopback endpoint. That tests bundle mechanics without publishing a package.
