# AGENTS.md

This file provides guidance to Codex when working with code in this repository.

## Project Overview

**QBit Marketing**: marketing for QQQ applications. It covers products and evidence, campaigns, content with revisions, hash-bound human approval, durable controlled execution through transports, honest measurement, a CRM gateway, and a marketing agent that prepares work but never publishes. Version 0.1.0 is unreleased. See `docs/DESIGN.md` (architecture), `docs/OPERATIONS.md` (configuration, run-books, limits) and `docs/STARTER-DATA.md`.

## Build Commands

```bash
./mvnw compile          # Compile (always use the wrapper: Maven 3.10 cannot read the parent POM)
./mvnw test             # Unit tests
./mvnw verify           # Unit tests + JaCoCo gate (70% instructions, 90% classes)
```

## Architecture

**Entry point:** `MarketingQBitProducer implements QBitMetaDataProducer<MarketingQBitConfig>` in `com.kingsrook.qbits.marketing`. It builds everything explicitly (no package scanning):
- `metadata.MarketingMetaDataBuilder`: tables, joins, child lists, PVSes and the app.
- `processes.*`: streamed-ETL record actions, plus two table actions.
- `widgets.MarketingWidgets`
- `MarketingRuntime`: the transports, providers, CRM gateway and limits.

Services: `approval.ApprovalService`, `execution.ExecutionService`, `measurement.*`, `readiness.ReadinessChecker`, `agent.MarketingAgentService` (tools in `agent.tools`).

- **Generated code:** entities (`model/`), enums (`model/enums/`) and the DDL (`docs/ddl/postgres.sql`, `src/test/resources/marketing-h2.sql`) come from `scripts/generate-model.py`. Edit the spec and regenerate (`python -I scripts/generate-model.py .`); never hand-edit the generated files.
- **System-written tables** (revisions, approvals, executions, attempts, test deliveries, agent runs and steps) are written only inside `MarketingInternalWrite.run(...)`.
- **Service entry points** check their permission, then run in `MarketingStore.trusted(...)`.
- **Agent tools** read with `PermissionEnforcement.ENFORCE`.

## Naming (owner rule)

- Tables are lowercase snake_case prefixed with the full qbit name: `marketing_<name>`. The QQQ table name and the backend (SQL) name are identical; possible-value sources take the table name.
- Permissions use the full qbit name: `marketing.<action>` (for example `marketing.view`, `marketing.admin`).
- Qbit data lives in standard, visible QQQ tables, secured by permissions and record security locks, never hidden.
- Processes run as table or record actions, never in the nav.

## Code Style

- 3-space indentation, opening braces on the next line, `return (x);`.
- Javadoc flower-box comments (`/*****` with `**` lines) on every class and method.
- Fluent setters (`withX()`).
- Wrapper types (`Integer`, `Boolean`) in config and entities.

## Dependencies

- Java 21.
- Parent `com.kingsrook:qbit-build-parent:2.1.2` (qqq 4.0.0), with architecture enforcement and strict UI API routes enabled. The default-active `qqq-snapshot` profile imports `qqq-bom-pom:4.1.0-RC.6` ahead of it, as in qbit-quick-search (ADR-0007). Remove that activation when the parent moves to 4.1.
- Assistant provider SPI `qbit-assistant-provider-api` `0.1.0-RC.12` (package `com.kingsrook.qbits.assistant.spi`); the OpenAI-compatible provider is test scope; no full assistant dependency. No per-repository guardrail allow-lists.
- Tests: JUnit 5, AssertJ.

## Release

- CircleCI qqq-orb 0.6.8.
- `develop` publishes SNAPSHOTs.
- A `release/*` branch publishes RCs (the orb bumps the version).
- A `v*` tag publishes the release.
