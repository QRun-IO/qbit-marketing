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

package com.kingsrook.qbits.marketing.approval;


import java.time.Instant;
import java.util.List;
import com.kingsrook.qbits.marketing.MarketingRuntime;
import com.kingsrook.qbits.marketing.MarketingStore;
import com.kingsrook.qbits.marketing.model.MarketingApproval;
import com.kingsrook.qbits.marketing.model.MarketingChannel;
import com.kingsrook.qbits.marketing.model.MarketingContent;
import com.kingsrook.qbits.marketing.model.MarketingContentRevision;
import com.kingsrook.qbits.marketing.model.MarketingExecution;
import com.kingsrook.qbits.marketing.model.MarketingFinding;
import com.kingsrook.qbits.marketing.model.enums.AiContentPolicy;
import com.kingsrook.qbits.marketing.model.enums.ApprovalStatus;
import com.kingsrook.qbits.marketing.model.enums.AuthorType;
import com.kingsrook.qbits.marketing.model.enums.ChannelType;
import com.kingsrook.qbits.marketing.model.enums.ContentStatus;
import com.kingsrook.qbits.marketing.model.enums.ExecutionStatus;
import com.kingsrook.qbits.marketing.model.enums.FindingSeverity;
import com.kingsrook.qbits.marketing.model.enums.FindingStatus;
import com.kingsrook.qbits.marketing.security.MarketingInternalWrite;
import com.kingsrook.qbits.marketing.security.MarketingPermissions;
import com.kingsrook.qbits.marketing.transport.ControlledTestTransport;
import com.kingsrook.qbits.marketing.transport.MarketingTransport;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 ** Moves content from draft to review, approval and schedule.
 **
 ** Every method checks the state it needs and throws a QUserFacingException
 ** (one plain sentence) when it can't proceed; the record actions turn those
 ** into Review / Result summary lines. Each also re-checks the permission the
 ** step needs, as defense in depth behind the process permission.
 **
 ** Approval binds the exact content: the approval row stores the content hash
 ** (text, destination, recipient scope, schedule, channel) and the revision.
 *******************************************************************************/
public class ApprovalService
{

   /*******************************************************************************
    ** DRAFT -> IN_REVIEW.
    *******************************************************************************/
   private String submitForReviewImpl(Integer contentId) throws QException
   {
      requirePermission(MarketingPermissions.PREPARE, "submit content for review");
      MarketingContent content = loadContent(contentId);
      requireStatus(content, ContentStatus.DRAFT);

      MarketingChannel channel = requireReadyFields(content);
      setStatus(content.getId(), ContentStatus.IN_REVIEW);
      return ("Submitted for review (revision " + content.getRevisionNumber() + ", channel " + channel.getName() + ").");
   }



   /*******************************************************************************
    ** IN_REVIEW -> APPROVED: record a human approval of this exact revision,
    ** destination, recipient scope and schedule.
    *******************************************************************************/
   private String approveImpl(Integer contentId, String note) throws QException
   {
      requirePermission(MarketingPermissions.APPROVE, "approve content");
      QSession session = QContext.getQSession();
      if(session instanceof QSystemUserSession)
      {
         throw (new QUserFacingException("Approval needs a person; the system user cannot approve content."));
      }

      MarketingContent content = loadContent(contentId);
      requireStatus(content, ContentStatus.IN_REVIEW);
      MarketingChannel channel = requireReadyFields(content);

      //////////////////////////////////////////////////////////////////
      // open blocker findings (unsupported claims, channel rules...) //
      //////////////////////////////////////////////////////////////////
      int blockers = MarketingStore.count(MarketingFinding.TABLE_NAME, new QQueryFilter(
         new QFilterCriteria("contentId", QCriteriaOperator.EQUALS, contentId),
         new QFilterCriteria("severity", QCriteriaOperator.EQUALS, FindingSeverity.BLOCKER.getId()),
         new QFilterCriteria("status", QCriteriaOperator.EQUALS, FindingStatus.OPEN.getId())));
      if(blockers > 0)
      {
         throw (new QUserFacingException(blockers + " open blocker finding(s) on this content must be resolved or dismissed first."));
      }

      MarketingContentRevision revision = currentRevision(content);
      checkAiContentPolicy(channel, revision, content);

      String approver = MarketingStore.currentUserId();
      if(Boolean.TRUE.equals(MarketingRuntime.current().getConfig().getRequireDifferentApprover()) && approver.equals(revision.getAuthorUserId()))
      {
         throw (new QUserFacingException("The author of this revision cannot approve it; another person must."));
      }

      MarketingInternalWrite.run(() ->
      {
         MarketingStore.insert(new MarketingApproval()
            .withProductId(content.getProductId())
            .withContentId(content.getId())
            .withRevisionId(revision.getId())
            .withRevisionNumber(revision.getRevisionNumber())
            .withContentHash(content.getContentHash())
            .withChannelId(content.getChannelId())
            .withDestinationUrl(content.getDestinationUrl())
            .withRecipientScope(content.getRecipientScope())
            .withScheduledFor(content.getScheduledFor())
            .withStatus(ApprovalStatus.ACTIVE.getId())
            .withApprovedByUserId(approver)
            .withApprovedAt(Instant.now())
            .withNote(StringUtils.hasContent(note) ? note : null));
         MarketingStore.update(MarketingContent.TABLE_NAME, new QRecord().withValue("id", content.getId()).withValue("status", ContentStatus.APPROVED.getId()));
         return (null);
      });
      return ("Approved revision " + revision.getRevisionNumber() + " for " + channel.getName() + ", scheduled for " + content.getScheduledFor() + ".");
   }



