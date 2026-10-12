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
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.data.QField;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;
import com.kingsrook.qqq.backend.core.model.metadata.fields.ValueTooLongBehavior;


/*******************************************************************************
 ** Runtime switches and limits: agent and execution kill switches, provider, model, budgets, concurrency.
 ** Table marketing_policy.
 *******************************************************************************/
public class MarketingPolicy extends QRecordEntity
{
   public static final String TABLE_NAME = "marketing_policy";

   @QField(isEditable = false, isPrimaryKey = true)
   private Integer id;

   @QField(maxLength = 64, valueTooLongBehavior = ValueTooLongBehavior.ERROR, isRequired = true)
   private String name;

   @QField()
   private Boolean agentEnabled;

   @QField()
   private Boolean executionEnabled;

   @QField(maxLength = 255, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String killSwitchReason;

   @QField(maxLength = 64, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String providerName;

   @QField(maxLength = 100, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String model;

   @QField()
   private Long dailyTokenLimit;

   @QField()
   private Long monthlyCostLimitMicros;

   @QField()
   private Integer maxConcurrentAgentRuns;

   @QField()
   private Integer maxToolCallsPerRun;

   @QField(isEditable = false)
   private Instant createDate;

   @QField(isEditable = false)
   private Instant modifyDate;



   /*******************************************************************************
    ** Default constructor.
    *******************************************************************************/
   public MarketingPolicy()
   {
   }



   /*******************************************************************************
    ** Constructor that populates from a QRecord.
    *******************************************************************************/
   public MarketingPolicy(QRecord record) throws QException
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
   public MarketingPolicy withId(Integer id)
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
   public MarketingPolicy withName(String name)
   {
      this.name = name;
      return (this);
   }


   /*******************************************************************************
    ** Getter for agentEnabled
    *******************************************************************************/
   public Boolean getAgentEnabled()
   {
      return (this.agentEnabled);
   }



   /*******************************************************************************
    ** Setter for agentEnabled
    *******************************************************************************/
   public void setAgentEnabled(Boolean agentEnabled)
   {
      this.agentEnabled = agentEnabled;
   }



   /*******************************************************************************
    ** Fluent setter for agentEnabled
    *******************************************************************************/
   public MarketingPolicy withAgentEnabled(Boolean agentEnabled)
   {
      this.agentEnabled = agentEnabled;
      return (this);
   }


   /*******************************************************************************
    ** Getter for executionEnabled
    *******************************************************************************/
   public Boolean getExecutionEnabled()
   {
      return (this.executionEnabled);
   }



   /*******************************************************************************
    ** Setter for executionEnabled
    *******************************************************************************/
   public void setExecutionEnabled(Boolean executionEnabled)
   {
      this.executionEnabled = executionEnabled;
   }



   /*******************************************************************************
    ** Fluent setter for executionEnabled
    *******************************************************************************/
   public MarketingPolicy withExecutionEnabled(Boolean executionEnabled)
   {
      this.executionEnabled = executionEnabled;
      return (this);
   }


   /*******************************************************************************
    ** Getter for killSwitchReason
    *******************************************************************************/
   public String getKillSwitchReason()
   {
      return (this.killSwitchReason);
   }



   /*******************************************************************************
    ** Setter for killSwitchReason
    *******************************************************************************/
   public void setKillSwitchReason(String killSwitchReason)
   {
      this.killSwitchReason = killSwitchReason;
   }



   /*******************************************************************************
    ** Fluent setter for killSwitchReason
    *******************************************************************************/
   public MarketingPolicy withKillSwitchReason(String killSwitchReason)
   {
      this.killSwitchReason = killSwitchReason;
      return (this);
   }


   /*******************************************************************************
    ** Getter for providerName
    *******************************************************************************/
   public String getProviderName()
   {
      return (this.providerName);
   }



   /*******************************************************************************
    ** Setter for providerName
    *******************************************************************************/
   public void setProviderName(String providerName)
   {
      this.providerName = providerName;
   }



   /*******************************************************************************
    ** Fluent setter for providerName
    *******************************************************************************/
   public MarketingPolicy withProviderName(String providerName)
   {
      this.providerName = providerName;
      return (this);
   }


   /*******************************************************************************
    ** Getter for model
    *******************************************************************************/
   public String getModel()
   {
      return (this.model);
   }



   /*******************************************************************************
    ** Setter for model
    *******************************************************************************/
   public void setModel(String model)
   {
      this.model = model;
   }



   /*******************************************************************************
    ** Fluent setter for model
    *******************************************************************************/
   public MarketingPolicy withModel(String model)
   {
      this.model = model;
      return (this);
   }


   /*******************************************************************************
    ** Getter for dailyTokenLimit
    *******************************************************************************/
   public Long getDailyTokenLimit()
   {
      return (this.dailyTokenLimit);
   }



   /*******************************************************************************
    ** Setter for dailyTokenLimit
    *******************************************************************************/
   public void setDailyTokenLimit(Long dailyTokenLimit)
   {
      this.dailyTokenLimit = dailyTokenLimit;
   }



   /*******************************************************************************
    ** Fluent setter for dailyTokenLimit
    *******************************************************************************/
   public MarketingPolicy withDailyTokenLimit(Long dailyTokenLimit)
   {
      this.dailyTokenLimit = dailyTokenLimit;
      return (this);
   }


   /*******************************************************************************
    ** Getter for monthlyCostLimitMicros
    *******************************************************************************/
   public Long getMonthlyCostLimitMicros()
   {
      return (this.monthlyCostLimitMicros);
   }



   /*******************************************************************************
    ** Setter for monthlyCostLimitMicros
    *******************************************************************************/
   public void setMonthlyCostLimitMicros(Long monthlyCostLimitMicros)
   {
      this.monthlyCostLimitMicros = monthlyCostLimitMicros;
   }



   /*******************************************************************************
    ** Fluent setter for monthlyCostLimitMicros
    *******************************************************************************/
   public MarketingPolicy withMonthlyCostLimitMicros(Long monthlyCostLimitMicros)
   {
      this.monthlyCostLimitMicros = monthlyCostLimitMicros;
      return (this);
   }


   /*******************************************************************************
    ** Getter for maxConcurrentAgentRuns
    *******************************************************************************/
   public Integer getMaxConcurrentAgentRuns()
   {
      return (this.maxConcurrentAgentRuns);
   }



   /*******************************************************************************
    ** Setter for maxConcurrentAgentRuns
    *******************************************************************************/
   public void setMaxConcurrentAgentRuns(Integer maxConcurrentAgentRuns)
   {
      this.maxConcurrentAgentRuns = maxConcurrentAgentRuns;
   }



   /*******************************************************************************
    ** Fluent setter for maxConcurrentAgentRuns
    *******************************************************************************/
   public MarketingPolicy withMaxConcurrentAgentRuns(Integer maxConcurrentAgentRuns)
   {
      this.maxConcurrentAgentRuns = maxConcurrentAgentRuns;
      return (this);
   }


   /*******************************************************************************
    ** Getter for maxToolCallsPerRun
    *******************************************************************************/
   public Integer getMaxToolCallsPerRun()
   {
      return (this.maxToolCallsPerRun);
   }



   /*******************************************************************************
    ** Setter for maxToolCallsPerRun
    *******************************************************************************/
   public void setMaxToolCallsPerRun(Integer maxToolCallsPerRun)
   {
      this.maxToolCallsPerRun = maxToolCallsPerRun;
   }



   /*******************************************************************************
    ** Fluent setter for maxToolCallsPerRun
    *******************************************************************************/
   public MarketingPolicy withMaxToolCallsPerRun(Integer maxToolCallsPerRun)
   {
      this.maxToolCallsPerRun = maxToolCallsPerRun;
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
   public MarketingPolicy withCreateDate(Instant createDate)
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
   public MarketingPolicy withModifyDate(Instant modifyDate)
   {
      this.modifyDate = modifyDate;
      return (this);
   }

}
