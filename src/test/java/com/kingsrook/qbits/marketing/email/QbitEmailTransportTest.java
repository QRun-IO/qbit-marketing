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

package com.kingsrook.qbits.marketing.email;


import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qbits.email.EmailQBitConfig;
import com.kingsrook.qbits.email.EmailQBitProducer;
import com.kingsrook.qbits.email.api.EmailApi;
import com.kingsrook.qbits.email.api.EmailApiCategory;
import com.kingsrook.qbits.email.api.EmailApiMessage;
import com.kingsrook.qbits.marketing.MarketingQBitConfig;
import com.kingsrook.qbits.marketing.MarketingQBitProducer;
import com.kingsrook.qbits.marketing.MarketingRuntime;
import com.kingsrook.qbits.marketing.model.enums.ChannelType;
import com.kingsrook.qbits.marketing.security.MarketingPermissions;
import com.kingsrook.qbits.marketing.transport.MarketingTransport;
import com.kingsrook.qbits.marketing.transport.MarketingTransport.Recipient;
import com.kingsrook.qbits.marketing.transport.MarketingTransport.TransportRequest;
import com.kingsrook.qbits.marketing.transport.MarketingTransport.TransportResult;
import com.kingsrook.qbits.marketing.transport.QqqMessagingEmailTransport;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QAuthenticationType;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.QAuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryBackendModule;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryRecordStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** QbitEmailTransport against a real qbit-email outbox (memory backend).
 *******************************************************************************/
class QbitEmailTransportTest
{
   private static final String BACKEND = "memory";



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void afterEach()
   {
      QContext.clear();
   }



