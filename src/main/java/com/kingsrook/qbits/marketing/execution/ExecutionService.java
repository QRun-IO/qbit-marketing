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

package com.kingsrook.qbits.marketing.execution;


import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import com.kingsrook.qbits.marketing.MarketingQBitConfig;
import com.kingsrook.qbits.marketing.MarketingRuntime;
import com.kingsrook.qbits.marketing.MarketingStore;
import com.kingsrook.qbits.marketing.approval.ApprovalService;
import com.kingsrook.qbits.marketing.crm.MarketingCrmGateway;
import com.kingsrook.qbits.marketing.model.MarketingApproval;
import com.kingsrook.qbits.marketing.model.MarketingChannel;
import com.kingsrook.qbits.marketing.model.MarketingContent;
import com.kingsrook.qbits.marketing.model.MarketingContentRevision;
import com.kingsrook.qbits.marketing.model.MarketingExecution;
import com.kingsrook.qbits.marketing.model.MarketingExecutionAttempt;
import com.kingsrook.qbits.marketing.model.MarketingOutcomeEvent;
import com.kingsrook.qbits.marketing.model.MarketingPolicy;
import com.kingsrook.qbits.marketing.model.enums.ApprovalStatus;
import com.kingsrook.qbits.marketing.model.enums.AttemptOutcome;
import com.kingsrook.qbits.marketing.model.enums.ChannelType;
import com.kingsrook.qbits.marketing.model.enums.ContentStatus;
import com.kingsrook.qbits.marketing.model.enums.ExecutionStatus;
import com.kingsrook.qbits.marketing.model.enums.FunnelStage;
import com.kingsrook.qbits.marketing.security.MarketingInternalWrite;
import com.kingsrook.qbits.marketing.security.MarketingPermissions;
import com.kingsrook.qbits.marketing.transport.ControlledTestTransport;
import com.kingsrook.qbits.marketing.transport.MarketingTransport;
import com.kingsrook.qbits.marketing.transport.MarketingTransport.Recipient;
import com.kingsrook.qbits.marketing.transport.MarketingTransport.TransportRequest;
import com.kingsrook.qbits.marketing.transport.MarketingTransport.TransportResult;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Runs approved content through its transport, durably and carefully.
 **
 ** - An execution is a row (marketing_execution) with an idempotency key; each
 **   try is a marketing_execution_attempt with the provider's receipt or error.
 ** - Before every send the worker re-checks everything that could have changed
 **   since approval: the kill switch, the execution's own status (paused /
 **   cancelled), the approval (still active, same content hash), the
 **   schedule, the channel and its transport, the channel's AI-content rules,
 **   the channel's hourly rate, and - for CRM recipients - consent and
 **   suppression of every recipient, plus the bulk limit.
 ** - Outcomes: ACCEPTED / DUPLICATE -> accepted by provider (approval
 **   consumed); REJECTED -> failed; RETRYABLE_FAILURE (certainly not sent) ->
 **   rescheduled with backoff until maxAttempts; AMBIGUOUS -> needs
 **   reconciliation, never retried blindly. A worker that dies mid-send
 **   leaves a RUNNING row whose lease expires; that also becomes "needs
 **   reconciliation".
 ** - Single worker per JVM (a lock); see docs/OPERATIONS.md for the
 **   multi-replica gap.
 *******************************************************************************/
public class ExecutionService
{
   private static final QLogger LOG = QLogger.getLogger(ExecutionService.class);

   public static final String PROVIDER_ACCEPTED_EVENT = "provider_accepted";



   /*******************************************************************************
    ** Run every due execution (up to the batch size). Returns one line per
    ** execution touched.
    *******************************************************************************/
   private List<String> runDueImpl(Instant now) throws QException
   {
      MarketingRuntime runtime = MarketingRuntime.current();
      List<String>     lines   = new ArrayList<>();
      if(!runtime.getWorkerLock().tryLock())
      {
         lines.add("Another run of the execution worker is in progress; nothing done.");
         return (lines);
      }
      try
      {
         lines.addAll(recoverStaleLeases(now));
         for(QRecord record : dueExecutions(now, runtime.getConfig().getWorkerBatchSize()))
         {
            lines.add("Execution #" + record.getValue("id") + ": " + executeLocked(record.getValueInteger("id"), now));
         }
      }
      finally
      {
         runtime.getWorkerLock().unlock();
      }
      return (lines);
   }



