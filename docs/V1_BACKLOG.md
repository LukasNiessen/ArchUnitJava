# Version 1.0 work items

Completed for the `1.0.0` publication on 2026-11-10. See [release evidence](V1_READINESS.md).

## Stack-safe cycles — #86

Replace recursive SCC and elementary-cycle traversal with explicit stacks. Preserve ordering,
bounds and evidence. Verify 10,000-node chains/rings, long cycle paths, and existing cycle tests.

## Declarative cycle rules — #89

Expose type/package cycle rules in the properties/JUnit path. Preserve dependency configurations
and constructors. Reject irrelevant fields. Test compiled cycles, clean/empty selections,
incomplete imports, explanations and exit codes. Document the induced graph and synthetic defaults.

## Compatibility contract — #88

Pin the family feature comparison. Document Java API, schema and runtime promises. Check a
deterministic public/protected API signature baseline in CI before freezing version 1.0.

## Candidate validation — #87

Update repository/Pages metadata for dragonsec-ai. Run full tests, performance corpus, Javadocs,
static analysis, reproducibility and independent consumers. Record CI and operational release
checks separately from the immutable published 0.1.0 beta.
