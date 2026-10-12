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


import java.time.Instant;
import java.util.List;
import com.kingsrook.qbits.marketing.approval.ApprovalService;
import com.kingsrook.qbits.marketing.execution.ExecutionService;
import com.kingsrook.qbits.marketing.model.MarketingApproval;
import com.kingsrook.qbits.marketing.model.MarketingContent;
import com.kingsrook.qbits.marketing.model.MarketingExecution;
import com.kingsrook.qbits.marketing.model.MarketingExecutionAttempt;
import com.kingsrook.qbits.marketing.model.MarketingPolicy;
import com.kingsrook.qbits.marketing.model.MarketingTestDelivery;
import com.kingsrook.qbits.marketing.model.enums.AiContentPolicy;
import com.kingsrook.qbits.marketing.model.enums.ApprovalStatus;
import com.kingsrook.qbits.marketing.model.enums.AttemptOutcome;
import com.kingsrook.qbits.marketing.model.enums.AuthorType;
import com.kingsrook.qbits.marketing.model.enums.ChannelType;
import com.kingsrook.qbits.marketing.model.enums.ContentStatus;
import com.kingsrook.qbits.marketing.model.enums.ExecutionStatus;
import com.kingsrook.qbits.marketing.security.MarketingInternalWrite;
import com.kingsrook.qbits.marketing.transport.ControlledTestTransport;
import com.kingsrook.qbits.marketing.transport.ControlledTestTransport.Behavior;
import com.kingsrook.qbits.marketing.transport.QqqMessagingEmailTransport;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** Approval invalidation, pause / cancel, duplicate execution, ambiguous
 ** outcomes, retries, leases, kill switch, schedule, channel rules, consent
 ** and suppression at execution time, and missing transports.
 *******************************************************************************/
class ApprovalAndExecutionTest extends BaseMarketingTest
{
   private final ApprovalService  approvals  = new ApprovalService();
   private final ExecutionService executions = new ExecutionService();

