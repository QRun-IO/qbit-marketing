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
import com.kingsrook.qbits.marketing.model.enums.FindingSeverity;
import com.kingsrook.qbits.marketing.model.enums.FindingStatus;
import com.kingsrook.qbits.marketing.model.enums.FindingType;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.data.QField;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;
import com.kingsrook.qqq.backend.core.model.metadata.fields.ValueTooLongBehavior;


/*******************************************************************************
 ** A finding for a person to act on: unsupported claims, stale sources, gaps, limitations, recommendations.
 ** Table marketing_finding.
 *******************************************************************************/
public class MarketingFinding extends QRecordEntity
{
   public static final String TABLE_NAME = "marketing_finding";

   @QField(isEditable = false, isPrimaryKey = true)
   private Integer id;

   @QField(possibleValueSourceName = MarketingProduct.TABLE_NAME)
   private Integer productId;

   @QField(possibleValueSourceName = MarketingCampaign.TABLE_NAME)
   private Integer campaignId;

   @QField(possibleValueSourceName = MarketingContent.TABLE_NAME)
   private Integer contentId;

   @QField(possibleValueSourceName = MarketingClaim.TABLE_NAME)
   private Integer claimId;

   @QField(possibleValueSourceName = MarketingEvidenceSource.TABLE_NAME)
   private Integer sourceId;

   @QField(possibleValueSourceName = MarketingAgentRun.TABLE_NAME)
   private Integer agentRunId;

   @QField(isRequired = true, possibleValueSourceName = FindingType.NAME)
   private Integer findingType;

   @QField(isRequired = true, possibleValueSourceName = FindingSeverity.NAME)
   private Integer severity;

   @QField(isRequired = true, possibleValueSourceName = FindingStatus.NAME)
   private Integer status;

   @QField(maxLength = 255, valueTooLongBehavior = ValueTooLongBehavior.ERROR, isRequired = true)
   private String title;

   @QField()
   private String detail;

   @QField()
   private String recommendation;

   @QField(maxLength = 32, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String origin;

   @QField(isEditable = false)
   private Instant createDate;

   @QField(isEditable = false)
   private Instant modifyDate;



   /*******************************************************************************
    ** Default constructor.
    *******************************************************************************/
   public MarketingFinding()
   {
   }



   /*******************************************************************************
    ** Constructor that populates from a QRecord.
    *******************************************************************************/
   public MarketingFinding(QRecord record) throws QException
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
   public MarketingFinding withId(Integer id)
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
   public MarketingFinding withProductId(Integer productId)
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
   public MarketingFinding withCampaignId(Integer campaignId)
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
   public MarketingFinding withContentId(Integer contentId)
   {
      this.contentId = contentId;
      return (this);
   }


   /*******************************************************************************
    ** Getter for claimId
    *******************************************************************************/
   public Integer getClaimId()
   {
      return (this.claimId);
   }



   /*******************************************************************************
    ** Setter for claimId
    *******************************************************************************/
   public void setClaimId(Integer claimId)
   {
      this.claimId = claimId;
   }



   /*******************************************************************************
    ** Fluent setter for claimId
    *******************************************************************************/
   public MarketingFinding withClaimId(Integer claimId)
   {
      this.claimId = claimId;
      return (this);
   }


   /*******************************************************************************
    ** Getter for sourceId
    *******************************************************************************/
   public Integer getSourceId()
   {
      return (this.sourceId);
   }



   /*******************************************************************************
    ** Setter for sourceId
    *******************************************************************************/
   public void setSourceId(Integer sourceId)
   {
      this.sourceId = sourceId;
   }



   /*******************************************************************************
    ** Fluent setter for sourceId
    *******************************************************************************/
   public MarketingFinding withSourceId(Integer sourceId)
   {
      this.sourceId = sourceId;
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
   public MarketingFinding withAgentRunId(Integer agentRunId)
   {
      this.agentRunId = agentRunId;
      return (this);
   }


   /*******************************************************************************
    ** Getter for findingType
    *******************************************************************************/
   public Integer getFindingType()
   {
      return (this.findingType);
   }



   /*******************************************************************************
    ** Setter for findingType
    *******************************************************************************/
   public void setFindingType(Integer findingType)
   {
      this.findingType = findingType;
   }



   /*******************************************************************************
    ** Fluent setter for findingType
    *******************************************************************************/
   public MarketingFinding withFindingType(Integer findingType)
   {
      this.findingType = findingType;
      return (this);
   }


   /*******************************************************************************
    ** Getter for severity
    *******************************************************************************/
   public Integer getSeverity()
   {
      return (this.severity);
   }



   /*******************************************************************************
    ** Setter for severity
    *******************************************************************************/
   public void setSeverity(Integer severity)
   {
      this.severity = severity;
   }



   /*******************************************************************************
    ** Fluent setter for severity
    *******************************************************************************/
   public MarketingFinding withSeverity(Integer severity)
   {
      this.severity = severity;
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
   public MarketingFinding withStatus(Integer status)
   {
      this.status = status;
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
   public MarketingFinding withTitle(String title)
   {
      this.title = title;
      return (this);
   }


   /*******************************************************************************
    ** Getter for detail
    *******************************************************************************/
   public String getDetail()
   {
      return (this.detail);
   }



   /*******************************************************************************
    ** Setter for detail
    *******************************************************************************/
   public void setDetail(String detail)
   {
      this.detail = detail;
   }



   /*******************************************************************************
    ** Fluent setter for detail
    *******************************************************************************/
   public MarketingFinding withDetail(String detail)
   {
      this.detail = detail;
      return (this);
   }


   /*******************************************************************************
    ** Getter for recommendation
    *******************************************************************************/
   public String getRecommendation()
   {
      return (this.recommendation);
   }



   /*******************************************************************************
    ** Setter for recommendation
    *******************************************************************************/
   public void setRecommendation(String recommendation)
   {
      this.recommendation = recommendation;
   }



   /*******************************************************************************
    ** Fluent setter for recommendation
    *******************************************************************************/
   public MarketingFinding withRecommendation(String recommendation)
   {
      this.recommendation = recommendation;
      return (this);
   }


   /*******************************************************************************
    ** Getter for origin
    *******************************************************************************/
   public String getOrigin()
   {
      return (this.origin);
   }



   /*******************************************************************************
    ** Setter for origin
    *******************************************************************************/
   public void setOrigin(String origin)
   {
      this.origin = origin;
   }



   /*******************************************************************************
    ** Fluent setter for origin
    *******************************************************************************/
   public MarketingFinding withOrigin(String origin)
   {
      this.origin = origin;
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
   public MarketingFinding withCreateDate(Instant createDate)
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
   public MarketingFinding withModifyDate(Instant modifyDate)
   {
      this.modifyDate = modifyDate;
      return (this);
   }

}
