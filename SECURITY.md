# Security policy

## Supported versions

Version `0.1.0` is a published public beta. Security fixes currently target `main` and will be
included in a new release, not retroactively applied to the immutable `0.1.0` artifact. No
long-term support commitment or stable release line exists before 1.0.

## Reporting a vulnerability

Please use GitHub's private
[security-advisory form](https://github.com/LukasNiessen/ArchUnitJava/security/advisories/new).
Do not include exploit details, malicious bytecode, credentials, or private repository content in a
public issue.

Include the affected commit or version, operating system and JDK, the smallest safe reproduction,
the expected trust boundary, and the observed impact. Reports involving target-code execution,
approved-root escapes, archive/resource-limit bypasses, cache poisoning, output injection, baseline
parser bypasses, or unexpected remote publication are especially important.

Receipt and remediation timing depend on severity and maintainer availability. Acknowledgement,
triage status, and coordinated-disclosure timing will be communicated through the private advisory.
Published artifacts are immutable, so a released vulnerability is fixed in a new version rather
than by replacing an existing Maven Central component.

The current security boundary and accepted residual risks are documented in
[`docs/THREAT_MODEL.md`](docs/THREAT_MODEL.md).
