/*
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

package com.kingsrook.qbits.marketing.metadata;


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.kingsrook.qbits.marketing.MarketingQBitConfig;
import com.kingsrook.qbits.marketing.content.MarketingContentCustomizer;
import com.kingsrook.qbits.marketing.content.ProductFromCampaignCustomizer;
import com.kingsrook.qbits.marketing.model.MarketingAgentRun;
import com.kingsrook.qbits.marketing.model.MarketingAgentStep;
import com.kingsrook.qbits.marketing.model.MarketingApproval;
import com.kingsrook.qbits.marketing.model.MarketingAudience;
import com.kingsrook.qbits.marketing.model.MarketingCampaign;
import com.kingsrook.qbits.marketing.model.MarketingChannel;
import com.kingsrook.qbits.marketing.model.MarketingClaim;
import com.kingsrook.qbits.marketing.model.MarketingContent;
import com.kingsrook.qbits.marketing.model.MarketingContentRevision;
import com.kingsrook.qbits.marketing.model.MarketingEvidenceSource;
import com.kingsrook.qbits.marketing.model.MarketingExecution;
import com.kingsrook.qbits.marketing.model.MarketingExecutionAttempt;
import com.kingsrook.qbits.marketing.model.MarketingFinding;
import com.kingsrook.qbits.marketing.model.MarketingMetric;
import com.kingsrook.qbits.marketing.model.MarketingOutcomeEvent;
import com.kingsrook.qbits.marketing.model.MarketingPolicy;
import com.kingsrook.qbits.marketing.model.MarketingProduct;
import com.kingsrook.qbits.marketing.model.MarketingTestDelivery;
import com.kingsrook.qbits.marketing.model.enums.AgentRunStatus;
import com.kingsrook.qbits.marketing.model.enums.AgentStepKind;
import com.kingsrook.qbits.marketing.model.enums.AgentTask;
import com.kingsrook.qbits.marketing.model.enums.AiContentPolicy;
import com.kingsrook.qbits.marketing.model.enums.ApprovalStatus;
import com.kingsrook.qbits.marketing.model.enums.AttemptOutcome;
import com.kingsrook.qbits.marketing.model.enums.AuthorType;
import com.kingsrook.qbits.marketing.model.enums.CampaignGoal;
import com.kingsrook.qbits.marketing.model.enums.CampaignStatus;
import com.kingsrook.qbits.marketing.model.enums.ChannelType;
import com.kingsrook.qbits.marketing.model.enums.ClaimStatus;
import com.kingsrook.qbits.marketing.model.enums.ContentStatus;
import com.kingsrook.qbits.marketing.model.enums.CountMode;
import com.kingsrook.qbits.marketing.model.enums.ExecutionStatus;
import com.kingsrook.qbits.marketing.model.enums.FindingSeverity;
import com.kingsrook.qbits.marketing.model.enums.FindingStatus;
import com.kingsrook.qbits.marketing.model.enums.FindingType;
import com.kingsrook.qbits.marketing.model.enums.FunnelStage;
import com.kingsrook.qbits.marketing.model.enums.InstrumentationStatus;
import com.kingsrook.qbits.marketing.model.enums.ProductMaturity;
import com.kingsrook.qbits.marketing.model.enums.SourceType;
import com.kingsrook.qbits.marketing.security.MarketingInternalWrite;
import com.kingsrook.qbits.marketing.security.MarketingPermissions;
import com.kingsrook.qbits.marketing.widgets.MarketingWidgets;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizers;
import com.kingsrook.qqq.backend.core.actions.dashboard.widgets.ChildRecordListRenderer;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instances.QInstanceEnricher;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinOn;
import com.kingsrook.qqq.backend.core.model.metadata.joins.JoinType;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QAppMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QAppSection;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.PossibleValueEnum;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSource;
import com.kingsrook.qqq.backend.core.model.metadata.security.RecordSecurityLock;
import com.kingsrook.qqq.backend.core.model.metadata.security.QSecurityKeyType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Capability;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QFieldSection;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Tier;
import com.kingsrook.qqq.backend.core.model.metadata.tables.UniqueKey;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 ** Builds the Marketing tables, joins, child lists, possible-value sources and
 ** the Marketing app.
 **
 ** Conventions (owner rules):
 ** - Standard, visible QQQ tables with full record screens; names are
 **   marketing_* in snake_case; permissions are marketing.*.
 ** - The nav (the Marketing app) lists only the main tables - products,
 **   campaigns, content, executions, agent runs - and the settings tables.
 **   Sub-tables (audiences, evidence, claims, revisions, approvals, attempts,
 **   test deliveries, agent steps, findings, metrics, outcome events) appear
 **   as child lists on their parents; they stay reachable by URL.
 ** - System-written tables (revisions, approvals, executions, attempts, test
 **   deliveries, agent runs, agent steps) are read-only in the UI and API and
 **   guarded by MarketingInternalWrite.
 *******************************************************************************/
