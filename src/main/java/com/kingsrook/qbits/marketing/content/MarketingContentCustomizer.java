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

package com.kingsrook.qbits.marketing.content;


import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import com.kingsrook.qbits.marketing.MarketingStore;
import com.kingsrook.qbits.marketing.model.MarketingApproval;
import com.kingsrook.qbits.marketing.model.MarketingCampaign;
import com.kingsrook.qbits.marketing.model.MarketingContent;
import com.kingsrook.qbits.marketing.model.MarketingContentRevision;
import com.kingsrook.qbits.marketing.model.MarketingExecution;
import com.kingsrook.qbits.marketing.model.enums.ApprovalStatus;
import com.kingsrook.qbits.marketing.model.enums.AuthorType;
import com.kingsrook.qbits.marketing.model.enums.ContentStatus;
import com.kingsrook.qbits.marketing.model.enums.ExecutionStatus;
import com.kingsrook.qbits.marketing.security.MarketingInternalWrite;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizerInterface;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.statusmessages.BadInputStatusMessage;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;


/*******************************************************************************
 ** Keeps marketing_content honest:
 **
 ** - Every insert, and every edit of an approved field (ContentHasher), appends
 **   an immutable marketing_content_revision: the revision history.
 ** - An edit of an approved field invalidates every ACTIVE approval of the
 **   content (with the reason recorded), sends the content back to Draft, and
 **   blocks its scheduled or paused executions. Approval covers one exact
 **   revision, destination, recipient scope and schedule - nothing else.
 ** - status, revisionNumber, contentHash, authorType and agentRunId are
 **   managed: people change status only through the actions (Submit for
 **   Review, Approve, Schedule, ...), which run inside MarketingInternalWrite.
 **   Only Marketing's own code (the agent's draft tool) may mark a revision as
 **   agent-written.
 ** - productId always follows the campaign.
 *******************************************************************************/
