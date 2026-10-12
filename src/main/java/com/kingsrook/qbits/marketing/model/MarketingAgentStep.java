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
import com.kingsrook.qbits.marketing.model.enums.AgentStepKind;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.data.QField;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;
import com.kingsrook.qqq.backend.core.model.metadata.fields.ValueTooLongBehavior;


/*******************************************************************************
 ** One step of an agent run: a model call, a tool call or a deterministic check.
 ** Table marketing_agent_step.
 *******************************************************************************/
public class MarketingAgentStep extends QRecordEntity
{
   public static final String TABLE_NAME = "marketing_agent_step";

   @QField(isEditable = false, isPrimaryKey = true)
   private Integer id;

   @QField(possibleValueSourceName = MarketingProduct.TABLE_NAME)
   private Integer productId;

   @QField(isRequired = true, possibleValueSourceName = MarketingAgentRun.TABLE_NAME)
   private Integer agentRunId;

   @QField()
   private Integer seq;

   @QField(isRequired = true, possibleValueSourceName = AgentStepKind.NAME)
   private Integer kind;

   @QField(maxLength = 100, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String toolName;

   @QField(maxLength = 32, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String outcome;

   @QField(maxLength = 1000, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String summary;

   @QField()
   private String inputJson;

   @QField()
   private String resultJson;

   @QField()
   private Long inputTokens;

   @QField()
   private Long outputTokens;

   @QField()
   private Long durationMs;

   @QField(isEditable = false)
   private Instant createDate;

   @QField(isEditable = false)
   private Instant modifyDate;



   /*******************************************************************************
    ** Default constructor.
    *******************************************************************************/
   public MarketingAgentStep()
   {
   }



   /*******************************************************************************
    ** Constructor that populates from a QRecord.
    *******************************************************************************/
   public MarketingAgentStep(QRecord record) throws QException
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
   public MarketingAgentStep withId(Integer id)
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
   public MarketingAgentStep withProductId(Integer productId)
   {
      this.productId = productId;
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
   public MarketingAgentStep withAgentRunId(Integer agentRunId)
   {
      this.agentRunId = agentRunId;
      return (this);
   }


   /*******************************************************************************
    ** Getter for seq
    *******************************************************************************/
   public Integer getSeq()
   {
      return (this.seq);
   }



   /*******************************************************************************
    ** Setter for seq
    *******************************************************************************/
   public void setSeq(Integer seq)
   {
      this.seq = seq;
   }



   /*******************************************************************************
    ** Fluent setter for seq
    *******************************************************************************/
   public MarketingAgentStep withSeq(Integer seq)
   {
      this.seq = seq;
      return (this);
   }


   /*******************************************************************************
    ** Getter for kind
    *******************************************************************************/
   public Integer getKind()
   {
      return (this.kind);
   }



   /*******************************************************************************
    ** Setter for kind
    *******************************************************************************/
   public void setKind(Integer kind)
   {
      this.kind = kind;
   }



   /*******************************************************************************
    ** Fluent setter for kind
    *******************************************************************************/
   public MarketingAgentStep withKind(Integer kind)
   {
      this.kind = kind;
      return (this);
   }


   /*******************************************************************************
    ** Getter for toolName
    *******************************************************************************/
   public String getToolName()
   {
      return (this.toolName);
   }



   /*******************************************************************************
    ** Setter for toolName
    *******************************************************************************/
   public void setToolName(String toolName)
   {
      this.toolName = toolName;
   }



   /*******************************************************************************
    ** Fluent setter for toolName
    *******************************************************************************/
   public MarketingAgentStep withToolName(String toolName)
   {
      this.toolName = toolName;
      return (this);
   }


   /*******************************************************************************
    ** Getter for outcome
    *******************************************************************************/
   public String getOutcome()
   {
      return (this.outcome);
   }



   /*******************************************************************************
    ** Setter for outcome
    *******************************************************************************/
   public void setOutcome(String outcome)
   {
      this.outcome = outcome;
   }



   /*******************************************************************************
    ** Fluent setter for outcome
    *******************************************************************************/
   public MarketingAgentStep withOutcome(String outcome)
   {
      this.outcome = outcome;
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
   public MarketingAgentStep withSummary(String summary)
   {
      this.summary = summary;
      return (this);
   }


   /*******************************************************************************
    ** Getter for inputJson
    *******************************************************************************/
   public String getInputJson()
   {
      return (this.inputJson);
   }



   /*******************************************************************************
    ** Setter for inputJson
    *******************************************************************************/
   public void setInputJson(String inputJson)
   {
      this.inputJson = inputJson;
   }



   /*******************************************************************************
    ** Fluent setter for inputJson
    *******************************************************************************/
   public MarketingAgentStep withInputJson(String inputJson)
   {
      this.inputJson = inputJson;
      return (this);
   }


   /*******************************************************************************
    ** Getter for resultJson
    *******************************************************************************/
   public String getResultJson()
   {
      return (this.resultJson);
   }



   /*******************************************************************************
    ** Setter for resultJson
    *******************************************************************************/
   public void setResultJson(String resultJson)
   {
      this.resultJson = resultJson;
   }



   /*******************************************************************************
    ** Fluent setter for resultJson
    *******************************************************************************/
   public MarketingAgentStep withResultJson(String resultJson)
   {
      this.resultJson = resultJson;
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
   public MarketingAgentStep withInputTokens(Long inputTokens)
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
   public MarketingAgentStep withOutputTokens(Long outputTokens)
   {
      this.outputTokens = outputTokens;
      return (this);
   }


   /*******************************************************************************
    ** Getter for durationMs
    *******************************************************************************/
   public Long getDurationMs()
   {
      return (this.durationMs);
   }



   /*******************************************************************************
    ** Setter for durationMs
    *******************************************************************************/
   public void setDurationMs(Long durationMs)
   {
      this.durationMs = durationMs;
   }



   /*******************************************************************************
    ** Fluent setter for durationMs
    *******************************************************************************/
   public MarketingAgentStep withDurationMs(Long durationMs)
   {
      this.durationMs = durationMs;
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
   public MarketingAgentStep withCreateDate(Instant createDate)
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
   public MarketingAgentStep withModifyDate(Instant modifyDate)
   {
      this.modifyDate = modifyDate;
      return (this);
   }

}