   /*******************************************************************************
    ** Run one execution now, if it is scheduled and due.
    *******************************************************************************/
   private String runOneImpl(Integer executionId, Instant now) throws QException
   {
      requireExecutePermission();
      MarketingRuntime runtime = MarketingRuntime.current();
      if(!runtime.getWorkerLock().tryLock())
      {
         throw (new QUserFacingException("The execution worker is busy; try again in a moment."));
      }
      try
      {
         return (executeLocked(executionId, now));
      }
      finally
      {
         runtime.getWorkerLock().unlock();
      }
   }



   /*******************************************************************************
    ** The due executions, oldest schedule first.
    *******************************************************************************/
   public static List<QRecord> dueExecutions(Instant now, int limit) throws QException
   {
      List<QRecord> due = new ArrayList<>();
      for(QRecord record : MarketingStore.query(MarketingExecution.TABLE_NAME, new QQueryFilter(
         new QFilterCriteria("status", QCriteriaOperator.EQUALS, ExecutionStatus.SCHEDULED.getId()),
         new QFilterCriteria("scheduledFor", QCriteriaOperator.LESS_THAN_OR_EQUALS, now)).withOrderBy(new QFilterOrderBy("scheduledFor")).withOrderBy(new QFilterOrderBy("id"))))
      {
         Instant next = record.getValueInstant("nextAttemptAt");
         if(next == null || !next.isAfter(now))
         {
            due.add(record);
         }
         if(due.size() >= limit)
         {
            break;
         }
      }
      return (due);
   }



