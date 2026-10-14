"""Generate qbit-marketing entities, enum possible-value sources and DDL from one spec.

Usage: python -I scripts/generate-model.py .   (from the repository root)
"""
import os
import re
import sys

ROOT = sys.argv[1]
PKG = "com.kingsrook.qbits.marketing"
JAVA = os.path.join(ROOT, "src", "main", "java", *PKG.split("."))

HEADER = """/*
 * Copyright 2026 Kingsrook, LLC
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
"""

###############################################################################
# enums: name -> (description, [(CONST, label)])
###############################################################################
ENUMS = {
   "ProductMaturity": ("How ready a product is for adoption marketing.", [
      ("CONCEPT", "Concept"), ("PRELAUNCH", "Prelaunch (no public release)"), ("PREVIEW", "Preview / pre-release"),
      ("RELEASED", "Released"), ("RETIRED", "Retired")]),
   "SourceType": ("The kind of an evidence source.", [
      ("WEBSITE", "Website"), ("DOCUMENTATION", "Documentation"), ("REPOSITORY", "Source repository"),
      ("RELEASE", "Release / changelog"), ("PACKAGE_REGISTRY", "Package registry"), ("STORE_LISTING", "Store listing"),
      ("CHANNEL_RULES", "Channel / community rules"), ("OTHER", "Other")]),
   "ClaimStatus": ("Whether a factual claim may be used in content.", [
      ("RELEASED", "Released (verified)"), ("PLANNED", "Planned - not released"), ("UNVERIFIED", "Unverified"),
      ("DISPUTED", "Sources disagree - needs review"), ("RETIRED", "Retired")]),
   "CampaignGoal": ("The primary goal of a campaign.", [
      ("AWARENESS", "Awareness"), ("OPEN_SOURCE_ADOPTION", "Free / open-source adoption"),
      ("COMMERCIAL_DEMAND", "Commercial demand"), ("PRELAUNCH_RESEARCH", "Prelaunch research / tester interest")]),
   "CampaignStatus": ("Where a campaign is in its life cycle.", [
      ("DRAFT", "Draft"), ("PLANNING", "Planning"), ("READY", "Ready"), ("ACTIVE", "Active"), ("PAUSED", "Paused"),
      ("COMPLETED", "Completed"), ("CANCELLED", "Cancelled")]),
   "ChannelType": ("The kind of a channel.", [
      ("TEST_SINK", "Controlled test sink"), ("EMAIL", "Email"), ("BLOG", "Blog / website"), ("SOCIAL", "Social network"),
      ("COMMUNITY", "Community / forum"), ("RELEASE_NOTES", "Release notes"), ("OTHER", "Other")]),
   "AiContentPolicy": ("What a channel's rules say about AI-generated content.", [
      ("ALLOWED", "Allowed"), ("ALLOWED_WITH_DISCLOSURE", "Allowed with disclosure"), ("NOT_ALLOWED", "Not allowed"),
      ("UNKNOWN", "Unknown - check the rules")]),
   "ContentStatus": ("Where a content item is between drafting and execution.", [
      ("DRAFT", "Draft"), ("IN_REVIEW", "In review"), ("APPROVED", "Approved"), ("SCHEDULED", "Scheduled"),
      ("PAUSED", "Paused"), ("ACCEPTED", "Accepted by provider"), ("FAILED", "Failed"), ("CANCELLED", "Cancelled"),
      ("NEEDS_RECONCILIATION", "Ambiguous - needs reconciliation")]),
   "AuthorType": ("Who wrote a content revision.", [("HUMAN", "Person"), ("AGENT", "Marketing agent")]),
   "ApprovalStatus": ("The state of an approval.", [
      ("ACTIVE", "Active"), ("INVALIDATED", "Invalidated by an edit"), ("REVOKED", "Revoked"), ("CONSUMED", "Used by an execution")]),
   "ExecutionStatus": ("The state of a durable execution job.", [
      ("SCHEDULED", "Scheduled"), ("PAUSED", "Paused"), ("CANCELLED", "Cancelled"), ("RUNNING", "Running"),
      ("ACCEPTED", "Accepted by provider"), ("FAILED", "Failed"), ("NEEDS_RECONCILIATION", "Ambiguous - needs reconciliation"),
      ("BLOCKED", "Blocked by execution-time checks")]),
   "AttemptOutcome": ("The outcome of one execution attempt.", [
      ("ACCEPTED", "Accepted by provider"), ("REJECTED", "Rejected by provider"), ("RETRYABLE_FAILURE", "Failed before sending (retryable)"),
      ("AMBIGUOUS", "Ambiguous (may have been sent)"), ("BLOCKED", "Blocked by checks"), ("DUPLICATE", "Duplicate - not sent again")]),
   "AgentTask": ("What a marketing agent run was asked to do.", [
      ("PLAN_CAMPAIGN", "Plan campaign"), ("DRAFT_CONTENT", "Draft content"), ("REVIEW_CLAIMS", "Check claims and readiness"),
      ("ANALYZE_RESULTS", "Analyze results")]),
   "AgentRunStatus": ("The state of a marketing agent run.", [
      ("QUEUED", "Queued"), ("RUNNING", "Running"), ("SUCCEEDED", "Succeeded"), ("FAILED", "Failed"), ("CANCELLED", "Cancelled")]),
   "AgentStepKind": ("The kind of a step in an agent run.", [
      ("MODEL_CALL", "Model call"), ("TOOL_CALL", "Tool call"), ("CHECK", "Deterministic check")]),
   "FindingType": ("The kind of a finding.", [
      ("UNSUPPORTED_CLAIM", "Unsupported claim"), ("STALE_SOURCE", "Stale source"), ("MISSING_EVIDENCE", "Missing evidence"),
      ("READINESS_GAP", "Launch-readiness gap"), ("SOURCE_DISCREPANCY", "Source discrepancy"), ("CHANNEL_RULE", "Channel rule"),
      ("MEASUREMENT_LIMITATION", "Measurement limitation"), ("RECOMMENDATION", "Recommendation"), ("PLAN", "Campaign plan")]),
   "FindingSeverity": ("How much a finding matters.", [("INFO", "Info"), ("WARNING", "Warning"), ("BLOCKER", "Blocker")]),
   "FindingStatus": ("Whether a finding was handled.", [
      ("OPEN", "Open"), ("ACCEPTED", "Accepted"), ("DISMISSED", "Dismissed"), ("RESOLVED", "Resolved")]),
   "FunnelStage": ("The measurement stage of an outcome event or metric.", [
      ("DELIVERY", "Delivery / provider status"), ("AWARENESS", "Awareness"), ("ENGAGEMENT", "Engagement"),
      ("ACTIVATION", "Activation"), ("REPEAT_USE", "Repeat use"), ("QUALIFIED_DEMAND", "Qualified demand")]),
   "InstrumentationStatus": ("Whether a metric is measured at all.", [
      ("INSTRUMENTED", "Instrumented"), ("PARTIAL", "Partially instrumented"), ("NOT_INSTRUMENTED", "Not instrumented")]),
   "CountMode": ("How a metric counts its events.", [("RAW_EVENTS", "Raw events"), ("UNIQUE_ACTORS", "Unique actors")]),
}

