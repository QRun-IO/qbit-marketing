# QBit Marketing

[![License](https://img.shields.io/badge/license-Apache%202.0-green.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Java](https://img.shields.io/badge/java-21+-blue.svg)](https://adoptium.net/)

> Marketing for QQQ applications: products and their verified evidence, campaigns, content with revision history, exact human approval, controlled execution, honest measurement, and a marketing agent that prepares work but never publishes.

**Status:** 0.1.0, not yet published.
- Built against QQQ `4.1.0-RC.4`.
- Hosted in QRun-IO/Voyage next to qbit-crm, on a local branch.
- The real integrations are implemented but unconfigured. See [what is configured](#what-is-implemented-what-is-not-configured).

It lives inside the host QQQ application: its navigation, authentication, permissions, API, record screens and widgets. It is not a separate marketing app.

## What it does

| Area | What you get |
|---|---|
| **Products and evidence** | Product briefs, maturity, audiences, value propositions, evidence sources with verification dates, and factual claims. Each claim is *Released*, *Planned*, *Unverified* or *Disputed*. Disagreeing sources are recorded as disputed for review, never resolved in favour of the more promotional one. |
| **Campaigns** | One goal per campaign: awareness, open-source adoption, commercial demand, or prelaunch research. Each also has an audience, problem, messaging, channel plan, call to action, destination, schedule, owner, measurable success criteria and an optional explicit lead-qualification rule. Adoption and demand run as separate campaigns. |
| **Content and review** | Drafts by people or the agent. Every relevant edit appends an immutable revision. The content record shows the exact proposed publication: text, destination, recipients and schedule. |
| **Approval** | A person approves a hash of the exact text, destination, recipient scope, schedule and channel. Any edit voids it. Preparation, approval and execution are separate permissions. |
| **Execution** | Durable jobs with idempotency keys, schedules, pause, resume and cancel, and attempts with provider receipts. Failures that certainly did not send are retried with backoff. Ambiguous outcomes are reconciled and never retried blindly. Everything is re-checked at send time (approval, schedule, consent, suppression, channel rules, rate, kill switch). |
| **Transports** | A complete controlled test transport, and email through the host's QQQ messaging provider. A channel that names a transport is not treated as a working connector unless that transport is registered and usable. |
| **Measurement** | Delivery, awareness, engagement, activation, repeat use and qualified demand are reported separately. Each metric shows raw events and unique actors. "Not measured" is never shown as 0. Test data is kept apart. |
| **CRM** | Uses the host CRM's own contacts, organizations, consent, suppression, lists, form submissions and activities through a narrow gateway. A qbit-crm adapter is included. Nothing is copied, and nobody becomes a lead without the campaign's explicit rule. |
| **Agent** | Plans campaigns, drafts and revises content, flags unsupported claims, stale sources and readiness gaps, and analyzes results to recommend the next experiment. It uses narrow typed tools, runs as the requesting user, sees data only as untrusted input, and sees no CRM personal data. Every run is recorded with its steps, provider, model, tokens and cost. It can create drafts and findings; it cannot approve, schedule or send. |
| **Models** | Interchangeable providers through qbit-assistant's `AssistantModelProvider`: Claude (`qbit-assistant-provider-anthropic`), any OpenAI-compatible endpoint (included), and a scripted provider for tests. With no provider configured, the agent says so and everything else works. |

## Install

```xml
<dependency>
   <groupId>com.kingsrook.qbits</groupId>
   <artifactId>qbit-marketing</artifactId>
   <version>${qbit-marketing.version}</version>
</dependency>
```

```java
new MarketingQBitProducer().withQBitConfig(new MarketingQBitConfig()
      .withBackendName("rdbms")                                   // where the marketing_* tables live
      .withCrmGateway(new QbitCrmGateway())                       // optional: qbit-crm
      .withSchedulerName("scheduler")                             // optional: runs the execution worker
      .withTransport(new QqqMessagingEmailTransport()             // optional: email via your QQQ messaging provider
         .withMessagingProviderName("email").withFromAddress("news@example.org"))
      .withModelProvider(AnthropicModelProvider.fromEnvironment()) // optional: only when ANTHROPIC_API_KEY is set
      .withModel("claude-sonnet-5-5"))
   .produce(qInstance);
```

Then:
1. Create the tables from [`docs/ddl/postgres.sql`](docs/ddl/postgres.sql), e.g. as a Liquibase formatted-SQL changeset.
2. Grant the permissions.
3. Optionally run **Load Verified Starter Data** from the Marketing Product table's actions.

[`docs/OPERATIONS.md`](docs/OPERATIONS.md) covers every setting, the run-books and the limits.

## Permissions

| Permission | Grants |
|---|---|
| `marketing.hasAccess` | The Marketing app and its dashboard. |
| `marketing.read` / `marketing.write` | Products, evidence, claims, audiences, campaigns, content, metrics, outcome events and findings. System-written history (revisions, approvals, executions, attempts, test deliveries, agent runs and steps) is read-only. |
| `marketing.admin.read` / `marketing.admin.write` | Channels and the Marketing Policy (kill switches, provider, model, budgets). Preparers usually also get `marketing.admin.read`, to pick channels. |
| `marketing.prepare.hasAccess` | Submit for Review, Check Readiness, Update Results from CRM, Load Verified Starter Data, and resolving or dismissing findings. |
| `marketing.approve.hasAccess` | Approve and Return to Draft. |
| `marketing.execute.hasAccess` | Schedule, Run Now, Pause, Resume, Cancel, Reconcile, Mark as Accepted, Mark Not Sent and Retry, and Run Due Executions. |
| `marketing.agent.hasAccess` | Plan / Draft / Check Claims / Analyze Results with Agent, and Cancel Run. |

- **Optional tenant boundary:** `withProductSecurityKeyType("…")` locks every table by product. Users see only the products they hold keys for, and so does the agent, which always runs as its user.
- **Optional four-eyes rule:** `withRequireDifferentApprover(true)` stops the author of a revision from approving it.

## UI

- **Nav:** the Marketing app lists only the main tables and the settings.
  - Main tables: Products, Campaigns, Content, Executions, Agent Runs.
  - Settings: Channels, Policy.
- **App home:** the Marketing dashboard. Needs Attention, Campaigns, Agent Activity and Configuration Status, all from real data.
- **Sub-tables** appear as child lists on their parent records. They are still reachable by URL.
- **Record widgets:**
  - Campaign: Results.
  - Content: Proposed Publication.
- **Actions:** every action is a streamed-ETL record action (record input, standard Review and Result summary lines) in the table's or record's Actions menu. None are nav items.

## What is implemented, what is not configured

| Implemented and tested | Implemented, not configured / verified live |
|---|---|
| All tables, actions, widgets, permissions, record locks | Claude and OpenAI-compatible providers (need credentials; tested only with the scripted provider and a local HTTP fixture) |
| Approval binding, invalidation, separation of duties | Email transport (needs the host's QQQ messaging provider; reports *Not configured* until then, and *Configured* is not *Operational*) |
| Durable execution, retries, reconciliation, leases | No social, community, blog or CMS connector exists: those channels are posted by hand |
| Controlled test transport (complete) | No analytics importer: awareness and activation metrics stay *Not measured* until something records outcome events |
| Agent loop, tools, budgets, limits, run records | QQQ AI tool kernel registration (needs QRun-IO/qqq#1096; the qbit works without it) |
| CRM gateway and qbit-crm adapter, explicit lead rules | |
| Persistence across restart (H2 file, and PostgreSQL in the Voyage host) | |

Scale gaps (single worker per JVM, in-memory rate limits) are listed in [`docs/OPERATIONS.md`](docs/OPERATIONS.md#scale-and-limits).

## Build

```bash
./mvnw verify      # unit and integration tests + JaCoCo gate (70% instructions, 90% classes)
```

Always use the Maven wrapper (Maven 3.9.11); Maven 3.10 cannot read the parent POM. The entities, enums and DDL are generated from one spec: `python -I scripts/generate-model.py .` (commit the output).

## License

Apache License 2.0. See [LICENSE](LICENSE) and [NOTICE](NOTICE).
