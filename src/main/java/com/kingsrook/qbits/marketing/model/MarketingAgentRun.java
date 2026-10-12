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
import com.kingsrook.qbits.marketing.model.enums.AgentRunStatus;
import com.kingsrook.qbits.marketing.model.enums.AgentTask;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.data.QField;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;
import com.kingsrook.qqq.backend.core.model.metadata.fields.ValueTooLongBehavior;


/*******************************************************************************
 ** A run of the marketing agent: task, status, provider, model usage and outputs.
 ** Table marketing_agent_run.
 *******************************************************************************/
public class MarketingAgentRun extends QRecordEntity
{
   public static final String TABLE_NAME = "marketing_agent_run";

   @QField(isEditable = false, isPrimaryKey = true)
   private Integer id;

   @QField(possibleValueSourceName = MarketingProduct.TABLE_NAME)
   private Integer productId;

   @QField(possibleValueSourceName = MarketingCampaign.TABLE_NAME)
   private Integer campaignId;

   @QField(possibleValueSourceName = MarketingContent.TABLE_NAME)
   private Integer contentId;

   @QField(isRequired = true, possibleValueSourceName = AgentTask.NAME)
   private Integer task;

   @QField(isRequired = true, possibleValueSourceName = AgentRunStatus.NAME)
   private Integer status;

   @QField()
   private String instructions;

