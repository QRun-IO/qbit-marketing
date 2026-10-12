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
import com.kingsrook.qbits.marketing.model.enums.ContentStatus;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.data.QField;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;
import com.kingsrook.qqq.backend.core.model.metadata.fields.ValueTooLongBehavior;


/*******************************************************************************
 ** A content item: the exact publication or message proposed for a channel.
 ** Table marketing_content.
 *******************************************************************************/
public class MarketingContent extends QRecordEntity
{
   public static final String TABLE_NAME = "marketing_content";

   @QField(isEditable = false, isPrimaryKey = true)
   private Integer id;

   @QField(isEditable = false, possibleValueSourceName = MarketingProduct.TABLE_NAME)
   private Integer productId;

   @QField(isRequired = true, possibleValueSourceName = MarketingCampaign.TABLE_NAME)
   private Integer campaignId;

   @QField(possibleValueSourceName = MarketingChannel.TABLE_NAME)
   private Integer channelId;

   @QField(maxLength = 255, valueTooLongBehavior = ValueTooLongBehavior.ERROR, isRequired = true)
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

   @QField(isRequired = true, possibleValueSourceName = ContentStatus.NAME)
   private Integer status;

   @QField(possibleValueSourceName = AuthorType.NAME)
   private Integer authorType;

   @QField(isEditable = false)
   private Integer revisionNumber;

   @QField(maxLength = 64, valueTooLongBehavior = ValueTooLongBehavior.ERROR, isEditable = false)
   private String contentHash;

   @QField(isEditable = false, possibleValueSourceName = MarketingAgentRun.TABLE_NAME)
   private Integer agentRunId;

   @QField(maxLength = 100, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String ownerUserId;

   @QField(isEditable = false)
   private Instant createDate;

   @QField(isEditable = false)
   private Instant modifyDate;



   /*******************************************************************************
    ** Default constructor.
    *******************************************************************************/
   public MarketingContent()
   {
   }



   /*******************************************************************************
    ** Constructor that populates from a QRecord.
    *******************************************************************************/
   public MarketingContent(QRecord record) throws QException
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
   public MarketingContent withId(Integer id)
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
   public MarketingContent withProductId(Integer productId)
   {
      this.productId = productId;
      return (this);
   }


   /*******************************************************************************
    ** Getter for campaignId
    *******************************************************************************/
   public Integer getCampaignId()
   {
      return (this.campaignId);
   }



   /*******************************************************************************
    ** Setter for campaignId
    *******************************************************************************/
   public void setCampaignId(Integer campaignId)
   {
      this.campaignId = campaignId;
   }



   /*******************************************************************************
    ** Fluent setter for campaignId
    *******************************************************************************/
   public MarketingContent withCampaignId(Integer campaignId)
   {
      this.campaignId = campaignId;
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
   public MarketingContent withChannelId(Integer channelId)
   {
      this.channelId = channelId;
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
   public MarketingContent withTitle(String title)
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
   public MarketingContent withBody(String body)
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
   public MarketingContent withDestinationUrl(String destinationUrl)
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
   public MarketingContent withCallToAction(String callToAction)
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
   public MarketingContent withRecipientScope(String recipientScope)
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
   public MarketingContent withScheduledFor(Instant scheduledFor)
   {
      this.scheduledFor = scheduledFor;
      return (this);
   }


   /*******************************************************************************
    ** Getter for status
    *******************************************************************************/
   public Integer getStatus()
   {
      return (this.status);
   }



   /*******************************************************************************
    ** Setter for status
    *******************************************************************************/
   public void setStatus(Integer status)
   {
      this.status = status;
   }



   /*******************************************************************************
    ** Fluent setter for status
    *******************************************************************************/
   public MarketingContent withStatus(Integer status)
   {
      this.status = status;
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
   public MarketingContent withAuthorType(Integer authorType)
   {
      this.authorType = authorType;
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
   public MarketingContent withRevisionNumber(Integer revisionNumber)
   {
      this.revisionNumber = revisionNumber;
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
   public MarketingContent withContentHash(String contentHash)
   {
      this.contentHash = contentHash;
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
   public MarketingContent withAgentRunId(Integer agentRunId)
   {
      this.agentRunId = agentRunId;
      return (this);
   }


   /*******************************************************************************
    ** Getter for ownerUserId
    *******************************************************************************/
   public String getOwnerUserId()
   {
      return (this.ownerUserId);
   }



   /*******************************************************************************
    ** Setter for ownerUserId
    *******************************************************************************/
   public void setOwnerUserId(String ownerUserId)
   {
      this.ownerUserId = ownerUserId;
   }



   /*******************************************************************************
    ** Fluent setter for ownerUserId
    *******************************************************************************/
   public MarketingContent withOwnerUserId(String ownerUserId)
   {
      this.ownerUserId = ownerUserId;
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
   public MarketingContent withCreateDate(Instant createDate)
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
   public MarketingContent withModifyDate(Instant modifyDate)
   {
      this.modifyDate = modifyDate;
      return (this);
   }

}