###############################################################################
# tables
# field tuple: (name, javaType, options)
#   javaType: Integer, Long, Boolean, Instant, LocalDate, String:<n>, Text, Enum:<EnumName>, Fk:<entity>
#   options: dict(required=bool, editable=bool, label=str, default=...)
###############################################################################
def F(name, jtype, **opts):
   return (name, jtype, opts)

COMMON_TAIL = [F("createDate", "Instant", editable=False), F("modifyDate", "Instant", editable=False)]

TABLES = [
   ("MarketingProduct", "marketing_product", "A product the marketing team promotes: its brief, maturity and official links.", [
      F("name", "String:100", required=True), F("slug", "String:64", required=True), F("tagline", "String:255"),
      F("summary", "Text"), F("maturity", "Enum:ProductMaturity", required=True), F("readinessNotes", "Text"),
      F("primaryUrl", "String:1000"), F("repositoryUrl", "String:1000"), F("documentationUrl", "String:1000"),
      F("releasesUrl", "String:1000"), F("license", "String:100"), F("platforms", "String:255"),
      F("latestRelease", "String:100"), F("adoptionPath", "Text"), F("valuePropositions", "Text"),
      F("ownerUserId", "String:100"), F("lastVerifiedDate", "LocalDate")]),
   ("MarketingAudience", "marketing_audience", "An audience of a product: who they are, the real problem and the task the product helps complete.", [
      F("productId", "Fk:MarketingProduct", required=True), F("name", "String:150", required=True), F("problem", "Text"),
      F("usefulTask", "Text"), F("description", "Text"), F("evidenceNote", "Text")]),
   ("MarketingEvidenceSource", "marketing_evidence_source", "A source that product facts were verified against, with its verification date.", [
      F("productId", "Fk:MarketingProduct", required=True), F("title", "String:255", required=True), F("url", "String:1000", required=True),
      F("sourceType", "Enum:SourceType", required=True), F("publisher", "String:150"), F("verifiedDate", "LocalDate"),
      F("verifiedBy", "String:100"), F("excerpt", "Text"), F("notes", "Text")]),
   ("MarketingClaim", "marketing_claim", "A factual claim about a product, its status (released, planned, disputed) and its source.", [
      F("productId", "Fk:MarketingProduct", required=True), F("claimText", "String:1000", required=True),
      F("status", "Enum:ClaimStatus", required=True), F("sourceId", "Fk:MarketingEvidenceSource"), F("category", "String:64"),
      F("evidenceQuote", "Text"), F("verifiedDate", "LocalDate"), F("discrepancyNote", "Text")]),
   ("MarketingCampaign", "marketing_campaign", "A campaign: one goal, audience, message, channels, call to action and measurable success criteria.", [
      F("productId", "Fk:MarketingProduct", required=True), F("name", "String:200", required=True),
      F("goal", "Enum:CampaignGoal", required=True), F("status", "Enum:CampaignStatus", required=True),
      F("audienceId", "Fk:MarketingAudience"), F("objective", "Text"), F("problem", "Text"), F("messaging", "Text"),
      F("channelPlan", "Text"), F("callToAction", "String:500"), F("destinationUrl", "String:1000"), F("utmCampaign", "String:100"),
      F("scheduleStart", "Instant"), F("scheduleEnd", "Instant"), F("ownerUserId", "String:100"), F("successCriteria", "Text"),
      F("leadQualificationEnabled", "Boolean"), F("qualifyMinFormSubmissions", "Integer"), F("qualifyRequireEmailConsent", "Boolean"),
      F("leadQualificationNote", "Text")]),
   ("MarketingChannel", "marketing_channel", "A channel content can go to, its transport and the rules it must respect.", [
      F("name", "String:100", required=True), F("channelType", "Enum:ChannelType", required=True), F("transportName", "String:64"),
      F("isActive", "Boolean"), F("allowsAutomation", "Boolean"), F("aiContentPolicy", "Enum:AiContentPolicy", required=True),
      F("rulesSummary", "Text"), F("rulesSourceUrl", "String:1000"), F("rulesVerifiedDate", "LocalDate"),
      F("rateLimitPerHour", "Integer"), F("maxRecipientsPerExecution", "Integer")]),
   ("MarketingContent", "marketing_content", "A content item: the exact publication or message proposed for a channel.", [
      F("productId", "Fk:MarketingProduct", editable=False), F("campaignId", "Fk:MarketingCampaign", required=True),
      F("channelId", "Fk:MarketingChannel"), F("title", "String:255", required=True), F("body", "Text"),
      F("destinationUrl", "String:1000"), F("callToAction", "String:500"), F("recipientScope", "String:255"),
      F("scheduledFor", "Instant"), F("status", "Enum:ContentStatus", editable=False), F("authorType", "Enum:AuthorType", editable=False),
      F("revisionNumber", "Integer", editable=False), F("contentHash", "String:64", editable=False),
      F("agentRunId", "Fk:MarketingAgentRun", editable=False), F("ownerUserId", "String:100")]),
   ("MarketingContentRevision", "marketing_content_revision", "An immutable revision of a content item (written on every relevant edit).", [
      F("productId", "Fk:MarketingProduct"), F("contentId", "Fk:MarketingContent", required=True), F("revisionNumber", "Integer", required=True),
      F("title", "String:255"), F("body", "Text"), F("destinationUrl", "String:1000"), F("callToAction", "String:500"),
      F("recipientScope", "String:255"), F("scheduledFor", "Instant"), F("channelId", "Fk:MarketingChannel"),
      F("contentHash", "String:64", required=True), F("authorType", "Enum:AuthorType"), F("authorUserId", "String:100"),
      F("agentRunId", "Fk:MarketingAgentRun"), F("changeNote", "String:500")]),
   ("MarketingApproval", "marketing_approval", "A human approval of one exact content revision, destination, recipient scope and schedule.", [
      F("productId", "Fk:MarketingProduct"), F("contentId", "Fk:MarketingContent", required=True),
      F("revisionId", "Fk:MarketingContentRevision"), F("revisionNumber", "Integer"), F("contentHash", "String:64", required=True),
      F("channelId", "Fk:MarketingChannel"), F("destinationUrl", "String:1000"), F("recipientScope", "String:255"),
      F("scheduledFor", "Instant"), F("status", "Enum:ApprovalStatus", required=True), F("approvedByUserId", "String:100"),
      F("approvedAt", "Instant"), F("note", "String:500"), F("invalidatedAt", "Instant"), F("invalidatedReason", "String:500")]),
   ("MarketingExecution", "marketing_execution", "A durable execution job: one approved content item to one channel transport.", [
      F("productId", "Fk:MarketingProduct"), F("campaignId", "Fk:MarketingCampaign"), F("contentId", "Fk:MarketingContent", required=True),
      F("approvalId", "Fk:MarketingApproval", required=True), F("revisionId", "Fk:MarketingContentRevision"),
      F("channelId", "Fk:MarketingChannel"), F("transportName", "String:64"), F("scheduledFor", "Instant"),
      F("status", "Enum:ExecutionStatus", required=True), F("idempotencyKey", "String:200", required=True),
      F("attemptCount", "Integer"), F("maxAttempts", "Integer"), F("nextAttemptAt", "Instant"), F("lastAttemptAt", "Instant"),
      F("completedAt", "Instant"), F("providerReceiptId", "String:255"), F("statusDetail", "Text"),
      F("requestedByUserId", "String:100"), F("isTestData", "Boolean"), F("leaseOwner", "String:100"), F("leaseExpiresAt", "Instant")]),
   ("MarketingExecutionAttempt", "marketing_execution_attempt", "One attempt to run an execution, with the provider's receipt or failure.", [
      F("productId", "Fk:MarketingProduct"), F("executionId", "Fk:MarketingExecution", required=True), F("attemptNumber", "Integer"),
      F("startedAt", "Instant"), F("finishedAt", "Instant"), F("outcome", "Enum:AttemptOutcome", required=True),
      F("recipientCount", "Integer"), F("providerReceiptId", "String:255"), F("providerStatus", "String:100"), F("message", "Text"),
      F("durationMs", "Long"), F("workerName", "String:100")]),
   ("MarketingTestDelivery", "marketing_test_delivery", "What the controlled test transport accepted: the exact payload, never sent anywhere.", [
      F("productId", "Fk:MarketingProduct"), F("executionId", "Fk:MarketingExecution"), F("idempotencyKey", "String:200", required=True),
      F("recipientKey", "String:200"), F("channelName", "String:100"), F("title", "String:255"), F("body", "Text"),
      F("destinationUrl", "String:1000"), F("receiptId", "String:100"), F("acceptedAt", "Instant"), F("duplicateCount", "Integer")]),
   ("MarketingAgentRun", "marketing_agent_run", "A run of the marketing agent: task, status, provider, model usage and outputs.", [
      F("productId", "Fk:MarketingProduct"), F("campaignId", "Fk:MarketingCampaign"), F("contentId", "Fk:MarketingContent"),
      F("task", "Enum:AgentTask", required=True), F("status", "Enum:AgentRunStatus", required=True), F("instructions", "Text"),
      F("providerName", "String:64"), F("model", "String:100"), F("requestedByUserId", "String:100"), F("startedAt", "Instant"),
      F("finishedAt", "Instant"), F("modelCalls", "Integer"), F("toolCalls", "Integer"), F("inputTokens", "Long"),
      F("outputTokens", "Long"), F("cacheReadTokens", "Long"), F("costMicros", "Long"), F("draftsCreated", "Integer"),
      F("findingsCreated", "Integer"), F("summary", "Text"), F("outputJson", "Text"), F("errorMessage", "Text")]),
   ("MarketingAgentStep", "marketing_agent_step", "One step of an agent run: a model call, a tool call or a deterministic check.", [
      F("productId", "Fk:MarketingProduct"), F("agentRunId", "Fk:MarketingAgentRun", required=True), F("seq", "Integer"),
      F("kind", "Enum:AgentStepKind", required=True), F("toolName", "String:100"), F("outcome", "String:32"), F("summary", "String:1000"),
      F("inputJson", "Text"), F("resultJson", "Text"), F("inputTokens", "Long"), F("outputTokens", "Long"), F("durationMs", "Long")]),
   ("MarketingFinding", "marketing_finding", "A finding for a person to act on: unsupported claims, stale sources, gaps, limitations, recommendations.", [
      F("productId", "Fk:MarketingProduct"), F("campaignId", "Fk:MarketingCampaign"), F("contentId", "Fk:MarketingContent"),
      F("claimId", "Fk:MarketingClaim"), F("sourceId", "Fk:MarketingEvidenceSource"), F("agentRunId", "Fk:MarketingAgentRun"),
      F("findingType", "Enum:FindingType", required=True), F("severity", "Enum:FindingSeverity", required=True),
      F("status", "Enum:FindingStatus", required=True), F("title", "String:255", required=True), F("detail", "Text"),
      F("recommendation", "Text"), F("origin", "String:32")]),
   ("MarketingMetric", "marketing_metric", "A campaign metric: its stage, the event it counts, how it counts and whether it is instrumented.", [
      F("productId", "Fk:MarketingProduct"), F("campaignId", "Fk:MarketingCampaign", required=True), F("stage", "Enum:FunnelStage", required=True),
      F("name", "String:150", required=True), F("eventType", "String:100", required=True), F("countMode", "Enum:CountMode", required=True),
      F("instrumentationStatus", "Enum:InstrumentationStatus", required=True), F("target", "Integer"), F("notes", "Text")]),
   ("MarketingOutcomeEvent", "marketing_outcome_event", "A raw outcome event (deduplicated by source and external id), with its funnel stage and anonymous actor.", [
      F("productId", "Fk:MarketingProduct"), F("campaignId", "Fk:MarketingCampaign"), F("contentId", "Fk:MarketingContent"),
      F("executionId", "Fk:MarketingExecution"), F("stage", "Enum:FunnelStage", required=True), F("eventType", "String:100", required=True),
      F("source", "String:100", required=True), F("externalEventId", "String:200", required=True), F("actorKey", "String:200"),
      F("occurredAt", "Instant"), F("quantity", "Integer"), F("isTestData", "Boolean"), F("note", "String:500")]),
   ("MarketingMetricDaily", "marketing_metric_daily", "A persisted daily rollup of a campaign's outcome events per stage and event type (system-written, recomputable from the raw events).", [
      F("campaignId", "Fk:MarketingCampaign", required=True), F("productId", "Fk:MarketingProduct"), F("dayUtc", "LocalDate", required=True),
      F("funnelStage", "Enum:FunnelStage", required=True), F("eventType", "String:100", required=True), F("isTestData", "Boolean", required=True),
      F("rawCount", "Integer"), F("uniqueActors", "Integer"), F("eventsWithoutActor", "Integer"), F("recomputedAt", "Instant")]),
   ("MarketingPolicy", "marketing_policy", "Runtime switches and limits: agent and execution kill switches, provider, model, budgets, concurrency.", [
      F("name", "String:64", required=True), F("agentEnabled", "Boolean"), F("executionEnabled", "Boolean"),
      F("killSwitchReason", "String:255"), F("providerName", "String:64"), F("model", "String:100"),
      F("dailyTokenLimit", "Long"), F("monthlyCostLimitMicros", "Long"), F("maxConcurrentAgentRuns", "Integer"),
      F("maxToolCallsPerRun", "Integer")]),
]

