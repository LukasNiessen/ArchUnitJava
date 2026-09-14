# Maintainer handover

Review date: 2026-09-14. Proposed recipient: [Lukas Niessen](https://github.com/LukasNiessen).
This document prepares a transfer; it does not record a completed ownership change.

## Before transferring

1. Resolve the destination name. `LukasNiessen/ArchUnitJava` already exists as a separate,
   non-fork repository with `README.md`, `AGENTS.md`, and `CLA.md`. Its owner should preserve any
   relevant guidance and rename it before accepting this repository under that name. Transferring
   under a different unused name is another option. Do not overwrite or delete the existing repo.
2. Decide whether the independent `ArchUnitJava-TestRepo-RAG` should move too. The library's CI
   checks it out explicitly, and its standalone CI tests the published Maven artifact.
3. Arrange Maven Central publishing access separately. The existing artifact is
   `io.github.tristankruse:archunitjava:0.1.0`; a GitHub transfer does not change that coordinate or
   automatically grant namespace access. Prefer retaining the coordinates for consumer continuity.
   Use Central Portal organization membership and namespace permissions, or Central Support if the
   necessary organization controls are unavailable. Lukas should use his own publisher account.
4. Review release credentials and approval. The `maven-central` environment currently requires
   TristanKruse approval and contains the five secret names listed in [RELEASE.md](RELEASE.md).
   GitHub keeps secrets associated with a transferred repository. Agree on replacing the Central
   token, signing-key custody, and the release approver before running any release under new
   ownership. Never copy secret values into repository files or handover notes.
5. Verify analytics access. Publisher Insights was enabled for `io.github.tristankruse`. Access to
   the correct Maven package in Scarf and its public badge still needs independent verification.
   The README currently links to the analytics sources but contains no live download-count badge.
   Repository transfer alone does not transfer external analytics accounts.

## Repository quality and release follow-up

- Dependabot PRs #76 and #77 were reviewed and merged: Maven Surefire 3.6.0 and Compiler 3.16.0.
- The focused CLI audit reproduced and corrected dotted rule-ID rejection and successful partial
  analysis after corrupt input. Regression tests cover strict checks, all six graph renderers,
  explicit partial-analysis opt-in, retained violations, and harmless input duplication.
- These fixes are on the development line. Maven Central `0.1.0` remains immutable and does not
  contain them. Plan a subsequent beta release after choosing publisher access and ownership.
- The current source uses MIT; the already published `0.1.0` artifact retains its original Apache
  2.0 metadata. Keep that distinction explicit until a subsequent release is published.
- CodeQL is active. At the review date, Dependabot security alerts/security updates and secret
  scanning/push protection were disabled. Enable the appropriate GitHub security features and
  review any resulting alerts. Scheduled dependency-version PRs are already enabled.
- No repository rulesets were present at the review date. A protected default branch is an
  optional next step when more maintainers contribute; it should fit the team's merge workflow.
- The public API remains provisional and JDK 25 remains required. A repository transfer is not
  a reason to claim 1.0 stability or change the runtime requirement.

## Transfer choices

The recommended option for the current family layout is GitHub's native transfer to
`LukasNiessen`, after resolving the name conflict. It preserves history, issues, PRs, stars, and
release records. Lukas must accept the personal-account transfer invitation within one day.
The original owner remains a collaborator. No transfer invitation has been sent during this review.

A shared GitHub organization is an alternative if the whole ArchUnitEverything family should have
multiple administrators. It requires a separately agreed organization and repository-creation
permission. Copying code into the existing repository would not preserve this repository's issue,
PR, and release continuity in the same way.

## After acceptance

- Update local Git remotes, README badges/links, the repository homepage, POM project/SCM metadata,
  issue-template links, support links, and the documentation site's social-preview URL.
- Deploy and verify Pages at the recipient's site URL. Repository URLs redirect after transfer;
  GitHub Pages URLs do not. Decide whether to keep a separate redirect site at the old Pages URL.
- Update the RAG repository links and the explicit checkout in `.github/workflows/ci.yml` if the
  fixture also moves. Test both the development candidate and the published artifact.
- Review `.github/workflows/prepare-release.yml`, which currently sets Tristan's tagger identity,
  and the release environment's approver. Preserve attribution on existing commits and tags.
- Confirm CI, documentation deployment, CodeQL, Central permissions, signing, and analytics from
  the new maintainer's account before the next release. Use a staging/dry-run first.

References: [GitHub repository transfers](https://docs.github.com/en/repositories/creating-and-managing-repositories/transferring-a-repository),
[Central Portal organizations and namespace permissions](https://central.sonatype.org/publish/publish-portal-organizations/),
and [Scarf's Maven integration](https://docs.scarf.sh/package-registry-integrations-maven-central/).
