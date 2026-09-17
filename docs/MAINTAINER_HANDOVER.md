# Maintainer handover

Status as of 2026-09-17: the GitHub repository transfer is complete.
[Lukas Niessen](https://github.com/LukasNiessen) owns
[`LukasNiessen/ArchUnitJava`](https://github.com/LukasNiessen/ArchUnitJava).
Tristan Kruse remains a collaborator. The independent
[`TristanKruse/ArchUnitJava-TestRepo-RAG`](https://github.com/TristanKruse/ArchUnitJava-TestRepo-RAG)
was deliberately not transferred; library CI continues to check it out from Tristan's account.

## Completed repository cleanup

- Live repository, issue, discussion, security, badge, support, POM/SCM, and documentation links
  point to Lukas's repository. Historical publishing facts and the RAG fixture's separate owner
  remain unchanged.
- The repository homepage and GitHub Pages site use
  <https://lukasniessen.github.io/ArchUnitJava/>. GitHub does not redirect the former Pages URL;
  it currently returns 404. Git repository and Git activity URLs redirect, but other local clones
  should update their remotes to `https://github.com/LukasNiessen/ArchUnitJava.git`.
- Release-tag automation uses the workflow initiator as tagger rather than hard-coding Tristan.
  Existing signed tags and their attribution have not been modified. A future tag's GitHub
  verification also depends on the signing key's identity matching the new tagger address.

## Remaining operational handover

1. Arrange Maven Central publishing access separately. The published artifact remains
   `io.github.tristankruse:archunitjava:0.1.0`, and the project should retain that coordinate for
   consumer continuity unless a deliberate migration is planned. GitHub transfer does not grant
   Central Portal namespace access. Lukas should use his own publisher account; arrange organization
   membership and namespace permissions or ask Central Support if the necessary controls are absent.
2. Change the `maven-central` GitHub environment's required reviewer if Lukas should approve
   releases: it still names TristanKruse as of this review. The five environment secrets listed in
   [RELEASE.md](RELEASE.md) remain part of the release setup. GitHub retains repository secrets
   during transfer, but the Central token, signing-key custody, and approval policy need an explicit
   decision before Lukas starts a release. Only Lukas has repository-admin access to change the
   environment's reviewer. Never copy secret values into repository files or handover notes.
3. Confirm the new maintainer's access to Publisher Insights for `io.github.tristankruse` and the
   correct Maven package in Scarf. These external accounts do not transfer with GitHub. The README
   links to analytics sources but has no live download-count badge.
4. Confirm CI, GitHub Pages, CodeQL, Dependabot, secret scanning, and the RAG consumer remain healthy
   after this cleanup. Before the next release, run a staged/dry-run publishing check from the new
   maintainer's account and review the resulting Central bundle before publication.

## Product and release context

- Dependabot PRs #76 and #77 updated Maven Surefire to 3.6.0 and Compiler to 3.16.0.
- The focused CLI audit corrected dotted rule-ID rejection and successful partial analysis after
  corrupt input. Tests cover strict checks, six graph renderers, explicit partial-analysis opt-in,
  retained violations, and harmless input duplication.
- These fixes are on the development line. Published Maven Central `0.1.0` is immutable and does
  not contain them. Plan a subsequent beta release after publishing access is settled.
- Current source uses MIT; published `0.1.0` retains its original Apache 2.0 metadata. The public
  API remains provisional and JDK 25 remains required.
- CodeQL, Dependabot vulnerability alerts/security updates, secret scanning, and push protection
  were active before transfer. The two optional broader secret-scanning modes remained disabled.
  Review the new owner's security settings and any resulting alerts.
- No repository ruleset was present before transfer. A protected default branch is optional once
  the team agrees on its review workflow.

References: [GitHub repository transfers](https://docs.github.com/en/repositories/creating-and-managing-repositories/transferring-a-repository),
[Central Portal organizations and namespace permissions](https://central.sonatype.org/publish/publish-portal-organizations/),
and [Scarf's Maven integration](https://docs.scarf.sh/package-registry-integrations-maven-central/).
