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
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qbits.marketing.agent.MarketingToolRunner;
import com.kingsrook.qbits.marketing.agent.tools.MarketingToolContext;
import com.kingsrook.qbits.marketing.agent.tools.MarketingTools;
import com.kingsrook.qbits.marketing.approval.ApprovalService;
import com.kingsrook.qbits.marketing.execution.ExecutionService;
import com.kingsrook.qbits.marketing.model.MarketingApproval;
import com.kingsrook.qbits.marketing.model.MarketingContent;
import com.kingsrook.qbits.marketing.model.MarketingExecution;
import com.kingsrook.qbits.marketing.model.MarketingFinding;
import com.kingsrook.qbits.marketing.model.MarketingPolicy;
import com.kingsrook.qbits.marketing.model.enums.ApprovalStatus;
import com.kingsrook.qbits.marketing.model.enums.ContentStatus;
import com.kingsrook.qbits.marketing.model.enums.ExecutionStatus;
import com.kingsrook.qbits.marketing.model.enums.FindingSeverity;
import com.kingsrook.qbits.marketing.model.enums.FindingStatus;
import com.kingsrook.qbits.marketing.model.enums.FindingType;
import com.kingsrook.qbits.marketing.processes.MarketingAction;
import com.kingsrook.qbits.marketing.processes.MarketingActions;
import com.kingsrook.qbits.marketing.security.MarketingPermissions;
import com.kingsrook.qqq.backend.core.actions.ai.AiToolOutcome;
import com.kingsrook.qqq.backend.core.actions.tables.DeleteAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.StreamedETLWithFrontendProcess;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** Regression tests for the code-review findings: the String-id edit path,
 ** worker isolation, recipient-scope validation, the fail-closed policy,
 ** blocker findings, agent write permission and draft-only revision, and
 ** approval pinned to the reviewed version.
 *******************************************************************************/
class ReviewFixesTest extends BaseMarketingTest
{
   private final ApprovalService  approvals  = new ApprovalService();
   private final ExecutionService executions = new ExecutionService();

   private Integer campaignId;
   private Integer channelId;



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void beforeEach() throws QException
   {
      campaignId = campaign(product("r1"));
      channelId = testChannel();
   }



   /*******************************************************************************
    ** The UI / API update path passes the id as a String: the edit must still
    ** invalidate approval, and status still can't be set.
    *******************************************************************************/
   @Test
   void testStringIdEditInvalidates() throws QException
   {
      Integer contentId = content(campaignId, channelId, "Approved text.");
      approvals.submitForReview(contentId);
      approvals.approve(contentId, null);
      approvals.schedule(contentId);

      QRecord edit = new QRecord().withValue("id", String.valueOf(contentId)).withValue("body", "Edited after approval.");
      new UpdateAction().execute(new UpdateInput(MarketingContent.TABLE_NAME).withRecord(edit));

      assertThat(MarketingStore.queryBy(MarketingApproval.TABLE_NAME, "contentId", contentId).get(0).getValueInteger("status")).isEqualTo(ApprovalStatus.INVALIDATED.getId());
      assertThat(reload(MarketingContent.TABLE_NAME, contentId).getValueInteger("revisionNumber")).isEqualTo(2);
      executions.runDue(Instant.now());
      assertThat(MarketingStore.queryBy(MarketingExecution.TABLE_NAME, "contentId", contentId).get(0).getValueInteger("status")).isEqualTo(ExecutionStatus.BLOCKED.getId());

      QRecord forged = new QRecord().withValue("id", String.valueOf(contentId)).withValue("status", String.valueOf(ContentStatus.APPROVED.getId()));
      QRecord result = new UpdateAction().execute(new UpdateInput(MarketingContent.TABLE_NAME).withRecord(forged)).getRecords().get(0);
      assertThat(result.getErrors()).isNotEmpty();
   }



