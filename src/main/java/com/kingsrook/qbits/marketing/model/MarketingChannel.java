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
import com.kingsrook.qbits.marketing.model.enums.AiContentPolicy;
import com.kingsrook.qbits.marketing.model.enums.ChannelType;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.data.QField;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;
import com.kingsrook.qqq.backend.core.model.metadata.fields.ValueTooLongBehavior;


/*******************************************************************************
 ** A channel content can go to, its transport and the rules it must respect.
 ** Table marketing_channel.
 *******************************************************************************/
public class MarketingChannel extends QRecordEntity
{
   public static final String TABLE_NAME = "marketing_channel";

   @QField(isEditable = false, isPrimaryKey = true)
   private Integer id;

   @QField(maxLength = 100, valueTooLongBehavior = ValueTooLongBehavior.ERROR, isRequired = true)
   private String name;

   @QField(isRequired = true, possibleValueSourceName = ChannelType.NAME)
   private Integer channelType;

   @QField(maxLength = 64, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String transportName;

   @QField()
   private Boolean isActive;

   @QField()
   private Boolean allowsAutomation;

   @QField(isRequired = true, possibleValueSourceName = AiContentPolicy.NAME)
   private Integer aiContentPolicy;

   @QField()
   private String rulesSummary;

   @QField(maxLength = 1000, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String rulesSourceUrl;

   @QField()
   private LocalDate rulesVerifiedDate;

   @QField()
   private Integer rateLimitPerHour;

   @QField()
   private Integer maxRecipientsPerExecution;

   @QField(isEditable = false)
   private Instant createDate;

   @QField(isEditable = false)
   private Instant modifyDate;



   /*******************************************************************************
    ** Default constructor.
    *******************************************************************************/
   public MarketingChannel()
   {
   }



   /*******************************************************************************
    ** Constructor that populates from a QRecord.
    *******************************************************************************/
   public MarketingChannel(QRecord record) throws QException
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
   public MarketingChannel withId(Integer id)
   {
      this.id = id;
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
   public MarketingChannel withName(String name)
   {
      this.name = name;
      return (this);
   }


   /*******************************************************************************
    ** Getter for channelType
    *******************************************************************************/
   public Integer getChannelType()
   {
      return (this.channelType);
   }



   /*******************************************************************************
    ** Setter for channelType
    *******************************************************************************/
   public void setChannelType(Integer channelType)
   {
      this.channelType = channelType;
   }



   /*******************************************************************************
    ** Fluent setter for channelType
    *******************************************************************************/
   public MarketingChannel withChannelType(Integer channelType)
   {
      this.channelType = channelType;
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
   public MarketingChannel withTransportName(String transportName)
   {
      this.transportName = transportName;
      return (this);
   }


   /*******************************************************************************
    ** Getter for isActive
    *******************************************************************************/
   public Boolean getIsActive()
   {
      return (this.isActive);
   }



   /*******************************************************************************
    ** Setter for isActive
    *******************************************************************************/
   public void setIsActive(Boolean isActive)
   {
      this.isActive = isActive;
   }



   /*******************************************************************************
    ** Fluent setter for isActive
    *******************************************************************************/
   public MarketingChannel withIsActive(Boolean isActive)
   {
      this.isActive = isActive;
      return (this);
   }


   /*******************************************************************************
    ** Getter for allowsAutomation
    *******************************************************************************/
   public Boolean getAllowsAutomation()
   {
      return (this.allowsAutomation);
   }



   /*******************************************************************************
    ** Setter for allowsAutomation
    *******************************************************************************/
   public void setAllowsAutomation(Boolean allowsAutomation)
   {
      this.allowsAutomation = allowsAutomation;
   }



   /*******************************************************************************
    ** Fluent setter for allowsAutomation
    *******************************************************************************/
   public MarketingChannel withAllowsAutomation(Boolean allowsAutomation)
   {
      this.allowsAutomation = allowsAutomation;
      return (this);
   }


   /*******************************************************************************
    ** Getter for aiContentPolicy
    *******************************************************************************/
   public Integer getAiContentPolicy()
   {
      return (this.aiContentPolicy);
   }



   /*******************************************************************************
    ** Setter for aiContentPolicy
    *******************************************************************************/
   public void setAiContentPolicy(Integer aiContentPolicy)
   {
      this.aiContentPolicy = aiContentPolicy;
   }



   /*******************************************************************************
    ** Fluent setter for aiContentPolicy
    *******************************************************************************/
   public MarketingChannel withAiContentPolicy(Integer aiContentPolicy)
   {
      this.aiContentPolicy = aiContentPolicy;
      return (this);
   }


   /*******************************************************************************
    ** Getter for rulesSummary
    *******************************************************************************/
   public String getRulesSummary()
   {
      return (this.rulesSummary);
   }



   /*******************************************************************************
    ** Setter for rulesSummary
    *******************************************************************************/
   public void setRulesSummary(String rulesSummary)
   {
      this.rulesSummary = rulesSummary;
   }



   /*******************************************************************************
    ** Fluent setter for rulesSummary
    *******************************************************************************/
   public MarketingChannel withRulesSummary(String rulesSummary)
   {
      this.rulesSummary = rulesSummary;
      return (this);
   }


   /*******************************************************************************
    ** Getter for rulesSourceUrl
    *******************************************************************************/
   public String getRulesSourceUrl()
   {
      return (this.rulesSourceUrl);
   }



   /*******************************************************************************
    ** Setter for rulesSourceUrl
    *******************************************************************************/
   public void setRulesSourceUrl(String rulesSourceUrl)
   {
      this.rulesSourceUrl = rulesSourceUrl;
   }



   /*******************************************************************************
    ** Fluent setter for rulesSourceUrl
    *******************************************************************************/
   public MarketingChannel withRulesSourceUrl(String rulesSourceUrl)
   {
      this.rulesSourceUrl = rulesSourceUrl;
      return (this);
   }


   /*******************************************************************************
    ** Getter for rulesVerifiedDate
    *******************************************************************************/
   public LocalDate getRulesVerifiedDate()
   {
      return (this.rulesVerifiedDate);
   }



   /*******************************************************************************
    ** Setter for rulesVerifiedDate
    *******************************************************************************/
   public void setRulesVerifiedDate(LocalDate rulesVerifiedDate)
   {
      this.rulesVerifiedDate = rulesVerifiedDate;
   }



   /*******************************************************************************
    ** Fluent setter for rulesVerifiedDate
    *******************************************************************************/
   public MarketingChannel withRulesVerifiedDate(LocalDate rulesVerifiedDate)
   {
      this.rulesVerifiedDate = rulesVerifiedDate;
      return (this);
   }


   /*******************************************************************************
    ** Getter for rateLimitPerHour
    *******************************************************************************/
   public Integer getRateLimitPerHour()
   {
      return (this.rateLimitPerHour);
   }



   /*******************************************************************************
    ** Setter for rateLimitPerHour
    *******************************************************************************/
   public void setRateLimitPerHour(Integer rateLimitPerHour)
   {
      this.rateLimitPerHour = rateLimitPerHour;
   }



   /*******************************************************************************
    ** Fluent setter for rateLimitPerHour
    *******************************************************************************/
   public MarketingChannel withRateLimitPerHour(Integer rateLimitPerHour)
   {
      this.rateLimitPerHour = rateLimitPerHour;
      return (this);
   }


   /*******************************************************************************
    ** Getter for maxRecipientsPerExecution
    *******************************************************************************/
   public Integer getMaxRecipientsPerExecution()
   {
      return (this.maxRecipientsPerExecution);
   }



   /*******************************************************************************
    ** Setter for maxRecipientsPerExecution
    *******************************************************************************/
   public void setMaxRecipientsPerExecution(Integer maxRecipientsPerExecution)
   {
      this.maxRecipientsPerExecution = maxRecipientsPerExecution;
   }



   /*******************************************************************************
    ** Fluent setter for maxRecipientsPerExecution
    *******************************************************************************/
   public MarketingChannel withMaxRecipientsPerExecution(Integer maxRecipientsPerExecution)
   {
      this.maxRecipientsPerExecution = maxRecipientsPerExecution;
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
   public MarketingChannel withCreateDate(Instant createDate)
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
   public MarketingChannel withModifyDate(Instant modifyDate)
   {
      this.modifyDate = modifyDate;
      return (this);
   }

}