   /*******************************************************************************
    ** Send content back to draft: revoke its active approvals and cancel its
    ** pending executions.
    *******************************************************************************/
   private String returnToDraftImpl(Integer contentId, String reason) throws QException
   {
      requirePermission(MarketingPermissions.APPROVE, "return content to draft");
      MarketingContent content = loadContent(contentId);
      ContentStatus    status  = ContentStatus.getById(content.getStatus());
      if(status == ContentStatus.DRAFT || status == ContentStatus.ACCEPTED || status == ContentStatus.CANCELLED)
      {
         throw (new QUserFacingException("Content that is " + status.getLabel() + " can't be returned to draft."));
      }

      String why = StringUtils.hasContent(reason) ? reason : "Returned to draft by " + MarketingStore.currentUserId();
      MarketingInternalWrite.run(() ->
      {
         revokeActiveApprovals(contentId, why);
         for(QRecord execution : pendingExecutions(contentId))
         {
            MarketingStore.update(MarketingExecution.TABLE_NAME, new QRecord().withValue("id", execution.getValue("id"))
               .withValue("status", ExecutionStatus.CANCELLED.getId()).withValue("statusDetail", why));
         }
         MarketingStore.update(MarketingContent.TABLE_NAME, new QRecord().withValue("id", contentId).withValue("status", ContentStatus.DRAFT.getId()));
         return (null);
      });
      return ("Returned to draft; active approvals revoked and pending executions cancelled.");
   }



   /*******************************************************************************
    ** APPROVED -> SCHEDULED: create the durable execution job (idempotent: one
    ** execution per approval).
    *******************************************************************************/
   private String scheduleImpl(Integer contentId) throws QException
   {
      requirePermission(MarketingPermissions.EXECUTE, "schedule content");
      MarketingContent content = loadContent(contentId);
      requireStatus(content, ContentStatus.APPROVED);

      MarketingApproval approval = activeApproval(content);
      if(approval == null)
      {
         throw (new QUserFacingException("There is no active approval of the current revision."));
      }

      MarketingChannel   channel   = new MarketingChannel(MarketingStore.require(MarketingChannel.TABLE_NAME, content.getChannelId()));
      MarketingTransport transport = MarketingRuntime.current().getTransport(channel.getTransportName());
      if(transport == null)
      {
         throw (new QUserFacingException("Channel " + channel.getName() + " names transport [" + channel.getTransportName() + "], which is not registered: it is not a working connector."));
      }

      String        idempotencyKey = "marketing-execution:" + contentId + ":" + approval.getId();
      List<QRecord> existing       = MarketingStore.queryBy(MarketingExecution.TABLE_NAME, "idempotencyKey", idempotencyKey);
      if(!existing.isEmpty())
      {
         return ("Already scheduled as execution #" + existing.get(0).getValue("id") + "; nothing new was created.");
      }

      QRecord execution = MarketingInternalWrite.run(() ->
      {
         QRecord stored = MarketingStore.insert(new MarketingExecution()
            .withProductId(content.getProductId())
            .withCampaignId(content.getCampaignId())
            .withContentId(contentId)
            .withApprovalId(approval.getId())
            .withRevisionId(approval.getRevisionId())
            .withChannelId(channel.getId())
            .withTransportName(transport.getName())
            .withScheduledFor(approval.getScheduledFor())
            .withStatus(ExecutionStatus.SCHEDULED.getId())
            .withIdempotencyKey(idempotencyKey)
            .withAttemptCount(0)
            .withMaxAttempts(MarketingRuntime.current().getConfig().getDefaultMaxAttempts())
            .withRequestedByUserId(MarketingStore.currentUserId())
            .withIsTestData(transport instanceof ControlledTestTransport));
         MarketingStore.update(MarketingContent.TABLE_NAME, new QRecord().withValue("id", contentId).withValue("status", ContentStatus.SCHEDULED.getId()));
         return (stored);
      });

      String note = transport.getStatus().isUsable() ? "" : " Transport " + transport.getName() + " is not usable yet (" + transport.getStatus().detail() + "); the execution will be blocked until it is.";
      return ("Scheduled as execution #" + execution.getValue("id") + " for " + approval.getScheduledFor() + " via " + transport.getName() + "." + note);
   }