   @QField(maxLength = 64, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String providerName;

   @QField(maxLength = 100, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String model;

   @QField(maxLength = 100, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String requestedByUserId;

   @QField()
   private Instant startedAt;

   @QField()
   private Instant finishedAt;

   @QField()
   private Integer modelCalls;

   @QField()
   private Integer toolCalls;

   @QField()
   private Long inputTokens;

   @QField()
   private Long outputTokens;

   @QField()
   private Long cacheReadTokens;

   @QField()
   private Long costMicros;

   @QField()
   private Integer draftsCreated;

   @QField()
   private Integer findingsCreated;

   @QField()
   private String summary;

   @QField()
   private String outputJson;

   @QField()
   private String errorMessage;

   @QField(isEditable = false)
   private Instant createDate;

   @QField(isEditable = false)
   private Instant modifyDate;



   /*******************************************************************************
    ** Default constructor.
    *******************************************************************************/
   public MarketingAgentRun()
   {
   }



   /*******************************************************************************
    ** Constructor that populates from a QRecord.
    *******************************************************************************/
   public MarketingAgentRun(QRecord record) throws QException
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
   public MarketingAgentRun withId(Integer id)
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
   public MarketingAgentRun withProductId(Integer productId)
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
   public MarketingAgentRun withCampaignId(Integer campaignId)
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
   public MarketingAgentRun withContentId(Integer contentId)
   {
      this.contentId = contentId;
      return (this);
   }


   /*******************************************************************************
    ** Getter for task
    *******************************************************************************/
   public Integer getTask()
   {
      return (this.task);
   }



   /*******************************************************************************
    ** Setter for task
    *******************************************************************************/
   public void setTask(Integer task)
   {
      this.task = task;
   }



   /*******************************************************************************
    ** Fluent setter for task
    *******************************************************************************/
   public MarketingAgentRun withTask(Integer task)
   {
      this.task = task;
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
   public MarketingAgentRun withStatus(Integer status)
   {
      this.status = status;
      return (this);
   }


   /*******************************************************************************
    ** Getter for instructions
    *******************************************************************************/
   public String getInstructions()
   {
      return (this.instructions);
   }



   /*******************************************************************************
    ** Setter for instructions
    *******************************************************************************/
   public void setInstructions(String instructions)
   {
      this.instructions = instructions;
   }



   /*******************************************************************************
    ** Fluent setter for instructions
    *******************************************************************************/
   public MarketingAgentRun withInstructions(String instructions)
   {
      this.instructions = instructions;
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
   public MarketingAgentRun withProviderName(String providerName)
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
   public MarketingAgentRun withModel(String model)
   {
      this.model = model;
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
   public MarketingAgentRun withRequestedByUserId(String requestedByUserId)
   {
      this.requestedByUserId = requestedByUserId;
      return (this);
   }


   /*******************************************************************************
    ** Getter for startedAt
    *******************************************************************************/
   public Instant getStartedAt()
   {
      return (this.startedAt);
   }



   /*******************************************************************************
    ** Setter for startedAt
    *******************************************************************************/
   public void setStartedAt(Instant startedAt)
   {
      this.startedAt = startedAt;
   }



   /*******************************************************************************
    ** Fluent setter for startedAt
    *******************************************************************************/
   public MarketingAgentRun withStartedAt(Instant startedAt)
   {
      this.startedAt = startedAt;
      return (this);
   }


   /*******************************************************************************
    ** Getter for finishedAt
    *******************************************************************************/
   public Instant getFinishedAt()
   {
      return (this.finishedAt);
   }



   /*******************************************************************************
    ** Setter for finishedAt
    *******************************************************************************/
   public void setFinishedAt(Instant finishedAt)
   {
      this.finishedAt = finishedAt;
   }



   /*******************************************************************************
    ** Fluent setter for finishedAt
    *******************************************************************************/
   public MarketingAgentRun withFinishedAt(Instant finishedAt)
   {
      this.finishedAt = finishedAt;
      return (this);
   }


   /*******************************************************************************
    ** Getter for modelCalls
    *******************************************************************************/
   public Integer getModelCalls()
   {
      return (this.modelCalls);
   }



   /*******************************************************************************
    ** Setter for modelCalls
    *******************************************************************************/
   public void setModelCalls(Integer modelCalls)
   {
      this.modelCalls = modelCalls;
   }



   /*******************************************************************************
    ** Fluent setter for modelCalls
    *******************************************************************************/
   public MarketingAgentRun withModelCalls(Integer modelCalls)
   {
      this.modelCalls = modelCalls;
      return (this);
   }


   /*******************************************************************************
    ** Getter for toolCalls
    *******************************************************************************/
   public Integer getToolCalls()
   {
      return (this.toolCalls);
   }



   /*******************************************************************************
    ** Setter for toolCalls
    *******************************************************************************/
   public void setToolCalls(Integer toolCalls)
   {
      this.toolCalls = toolCalls;
   }



   /*******************************************************************************
    ** Fluent setter for toolCalls
    *******************************************************************************/
   public MarketingAgentRun withToolCalls(Integer toolCalls)
   {
      this.toolCalls = toolCalls;
      return (this);
   }


   /*******************************************************************************
    ** Getter for inputTokens
    *******************************************************************************/
   public Long getInputTokens()
   {
      return (this.inputTokens);
   }



   /*******************************************************************************
    ** Setter for inputTokens
    *******************************************************************************/
   public void setInputTokens(Long inputTokens)
   {
      this.inputTokens = inputTokens;
   }



   /*******************************************************************************
    ** Fluent setter for inputTokens
    *******************************************************************************/
   public MarketingAgentRun withInputTokens(Long inputTokens)
   {
      this.inputTokens = inputTokens;
      return (this);
   }


   /*******************************************************************************
    ** Getter for outputTokens
    *******************************************************************************/
   public Long getOutputTokens()
   {
      return (this.outputTokens);
   }



   /*******************************************************************************
    ** Setter for outputTokens
    *******************************************************************************/
   public void setOutputTokens(Long outputTokens)
   {
      this.outputTokens = outputTokens;
   }



   /*******************************************************************************
    ** Fluent setter for outputTokens
    *******************************************************************************/
   public MarketingAgentRun withOutputTokens(Long outputTokens)
   {
      this.outputTokens = outputTokens;
      return (this);
   }


   /*******************************************************************************
    ** Getter for cacheReadTokens
    *******************************************************************************/
   public Long getCacheReadTokens()
   {
      return (this.cacheReadTokens);
   }



   /*******************************************************************************
    ** Setter for cacheReadTokens
    *******************************************************************************/
   public void setCacheReadTokens(Long cacheReadTokens)
   {
      this.cacheReadTokens = cacheReadTokens;
   }



   /*******************************************************************************
    ** Fluent setter for cacheReadTokens
    *******************************************************************************/
   public MarketingAgentRun withCacheReadTokens(Long cacheReadTokens)
   {
      this.cacheReadTokens = cacheReadTokens;
      return (this);
   }


   /*******************************************************************************
    ** Getter for costMicros
    *******************************************************************************/
   public Long getCostMicros()
   {
      return (this.costMicros);
   }



   /*******************************************************************************
    ** Setter for costMicros
    *******************************************************************************/
   public void setCostMicros(Long costMicros)
   {
      this.costMicros = costMicros;
   }



   /*******************************************************************************
    ** Fluent setter for costMicros
    *******************************************************************************/
   public MarketingAgentRun withCostMicros(Long costMicros)
   {
      this.costMicros = costMicros;
      return (this);
   }


   /*******************************************************************************
    ** Getter for draftsCreated
    *******************************************************************************/
   public Integer getDraftsCreated()
   {
      return (this.draftsCreated);
   }



   /*******************************************************************************
    ** Setter for draftsCreated
    *******************************************************************************/
   public void setDraftsCreated(Integer draftsCreated)
   {
      this.draftsCreated = draftsCreated;
   }



   /*******************************************************************************
    ** Fluent setter for draftsCreated
    *******************************************************************************/
   public MarketingAgentRun withDraftsCreated(Integer draftsCreated)
   {
      this.draftsCreated = draftsCreated;
      return (this);
   }


   /*******************************************************************************
    ** Getter for findingsCreated
    *******************************************************************************/
   public Integer getFindingsCreated()
   {
      return (this.findingsCreated);
   }



   /*******************************************************************************
    ** Setter for findingsCreated
    *******************************************************************************/
   public void setFindingsCreated(Integer findingsCreated)
   {
      this.findingsCreated = findingsCreated;
   }



   /*******************************************************************************
    ** Fluent setter for findingsCreated
    *******************************************************************************/
   public MarketingAgentRun withFindingsCreated(Integer findingsCreated)
   {
      this.findingsCreated = findingsCreated;
      return (this);
   }


   /*******************************************************************************
    ** Getter for summary
    *******************************************************************************/
   public String getSummary()
   {
      return (this.summary);
   }



   /*******************************************************************************
    ** Setter for summary
    *******************************************************************************/
   public void setSummary(String summary)
   {
      this.summary = summary;
   }



   /*******************************************************************************
    ** Fluent setter for summary
    *******************************************************************************/
   public MarketingAgentRun withSummary(String summary)
   {
      this.summary = summary;
      return (this);
   }


   /*******************************************************************************
    ** Getter for outputJson
    *******************************************************************************/
   public String getOutputJson()
   {
      return (this.outputJson);
   }



   /*******************************************************************************
    ** Setter for outputJson
    *******************************************************************************/
   public void setOutputJson(String outputJson)
   {
      this.outputJson = outputJson;
   }



   /*******************************************************************************
    ** Fluent setter for outputJson
    *******************************************************************************/
   public MarketingAgentRun withOutputJson(String outputJson)
   {
      this.outputJson = outputJson;
      return (this);
   }


   /*******************************************************************************
    ** Getter for errorMessage
    *******************************************************************************/
   public String getErrorMessage()
   {
      return (this.errorMessage);
   }



   /*******************************************************************************
    ** Setter for errorMessage
    *******************************************************************************/
   public void setErrorMessage(String errorMessage)
   {
      this.errorMessage = errorMessage;
   }



   /*******************************************************************************
    ** Fluent setter for errorMessage
    *******************************************************************************/
   public MarketingAgentRun withErrorMessage(String errorMessage)
   {
      this.errorMessage = errorMessage;
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
   public MarketingAgentRun withCreateDate(Instant createDate)
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
   public MarketingAgentRun withModifyDate(Instant modifyDate)
   {
      this.modifyDate = modifyDate;
      return (this);
   }

}
