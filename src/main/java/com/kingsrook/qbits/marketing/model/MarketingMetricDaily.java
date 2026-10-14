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
import java.time.LocalDate;
import com.kingsrook.qbits.marketing.model.enums.FunnelStage;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.data.QField;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;
import com.kingsrook.qqq.backend.core.model.metadata.fields.ValueTooLongBehavior;


/*******************************************************************************
 ** A persisted daily rollup of a campaign's outcome events per stage and event type (system-written, recomputable from the raw events).
 ** Table marketing_metric_daily.
 *******************************************************************************/
public class MarketingMetricDaily extends QRecordEntity
{
   public static final String TABLE_NAME = "marketing_metric_daily";

   @QField(isEditable = false, isPrimaryKey = true)
   private Integer id;

   @QField(isRequired = true, possibleValueSourceName = MarketingCampaign.TABLE_NAME)
   private Integer campaignId;

   @QField(possibleValueSourceName = MarketingProduct.TABLE_NAME)
   private Integer productId;

   @QField(isRequired = true)
   private LocalDate dayUtc;

   @QField(isRequired = true, possibleValueSourceName = FunnelStage.NAME)
   private Integer funnelStage;

   @QField(maxLength = 100, valueTooLongBehavior = ValueTooLongBehavior.ERROR, isRequired = true)
   private String eventType;

   @QField(isRequired = true)
   private Boolean isTestData;

   @QField()
   private Integer rawCount;

   @QField()
   private Integer uniqueActors;

   @QField()
   private Integer eventsWithoutActor;

   @QField()
   private Instant recomputedAt;

   @QField(isEditable = false)
   private Instant createDate;

   @QField(isEditable = false)
   private Instant modifyDate;



   /*******************************************************************************
    ** Default constructor.
    *******************************************************************************/
   public MarketingMetricDaily()
   {
   }



   /*******************************************************************************
    ** Constructor that populates from a QRecord.
    *******************************************************************************/
   public MarketingMetricDaily(QRecord record) throws QException
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
   public MarketingMetricDaily withId(Integer id)
   {
      this.id = id;
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
   public MarketingMetricDaily withCampaignId(Integer campaignId)
   {
      this.campaignId = campaignId;
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
   public MarketingMetricDaily withProductId(Integer productId)
   {
      this.productId = productId;
      return (this);
   }


   /*******************************************************************************
    ** Getter for dayUtc
    *******************************************************************************/
   public LocalDate getDayUtc()
   {
      return (this.dayUtc);
   }



   /*******************************************************************************
    ** Setter for dayUtc
    *******************************************************************************/
   public void setDayUtc(LocalDate dayUtc)
   {
      this.dayUtc = dayUtc;
   }



   /*******************************************************************************
    ** Fluent setter for dayUtc
    *******************************************************************************/
   public MarketingMetricDaily withDayUtc(LocalDate dayUtc)
   {
      this.dayUtc = dayUtc;
      return (this);
   }


   /*******************************************************************************
    ** Getter for funnelStage
    *******************************************************************************/
   public Integer getFunnelStage()
   {
      return (this.funnelStage);
   }



   /*******************************************************************************
    ** Setter for funnelStage
    *******************************************************************************/
   public void setFunnelStage(Integer funnelStage)
   {
      this.funnelStage = funnelStage;
   }



   /*******************************************************************************
    ** Fluent setter for funnelStage
    *******************************************************************************/
   public MarketingMetricDaily withFunnelStage(Integer funnelStage)
   {
      this.funnelStage = funnelStage;
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
   public MarketingMetricDaily withEventType(String eventType)
   {
      this.eventType = eventType;
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
   public MarketingMetricDaily withIsTestData(Boolean isTestData)
   {
      this.isTestData = isTestData;
      return (this);
   }


   /*******************************************************************************
    ** Getter for rawCount
    *******************************************************************************/
   public Integer getRawCount()
   {
      return (this.rawCount);
   }



   /*******************************************************************************
    ** Setter for rawCount
    *******************************************************************************/
   public void setRawCount(Integer rawCount)
   {
      this.rawCount = rawCount;
   }



   /*******************************************************************************
    ** Fluent setter for rawCount
    *******************************************************************************/
   public MarketingMetricDaily withRawCount(Integer rawCount)
   {
      this.rawCount = rawCount;
      return (this);
   }


   /*******************************************************************************
    ** Getter for uniqueActors
    *******************************************************************************/
   public Integer getUniqueActors()
   {
      return (this.uniqueActors);
   }



   /*******************************************************************************
    ** Setter for uniqueActors
    *******************************************************************************/
   public void setUniqueActors(Integer uniqueActors)
   {
      this.uniqueActors = uniqueActors;
   }



   /*******************************************************************************
    ** Fluent setter for uniqueActors
    *******************************************************************************/
   public MarketingMetricDaily withUniqueActors(Integer uniqueActors)
   {
      this.uniqueActors = uniqueActors;
      return (this);
   }


   /*******************************************************************************
    ** Getter for eventsWithoutActor
    *******************************************************************************/
   public Integer getEventsWithoutActor()
   {
      return (this.eventsWithoutActor);
   }



   /*******************************************************************************
    ** Setter for eventsWithoutActor
    *******************************************************************************/
   public void setEventsWithoutActor(Integer eventsWithoutActor)
   {
      this.eventsWithoutActor = eventsWithoutActor;
   }



   /*******************************************************************************
    ** Fluent setter for eventsWithoutActor
    *******************************************************************************/
   public MarketingMetricDaily withEventsWithoutActor(Integer eventsWithoutActor)
   {
      this.eventsWithoutActor = eventsWithoutActor;
      return (this);
   }


   /*******************************************************************************
    ** Getter for recomputedAt
    *******************************************************************************/
   public Instant getRecomputedAt()
   {
      return (this.recomputedAt);
   }



   /*******************************************************************************
    ** Setter for recomputedAt
    *******************************************************************************/
   public void setRecomputedAt(Instant recomputedAt)
   {
      this.recomputedAt = recomputedAt;
   }



   /*******************************************************************************
    ** Fluent setter for recomputedAt
    *******************************************************************************/
   public MarketingMetricDaily withRecomputedAt(Instant recomputedAt)
   {
      this.recomputedAt = recomputedAt;
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
   public MarketingMetricDaily withCreateDate(Instant createDate)
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
   public MarketingMetricDaily withModifyDate(Instant modifyDate)
   {
      this.modifyDate = modifyDate;
      return (this);
   }

}