   /*******************************************************************************
    ** The active approval of the content's current revision (hash match), or null.
    *******************************************************************************/
   public static MarketingApproval activeApproval(MarketingContent content) throws QException
   {
      List<QRecord> approvals = MarketingStore.query(MarketingApproval.TABLE_NAME, new QQueryFilter(
         new QFilterCriteria("contentId", QCriteriaOperator.EQUALS, content.getId()),
         new QFilterCriteria("status", QCriteriaOperator.EQUALS, ApprovalStatus.ACTIVE.getId()),
         new QFilterCriteria("contentHash", QCriteriaOperator.EQUALS, content.getContentHash())));
      return (approvals.isEmpty() ? null : new MarketingApproval(approvals.get(0)));
   }



   /*******************************************************************************
    ** The channel's rules on AI-generated content apply to agent-written
    ** revisions, whatever a person approves.
    *******************************************************************************/
   public static void checkAiContentPolicy(MarketingChannel channel, MarketingContentRevision revision, MarketingContent content) throws QException
   {
      if(!AuthorType.AGENT.getId().equals(revision.getAuthorType()))
      {
         return;
      }

      AiContentPolicy policy = AiContentPolicy.getById(channel.getAiContentPolicy());
      if(policy == AiContentPolicy.NOT_ALLOWED)
      {
         throw (new QUserFacingException("Channel " + channel.getName() + " does not allow AI-generated content; this revision was written by the agent. A person must write it."));
      }
      if(policy == null || policy == AiContentPolicy.UNKNOWN)
      {
         throw (new QUserFacingException("Channel " + channel.getName() + "'s rules on AI-generated content are unknown; record them on the channel before approving agent-written content."));
      }
      if(policy == AiContentPolicy.ALLOWED_WITH_DISCLOSURE)
      {
         String disclosure = MarketingRuntime.current().getConfig().getAiDisclosureText();
         if(content.getBody() == null || !content.getBody().contains(disclosure))
         {
            throw (new QUserFacingException("Channel " + channel.getName() + " requires disclosure of AI-generated content; add \"" + disclosure + "\" to the text."));
         }
      }
   }



