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
import java.time.temporal.ChronoUnit;
import java.util.List;
import com.kingsrook.qbits.marketing.crm.MarketingCrmGateway.CrmFormSubmissionRef;
import com.kingsrook.qbits.marketing.crm.MarketingCrmGateway.CrmRecipient;
import com.kingsrook.qbits.marketing.crm.QbitCrmGateway;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** QbitCrmGateway against fixture tables shaped like qbit-crm 0.2.0's crm_*
 ** tables.
 *******************************************************************************/
class QbitCrmGatewayTest extends BaseMarketingTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testNotAvailableWithoutCrmTables()
   {
      assertThat(new QbitCrmGateway().isAvailable()).isFalse();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testCrmFlows() throws QException
   {
      defineSnakeCaseTables();
      assertThat(QbitCrmGateway.CONTACT_TABLE).isEqualTo("crm_contact");
      exerciseGateway();
   }



   /*******************************************************************************
    ** Eligibility, recipient scopes, form submissions and activities.
    *******************************************************************************/
   private void exerciseGateway() throws QException
   {
      QbitCrmGateway gateway = new QbitCrmGateway();
      assertThat(gateway.isAvailable()).isTrue();

      Integer granted    = insert(QbitCrmGateway.CONTACT_TABLE, new QRecord().withValue("email", "yes@example.org").withValue("doNotEmail", false));
      Integer noConsent  = insert(QbitCrmGateway.CONTACT_TABLE, new QRecord().withValue("email", "none@example.org").withValue("doNotEmail", false));
      Integer suppressed = insert(QbitCrmGateway.CONTACT_TABLE, new QRecord().withValue("email", "dne@example.org").withValue("doNotEmail", true));
      for(Integer contactId : List.of(granted, suppressed))
      {
         insert(QbitCrmGateway.CONSENT_TABLE, new QRecord().withValue("contactId", contactId).withValue("consentType", 1).withValue("status", 1)
            .withValue("consentDate", Instant.now().minus(2, ChronoUnit.DAYS)));
      }

      /////////////////////////////
      // checkEmailEligibility   //
      /////////////////////////////
      assertThat(gateway.checkEmailEligibility(granted).eligible()).isTrue();
      assertThat(gateway.checkEmailEligibility(noConsent).reason()).isEqualTo("no_email_consent");
      assertThat(gateway.checkEmailEligibility(suppressed).reason()).isEqualTo("do_not_email");
      assertThat(gateway.checkEmailEligibility(9999).reason()).isEqualTo("contact_not_found");

      ///////////////////////////////////////
      // resolveRecipients: contact scope  //
      ///////////////////////////////////////
      List<CrmRecipient> single = gateway.resolveRecipients("crm-contact:" + granted);
      assertThat(single).hasSize(1);
      assertThat(single.get(0).address()).isEqualTo("yes@example.org");

      /////////////////////////////////////
      // resolveRecipients: list scope   //
      /////////////////////////////////////
      insert(QbitCrmGateway.LIST_MEMBER_TABLE, new QRecord().withValue("emailListId", 7).withValue("contactId", granted).withValue("status", 1));
      insert(QbitCrmGateway.LIST_MEMBER_TABLE, new QRecord().withValue("emailListId", 7).withValue("contactId", noConsent).withValue("status", 1));
      insert(QbitCrmGateway.LIST_MEMBER_TABLE, new QRecord().withValue("emailListId", 7).withValue("contactId", suppressed).withValue("status", 2));
      List<CrmRecipient> members = gateway.resolveRecipients("crm-list:7");
      assertThat(members).hasSize(3);
      assertThat(members.stream().filter(r -> r.eligibility().eligible()).map(CrmRecipient::address)).containsExactly("yes@example.org");
      assertThat(members.stream().map(r -> r.eligibility().reason())).contains("no_email_consent", "list_member_not_subscribed");

      ///////////////////////////
      // listFormSubmissions   //
      ///////////////////////////
      Instant created = Instant.now().minus(1, ChronoUnit.HOURS);
      insert(QbitCrmGateway.FORM_SUBMISSION_TABLE, new QRecord().withValue("contactId", granted).withValue("utmCampaign", "spring").withValue("createDate", created));
      insert(QbitCrmGateway.FORM_SUBMISSION_TABLE, new QRecord().withValue("contactId", noConsent).withValue("utmCampaign", "other").withValue("createDate", created));
      List<CrmFormSubmissionRef> submissions = gateway.listFormSubmissions("spring", null);
      assertThat(submissions).hasSize(1);
      assertThat(submissions.get(0).contactId()).isEqualTo(granted);
      assertThat(gateway.listFormSubmissions("spring", Instant.now())).isEmpty();

      //////////////////////
      // recordActivity   //
      //////////////////////
      assertThatThrownBy(() -> gateway.recordActivity(granted, "Subject", "Body")).hasMessageContaining("Note");
      insert(QbitCrmGateway.ACTIVITY_TYPE_TABLE, new QRecord().withValue("name", "Note"));
      gateway.recordActivity(granted, "Subject", "Body");
      List<QRecord> activities = MarketingStore.query(QbitCrmGateway.ACTIVITY_TABLE, new QQueryFilter());
      assertThat(activities).hasSize(1);
      assertThat(activities.get(0).getValueString("subject")).isEqualTo("Subject");
      assertThat(activities.get(0).getValueInteger("contactId")).isEqualTo(granted);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Integer insert(String table, QRecord record) throws QException
   {
      return (MarketingStore.insert(table, record).getValueInteger("id"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void defineSnakeCaseTables()
   {
      defineCrmTables(QbitCrmGateway.CONTACT_TABLE, QbitCrmGateway.CONSENT_TABLE, QbitCrmGateway.LIST_MEMBER_TABLE,
         QbitCrmGateway.FORM_SUBMISSION_TABLE, QbitCrmGateway.ACTIVITY_TABLE, QbitCrmGateway.ACTIVITY_TYPE_TABLE);
   }



   /*******************************************************************************
    ** Define fixture tables (memory backend) shaped like qbit-crm's.
    *******************************************************************************/
   private void defineCrmTables(String contact, String consent, String listMember, String formSubmission, String activity, String activityType)
   {
      defineTable(contact, field("email", QFieldType.STRING), field("doNotEmail", QFieldType.BOOLEAN), field("unsubscribeDate", QFieldType.DATE_TIME));
      defineTable(consent, field("contactId", QFieldType.INTEGER), field("consentType", QFieldType.INTEGER), field("status", QFieldType.INTEGER),
         field("consentDate", QFieldType.DATE_TIME), field("expiryDate", QFieldType.DATE_TIME));
      defineTable(listMember, field("emailListId", QFieldType.INTEGER), field("contactId", QFieldType.INTEGER), field("status", QFieldType.INTEGER));
      defineTable(formSubmission, field("contactId", QFieldType.INTEGER), field("utmCampaign", QFieldType.STRING), field("createDate", QFieldType.DATE_TIME));
      defineTable(activity, field("activityTypeId", QFieldType.INTEGER), field("subject", QFieldType.STRING), field("description", QFieldType.STRING),
         field("contactId", QFieldType.INTEGER), field("ownerUserId", QFieldType.STRING), field("isCompleted", QFieldType.BOOLEAN), field("completedDate", QFieldType.DATE_TIME));
      defineTable(activityType, field("name", QFieldType.STRING));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static QFieldMetaData field(String name, QFieldType type)
   {
      return (new QFieldMetaData(name, type));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void defineTable(String name, QFieldMetaData... fields)
   {
      QTableMetaData table = new QTableMetaData().withName(name).withBackendName(BACKEND).withPrimaryKeyField("id")
         .withField(new QFieldMetaData("id", QFieldType.INTEGER));
      for(QFieldMetaData field : fields)
      {
         table.withField(field);
      }
      qInstance.addTable(table);
   }

}
