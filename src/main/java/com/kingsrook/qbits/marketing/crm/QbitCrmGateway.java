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


import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import com.kingsrook.qbits.marketing.MarketingStore;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;


/*******************************************************************************
 ** MarketingCrmGateway over qbit-crm (com.kingsrook.qbits:qbit-crm 0.1.0) -
 ** by table name, through the standard QQQ actions, so Marketing needs no
 ** compile dependency on the CRM and CRM permissions and locks apply.
 **
 ** Rules it applies (from qbit-crm's own model):
 ** - Email consent: the contact's most recent crmConsentRecord of type Email
 **   decides (Granted, and not past its expiry date). No record means no
 **   consent: Marketing does not send unsolicited email.
 ** - Suppression: doNotEmail, an unsubscribeDate, or a list membership that
 **   is not Subscribed.
 ** - Activities are logged as qbit-crm "Note" activities.
 *******************************************************************************/
public class QbitCrmGateway implements MarketingCrmGateway
{
   public static final String CONTACT_TABLE         = "crmContact";
   public static final String CONSENT_TABLE         = "crmConsentRecord";
   public static final String LIST_MEMBER_TABLE     = "crmEmailListMember";
   public static final String FORM_SUBMISSION_TABLE = "crmFormSubmission";
   public static final String ACTIVITY_TABLE        = "crmActivity";
   public static final String ACTIVITY_TYPE_TABLE   = "crmActivityType";