public class MarketingMetaDataBuilder
{
   public static final String APP_NAME = "marketing";

   /** The main tables: the nav entries, and the tables to make searchable and publish on the ESB. */
   public static final List<String> MAIN_TABLE_NAMES = List.of(MarketingProduct.TABLE_NAME, MarketingCampaign.TABLE_NAME, MarketingContent.TABLE_NAME,
      MarketingExecution.TABLE_NAME, MarketingAgentRun.TABLE_NAME);

   /** The settings tables (also in the nav). */
   public static final List<String> SETTINGS_TABLE_NAMES = List.of(MarketingChannel.TABLE_NAME, MarketingPolicy.TABLE_NAME);

   /** Written only by Marketing's services. */
   public static final Set<String> SYSTEM_WRITTEN_TABLE_NAMES = Set.of(MarketingContentRevision.TABLE_NAME, MarketingApproval.TABLE_NAME,
      MarketingExecution.TABLE_NAME, MarketingExecutionAttempt.TABLE_NAME, MarketingTestDelivery.TABLE_NAME,
      MarketingAgentRun.TABLE_NAME, MarketingAgentStep.TABLE_NAME);

   private final MarketingQBitConfig config;

   private final Map<String, QTableMetaData>   tables  = new LinkedHashMap<>();
   private final List<QJoinMetaData>           joins   = new ArrayList<>();
   private final List<QWidgetMetaData>         widgets = new ArrayList<>();



   /*******************************************************************************
    ** One table's shape.
    *******************************************************************************/
   private record TableSpec(String name, String label, String icon, Class<? extends QRecordEntity> entity, String labelFormat, List<String> labelFields,
                            List<String> t1Fields, boolean admin)
   {
   }



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   public MarketingMetaDataBuilder(MarketingQBitConfig config)
   {
      this.config = config;
   }