UNIQUES = {
   "marketing_product": [["slug"]],
   "marketing_content_revision": [["contentId", "revisionNumber"]],
   "marketing_execution": [["idempotencyKey"]],
   "marketing_outcome_event": [["source", "externalEventId"]],
   "marketing_metric_daily": [["campaignId", "dayUtc", "funnelStage", "eventType", "isTestData"]],
   "marketing_test_delivery": [["idempotencyKey", "recipientKey"]],
   "marketing_policy": [["name"]],
   "marketing_channel": [["name"]],
}

ENTITY_TABLE = {e: t for (e, t, _, _) in TABLES}


def snake(name):
   return re.sub(r"(?<=[a-z0-9])([A-Z])", r"_\1", name).lower()


def java_type(jtype):
   if jtype.startswith("String") or jtype == "Text":
      return "String"
   if jtype.startswith("Enum") or jtype.startswith("Fk"):
      return "Integer"
   return jtype


def field_annotation(name, jtype, opts):
   parts = []
   if jtype.startswith("String:"):
      parts.append("maxLength = " + jtype.split(":")[1])
      parts.append("valueTooLongBehavior = ValueTooLongBehavior.ERROR")
   if opts.get("required"):
      parts.append("isRequired = true")
   if opts.get("editable") is False:
      parts.append("isEditable = false")
   if jtype.startswith("Enum:"):
      parts.append("possibleValueSourceName = " + jtype.split(":")[1] + ".NAME")
   if jtype.startswith("Fk:"):
      parts.append("possibleValueSourceName = " + jtype.split(":")[1] + ".TABLE_NAME")
   return "@QField(" + ", ".join(parts) + ")"