   /*******************************************************************************
    ** An instance with marketing and (optionally) the email qbit.
    *******************************************************************************/
   private QInstance instance(boolean withEmail, String publicBaseUrl, String unsubscribeKey, MarketingQBitConfig marketingConfig) throws Exception
   {
      MemoryRecordStore.getInstance().reset();
      QInstance qInstance = new QInstance();
      qInstance.addBackend(new QBackendMetaData().withName(BACKEND).withBackendType(MemoryBackendModule.class));
      qInstance.withInstanceDefaultAuthentication(new QAuthenticationMetaData().withName("anonymous").withType(QAuthenticationType.FULLY_ANONYMOUS));
      if(withEmail)
      {
         EmailQBitConfig emailConfig = new EmailQBitConfig().withBackendName(BACKEND).withPublicBaseUrl(publicBaseUrl)
            .withSecretResolver(name -> "QQQ_EMAIL_UNSUBSCRIBE_KEY".equals(name) ? unsubscribeKey : null);
         new EmailQBitProducer().withQBitConfig(emailConfig).produce(qInstance);
      }
      new MarketingQBitProducer().withQBitConfig(marketingConfig.withBackendName(BACKEND)).produce(qInstance);
      new QInstanceValidator().validate(qInstance);

      List<String> permissions = new ArrayList<>(MarketingPermissions.ALL);
      permissions.addAll(List.of("email.view", "email.send", "email.manage", "email.admin", "email.hasAccess"));
      QContext.init(qInstance, new QSession().withPermissions(permissions));
      return (qInstance);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void provider() throws QException
   {
      brand();
      insert("email_provider", new QRecord().withValue("name", "relay").withValue("label", "Relay").withValue("kind", "SMTP").withValue("host", "127.0.0.1")
         .withValue("port", 2525).withValue("security", "NONE").withValue("fromAddress", "sender@example.com").withValue("enabled", true).withValue("isDefault", true));
   }



   /*******************************************************************************
    ** Marketing email needs a default brand with a postal address.
    *******************************************************************************/
   private void brand() throws QException
   {
      insert("email_brand", new QRecord().withValue("name", "default").withValue("label", "Example").withValue("isDefault", true)
         .withValue("postalAddress", "1 Main St, Springfield"));
   }



   /*******************************************************************************
    ** Configure the email qbit's editable tables through ordinary QQQ actions.
    *******************************************************************************/
   private void insert(String table, QRecord record) throws QException
   {
      var output = new InsertAction().execute(new InsertInput(table).withRecords(List.of(record)));
      assertThat(output.getRecords()).hasSize(1).allSatisfy(row -> assertThat(row.getErrors()).isNullOrEmpty());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private TransportRequest request(String key, Recipient... recipients)
   {
      return (new TransportRequest(key, 7, 1, "Email", ChannelType.EMAIL, "Big news", "Hello **there**.", "https://example.org/try", "Try it", List.of(recipients)));
   }



   /*******************************************************************************
    ** One MARKETING message per recipient with the right key and link; a repeat
    ** is DUPLICATE; the qqq_email name resolves to the transport.
    *******************************************************************************/
   @Test
   void testSendsOneMarketingMessagePerRecipient() throws Exception
   {
      instance(true, "https://app.example.org", "k", new MarketingQBitConfig());
      provider();

      MarketingTransport transport = MarketingRuntime.current().getTransport(QqqMessagingEmailTransport.DEFAULT_NAME);
      assertThat(transport).isInstanceOf(QbitEmailTransport.class);
      assertThat(transport.getStatus().state()).isEqualTo(MarketingTransport.TransportStatus.State.OPERATIONAL);
      assertThat(transport.getSupportedChannelTypes()).containsExactly(ChannelType.EMAIL);

      TransportRequest request = request("marketing-execution:3:4", new Recipient("crm-contact:1", "a@example.org"), new Recipient("crm-contact:2", "b@example.org"));
      TransportResult  result  = transport.send(request);
      assertThat(result.message()).as(result.message()).isNotNull();
      assertThat(result.outcome()).as(result.message()).isEqualTo(TransportResult.Outcome.ACCEPTED);
      assertThat(result.providerStatus()).isEqualTo("queued");

      List<EmailApiMessage> rows = EmailApi.findMessagesByIdempotencyKeyPrefix("marketing:3:4:", 10);
      assertThat(rows).hasSize(2);
      assertThat(rows).extracting(EmailApiMessage::idempotencyKey).containsExactlyInAnyOrder("marketing:3:4:crm-contact:1", "marketing:3:4:crm-contact:2");
      assertThat(rows).allSatisfy(r ->
      {
         assertThat(r.category()).isEqualTo(EmailApiCategory.MARKETING);
         assertThat(r.linkedTableName()).isEqualTo("marketing_execution");
         assertThat(r.linkedRecordId()).isEqualTo("7");
         assertThat(r.templateName()).isNull();
         assertThat(r.status()).isEqualTo("QUEUED");
      });

      // The public API intentionally omits content; inspect it as an email admin.
      List<QRecord> messages = new QueryAction().execute(new QueryInput("email_message")
         .withShouldFetchHeavyFields(true).withShouldOmitHiddenFields(false)).getRecords();
      assertThat(messages).hasSize(2).allSatisfy(r ->
      {
         assertThat(r.getValueString("subject")).isEqualTo("Big news");
         assertThat(r.getValueString("senderQbit")).isEqualTo("marketing");
         assertThat(r.getValueString("senderProcess")).isEqualTo("execution");
         assertThat(r.getValueString("htmlBody")).contains("https://example.org/try").contains("nsubscribe");
         assertThat(r.getValueString("toAddresses")).doesNotContain(",");
      });

      assertThat(transport.send(request).outcome()).isEqualTo(TransportResult.Outcome.DUPLICATE);
      assertThat(EmailApi.findMessagesByIdempotencyKeyPrefix("marketing:3:4:", 10)).hasSize(2);

      assertThat(transport.lookup("marketing-execution:3:4").state()).isEqualTo(MarketingTransport.TransportLookup.State.FOUND_ACCEPTED);
      assertThat(transport.lookup("marketing-execution:9:9").state()).isEqualTo(MarketingTransport.TransportLookup.State.NOT_FOUND);
   }



   /*******************************************************************************
    ** A suppressed address is skipped, not failed; with every recipient
    ** suppressed the result is still ACCEPTED (all_suppressed).
    *******************************************************************************/
   @Test
   void testSuppressedIsSkipped() throws Exception
   {
      instance(true, "https://app.example.org", "k", new MarketingQBitConfig());
      provider();
      insert("email_suppression", new QRecord().withValue("address", "gone@example.org").withValue("reason", "UNSUBSCRIBED").withValue("scope", "MARKETING"));

      MarketingTransport transport = new QbitEmailTransport();
      TransportResult    result    = transport.send(request("marketing-execution:1:1", new Recipient("crm-contact:9", "gone@example.org")));
      assertThat(result.outcome()).isEqualTo(TransportResult.Outcome.ACCEPTED);
      assertThat(result.providerStatus()).isEqualTo("all_suppressed");
      assertThat(result.message()).contains("1 skipped as suppressed");

      TransportResult mixed = transport.send(request("marketing-execution:2:1", new Recipient("crm-contact:9", "gone@example.org"), new Recipient("crm-contact:10", "ok@example.org")));
      assertThat(mixed.providerStatus()).isEqualTo("queued");
      assertThat(mixed.message()).contains("1 queued, 1 skipped");
   }



   /*******************************************************************************
    ** Without the unsubscribe key or public base URL the transport is not
    ** configured and says why; a refusal by the email qbit is REJECTED with
    ** its reason.
    *******************************************************************************/
   @Test
   void testNotConfiguredAndRefused() throws Exception
   {
      instance(true, "https://app.example.org", null, new MarketingQBitConfig());
      QbitEmailTransport transport = new QbitEmailTransport();
      assertThat(transport.getStatus().state()).isEqualTo(MarketingTransport.TransportStatus.State.NOT_CONFIGURED);
      assertThat(transport.getStatus().detail()).contains("QQQ_EMAIL_UNSUBSCRIBE_KEY");
      assertThat(transport.send(request("k", new Recipient("a", "a@example.org"))).outcome()).isEqualTo(TransportResult.Outcome.RETRYABLE_FAILURE);

      instance(true, null, "k", new MarketingQBitConfig());
      assertThat(new QbitEmailTransport().getStatus().detail()).contains("publicBaseUrl");

      instance(true, "https://app.example.org", "k", new MarketingQBitConfig());
      assertThat(transport.getStatus().state()).isEqualTo(MarketingTransport.TransportStatus.State.OPERATIONAL);
      TransportResult refused = new QbitEmailTransport().send(request("marketing-execution:5:5", new Recipient("a", "a@example.org")));
      assertThat(refused.outcome()).isEqualTo(TransportResult.Outcome.REJECTED);
      assertThat(refused.message()).contains("refused the message");

      provider();
      TransportResult noAddress = new QbitEmailTransport().send(request("marketing-execution:6:6", new Recipient("a", null)));
      assertThat(noAddress.outcome()).isEqualTo(TransportResult.Outcome.REJECTED);
   }



   /*******************************************************************************
    ** A missing postal address is a send-time refusal, not NOT_CONFIGURED.
    *******************************************************************************/
   @Test
   void testMissingBrandIsRefusedAtSend() throws Exception
   {
      instance(true, "https://app.example.org", "k", new MarketingQBitConfig());
      insert("email_provider", new QRecord().withValue("name", "relay").withValue("label", "Relay").withValue("kind", "SMTP").withValue("host", "127.0.0.1")
         .withValue("port", 2525).withValue("security", "NONE").withValue("fromAddress", "sender@example.com").withValue("enabled", true).withValue("isDefault", true));

      QbitEmailTransport transport = new QbitEmailTransport();
      assertThat(transport.getStatus().state()).isEqualTo(MarketingTransport.TransportStatus.State.OPERATIONAL);
      TransportResult result = transport.send(request("marketing-execution:8:8", new Recipient("a", "a@example.org")));
      assertThat(result.outcome()).isEqualTo(TransportResult.Outcome.REJECTED);
      assertThat(result.message()).contains("postal address");
   }



   /*******************************************************************************
    ** Without the email qbit in the instance the name does not resolve to the
    ** transport, and a host-registered transport always wins.
    *******************************************************************************/
   @Test
   void testFallbackAndHostTransportWin() throws Exception
   {
      instance(false, null, null, new MarketingQBitConfig());
      assertThat(MarketingRuntime.current().getTransport(QqqMessagingEmailTransport.DEFAULT_NAME)).isNull();
      assertThat(new QbitEmailTransport().getStatus().state()).isEqualTo(MarketingTransport.TransportStatus.State.NOT_CONFIGURED);

      QqqMessagingEmailTransport host = new QqqMessagingEmailTransport().withMessagingProviderName("email").withFromAddress("n@example.org");
      instance(true, "https://app.example.org", "k", new MarketingQBitConfig().withTransport(host));
      assertThat(MarketingRuntime.current().getTransport(QqqMessagingEmailTransport.DEFAULT_NAME)).isSameAs(host);
   }

}