public class MarketingContentCustomizer implements TableCustomizerInterface
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public List<QRecord> preInsert(InsertInput insertInput, List<QRecord> records, boolean isPreview) throws QException
   {
      boolean internal = MarketingInternalWrite.isActive();
      for(QRecord record : records)
      {
         if(!internal)
         {
            record.setValue("status", ContentStatus.DRAFT.getId());
            record.setValue("authorType", AuthorType.HUMAN.getId());
            record.setValue("agentRunId", null);
         }
         if(record.getValue("status") == null)
         {
            record.setValue("status", ContentStatus.DRAFT.getId());
         }
         if(record.getValue("authorType") == null)
         {
            record.setValue("authorType", AuthorType.HUMAN.getId());
         }
         if(record.getValue("ownerUserId") == null)
         {
            record.setValue("ownerUserId", MarketingStore.currentUserId());
         }
         setProductFromCampaign(record, record.getValueInteger("campaignId"));
         normalizeSchedule(record);
         record.setValue("revisionNumber", 1);
         record.setValue("contentHash", ContentHasher.hash(record));
      }
      return (records);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public List<QRecord> postInsert(InsertInput insertInput, List<QRecord> records) throws QException
   {
      for(QRecord record : records)
      {
         if(record.getErrors() == null || record.getErrors().isEmpty())
         {
            appendRevision(record, "Created");
         }
      }
      return (records);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public List<QRecord> preUpdate(UpdateInput updateInput, List<QRecord> records, boolean isPreview, Optional<List<QRecord>> oldRecordList) throws QException
   {
      boolean               internal = MarketingInternalWrite.isActive();
      Map<Integer, QRecord> oldById  = new java.util.HashMap<>();
      for(QRecord old : oldRecordList.orElse(List.of()))
      {
         oldById.put(ValueUtils.getValueAsInteger(old.getValue("id")), old);
      }

      for(QRecord record : records)
      {
         ///////////////////////////////////////////////////////////////////
         // the UI and API pass the id as a String: normalize it, and     //
         // never let an edit through without its old record (fail closed) //
         ///////////////////////////////////////////////////////////////////
         Integer id  = ValueUtils.getValueAsInteger(record.getValue("id"));
         QRecord old = id == null ? null : oldById.get(id);
         if(old == null && id != null)
         {
            old = MarketingStore.get(MarketingContent.TABLE_NAME, id);
         }
         if(old == null)
         {
            record.addError(new BadInputStatusMessage("The content to update was not found."));
            continue;
         }
         record.setValue("id", id);

         ////////////////////////////////////////////////////////////////////
         // managed fields: people can't set them (the UI shows them read- //
         // only; this also covers the API)                                //
         ////////////////////////////////////////////////////////////////////
         if(!internal)
         {
            if(record.getValues().containsKey("status") && !Objects.equals(ValueUtils.getValueAsInteger(record.getValue("status")), old.getValueInteger("status")))
            {
               record.addError(new BadInputStatusMessage("Status changes only through the content actions (Submit for Review, Approve, Schedule, ...)."));
               continue;
            }
            record.removeValue("authorType");
            record.removeValue("agentRunId");
         }
         record.removeValue("revisionNumber");
         record.removeValue("contentHash");
         normalizeSchedule(record);

         QRecord merged = new QRecord(old);
         merged.getValues().putAll(record.getValues());
         if(record.getValues().containsKey("campaignId"))
         {
            setProductFromCampaign(record, record.getValueInteger("campaignId"));
         }

         String newHash = ContentHasher.hash(merged);
         if(!newHash.equals(old.getValueString("contentHash")))
         {
            Integer oldRevision = old.getValueInteger("revisionNumber");
            record.setValue("revisionNumber", (oldRevision == null ? 0 : oldRevision) + 1);
            record.setValue("contentHash", newHash);
            record.setValue("status", ContentStatus.DRAFT.getId());
            if(!internal)
            {
               record.setValue("authorType", AuthorType.HUMAN.getId());
            }
         }
      }
      return (records);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public List<QRecord> postUpdate(UpdateInput updateInput, List<QRecord> records, Optional<List<QRecord>> oldRecordList) throws QException
   {
      ////////////////////////////////////////////////////////////////////
      // preUpdate sets contentHash only when the approved fields changed //
      ////////////////////////////////////////////////////////////////////
      for(QRecord record : records)
      {
         if((record.getErrors() != null && !record.getErrors().isEmpty()) || record.getValue("contentHash") == null)
         {
            continue;
         }

         QRecord current = MarketingStore.get(MarketingContent.TABLE_NAME, ValueUtils.getValueAsInteger(record.getValue("id")));
         if(current == null)
         {
            continue;
         }
         appendRevision(current, "Edited");
         invalidateApprovals(current);
      }
      return (records);
   }



   /*******************************************************************************
    ** Append the current state of a content item as a revision.
    *******************************************************************************/
   static void appendRevision(QRecord content, String changeNote) throws QException
   {
      MarketingContentRevision revision = new MarketingContentRevision()
         .withProductId(content.getValueInteger("productId"))
         .withContentId(content.getValueInteger("id"))
         .withRevisionNumber(content.getValueInteger("revisionNumber"))
         .withTitle(content.getValueString("title"))
         .withBody(content.getValueString("body"))
         .withDestinationUrl(content.getValueString("destinationUrl"))
         .withCallToAction(content.getValueString("callToAction"))
         .withRecipientScope(content.getValueString("recipientScope"))
         .withScheduledFor(ValueUtils.getValueAsInstant(content.getValue("scheduledFor")))
         .withChannelId(content.getValueInteger("channelId"))
         .withContentHash(content.getValueString("contentHash"))
         .withAuthorType(content.getValueInteger("authorType"))
         .withAuthorUserId(MarketingStore.currentUserId())
         .withAgentRunId(AuthorType.AGENT.getId().equals(content.getValueInteger("authorType")) ? content.getValueInteger("agentRunId") : null)
         .withChangeNote(changeNote);
      MarketingStore.insertInternal(revision);
   }



   /*******************************************************************************
    ** An edit voids every active approval of the content and blocks its pending
    ** executions.
    *******************************************************************************/
   static void invalidateApprovals(QRecord content) throws QException
   {
      Integer contentId = content.getValueInteger("id");
      String  reason    = "Content edited (now revision " + content.getValueInteger("revisionNumber") + "); the approved revision no longer matches.";

      for(QRecord approval : MarketingStore.query(MarketingApproval.TABLE_NAME, new QQueryFilter(
         new QFilterCriteria("contentId", QCriteriaOperator.EQUALS, contentId),
         new QFilterCriteria("status", QCriteriaOperator.EQUALS, ApprovalStatus.ACTIVE.getId()))))
      {
         MarketingStore.updateInternal(MarketingApproval.TABLE_NAME, new QRecord()
            .withValue("id", approval.getValue("id"))
            .withValue("status", ApprovalStatus.INVALIDATED.getId())
            .withValue("invalidatedAt", Instant.now())
            .withValue("invalidatedReason", reason));
      }

      for(QRecord execution : MarketingStore.query(MarketingExecution.TABLE_NAME, new QQueryFilter(
         new QFilterCriteria("contentId", QCriteriaOperator.EQUALS, contentId),
         new QFilterCriteria("status", QCriteriaOperator.IN, List.of(ExecutionStatus.SCHEDULED.getId(), ExecutionStatus.PAUSED.getId())))))
      {
         MarketingStore.updateInternal(MarketingExecution.TABLE_NAME, new QRecord()
            .withValue("id", execution.getValue("id"))
            .withValue("status", ExecutionStatus.BLOCKED.getId())
            .withValue("statusDetail", reason));
      }
   }



   /*******************************************************************************
    ** Schedules are kept to the second, so what is approved (and hashed) is
    ** exactly what every database stores.
    *******************************************************************************/
   static void normalizeSchedule(QRecord record)
   {
      if(record.getValues().containsKey("scheduledFor") && record.getValue("scheduledFor") != null)
      {
         Instant instant = ValueUtils.getValueAsInstant(record.getValue("scheduledFor"));
         record.setValue("scheduledFor", instant == null ? null : instant.truncatedTo(java.time.temporal.ChronoUnit.SECONDS));
      }
   }



   /*******************************************************************************
    ** productId follows the campaign.
    *******************************************************************************/
   private static void setProductFromCampaign(QRecord record, Integer campaignId) throws QException
   {
      if(campaignId != null)
      {
         QRecord campaign = MarketingStore.get(MarketingCampaign.TABLE_NAME, campaignId);
         if(campaign != null)
         {
            record.setValue("productId", campaign.getValueInteger("productId"));
         }
      }
   }

}