def gen_enum(name, desc, values):
   consts = []
   for i, (c, label) in enumerate(values):
      consts.append(f'   {c}({i + 1}, "{label}")')
   pvs = "marketing_" + snake(name)
   return HEADER + f"""
package {PKG}.model.enums;


import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.PossibleValueEnum;


/*******************************************************************************
 ** {desc}
 ** Possible-value source {pvs} (integer ids).
 *******************************************************************************/
public enum {name} implements PossibleValueEnum<Integer>
{{
{(","+chr(10)).join(consts)};

   public static final String NAME = "{pvs}";

   private final Integer id;
   private final String  label;



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   {name}(Integer id, String label)
   {{
      this.id = id;
      this.label = label;
   }}



   /*******************************************************************************
    ** The value with this id, or null.
    *******************************************************************************/
   public static {name} getById(Integer id)
   {{
      if(id == null)
      {{
         return (null);
      }}

      for({name} value : {name}.values())
      {{
         if(value.id.equals(id))
         {{
            return (value);
         }}
      }}
      return (null);
   }}



   /*******************************************************************************
    ** Getter for id
    *******************************************************************************/
   public Integer getId()
   {{
      return (id);
   }}



   /*******************************************************************************
    ** Getter for label
    *******************************************************************************/
   public String getLabel()
   {{
      return (label);
   }}



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public Integer getPossibleValueId()
   {{
      return (id);
   }}



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String getPossibleValueLabel()
   {{
      return (label);
   }}
}}
"""