   /*******************************************************************************
    ** Every table spec, in nav order.
    *******************************************************************************/
   private static List<TableSpec> specs()
   {
      return (List.of(
         new TableSpec(MarketingProduct.TABLE_NAME, "Marketing Product", "inventory_2", MarketingProduct.class, "%s", List.of("name"),
            List.of("id", "name", "slug", "maturity", "tagline", "latestRelease", "license", "lastVerifiedDate"), false),
         new TableSpec(MarketingAudience.TABLE_NAME, "Marketing Audience", "groups", MarketingAudience.class, "%s", List.of("name"),
            List.of("id", "productId", "name"), false),
         new TableSpec(MarketingEvidenceSource.TABLE_NAME, "Marketing Evidence Source", "source", MarketingEvidenceSource.class, "%s", List.of("title"),
            List.of("id", "productId", "title", "sourceType", "url", "verifiedDate", "verifiedBy"), false),
         new TableSpec(MarketingClaim.TABLE_NAME, "Marketing Claim", "fact_check", MarketingClaim.class, "%s", List.of("claimText"),
            List.of("id", "productId", "claimText", "status", "sourceId", "verifiedDate", "category"), false),
         new TableSpec(MarketingCampaign.TABLE_NAME, "Marketing Campaign", "campaign", MarketingCampaign.class, "%s", List.of("name"),
            List.of("id", "productId", "name", "goal", "status", "audienceId", "ownerUserId", "scheduleStart", "scheduleEnd"), false),
         new TableSpec(MarketingChannel.TABLE_NAME, "Marketing Channel", "hub", MarketingChannel.class, "%s", List.of("name"),
            List.of("id", "name", "channelType", "transportName", "isActive", "allowsAutomation", "aiContentPolicy", "rulesVerifiedDate"), true),
         new TableSpec(MarketingContent.TABLE_NAME, "Marketing Content", "edit_note", MarketingContent.class, "%s", List.of("title"),
            List.of("id", "campaignId", "title", "channelId", "status", "scheduledFor", "revisionNumber", "authorType"), false),
         new TableSpec(MarketingContentRevision.TABLE_NAME, "Marketing Content Revision", "history", MarketingContentRevision.class, "Revision %s", List.of("revisionNumber"),
            List.of("id", "contentId", "revisionNumber", "authorType", "authorUserId", "changeNote"), false),
         new TableSpec(MarketingApproval.TABLE_NAME, "Marketing Approval", "verified", MarketingApproval.class, "Approval #%s", List.of("id"),
            List.of("id", "contentId", "status", "revisionNumber", "approvedByUserId", "approvedAt", "scheduledFor"), false),
         new TableSpec(MarketingExecution.TABLE_NAME, "Marketing Execution", "send", MarketingExecution.class, "Execution #%s", List.of("id"),
            List.of("id", "contentId", "status", "transportName", "scheduledFor", "attemptCount", "providerReceiptId", "isTestData"), false),
         new TableSpec(MarketingExecutionAttempt.TABLE_NAME, "Marketing Execution Attempt", "replay",
            MarketingExecutionAttempt.class, "Attempt %s", List.of("attemptNumber"),
            List.of("id", "executionId", "attemptNumber", "outcome", "providerReceiptId", "startedAt"), false),
         new TableSpec(MarketingTestDelivery.TABLE_NAME, "Marketing Test Delivery", "science", MarketingTestDelivery.class, "Test delivery #%s", List.of("id"),
            List.of("id", "executionId", "recipientKey", "receiptId", "acceptedAt", "duplicateCount"), false),
         new TableSpec(MarketingAgentRun.TABLE_NAME, "Marketing Agent Run", "smart_toy", MarketingAgentRun.class, "Agent run #%s", List.of("id"),
            List.of("id", "task", "status", "campaignId", "providerName", "model", "startedAt", "finishedAt"), false),
         new TableSpec(MarketingAgentStep.TABLE_NAME, "Marketing Agent Step", "footprint", MarketingAgentStep.class, "Step %s", List.of("seq"),
            List.of("id", "agentRunId", "seq", "kind", "toolName", "outcome"), false),
         new TableSpec(MarketingFinding.TABLE_NAME, "Marketing Finding", "flag", MarketingFinding.class, "%s", List.of("title"),
            List.of("id", "title", "findingType", "severity", "status", "productId", "campaignId", "contentId"), false),
         new TableSpec(MarketingMetric.TABLE_NAME, "Marketing Metric", "monitoring", MarketingMetric.class, "%s", List.of("name"),
            List.of("id", "campaignId", "name", "stage", "eventType", "countMode", "instrumentationStatus", "target"), false),
         new TableSpec(MarketingOutcomeEvent.TABLE_NAME, "Marketing Outcome Event", "insights", MarketingOutcomeEvent.class, "%s %s", List.of("eventType", "externalEventId"),
            List.of("id", "campaignId", "stage", "eventType", "source", "externalEventId", "occurredAt", "isTestData"), false),
         new TableSpec(MarketingPolicy.TABLE_NAME, "Marketing Policy", "policy", MarketingPolicy.class, "%s", List.of("name"),
            List.of("id", "name", "agentEnabled", "executionEnabled", "providerName", "model"), true)
      ));
   }



