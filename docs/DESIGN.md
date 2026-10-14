# QBit Marketing: design

The design of qbit-marketing 0.1.0 as implemented. Operating details are in [OPERATIONS.md](OPERATIONS.md); the starter products and their sources are in [STARTER-DATA.md](STARTER-DATA.md).

## Goals

Automate the repeatable work of marketing our software, inside the host QQQ application:
- research positioning from recorded evidence
- plan campaigns
- prepare content
- coordinate approved execution
- measure outcomes
- recommend improvements

The work is truthful and auditable: facts come from sources with dates, people approve exactly what goes out, and results never claim more than was measured.

## Existing patterns reused

| From | Reused for |
|---|---|
| **qbit-assistant** (0.1.0-RC.2) | The model-provider SPI: `AssistantModelProvider`, `ModelRequest` / `ModelResponse` / `ModelPart`, `StreamHandle`, `ModelProviderException` with its retryable kinds, and `ModelUsage`. Since RC.12 it lives in `com.kingsrook.qbits.assistant.spi` (`qbit-assistant-provider-api`), together with `ModelPricing` for cost; hosts register a provider such as `qbit-assistant-provider-anthropic` or `qbit-assistant-provider-openai`. There is no second provider abstraction. |
| **QQQ AI tool kernel** (qqq#1078, core) | The tool contract: `AbstractAiTool` / `AiToolKind` (READ, WRITE_PROPOSAL), `AiToolOutcome`, the `qqq_data` untrusted-data envelope (`AiDataEnvelope`), secret redaction (`AiRedactor`), strict input keys, and running as the user's session. The RC.6 baseline includes `AiToolRegistry` (QRun-IO/qqq#1096); Marketing contributes its read tools there and runs its own agent tools with the same contract. |
| **QQQ core permission enforcement** (qqq#1061) | Agent tools read with `PermissionEnforcement.ENFORCE`. Service entry points check their step's permission, then run as trusted internal work in an authorized scope, as QQQ does for calls nested in a core action. Record security locks always apply. |
| **qbit-assistant / sql-workbench metadata builders** | Explicit builder-produced tables (not annotation scanning), snake_case `marketing_*` names, QQQ standard permission suffixes, `DenyBehavior.HIDDEN`, read-only system tables, record locks, reference DDL in `docs/ddl`, `tableMetaDataCustomizer` and `inferBackendNames`. |
| **Website-Backend admin conventions** | Processes are table and record actions, never nav items, with plain labels. Record actions use `StreamedETLWithFrontendProcess` with standard summary lines. App homes are real-data dashboards built from ≤3-column full-width tables. Links use the Next route grammar (`AiLinkBuilder`). |
| **qbit-crm** | The CRM itself, via a gateway: consent records, `doNotEmail` / unsubscribe, list membership, form submissions with UTM, and "Note" activities. |
| **QQQ messaging** | `SendMessageAction` and the host's `QMessagingProviderMetaData` for email. |
| **QQQ ESB / Quick Search** | Every write goes through core table actions, so record-change listeners (ESB publications) and Quick Search indexing apply. Hosts list `MAIN_TABLE_NAMES` in their ESB and Quick Search config (Voyage does this for Quick Search). |

## Data model (18 tables)

**Main tables** (in the nav):

| Table | Holds |
|---|---|
| `marketing_product` | The product brief: maturity, tagline, summary, value propositions, license, platforms, latest release, adoption path, official URLs and last verification date. |
| `marketing_campaign` | Goal, status, audience, objective, problem, messaging, channel plan, call to action, destination, UTM campaign, schedule, owner, success criteria and the lead-qualification rule. |
| `marketing_content` | The proposed publication: title, body, destination, call to action, recipient scope, schedule and channel. Managed fields: status, revision number, content hash, author type and agent run. |
| `marketing_execution` | A durable job: approval, revision, channel, transport, schedule, status, idempotency key, attempts, backoff, receipt, lease and test-data flag. |
| `marketing_agent_run` | Task, status, instructions, provider, model, requester, times, model and tool calls, tokens, cost, drafts and findings created, summary, output JSON and error. |

**Settings tables** (in the nav):

| Table | Holds |
|---|---|
| `marketing_channel` | Type, transport, active flag, automation allowed, AI-content policy, rules summary and source, rules verification date, hourly rate, recipients per execution. |
| `marketing_policy` | Kill switches for the agent and execution, provider and model overrides, the daily token budget, the monthly cost budget, and concurrency and tool limits. |

**Sub-tables** (child lists on their parents):

| Table | Parent | Notes |
|---|---|---|
| `marketing_audience` | product | |
| `marketing_evidence_source` | product | With its verification date. |
| `marketing_claim` | product, source | Status, evidence quote, discrepancy note. |
| `marketing_content_revision` | content | Immutable. |
| `marketing_approval` | content | Hash-bound. Status: active, invalidated, revoked or consumed. |
| `marketing_execution_attempt` | execution | |
| `marketing_test_delivery` | execution | What the test transport accepted. |
| `marketing_agent_step` | agent run | Model calls, tool calls and checks. |
| `marketing_finding` | product, campaign, content, agent run | |
| `marketing_metric` | campaign | The measurement plan. |
| `marketing_outcome_event` | campaign | Raw events, deduplicated by (source, external id). |

Most rows carry a denormalized `productId`, so the optional product security lock covers every table. `scripts/generate-model.py` generates the entities, the 21 enum possible-value sources and the DDL from one spec.

## Content, revisions and approval

- **Revisions:** `MarketingContentCustomizer` hashes the approved fields (`ContentHasher`): title, body, destination, call to action, recipient scope, schedule (to the second) and channel. Every insert and every change to one of those fields appends a revision.
- **Edits after approval:** a hash change voids every active approval (recorded with the reason), returns the content to Draft, and blocks its scheduled or paused executions.
- **Managed fields:** people can't set status or the managed fields directly. Status moves only through the actions, which run inside `MarketingInternalWrite`.
- **Approve:** the person must hold `marketing.approve.hasAccess`. The content must be In Review with no open blocker findings, and pass the channel's AI-content rule:
  - *Not allowed* refuses agent-written revisions.
  - *Unknown* refuses them until the rules are recorded.
  - *With disclosure* requires the disclosure text in the body.
  - Optionally, the approver must not be the revision's author.
  - The approval row stores the hash, revision, destination, scope and schedule.
- **Guarded tables:** approvals, executions, attempts, revisions, test deliveries, agent runs and steps are read-only in the UI and API. A guard customizer rejects any write outside Marketing's services, including the generic API and any tool.

## Execution

- **Schedule:** `Schedule` (`marketing.execute.hasAccess`) creates one execution per approval (idempotency key `marketing-execution:<content>:<approval>`). It refuses a channel whose transport isn't registered: a declared channel name is not a connector.
- **Worker:** `Run Due Executions` runs scheduled on the host scheduler, or by hand. It holds one lock per JVM, recovers expired leases first, then runs each due execution.
- **Checks before every send:**
  1. the policy kill switch
  2. the execution is still Scheduled (so paused and cancelled work never runs)
  3. the approval is still Active, with the same hash as the content
  4. the approved schedule has been reached
  5. the channel is active
  6. the transport is registered, usable (Operational or Test only), and serves the channel type
  7. the channel allows automation (except on the test transport)
  8. the channel's AI-content rule
  9. the channel's hourly rate (over it, the send is postponed, not failed)
  10. for CRM recipients: the scope is resolved through the gateway now, each contact's consent and suppression is re-checked, ineligible contacts are excluded and counted, it is blocked when nobody is left, and blocked over the per-execution limit (no bulk outreach)
- **Claim:** the execution moves to Running with a lease, the attempt counter is incremented, and the transport gets the exact approved text and the idempotency key.
- **Outcomes:**
  - *Accepted* or *Duplicate*: Accepted by provider. The approval is consumed, the content is Accepted, and one Delivery-stage `provider_accepted` event is recorded. That is not delivery or engagement.
  - *Rejected*: Failed.
  - *Retryable failure* (certainly not sent): rescheduled with exponential backoff until `maxAttempts`, then Failed.
  - *Ambiguous*: Needs reconciliation, never retried automatically. **Reconcile with Provider** asks the transport (`lookup`). **Mark as Accepted** and **Mark Not Sent and Retry** record a person's decision; a retry reuses the idempotency key, so the transport can drop a repeat.
  - *Lease expired* (the worker died mid-send): Needs reconciliation.
- **Transports:** `MarketingTransport` defines `getStatus` (Not configured / Configured / Operational / Test only), `send`, which never throws for provider outcomes, and `lookup`.
  - `ControlledTestTransport` is complete. It stores each payload per recipient, honors idempotency keys (repeats are counted, not stored), and supports scripted failures for tests and demos.
  - `QqqMessagingEmailTransport` is one message per recipient. It reports Configured but not Operational until the host verifies its provider. Exceptions are Ambiguous, because SMTP and SES can fail after accepting.

## Measurement

`MeasurementReport` (the campaign Results widget and the agent's results tool) works from the campaign's metrics: stage, event type, count mode and instrumentation status.

- **Separation:** Delivery, Awareness, Engagement, Activation, Repeat use and Qualified demand are separate rows.
- **Counts:** each metric shows raw events (summed quantities) and unique actors (distinct `actorKey`). Events without an actor are noted.
- **Missing instrumentation:** a Not instrumented metric shows **Not measured**, never 0. Partial instrumentation is a lower bound. Stages with no metric and events outside the plan are listed as limitations.
- **Test data:** test executions and fixtures are flagged `isTestData` and excluded from the numbers, with a count.
- **Fixed limitations:** provider acceptance is not delivery; visits, stars and downloads are not adoption.
- **Deduplication:** outcome events are deduplicated by the unique key (source, external id) on every insert path: the admin UI, the API and services.

## CRM integration

`MarketingCrmGateway` is the whole contract:
- `resolveRecipients(scope)`: addresses only for eligible contacts, used only at send time
- `checkEmailEligibility`
- `listFormSubmissions(utmCampaign)`: ids and times only
- `recordActivity`

`QbitCrmGateway` implements it over qbit-crm 0.2.0's `crm_*` tables (`crm_contact`, `crm_consent_record`, ...) by table name, through the standard actions, so there is no compile dependency on the CRM. It moves to qbit-crm's public `com.kingsrook.qbits.crm.api.CrmMarketingApi` once that is released:
- **Email consent:** the most recent Email consent record decides (Granted and not expired). No record means no consent.
- **Suppression:** `doNotEmail`, an unsubscribe date, or a list membership that isn't Subscribed.

**Update Results from CRM** imports a campaign's attributed form submissions as Engagement events, deduplicated by submission. It then applies the campaign's explicit rule: at least N attributed submissions, and email consent if required. Each newly qualified contact is logged as a CRM "Note" activity and recorded as one Qualified-demand event. Without the rule, nobody is a lead.

## The agent

`MarketingAgentService.run(task, campaign, content, instructions)` is a bounded tool-use loop:

1. **Preflight**, which fails the run with a plain reason: the policy kill switch, provider and model resolved (policy, then config, then the single registered provider), the daily token and monthly cost budgets, and the concurrency permit.
2. **Deterministic readiness checks** (`ReadinessChecker`, also available without the agent as **Check Readiness**), recorded as a CHECK step:
   - missing or stale evidence
   - unverified, unsourced or disputed claims
   - missing audience
   - prelaunch maturity, and goals that don't fit it
   - missing call to action or success criteria
   - measurement gaps
   - unverified channel rules
3. **The loop:**
   - **Inputs:** a fixed system prompt (truth rules, untrusted data, no personal data, no authority to publish) and the task message (ids and the person's instructions only).
   - **Each call:** one model call, then each tool call through `MarketingToolRunner`, which enforces strict keys, the user's session with `ENFORCE`, redaction, a cap of 8000 characters per string, the envelope, and errors as outcomes.
   - **Bounds:** model calls, tool calls, wall-clock time, and model calls per minute.
   - **Cancel:** the run re-reads its status before each model call, so **Cancel Run** stops it.
   - **Retries:** retryable provider errors are retried twice with backoff.
4. **Record:** every step (model calls with tokens and stop reason, tool calls with the input, the outcome and the enveloped result as the model saw it), the usage, the cost (when the model has a price), the drafts and findings created, the redacted final summary, and the plan.

**Tools** (`MarketingTools`). The kind of each tool:
- **READ** (also contributed to the AI tool kernel):
  - `marketing_get_campaign_brief`
  - `marketing_list_product_claims`
  - `marketing_list_campaign_content`
  - `marketing_get_campaign_results` (counts and anonymous actor counts only)
  - `marketing_list_channels`
  - `marketing_list_findings`
- **WRITE_PROPOSAL** (agent only):
  - `marketing_save_draft`: always a Draft by the agent. It refuses channels that forbid AI content, cited claims must belong to the product, and non-released claims raise a BLOCKER finding.
  - `marketing_record_finding`
  - `marketing_submit_plan`

Each task gets only the tools it needs. No tool can approve, schedule, execute or touch the guarded tables. The write-proposal tools need the requester's `marketing.write`, and `marketing_save_draft` revises only content that is still a Draft.

**Review safeguards:**
- **The content edit path:** ids normalized, the old record re-read, never fail open. Execution also re-hashes the fields it is about to send.
- **Approve is pinned:** a record whose content hash changed between the Review screen and execute is skipped.
- **The policy fails closed.**
- **Global limits and budgets** are counted as the system user, across all products.
- **Worker isolation:** one failing execution is blocked without stopping the others.
- **Open blocker findings** can only be closed or downgraded with approval authority.

**Data sent to the provider:** the tool definitions, the fixed prompt, the task text and enveloped tool results. Never CRM addresses, names or contact details: the results tool aggregates, and recipients are resolved only at send time.

## Configuration status

The dashboard's Configuration Status widget (`MarketingRuntime.describeIntegrations`) lists every model provider, transport and the CRM gateway with its honest state:
- *Not configured*
- *Registered (not selected)*
- *Configured (not verified by a live call)*
- *Configured, not verified (executions blocked)*
- *Operational*
- *Test only*
- *Available*

No live call is made to verify a provider or transport.

## Owner decisions (2026-10-12): all recommendations accepted

The owner accepted every recommendation on 2026-10-12. The concrete defaults, recorded 2026-10-13:

**Providers**

| Integration | Default | Why |
|---|---|---|
| Model provider | Claude through `qbit-assistant-provider-anthropic`, model `claude-sonnet-5-5`, set by the host (`ANTHROPIC_API_KEY`, `MARKETING_MODEL`) | The existing provider SPI and pricing table; no second abstraction. |
| OpenAI-compatible provider | Not configured | Optional; register it only for a specific need (for example a local Ollama for experiments). With two providers the host must name one. |
| Email transport | QQQ messaging over Amazon SES, from a verified `qrun.io` domain (SPF, DKIM, DMARC), from-address `news@qrun.io`, replies to `hello@qrun.io` | SES is a low-cost, reliable sender and QQQ messaging supports it directly. Reports *Configured, not verified* until one end-to-end test send, then the host sets `withVerifiedOperational(true)`. |
| Social and community posting | No transports. Posted by hand | No connector exists, and each community's rules must be checked before any automation. |
| Measurement | Outcome events from the CRM import and by hand; no third-party analytics provider | Matches the honest-measurement rules: not instrumented shows *Not measured*. |
| CRM gateway | `QbitCrmGateway` (qbit-crm) | What Voyage's Marketing wiring uses. |

**AI-content channel policy**

- The agent drafts and a person approves every publication. The agent never publishes.
- **Owned channels:** email to the opted-in CRM list and the project blog are *Allowed with disclosure*, with the default `aiDisclosureText` ("Prepared with AI assistance.").
- **GitHub release notes:** *Allowed*, without disclosure. A maintainer checks every line against the release's merged changes before publishing.
- **Third-party community channels** (forums, Reddit, Hacker News, social networks): *Not allowed* for agent-written text. A new community channel starts *Not allowed*, or *Unknown* until its current rules are read and recorded with a verification date. Never loosen one without its rules on file.
- **Test sink:** *Allowed*.

**Policy row and approval**

- `default` policy: agent and execution on, 1,000,000 tokens a day, a **USD 25 monthly cost limit** (`monthlyCostLimitMicros` 25,000,000), 2 concurrent runs, 24 tool calls per run.
- `requireDifferentApprover` stays `false`: the team is one person. Turn it on when a second approver exists.

The starter data (`marketing-starter-data.json`) carries these channel policies, verification dates and the cost limit.
