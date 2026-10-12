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

package com.kingsrook.qbits.marketing.model;


import java.time.Instant;
import com.kingsrook.qbits.marketing.model.enums.AuthorType;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.data.QField;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;
import com.kingsrook.qqq.backend.core.model.metadata.fields.ValueTooLongBehavior;


/*******************************************************************************
 ** An immutable revision of a content item (written on every relevant edit).
 ** Table marketing_content_revision.
 *******************************************************************************/
public class MarketingContentRevision extends QRecordEntity
{
   public static final String TABLE_NAME = "marketing_content_revision";

   @QField(isEditable = false, isPrimaryKey = true)
   private Integer id;

   @QField(possibleValueSourceName = MarketingProduct.TABLE_NAME)
   private Integer productId;

   @QField(isRequired = true, possibleValueSourceName = MarketingContent.TABLE_NAME)
   private Integer contentId;

   @QField(isRequired = true)
   private Integer revisionNumber;

   @QField(maxLength = 255, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String title;

   @QField()
   private String body;

   @QField(maxLength = 1000, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String destinationUrl;

   @QField(maxLength = 500, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String callToAction;

   @QField(maxLength = 255, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String recipientScope;

   @QField()
   private Instant scheduledFor;

   @QField(possibleValueSourceName = MarketingChannel.TABLE_NAME)
   private Integer channelId;

   @QField(maxLength = 64, valueTooLongBehavior = ValueTooLongBehavior.ERROR, isRequired = true)
   private String contentHash;

   @QField(possibleValueSourceName = AuthorType.NAME)
   private Integer authorType;

   @QField(maxLength = 100, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String authorUserId;

   @QField(possibleValueSourceName = MarketingAgentRun.TABLE_NAME)
   private Integer agentRunId;

   @QField(maxLength = 500, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String changeNote;

   @QField(isEditable = false)
   private Instant createDate;

   @QField(isEditable = false)
   private Instant modifyDate;



   /*******************************************************************************
    ** Default constructor.
    *******************************************************************************/
   public MarketingContentRevision()
   {
   }



   /*******************************************************************************
    ** Constructor that populates from a QRecord.
    *******************************************************************************/
   public MarketingContentRevision(QRecord record) throws QException
   {
      populateFromQRecord(record);
   }


   /*******************************************************************************
    ** Getter for id
    *******************************************************************************/
   public Integer getId()
   {
      return (this.id);
   }



   /*******************************************************************************
    ** Setter for id
    *******************************************************************************/
   public void setId(Integer id)
   {
      this.id = id;
   }



   /*******************************************************************************
    ** Fluent setter for id
    *******************************************************************************/
   public MarketingContentRevision withId(Integer id)
   {
      this.id = id;
      return (this);
   }


   /*******************************************************************************
    ** Getter for productId
    *******************************************************************************/
   public Integer getProductId()
   {
      return (this.productId);
   }



   /*******************************************************************************
    ** Setter for productId
    *******************************************************************************/
   public void setProductId(Integer productId)
   {
      this.productId = productId;
   }



   /*******************************************************************************
    ** Fluent setter for productId
    *******************************************************************************/
   public MarketingContentRevision withProductId(Integer productId)
   {
      this.productId = productId;
      return (this);
   }


   /*******************************************************************************
    ** Getter for contentId
    *******************************************************************************/
   public Integer getContentId()
   {
      return (this.contentId);
   }



   /*******************************************************************************
    ** Setter for contentId
    *******************************************************************************/
   public void setContentId(Integer contentId)
   {
      this.contentId = contentId;
   }



   /*******************************************************************************
    ** Fluent setter for contentId
    *******************************************************************************/
   public MarketingContentRevision withContentId(Integer contentId)
   {
      this.contentId = contentId;
      return (this);
   }


   /*******************************************************************************
    ** Getter for revisionNumber
    *******************************************************************************/
   public Integer getRevisionNumber()
   {
      return (this.revisionNumber);
   }



   /*******************************************************************************
    ** Setter for revisionNumber
    *******************************************************************************/
   public void setRevisionNumber(Integer revisionNumber)
   {
      this.revisionNumber = revisionNumber;
   }



   /*******************************************************************************
    ** Fluent setter for revisionNumber
    *******************************************************************************/
   public MarketingContentRevision withRevisionNumber(Integer revisionNumber)
   {
      this.revisionNumber = revisionNumber;
      return (this);
   }


   /*******************************************************************************
    ** Getter for title
    *******************************************************************************/
   public String getTitle()
   {
      return (this.title);
   }



   /*******************************************************************************
    ** Setter for title
    *******************************************************************************/
   public void setTitle(String title)
   {
      this.title = title;
   }



   /*******************************************************************************
    ** Fluent setter for title
    *******************************************************************************/
   public MarketingContentRevision withTitle(String title)
   {
      this.title = title;
      return (this);
   }


   /*******************************************************************************
    ** Getter for body
    *******************************************************************************/
   public String getBody()
   {
      return (this.body);
   }



   /*******************************************************************************
    ** Setter for body
    *******************************************************************************/
   public void setBody(String body)
   {
      this.body = body;
   }



   /*******************************************************************************
    ** Fluent setter for body
    *******************************************************************************/
   public MarketingContentRevision withBody(String body)
   {
      this.body = body;
      return (this);
   }


   /*******************************************************************************
    ** Getter for destinationUrl
    *******************************************************************************/
   public String getDestinationUrl()
   {
      return (this.destinationUrl);
   }



   /*******************************************************************************
    ** Setter for destinationUrl
    *******************************************************************************/
   public void setDestinationUrl(String destinationUrl)
   {
      this.destinationUrl = destinationUrl;
   }



   /*******************************************************************************
    ** Fluent setter for destinationUrl
    *******************************************************************************/
   public MarketingContentRevision withDestinationUrl(String destinationUrl)
   {
      this.destinationUrl = destinationUrl;
      return (this);
   }


   /*******************************************************************************
    ** Getter for callToAction
    *******************************************************************************/
   public String getCallToAction()
   {
      return (this.callToAction);
   }



   /*******************************************************************************
    ** Setter for callToAction
    *******************************************************************************/
   public void setCallToAction(String callToAction)
   {
      this.callToAction = callToAction;
   }



   /*******************************************************************************
    ** Fluent setter for callToAction
    *******************************************************************************/
   public MarketingContentRevision withCallToAction(String callToAction)
   {
      this.callToAction = callToAction;
      return (this);
   }


   /*******************************************************************************
    ** Getter for recipientScope
    *******************************************************************************/
   public String getRecipientScope()
   {
      return (this.recipientScope);
   }



   /*******************************************************************************
    ** Setter for recipientScope
    *******************************************************************************/
   public void setRecipientScope(String recipientScope)
   {
      this.recipientScope = recipientScope;
   }



   /*******************************************************************************
    ** Fluent setter for recipientScope
    *******************************************************************************/
   public MarketingContentRevision withRecipientScope(String recipientScope)
   {
      this.recipientScope = recipientScope;
      return (this);
   }


   /*******************************************************************************
    ** Getter for scheduledFor
    *******************************************************************************/
   public Instant getScheduledFor()
   {
      return (this.scheduledFor);
   }



   /*******************************************************************************
    ** Setter for scheduledFor
    *******************************************************************************/
   public void setScheduledFor(Instant scheduledFor)
   {
      this.scheduledFor = scheduledFor;
   }



   /*******************************************************************************
    ** Fluent setter for scheduledFor
    *******************************************************************************/
   public MarketingContentRevision withScheduledFor(Instant scheduledFor)
   {
      this.scheduledFor = scheduledFor;
      return (this);
   }


   /*******************************************************************************
    ** Getter for channelId
    *******************************************************************************/
   public Integer getChannelId()
   {
      return (this.channelId);
   }



   /*******************************************************************************
    ** Setter for channelId
    *******************************************************************************/
   public void setChannelId(Integer channelId)
   {
      this.channelId = channelId;
   }



   /*******************************************************************************
    ** Fluent setter for channelId
    *******************************************************************************/
   public MarketingContentRevision withChannelId(Integer channelId)
   {
      this.channelId = channelId;
      return (this);
   }


   /*******************************************************************************
    ** Getter for contentHash
    *******************************************************************************/
   public String getContentHash()
   {
      return (this.contentHash);
   }



   /*******************************************************************************
    ** Setter for contentHash
    *******************************************************************************/
   public void setContentHash(String contentHash)
   {
      this.contentHash = contentHash;
   }



   /*******************************************************************************
    ** Fluent setter for contentHash
    *******************************************************************************/
   public MarketingContentRevision withContentHash(String contentHash)
   {
      this.contentHash = contentHash;
      return (this);
   }


   /*******************************************************************************
    ** Getter for authorType
    *******************************************************************************/
   public Integer getAuthorType()
   {
      return (this.authorType);
   }



   /*******************************************************************************
    ** Setter for authorType
    *******************************************************************************/
   public void setAuthorType(Integer authorType)
   {
      this.authorType = authorType;
   }



   /*******************************************************************************
    ** Fluent setter for authorType
    *******************************************************************************/
   public MarketingContentRevision withAuthorType(Integer authorType)
   {
      this.authorType = authorType;
      return (this);
   }


   /*******************************************************************************
    ** Getter for authorUserId
    *******************************************************************************/
   public String getAuthorUserId()
   {
      return (this.authorUserId);
   }



   /*******************************************************************************
    ** Setter for authorUserId
    *******************************************************************************/
   public void setAuthorUserId(String authorUserId)
   {
      this.authorUserId = authorUserId;
   }



   /*******************************************************************************
    ** Fluent setter for authorUserId
    *******************************************************************************/
   public MarketingContentRevision withAuthorUserId(String authorUserId)
   {
      this.authorUserId = authorUserId;
      return (this);
   }


   /*******************************************************************************
    ** Getter for agentRunId
    *******************************************************************************/
   public Integer getAgentRunId()
   {
      return (this.agentRunId);
   }



   /*******************************************************************************
    ** Setter for agentRunId
    *******************************************************************************/
   public void setAgentRunId(Integer agentRunId)
   {
      this.agentRunId = agentRunId;
   }



   /*******************************************************************************
    ** Fluent setter for agentRunId
    *******************************************************************************/
   public MarketingContentRevision withAgentRunId(Integer agentRunId)
   {
      this.agentRunId = agentRunId;
      return (this);
   }


   /*******************************************************************************
    ** Getter for changeNote
    *******************************************************************************/
   public String getChangeNote()
   {
      return (this.changeNote);
   }



   /*******************************************************************************
    ** Setter for changeNote
    *******************************************************************************/
   public void setChangeNote(String changeNote)
   {
      this.changeNote = changeNote;
   }



   /*******************************************************************************
    ** Fluent setter for changeNote
    *******************************************************************************/
   public MarketingContentRevision withChangeNote(String changeNote)
   {
      this.changeNote = changeNote;
      return (this);
   }


   /*******************************************************************************
    ** Getter for createDate
    *******************************************************************************/
   public Instant getCreateDate()
   {
      return (this.createDate);
   }



   /*******************************************************************************
    ** Setter for createDate
    *******************************************************************************/
   public void setCreateDate(Instant createDate)
   {
      this.createDate = createDate;
   }



   /*******************************************************************************
    ** Fluent setter for createDate
    *******************************************************************************/
   public MarketingContentRevision withCreateDate(Instant createDate)
   {
      this.createDate = createDate;
      return (this);
   }


   /*******************************************************************************
    ** Getter for modifyDate
    *******************************************************************************/
   public Instant getModifyDate()
   {
      return (this.modifyDate);
   }



   /*******************************************************************************
    ** Setter for modifyDate
    *******************************************************************************/
   public void setModifyDate(Instant modifyDate)
   {
      this.modifyDate = modifyDate;
   }



   /*******************************************************************************
    ** Fluent setter for modifyDate
    *******************************************************************************/
   public MarketingContentRevision withModifyDate(Instant modifyDate)
   {
      this.modifyDate = modifyDate;
      return (this);
   }

}
