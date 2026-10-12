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

package com.kingsrook.qbits.marketing.crm;


import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import com.kingsrook.qqq.backend.core.exceptions.QException;


/*******************************************************************************
 ** The narrow seam between Marketing and the host's CRM.
 **
 ** Marketing never copies CRM data: contacts, organizations, consent,
 ** suppression, activities and leads stay in the CRM and are used through
 ** these operations, which a gateway implements over the CRM's own tables and
 ** workflows (QbitCrmGateway does it for qbit-crm). Everything runs as the
 ** current session, so CRM permissions and record locks apply.
 **
 ** Personal data: only resolveRecipients returns addresses, and only the
 ** execution worker calls it - after approval, at send time. Summaries and
 ** lead candidates carry ids and counts only; nothing here is ever given to a
 ** model.
 *******************************************************************************/
public interface MarketingCrmGateway
{

   /*******************************************************************************
    ** The gateway's name (shown in the configuration status).
    *******************************************************************************/
   String getName();

   /*******************************************************************************
    ** Whether the CRM is there (its tables exist in the instance).
    *******************************************************************************/
   boolean isAvailable();

   /*******************************************************************************
    ** The contacts of a recipient scope ("crm-list:<id>" or "crm-contact:<id>"),
    ** each with its email-eligibility (consent and suppression) checked now.
    *******************************************************************************/
   List<CrmRecipient> resolveRecipients(String recipientScope) throws QException;

   /*******************************************************************************
    ** Form submissions attributed to a campaign (by UTM campaign), ids only.
    *******************************************************************************/
   List<CrmFormSubmissionRef> listFormSubmissions(String utmCampaign, Instant since) throws QException;

   /*******************************************************************************
    ** Whether a contact may be emailed now (consent granted, not suppressed).
    *******************************************************************************/
   Eligibility checkEmailEligibility(Integer contactId) throws QException;

   /*******************************************************************************
    ** Record an activity on a contact through the CRM's own activity workflow
    ** (e.g. "Marketing-qualified lead").
    *******************************************************************************/
   void recordActivity(Integer contactId, String subject, String description) throws QException;



   /*******************************************************************************
    ** A recipient. address is filled only for eligible recipients.
    *******************************************************************************/
   record CrmRecipient(Integer contactId, String address, Eligibility eligibility) implements Serializable
   {
      /*******************************************************************************
       ** The stable recipient key used by Marketing (never the address).
       *******************************************************************************/
      public String key()
      {
         return ("crm-contact:" + contactId);
      }
   }



   /*******************************************************************************
    ** A form submission: ids and time only.
    *******************************************************************************/
   record CrmFormSubmissionRef(Integer submissionId, Integer contactId, Instant submittedAt) implements Serializable
   {
   }



   /*******************************************************************************
    ** Email eligibility: allowed, or a reason code (no personal data).
    *******************************************************************************/
   record Eligibility(boolean eligible, String reason) implements Serializable
   {
      public static final Eligibility OK = new Eligibility(true, "eligible");



      /*******************************************************************************
       ** A not-eligible result.
       *******************************************************************************/
      public static Eligibility no(String reason)
      {
         return (new Eligibility(false, reason));
      }
   }

}
