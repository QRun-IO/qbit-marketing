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

package com.kingsrook.qbits.marketing;


import java.time.Instant;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qbits.marketing.crm.MarketingCrmGateway;


/*******************************************************************************
 ** An in-memory CRM for tests: contacts with an email, consent and
 ** suppression, lists, form submissions and the activities Marketing logs.
 *******************************************************************************/
public class FakeCrmGateway implements MarketingCrmGateway
{
   /*******************************************************************************
    ** A contact.
    *******************************************************************************/
   public static class Contact
   {
      public Integer id;
      public String  email;
      public boolean consent;
      public boolean doNotEmail;
   }

   public final Map<Integer, Contact>       contacts    = new LinkedHashMap<>();
   public final Map<Integer, List<Integer>> lists       = new LinkedHashMap<>();
   public final List<CrmFormSubmissionRef>  submissions = new ArrayList<>();
   public final List<String>                activities  = new ArrayList<>();
   public final Map<String, String>         submissionCampaigns = new LinkedHashMap<>();



   /*******************************************************************************
    ** Add a contact.
    *******************************************************************************/
   public FakeCrmGateway withContact(int id, String email, boolean consent, boolean doNotEmail)
   {
      Contact contact = new Contact();
      contact.id = id;
      contact.email = email;
      contact.consent = consent;
      contact.doNotEmail = doNotEmail;
      contacts.put(id, contact);
      return (this);
   }



   /*******************************************************************************
    ** Add a list.
    *******************************************************************************/
   public FakeCrmGateway withList(int listId, Integer... contactIds)
   {
      lists.put(listId, List.of(contactIds));
      return (this);
   }



   /*******************************************************************************
    ** Add a form submission attributed to a UTM campaign.
    *******************************************************************************/
   public FakeCrmGateway withSubmission(int id, Integer contactId, String utmCampaign)
   {
      submissions.add(new CrmFormSubmissionRef(id, contactId, Instant.now()));
      submissionCampaigns.put(String.valueOf(id), utmCampaign);
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String getName()
   {
      return ("fake-crm");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public boolean isAvailable()
   {
      return (true);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public List<CrmRecipient> resolveRecipients(String recipientScope)
   {
      if("crm-list:99".equals(recipientScope))
      {
         throw (new IllegalStateException("simulated CRM outage for list 99"));
      }
      List<CrmRecipient> list = new ArrayList<>();
      List<Integer>      ids  = recipientScope.startsWith("crm-list:") ? lists.getOrDefault(Integer.valueOf(recipientScope.substring(9)), List.of())
         : List.of(Integer.valueOf(recipientScope.substring(12)));
      for(Integer id : ids)
      {
         Eligibility eligibility = checkEmailEligibility(id);
         list.add(new CrmRecipient(id, eligibility.eligible() ? contacts.get(id).email : null, eligibility));
      }
      return (list);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public List<CrmFormSubmissionRef> listFormSubmissions(String utmCampaign, Instant since)
   {
      return (submissions.stream().filter(s -> utmCampaign.equals(submissionCampaigns.get(String.valueOf(s.submissionId())))).toList());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public Eligibility checkEmailEligibility(Integer contactId)
   {
      Contact contact = contacts.get(contactId);
      if(contact == null)
      {
         return (Eligibility.no("contact_not_found"));
      }
      if(contact.doNotEmail)
      {
         return (Eligibility.no("do_not_email"));
      }
      if(!contact.consent)
      {
         return (Eligibility.no("no_email_consent"));
      }
      return (Eligibility.OK);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void recordActivity(Integer contactId, String subject, String description)
   {
      activities.add(contactId + "|" + subject);
   }

}
