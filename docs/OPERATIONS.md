# QBit Marketing: operations

## Configuration (`MarketingQBitConfig`)

| Setting | Default | Meaning |
|---|---|---|
| `backendName` | required | The backend of the `marketing_*` tables. |
| `tableMetaDataCustomizer` | none | Standard QBit hook for each table (backend details, ESB publications, API meta-data). |
| `inferBackendNames` | `true` | Snake_case column names, matching `docs/ddl/postgres.sql`. |
| `productSecurityKeyType` | none | A tenant boundary by product: a record lock on `productId` (products on `id`). The all-access key is `<type>_all_access`. |
| `withModelProvider(p)` | none | Register an `AssistantModelProvider` (Claude, OpenAI-compatible, ...). |
| `providerName` / `model` / `effort` | none | Which provider and model the agent uses. The Marketing Policy can override them. |
| `maxOutputTokens` | 8000 | Per model call. |
| `maxToolCallsPerRun` / `maxModelCallsPerRun` / `maxRunSeconds` | 24 / 16 / 300 | Limits of one agent run. |
| `maxConcurrentAgentRuns` / `maxModelCallsPerMinute` | 2 / 30 | Agent concurrency and rate (per JVM). |
| `maxRowsToModel` | 50 | Rows per read tool. |
| `pricing` | Anthropic list prices (qbit-assistant) | USD per million tokens, by model id. Models without a price show no cost. Prices are never invented. |
| `registerKernelTools` | `true` | Contribute the read tools to the QQQ AI tool kernel (when QQQ has `AiToolRegistry`). |
| `withTransport(t)` / `includeTestTransport` | none / `true` | Transports. The controlled test transport is always available unless excluded. |
| `schedulerName` / `workerRepeatSeconds` | none / 60 | Run the execution worker on this scheduler. Without one, run **Run Due Executions** by hand. |
| `workerBatchSize` / `defaultMaxAttempts` / `retryBackoffSeconds` / `leaseSeconds` | 25 / 3 / 60 / 300 | Execution worker behaviour. Backoff doubles for each attempt. |
| `maxRecipientsPerExecution` | 50 | Bulk limit (a channel can lower it). |
| `requireDifferentApprover` | `false` | Four-eyes: the author of a revision can't approve it. |
| `aiDisclosureText` | `Prepared with AI assistance.` | Required in agent-written text for channels that allow AI content *with disclosure*. |
| `staleSourceDays` | 90 | When evidence counts as stale. |
| `crmGateway` | none | `QbitCrmGateway` for qbit-crm, or your own `MarketingCrmGateway`. |

**Fail closed:** without the policy row, or unless a switch is explicitly on, the agent and execution are **off**. **Load Verified Starter Data** creates the row, or you can create it by hand. Configuration Status shows the policy state.

**Runtime overrides:** the single **Marketing Policy** row named `default` (settings, `marketing.admin.*`) holds:
- `agentEnabled` and `executionEnabled` (kill switches, with a reason)
- `providerName` and `model`
- `dailyTokenLimit` and `monthlyCostLimitMicros`
- `maxConcurrentAgentRuns` and `maxToolCallsPerRun`

The policy is read without the user's permissions, because it is the application's own configuration.

## Schema

`docs/ddl/postgres.sql` is the reference DDL. It is idempotent, and Liquibase-ready as a formatted-SQL changeset. Voyage ships it as `0002-marketing-schema.sql`. The tests use the H2 twin, `src/test/resources/marketing-h2.sql`.

**Changing the model:** to add or change a field, edit `scripts/generate-model.py`, run `python -I scripts/generate-model.py .`, commit the regenerated entities, enums and DDL, and add a migration changeset in each host. The generator rewrites the reference DDL; it does not write migrations.

## Credentials (configure later)

Nothing in Marketing stores a credential, and every key is read from the host's environment or secret store. Providers and transports never print secrets: `toString` hides keys, and provider errors carry only the HTTP status and error type.

