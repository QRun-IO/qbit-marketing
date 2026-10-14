# QBit Marketing

[![License](https://img.shields.io/badge/license-Apache%202.0-green.svg)](https://www.apache.org/licenses/LICENSE-2.0)
[![Java](https://img.shields.io/badge/java-21+-blue.svg)](https://adoptium.net/)

> Marketing for QQQ applications: products and their verified evidence, campaigns, content with revision history, exact human approval, controlled execution, honest measurement, and a marketing agent that prepares work but never publishes.

**Status:** 0.1.0, not yet published.
- Built against QQQ `4.1.0-RC.6` with `qbit-build-parent` `2.1.1`.
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
| **Measurement** | Delivery, awareness, engagement, activation, repeat use and qualified demand are reported separately. Each metric shows raw events and unique actors. "Not measured" is never shown as 0. Test data is kept apart. Results are read from a persisted daily rollup (**Daily Results**, rebuilt by **Rebuild Results**), and a host can register outcome importers (**Import Outcomes**). |
| **CRM** | Uses the host CRM's own contacts, organizations, consent, suppression, lists, form submissions and activities through a narrow gateway. A qbit-crm adapter is included. Nothing is copied, and nobody becomes a lead without the campaign's explicit rule. |
| **Agent** | Plans campaigns, drafts and revises content, flags unsupported claims, stale sources and readiness gaps, and analyzes results to recommend the next experiment. It uses narrow typed tools, runs as the requesting user, sees data only as untrusted input, and sees no CRM personal data. Every run is recorded with its steps, provider, model, tokens and cost. It can create drafts and findings; it cannot approve, schedule or send. |
| **Models** | Interchangeable providers through `qbit-assistant-provider-api`'s `AssistantModelProvider`: OpenAI-compatible endpoints through `qbit-assistant-provider-openai` (included), or a host-supplied provider such as Claude. Tests use a local scripted double. With no provider configured, the agent says so and everything else works. Installing marketing does not install the assistant runtime, tables or routes. |

## Install

```xml
<dependency>
   <groupId>com.kingsrook.qbits</groupId>
   <artifactId>qbit-marketing</artifactId>
   <version>${qbit-marketing.version}</version>
</dependency>
```

```java
import com.kingsrook.qbits.assistant.provider.ProviderSettings;
import com.kingsrook.qbits.assistant.provider.openai.OpenAiCompatibleModelProvider;
import com.kingsrook.qqq.backend.core.instances.QMetaDataVariableInterpreter;

var provider = new OpenAiCompatibleModelProvider(new ProviderSettings()
   .withKind("OPENAI_COMPATIBLE")
   .withBaseUrl("https://api.openai.com/v1")
   .withApiKey(new QMetaDataVariableInterpreter().interpret("${env.MARKETING_OPENAI_API_KEY}")));

new MarketingQBitProducer().withQBitConfig(new MarketingQBitConfig()
      .withBackendName("rdbms")                                   // where the marketing_* tables live
      .withCrmGateway(new QbitCrmGateway())                       // optional: qbit-crm
      .withSchedulerName("scheduler")                             // optional: runs the execution worker
      .withTransport(new QqqMessagingEmailTransport()             // optional: email via your QQQ messaging provider
         .withMessagingProviderName("email").withFromAddress("news@example.org"))
      .withModelProvider(provider)                               // optional: configure only for agent use
      .withProviderName("openai-compatible")
      .withModel("your-model-id"))
   .produce(qInstance);
```

Then:
1. Create the tables from [`docs/ddl/postgres.sql`](docs/ddl/postgres.sql), e.g. as a Liquibase formatted-SQL changeset.
2. Grant the permissions.
3. Optionally run **Load Verified Starter Data** from the Marketing Product table's actions.

[`docs/OPERATIONS.md`](docs/OPERATIONS.md) covers every setting, the run-books and the limits.

The provider SPI, pricing and OpenAI-compatible implementation are pinned to `0.1.0-RC.11`.
Replace imports of marketing's former `agent.provider.OpenAiCompatibleModelProvider` with the published class above and replace its builder with `ProviderSettings`.
The host resolves the API key reference; `ProviderSettings.withApiKeyEnvName(...)` alone does not resolve it.
Set any existing Marketing Policy provider override to `openai-compatible` as well. The base URL, key environment variable name and model remain host-configurable; omit the key for a local endpoint that needs none.

## Permissions

| Permission | Grants |
|---|---|
| `marketing.hasAccess` | The Marketing app and its dashboard. |
| `marketing.read` / `marketing.write` | Products, evidence, claims, audiences, campaigns, content, metrics, outcome events and findings. System-written history (revisions, approvals, executions, attempts, test deliveries, agent runs and steps) is read-only. |
| `marketing.admin.read` / `marketing.admin.write` | Channels and the Marketing Policy (kill switches, provider, model, budgets). Preparers usually also get `marketing.admin.read`, to pick channels. |
| `marketing.prepare.hasAccess` | Submit for Review, Check Readiness, Update Results from CRM, Load Verified Starter Data, and resolving or dismissing findings. Closing or downgrading an open **blocker** finding also needs `marketing.approve.hasAccess`. |
| `marketing.approve.hasAccess` | Approve and Return to Draft. |
| `marketing.execute.hasAccess` | Schedule, Run Now, Pause, Resume, Cancel, Reconcile, Mark as Accepted, Mark Not Sent and Retry, and Run Due Executions. |
| `marketing.agent.hasAccess` | Plan / Draft / Check Claims / Analyze Results with Agent, and Cancel Run. The agent's draft, finding and plan tools also need the requester's `marketing.write`, and the agent only revises content that is still a Draft. |

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

## Email through qbit-email

When the optional [qbit-email](https://github.com/QRun-IO/qbit-email) qbit is produced in the same instance, email channels named `qqq_email` (the starter-data channel) send through it automatically, unless the host registered its own transport under that name. `QqqMessagingEmailTransport` stays the fallback; without qbit-email nothing changes.

- Each recipient is one `MARKETING` message. qbit-email adds the RFC 8058 unsubscribe link and headers, enforces suppression and owns retries.
- The campaign content is sent as an ad-hoc Markdown email (subject = title, body plus the call to action link); no qbit-email template is registered.
- Idempotency key `marketing:<content id>:<approval id>:<recipient key>`; the linked record is the `marketing_execution` row. A repeat is a duplicate, never a second send.
- Accepted means queued in the qbit-email outbox. A suppressed recipient is skipped, not failed (the result says how many); a refusal by qbit-email is a rejected execution with its reason.
- Needs qbit-email's `publicBaseUrl`, the `QQQ_EMAIL_UNSUBSCRIBE_KEY` secret, an enabled default provider and a default brand with a postal address. Without the first two the transport reports *Not configured* and says which is missing.
- qbit-email is an optional dependency pinned to `0.1.0-RC.3` from Maven Central (`qbit-email.version`). The integration uses only its published `com.kingsrook.qbits.email.api` package for installation checks, readiness, sends and lookups, with the parent's default guardrail allow-lists.

## What is implemented, what is not configured

| Implemented and tested | Implemented, not configured / verified live |
|---|---|
| All tables, actions, widgets, permissions, record locks | Claude and OpenAI-compatible providers (need credentials; tested only with the scripted provider and a local HTTP fixture) |
| Approval binding, invalidation, separation of duties | Email transport (needs the host's QQQ messaging provider; reports *Not configured* until then, and *Configured* is not *Operational*) |
| Durable execution, retries, reconciliation, leases | No social, community, blog or CMS connector exists: those channels are posted by hand |
| Controlled test transport (complete) | No concrete analytics importer exists (only the `MarketingOutcomeImporter` SPI): awareness and activation metrics stay *Not measured* until something records outcome events |
| Agent loop, tools, budgets, limits, run records; read-tool registration in the QQQ RC.6 AI tool registry | |
| CRM gateway and qbit-crm adapter, explicit lead rules | |
| Persisted daily rollup of outcome events, Rebuild Results, and the importer SPI (tested with a fake importer; backend `SUM` / `COUNT_DISTINCT` tested on H2) | |
| Persistence across restart (H2 file, and PostgreSQL in the Voyage host) | |

Scale gaps (single worker per JVM, in-memory rate limits) are listed in [`docs/OPERATIONS.md`](docs/OPERATIONS.md#scale-and-limits).

## Build

```bash
./mvnw verify      # unit and integration tests + JaCoCo gate (70% instructions, 90% classes)
```

Always use the Maven wrapper (Maven 3.9.11); Maven 3.10 cannot read the parent POM. The entities, enums and DDL are generated from one spec: `python -I scripts/generate-model.py .` (commit the output).

Architecture guardrails and strict UI API route checks are enabled. The artifact allow-list preserves the parent's defaults and adds only `qbit-assistant-provider-openai`; the package allow-list also permits the extracted provider SPI and pricing. Parent 2.1.0 matches package prefixes using `prefix + "."`, so the pricing allowance must be `com.kingsrook.qbits.assistant.engine`, not the class name `…engine.ModelPricing`. The API artifact contains only `ModelPricing` in that package; the full assistant artifact remains prohibited.

Until provider `0.1.0-RC.11` is available on Central, verify against the locally installed assistant develop build only with:

```bash
./mvnw -Dqbit-assistant.version=0.1.0-ARCHLOCAL-SNAPSHOT -Dqbit.guardrails.enforce=false verify
```

This runs the guardrails in report mode so G7 can report the local SNAPSHOT dependencies. Keep the release version and enforcement enabled in the POM.

## License

Apache License 2.0. See [LICENSE](LICENSE) and [NOTICE](NOTICE).
