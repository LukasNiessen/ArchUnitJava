# Version 1 release candidate

Status: **NOT YET APPROVED FOR PUBLICATION**. The working line is `1.0.0-SNAPSHOT`; public Maven
Central `0.1.0` is unchanged. Feature scope is defined in [the family review](V1_FEATURE_COMPARISON.md)
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
- [ ] Final integrated Linux/Windows CI and CodeQL pass.
- [ ] Signed exact-commit tag and protected Central staging validate the prospective `1.0.0` bundle;
  a maintainer reviews signatures, checksums, metadata and immutable coordinates before publication.

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

Run the independent RAG repository with `-Darchunitjava.version=1.0.0-SNAPSHOT`. Build the JAR twice
from clean sources to compare SHA-256 values. Use `scripts/build-pages.ps1` after generating site
Javadocs. CI additionally exercises disposable signing and a Central bundle against an unreachable
loopback endpoint. That tests bundle mechanics without publishing a package.
