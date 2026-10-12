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

package com.kingsrook.qbits.marketing.measurement;


import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import com.kingsrook.qbits.marketing.MarketingRuntime;
import com.kingsrook.qbits.marketing.MarketingStore;
import com.kingsrook.qbits.marketing.crm.MarketingCrmGateway;
import com.kingsrook.qbits.marketing.model.MarketingCampaign;
import com.kingsrook.qbits.marketing.model.MarketingOutcomeEvent;
import com.kingsrook.qbits.marketing.model.enums.FunnelStage;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 ** Brings a campaign's CRM results into Marketing as outcome events - without
 ** copying CRM data (events carry CRM ids only) - and qualifies leads only by
 ** the campaign's explicit rule.
 **
 ** - Every CRM form submission with the campaign's UTM campaign becomes one
 **   Engagement event "crm_form_submission" (deduplicated by submission id).
 ** - A contact becomes a marketing-qualified lead only when the campaign has
 **   lead qualification turned on and the contact meets its rule (at least N
 **   attributed form submissions, and email consent when required). A visit,
 **   a download or a single anonymous submission is never a lead by itself.
 **   Qualification is logged in the CRM as an activity (the CRM's own
 **   workflow) and recorded as one Qualified-demand event per contact.
 *******************************************************************************/
public class CrmResultsService
{
   public static final String FORM_EVENT = "crm_form_submission";
   public static final String LEAD_EVENT = "marketing_qualified_lead";



   /*******************************************************************************
    ** Import and qualify. Returns a one-line summary.
    *******************************************************************************/
   public String updateFromCrm(Integer campaignId) throws QException
   {
      MarketingCrmGateway gateway = MarketingRuntime.current().getAvailableCrmGateway();
      if(gateway == null)
      {
         throw (new QUserFacingException("No CRM gateway is available in this application."));
      }

      MarketingCampaign campaign = new MarketingCampaign(MarketingStore.require(MarketingCampaign.TABLE_NAME, campaignId));
      if(!StringUtils.hasContent(campaign.getUtmCampaign()))
      {
         throw (new QUserFacingException("Set the campaign's UTM campaign first: it is how CRM form submissions are attributed."));
      }

      int                  imported   = 0;
      int                  duplicates = 0;
      Map<Integer, Integer> perContact = new LinkedHashMap<>();
      for(MarketingCrmGateway.CrmFormSubmissionRef submission : gateway.listFormSubmissions(campaign.getUtmCampaign(), null))
      {
         if(submission.contactId() != null)
         {
            perContact.merge(submission.contactId(), 1, Integer::sum);
         }

         String externalId = "crm-form:" + submission.submissionId();
         if(eventExists("crm", externalId))
         {
            duplicates++;
            continue;
         }
         MarketingStore.insert(new MarketingOutcomeEvent()
            .withCampaignId(campaignId)
            .withStage(FunnelStage.ENGAGEMENT.getId())
            .withEventType(FORM_EVENT)
            .withSource("crm")
            .withExternalEventId(externalId)
            .withActorKey(submission.contactId() == null ? "crm-form:" + submission.submissionId() : "crm-contact:" + submission.contactId())
            .withOccurredAt(submission.submittedAt())
            .withQuantity(1)
            .withIsTestData(false));
         imported++;
      }

      String qualification;
      if(!Boolean.TRUE.equals(campaign.getLeadQualificationEnabled()))
      {
         qualification = "Lead qualification is off for this campaign: no leads were qualified.";
      }
      else
      {
         int min        = campaign.getQualifyMinFormSubmissions() == null || campaign.getQualifyMinFormSubmissions() < 1 ? 1 : campaign.getQualifyMinFormSubmissions();
         int qualified  = 0;
         int noConsent  = 0;
         int belowRule  = 0;
         for(Map.Entry<Integer, Integer> entry : perContact.entrySet())
         {
            Integer contactId = entry.getKey();
            if(entry.getValue() < min)
            {
               belowRule++;
               continue;
            }
            if(Boolean.TRUE.equals(campaign.getQualifyRequireEmailConsent()) && !gateway.checkEmailEligibility(contactId).eligible())
            {
               noConsent++;
               continue;
            }

            String externalId = "lead:" + campaignId + ":" + contactId;
            if(eventExists("marketing", externalId))
            {
               continue;
            }
            String rule = "Rule: at least " + min + " form submission(s) attributed to UTM campaign \"" + campaign.getUtmCampaign() + "\""
               + (Boolean.TRUE.equals(campaign.getQualifyRequireEmailConsent()) ? " and current email consent" : "") + ".";
            gateway.recordActivity(contactId, "Marketing-qualified lead: " + campaign.getName(), rule);
            MarketingStore.insert(new MarketingOutcomeEvent()
               .withCampaignId(campaignId)
               .withStage(FunnelStage.QUALIFIED_DEMAND.getId())
               .withEventType(LEAD_EVENT)
               .withSource("marketing")
               .withExternalEventId(externalId)
               .withActorKey("crm-contact:" + contactId)
               .withOccurredAt(Instant.now())
               .withQuantity(1)
               .withIsTestData(false)
               .withNote(rule));
            qualified++;
         }
         qualification = qualified + " lead(s) newly qualified; " + belowRule + " contact(s) below the rule; " + noConsent + " without email consent.";
      }

      return (imported + " form submission(s) imported, " + duplicates + " already recorded. " + qualification);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static boolean eventExists(String source, String externalId) throws QException
   {
      return (MarketingStore.count(MarketingOutcomeEvent.TABLE_NAME, new QQueryFilter(
         new QFilterCriteria("source", QCriteriaOperator.EQUALS, source),
         new QFilterCriteria("externalEventId", QCriteriaOperator.EQUALS, externalId))) > 0);
   }

}