   /*******************************************************************************
    ** One failing execution does not stop the others.
    *******************************************************************************/
   @Test
   void testWorkerIsolation() throws QException
   {
      Integer bad = content(campaignId, channelId, "To list 99.");
      MarketingStore.update(MarketingContent.TABLE_NAME, new QRecord().withValue("id", bad).withValue("recipientScope", "crm-list:99")
         .withValue("scheduledFor", Instant.now().minusSeconds(600)));
      approvals.submitForReview(bad);
      approvals.approve(bad, null);
      approvals.schedule(bad);

      Integer good = content(campaignId, channelId, "Public post.");
      approvals.submitForReview(good);
      approvals.approve(good, null);
      approvals.schedule(good);

      List<String> lines = executions.runDue(Instant.now());
      assertThat(lines).anyMatch(l -> l.contains("blocked")).anyMatch(l -> l.contains("accepted"));
      assertThat(MarketingStore.queryBy(MarketingExecution.TABLE_NAME, "contentId", bad).get(0).getValueInteger("status")).isEqualTo(ExecutionStatus.BLOCKED.getId());
      assertThat(MarketingStore.queryBy(MarketingExecution.TABLE_NAME, "contentId", good).get(0).getValueInteger("status")).isEqualTo(ExecutionStatus.ACCEPTED.getId());
   }



   /*******************************************************************************
    ** Malformed recipient scopes can't be submitted.
    *******************************************************************************/
   @Test
   void testRecipientScopeValidated() throws QException
   {
      Integer contentId = content(campaignId, channelId, "Text.");
      MarketingStore.update(MarketingContent.TABLE_NAME, new QRecord().withValue("id", contentId).withValue("recipientScope", "crm-list:abc"));
      assertThatThrownBy(() -> approvals.submitForReview(contentId)).hasMessageContaining("must be empty or public");
   }



   /*******************************************************************************
    ** No policy row: nothing is sent and the agent is off (fail closed).
    *******************************************************************************/
   @Test
   void testMissingPolicyFailsClosed() throws QException
   {
      Integer contentId = content(campaignId, channelId, "Text.");
      approvals.submitForReview(contentId);
      approvals.approve(contentId, null);
      approvals.schedule(contentId);
      new DeleteAction().execute(new DeleteInput(MarketingPolicy.TABLE_NAME).withPrimaryKeys(List.of(policyId())));

      Integer executionId = MarketingStore.queryBy(MarketingExecution.TABLE_NAME, "contentId", contentId).get(0).getValueInteger("id");
      assertThat(executions.runOne(executionId, Instant.now())).contains("no Marketing Policy row");
      assertThat(MarketingRuntime.current().describeIntegrations()).anyMatch(s -> s.kind().equals("Policy") && s.status().startsWith("Missing"));
   }



   /*******************************************************************************
    ** A preparer can't close or downgrade an open blocker; an approver can.
    *******************************************************************************/
   @Test
   void testBlockerFindingsNeedApprover() throws QException
   {
      Integer findingId = MarketingStore.insert(new MarketingFinding().withCampaignId(campaignId).withFindingType(FindingType.UNSUPPORTED_CLAIM.getId())
         .withSeverity(FindingSeverity.BLOCKER.getId()).withStatus(FindingStatus.OPEN.getId()).withTitle("Unsupported")).getValueInteger("id");

      as(user("preparer", MarketingPermissions.READ, MarketingPermissions.WRITE, MarketingPermissions.PREPARE));
      assertThatThrownBy(() -> MarketingStore.update(MarketingFinding.TABLE_NAME, new QRecord().withValue("id", findingId).withValue("status", FindingStatus.DISMISSED.getId())))
         .hasMessageContaining(MarketingPermissions.APPROVE);
      assertThatThrownBy(() -> MarketingStore.update(MarketingFinding.TABLE_NAME, new QRecord().withValue("id", String.valueOf(findingId)).withValue("severity", FindingSeverity.INFO.getId())))
         .hasMessageContaining(MarketingPermissions.APPROVE);

      as(user("approver", MarketingPermissions.READ, MarketingPermissions.WRITE, MarketingPermissions.APPROVE));
      MarketingStore.update(MarketingFinding.TABLE_NAME, new QRecord().withValue("id", findingId).withValue("status", FindingStatus.DISMISSED.getId()));
      assertThat(reload(MarketingFinding.TABLE_NAME, findingId).getValueInteger("status")).isEqualTo(FindingStatus.DISMISSED.getId());
   }



