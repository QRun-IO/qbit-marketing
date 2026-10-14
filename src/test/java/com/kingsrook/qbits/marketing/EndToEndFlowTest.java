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

package com.kingsrook.qbits.marketing;


import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import com.kingsrook.qbits.assistant.provider.ModelUsage;
import com.kingsrook.qbits.marketing.agent.MarketingAgentService;
import com.kingsrook.qbits.marketing.agent.tools.MarketingTools;
import com.kingsrook.qbits.marketing.measurement.MeasurementReport;
import com.kingsrook.qbits.marketing.model.MarketingAgentRun;
import com.kingsrook.qbits.marketing.model.MarketingAgentStep;
import com.kingsrook.qbits.marketing.model.MarketingApproval;
import com.kingsrook.qbits.marketing.model.MarketingCampaign;
import com.kingsrook.qbits.marketing.model.MarketingChannel;
import com.kingsrook.qbits.marketing.model.MarketingClaim;
import com.kingsrook.qbits.marketing.model.MarketingContent;
import com.kingsrook.qbits.marketing.model.MarketingContentRevision;
import com.kingsrook.qbits.marketing.model.MarketingExecution;
import com.kingsrook.qbits.marketing.model.MarketingFinding;
import com.kingsrook.qbits.marketing.model.MarketingOutcomeEvent;
import com.kingsrook.qbits.marketing.model.MarketingProduct;
import com.kingsrook.qbits.marketing.model.MarketingTestDelivery;
import com.kingsrook.qbits.marketing.model.enums.AgentRunStatus;
import com.kingsrook.qbits.marketing.model.enums.AgentTask;
import com.kingsrook.qbits.marketing.model.enums.ApprovalStatus;
import com.kingsrook.qbits.marketing.model.enums.AuthorType;
import com.kingsrook.qbits.marketing.model.enums.ClaimStatus;
import com.kingsrook.qbits.marketing.model.enums.ContentStatus;
import com.kingsrook.qbits.marketing.model.enums.ExecutionStatus;
import com.kingsrook.qbits.marketing.model.enums.FindingType;
import com.kingsrook.qbits.marketing.model.enums.FunnelStage;
import com.kingsrook.qbits.marketing.processes.MarketingActions;
import com.kingsrook.qbits.marketing.processes.MarketingTableProcesses;
import com.kingsrook.qbits.marketing.security.MarketingPermissions;
import com.kingsrook.qbits.marketing.seed.MarketingStarterData;
import com.kingsrook.qqq.backend.core.actions.processes.QProcessCallbackFactory;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** The complete flow, inside QQQ, with verified starter data and a scripted
 ** (synthetic) model:
 **
 ** product evidence -> campaign -> agent run -> saved drafts -> exact human
 ** review / approval -> controlled test execution -> recorded outcomes ->
 ** agent recommendations.
 **
 ** Three people with separate permissions: a preparer (drafts, agent), an
 ** approver, and an executor.
 *******************************************************************************/