def gen_entity(entity, table, desc, fields):
   all_fields = [F("id", "Integer", editable=False, pk=True)] + fields + COMMON_TAIL
   imports = {"com.kingsrook.qqq.backend.core.exceptions.QException",
              "com.kingsrook.qqq.backend.core.model.data.QField",
              "com.kingsrook.qqq.backend.core.model.data.QRecord",
              "com.kingsrook.qqq.backend.core.model.data.QRecordEntity"}
   body = []
   for (name, jtype, opts) in all_fields:
      jt = java_type(jtype)
      if jt == "Instant":
         imports.add("java.time.Instant")
      if jt == "LocalDate":
         imports.add("java.time.LocalDate")
      if jtype.startswith("String:"):
         imports.add("com.kingsrook.qqq.backend.core.model.metadata.fields.ValueTooLongBehavior")
      if jtype.startswith("Enum:"):
         imports.add(f"{PKG}.model.enums." + jtype.split(":")[1])
      if opts.get("pk"):
         ann = "@QField(isEditable = false, isPrimaryKey = true)"
      else:
         ann = field_annotation(name, jtype, opts)
      body.append(f"   {ann}\n   private {jt} {name};\n")

   accessors = []
   for (name, jtype, opts) in all_fields:
      jt = java_type(jtype)
      cap = name[0].upper() + name[1:]
      accessors.append(f"""

   /*******************************************************************************
    ** Getter for {name}
    *******************************************************************************/
   public {jt} get{cap}()
   {{
      return (this.{name});
   }}



   /*******************************************************************************
    ** Setter for {name}
    *******************************************************************************/
   public void set{cap}({jt} {name})
   {{
      this.{name} = {name};
   }}



   /*******************************************************************************
    ** Fluent setter for {name}
    *******************************************************************************/
   public {entity} with{cap}({jt} {name})
   {{
      this.{name} = {name};
      return (this);
   }}
""")

   imp = "\n".join("import " + i + ";" for i in sorted(imports, key=lambda s: (not s.startswith("java."), s)))
   return HEADER + f"""
package {PKG}.model;


{imp}


/*******************************************************************************
 ** {desc}
 ** Table {table}.
 *******************************************************************************/
public class {entity} extends QRecordEntity
{{
   public static final String TABLE_NAME = "{table}";

{chr(10).join(body)}


   /*******************************************************************************
    ** Default constructor.
    *******************************************************************************/
   public {entity}()
   {{
   }}



   /*******************************************************************************
    ** Constructor that populates from a QRecord.
    *******************************************************************************/
   public {entity}(QRecord record) throws QException
   {{
      populateFromQRecord(record);
   }}
{"".join(accessors)}
}}
"""


