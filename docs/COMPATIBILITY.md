# Compatibility policy

## Runtime and build JDK

The published `1.0.0` release requires JDK 25. The implementation uses the final
`java.lang.classfile` API and the Maven build enforces `[25,26)`. Java 17 or 21 runtime support would
require a different bytecode backend or a multi-release implementation and is not claimed.

## Analyzed bytecode

Checked-in extraction snapshots cover `javac` output for Java 8 through Java 25, class-file major
versions 52 through 69. Records, sealed types, modules, nesting, generics, annotations, compiler
artifacts, method handles, dynamic constants, and `invokedynamic` have dedicated fixtures. Future
class-file versions are unsupported until the parser and corpus are updated. Older class versions,
alternate compilers, post-processors, and obfuscators may work but are outside the verified claim.

Malformed or semantically incomplete inputs produce explicit diagnostics. The library does not load
target classes to fill gaps. Missing external types remain explicit and rule policy decides whether
to ignore, fail, or treat them as non-matching.

## JPMS

ArchUnitJava models `module-info.class`, requires, exports, opens, uses, provides, automatic/unnamed
placement, readability, and observed cross-module dependencies. This is static evidence, not a
replacement for JVM resolution. Runtime layers, command-line `--add-reads`/`--add-exports`, agents,
and reflective access can differ from the analyzed descriptors.

The library artifact contains no `module-info.class`. Its manifest declares
`Automatic-Module-Name: dev.archunitjava`, giving JPMS consumers a stable module name.
JUnit Platform integration is an optional dependency and a service-provider entry is packaged.

## API and ecosystem compatibility

The pre-1.0 API was provisional. Version 1 follows the contract below; release notes identify
compatibility changes. ArchUnitJava does not claim compatibility
with the original ArchUnit API or its rule semantics. Configuration and report formats carry their
own schema versions and reject unknown schemas where ingestion exists.

## Version 1 contract

Version `1.0.0` was published on 2026-11-10 after the [release gates](V1_READINESS.md) passed.
The development line is `1.0.1-SNAPSHOT`. The following contract applies to the published v1 line:

- Public/protected declarations in the primary artifact form the supported Java API. Package-private
  implementation and generated files are excluded. Public records' constructors/components are
  API, including the preserved pre-cycle `CliRuleConfiguration` constructor.
- Compatible additions may ship in minor releases. Removing or changing existing signatures,
  changing required configuration fields, raising the runtime floor, or making other incompatible
  contract changes requires a major release. Fixing a documented rule's incorrect result is a bug
  fix: cycle checks may correctly fail after an upgrade when earlier versions missed a violation.
- The v1 runtime/build baseline is JDK 25, with supported analyzed `javac` releases 8–25 as above.
  Other runtimes, compilers, obfuscators and future class-file versions are not promised.
- Maven coordinates remain `io.github.tristankruse:archunitjava` despite the organization transfer.
  The automatic module name remains `dev.archunitjava`. Consumers need no publishing account.
- Existing `archunitjava.cli.v1` dependency configurations keep their defaults. The new cycle kind is
  additive; older beta readers reject that new field. Incompatible ingestion changes use a new
  schema. CLI exit codes 0/2/3/4/5 preserve their documented meanings.
- Structured report IDs and schema-versioned fields are contracts; exact human prose, stack traces,
  wall times, HTML/CSS layout and diagnostic ordering beyond documented deterministic sorting are
  not byte-for-byte compatibility promises. Consumers should tolerate additive output fields.
- No Java serialization format, original ArchUnit API compatibility, complete dynamic-dependency
  detection, or fixed performance/heap guarantee is promised.

`scripts/check-public-api.ps1` extracts public/protected declarations, descriptors, generic
signatures and constants using JDK 25 `javap` without initializing the analyzed classes. CI checks
them against `docs/api/v1-public-api.txt`. The gate deliberately flags additions too, requiring a
reviewed `-Update` change. It is a signature regression gate, not a proof of every behavioral or
binary compatibility property: tests and release review still cover semantics and schemas.

## Decision

The `1.x` line retains JDK 25 instead of adding a third-party parser solely to lower the
runtime floor. That keeps the dependency surface small and the extraction behavior anchored to the
JDK API. The decision should be reconsidered only with usage data and a separately tested backend;
lowering the runtime floor requires a separately tested backend.
