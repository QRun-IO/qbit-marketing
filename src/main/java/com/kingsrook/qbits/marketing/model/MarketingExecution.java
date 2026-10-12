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
import com.kingsrook.qbits.marketing.model.enums.ExecutionStatus;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.data.QField;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;
import com.kingsrook.qqq.backend.core.model.metadata.fields.ValueTooLongBehavior;


/*******************************************************************************
 ** A durable execution job: one approved content item to one channel transport.
 ** Table marketing_execution.
 *******************************************************************************/
public class MarketingExecution extends QRecordEntity
{
   public static final String TABLE_NAME = "marketing_execution";

   @QField(isEditable = false, isPrimaryKey = true)
   private Integer id;

   @QField(possibleValueSourceName = MarketingProduct.TABLE_NAME)
   private Integer productId;

   @QField(possibleValueSourceName = MarketingCampaign.TABLE_NAME)
   private Integer campaignId;

   @QField(isRequired = true, possibleValueSourceName = MarketingContent.TABLE_NAME)
   private Integer contentId;

   @QField(isRequired = true, possibleValueSourceName = MarketingApproval.TABLE_NAME)
   private Integer approvalId;

   @QField(possibleValueSourceName = MarketingContentRevision.TABLE_NAME)
   private Integer revisionId;

   @QField(possibleValueSourceName = MarketingChannel.TABLE_NAME)
   private Integer channelId;