def sql_type(jtype, dialect):
   jt = java_type(jtype)
   if jtype == "Text":
      return "TEXT" if dialect == "postgres" else "CLOB"
   if jtype.startswith("String:"):
      return "VARCHAR(" + jtype.split(":")[1] + ")"
   return {"Integer": "INTEGER", "Long": "BIGINT", "Boolean": "BOOLEAN", "Instant": "TIMESTAMP WITHOUT TIME ZONE" if dialect == "postgres" else "TIMESTAMP",
           "LocalDate": "DATE"}[jt]


def gen_ddl(dialect):
   out = []
   if dialect == "postgres":
      out.append("""-- Marketing QBit (com.kingsrook.qbits:qbit-marketing) - PostgreSQL DDL
--
-- Generated by scripts/generate-model.py (the single spec for entities, enums and DDL).
-- Table names are the QQQ table names (marketing_*), and column names are the
-- snake_case forms of the QQQ field names, which the qbit sets as each field's
-- backend name (MarketingQBitConfig.inferBackendNames, default true).
--
-- Liquibase: paste into a formatted-SQL changeset. Idempotent (IF NOT EXISTS).
""")
   else:
      out.append("-- Marketing QBit - H2 test schema (generated, mirrors docs/ddl/postgres.sql)\n")
   order = [t for (_, t, _, _) in TABLES]
   for (entity, table, desc, fields) in TABLES:
      cols = ["   id INTEGER GENERATED BY DEFAULT AS IDENTITY NOT NULL"]
      for (name, jtype, opts) in fields + COMMON_TAIL:
         col = f"   {snake(name)} {sql_type(jtype, dialect)}"
         if opts.get("required"):
            col += " NOT NULL"
         cols.append(col)
      cols.append(f"   CONSTRAINT pk_{table} PRIMARY KEY (id)")
      for i, uk in enumerate(UNIQUES.get(table, [])):
         cols.append(f"   CONSTRAINT uq_{table}_{i + 1} UNIQUE ({', '.join(snake(c) for c in uk)})")
      out.append(f"-- {desc}\nCREATE TABLE IF NOT EXISTS {table} (\n" + ",\n".join(cols) + "\n);\n")
      for (name, jtype, opts) in fields:
         if jtype.startswith("Fk:"):
            out.append(f"CREATE INDEX IF NOT EXISTS ix_{table}_{snake(name)} ON {table} ({snake(name)});")
      out.append("")
   return "\n".join(out) + "\n"


def write(path, text):
   os.makedirs(os.path.dirname(path), exist_ok=True)
   with open(path, "w", encoding="utf-8", newline="\n") as f:
      f.write(text)


for (name, (desc, values)) in ENUMS.items():
   write(os.path.join(JAVA, "model", "enums", name + ".java"), gen_enum(name, desc, values))
for (entity, table, desc, fields) in TABLES:
   write(os.path.join(JAVA, "model", entity + ".java"), gen_entity(entity, table, desc, fields))
write(os.path.join(ROOT, "docs", "ddl", "postgres.sql"), gen_ddl("postgres"))
write(os.path.join(ROOT, "src", "test", "resources", "marketing-h2.sql"), gen_ddl("h2"))

# a machine-readable list for the metadata builder
lines = ["# table|entity|text fields"]
for (entity, table, desc, fields) in TABLES:
   text_fields = [n for (n, j, o) in fields if j == "Text"]
   lines.append(f"{table}|{entity}|{','.join(text_fields)}")
print("\n".join(lines))
print("enums:", len(ENUMS), "tables:", len(TABLES))
