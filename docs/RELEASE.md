# Release status

**Decision: PUBLISHED 1.0.** Version `1.0.0` was published on 2026-11-10 as
`io.github.tristankruse:archunitjava:1.0.0`, under MIT, with JDK 25 required at runtime.

The signed `v1.0.0` tag targets `970d5f79005153b229ab526203482138446a4756`.
[Tag preparation](https://github.com/dragonsec-ai/ArchUnitJava/actions/runs/42804890008) and
[Central staging](https://github.com/dragonsec-ai/ArchUnitJava/actions/runs/42806337865) passed.
The exact non-snapshot candidate passed 378 tests (zero failures/errors, one Linux opt-in skip),
doclint and the public API gate. All sixteen checksums and four signatures were independently
verified against release key `8F7C42989C49216FA75523251BB3BFA38C776312`.

[Publication](https://github.com/dragonsec-ai/ArchUnitJava/actions/runs/42809671438) used the reviewed
deployment `0aa00785-a3b2-4560-b3f7-74463a3fc56f`. Central subsequently reported `PUBLISHED`.
Public POM/JAR SHA-256 values match the retained bundle, and the separate RAG consumer passed all
five tests against public `1.0.0` using a fresh Maven repository. The checked-in
[machine-readable review record](releases/1.0.0.json) contains all artifact hashes and run IDs.

See the [completed v1 checklist](V1_READINESS.md) and [compatibility contract](COMPATIBILITY.md).
The development line has advanced to `1.0.1-SNAPSHOT`.

## Historical 0.1.0 beta record

The following record describes the immutable beta; its test counts and account configuration are
historical, not current candidate evidence.

**Decision: PUBLISHED PUBLIC BETA.** Version `0.1.0` became publicly available from Maven Central on
2026-09-01.

The immutable coordinates are `io.github.tristankruse:archunitjava:0.1.0`. The protected release
workflow uploaded a signed, user-managed deployment with automatic publishing disabled. Central
validated the deployment, a human reviewed the retained bundle, and publication was confirmed only
after the immutability warning.

The release is deliberately presented as a public beta. Publication proves that installation and
release mechanics work; it does not promote the provisional pre-1.0 Java API to a stable contract.

## Evidence available

- The current suite contains 353 tests with zero failures or errors; two platform/opt-in cases are
  intentionally skipped in a standard run. The release audit also exercises the performance suite.
- Windows and Linux CI run the full JDK 25 Maven verification and an independent RAG consumer.
- The primary JAR has a fixed output timestamp and is rebuilt byte-for-byte in CI.
- Source and Javadoc JARs are attached. Javadoc doclint enforces every category except missing
  record-component tags; Central requires real Javadoc content but does not require tag-complete
  prose.
- Package checks verify the CLI entry point, JUnit Platform service entry, and automatic module
  name.
- SpotBugs reports no analysis errors, missing classes, mutable-input exposure, null-path, resource,
  or constructor-safety findings. CI rejects every category outside the reviewed immutable-collection
  accessor and intentional natural-order baseline.
- `examples/basic` tests the current locally installed development candidate. The separate RAG
  repository resolves public `0.1.0` directly from Maven Central in its own Linux and Windows CI;
  library CI overrides that version when exercising the current development candidate.
- The official Central Portal Maven plugin creates a user-managed bundle. GPG uses best-practices
  mode, and release CI obtains credentials and passphrases only through environment-backed GitHub
  secrets.
- Coordinates use the GitHub-verifiable `io.github.tristankruse` namespace and include license,
  developer, SCM, project, and dependency metadata required by Central.
- Performance inputs are version/digest pinned and semantic snapshots cover real open-source
  bytecode, including `package-info.class` through rules and metrics.
- The adversarial corpus verifies target-code non-execution, containment, resource limits, opaque
  caches, output escaping, baseline ingestion, and value invariants.

## Findings resolved after the first no-go review

1. Canonical baseline JSON now has a bounded strict reader for UTF-8 text, bytes, and regular files.
   It rejects unknown and duplicate fields, unknown schemas, invalid Unicode and dates, excessive
   bytes/depth/counts/strings, malformed fingerprints, and non-regular paths.
2. `package-info.class` has a valid stable type identity and now flows through the real-bytecode
   type-rule and component-metric pipeline without benchmark exclusions.
3. `JavaPattern.regex` accepts a conservative bounded subset. Unrestricted JDK regex is available
   only through the explicitly named `trustedRegex` API; CLI configuration still permits exact and
   bounded glob patterns only.
4. CSV is spreadsheet-safe by default. Lossless, non-neutralized interchange requires the explicit
   `renderMachineReadable` API and is documented as unsuitable for direct spreadsheet opening.
5. Maven metadata now uses a verifiable namespace and the build contains an official Central Portal
   staging configuration plus a tag- and confirmation-gated workflow.
6. Javadoc generation now fails on actionable doclint warnings while treating missing
   record-component tags as a documentation backlog rather than a semantic release failure.

## Publisher integration and v0.1.0 publication

Publisher/account setup was completed on 2026-09-01:

- Central Portal access uses the `TristanKruse` GitHub identity, and
  `io.github.tristankruse` is verified.
- The protected `maven-central` GitHub environment requires `TristanKruse` approval before release
  jobs can access its secrets.
- `MAVEN_CENTRAL_USERNAME`, `MAVEN_CENTRAL_PASSWORD`, `MAVEN_GPG_PRIVATE_KEY`,
  `MAVEN_GPG_PASSPHRASE`, and `MAVEN_GPG_KEY_FINGERPRINT` are configured as environment secrets.
- The dedicated signing key has fingerprint `8F7C42989C49216FA75523251BB3BFA38C776312`.
  Its public key is available from `keyserver.ubuntu.com`; its private key and passphrase are not
  stored in the repository.
- The Central token expires on 2027-09-01 and the signing key on 2028-08-31. Rotate each credential
  before its expiry without changing published component coordinates.

Publication evidence:

- Signed tag `v0.1.0` resolves to reviewed commit
  `631e4506e99b8604b096971792fadba9e5edb55d`; independent GPG verification returned `GOODSIG` and
  `VALIDSIG` for the dedicated signing-key fingerprint.
- [GitHub Actions staging run 33523489977](https://github.com/dragonsec-ai/ArchUnitJava/actions/runs/33523489977)
  verified the tag, materialized `0.1.0`, ran 347 tests with zero failures or errors, signed the four
  release artifacts, uploaded the bundle, and waited for Central validation.
- Central deployment `12dfc7c7-4015-4fbf-887e-bc3103ff6560` validated one out of one components and
  reached `PUBLISHED` after explicit human confirmation.
- The retained bundle contains the primary, source, and Javadoc JARs plus the generated POM. All 16
  MD5, SHA-1, SHA-256, and SHA-512 checksum files matched, and all four detached signatures verified
  against fingerprint `8F7C42989C49216FA75523251BB3BFA38C776312`.
- The public POM and artifacts resolve from
  [Maven Central](https://central.sonatype.com/artifact/io.github.tristankruse/archunitjava/0.1.0).

For future releases, `prepare-release.yml` creates and verifies a signed tag without copying the
passphrase to a maintainer workstation. `release.yml` then checks out that immutable tag, verifies
its signature, uploads a user-managed deployment, and waits for validation. Reviewing and publishing
the validated deployment remain separate human decisions because Central versions cannot be
replaced or deleted.

## Known limitations that remain part of the 0.1.0 contract

- JDK 25 is required at runtime. Java 8–25 describes analyzed `javac` bytecode, not the runtime
  requirement.
- The lower-level public API is provisional before 1.0 and is not compatible with ArchUnit's API.
- The pinned performance corpus is modest. The project makes no absolute throughput or heap claim.
- `trustedRegex` and machine-readable CSV are intentionally sharp tools whose names and Javadocs
  expose the trust boundary.
- Static analysis cannot observe reflection-only, native, generated-at-runtime, or string-assembled
  dependencies.

## Local review commands

For ownership changes, see the [maintainer handover](MAINTAINER_HANDOVER.md) for repository,
publishing, signing, documentation, and analytics dependencies.

```shell
./mvnw --batch-mode --no-transfer-progress verify
./mvnw --batch-mode --no-transfer-progress -Prelease-candidate verify
./mvnw --batch-mode --no-transfer-progress -Prelease-candidate install
./mvnw --batch-mode --no-transfer-progress -f examples/basic/pom.xml test
```

The `release-dry-run` CI job materializes a non-snapshot version, creates a disposable signing key,
and runs the real Central plugin against an unreachable loopback endpoint. The expected upload
failure occurs only after bundle creation; CI then verifies the signed primary, source, Javadoc, and
POM entries in `central-bundle.zip`. This exercises the bundler while making a remote upload
impossible. The protected release workflow performs the corresponding credentialed upload with
`autoPublish=false`.

After reviewing the retained bundle, `publish-release.yml` can inspect or publish the exact
deployment ID using the same protected environment. It verifies the signed release tag and
accepts only the expected Maven coordinate in a validated or already publishing state. Credentials
remain environment-backed; inspect mode never requests publication. Once published, confirm the
public artifacts from a clean Maven repository before marking the GitHub release stable.

Central can return an empty `purls` list once a deployment reaches `PUBLISHED`. In that state the
publisher requires the exact committed `docs/releases/<version>.json` review record to recover the
original coordinate. An unpublished/validated deployment must still return exactly the requested
coordinate; a review record cannot bypass that check. Keep the review record from the verified
bundle before future publications.