   /*******************************************************************************
    ** Claim and run one execution (caller holds the worker lock).
    *******************************************************************************/
   private String executeLocked(Integer executionId, Instant now) throws QException
   {
      MarketingRuntime    runtime = MarketingRuntime.current();
      MarketingQBitConfig config  = runtime.getConfig();

      ////////////////////////////////////////////////////////////////////
      // re-read: a duplicate run (two clicks, scheduler + Run Now) sees //
      // the first one's status and does nothing                         //
      ////////////////////////////////////////////////////////////////////
      MarketingExecution execution = new MarketingExecution(MarketingStore.require(MarketingExecution.TABLE_NAME, executionId));
      if(!ExecutionStatus.SCHEDULED.getId().equals(execution.getStatus()))
      {
         return ("not run: it is " + ExecutionStatus.getById(execution.getStatus()).getLabel() + ", not Scheduled.");
      }
      if(execution.getScheduledFor() != null && execution.getScheduledFor().isAfter(now))
      {
         return ("not run: it is scheduled for " + execution.getScheduledFor() + ".");
      }
      if(execution.getNextAttemptAt() != null && execution.getNextAttemptAt().isAfter(now))
      {
         return ("not run: the next attempt is at " + execution.getNextAttemptAt() + ".");
      }

      ////////////////////
      // kill switch    //
      ////////////////////
      MarketingPolicy policy = runtime.readPolicy();
      if(policy != null && Boolean.FALSE.equals(policy.getExecutionEnabled()))
      {
         return ("not run: execution is switched off in Marketing Policy" + (StringUtils.hasContent(policy.getKillSwitchReason()) ? " (" + policy.getKillSwitchReason() + ")" : "") + ".");
      }

      ////////////////////////////////////////////////////////////////////////
      // execution-time checks: anything that changed since approval blocks //
      ////////////////////////////////////////////////////////////////////////
      Checked checked = check(execution, now);
      if(checked.postponeUntil() != null)
      {
         updateExecution(new QRecord().withValue("id", executionId).withValue("nextAttemptAt", checked.postponeUntil()).withValue("statusDetail", checked.reason()));
         return ("postponed: " + checked.reason());
      }
      if(checked.blockReason() != null)
      {
         recordAttempt(execution, now, AttemptOutcome.BLOCKED, null, null, checked.blockReason(), 0, 0);
         updateExecution(new QRecord().withValue("id", executionId).withValue("status", ExecutionStatus.BLOCKED.getId()).withValue("statusDetail", checked.blockReason()));
         return ("blocked: " + checked.blockReason());
      }

      ///////////////////////////////////////
      // claim (lease), then the one send  //
      ///////////////////////////////////////
      int attemptNumber = (execution.getAttemptCount() == null ? 0 : execution.getAttemptCount()) + 1;
      updateExecution(new QRecord().withValue("id", executionId)
         .withValue("status", ExecutionStatus.RUNNING.getId())
         .withValue("attemptCount", attemptNumber)
         .withValue("lastAttemptAt", now)
         .withValue("leaseOwner", runtime.getWorkerName())
         .withValue("leaseExpiresAt", now.plusSeconds(config.getLeaseSeconds())));

      long            start  = System.currentTimeMillis();
      TransportResult result = checked.transport().send(checked.request());
      long            millis = System.currentTimeMillis() - start;
      if(result == null)
      {
         result = new TransportResult(TransportResult.Outcome.AMBIGUOUS, null, "no_result", "The transport returned no result.");
      }

      int recipients = checked.request().recipients().size();
      switch(result.outcome())
      {
         case ACCEPTED, DUPLICATE ->
         {
            recordAttempt(execution, now, result.outcome() == TransportResult.Outcome.DUPLICATE ? AttemptOutcome.DUPLICATE : AttemptOutcome.ACCEPTED,
               result.receiptId(), result.providerStatus(), result.message() + checked.note(), recipients, millis);
            markAccepted(execution, result.receiptId(), recipients, result.message() + checked.note());
            return ("accepted by provider (" + result.receiptId() + ")" + (result.outcome() == TransportResult.Outcome.DUPLICATE ? ", duplicate request not sent again" : "") + ".");
         }
         case REJECTED ->
         {
            recordAttempt(execution, now, AttemptOutcome.REJECTED, result.receiptId(), result.providerStatus(), result.message(), recipients, millis);
            updateExecution(new QRecord().withValue("id", executionId).withValue("status", ExecutionStatus.FAILED.getId())
               .withValue("statusDetail", result.message()).withValue("completedAt", Instant.now()).withValue("leaseOwner", null).withValue("leaseExpiresAt", null));
            setContentStatus(execution.getContentId(), ContentStatus.FAILED);
            return ("rejected by provider: " + result.message());
         }
         case RETRYABLE_FAILURE ->
         {
            recordAttempt(execution, now, AttemptOutcome.RETRYABLE_FAILURE, null, result.providerStatus(), result.message(), recipients, millis);
            int maxAttempts = execution.getMaxAttempts() == null ? config.getDefaultMaxAttempts() : execution.getMaxAttempts();
            if(attemptNumber >= maxAttempts)
            {
               updateExecution(new QRecord().withValue("id", executionId).withValue("status", ExecutionStatus.FAILED.getId())
                  .withValue("statusDetail", "Gave up after " + attemptNumber + " attempt(s): " + result.message()).withValue("completedAt", Instant.now())
                  .withValue("leaseOwner", null).withValue("leaseExpiresAt", null));
               setContentStatus(execution.getContentId(), ContentStatus.FAILED);
               return ("failed before sending; no attempts left.");
            }
            Instant next = now.plus(Duration.ofSeconds((long) config.getRetryBackoffSeconds() * (1L << (attemptNumber - 1))));
            updateExecution(new QRecord().withValue("id", executionId).withValue("status", ExecutionStatus.SCHEDULED.getId())
               .withValue("nextAttemptAt", next).withValue("statusDetail", "Retry " + (attemptNumber + 1) + " of " + maxAttempts + " at " + next + ": " + result.message())
               .withValue("leaseOwner", null).withValue("leaseExpiresAt", null));
            return ("failed before sending; retry at " + next + ".");
         }
         default ->
         {
            recordAttempt(execution, now, AttemptOutcome.AMBIGUOUS, result.receiptId(), result.providerStatus(), result.message(), recipients, millis);
            updateExecution(new QRecord().withValue("id", executionId).withValue("status", ExecutionStatus.NEEDS_RECONCILIATION.getId())
               .withValue("statusDetail", "Outcome unknown, not retried: " + result.message()).withValue("leaseOwner", null).withValue("leaseExpiresAt", null));
            setContentStatus(execution.getContentId(), ContentStatus.NEEDS_RECONCILIATION);
            return ("ambiguous outcome; needs reconciliation (not retried).");
         }
      }
   }