   @QField(maxLength = 64, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String transportName;

   @QField()
   private Instant scheduledFor;

   @QField(isRequired = true, possibleValueSourceName = ExecutionStatus.NAME)
   private Integer status;

   @QField(maxLength = 200, valueTooLongBehavior = ValueTooLongBehavior.ERROR, isRequired = true)
   private String idempotencyKey;

   @QField()
   private Integer attemptCount;

   @QField()
   private Integer maxAttempts;

   @QField()
   private Instant nextAttemptAt;

   @QField()
   private Instant lastAttemptAt;

   @QField()
   private Instant completedAt;

   @QField(maxLength = 255, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String providerReceiptId;

   @QField()
   private String statusDetail;

   @QField(maxLength = 100, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String requestedByUserId;

   @QField()
   private Boolean isTestData;

   @QField(maxLength = 100, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String leaseOwner;

   @QField()
   private Instant leaseExpiresAt;

   @QField(isEditable = false)
   private Instant createDate;

   @QField(isEditable = false)
   private Instant modifyDate;



   /*******************************************************************************
    ** Default constructor.
    *******************************************************************************/
   public MarketingExecution()
   {
   }



   /*******************************************************************************
    ** Constructor that populates from a QRecord.
    *******************************************************************************/
   public MarketingExecution(QRecord record) throws QException
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
   public MarketingExecution withId(Integer id)
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
   public MarketingExecution withProductId(Integer productId)
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
   public MarketingExecution withCampaignId(Integer campaignId)
   {
      this.campaignId = campaignId;
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
   public MarketingExecution withContentId(Integer contentId)
   {
      this.contentId = contentId;
      return (this);
   }


   /*******************************************************************************
    ** Getter for approvalId
    *******************************************************************************/
   public Integer getApprovalId()
   {
      return (this.approvalId);
   }



   /*******************************************************************************
    ** Setter for approvalId
    *******************************************************************************/
   public void setApprovalId(Integer approvalId)
   {
      this.approvalId = approvalId;
   }



   /*******************************************************************************
    ** Fluent setter for approvalId
    *******************************************************************************/
   public MarketingExecution withApprovalId(Integer approvalId)
   {
      this.approvalId = approvalId;
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
   public MarketingExecution withRevisionId(Integer revisionId)
   {
      this.revisionId = revisionId;
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
   public MarketingExecution withChannelId(Integer channelId)
   {
      this.channelId = channelId;
      return (this);
   }


   /*******************************************************************************
    ** Getter for transportName
    *******************************************************************************/
   public String getTransportName()
   {
      return (this.transportName);
   }



   /*******************************************************************************
    ** Setter for transportName
    *******************************************************************************/
   public void setTransportName(String transportName)
   {
      this.transportName = transportName;
   }



   /*******************************************************************************
    ** Fluent setter for transportName
    *******************************************************************************/
   public MarketingExecution withTransportName(String transportName)
   {
      this.transportName = transportName;
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
   public MarketingExecution withScheduledFor(Instant scheduledFor)
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
   public MarketingExecution withStatus(Integer status)
   {
      this.status = status;
      return (this);
   }


   /*******************************************************************************
    ** Getter for idempotencyKey
    *******************************************************************************/
   public String getIdempotencyKey()
   {
      return (this.idempotencyKey);
   }



   /*******************************************************************************
    ** Setter for idempotencyKey
    *******************************************************************************/
   public void setIdempotencyKey(String idempotencyKey)
   {
      this.idempotencyKey = idempotencyKey;
   }



   /*******************************************************************************
    ** Fluent setter for idempotencyKey
    *******************************************************************************/
   public MarketingExecution withIdempotencyKey(String idempotencyKey)
   {
      this.idempotencyKey = idempotencyKey;
      return (this);
   }


   /*******************************************************************************
    ** Getter for attemptCount
    *******************************************************************************/
   public Integer getAttemptCount()
   {
      return (this.attemptCount);
   }



   /*******************************************************************************
    ** Setter for attemptCount
    *******************************************************************************/
   public void setAttemptCount(Integer attemptCount)
   {
      this.attemptCount = attemptCount;
   }



   /*******************************************************************************
    ** Fluent setter for attemptCount
    *******************************************************************************/
   public MarketingExecution withAttemptCount(Integer attemptCount)
   {
      this.attemptCount = attemptCount;
      return (this);
   }


   /*******************************************************************************
    ** Getter for maxAttempts
    *******************************************************************************/
   public Integer getMaxAttempts()
   {
      return (this.maxAttempts);
   }



   /*******************************************************************************
    ** Setter for maxAttempts
    *******************************************************************************/
   public void setMaxAttempts(Integer maxAttempts)
   {
      this.maxAttempts = maxAttempts;
   }



   /*******************************************************************************
    ** Fluent setter for maxAttempts
    *******************************************************************************/
   public MarketingExecution withMaxAttempts(Integer maxAttempts)
   {
      this.maxAttempts = maxAttempts;
      return (this);
   }


   /*******************************************************************************
    ** Getter for nextAttemptAt
    *******************************************************************************/
   public Instant getNextAttemptAt()
   {
      return (this.nextAttemptAt);
   }



   /*******************************************************************************
    ** Setter for nextAttemptAt
    *******************************************************************************/
   public void setNextAttemptAt(Instant nextAttemptAt)
   {
      this.nextAttemptAt = nextAttemptAt;
   }



   /*******************************************************************************
    ** Fluent setter for nextAttemptAt
    *******************************************************************************/
   public MarketingExecution withNextAttemptAt(Instant nextAttemptAt)
   {
      this.nextAttemptAt = nextAttemptAt;
      return (this);
   }


   /*******************************************************************************
    ** Getter for lastAttemptAt
    *******************************************************************************/
   public Instant getLastAttemptAt()
   {
      return (this.lastAttemptAt);
   }



   /*******************************************************************************
    ** Setter for lastAttemptAt
    *******************************************************************************/
   public void setLastAttemptAt(Instant lastAttemptAt)
   {
      this.lastAttemptAt = lastAttemptAt;
   }



   /*******************************************************************************
    ** Fluent setter for lastAttemptAt
    *******************************************************************************/
   public MarketingExecution withLastAttemptAt(Instant lastAttemptAt)
   {
      this.lastAttemptAt = lastAttemptAt;
      return (this);
   }


   /*******************************************************************************
    ** Getter for completedAt
    *******************************************************************************/
   public Instant getCompletedAt()
   {
      return (this.completedAt);
   }



   /*******************************************************************************
    ** Setter for completedAt
    *******************************************************************************/
   public void setCompletedAt(Instant completedAt)
   {
      this.completedAt = completedAt;
   }



   /*******************************************************************************
    ** Fluent setter for completedAt
    *******************************************************************************/
   public MarketingExecution withCompletedAt(Instant completedAt)
   {
      this.completedAt = completedAt;
      return (this);
   }


   /*******************************************************************************
    ** Getter for providerReceiptId
    *******************************************************************************/
   public String getProviderReceiptId()
   {
      return (this.providerReceiptId);
   }



   /*******************************************************************************
    ** Setter for providerReceiptId
    *******************************************************************************/
   public void setProviderReceiptId(String providerReceiptId)
   {
      this.providerReceiptId = providerReceiptId;
   }



   /*******************************************************************************
    ** Fluent setter for providerReceiptId
    *******************************************************************************/
   public MarketingExecution withProviderReceiptId(String providerReceiptId)
   {
      this.providerReceiptId = providerReceiptId;
      return (this);
   }


   /*******************************************************************************
    ** Getter for statusDetail
    *******************************************************************************/
   public String getStatusDetail()
   {
      return (this.statusDetail);
   }



   /*******************************************************************************
    ** Setter for statusDetail
    *******************************************************************************/
   public void setStatusDetail(String statusDetail)
   {
      this.statusDetail = statusDetail;
   }



   /*******************************************************************************
    ** Fluent setter for statusDetail
    *******************************************************************************/
   public MarketingExecution withStatusDetail(String statusDetail)
   {
      this.statusDetail = statusDetail;
      return (this);
   }


   /*******************************************************************************
    ** Getter for requestedByUserId
    *******************************************************************************/
   public String getRequestedByUserId()
   {
      return (this.requestedByUserId);
   }



   /*******************************************************************************
    ** Setter for requestedByUserId
    *******************************************************************************/
   public void setRequestedByUserId(String requestedByUserId)
   {
      this.requestedByUserId = requestedByUserId;
   }



   /*******************************************************************************
    ** Fluent setter for requestedByUserId
    *******************************************************************************/
   public MarketingExecution withRequestedByUserId(String requestedByUserId)
   {
      this.requestedByUserId = requestedByUserId;
      return (this);
   }


   /*******************************************************************************
    ** Getter for isTestData
    *******************************************************************************/
   public Boolean getIsTestData()
   {
      return (this.isTestData);
   }



   /*******************************************************************************
    ** Setter for isTestData
    *******************************************************************************/
   public void setIsTestData(Boolean isTestData)
   {
      this.isTestData = isTestData;
   }



   /*******************************************************************************
    ** Fluent setter for isTestData
    *******************************************************************************/
   public MarketingExecution withIsTestData(Boolean isTestData)
   {
      this.isTestData = isTestData;
      return (this);
   }


   /*******************************************************************************
    ** Getter for leaseOwner
    *******************************************************************************/
   public String getLeaseOwner()
   {
      return (this.leaseOwner);
   }



   /*******************************************************************************
    ** Setter for leaseOwner
    *******************************************************************************/
   public void setLeaseOwner(String leaseOwner)
   {
      this.leaseOwner = leaseOwner;
   }



   /*******************************************************************************
    ** Fluent setter for leaseOwner
    *******************************************************************************/
   public MarketingExecution withLeaseOwner(String leaseOwner)
   {
      this.leaseOwner = leaseOwner;
      return (this);
   }


   /*******************************************************************************
    ** Getter for leaseExpiresAt
    *******************************************************************************/
   public Instant getLeaseExpiresAt()
   {
      return (this.leaseExpiresAt);
   }



   /*******************************************************************************
    ** Setter for leaseExpiresAt
    *******************************************************************************/
   public void setLeaseExpiresAt(Instant leaseExpiresAt)
   {
      this.leaseExpiresAt = leaseExpiresAt;
   }



   /*******************************************************************************
    ** Fluent setter for leaseExpiresAt
    *******************************************************************************/
   public MarketingExecution withLeaseExpiresAt(Instant leaseExpiresAt)
   {
      this.leaseExpiresAt = leaseExpiresAt;
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
   public MarketingExecution withCreateDate(Instant createDate)
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
   public MarketingExecution withModifyDate(Instant modifyDate)
   {
      this.modifyDate = modifyDate;
      return (this);
   }

}
