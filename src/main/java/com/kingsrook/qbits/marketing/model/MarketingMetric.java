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
import com.kingsrook.qbits.marketing.model.enums.CountMode;
import com.kingsrook.qbits.marketing.model.enums.FunnelStage;
import com.kingsrook.qbits.marketing.model.enums.InstrumentationStatus;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.data.QField;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;
import com.kingsrook.qqq.backend.core.model.metadata.fields.ValueTooLongBehavior;


/*******************************************************************************
 ** A campaign metric: its stage, the event it counts, how it counts and whether it is instrumented.
 ** Table marketing_metric.
 *******************************************************************************/
public class MarketingMetric extends QRecordEntity
{
   public static final String TABLE_NAME = "marketing_metric";

   @QField(isEditable = false, isPrimaryKey = true)
   private Integer id;

   @QField(possibleValueSourceName = MarketingProduct.TABLE_NAME)
   private Integer productId;

   @QField(isRequired = true, possibleValueSourceName = MarketingCampaign.TABLE_NAME)
   private Integer campaignId;

   @QField(isRequired = true, possibleValueSourceName = FunnelStage.NAME)
   private Integer stage;

   @QField(maxLength = 150, valueTooLongBehavior = ValueTooLongBehavior.ERROR, isRequired = true)
   private String name;

   @QField(maxLength = 100, valueTooLongBehavior = ValueTooLongBehavior.ERROR, isRequired = true)
   private String eventType;

   @QField(isRequired = true, possibleValueSourceName = CountMode.NAME)
   private Integer countMode;

   @QField(isRequired = true, possibleValueSourceName = InstrumentationStatus.NAME)
   private Integer instrumentationStatus;

   @QField()
   private Integer target;

   @QField()
   private String notes;

   @QField(isEditable = false)
   private Instant createDate;

   @QField(isEditable = false)
   private Instant modifyDate;



   /*******************************************************************************
    ** Default constructor.
    *******************************************************************************/
   public MarketingMetric()
   {
   }



   /*******************************************************************************
    ** Constructor that populates from a QRecord.
    *******************************************************************************/
   public MarketingMetric(QRecord record) throws QException
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
   public MarketingMetric withId(Integer id)
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
   public MarketingMetric withProductId(Integer productId)
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
   public MarketingMetric withCampaignId(Integer campaignId)
   {
      this.campaignId = campaignId;
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
   public MarketingMetric withStage(Integer stage)
   {
      this.stage = stage;
      return (this);
   }


   /*******************************************************************************
    ** Getter for name
    *******************************************************************************/
   public String getName()
   {
      return (this.name);
   }



   /*******************************************************************************
    ** Setter for name
    *******************************************************************************/
   public void setName(String name)
   {
      this.name = name;
   }



   /*******************************************************************************
    ** Fluent setter for name
    *******************************************************************************/
   public MarketingMetric withName(String name)
   {
      this.name = name;
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
   public MarketingMetric withEventType(String eventType)
   {
      this.eventType = eventType;
      return (this);
   }


   /*******************************************************************************
    ** Getter for countMode
    *******************************************************************************/
   public Integer getCountMode()
   {
      return (this.countMode);
   }



   /*******************************************************************************
    ** Setter for countMode
    *******************************************************************************/
   public void setCountMode(Integer countMode)
   {
      this.countMode = countMode;
   }



   /*******************************************************************************
    ** Fluent setter for countMode
    *******************************************************************************/
   public MarketingMetric withCountMode(Integer countMode)
   {
      this.countMode = countMode;
      return (this);
   }


   /*******************************************************************************
    ** Getter for instrumentationStatus
    *******************************************************************************/
   public Integer getInstrumentationStatus()
   {
      return (this.instrumentationStatus);
   }



   /*******************************************************************************
    ** Setter for instrumentationStatus
    *******************************************************************************/
   public void setInstrumentationStatus(Integer instrumentationStatus)
   {
      this.instrumentationStatus = instrumentationStatus;
   }



   /*******************************************************************************
    ** Fluent setter for instrumentationStatus
    *******************************************************************************/
   public MarketingMetric withInstrumentationStatus(Integer instrumentationStatus)
   {
      this.instrumentationStatus = instrumentationStatus;
      return (this);
   }


   /*******************************************************************************
    ** Getter for target
    *******************************************************************************/
   public Integer getTarget()
   {
      return (this.target);
   }



   /*******************************************************************************
    ** Setter for target
    *******************************************************************************/
   public void setTarget(Integer target)
   {
      this.target = target;
   }



   /*******************************************************************************
    ** Fluent setter for target
    *******************************************************************************/
   public MarketingMetric withTarget(Integer target)
   {
      this.target = target;
      return (this);
   }


   /*******************************************************************************
    ** Getter for notes
    *******************************************************************************/
   public String getNotes()
   {
      return (this.notes);
   }



   /*******************************************************************************
    ** Setter for notes
    *******************************************************************************/
   public void setNotes(String notes)
   {
      this.notes = notes;
   }



   /*******************************************************************************
    ** Fluent setter for notes
    *******************************************************************************/
   public MarketingMetric withNotes(String notes)
   {
      this.notes = notes;
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
   public MarketingMetric withCreateDate(Instant createDate)
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
   public MarketingMetric withModifyDate(Instant modifyDate)
   {
      this.modifyDate = modifyDate;
      return (this);
   }

}