class EndToEndFlowTest extends BaseMarketingTest
{
   private ScriptedModelProvider model;



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   protected void customizeConfig(MarketingQBitConfig config)
   {
      model = new ScriptedModelProvider("scripted").withUsagePerCall(new ModelUsage().withInputTokens(1200L).withOutputTokens(300L));
      config.withModelProvider(model).withModel("scripted-model");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   static QSession preparer()
   {
      return (user("pat.preparer", MarketingPermissions.APP_ACCESS, MarketingPermissions.READ, MarketingPermissions.WRITE, MarketingPermissions.ADMIN_READ,
         MarketingPermissions.PREPARE, MarketingPermissions.AGENT));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   static QSession approver()
   {
      return (user("alex.approver", MarketingPermissions.APP_ACCESS, MarketingPermissions.READ, MarketingPermissions.APPROVE));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   static QSession executor()
   {
      return (user("eve.executor", MarketingPermissions.APP_ACCESS, MarketingPermissions.READ, MarketingPermissions.EXECUTE));
   }



   /*******************************************************************************
    ** Run a record action headlessly on one record (no frontend steps).
    *******************************************************************************/
   static RunProcessOutput runAction(String processName, Serializable id) throws QException
   {
      return (new RunProcessAction().execute(new RunProcessInput()
         .withProcessName(processName)
         .withCallback(QProcessCallbackFactory.forPrimaryKey("id", id))
         .withFrontendStepBehavior(RunProcessInput.FrontendStepBehavior.SKIP)));
   }



   /*******************************************************************************
    ** An agent run's steps, for assertion messages.
    *******************************************************************************/
   static String steps(Integer runId) throws QException
   {
      StringBuilder text = new StringBuilder();
      for(QRecord step : MarketingStore.queryBy(MarketingAgentStep.TABLE_NAME, "agentRunId", runId))
      {
         text.append("\n").append(step.getValue("seq")).append(" ").append(step.getValue("toolName")).append(" ").append(step.getValue("outcome")).append(": ")
            .append(step.getValue("summary")).append(" | ").append(step.getValueString("resultJson") == null ? "" : step.getValueString("resultJson").substring(0, Math.min(400, step.getValueString("resultJson").length())));
      }
      return (text.toString());
   }



   /*******************************************************************************
    ** The whole flow.
    *******************************************************************************/
   @Test
   void testCompleteFlow() throws Exception
   {
      ///////////////////////////////////////////////////////////
      // 1. product evidence: the verified starter data        //
      ///////////////////////////////////////////////////////////
      as(admin());
      MarketingStarterData.Result loaded = new MarketingStarterData().load();
      assertThat(loaded.products()).isEqualTo(3);
      assertThat(loaded.claims()).isGreaterThan(20);
      assertThat(new MarketingStarterData().load().products()).as("idempotent").isZero();

      QRecord product  = MarketingStore.queryBy(MarketingProduct.TABLE_NAME, "slug", "qqq").get(0);
      QRecord campaign = MarketingStore.queryBy(MarketingCampaign.TABLE_NAME, "name", "QQQ 4.0: from quickstart to your first table").get(0);
      QRecord sink     = MarketingStore.queryBy(MarketingChannel.TABLE_NAME, "name", "Controlled test sink").get(0);
      Integer released = MarketingStore.query(MarketingClaim.TABLE_NAME, new com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter(
         new com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria("productId", com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator.EQUALS, product.getValue("id")),
         new com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria("status", com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator.EQUALS, ClaimStatus.RELEASED.getId())))
         .get(0).getValueInteger("id");
      Integer campaignId = campaign.getValueInteger("id");

      ///////////////////////////////////////////////////////
      // 2-3. the agent drafts (as the preparer)           //
      ///////////////////////////////////////////////////////
      String body = "QQQ 4.0 is on Maven Central: you can try it with the quickstart in a few minutes. Java 21, Docker and Bash are required.";
      model.thenToolUses("Reading the brief first.", List.of(
            new Object[] { MarketingTools.GET_CAMPAIGN_BRIEF, Map.of("campaign_id", campaignId) },
            new Object[] { MarketingTools.LIST_PRODUCT_CLAIMS, Map.of("product_id", product.getValueInteger("id")) },
            new Object[] { MarketingTools.LIST_CHANNELS, Map.of() }))
         .thenToolUse(MarketingTools.SAVE_DRAFT, Map.of("campaign_id", campaignId, "channel_id", sink.getValueInteger("id"), "title", "Try QQQ 4.0 in a few minutes",
            "body", body, "claim_ids", List.of(released)))
         .thenText("I saved one draft for review on the controlled test sink.");

      as(preparer());
      MarketingAgentRun draftRun = new MarketingAgentService().run(AgentTask.DRAFT_CONTENT, campaignId, null, null);
      assertThat(draftRun.getErrorMessage()).isNull();
      assertThat(AgentRunStatus.getById(draftRun.getStatus())).isEqualTo(AgentRunStatus.SUCCEEDED);
      assertThat(draftRun.getDraftsCreated()).as(steps(draftRun.getId())).isEqualTo(1);
      assertThat(draftRun.getModelCalls()).isEqualTo(3);
      assertThat(draftRun.getToolCalls()).isEqualTo(4);
      assertThat(draftRun.getInputTokens()).isEqualTo(3600);
      assertThat(draftRun.getSummary()).contains("saved one draft");
      assertThat(MarketingStore.queryBy(MarketingAgentStep.TABLE_NAME, "agentRunId", draftRun.getId())).hasSize(1 + 3 + 4);

      //////////////////////////////////////////////////////////////////////
      // the model saw only enveloped, untrusted data and no CRM contacts //
      //////////////////////////////////////////////////////////////////////
      String lastRequest = model.getRequestJson().get(model.getRequestJson().size() - 1);
      assertThat(lastRequest).contains("trust=\\\"untrusted\\\"").contains("Maven Central");

      QRecord draft = MarketingStore.queryBy(MarketingContent.TABLE_NAME, "agentRunId", draftRun.getId()).get(0);
      Integer contentId = draft.getValueInteger("id");
      assertThat(draft.getValueInteger("status")).isEqualTo(ContentStatus.DRAFT.getId());
      assertThat(draft.getValueInteger("authorType")).isEqualTo(AuthorType.AGENT.getId());
      assertThat(MarketingStore.queryBy(MarketingApproval.TABLE_NAME, "contentId", contentId)).as("the agent has no publication authority").isEmpty();
      assertThat(MarketingStore.queryBy(MarketingExecution.TABLE_NAME, "contentId", contentId)).isEmpty();

      ///////////////////////////////////////////////////////////////
      // 4. a person sets the schedule (an edit: a new revision)    //
      ///////////////////////////////////////////////////////////////
      MarketingStore.update(MarketingContent.TABLE_NAME, new QRecord().withValue("id", contentId).withValue("scheduledFor", Instant.now().minusSeconds(5)));
      assertThat(reload(MarketingContent.TABLE_NAME, contentId).getValueInteger("revisionNumber")).isEqualTo(2);
      assertThat(MarketingStore.queryBy(MarketingContentRevision.TABLE_NAME, "contentId", contentId)).hasSize(2);

      RunProcessOutput submitted = runAction(MarketingActions.SUBMIT_FOR_REVIEW, contentId);
      assertThat(submitted.getException()).isEmpty();
      assertThat(reload(MarketingContent.TABLE_NAME, contentId).getValueInteger("status")).isEqualTo(ContentStatus.IN_REVIEW.getId());

      ///////////////////////////////////////////////////////////
      // 5. exact human approval (as the approver)             //
      ///////////////////////////////////////////////////////////
      as(approver());
      RunProcessOutput approved = runAction(MarketingActions.APPROVE, contentId);
      assertThat(approved.getException()).isEmpty();
      QRecord approval = MarketingStore.queryBy(MarketingApproval.TABLE_NAME, "contentId", contentId).get(0);
      assertThat(approval.getValueInteger("status")).isEqualTo(ApprovalStatus.ACTIVE.getId());
      assertThat(approval.getValueString("approvedByUserId")).isEqualTo("alex.approver");
      assertThat(approval.getValueString("contentHash")).isEqualTo(reload(MarketingContent.TABLE_NAME, contentId).getValueString("contentHash"));
      assertThat(approval.getValueInteger("revisionNumber")).isEqualTo(2);

      //////////////////////////////////////////////////////////////
      // 6. schedule and the controlled test execution (executor) //
      //////////////////////////////////////////////////////////////
      as(executor());
      assertThat(runAction(MarketingActions.SCHEDULE, contentId).getException()).isEmpty();
      QRecord execution = MarketingStore.queryBy(MarketingExecution.TABLE_NAME, "contentId", contentId).get(0);
      assertThat(execution.getValueBoolean("isTestData")).isTrue();

      RunProcessOutput worker = new RunProcessAction().execute(new RunProcessInput().withProcessName(MarketingTableProcesses.RUN_DUE_EXECUTIONS)
         .withFrontendStepBehavior(RunProcessInput.FrontendStepBehavior.SKIP));
      assertThat((String) worker.getValues().get("summary")).contains("accepted by provider");

      execution = reload(MarketingExecution.TABLE_NAME, execution.getValueInteger("id"));
      assertThat(execution.getValueInteger("status")).isEqualTo(ExecutionStatus.ACCEPTED.getId());
      List<QRecord> deliveries = MarketingStore.queryBy(MarketingTestDelivery.TABLE_NAME, "executionId", execution.getValue("id"));
      assertThat(deliveries).hasSize(1);
      assertThat(deliveries.get(0).getValueString("body")).as("exactly the approved text").isEqualTo(body);
      assertThat(reload(MarketingApproval.TABLE_NAME, approval.getValueInteger("id")).getValueInteger("status")).isEqualTo(ApprovalStatus.CONSUMED.getId());
      assertThat(reload(MarketingContent.TABLE_NAME, contentId).getValueInteger("status")).isEqualTo(ContentStatus.ACCEPTED.getId());

      ///////////////////////////////////////////////////////////////////
      // 7. recorded outcomes: the provider acceptance (test data) and //
      //    controlled fixture events, also test data                  //
      ///////////////////////////////////////////////////////////////////
      as(admin());
      List<QRecord> events = MarketingStore.queryBy(MarketingOutcomeEvent.TABLE_NAME, "campaignId", campaignId);
      assertThat(events).hasSize(1);
      assertThat(events.get(0).getValueInteger("stage")).isEqualTo(FunnelStage.DELIVERY.getId());
      assertThat(events.get(0).getValueBoolean("isTestData")).isTrue();
      for(int i = 1; i <= 3; i++)
      {
         MarketingStore.insert(new MarketingOutcomeEvent().withCampaignId(campaignId).withStage(FunnelStage.AWARENESS.getId()).withEventType("try_page_visit")
            .withSource("fixture").withExternalEventId("visit-" + i).withActorKey("visitor-" + (i % 2)).withIsTestData(true));
      }

      MeasurementReport report = MeasurementReport.forCampaign(campaignId);
      assertThat(report.getTestEvents()).isEqualTo(4);
      assertThat(report.getRows()).anySatisfy(r ->
      {
         assertThat(r.eventType()).isEqualTo("try_page_visit");
         assertThat(r.headline()).isEqualTo(MeasurementReport.NOT_MEASURED);
      });
      assertThat(report.getRows()).anySatisfy(r ->
      {
         assertThat(r.eventType()).isEqualTo("provider_accepted");
         assertThat(r.rawEvents()).as("test data excluded from real numbers").isZero();
      });

      ////////////////////////////////////////////////////
      // 8. agent recommendations (as the preparer)     //
      ////////////////////////////////////////////////////
      model.thenToolUse(MarketingTools.GET_CAMPAIGN_RESULTS, Map.of("campaign_id", campaignId))
         .thenToolUse(MarketingTools.RECORD_FINDING, Map.of("campaign_id", campaignId, "finding_type", "RECOMMENDATION", "severity", "INFO",
            "title", "Instrument quickstart completion before the next send",
            "detail", "Awareness and activation are not instrumented, so this campaign's effect is unknown; the only recorded events are test data.",
            "recommendation", "Add an opt-in 'quickstart finished' event, then compare the tutorial against a release-notes-only variant."))
         .thenText("Results are not measurable yet; I recommended instrumenting quickstart completion first.");

      as(preparer());
      MarketingAgentRun analysis = new MarketingAgentService().run(AgentTask.ANALYZE_RESULTS, campaignId, null, null);
      assertThat(AgentRunStatus.getById(analysis.getStatus())).isEqualTo(AgentRunStatus.SUCCEEDED);
      assertThat(analysis.getFindingsCreated()).isEqualTo(1);
      String resultsSeenByModel = model.getRequestJson().get(model.getRequestJson().size() - 2);
      assertThat(resultsSeenByModel).contains("Not measured").contains("excluded");

      QRecord recommendation = MarketingStore.queryBy(MarketingFinding.TABLE_NAME, "agentRunId", analysis.getId()).stream()
         .filter(f -> FindingType.RECOMMENDATION.getId().equals(f.getValueInteger("findingType"))).findFirst().orElseThrow();
      assertThat(recommendation.getValueString("recommendation")).contains("quickstart finished");
   }

}