   /*******************************************************************************
    ** The security key type of the product boundary (when configured).
    *******************************************************************************/
   public QSecurityKeyType buildProductSecurityKeyType()
   {
      String keyType = config.getProductSecurityKeyType();
      return (new QSecurityKeyType().withName(keyType).withAllAccessKeyName(keyType + "_all_access"));
   }



   /*******************************************************************************
    ** Build every table (and the joins and child-list widgets they show).
    *******************************************************************************/
   public List<QTableMetaData> buildTables() throws QException
   {
      for(TableSpec spec : specs())
      {
         tables.put(spec.name(), buildTable(spec));
      }

      ////////////////////////////////////////////////////////////////////////
      // relations: each parent shows its children as lists (owner rule:    //
      // sub-tables are relations and child lists on their parents)        //
      ////////////////////////////////////////////////////////////////////////
      child(MarketingProduct.TABLE_NAME, MarketingAudience.TABLE_NAME, "productId", "Audiences", true);
      child(MarketingProduct.TABLE_NAME, MarketingEvidenceSource.TABLE_NAME, "productId", "Evidence Sources", true);
      child(MarketingProduct.TABLE_NAME, MarketingClaim.TABLE_NAME, "productId", "Claims", true);
      child(MarketingProduct.TABLE_NAME, MarketingCampaign.TABLE_NAME, "productId", "Campaigns", true);
      child(MarketingProduct.TABLE_NAME, MarketingFinding.TABLE_NAME, "productId", "Findings", false);
      child(MarketingEvidenceSource.TABLE_NAME, MarketingClaim.TABLE_NAME, "sourceId", "Claims From This Source", true);

      sectionWidget(MarketingCampaign.TABLE_NAME, MarketingWidgets.CAMPAIGN_RESULTS, "Results");
      child(MarketingCampaign.TABLE_NAME, MarketingContent.TABLE_NAME, "campaignId", "Content", true);
      child(MarketingCampaign.TABLE_NAME, MarketingMetric.TABLE_NAME, "campaignId", "Measurement Plan", true);
      child(MarketingCampaign.TABLE_NAME, MarketingFinding.TABLE_NAME, "campaignId", "Findings", false);
      child(MarketingCampaign.TABLE_NAME, MarketingAgentRun.TABLE_NAME, "campaignId", "Agent Runs", false);
      child(MarketingCampaign.TABLE_NAME, MarketingOutcomeEvent.TABLE_NAME, "campaignId", "Outcome Events", true);

      sectionWidget(MarketingContent.TABLE_NAME, MarketingWidgets.CONTENT_PROPOSAL, "Proposed Publication");
      child(MarketingContent.TABLE_NAME, MarketingContentRevision.TABLE_NAME, "contentId", "Revision History", false);
      child(MarketingContent.TABLE_NAME, MarketingApproval.TABLE_NAME, "contentId", "Approvals", false);
      child(MarketingContent.TABLE_NAME, MarketingExecution.TABLE_NAME, "contentId", "Executions", false);
      child(MarketingContent.TABLE_NAME, MarketingFinding.TABLE_NAME, "contentId", "Findings", false);

      child(MarketingExecution.TABLE_NAME, MarketingExecutionAttempt.TABLE_NAME, "executionId", "Attempts", false);
      child(MarketingExecution.TABLE_NAME, MarketingTestDelivery.TABLE_NAME, "executionId", "Test Deliveries", false);

      child(MarketingAgentRun.TABLE_NAME, MarketingAgentStep.TABLE_NAME, "agentRunId", "Steps", false);
      child(MarketingAgentRun.TABLE_NAME, MarketingFinding.TABLE_NAME, "agentRunId", "Findings", false);
      child(MarketingAgentRun.TABLE_NAME, MarketingContent.TABLE_NAME, "agentRunId", "Drafts", false);

      for(QTableMetaData table : tables.values())
      {
         addDatesSection(table);
      }
      return (new ArrayList<>(tables.values()));
   }



