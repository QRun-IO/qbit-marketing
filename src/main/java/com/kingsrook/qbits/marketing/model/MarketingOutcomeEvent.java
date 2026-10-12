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
import com.kingsrook.qbits.marketing.model.enums.FunnelStage;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.data.QField;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;
import com.kingsrook.qqq.backend.core.model.metadata.fields.ValueTooLongBehavior;


/*******************************************************************************
 ** A raw outcome event (deduplicated by source and external id), with its funnel stage and anonymous actor.
 ** Table marketing_outcome_event.
 *******************************************************************************/
public class MarketingOutcomeEvent extends QRecordEntity
{
   public static final String TABLE_NAME = "marketing_outcome_event";

   @QField(isEditable = false, isPrimaryKey = true)
   private Integer id;

   @QField(possibleValueSourceName = MarketingProduct.TABLE_NAME)
   private Integer productId;

   @QField(possibleValueSourceName = MarketingCampaign.TABLE_NAME)
   private Integer campaignId;

   @QField(possibleValueSourceName = MarketingContent.TABLE_NAME)
   private Integer contentId;

   @QField(possibleValueSourceName = MarketingExecution.TABLE_NAME)
   private Integer executionId;

   @QField(isRequired = true, possibleValueSourceName = FunnelStage.NAME)
   private Integer stage;

   @QField(maxLength = 100, valueTooLongBehavior = ValueTooLongBehavior.ERROR, isRequired = true)
   private String eventType;

   @QField(maxLength = 100, valueTooLongBehavior = ValueTooLongBehavior.ERROR, isRequired = true)
   private String source;

   @QField(maxLength = 200, valueTooLongBehavior = ValueTooLongBehavior.ERROR, isRequired = true)
   private String externalEventId;

   @QField(maxLength = 200, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String actorKey;

   @QField()
   private Instant occurredAt;

   @QField()
   private Integer quantity;

   @QField()
   private Boolean isTestData;

   @QField(maxLength = 500, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String note;

   @QField(isEditable = false)
   private Instant createDate;

   @QField(isEditable = false)
   private Instant modifyDate;



   /*******************************************************************************
    ** Default constructor.
    *******************************************************************************/
   public MarketingOutcomeEvent()
   {
   }



   /*******************************************************************************
    ** Constructor that populates from a QRecord.
    *******************************************************************************/
   public MarketingOutcomeEvent(QRecord record) throws QException
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
   public MarketingOutcomeEvent withId(Integer id)
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
   public MarketingOutcomeEvent withProductId(Integer productId)
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
   public MarketingOutcomeEvent withCampaignId(Integer campaignId)
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
   public MarketingOutcomeEvent withContentId(Integer contentId)
   {
      this.contentId = contentId;
      return (this);
   }


   /*******************************************************************************
    ** Getter for executionId
    *******************************************************************************/
   public Integer getExecutionId()
   {
      return (this.executionId);
   }



   /*******************************************************************************
    ** Setter for executionId
    *******************************************************************************/
   public void setExecutionId(Integer executionId)
   {
      this.executionId = executionId;
   }



   /*******************************************************************************
    ** Fluent setter for executionId
    *******************************************************************************/
   public MarketingOutcomeEvent withExecutionId(Integer executionId)
   {
      this.executionId = executionId;
      return (this);
   }


   /*******************************************************************************
    ** Getter for stage
    *******************************************************************************/
   public Integer getStage()
   {
      return (this.stage);
   }



   /*******************************************************************************
    ** Setter for stage
    *******************************************************************************/
   public void setStage(Integer stage)
   {
      this.stage = stage;
   }



   /*******************************************************************************
    ** Fluent setter for stage
    *******************************************************************************/
   public MarketingOutcomeEvent withStage(Integer stage)
   {
      this.stage = stage;
      return (this);
   }


   /*******************************************************************************
    ** Getter for eventType
    *******************************************************************************/
   public String getEventType()
   {
      return (this.eventType);
   }



   /*******************************************************************************
    ** Setter for eventType
    *******************************************************************************/
   public void setEventType(String eventType)
   {
      this.eventType = eventType;
   }



   /*******************************************************************************
    ** Fluent setter for eventType
    *******************************************************************************/
   public MarketingOutcomeEvent withEventType(String eventType)
   {
      this.eventType = eventType;
      return (this);
   }


   /*******************************************************************************
    ** Getter for source
    *******************************************************************************/
   public String getSource()
   {
      return (this.source);
   }



   /*******************************************************************************
    ** Setter for source
    *******************************************************************************/
   public void setSource(String source)
   {
      this.source = source;
   }



   /*******************************************************************************
    ** Fluent setter for source
    *******************************************************************************/
   public MarketingOutcomeEvent withSource(String source)
   {
      this.source = source;
      return (this);
   }


   /*******************************************************************************
    ** Getter for externalEventId
    *******************************************************************************/
   public String getExternalEventId()
   {
      return (this.externalEventId);
   }



   /*******************************************************************************
    ** Setter for externalEventId
    *******************************************************************************/
   public void setExternalEventId(String externalEventId)
   {
      this.externalEventId = externalEventId;
   }



   /*******************************************************************************
    ** Fluent setter for externalEventId
    *******************************************************************************/
   public MarketingOutcomeEvent withExternalEventId(String externalEventId)
   {
      this.externalEventId = externalEventId;
      return (this);
   }


   /*******************************************************************************
    ** Getter for actorKey
    *******************************************************************************/
   public String getActorKey()
   {
      return (this.actorKey);
   }



   /*******************************************************************************
    ** Setter for actorKey
    *******************************************************************************/
   public void setActorKey(String actorKey)
   {
      this.actorKey = actorKey;
   }



   /*******************************************************************************
    ** Fluent setter for actorKey
    *******************************************************************************/
   public MarketingOutcomeEvent withActorKey(String actorKey)
   {
      this.actorKey = actorKey;
      return (this);
   }


   /*******************************************************************************
    ** Getter for occurredAt
    *******************************************************************************/
   public Instant getOccurredAt()
   {
      return (this.occurredAt);
   }



   /*******************************************************************************
    ** Setter for occurredAt
    *******************************************************************************/
   public void setOccurredAt(Instant occurredAt)
   {
      this.occurredAt = occurredAt;
   }



   /*******************************************************************************
    ** Fluent setter for occurredAt
    *******************************************************************************/
   public MarketingOutcomeEvent withOccurredAt(Instant occurredAt)
   {
      this.occurredAt = occurredAt;
      return (this);
   }


   /*******************************************************************************
    ** Getter for quantity
    *******************************************************************************/
   public Integer getQuantity()
   {
      return (this.quantity);
   }



   /*******************************************************************************
    ** Setter for quantity
    *******************************************************************************/
   public void setQuantity(Integer quantity)
   {
      this.quantity = quantity;
   }



   /*******************************************************************************
    ** Fluent setter for quantity
    *******************************************************************************/
   public MarketingOutcomeEvent withQuantity(Integer quantity)
   {
      this.quantity = quantity;
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
   public MarketingOutcomeEvent withIsTestData(Boolean isTestData)
   {
      this.isTestData = isTestData;
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
   public MarketingOutcomeEvent withNote(String note)
   {
      this.note = note;
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
   public MarketingOutcomeEvent withCreateDate(Instant createDate)
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
   public MarketingOutcomeEvent withModifyDate(Instant modifyDate)
   {
      this.modifyDate = modifyDate;
      return (this);
   }

}