   /*******************************************************************************
    ** The result of the execution-time checks.
    *******************************************************************************/
   private record Checked(String blockReason, Instant postponeUntil, String reason, MarketingTransport transport, TransportRequest request, String note)
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      static Checked blocked(String reason)
      {
         return (new Checked(reason, null, reason, null, null, ""));
      }
   }



   /*******************************************************************************
    ** Re-check everything the send depends on.
    *******************************************************************************/
   private Checked check(MarketingExecution execution, Instant now) throws QException
   {
      MarketingRuntime runtime = MarketingRuntime.current();

      QRecord contentRecord = MarketingStore.get(MarketingContent.TABLE_NAME, execution.getContentId());
      if(contentRecord == null)
      {
         return (Checked.blocked("the content no longer exists."));
      }
      MarketingContent content = new MarketingContent(contentRecord);

      QRecord approvalRecord = MarketingStore.get(MarketingApproval.TABLE_NAME, execution.getApprovalId());
      if(approvalRecord == null)
      {
         return (Checked.blocked("the approval no longer exists."));
      }
      MarketingApproval approval = new MarketingApproval(approvalRecord);
      if(!ApprovalStatus.ACTIVE.getId().equals(approval.getStatus()))
      {
         return (Checked.blocked("the approval is " + ApprovalStatus.getById(approval.getStatus()).getLabel()
            + (StringUtils.hasContent(approval.getInvalidatedReason()) ? " (" + approval.getInvalidatedReason() + ")" : "") + "."));
      }
      if(!Objects.equals(approval.getContentHash(), content.getContentHash()))
      {
         return (Checked.blocked("the content changed after approval (hash mismatch)."));
      }
      if(approval.getScheduledFor() != null && approval.getScheduledFor().isAfter(now))
      {
         return (Checked.blocked("the approved schedule (" + approval.getScheduledFor() + ") has not been reached."));
      }

      QRecord channelRecord = MarketingStore.get(MarketingChannel.TABLE_NAME, content.getChannelId());
      if(channelRecord == null)
      {
         return (Checked.blocked("the channel no longer exists."));
      }
      MarketingChannel channel = new MarketingChannel(channelRecord);
      if(!Boolean.TRUE.equals(channel.getIsActive()))
      {
         return (Checked.blocked("channel " + channel.getName() + " is not active."));
      }

      MarketingTransport transport = runtime.getTransport(execution.getTransportName());
      if(transport == null)
      {
         return (Checked.blocked("transport [" + execution.getTransportName() + "] is not registered: it is not a working connector."));
      }
      if(!transport.getStatus().isUsable())
      {
         return (Checked.blocked("transport " + transport.getName() + " is not usable: " + transport.getStatus().detail()));
      }
      ChannelType channelType = ChannelType.getById(channel.getChannelType());
      if(!transport.getSupportedChannelTypes().contains(channelType))
      {
         return (Checked.blocked("transport " + transport.getName() + " does not serve " + channelType.getLabel() + " channels."));
      }
      boolean isTest = transport instanceof ControlledTestTransport;
      if(!isTest && !Boolean.TRUE.equals(channel.getAllowsAutomation()))
      {
         return (Checked.blocked("channel " + channel.getName() + " does not allow automated publishing; post it by hand."));
      }

      try
      {
         MarketingContentRevision revision = ApprovalService.currentRevision(content);
         ApprovalService.checkAiContentPolicy(channel, revision, content);
      }
      catch(QUserFacingException e)
      {
         return (Checked.blocked(e.getMessage()));
      }

      ////////////////////////////////
      // the channel's hourly rate  //
      ////////////////////////////////
      if(channel.getRateLimitPerHour() != null && channel.getRateLimitPerHour() > 0)
      {
         int forChannel = countChannelAttemptsLastHour(channel.getId(), now);
         if(forChannel >= channel.getRateLimitPerHour())
         {
            Instant later = now.plus(Duration.ofMinutes(15));
            return (new Checked(null, later, "channel " + channel.getName() + " reached its limit of " + channel.getRateLimitPerHour() + " sends per hour; postponed to " + later + ".", null, null, ""));
         }
      }

      //////////////////////////////////////////////////////////
      // recipients: consent and suppression re-checked now   //
      //////////////////////////////////////////////////////////
      List<Recipient> recipients = new ArrayList<>();
      String          note       = "";
      String          scope      = content.getRecipientScope();
      if(scope != null && scope.startsWith("crm-"))
      {
         MarketingCrmGateway gateway = runtime.getAvailableCrmGateway();
         if(gateway == null)
         {
            return (Checked.blocked("the recipient scope needs the CRM, and no CRM gateway is available."));
         }
         int excluded = 0;
         for(MarketingCrmGateway.CrmRecipient recipient : gateway.resolveRecipients(scope))
         {
            if(recipient.eligibility().eligible() && StringUtils.hasContent(recipient.address()))
            {
               recipients.add(new Recipient(recipient.key(), isTest ? null : recipient.address()));
            }
            else
            {
               excluded++;
            }
         }
         if(recipients.isEmpty())
         {
            return (Checked.blocked("no recipient in " + scope + " has email consent and is not suppressed (" + excluded + " excluded)."));
         }
         int max = channel.getMaxRecipientsPerExecution() != null ? channel.getMaxRecipientsPerExecution() : runtime.getConfig().getMaxRecipientsPerExecution();
         if(recipients.size() > max)
         {
            return (Checked.blocked(recipients.size() + " recipients exceed the limit of " + max + " per execution (no bulk outreach)."));
         }
         if(excluded > 0)
         {
            note = " " + excluded + " recipient(s) excluded at send time (no consent or suppressed).";
         }
      }
      else
      {
         recipients.add(new Recipient("public", null));
      }

      TransportRequest request = new TransportRequest(execution.getIdempotencyKey(), execution.getId(), content.getProductId(), channel.getName(), channelType,
         content.getTitle(), content.getBody(), content.getDestinationUrl(), content.getCallToAction(), recipients);
      return (new Checked(null, null, null, transport, request, note));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static int countChannelAttemptsLastHour(Integer channelId, Instant now) throws QException
   {
      int count = 0;
      for(QRecord execution : MarketingStore.query(MarketingExecution.TABLE_NAME, new QQueryFilter(
         new QFilterCriteria("channelId", QCriteriaOperator.EQUALS, channelId),
         new QFilterCriteria("lastAttemptAt", QCriteriaOperator.GREATER_THAN, now.minus(Duration.ofHours(1))))))
      {
         Integer status = execution.getValueInteger("status");
         if(ExecutionStatus.ACCEPTED.getId().equals(status) || ExecutionStatus.NEEDS_RECONCILIATION.getId().equals(status) || ExecutionStatus.RUNNING.getId().equals(status))
         {
            count++;
         }
      }
      return (count);
   }



   /*******************************************************************************
    ** Accepted by the provider: consume the approval, mark the content, and
    ** record the (delivery-stage) outcome event - which is NOT delivery,
    ** reading or engagement.
    *******************************************************************************/
   private void markAccepted(MarketingExecution execution, String receiptId, int recipients, String detail) throws QException
   {
      MarketingInternalWrite.run(() ->
      {
         MarketingStore.update(MarketingExecution.TABLE_NAME, new QRecord().withValue("id", execution.getId())
            .withValue("status", ExecutionStatus.ACCEPTED.getId()).withValue("providerReceiptId", receiptId)
            .withValue("statusDetail", "Accepted by the provider (not proof of delivery or engagement)." + (StringUtils.hasContent(detail) ? " " + detail : ""))
            .withValue("completedAt", Instant.now()).withValue("leaseOwner", null).withValue("leaseExpiresAt", null).withValue("nextAttemptAt", null));
         MarketingStore.update(MarketingApproval.TABLE_NAME, new QRecord().withValue("id", execution.getApprovalId()).withValue("status", ApprovalStatus.CONSUMED.getId()));
         MarketingStore.update(MarketingContent.TABLE_NAME, new QRecord().withValue("id", execution.getContentId()).withValue("status", ContentStatus.ACCEPTED.getId()));

         String externalId = "execution:" + execution.getId();
         if(MarketingStore.count(MarketingOutcomeEvent.TABLE_NAME, new QQueryFilter(
            new QFilterCriteria("source", QCriteriaOperator.EQUALS, "marketing"),
            new QFilterCriteria("externalEventId", QCriteriaOperator.EQUALS, externalId))) == 0)
         {
            MarketingStore.insert(new MarketingOutcomeEvent()
               .withProductId(execution.getProductId())
               .withCampaignId(execution.getCampaignId())
               .withContentId(execution.getContentId())
               .withExecutionId(execution.getId())
               .withStage(FunnelStage.DELIVERY.getId())
               .withEventType(PROVIDER_ACCEPTED_EVENT)
               .withSource("marketing")
               .withExternalEventId(externalId)
               .withOccurredAt(Instant.now())
               .withQuantity(recipients)
               .withIsTestData(execution.getIsTestData())
               .withNote("Provider accepted the request for " + recipients + " recipient(s); this is not delivery, reading or engagement."));
         }
         return (null);
      });
   }



   /*******************************************************************************
    ** SCHEDULED -> PAUSED.
    *******************************************************************************/
   private String pauseImpl(Integer executionId) throws QException
   {
      requireExecutePermission();
      MarketingExecution execution = load(executionId);
      requireStatus(execution, ExecutionStatus.SCHEDULED);
      updateExecution(new QRecord().withValue("id", executionId).withValue("status", ExecutionStatus.PAUSED.getId()).withValue("statusDetail", "Paused by " + MarketingStore.currentUserId()));
      setContentStatus(execution.getContentId(), ContentStatus.PAUSED);
      return ("Paused.");
   }



   /*******************************************************************************
    ** PAUSED -> SCHEDULED, if its approval still covers the content.
    *******************************************************************************/
   private String resumeImpl(Integer executionId) throws QException
   {
      requireExecutePermission();
      MarketingExecution execution = load(executionId);
      requireStatus(execution, ExecutionStatus.PAUSED);
      MarketingContent  content  = new MarketingContent(MarketingStore.require(MarketingContent.TABLE_NAME, execution.getContentId()));
      MarketingApproval approval = ApprovalService.activeApproval(content);
      if(approval == null || !approval.getId().equals(execution.getApprovalId()))
      {
         throw (new QUserFacingException("Its approval no longer covers the current content; approve and schedule again."));
      }
      updateExecution(new QRecord().withValue("id", executionId).withValue("status", ExecutionStatus.SCHEDULED.getId()).withValue("statusDetail", "Resumed by " + MarketingStore.currentUserId()));
      setContentStatus(execution.getContentId(), ContentStatus.SCHEDULED);
      return ("Resumed; it runs at " + execution.getScheduledFor() + " or the next worker run after that.");
   }



   /*******************************************************************************
    ** Cancel an execution that has not completed; its approval is revoked.
    *******************************************************************************/
   private String cancelImpl(Integer executionId) throws QException
   {
      requireExecutePermission();
      MarketingExecution execution = load(executionId);
      ExecutionStatus    status    = ExecutionStatus.getById(execution.getStatus());
      if(status == ExecutionStatus.ACCEPTED || status == ExecutionStatus.CANCELLED || status == ExecutionStatus.RUNNING)
      {
         throw (new QUserFacingException("An execution that is " + status.getLabel() + " can't be cancelled."));
      }

      String who = "Cancelled by " + MarketingStore.currentUserId();
      MarketingInternalWrite.run(() ->
      {
         MarketingStore.update(MarketingExecution.TABLE_NAME, new QRecord().withValue("id", executionId).withValue("status", ExecutionStatus.CANCELLED.getId())
            .withValue("statusDetail", who + (status == ExecutionStatus.NEEDS_RECONCILIATION ? " while its outcome was unknown: it may have been sent." : ".")));
         QRecord approval = MarketingStore.get(MarketingApproval.TABLE_NAME, execution.getApprovalId());
         if(approval != null && ApprovalStatus.ACTIVE.getId().equals(approval.getValueInteger("status")))
         {
            MarketingStore.update(MarketingApproval.TABLE_NAME, new QRecord().withValue("id", approval.getValue("id")).withValue("status", ApprovalStatus.REVOKED.getId())
               .withValue("invalidatedAt", Instant.now()).withValue("invalidatedReason", who + " (execution cancelled)."));
         }
         MarketingStore.update(MarketingContent.TABLE_NAME, new QRecord().withValue("id", execution.getContentId()).withValue("status", ContentStatus.CANCELLED.getId()));
         return (null);
      });
      return ("Cancelled; its approval is revoked.");
   }



   /*******************************************************************************
    ** Ask the transport about an ambiguous execution.
    *******************************************************************************/
   private String reconcileImpl(Integer executionId) throws QException
   {
      requireExecutePermission();
      MarketingExecution execution = load(executionId);
      requireStatus(execution, ExecutionStatus.NEEDS_RECONCILIATION);
      MarketingTransport transport = MarketingRuntime.current().getTransport(execution.getTransportName());
      if(transport == null)
      {
         throw (new QUserFacingException("Transport [" + execution.getTransportName() + "] is not registered; decide by hand (Mark as Accepted, or Mark Not Sent and Retry)."));
      }

      MarketingTransport.TransportLookup lookup = transport.lookup(execution.getIdempotencyKey());
      switch(lookup.state())
      {
         case FOUND_ACCEPTED ->
         {
            recordAttempt(execution, Instant.now(), AttemptOutcome.ACCEPTED, lookup.receiptId(), "reconciled", "Reconciliation found the request accepted by the provider.", 0, 0);
            markAccepted(execution, lookup.receiptId(), recipientCountOfLastAttempt(executionId), "Found by reconciliation.");
            return ("The provider has it: marked accepted (" + lookup.receiptId() + ").");
         }
         case NOT_FOUND ->
         {
            return ("The provider has no record of it. If you are sure it was not sent, use Mark Not Sent and Retry.");
         }
         default ->
         {
            return ("Transport " + transport.getName() + " cannot look requests up; check the provider and decide by hand.");
         }
      }
   }



   /*******************************************************************************
    ** A person confirms the provider accepted an ambiguous execution.
    *******************************************************************************/
   private String resolveAsAcceptedImpl(Integer executionId) throws QException
   {
      requireExecutePermission();
      MarketingExecution execution = load(executionId);
      requireStatus(execution, ExecutionStatus.NEEDS_RECONCILIATION);
      String who = MarketingStore.currentUserId();
      recordAttempt(execution, Instant.now(), AttemptOutcome.ACCEPTED, null, "confirmed_by_person", "Confirmed accepted by " + who + ".", 0, 0);
      markAccepted(execution, null, recipientCountOfLastAttempt(executionId), "Confirmed by " + who + ".");
      return ("Marked as accepted by " + who + ".");
   }



   /*******************************************************************************
    ** A person decides an ambiguous execution was not sent: schedule it again
    ** (same idempotency key, so a provider that did get it can drop the
    ** repeat). The approval must still cover the content.
    *******************************************************************************/
   private String resolveAsNotSentImpl(Integer executionId) throws QException
   {
      requireExecutePermission();
      MarketingExecution execution = load(executionId);
      requireStatus(execution, ExecutionStatus.NEEDS_RECONCILIATION);
      MarketingContent  content  = new MarketingContent(MarketingStore.require(MarketingContent.TABLE_NAME, execution.getContentId()));
      MarketingApproval approval = ApprovalService.activeApproval(content);
      if(approval == null || !approval.getId().equals(execution.getApprovalId()))
      {
         throw (new QUserFacingException("Its approval no longer covers the current content; approve and schedule again."));
      }

      int attempts = execution.getAttemptCount() == null ? 0 : execution.getAttemptCount();
      int max      = execution.getMaxAttempts() == null ? 1 : execution.getMaxAttempts();
      updateExecution(new QRecord().withValue("id", executionId).withValue("status", ExecutionStatus.SCHEDULED.getId())
         .withValue("maxAttempts", Math.max(max, attempts + 1)).withValue("nextAttemptAt", null)
         .withValue("statusDetail", "Marked not sent by " + MarketingStore.currentUserId() + "; scheduled again with the same idempotency key."));
      setContentStatus(execution.getContentId(), ContentStatus.SCHEDULED);
      return ("Scheduled again (same idempotency key).");
   }



   /*******************************************************************************
    ** RUNNING rows whose lease expired: the worker stopped mid-send, so the
    ** outcome is unknown.
    *******************************************************************************/
   public List<String> recoverStaleLeases(Instant now) throws QException
   {
      List<String> lines = new ArrayList<>();
      for(QRecord record : MarketingStore.query(MarketingExecution.TABLE_NAME, new QQueryFilter(
         new QFilterCriteria("status", QCriteriaOperator.EQUALS, ExecutionStatus.RUNNING.getId()),
         new QFilterCriteria("leaseExpiresAt", QCriteriaOperator.LESS_THAN, now))))
      {
         updateExecution(new QRecord().withValue("id", record.getValue("id")).withValue("status", ExecutionStatus.NEEDS_RECONCILIATION.getId())
            .withValue("statusDetail", "Worker " + record.getValueString("leaseOwner") + " stopped during a send; the outcome is unknown.")
            .withValue("leaseOwner", null).withValue("leaseExpiresAt", null));
         setContentStatus(record.getValueInteger("contentId"), ContentStatus.NEEDS_RECONCILIATION);
         lines.add("Execution #" + record.getValue("id") + ": its worker stopped mid-send; needs reconciliation.");
      }
      return (lines);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static int recipientCountOfLastAttempt(Integer executionId) throws QException
   {
      List<QRecord> attempts = MarketingStore.query(MarketingExecutionAttempt.TABLE_NAME, new QQueryFilter(
         new QFilterCriteria("executionId", QCriteriaOperator.EQUALS, executionId),
         new QFilterCriteria("recipientCount", QCriteriaOperator.GREATER_THAN, 0)).withOrderBy(new QFilterOrderBy("id", false)));
      return (attempts.isEmpty() ? 0 : attempts.get(0).getValueInteger("recipientCount"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void recordAttempt(MarketingExecution execution, Instant startedAt, AttemptOutcome outcome, String receiptId, String providerStatus, String message, int recipients, long millis)
      throws QException
   {
      int attemptNumber = execution.getAttemptCount() == null ? 1 : execution.getAttemptCount() + (outcome == AttemptOutcome.BLOCKED ? 0 : 1);
      MarketingStore.insertInternal(new MarketingExecutionAttempt()
         .withProductId(execution.getProductId())
         .withExecutionId(execution.getId())
         .withAttemptNumber(attemptNumber)
         .withStartedAt(startedAt)
         .withFinishedAt(Instant.now())
         .withOutcome(outcome.getId())
         .withRecipientCount(recipients)
         .withProviderReceiptId(receiptId)
         .withProviderStatus(providerStatus)
         .withMessage(message)
         .withDurationMs(millis)
         .withWorkerName(MarketingRuntime.current().getWorkerName()));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void updateExecution(QRecord record) throws QException
   {
      MarketingStore.updateInternal(MarketingExecution.TABLE_NAME, record);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void setContentStatus(Integer contentId, ContentStatus status) throws QException
   {
      MarketingInternalWrite.run(() ->
      {
         MarketingStore.update(MarketingContent.TABLE_NAME, new QRecord().withValue("id", contentId).withValue("status", status.getId()));
         return (null);
      });
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static MarketingExecution load(Integer executionId) throws QException
   {
      return (new MarketingExecution(MarketingStore.require(MarketingExecution.TABLE_NAME, executionId)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void requireStatus(MarketingExecution execution, ExecutionStatus required) throws QUserFacingException
   {
      if(!required.getId().equals(execution.getStatus()))
      {
         throw (new QUserFacingException("The execution is " + ExecutionStatus.getById(execution.getStatus()).getLabel() + ", not " + required.getLabel() + "."));
      }
   }



   /*******************************************************************************
    ** Defense in depth behind the process permission (system sessions pass:
    ** the scheduler runs the worker).
    *******************************************************************************/
   private static void requireExecutePermission() throws QUserFacingException
   {
      QSession session = QContext.getQSession();
      if(session instanceof QSystemUserSession)
      {
         return;
      }
      if(session == null || !session.hasPermission(MarketingPermissions.EXECUTE))
      {
         throw (new QUserFacingException("You do not have permission to run executions (" + MarketingPermissions.EXECUTE + ")."));
      }
   }


   /*******************************************************************************
    ** Entry point (see the implementation): runs as trusted internal work after
    ** its permission check.
    *******************************************************************************/
   public List<String> runDue(Instant now) throws QException
   {
      return (MarketingStore.trusted(() -> runDueImpl(now)));
   }

   /*******************************************************************************
    ** Entry point (see the implementation): runs as trusted internal work after
    ** its permission check.
    *******************************************************************************/
   public String runOne(Integer executionId, Instant now) throws QException
   {
      requireExecutePermission();
      return (MarketingStore.trusted(() -> runOneImpl(executionId, now)));
   }

   /*******************************************************************************
    ** Entry point (see the implementation): runs as trusted internal work after
    ** its permission check.
    *******************************************************************************/
   public String pause(Integer executionId) throws QException
   {
      requireExecutePermission();
      return (MarketingStore.trusted(() -> pauseImpl(executionId)));
   }

   /*******************************************************************************
    ** Entry point (see the implementation): runs as trusted internal work after
    ** its permission check.
    *******************************************************************************/
   public String resume(Integer executionId) throws QException
   {
      requireExecutePermission();
      return (MarketingStore.trusted(() -> resumeImpl(executionId)));
   }

   /*******************************************************************************
    ** Entry point (see the implementation): runs as trusted internal work after
    ** its permission check.
    *******************************************************************************/
   public String cancel(Integer executionId) throws QException
   {
      requireExecutePermission();
      return (MarketingStore.trusted(() -> cancelImpl(executionId)));
   }

   /*******************************************************************************
    ** Entry point (see the implementation): runs as trusted internal work after
    ** its permission check.
    *******************************************************************************/
   public String reconcile(Integer executionId) throws QException
   {
      requireExecutePermission();
      return (MarketingStore.trusted(() -> reconcileImpl(executionId)));
   }

   /*******************************************************************************
    ** Entry point (see the implementation): runs as trusted internal work after
    ** its permission check.
    *******************************************************************************/
   public String resolveAsAccepted(Integer executionId) throws QException
   {
      requireExecutePermission();
      return (MarketingStore.trusted(() -> resolveAsAcceptedImpl(executionId)));
   }

   /*******************************************************************************
    ** Entry point (see the implementation): runs as trusted internal work after
    ** its permission check.
    *******************************************************************************/
   public String resolveAsNotSent(Integer executionId) throws QException
   {
      requireExecutePermission();
      return (MarketingStore.trusted(() -> resolveAsNotSentImpl(executionId)));
   }

}
