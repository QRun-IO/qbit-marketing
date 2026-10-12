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
import com.kingsrook.qbits.marketing.model.enums.CampaignGoal;
import com.kingsrook.qbits.marketing.model.enums.CampaignStatus;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.data.QField;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;
import com.kingsrook.qqq.backend.core.model.metadata.fields.ValueTooLongBehavior;


/*******************************************************************************
 ** A campaign: one goal, audience, message, channels, call to action and measurable success criteria.
 ** Table marketing_campaign.
 *******************************************************************************/
public class MarketingCampaign extends QRecordEntity
{
   public static final String TABLE_NAME = "marketing_campaign";

   @QField(isEditable = false, isPrimaryKey = true)
   private Integer id;

   @QField(isRequired = true, possibleValueSourceName = MarketingProduct.TABLE_NAME)
   private Integer productId;

   @QField(maxLength = 200, valueTooLongBehavior = ValueTooLongBehavior.ERROR, isRequired = true)
   private String name;

   @QField(isRequired = true, possibleValueSourceName = CampaignGoal.NAME)
   private Integer goal;

   @QField(isRequired = true, possibleValueSourceName = CampaignStatus.NAME)
   private Integer status;

   @QField(possibleValueSourceName = MarketingAudience.TABLE_NAME)
   private Integer audienceId;

   @QField()
   private String objective;

   @QField()
   private String problem;

   @QField()
   private String messaging;

   @QField()
   private String channelPlan;