   /*******************************************************************************
    ** The agent's write tools need marketing.write and revise only drafts.
    *******************************************************************************/
   @Test
   void testAgentWritesNeedWriteAndDrafts() throws QException
   {
      Integer contentId = content(campaignId, channelId, "Human text.");
      approvals.submitForReview(contentId);

      MarketingToolRunner runner = new MarketingToolRunner(MarketingTools.all());
      Map<String, Object> revise = Map.of("campaign_id", campaignId, "channel_id", channelId, "title", "T", "body", "Agent rewrite", "claim_ids", List.of(), "content_id", contentId);
      MarketingToolRunner.Result inReview = runner.execute(MarketingTools.SAVE_DRAFT, revise, new MarketingToolContext(null, 50));
      assertThat(inReview.outcome()).isEqualTo(AiToolOutcome.INVALID_INPUT);
      assertThat(inReview.summary()).contains("not a draft");
      assertThat(reload(MarketingContent.TABLE_NAME, contentId).getValueString("body")).isEqualTo("Human text.");

      as(user("agent-only", MarketingPermissions.READ, MarketingPermissions.AGENT, MarketingPermissions.ADMIN_READ));
      MarketingToolRunner.Result denied = runner.execute(MarketingTools.SAVE_DRAFT,
         Map.of("campaign_id", campaignId, "channel_id", channelId, "title", "T", "body", "New", "claim_ids", List.of()), new MarketingToolContext(null, 50));
      assertThat(denied.outcome()).isEqualTo(AiToolOutcome.DENIED);
   }



   /*******************************************************************************
    ** Approve skips content that changed after the approver's Review screen.
    *******************************************************************************/
   @Test
   void testApprovalPinnedToReviewedVersion() throws QException
   {
      Integer contentId = content(campaignId, channelId, "Version A.");
      approvals.submitForReview(contentId);
      QRecord reviewed = reload(MarketingContent.TABLE_NAME, contentId);

      MarketingAction.Transform review = new MarketingAction.Transform();
      RunBackendStepInput  input  = new RunBackendStepInput().withProcessName(MarketingActions.APPROVE).withRecords(List.of(reviewed));
      input.setStepName(StreamedETLWithFrontendProcess.STEP_NAME_VALIDATE);
      RunBackendStepOutput output = new RunBackendStepOutput();
      review.runOnePage(input, output);
      Map<String, Serializable> state = new HashMap<>(output.getValues());

      //////////////////////////////////////////////////////////////////
      // the author edits and resubmits while the approver is looking //
      //////////////////////////////////////////////////////////////////
      MarketingStore.update(MarketingContent.TABLE_NAME, new QRecord().withValue("id", contentId).withValue("body", "Version B."));
      approvals.submitForReview(contentId);

      MarketingAction.Transform execute = new MarketingAction.Transform();
      RunBackendStepInput  input2  = new RunBackendStepInput().withProcessName(MarketingActions.APPROVE).withRecords(List.of(reload(MarketingContent.TABLE_NAME, contentId))).withValues(state);
      input2.setStepName(StreamedETLWithFrontendProcess.STEP_NAME_EXECUTE);
      RunBackendStepOutput output2 = new RunBackendStepOutput();
      execute.runOnePage(input2, output2);
      assertThat(output2.getRecords()).isEmpty();
      assertThat(String.valueOf(execute.getProcessSummary(output2, true))).contains("changed after you reviewed it");
   }

}
