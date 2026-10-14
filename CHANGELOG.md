# Changelog

All notable changes to this project are documented here. The format follows Keep a Changelog; versions follow semantic versioning.

## [0.1.0] - Unreleased

### Changed
- Make the agent limits distributed (#4): `maxConcurrentAgentRuns` counts Running `marketing_agent_run` rows in the database (a new `heartbeatAt` column, updated on each model call, lets runs whose server stopped be marked Failed first), and `maxModelCallsPerMinute` counts model-call steps in the last 60 seconds and makes a run wait rather than fail. The in-memory semaphore and rate window are gone; the settings and Marketing Policy override keep their names and defaults (the policy `maxConcurrentAgentRuns` override now applies). Hosts add `heartbeat_at` to `marketing_agent_run`. The count-then-insert window is still racy; a conditional-update claim (qqq#1178) is a follow-up.
- Move to the assistant provider SPI `0.1.0-RC.12` (package `com.kingsrook.qbits.assistant.spi`) and `qbit-build-parent` `2.1.2`, and drop the per-repository guardrail allow-lists. `qbit-assistant-provider-openai` becomes a test dependency; hosts that register it add it themselves.
- Switch the optional qbit-email integration to the published `com.kingsrook.qbits.email.api` package in `0.1.0-RC.3` (fixes #13). Preserve MARKETING messages, idempotency keys, execution links and outcome mapping; use typed readiness failures so missing providers or brand postal addresses remain send-time rejections. Upgrade `qbit-build-parent` to `2.1.1` and remove all per-repository guardrail allow-lists.
- Resolve architecture finding F5 (#9): depend on `qbit-assistant-provider-api` and `qbit-assistant-provider-openai` `0.1.0-RC.11`, with no full assistant dependency, including in tests.
- Replace the local OpenAI-compatible implementation with the published provider and document `ProviderSettings`, environment-key resolution and provider-name migration. Agent tests use a local scripted double.
- Upgrade QQQ to `4.1.0-RC.6`; enable architecture enforcement and strict UI API routes.

### Added
- **Persisted measurement** (issue #4, item 4): the `marketing_metric_daily` sub-table (**Daily Results** on a campaign; system-written, read-only, product-locked) rolls outcome events up per UTC day, stage, event type and test-data flag. `MeasurementRollupService` recomputes the affected campaign-days idempotently after every event insert, update and delete; **Rebuild Results** (campaign record action) rebuilds every day. `MeasurementReport` sums raw counts from the rollup and counts campaign-wide unique actors with a backend `COUNT_DISTINCT`; its output is unchanged. An event insert now defaults `occurredAt` to now and `isTestData` to false, and a blank `actorKey` is stored as null.
- **Outcome importer SPI:** `MarketingOutcomeImporter`, registered with `MarketingQBitConfig.withOutcomeImporter(...)`, and the **Import Outcomes** campaign action, which stores outcomes through the normal (source, external id) dedupe. The Configuration Status widget lists the importers. No concrete network importer.
- Help bundle `help/qbit-marketing.yaml` (346 entries) covering the app, tables, fields, actions, widgets and possible-value sources, with `HelpBundleCoverageTest` as the coverage gate until `HelpBundleAssertions` ships in core (qqq#1198). The producer registers it once that API exists.
- Product-contingent possible values for editable campaign, claim, finding, metric and outcome-event references.
- Repository scaffold: build, CI and the repository standard files.
- **Model:** 19 standard `marketing_*` tables, 21 enum possible-value sources, joins and child lists. The PostgreSQL reference DDL (`docs/ddl/postgres.sql`) and an H2 twin are generated from one spec (`scripts/generate-model.py`).
- **Products and evidence:**
  - Briefs, audiences, evidence sources with verification dates, and claims (Released / Planned / Unverified / Disputed).
  - Deterministic **Check Readiness** for products and campaigns.
- **Campaigns:** one goal each (awareness, open-source adoption, commercial demand, prelaunch research), success criteria, and an explicit lead-qualification rule.
- **Content:**
  - Immutable revisions on every relevant edit.
  - The **Proposed Publication** widget.
  - Hash-bound approval, invalidated by any edit.
  - Separate preparation, approval and execution permissions, and an optional four-eyes rule.
  - Channel AI-content rules.
- **Execution:**
  - Durable jobs with idempotency keys, leases, backoff retries only for failures that certainly did not send, and no blind retry of ambiguous outcomes (Reconcile, Mark as Accepted, Mark Not Sent and Retry).
  - Pause, resume and cancel.
  - Execution-time re-checks of approval, schedule, channel, transport, rate, consent and suppression, and the bulk limit.
  - The kill switch.
  - Run Due Executions on the host scheduler.
- **Transports:** `MarketingTransport`, the complete `ControlledTestTransport`, and `QqqMessagingEmailTransport` over the host's QQQ messaging provider.
- **Measurement:**
  - Separate funnel stages, raw events vs unique actors, "Not measured" for uninstrumented metrics, and test data kept apart.
  - Outcome events deduplicated by (source, external id).
  - **Update Results from CRM** and lead qualification only by rule.
- **CRM:** the `MarketingCrmGateway` SPI and `QbitCrmGateway` for qbit-crm.
- **Agent:**
  - A bounded tool-use loop over qbit-assistant's model-provider SPI.
  - Narrow typed tools (6 read, 3 write-proposal), run as the user with core permission enforcement, the untrusted-data envelope and redaction.
  - Run and step records with usage and cost.
  - Budgets, concurrency and rate limits, cancellation, and a plain failure reason when no provider is configured.
- **Model providers:** the published `OpenAiCompatibleModelProvider` (no SDK), host-supplied providers such as Claude, and a local scripted test double.
- **AI tool kernel:** the read tools are contributed through `AiToolRegistry` when QQQ has it (QRun-IO/qqq#1096).
- **UI:**
  - The Marketing app; its home is the Marketing dashboard (Needs Attention, Campaigns, Agent Activity, Configuration Status).
  - The nav lists only the main tables and settings.
  - Streamed-ETL record actions with standard summary lines.
- **Starter data:** QQQ, gclo and MarkdownViewr for Windows, verified 2026-10-12, with honest starter campaigns and the four channels.
- **Docs:** README, DESIGN, OPERATIONS and STARTER-DATA.

### Changed
- Move to the assistant provider SPI `0.1.0-RC.12` (package `com.kingsrook.qbits.assistant.spi`) and `qbit-build-parent` `2.1.2`, and drop the per-repository guardrail allow-lists. `qbit-assistant-provider-openai` becomes a test dependency; hosts that register it add it themselves.
- **Email:** new `QbitEmailTransport` sends campaign email through qbit-email (one MARKETING message per recipient, unsubscribe and suppression enforced there, idempotent, linked to the execution). It is selected automatically for the `qqq_email` channel when qbit-email is produced and the host registered no such transport; `QqqMessagingEmailTransport` remains the fallback. qbit-email is an optional dependency; QQQ moves to `4.1.0-RC.5`.