   /*******************************************************************************
    ** One table.
    *******************************************************************************/
   private QTableMetaData buildTable(TableSpec spec) throws QException
   {
      QTableMetaData table = new QTableMetaData()
         .withName(spec.name())
         .withLabel(spec.label())
         .withIcon(new QIcon(spec.icon()))
         .withBackendName(config.getBackendName())
         .withPrimaryKeyField("id")
         .withFieldsFromEntity(spec.entity())
         .withRecordLabelFormat(spec.labelFormat())
         .withRecordLabelFields(spec.labelFields())
         .withPermissionRules(spec.admin() ? MarketingPermissions.adminTableRules() : MarketingPermissions.tableRules());

      ////////////////////////////////////////////////////
      // long text columns, JSON and the read-only bits //
      ////////////////////////////////////////////////////
      List<String> textFields = new ArrayList<>();
      for(QFieldMetaData field : table.getFields().values())
      {
         if(field.getType() == QFieldType.STRING && (field.getMaxLength() == null || field.getMaxLength() == Integer.MAX_VALUE))
         {
            field.setType(QFieldType.TEXT);
            textFields.add(field.getName());
         }
      }

      if(Boolean.TRUE.equals(config.getInferBackendNames()))
      {
         QInstanceEnricher.setInferredFieldBackendNames(table);
      }

      ///////////////////////////////////////////////////////////////
      // sections: T1 identity, T2 details, T2 text, T3 dates      //
      ///////////////////////////////////////////////////////////////
      Set<String> remaining = new LinkedHashSet<>(table.getFields().keySet());
      remaining.removeAll(List.of("createDate", "modifyDate"));
      remaining.removeAll(spec.t1Fields());
      remaining.removeAll(textFields);
      table.addSection(new QFieldSection("identity", "Summary", new QIcon(spec.icon()), Tier.T1, spec.t1Fields()));
      if(!remaining.isEmpty())
      {
         table.addSection(new QFieldSection("details", "Details", new QIcon("info"), Tier.T2, new ArrayList<>(remaining)));
      }
      if(!textFields.isEmpty())
      {
         table.addSection(new QFieldSection("text", textSectionLabel(spec.name()), new QIcon("notes"), Tier.T2, textFields));
      }

      ///////////////////
      // unique keys   //
      ///////////////////
      switch(spec.name())
      {
         case MarketingProduct.TABLE_NAME -> table.withUniqueKey(new UniqueKey("slug"));
         case MarketingChannel.TABLE_NAME, MarketingPolicy.TABLE_NAME -> table.withUniqueKey(new UniqueKey("name"));
         case MarketingContentRevision.TABLE_NAME -> table.withUniqueKey(new UniqueKey("contentId", "revisionNumber"));
         case MarketingExecution.TABLE_NAME -> table.withUniqueKey(new UniqueKey("idempotencyKey"));
         case MarketingOutcomeEvent.TABLE_NAME -> table.withUniqueKey(new UniqueKey("source", "externalEventId"));
         case MarketingTestDelivery.TABLE_NAME -> table.withUniqueKey(new UniqueKey("idempotencyKey", "recipientKey"));
         default ->
         {
         }
      }

      ////////////////////////////////////////////////////////////////
      // customizers: system-written tables are guarded; content    //
      // keeps revisions and invalidates approvals; campaign-owned  //
      // rows follow their campaign's product                       //
      ////////////////////////////////////////////////////////////////
      if(SYSTEM_WRITTEN_TABLE_NAMES.contains(spec.name()))
      {
         table.withoutCapabilities(Capability.TABLE_INSERT, Capability.TABLE_UPDATE, Capability.TABLE_DELETE);
         QCodeReference guard = new QCodeReference(MarketingInternalWrite.class);
         table.withCustomizer(TableCustomizers.PRE_INSERT_RECORD, guard);
         table.withCustomizer(TableCustomizers.PRE_UPDATE_RECORD, guard);
         table.withCustomizer(TableCustomizers.PRE_DELETE_RECORD, guard);
      }
      if(MarketingContent.TABLE_NAME.equals(spec.name()))
      {
         QCodeReference content = new QCodeReference(MarketingContentCustomizer.class);
         table.withCustomizer(TableCustomizers.PRE_INSERT_RECORD, content);
         table.withCustomizer(TableCustomizers.POST_INSERT_RECORD, content);
         table.withCustomizer(TableCustomizers.PRE_UPDATE_RECORD, content);
         table.withCustomizer(TableCustomizers.POST_UPDATE_RECORD, content);
      }
      if(Set.of(MarketingMetric.TABLE_NAME, MarketingOutcomeEvent.TABLE_NAME, MarketingFinding.TABLE_NAME).contains(spec.name()))
      {
         QCodeReference product = new QCodeReference(ProductFromCampaignCustomizer.class);
         table.withCustomizer(TableCustomizers.PRE_INSERT_RECORD, product);
         table.withCustomizer(TableCustomizers.PRE_UPDATE_RECORD, product);
      }

      ////////////////////////////////////////////////////////////////////
      // the optional product (tenant) boundary: a record lock on the   //
      // product, for products and every table that carries productId   //
      ////////////////////////////////////////////////////////////////////
      if(StringUtils.hasContent(config.getProductSecurityKeyType()))
      {
         String lockField = MarketingProduct.TABLE_NAME.equals(spec.name()) ? "id" : (table.getFields().containsKey("productId") ? "productId" : null);
         if(lockField != null)
         {
            table.withRecordSecurityLock(new RecordSecurityLock()
               .withSecurityKeyType(config.getProductSecurityKeyType())
               .withFieldName(lockField)
               .withNullValueBehavior(RecordSecurityLock.NullValueBehavior.DENY));
         }
      }

      return (table);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static String textSectionLabel(String tableName)
   {
      return switch(tableName)
      {
         case MarketingContent.TABLE_NAME, MarketingContentRevision.TABLE_NAME, MarketingTestDelivery.TABLE_NAME -> "Text";
         case MarketingAgentRun.TABLE_NAME, MarketingAgentStep.TABLE_NAME -> "Input and Output";
         default -> "Notes";
      };
   }



   /*******************************************************************************
    ** A one-to-many join from parent to child on child.fkField, with a child
    ** list widget section on the parent.
    *******************************************************************************/
   private void child(String parentName, String childName, String fkField, String label, boolean canAdd)
   {
      QJoinMetaData join = new QJoinMetaData()
         .withName(parentName + "_join_" + childName + ("productId".equals(fkField) || "campaignId".equals(fkField) || "contentId".equals(fkField)
            || "executionId".equals(fkField) || "agentRunId".equals(fkField) ? "" : "_by_" + fkField))
         .withLeftTable(parentName)
         .withRightTable(childName)
         .withType(JoinType.ONE_TO_MANY)
         .withJoinOn(new JoinOn("id", fkField));
      joins.add(join);

      QWidgetMetaData widget = ChildRecordListRenderer.widgetMetaDataBuilder(join)
         .withLabel(label)
         .withCanAddChildRecord(canAdd)
         .withMaxRows(25)
         .getWidgetMetaData()
         .withGridColumns(12);
      widgets.add(widget);

      tables.get(parentName).addSection(new QFieldSection(widget.getName(), label, new QIcon(tables.get(childName).getIcon().getName()), Tier.T2, List.of())
         .withWidgetName(widget.getName()));
   }



   /*******************************************************************************
    ** A widget (produced by MarketingWidgets) as a section of a table.
    *******************************************************************************/
   private void sectionWidget(String tableName, String widgetName, String label)
   {
      tables.get(tableName).addSection(new QFieldSection(widgetName, label, new QIcon("dashboard"), Tier.T2, List.of()).withWidgetName(widgetName));
   }



   /*******************************************************************************
    ** Dates last.
    *******************************************************************************/
   private static void addDatesSection(QTableMetaData table)
   {
      table.addSection(new QFieldSection("dates", "Dates", new QIcon("calendar_month"), Tier.T3, List.of("createDate", "modifyDate")));
   }



   /*******************************************************************************
    ** Possible-value sources: every enum, and every table (for its id fields).
    *******************************************************************************/
   public List<QPossibleValueSource> buildPossibleValueSources()
   {
      List<QPossibleValueSource> list = new ArrayList<>();
      list.add(enumSource(ProductMaturity.NAME, ProductMaturity.values()));
      list.add(enumSource(SourceType.NAME, SourceType.values()));
      list.add(enumSource(ClaimStatus.NAME, ClaimStatus.values()));
      list.add(enumSource(CampaignGoal.NAME, CampaignGoal.values()));
      list.add(enumSource(CampaignStatus.NAME, CampaignStatus.values()));
      list.add(enumSource(ChannelType.NAME, ChannelType.values()));
      list.add(enumSource(AiContentPolicy.NAME, AiContentPolicy.values()));
      list.add(enumSource(ContentStatus.NAME, ContentStatus.values()));
      list.add(enumSource(AuthorType.NAME, AuthorType.values()));
      list.add(enumSource(ApprovalStatus.NAME, ApprovalStatus.values()));
      list.add(enumSource(ExecutionStatus.NAME, ExecutionStatus.values()));
      list.add(enumSource(AttemptOutcome.NAME, AttemptOutcome.values()));
      list.add(enumSource(AgentTask.NAME, AgentTask.values()));
      list.add(enumSource(AgentRunStatus.NAME, AgentRunStatus.values()));
      list.add(enumSource(AgentStepKind.NAME, AgentStepKind.values()));
      list.add(enumSource(FindingType.NAME, FindingType.values()));
      list.add(enumSource(FindingSeverity.NAME, FindingSeverity.values()));
      list.add(enumSource(FindingStatus.NAME, FindingStatus.values()));
      list.add(enumSource(FunnelStage.NAME, FunnelStage.values()));
      list.add(enumSource(InstrumentationStatus.NAME, InstrumentationStatus.values()));
      list.add(enumSource(CountMode.NAME, CountMode.values()));
      for(String tableName : tables.keySet())
      {
         list.add(QPossibleValueSource.newForTable(tableName));
      }
      return (list);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static <T extends Enum<T> & PossibleValueEnum<Integer>> QPossibleValueSource enumSource(String name, T[] values)
   {
      return (QPossibleValueSource.newForEnum(name, values));
   }



   /*******************************************************************************
    ** The Marketing app: its home is the Marketing dashboard (the one place that
    ** owns the overview); the nav lists the main tables and the settings.
    *******************************************************************************/
   public QAppMetaData buildApp(List<String> homeWidgetNames)
   {
      QAppMetaData app = new QAppMetaData()
         .withName(APP_NAME)
         .withLabel("Marketing")
         .withIcon(new QIcon("campaign"))
         .withPermissionRules(MarketingPermissions.hasAccessRules(MarketingPermissions.BASE))
         .withWidgets(homeWidgetNames)
         .withSection(new QAppSection().withName("marketingWork").withLabel("Marketing").withIcon(new QIcon("campaign")).withTables(new ArrayList<>(MAIN_TABLE_NAMES)))
         .withSection(new QAppSection().withName("marketingSettings").withLabel("Settings").withIcon(new QIcon("tune")).withTables(new ArrayList<>(SETTINGS_TABLE_NAMES)));

      for(String tableName : MAIN_TABLE_NAMES)
      {
         app.withChild(tables.get(tableName));
      }
      for(String tableName : SETTINGS_TABLE_NAMES)
      {
         app.withChild(tables.get(tableName));
      }
      return (app);
   }



   /*******************************************************************************
    ** Getter for joins
    *******************************************************************************/
   public List<QJoinMetaData> getJoins()
   {
      return (joins);
   }



   /*******************************************************************************
    ** Getter for the child-list widgets
    *******************************************************************************/
   public List<QWidgetMetaData> getChildListWidgets()
   {
      return (widgets);
   }

}