   @QField(maxLength = 500, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String callToAction;

   @QField(maxLength = 1000, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String destinationUrl;

   @QField(maxLength = 100, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String utmCampaign;

   @QField()
   private Instant scheduleStart;

   @QField()
   private Instant scheduleEnd;

   @QField(maxLength = 100, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String ownerUserId;

   @QField()
   private String successCriteria;

   @QField()
   private Boolean leadQualificationEnabled;

   @QField()
   private Integer qualifyMinFormSubmissions;

   @QField()
   private Boolean qualifyRequireEmailConsent;

   @QField()
   private String leadQualificationNote;

   @QField(isEditable = false)
   private Instant createDate;

   @QField(isEditable = false)
   private Instant modifyDate;



   /*******************************************************************************
    ** Default constructor.
    *******************************************************************************/
   public MarketingCampaign()
   {
   }



   /*******************************************************************************
    ** Constructor that populates from a QRecord.
    *******************************************************************************/
   public MarketingCampaign(QRecord record) throws QException
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
   public MarketingCampaign withId(Integer id)
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
   public MarketingCampaign withProductId(Integer productId)
   {
      this.productId = productId;
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
   public MarketingCampaign withName(String name)
   {
      this.name = name;
      return (this);
   }


   /*******************************************************************************
    ** Getter for goal
    *******************************************************************************/
   public Integer getGoal()
   {
      return (this.goal);
   }



   /*******************************************************************************
    ** Setter for goal
    *******************************************************************************/
   public void setGoal(Integer goal)
   {
      this.goal = goal;
   }



   /*******************************************************************************
    ** Fluent setter for goal
    *******************************************************************************/
   public MarketingCampaign withGoal(Integer goal)
   {
      this.goal = goal;
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
   public MarketingCampaign withStatus(Integer status)
   {
      this.status = status;
      return (this);
   }


   /*******************************************************************************
    ** Getter for audienceId
    *******************************************************************************/
   public Integer getAudienceId()
   {
      return (this.audienceId);
   }



   /*******************************************************************************
    ** Setter for audienceId
    *******************************************************************************/
   public void setAudienceId(Integer audienceId)
   {
      this.audienceId = audienceId;
   }



   /*******************************************************************************
    ** Fluent setter for audienceId
    *******************************************************************************/
   public MarketingCampaign withAudienceId(Integer audienceId)
   {
      this.audienceId = audienceId;
      return (this);
   }


   /*******************************************************************************
    ** Getter for objective
    *******************************************************************************/
   public String getObjective()
   {
      return (this.objective);
   }



   /*******************************************************************************
    ** Setter for objective
    *******************************************************************************/
   public void setObjective(String objective)
   {
      this.objective = objective;
   }



   /*******************************************************************************
    ** Fluent setter for objective
    *******************************************************************************/
   public MarketingCampaign withObjective(String objective)
   {
      this.objective = objective;
      return (this);
   }


   /*******************************************************************************
    ** Getter for problem
    *******************************************************************************/
   public String getProblem()
   {
      return (this.problem);
   }



   /*******************************************************************************
    ** Setter for problem
    *******************************************************************************/
   public void setProblem(String problem)
   {
      this.problem = problem;
   }



   /*******************************************************************************
    ** Fluent setter for problem
    *******************************************************************************/
   public MarketingCampaign withProblem(String problem)
   {
      this.problem = problem;
      return (this);
   }


   /*******************************************************************************
    ** Getter for messaging
    *******************************************************************************/
   public String getMessaging()
   {
      return (this.messaging);
   }



   /*******************************************************************************
    ** Setter for messaging
    *******************************************************************************/
   public void setMessaging(String messaging)
   {
      this.messaging = messaging;
   }



   /*******************************************************************************
    ** Fluent setter for messaging
    *******************************************************************************/
   public MarketingCampaign withMessaging(String messaging)
   {
      this.messaging = messaging;
      return (this);
   }


   /*******************************************************************************
    ** Getter for channelPlan
    *******************************************************************************/
   public String getChannelPlan()
   {
      return (this.channelPlan);
   }



   /*******************************************************************************
    ** Setter for channelPlan
    *******************************************************************************/
   public void setChannelPlan(String channelPlan)
   {
      this.channelPlan = channelPlan;
   }



   /*******************************************************************************
    ** Fluent setter for channelPlan
    *******************************************************************************/
   public MarketingCampaign withChannelPlan(String channelPlan)
   {
      this.channelPlan = channelPlan;
      return (this);
   }


   /*******************************************************************************
    ** Getter for callToAction
    *******************************************************************************/
   public String getCallToAction()
   {
      return (this.callToAction);
   }



   /*******************************************************************************
    ** Setter for callToAction
    *******************************************************************************/
   public void setCallToAction(String callToAction)
   {
      this.callToAction = callToAction;
   }



   /*******************************************************************************
    ** Fluent setter for callToAction
    *******************************************************************************/
   public MarketingCampaign withCallToAction(String callToAction)
   {
      this.callToAction = callToAction;
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
   public MarketingCampaign withDestinationUrl(String destinationUrl)
   {
      this.destinationUrl = destinationUrl;
      return (this);
   }


   /*******************************************************************************
    ** Getter for utmCampaign
    *******************************************************************************/
   public String getUtmCampaign()
   {
      return (this.utmCampaign);
   }



   /*******************************************************************************
    ** Setter for utmCampaign
    *******************************************************************************/
   public void setUtmCampaign(String utmCampaign)
   {
      this.utmCampaign = utmCampaign;
   }



   /*******************************************************************************
    ** Fluent setter for utmCampaign
    *******************************************************************************/
   public MarketingCampaign withUtmCampaign(String utmCampaign)
   {
      this.utmCampaign = utmCampaign;
      return (this);
   }


   /*******************************************************************************
    ** Getter for scheduleStart
    *******************************************************************************/
   public Instant getScheduleStart()
   {
      return (this.scheduleStart);
   }



   /*******************************************************************************
    ** Setter for scheduleStart
    *******************************************************************************/
   public void setScheduleStart(Instant scheduleStart)
   {
      this.scheduleStart = scheduleStart;
   }



   /*******************************************************************************
    ** Fluent setter for scheduleStart
    *******************************************************************************/
   public MarketingCampaign withScheduleStart(Instant scheduleStart)
   {
      this.scheduleStart = scheduleStart;
      return (this);
   }


   /*******************************************************************************
    ** Getter for scheduleEnd
    *******************************************************************************/
   public Instant getScheduleEnd()
   {
      return (this.scheduleEnd);
   }



   /*******************************************************************************
    ** Setter for scheduleEnd
    *******************************************************************************/
   public void setScheduleEnd(Instant scheduleEnd)
   {
      this.scheduleEnd = scheduleEnd;
   }



   /*******************************************************************************
    ** Fluent setter for scheduleEnd
    *******************************************************************************/
   public MarketingCampaign withScheduleEnd(Instant scheduleEnd)
   {
      this.scheduleEnd = scheduleEnd;
      return (this);
   }


   /*******************************************************************************
    ** Getter for ownerUserId
    *******************************************************************************/
   public String getOwnerUserId()
   {
      return (this.ownerUserId);
   }



   /*******************************************************************************
    ** Setter for ownerUserId
    *******************************************************************************/
   public void setOwnerUserId(String ownerUserId)
   {
      this.ownerUserId = ownerUserId;
   }



   /*******************************************************************************
    ** Fluent setter for ownerUserId
    *******************************************************************************/
   public MarketingCampaign withOwnerUserId(String ownerUserId)
   {
      this.ownerUserId = ownerUserId;
      return (this);
   }


   /*******************************************************************************
    ** Getter for successCriteria
    *******************************************************************************/
   public String getSuccessCriteria()
   {
      return (this.successCriteria);
   }



   /*******************************************************************************
    ** Setter for successCriteria
    *******************************************************************************/
   public void setSuccessCriteria(String successCriteria)
   {
      this.successCriteria = successCriteria;
   }



   /*******************************************************************************
    ** Fluent setter for successCriteria
    *******************************************************************************/
   public MarketingCampaign withSuccessCriteria(String successCriteria)
   {
      this.successCriteria = successCriteria;
      return (this);
   }


   /*******************************************************************************
    ** Getter for leadQualificationEnabled
    *******************************************************************************/
   public Boolean getLeadQualificationEnabled()
   {
      return (this.leadQualificationEnabled);
   }



   /*******************************************************************************
    ** Setter for leadQualificationEnabled
    *******************************************************************************/
   public void setLeadQualificationEnabled(Boolean leadQualificationEnabled)
   {
      this.leadQualificationEnabled = leadQualificationEnabled;
   }



   /*******************************************************************************
    ** Fluent setter for leadQualificationEnabled
    *******************************************************************************/
   public MarketingCampaign withLeadQualificationEnabled(Boolean leadQualificationEnabled)
   {
      this.leadQualificationEnabled = leadQualificationEnabled;
      return (this);
   }


   /*******************************************************************************
    ** Getter for qualifyMinFormSubmissions
    *******************************************************************************/
   public Integer getQualifyMinFormSubmissions()
   {
      return (this.qualifyMinFormSubmissions);
   }



   /*******************************************************************************
    ** Setter for qualifyMinFormSubmissions
    *******************************************************************************/
   public void setQualifyMinFormSubmissions(Integer qualifyMinFormSubmissions)
   {
      this.qualifyMinFormSubmissions = qualifyMinFormSubmissions;
   }



   /*******************************************************************************
    ** Fluent setter for qualifyMinFormSubmissions
    *******************************************************************************/
   public MarketingCampaign withQualifyMinFormSubmissions(Integer qualifyMinFormSubmissions)
   {
      this.qualifyMinFormSubmissions = qualifyMinFormSubmissions;
      return (this);
   }


   /*******************************************************************************
    ** Getter for qualifyRequireEmailConsent
    *******************************************************************************/
   public Boolean getQualifyRequireEmailConsent()
   {
      return (this.qualifyRequireEmailConsent);
   }



   /*******************************************************************************
    ** Setter for qualifyRequireEmailConsent
    *******************************************************************************/
   public void setQualifyRequireEmailConsent(Boolean qualifyRequireEmailConsent)
   {
      this.qualifyRequireEmailConsent = qualifyRequireEmailConsent;
   }



   /*******************************************************************************
    ** Fluent setter for qualifyRequireEmailConsent
    *******************************************************************************/
   public MarketingCampaign withQualifyRequireEmailConsent(Boolean qualifyRequireEmailConsent)
   {
      this.qualifyRequireEmailConsent = qualifyRequireEmailConsent;
      return (this);
   }


   /*******************************************************************************
    ** Getter for leadQualificationNote
    *******************************************************************************/
   public String getLeadQualificationNote()
   {
      return (this.leadQualificationNote);
   }



   /*******************************************************************************
    ** Setter for leadQualificationNote
    *******************************************************************************/
   public void setLeadQualificationNote(String leadQualificationNote)
   {
      this.leadQualificationNote = leadQualificationNote;
   }



   /*******************************************************************************
    ** Fluent setter for leadQualificationNote
    *******************************************************************************/
   public MarketingCampaign withLeadQualificationNote(String leadQualificationNote)
   {
      this.leadQualificationNote = leadQualificationNote;
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
   public MarketingCampaign withCreateDate(Instant createDate)
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
   public MarketingCampaign withModifyDate(Instant modifyDate)
   {
      this.modifyDate = modifyDate;
      return (this);
   }

}