   /*******************************************************************************
    ** The revision that matches the content's current hash.
    *******************************************************************************/
   public static MarketingContentRevision currentRevision(MarketingContent content) throws QException
   {
      List<QRecord> revisions = MarketingStore.query(MarketingContentRevision.TABLE_NAME, new QQueryFilter(
         new QFilterCriteria("contentId", QCriteriaOperator.EQUALS, content.getId()),
         new QFilterCriteria("revisionNumber", QCriteriaOperator.EQUALS, content.getRevisionNumber())));
      if(revisions.isEmpty())
      {
         throw (new QUserFacingException("Revision " + content.getRevisionNumber() + " of this content is missing."));
      }
      return (new MarketingContentRevision(revisions.get(0)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   static void revokeActiveApprovals(Integer contentId, String reason) throws QException
   {
      for(QRecord approval : MarketingStore.query(MarketingApproval.TABLE_NAME, new QQueryFilter(
         new QFilterCriteria("contentId", QCriteriaOperator.EQUALS, contentId),
         new QFilterCriteria("status", QCriteriaOperator.EQUALS, ApprovalStatus.ACTIVE.getId()))))
      {
         MarketingStore.update(MarketingApproval.TABLE_NAME, new QRecord().withValue("id", approval.getValue("id"))
            .withValue("status", ApprovalStatus.REVOKED.getId()).withValue("invalidatedAt", Instant.now()).withValue("invalidatedReason", reason));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static List<QRecord> pendingExecutions(Integer contentId) throws QException
   {
      return (MarketingStore.query(MarketingExecution.TABLE_NAME, new QQueryFilter(
         new QFilterCriteria("contentId", QCriteriaOperator.EQUALS, contentId),
         new QFilterCriteria("status", QCriteriaOperator.IN, List.of(ExecutionStatus.SCHEDULED.getId(), ExecutionStatus.PAUSED.getId(), ExecutionStatus.BLOCKED.getId())))));
   }



   /*******************************************************************************
    ** The fields a reviewer needs; returns the channel.
    *******************************************************************************/
   private static MarketingChannel requireReadyFields(MarketingContent content) throws QException
   {
      if(!StringUtils.hasContent(content.getBody()))
      {
         throw (new QUserFacingException("The content has no text."));
      }
      if(content.getChannelId() == null)
      {
         throw (new QUserFacingException("Choose a channel first."));
      }
      if(content.getScheduledFor() == null)
      {
         throw (new QUserFacingException("Set the schedule (Scheduled For) first: approval covers the schedule."));
      }

      MarketingChannel channel = new MarketingChannel(MarketingStore.require(MarketingChannel.TABLE_NAME, content.getChannelId()));
      if(!Boolean.TRUE.equals(channel.getIsActive()))
      {
         throw (new QUserFacingException("Channel " + channel.getName() + " is not active."));
      }

      String scope = content.getRecipientScope();
      if(StringUtils.hasContent(scope) && !"public".equals(scope) && !scope.matches("^crm-(list|contact):[0-9]{1,9}$"))
      {
         throw (new QUserFacingException("Recipient scope [" + scope + "] must be empty or public (a publication), crm-list:<id> or crm-contact:<id>."));
      }
      boolean crmScope = scope != null && scope.startsWith("crm-");
      if(ChannelType.EMAIL.getId().equals(channel.getChannelType()) && !crmScope)
      {
         throw (new QUserFacingException("Email needs a recipient scope (crm-list:<id> or crm-contact:<id>)."));
      }
      if(crmScope && !ChannelType.EMAIL.getId().equals(channel.getChannelType()) && !ChannelType.TEST_SINK.getId().equals(channel.getChannelType()))
      {
         throw (new QUserFacingException("CRM recipients only apply to email channels."));
      }
      return (channel);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static MarketingContent loadContent(Integer contentId) throws QException
   {
      return (new MarketingContent(MarketingStore.require(MarketingContent.TABLE_NAME, contentId)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void requireStatus(MarketingContent content, ContentStatus required) throws QUserFacingException
   {
      if(!required.getId().equals(content.getStatus()))
      {
         ContentStatus status = ContentStatus.getById(content.getStatus());
         throw (new QUserFacingException("The content is " + (status == null ? "in an unknown state" : status.getLabel()) + ", not " + required.getLabel() + "."));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void setStatus(Integer contentId, ContentStatus status) throws QException
   {
      MarketingInternalWrite.run(() ->
      {
         MarketingStore.update(MarketingContent.TABLE_NAME, new QRecord().withValue("id", contentId).withValue("status", status.getId()));
         return (null);
      });
   }



   /*******************************************************************************
    ** Defense in depth behind the process permission. System sessions pass
    ** (the scheduler); approve() refuses them separately.
    *******************************************************************************/
   static void requirePermission(String permission, String what) throws QUserFacingException
   {
      QSession session = QContext.getQSession();
      if(session instanceof QSystemUserSession)
      {
         return;
      }
      if(session == null || !session.hasPermission(permission))
      {
         throw (new QUserFacingException("You do not have permission to " + what + " (" + permission + ")."));
      }
   }


   /*******************************************************************************
    ** Entry point (see the implementation): runs as trusted internal work after
    ** its permission check.
    *******************************************************************************/
   public String submitForReview(Integer contentId) throws QException
   {
      requirePermission(MarketingPermissions.PREPARE, "submit content for review");
      return (MarketingStore.trusted(() -> submitForReviewImpl(contentId)));
   }

   /*******************************************************************************
    ** Entry point (see the implementation): runs as trusted internal work after
    ** its permission check.
    *******************************************************************************/
   public String approve(Integer contentId, String note) throws QException
   {
      requirePermission(MarketingPermissions.APPROVE, "approve content");
      return (MarketingStore.trusted(() -> approveImpl(contentId, note)));
   }

   /*******************************************************************************
    ** Entry point (see the implementation): runs as trusted internal work after
    ** its permission check.
    *******************************************************************************/
   public String returnToDraft(Integer contentId, String reason) throws QException
   {
      requirePermission(MarketingPermissions.APPROVE, "return content to draft");
      return (MarketingStore.trusted(() -> returnToDraftImpl(contentId, reason)));
   }

   /*******************************************************************************
    ** Entry point (see the implementation): runs as trusted internal work after
    ** its permission check.
    *******************************************************************************/
   public String schedule(Integer contentId) throws QException
   {
      requirePermission(MarketingPermissions.EXECUTE, "schedule content");
      return (MarketingStore.trusted(() -> scheduleImpl(contentId)));
   }

}
