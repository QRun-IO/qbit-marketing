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
import com.kingsrook.qbits.marketing.model.enums.ApprovalStatus;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.data.QField;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;
import com.kingsrook.qqq.backend.core.model.metadata.fields.ValueTooLongBehavior;


/*******************************************************************************
 ** A human approval of one exact content revision, destination, recipient scope and schedule.
 ** Table marketing_approval.
 *******************************************************************************/
public class MarketingApproval extends QRecordEntity
{
   public static final String TABLE_NAME = "marketing_approval";

   @QField(isEditable = false, isPrimaryKey = true)
   private Integer id;

   @QField(possibleValueSourceName = MarketingProduct.TABLE_NAME)
   private Integer productId;

   @QField(isRequired = true, possibleValueSourceName = MarketingContent.TABLE_NAME)
   private Integer contentId;

   @QField(possibleValueSourceName = MarketingContentRevision.TABLE_NAME)
   private Integer revisionId;

   @QField()
   private Integer revisionNumber;

   @QField(maxLength = 64, valueTooLongBehavior = ValueTooLongBehavior.ERROR, isRequired = true)
   private String contentHash;

   @QField(possibleValueSourceName = MarketingChannel.TABLE_NAME)
   private Integer channelId;

   @QField(maxLength = 1000, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String destinationUrl;

   @QField(maxLength = 255, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String recipientScope;

   @QField()
   private Instant scheduledFor;

   @QField(isRequired = true, possibleValueSourceName = ApprovalStatus.NAME)
   private Integer status;

   @QField(maxLength = 100, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String approvedByUserId;

   @QField()
   private Instant approvedAt;

   @QField(maxLength = 500, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String note;

   @QField()
   private Instant invalidatedAt;

   @QField(maxLength = 500, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String invalidatedReason;

   @QField(isEditable = false)
   private Instant createDate;

   @QField(isEditable = false)
   private Instant modifyDate;



   /*******************************************************************************
    ** Default constructor.
    *******************************************************************************/
   public MarketingApproval()
   {
   }



   /*******************************************************************************
    ** Constructor that populates from a QRecord.
    *******************************************************************************/
   public MarketingApproval(QRecord record) throws QException
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
   public MarketingApproval withId(Integer id)
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
   public MarketingApproval withProductId(Integer productId)
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
   public MarketingApproval withContentId(Integer contentId)
   {
      this.contentId = contentId;
      return (this);
   }


   /*******************************************************************************
    ** Getter for revisionId
    *******************************************************************************/
   public Integer getRevisionId()
   {
      return (this.revisionId);
   }



   /*******************************************************************************
    ** Setter for revisionId
    *******************************************************************************/
   public void setRevisionId(Integer revisionId)
   {
      this.revisionId = revisionId;
   }



   /*******************************************************************************
    ** Fluent setter for revisionId
    *******************************************************************************/
   public MarketingApproval withRevisionId(Integer revisionId)
   {
      this.revisionId = revisionId;
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
   public MarketingApproval withRevisionNumber(Integer revisionNumber)
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
   public MarketingApproval withContentHash(String contentHash)
   {
      this.contentHash = contentHash;
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
   public MarketingApproval withChannelId(Integer channelId)
   {
      this.channelId = channelId;
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
   public MarketingApproval withDestinationUrl(String destinationUrl)
   {
      this.destinationUrl = destinationUrl;
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
   public MarketingApproval withRecipientScope(String recipientScope)
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
   public MarketingApproval withScheduledFor(Instant scheduledFor)
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
   public MarketingApproval withStatus(Integer status)
   {
      this.status = status;
      return (this);
   }


   /*******************************************************************************
    ** Getter for approvedByUserId
    *******************************************************************************/
   public String getApprovedByUserId()
   {
      return (this.approvedByUserId);
   }



   /*******************************************************************************
    ** Setter for approvedByUserId
    *******************************************************************************/
   public void setApprovedByUserId(String approvedByUserId)
   {
      this.approvedByUserId = approvedByUserId;
   }



   /*******************************************************************************
    ** Fluent setter for approvedByUserId
    *******************************************************************************/
   public MarketingApproval withApprovedByUserId(String approvedByUserId)
   {
      this.approvedByUserId = approvedByUserId;
      return (this);
   }


   /*******************************************************************************
    ** Getter for approvedAt
    *******************************************************************************/
   public Instant getApprovedAt()
   {
      return (this.approvedAt);
   }



   /*******************************************************************************
    ** Setter for approvedAt
    *******************************************************************************/
   public void setApprovedAt(Instant approvedAt)
   {
      this.approvedAt = approvedAt;
   }



   /*******************************************************************************
    ** Fluent setter for approvedAt
    *******************************************************************************/
   public MarketingApproval withApprovedAt(Instant approvedAt)
   {
      this.approvedAt = approvedAt;
      return (this);
   }


   /*******************************************************************************
    ** Getter for note
    *******************************************************************************/
   public String getNote()
   {
      return (this.note);
   }



   /*******************************************************************************
    ** Setter for note
    *******************************************************************************/
   public void setNote(String note)
   {
      this.note = note;
   }



   /*******************************************************************************
    ** Fluent setter for note
    *******************************************************************************/
   public MarketingApproval withNote(String note)
   {
      this.note = note;
      return (this);
   }


   /*******************************************************************************
    ** Getter for invalidatedAt
    *******************************************************************************/
   public Instant getInvalidatedAt()
   {
      return (this.invalidatedAt);
   }



   /*******************************************************************************
    ** Setter for invalidatedAt
    *******************************************************************************/
   public void setInvalidatedAt(Instant invalidatedAt)
   {
      this.invalidatedAt = invalidatedAt;
   }



   /*******************************************************************************
    ** Fluent setter for invalidatedAt
    *******************************************************************************/
   public MarketingApproval withInvalidatedAt(Instant invalidatedAt)
   {
      this.invalidatedAt = invalidatedAt;
      return (this);
   }


   /*******************************************************************************
    ** Getter for invalidatedReason
    *******************************************************************************/
   public String getInvalidatedReason()
   {
      return (this.invalidatedReason);
   }



   /*******************************************************************************
    ** Setter for invalidatedReason
    *******************************************************************************/
   public void setInvalidatedReason(String invalidatedReason)
   {
      this.invalidatedReason = invalidatedReason;
   }



   /*******************************************************************************
    ** Fluent setter for invalidatedReason
    *******************************************************************************/
   public MarketingApproval withInvalidatedReason(String invalidatedReason)
   {
      this.invalidatedReason = invalidatedReason;
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
   public MarketingApproval withCreateDate(Instant createDate)
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
   public MarketingApproval withModifyDate(Instant modifyDate)
   {
      this.modifyDate = modifyDate;
      return (this);
   }

}