   static final Integer CONSENT_TYPE_EMAIL       = 1;
   static final Integer CONSENT_STATUS_GRANTED   = 1;
   static final Integer LIST_MEMBER_SUBSCRIBED   = 1;
   static final String  NOTE_ACTIVITY_TYPE_NAME  = "Note";



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String getName()
   {
      return ("qbit-crm");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public boolean isAvailable()
   {
      QInstance qInstance = QContext.getQInstance();
      return (qInstance != null && qInstance.getTable(CONTACT_TABLE) != null && qInstance.getTable(CONSENT_TABLE) != null && qInstance.getTable(FORM_SUBMISSION_TABLE) != null);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public List<CrmRecipient> resolveRecipients(String recipientScope) throws QException
   {
      List<CrmRecipient> recipients = new ArrayList<>();
      if(recipientScope == null)
      {
         return (recipients);
      }

      if(recipientScope.startsWith("crm-contact:"))
      {
         Integer contactId = parseId(recipientScope, "crm-contact:");
         recipients.add(recipient(contactId, null));
      }
      else if(recipientScope.startsWith("crm-list:"))
      {
         Integer listId = parseId(recipientScope, "crm-list:");
         for(QRecord member : MarketingStore.queryBy(LIST_MEMBER_TABLE, "emailListId", listId))
         {
            Integer contactId = member.getValueInteger("contactId");
            if(contactId == null)
            {
               continue;
            }
            boolean subscribed = LIST_MEMBER_SUBSCRIBED.equals(member.getValueInteger("status"));
            recipients.add(recipient(contactId, subscribed ? null : "list_member_not_subscribed"));
         }
      }
      else
      {
         throw (new QUserFacingException("Unknown CRM recipient scope [" + recipientScope + "]: use crm-list:<id> or crm-contact:<id>."));
      }
      return (recipients);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private CrmRecipient recipient(Integer contactId, String listReason) throws QException
   {
      Eligibility eligibility = listReason == null ? checkEmailEligibility(contactId) : Eligibility.no(listReason);
      String      address     = null;
      if(eligibility.eligible())
      {
         QRecord contact = MarketingStore.get(CONTACT_TABLE, contactId);
         address = contact == null ? null : contact.getValueString("email");
      }
      return (new CrmRecipient(contactId, address, eligibility));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public Eligibility checkEmailEligibility(Integer contactId) throws QException
   {
      QRecord contact = MarketingStore.get(CONTACT_TABLE, contactId);
      if(contact == null)
      {
         return (Eligibility.no("contact_not_found"));
      }
      if(Boolean.TRUE.equals(contact.getValueBoolean("doNotEmail")))
      {
         return (Eligibility.no("do_not_email"));
      }
      if(contact.getValue("unsubscribeDate") != null)
      {
         return (Eligibility.no("unsubscribed"));
      }
      if(contact.getValueString("email") == null || contact.getValueString("email").isBlank())
      {
         return (Eligibility.no("no_email_address"));
      }

      List<QRecord> consents = MarketingStore.query(CONSENT_TABLE, new QQueryFilter(
         new QFilterCriteria("contactId", QCriteriaOperator.EQUALS, contactId),
         new QFilterCriteria("consentType", QCriteriaOperator.EQUALS, CONSENT_TYPE_EMAIL)));
      if(consents.isEmpty())
      {
         return (Eligibility.no("no_email_consent"));
      }

      QRecord latest = consents.stream()
         .max(Comparator.comparing((QRecord r) -> instantOrEpoch(r.getValue("consentDate"))).thenComparing(r -> ValueUtils.getValueAsInteger(r.getValue("id"))))
         .orElseThrow();
      if(!CONSENT_STATUS_GRANTED.equals(latest.getValueInteger("status")))
      {
         return (Eligibility.no("email_consent_withdrawn"));
      }
      Instant expiry = ValueUtils.getValueAsInstant(latest.getValue("expiryDate"));
      if(expiry != null && expiry.isBefore(Instant.now()))
      {
         return (Eligibility.no("email_consent_expired"));
      }
      return (Eligibility.OK);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public List<CrmFormSubmissionRef> listFormSubmissions(String utmCampaign, Instant since) throws QException
   {
      List<CrmFormSubmissionRef> list = new ArrayList<>();
      if(utmCampaign == null || utmCampaign.isBlank())
      {
         return (list);
      }

      QQueryFilter filter = new QQueryFilter(new QFilterCriteria("utmCampaign", QCriteriaOperator.EQUALS, utmCampaign))
         .withOrderBy(new QFilterOrderBy("id"));
      if(since != null)
      {
         filter.addCriteria(new QFilterCriteria("createDate", QCriteriaOperator.GREATER_THAN_OR_EQUALS, since));
      }
      for(QRecord submission : MarketingStore.query(FORM_SUBMISSION_TABLE, filter))
      {
         list.add(new CrmFormSubmissionRef(submission.getValueInteger("id"), submission.getValueInteger("contactId"), ValueUtils.getValueAsInstant(submission.getValue("createDate"))));
      }
      return (list);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public void recordActivity(Integer contactId, String subject, String description) throws QException
   {
      List<QRecord> types = MarketingStore.queryBy(ACTIVITY_TYPE_TABLE, "name", NOTE_ACTIVITY_TYPE_NAME);
      if(types.isEmpty())
      {
         throw (new QUserFacingException("The CRM has no \"" + NOTE_ACTIVITY_TYPE_NAME + "\" activity type to log marketing activities with."));
      }

      MarketingStore.insert(ACTIVITY_TABLE, new QRecord()
         .withValue("activityTypeId", types.get(0).getValueInteger("id"))
         .withValue("subject", subject.length() > 500 ? subject.substring(0, 500) : subject)
         .withValue("description", description)
         .withValue("contactId", contactId)
         .withValue("ownerUserId", MarketingStore.currentUserId())
         .withValue("isCompleted", true)
         .withValue("completedDate", Instant.now()));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static Integer parseId(String scope, String prefix) throws QUserFacingException
   {
      try
      {
         return (Integer.valueOf(scope.substring(prefix.length()).trim()));
      }
      catch(NumberFormatException e)
      {
         throw (new QUserFacingException("Recipient scope [" + scope + "] must end in a numeric id."));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static Instant instantOrEpoch(Object value)
   {
      Instant instant = ValueUtils.getValueAsInstant(value);
      return (instant == null ? Instant.EPOCH : instant);
   }

}