   private Integer campaignId;
   private Integer channelId;



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   protected void customizeConfig(MarketingQBitConfig config)
   {
      config.withTransport(new QqqMessagingEmailTransport().withMessagingProviderName("email").withFromAddress("news@example.org")).withMaxRecipientsPerExecution(2);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void beforeEach() throws QException
   {
      campaignId = campaign(product("p1"));
      channelId = testChannel();
   }



   /*******************************************************************************
    ** Submit, approve and schedule a content item; returns the execution id.
    *******************************************************************************/
   private Integer approvedAndScheduled(Integer contentId) throws QException
   {
      approvals.submitForReview(contentId);
      approvals.approve(contentId, null);
      approvals.schedule(contentId);
      return (MarketingStore.queryBy(MarketingExecution.TABLE_NAME, "contentId", contentId).get(0).getValueInteger("id"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private ExecutionStatus status(Integer executionId) throws QException
   {
      return (ExecutionStatus.getById(reload(MarketingExecution.TABLE_NAME, executionId).getValueInteger("status")));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private ControlledTestTransport transport() throws QException
   {
      return (MarketingRuntime.current().getTestTransport());
   }



   /*******************************************************************************
    ** An edit after approval invalidates it, returns the content to draft and
    ** blocks the scheduled execution; nothing is sent.
    *******************************************************************************/
   @Test
   void testApprovalInvalidatedByEdit() throws QException
   {
      Integer contentId   = content(campaignId, channelId, "Version one.");
      Integer executionId = approvedAndScheduled(contentId);

      MarketingStore.update(MarketingContent.TABLE_NAME, new QRecord().withValue("id", contentId).withValue("body", "Version two."));

      QRecord approval = MarketingStore.queryBy(MarketingApproval.TABLE_NAME, "contentId", contentId).get(0);
      assertThat(approval.getValueInteger("status")).isEqualTo(ApprovalStatus.INVALIDATED.getId());
      assertThat(approval.getValueString("invalidatedReason")).contains("revision 2");
      assertThat(reload(MarketingContent.TABLE_NAME, contentId).getValueInteger("status")).isEqualTo(ContentStatus.DRAFT.getId());
      assertThat(status(executionId)).isEqualTo(ExecutionStatus.BLOCKED);

      executions.runDue(Instant.now());
      assertThat(MarketingStore.queryBy(MarketingTestDelivery.TABLE_NAME, "executionId", executionId)).isEmpty();
      assertThatThrownBy(() -> approvals.schedule(contentId)).isInstanceOf(QUserFacingException.class);

      //////////////////////////////////////////////////////////
      // a change to the schedule alone also voids approval   //
      //////////////////////////////////////////////////////////
      approvals.submitForReview(contentId);
      approvals.approve(contentId, null);
      MarketingStore.update(MarketingContent.TABLE_NAME, new QRecord().withValue("id", contentId).withValue("scheduledFor", Instant.now().plusSeconds(3600)));
      assertThat(ApprovalService.activeApproval(new MarketingContent(reload(MarketingContent.TABLE_NAME, contentId)))).isNull();

      ///////////////////////////////////////////////////////
      // a non-relevant change (owner) does not            //
      ///////////////////////////////////////////////////////
      approvals.submitForReview(contentId);
      approvals.approve(contentId, null);
      MarketingStore.update(MarketingContent.TABLE_NAME, new QRecord().withValue("id", contentId).withValue("ownerUserId", "someone"));
      assertThat(ApprovalService.activeApproval(new MarketingContent(reload(MarketingContent.TABLE_NAME, contentId)))).isNotNull();
   }



   /*******************************************************************************
    ** Status can't be set directly; approvals can't be written directly.
    *******************************************************************************/
   @Test
   void testNoShortcutsAroundApproval() throws QException
   {
      Integer contentId = content(campaignId, channelId, "Text.");
      assertThatThrownBy(() -> MarketingStore.update(MarketingContent.TABLE_NAME, new QRecord().withValue("id", contentId).withValue("status", ContentStatus.APPROVED.getId())))
         .hasMessageContaining("Status changes only through");
      assertThatThrownBy(() -> MarketingStore.insert(new MarketingApproval().withContentId(contentId).withContentHash("x").withStatus(ApprovalStatus.ACTIVE.getId())))
         .hasMessageContaining("written only by Marketing");

      Integer other = content(campaignId, channelId, "Other.");
      approvals.submitForReview(other);
      approvals.approve(other, null);
      Integer approvalId = MarketingStore.queryBy(MarketingApproval.TABLE_NAME, "contentId", other).get(0).getValueInteger("id");
      assertThatThrownBy(() -> MarketingStore.update(MarketingApproval.TABLE_NAME, new QRecord().withValue("id", approvalId).withValue("contentHash", "forged")))
         .hasMessageContaining("written only by Marketing");
      assertThat(MarketingInternalWrite.isActive()).isFalse();
   }



   /*******************************************************************************
    ** Paused work does not run; resumed work does; cancelled work never does.
    *******************************************************************************/
   @Test
   void testPauseResumeCancel() throws QException
   {
      Integer executionId = approvedAndScheduled(content(campaignId, channelId, "Paused text."));
      executions.pause(executionId);
      executions.runDue(Instant.now());
      assertThat(status(executionId)).isEqualTo(ExecutionStatus.PAUSED);
      assertThat(MarketingStore.queryBy(MarketingTestDelivery.TABLE_NAME, "executionId", executionId)).isEmpty();

      executions.resume(executionId);
      executions.runDue(Instant.now());
      assertThat(status(executionId)).isEqualTo(ExecutionStatus.ACCEPTED);

      Integer cancelledContent = content(campaignId, channelId, "Cancelled text.");
      Integer cancelled        = approvedAndScheduled(cancelledContent);
      executions.cancel(cancelled);
      executions.runDue(Instant.now());
      assertThat(status(cancelled)).isEqualTo(ExecutionStatus.CANCELLED);
      assertThat(MarketingStore.queryBy(MarketingTestDelivery.TABLE_NAME, "executionId", cancelled)).isEmpty();
      assertThat(MarketingStore.queryBy(MarketingApproval.TABLE_NAME, "contentId", cancelledContent).get(0).getValueInteger("status")).isEqualTo(ApprovalStatus.REVOKED.getId());
      assertThatThrownBy(() -> executions.resume(cancelled)).isInstanceOf(QUserFacingException.class);
   }



   /*******************************************************************************
    ** Scheduling twice makes one execution; running twice sends once; a repeat
    ** of the same idempotency key is not stored again.
    *******************************************************************************/
   @Test
   void testDuplicateExecution() throws QException
   {
      Integer contentId   = content(campaignId, channelId, "Once only.");
      Integer executionId = approvedAndScheduled(contentId);
      assertThatThrownBy(() -> approvals.schedule(contentId)).as("a second Schedule is refused").hasMessageContaining("is Scheduled");

      String first  = executions.runOne(executionId, Instant.now());
      String second = executions.runOne(executionId, Instant.now());
      assertThat(first).contains("accepted");
      assertThat(second).contains("not run");
      assertThat(MarketingStore.queryBy(MarketingTestDelivery.TABLE_NAME, "executionId", executionId)).hasSize(1);
      assertThat(MarketingStore.queryBy(MarketingExecution.TABLE_NAME, "contentId", contentId)).hasSize(1);
   }



   /*******************************************************************************
    ** An ambiguous outcome is never retried blindly; reconciliation finds a
    ** stored request; a person's "not sent" re-runs with the same key and the
    ** transport drops the repeat.
    *******************************************************************************/
   @Test
   void testAmbiguousOutcomes() throws QException
   {
      ////////////////////////////////////////////////////////////
      // ambiguous, but the provider actually had it            //
      ////////////////////////////////////////////////////////////
      Integer stored = approvedAndScheduled(content(campaignId, channelId, "Maybe sent."));
      transport().thenBehave(Behavior.AMBIGUOUS_BUT_STORED);
      executions.runDue(Instant.now());
      assertThat(status(stored)).isEqualTo(ExecutionStatus.NEEDS_RECONCILIATION);
      executions.runDue(Instant.now());
      assertThat(MarketingStore.queryBy(MarketingExecutionAttempt.TABLE_NAME, "executionId", stored)).as("not retried").hasSize(1);

      assertThat(executions.reconcile(stored)).contains("marked accepted");
      assertThat(status(stored)).isEqualTo(ExecutionStatus.ACCEPTED);

      ///////////////////////////////////////////////////////////////////
      // ambiguous and really not stored: reconcile finds nothing;     //
      // a person says "not sent"; it runs again with the same key     //
      ///////////////////////////////////////////////////////////////////
      Integer lost = approvedAndScheduled(content(campaignId, channelId, "Lost."));
      transport().thenBehave(Behavior.AMBIGUOUS_NOT_STORED);
      executions.runDue(Instant.now());
      assertThat(executions.reconcile(lost)).contains("no record");
      executions.resolveAsNotSent(lost);
      executions.runDue(Instant.now());
      assertThat(status(lost)).isEqualTo(ExecutionStatus.ACCEPTED);
      assertThat(MarketingStore.queryBy(MarketingTestDelivery.TABLE_NAME, "executionId", lost)).hasSize(1);

      ////////////////////////////////////////////////////////////////////
      // stored-but-ambiguous, then "not sent" by mistake: the repeat   //
      // carries the same key, so the transport reports a duplicate     //
      ////////////////////////////////////////////////////////////////////
      Integer twice = approvedAndScheduled(content(campaignId, channelId, "Twice?"));
      transport().thenBehave(Behavior.AMBIGUOUS_BUT_STORED);
      executions.runDue(Instant.now());
      executions.resolveAsNotSent(twice);
      executions.runDue(Instant.now());
      assertThat(status(twice)).isEqualTo(ExecutionStatus.ACCEPTED);
      List<QRecord> deliveries = MarketingStore.queryBy(MarketingTestDelivery.TABLE_NAME, "executionId", twice);
      assertThat(deliveries).hasSize(1);
      assertThat(deliveries.get(0).getValueInteger("duplicateCount")).isEqualTo(1);
      assertThat(MarketingStore.queryBy(MarketingExecutionAttempt.TABLE_NAME, "executionId", twice))
         .anyMatch(a -> AttemptOutcome.DUPLICATE.getId().equals(a.getValueInteger("outcome")));
   }



   /*******************************************************************************
    ** A failure before sending is retried with backoff, up to maxAttempts; a
    ** rejection is final.
    *******************************************************************************/
   @Test
   void testRetryableAndRejected() throws QException
   {
      Integer executionId = approvedAndScheduled(content(campaignId, channelId, "Retry me."));
      transport().thenBehave(Behavior.FAIL_BEFORE_SENDING, Behavior.FAIL_BEFORE_SENDING, Behavior.FAIL_BEFORE_SENDING);
      Instant now = Instant.now();
      executions.runDue(now);
      QRecord after = reload(MarketingExecution.TABLE_NAME, executionId);
      assertThat(status(executionId)).isEqualTo(ExecutionStatus.SCHEDULED);
      assertThat(after.getValueInstant("nextAttemptAt")).isAfter(now);
      executions.runDue(now);
      assertThat(MarketingStore.queryBy(MarketingExecutionAttempt.TABLE_NAME, "executionId", executionId)).as("waits for its backoff").hasSize(1);

      executions.runDue(now.plusSeconds(3600));
      executions.runDue(now.plusSeconds(7200));
      assertThat(status(executionId)).isEqualTo(ExecutionStatus.FAILED);
      assertThat(MarketingStore.queryBy(MarketingExecutionAttempt.TABLE_NAME, "executionId", executionId)).hasSize(3);

      Integer rejected = approvedAndScheduled(content(campaignId, channelId, "Reject me."));
      transport().thenBehave(Behavior.REJECT);
      executions.runDue(Instant.now());
      assertThat(status(rejected)).isEqualTo(ExecutionStatus.FAILED);
   }



   /*******************************************************************************
    ** A worker that stopped mid-send leaves an expired lease: needs
    ** reconciliation, not a retry.
    *******************************************************************************/
   @Test
   void testExpiredLease() throws QException
   {
      Integer executionId = approvedAndScheduled(content(campaignId, channelId, "Crash."));
      MarketingStore.updateInternal(MarketingExecution.TABLE_NAME, new QRecord().withValue("id", executionId).withValue("status", ExecutionStatus.RUNNING.getId())
         .withValue("leaseOwner", "dead-worker").withValue("leaseExpiresAt", Instant.now().minusSeconds(10)));
      executions.runDue(Instant.now());
      assertThat(status(executionId)).isEqualTo(ExecutionStatus.NEEDS_RECONCILIATION);
   }



   /*******************************************************************************
    ** The kill switch and the schedule are honored.
    *******************************************************************************/
   @Test
   void testKillSwitchAndSchedule() throws QException
   {
      Integer executionId = approvedAndScheduled(content(campaignId, channelId, "Not now."));
      MarketingStore.insert(new MarketingPolicy().withName(MarketingRuntime.POLICY_NAME).withExecutionEnabled(false).withKillSwitchReason("launch freeze"));
      assertThat(executions.runOne(executionId, Instant.now())).contains("switched off").contains("launch freeze");
      assertThat(status(executionId)).isEqualTo(ExecutionStatus.SCHEDULED);

      MarketingStore.update(MarketingPolicy.TABLE_NAME, new QRecord().withValue("id", MarketingStore.queryBy(MarketingPolicy.TABLE_NAME, "name", MarketingRuntime.POLICY_NAME).get(0).getValue("id"))
         .withValue("executionEnabled", true));

      Integer later = content(campaignId, channelId, "Later.");
      MarketingStore.update(MarketingContent.TABLE_NAME, new QRecord().withValue("id", later).withValue("scheduledFor", Instant.now().plusSeconds(86400)));
      Integer laterExecution = approvedAndScheduled(later);
      assertThat(executions.runOne(laterExecution, Instant.now())).contains("scheduled for");
      assertThat(status(laterExecution)).isEqualTo(ExecutionStatus.SCHEDULED);
   }



   /*******************************************************************************
    ** Consent and suppression are re-checked when the execution runs; the bulk
    ** limit holds; nobody without consent is sent anything.
    *******************************************************************************/
   @Test
   void testConsentAndSuppressionAtExecutionTime() throws QException
   {
      crm.withContact(1, "a@example.org", true, false).withContact(2, "b@example.org", false, false).withContact(3, "c@example.org", true, true)
         .withContact(4, "d@example.org", true, false).withContact(5, "e@example.org", true, false)
         .withList(10, 1, 2, 3).withList(20, 1, 4, 5);

      Integer contentId = content(campaignId, channelId, "List mail.");
      MarketingStore.update(MarketingContent.TABLE_NAME, new QRecord().withValue("id", contentId).withValue("recipientScope", "crm-list:10"));
      Integer executionId = approvedAndScheduled(contentId);

      ///////////////////////////////////////////////////////////////////
      // after approval, contact 1 withdraws consent: nobody is left   //
      ///////////////////////////////////////////////////////////////////
      crm.contacts.get(1).consent = false;
      executions.runDue(Instant.now());
      assertThat(status(executionId)).isEqualTo(ExecutionStatus.BLOCKED);
      assertThat(reload(MarketingExecution.TABLE_NAME, executionId).getValueString("statusDetail")).contains("no recipient");
      assertThat(MarketingStore.queryBy(MarketingTestDelivery.TABLE_NAME, "executionId", executionId)).isEmpty();

      ////////////////////////////////////////////////////////////////
      // consent restored: only contact 1 gets it (2 and 3 excluded) //
      ////////////////////////////////////////////////////////////////
      crm.contacts.get(1).consent = true;
      Integer again = content(campaignId, channelId, "List mail again.");
      MarketingStore.update(MarketingContent.TABLE_NAME, new QRecord().withValue("id", again).withValue("recipientScope", "crm-list:10"));
      Integer againExecution = approvedAndScheduled(again);
      executions.runDue(Instant.now());
      assertThat(status(againExecution)).isEqualTo(ExecutionStatus.ACCEPTED);
      List<QRecord> deliveries = MarketingStore.queryBy(MarketingTestDelivery.TABLE_NAME, "executionId", againExecution);
      assertThat(deliveries).extracting(r -> r.getValueString("recipientKey")).containsExactly("crm-contact:1");
      assertThat(deliveries.get(0).getValueString("body")).doesNotContain("@");

      /////////////////////////////////////////////////////////////
      // three eligible recipients exceed the limit of two       //
      /////////////////////////////////////////////////////////////
      Integer bulk = content(campaignId, channelId, "Bulk.");
      MarketingStore.update(MarketingContent.TABLE_NAME, new QRecord().withValue("id", bulk).withValue("recipientScope", "crm-list:20"));
      Integer bulkExecution = approvedAndScheduled(bulk);
      executions.runDue(Instant.now());
      assertThat(status(bulkExecution)).isEqualTo(ExecutionStatus.BLOCKED);
      assertThat(reload(MarketingExecution.TABLE_NAME, bulkExecution).getValueString("statusDetail")).contains("no bulk outreach");
   }



   /*******************************************************************************
    ** A channel that forbids AI content refuses approval of agent-written
    ** content, and blocks it at execution when the rule changed after approval.
    *******************************************************************************/
   @Test
   void testChannelAiContentRules() throws QException
   {
      Integer contentId = MarketingInternalWrite.run(() -> MarketingStore.insert(new MarketingContent().withCampaignId(campaignId).withChannelId(channelId)
         .withTitle("By the agent").withBody("Agent text.").withScheduledFor(Instant.now().minusSeconds(5)).withStatus(ContentStatus.DRAFT.getId())
         .withAuthorType(AuthorType.AGENT.getId())).getValueInteger("id"));
      Integer executionId = approvedAndScheduled(contentId);

      com.kingsrook.qbits.marketing.model.MarketingChannel channel = new com.kingsrook.qbits.marketing.model.MarketingChannel(reload(com.kingsrook.qbits.marketing.model.MarketingChannel.TABLE_NAME, channelId));
      MarketingStore.update(com.kingsrook.qbits.marketing.model.MarketingChannel.TABLE_NAME, new QRecord().withValue("id", channel.getId()).withValue("aiContentPolicy", AiContentPolicy.NOT_ALLOWED.getId()));
      executions.runDue(Instant.now());
      assertThat(status(executionId)).isEqualTo(ExecutionStatus.BLOCKED);
      assertThat(reload(MarketingExecution.TABLE_NAME, executionId).getValueString("statusDetail")).contains("does not allow AI-generated content");

      Integer second = MarketingInternalWrite.run(() -> MarketingStore.insert(new MarketingContent().withCampaignId(campaignId).withChannelId(channelId)
         .withTitle("By the agent 2").withBody("Agent text 2.").withScheduledFor(Instant.now().minusSeconds(5)).withStatus(ContentStatus.DRAFT.getId())
         .withAuthorType(AuthorType.AGENT.getId())).getValueInteger("id"));
      approvals.submitForReview(second);
      assertThatThrownBy(() -> approvals.approve(second, null)).hasMessageContaining("does not allow AI-generated content");
   }



   /*******************************************************************************
    ** A channel whose transport is declared but not configured is not a
    ** working connector: the execution is blocked, nothing is sent.
    *******************************************************************************/
   @Test
   void testUnconfiguredTransportIsNotAConnector() throws QException
   {
      crm.withContact(1, "a@example.org", true, false);
      Integer emailChannel = channel("Email", ChannelType.EMAIL, QqqMessagingEmailTransport.DEFAULT_NAME, AiContentPolicy.ALLOWED, true);
      Integer contentId    = content(campaignId, emailChannel, "Email text.");
      MarketingStore.update(MarketingContent.TABLE_NAME, new QRecord().withValue("id", contentId).withValue("recipientScope", "crm-contact:1"));
      approvals.submitForReview(contentId);
      approvals.approve(contentId, null);
      assertThat(approvals.schedule(contentId)).contains("not usable yet");

      Integer executionId = MarketingStore.queryBy(MarketingExecution.TABLE_NAME, "contentId", contentId).get(0).getValueInteger("id");
      executions.runDue(Instant.now());
      assertThat(status(executionId)).isEqualTo(ExecutionStatus.BLOCKED);
      assertThat(reload(MarketingExecution.TABLE_NAME, executionId).getValueString("statusDetail")).contains("not usable");

      Integer ghost = channel("Ghost", ChannelType.SOCIAL, "no_such_transport", AiContentPolicy.ALLOWED, true);
      Integer ghostContent = content(campaignId, ghost, "Ghost text.");
      approvals.submitForReview(ghostContent);
      approvals.approve(ghostContent, null);
      assertThatThrownBy(() -> approvals.schedule(ghostContent)).hasMessageContaining("not a working connector");
   }

}