| Integration | What to provide |
|---|---|
| Claude | `ANTHROPIC_API_KEY` (or `AnthropicModelProvider.builder().apiKey(fromSecretStore)`), and a model id such as `claude-sonnet-5-5`. |
| OpenAI-compatible | Use `com.kingsrook.qbits.assistant.provider.openai.OpenAiCompatibleModelProvider` from `qbit-assistant-provider-openai` with `ProviderSettings`: a base URL (`https://api.openai.com/v1`, `http://localhost:11434/v1` for Ollama, ...), a resolved key if the server needs one, and a model id in `MarketingQBitConfig.withModel(...)`. Resolve `${env.MARKETING_OPENAI_API_KEY}` with `QMetaDataVariableInterpreter` before `withApiKey(...)`; setting `apiKeyEnvName` alone does not resolve a key. The provider name is `openai-compatible`, including in Marketing Policy overrides. See the [setup example](../README.md#install). |
| Email (qbit-email) | When qbit-email is produced in the instance and no transport named `qqq_email` is registered, email goes through its outbox (category MARKETING). It needs `publicBaseUrl`, the `QQQ_EMAIL_UNSUBSCRIBE_KEY` secret, an enabled default provider and a default brand with a postal address; a missing piece is reported as *Not configured* or as a plain rejected-execution reason. |
| Email (QQQ messaging) | A QQQ messaging provider in the host instance (SMTP or SES), its name, and a from-address. After verifying it end to end, `withVerifiedOperational(true)`. Until then the email transport reports *Configured, not verified* and executions are blocked. |

**Status:** check **Configuration Status** on the Marketing dashboard after any change.

## Run-books

**Publishing approved content**
1. In the content record, check the **Proposed Publication** widget: the exact text, destination, recipients, schedule and channel rules.
2. **Approve**. You need `marketing.approve.hasAccess`.
3. **Schedule**. You need `marketing.execute.hasAccess`.
4. The worker sends it at or after the approved time. Alternatively, use **Run Now** on the execution; it still repeats every check.

**An execution is Blocked:** read its Status Detail. Typical causes:
- the approval was invalidated by an edit
- the transport is not usable
- nobody in the scope has consent
- over the bulk limit
- the channel rules

Fix the cause. If the content changed, it needs a new approval. Then schedule again.

**An execution Needs reconciliation** (the outcome is unknown: a timeout after sending, or the worker stopped mid-send):
1. **Reconcile with Provider**. If the transport supports lookup, a found request becomes Accepted.
2. If the provider can't tell, check the provider's own records and decide:
   - **Mark as Accepted** if it was sent.
   - **Mark Not Sent and Retry** if it wasn't. The retry reuses the same idempotency key, and needs the approval still active.
3. Never just re-approve and re-schedule ambiguous content: it may go out twice.

**Stop everything now:** in Marketing Policy, set `executionEnabled` to false (and `agentEnabled`, if needed) with a reason. Scheduled executions stay Scheduled and resume when switched back on.

**Pause or withdraw one item:** use **Pause** or **Cancel** on the execution, or **Return to Draft** on the content. Return to Draft revokes the content's approvals and cancels its pending executions.

**Recording outcomes:**
- Insert outcome events through the table, the QQQ API or an importer. Set `source` and `externalEventId`: the pair is unique, so replays are dropped. An event without `occurredAt` gets the current time.
- Set `actorKey` to an anonymous, stable id. Never an email.
- Mark test fixtures `isTestData`.
- **Update Results from CRM** imports form submissions and applies the campaign's lead rule.
- **Import Outcomes** runs every importer the host registered (`MarketingQBitConfig.withOutcomeImporter`) and stores what they report; repeats are dropped by (source, external id). Registered importers are listed in the Configuration Status widget.
- **Rebuild Results** recomputes a campaign's **Daily Results** from its raw events. Use it after events were written outside Marketing (direct SQL, a data fix) or if the rollup looks wrong. It is safe to repeat.

**Agent runs:** **Plan / Draft / Check Claims / Analyze Results with Agent** on a campaign, at most 3 campaigns per run. The run record lists every step and what the model saw. **Cancel Run** stops a run before its next model call. Runs left Running by a restart are marked Failed by the next **Run Due Executions**.

## Scale and limits

These are real limitations of 0.1.0. Nothing here is claimed to be production-scale.

- **Single execution worker per JVM.** The worker takes an in-process lock and claims a row by re-reading its status before marking it Running. Two replicas could both claim the same due execution in the instant between read and update, and the idempotency key is then the only protection (it fully protects the test transport, but not email). **Run Marketing's worker on one replica**, or add a database-level claim (`UPDATE ... WHERE status = SCHEDULED`, or `SELECT ... FOR UPDATE SKIP LOCKED`) before scaling out.
- **Rate limits and agent concurrency are per JVM** (in memory). The channel hourly rate is computed from the database, so it holds across replicas, but only up to the race above.
- **Agent runs are synchronous** inside the record action, bounded by `maxRunSeconds`. A restart interrupts them: they are marked Failed and can be run again. There is no queue or resume.
- **The worker queries due executions page by page** (`workerBatchSize` per run) and scans channel attempts for the hourly rate. That suits hundreds of executions per hour, not bulk email. Bulk outreach is deliberately not supported.
- **Measurement is read from a persisted daily rollup** (`marketing_metric_daily`, the **Daily Results** list on a campaign), kept in step by every event insert, edit and delete, so a view reads a few rows per day plus backend aggregates (`SUM` over the rollup, `COUNT_DISTINCT` of `actorKey`), not the events. What remains:
  - Each event write recomputes its (campaign, day) by reading that day's events, so a very busy day (hundreds of thousands of events) makes writes slow; batch imports are the intended way in. A bulk insert recomputes each affected day once.
  - The unique-actor count is a `COUNT_DISTINCT` over all of a campaign's real events: fine for hundreds of thousands, but it scans them. Rebuild Results reads events in pages and keeps only the distinct days in memory.
  - A day is the UTC date of `occurredAt`; the rollup does not follow a reporting time zone.
  - Importers run synchronously inside the action, bounded by what `fetch` returns; there is no scheduler for them in 0.1.0.
- **No live verification** of providers or transports. *Configured* means the settings are present.
- **Material Dashboard** (Voyage's current frontend) may not route sub-tables that are not in the app tree. The links in the widgets follow the Next route grammar.
