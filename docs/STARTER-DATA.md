# Starter data: products and campaigns

**Load Verified Starter Data** (Marketing Product table actions) loads [`src/main/resources/marketing-starter-data.json`](../src/main/resources/marketing-starter-data.json).

- **Verified:** every fact was checked against its source on **2026-10-12**, and every claim records its source and that date.
- **Disagreements:** where sources disagree, the claim is *Disputed*, with the discrepancy written out.
- **Campaigns:** all start as Draft. No results, customers, testimonials or benchmarks are included.
- **Re-running:** loading is idempotent. Existing products (by slug), channels and the policy are left alone.

Re-verify before use. The readiness check flags sources older than `staleSourceDays` (90).

## QQQ: ready for adoption marketing of the framework

- **Release:** 4.0.0 is GA (2026-09-24), Apache-2.0, on Maven Central. 4.1 is in release candidates (RC.4, 2026-10-12).
- **Previews:** qRun hosting, qStudio and production qBits are previews or planned.
- **Sources:**
  - https://www.qrun.io
  - https://www.qrun.io/try
  - https://www.qrun.io/docs/getting-started/installation
  - https://github.com/QRun-IO/qqq/releases/tag/v4.0.0
  - https://github.com/QRun-IO/qqq (README)
  - Maven Central metadata
  - https://www.qrun.io/pricing
  - https://www.qrun.io/licensing
- **Disputed (fix before campaigns):**
  - The site's share titles say "QQQ 4.0 Preview" and its framework card says "release candidate", while GitHub marks 4.0.0 GA.
  - "Next is the default dashboard" (develop README) contradicts "Material is standard, Next is a preview" (4.0.0 notes).
  - The website never names the license; the repository's is Apache-2.0.
- **Campaigns:**
  - *QQQ 4.0: from quickstart to your first table* (open-source adoption): a tutorial to /try. Activation and awareness are not instrumented yet.
  - *QQQ support inquiries* (commercial demand): runs separately. Explicit lead rule: at least one attributed contact-form submission plus email consent. It states truthfully that the release has no SLA and that support is negotiated.

## gclo: ready for adoption marketing on Windows 11 x64 only

- **Release:** MIT. 1.1.0 (2026-10-11): Setup, Portable and CLI zip. Scoop works.
- **Not available yet:**
  - winget: PR microsoft/winget-pkgs#450621 is open.
  - Chocolatey: only 1.0.1 is listed, unapproved. The README lists Chocolatey anyway (Disputed).
  - Homebrew, and macOS or Linux binaries.
- **Official repository:** github.com/KofTwentyTwo/gclo. gclo-macos is private, with only an initial commit.
- **Sources:**
  - https://github.com/KofTwentyTwo/gclo
  - its releases
  - docs/CLI.md
  - https://community.chocolatey.org/packages/gclo
  - https://github.com/microsoft/winget-pkgs/pull/450621
- **Campaign:** *gclo 1.1 for Windows: mirror a whole GitHub org* (open-source adoption). It shows the task end to end on Windows 11 and mentions no unavailable install routes. gclo has no telemetry, so activation is not instrumented, and downloads are interest, not adoption.

## MarkdownViewr for Windows: prelaunch

- **Status:** no GitHub release, installer, winget or Store listing.
- **Tags:** v1.0.0 and v1.0.0-beta.1 exist, but both release runs failed at startup on 2026-10-12 (Disputed: a tag is not a release).
- **README wording:** it says "built with AppKit and WinUI 3", which is misleading; the app is WinUI 3 on .NET 10 (Disputed).
- **Separate product:** do not conflate it with the macOS markdownViewr.
- **Sources:**
  - https://github.com/KofTwentyTwo/MarkdownViewr.Windows (README)
  - its releases page
  - docs/user-guide.md
- **Features:** all *Planned* until released.
- **Campaign:** *MarkdownViewr for Windows: early testers* (prelaunch research and tester interest). It points to the repository's Discussions, describes features as planned, and has no adoption metrics until a release exists.

## Channels and policy

Channel AI policies follow the owner decisions in `DESIGN.md` (2026-10-12, recorded 2026-10-13).

- **Controlled test sink:** test only. AI content *Allowed*.
- **Email to opted-in CRM list:** needs a configured, verified QQQ messaging provider (Amazon SES). AI content *Allowed with disclosure*.
- **Project blog (posted by hand):** no connector. AI content *Allowed with disclosure*.
- **GitHub release notes (posted by hand):** no connector. AI content *Allowed*; a maintainer checks every line against the release.
- **No community channels:** none are seeded, because their current rules have not been checked. Agent-written text is *Not allowed* on them by policy.
- **Policy row `default`:** agent and execution on, 1,000,000 agent tokens per day, USD 25 monthly cost limit, 2 concurrent runs, 24 tool calls per run.
