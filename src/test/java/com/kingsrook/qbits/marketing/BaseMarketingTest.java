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
import java.util.List;
import com.kingsrook.qbits.marketing.model.MarketingCampaign;
import com.kingsrook.qbits.marketing.model.MarketingChannel;
import com.kingsrook.qbits.marketing.model.MarketingClaim;
import com.kingsrook.qbits.marketing.model.MarketingContent;
import com.kingsrook.qbits.marketing.model.MarketingEvidenceSource;
import com.kingsrook.qbits.marketing.model.MarketingProduct;
import com.kingsrook.qbits.marketing.model.enums.AiContentPolicy;
import com.kingsrook.qbits.marketing.model.enums.CampaignGoal;
import com.kingsrook.qbits.marketing.model.enums.CampaignStatus;
import com.kingsrook.qbits.marketing.model.enums.ChannelType;
import com.kingsrook.qbits.marketing.model.enums.ClaimStatus;
import com.kingsrook.qbits.marketing.model.enums.ProductMaturity;
import com.kingsrook.qbits.marketing.model.enums.SourceType;
import com.kingsrook.qbits.marketing.security.MarketingPermissions;
import com.kingsrook.qbits.marketing.transport.ControlledTestTransport;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QAuthenticationType;
import com.kingsrook.qqq.backend.core.model.metadata.QBackendMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.QAuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QUser;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryBackendModule;
import com.kingsrook.qqq.backend.core.modules.backend.implementations.memory.MemoryRecordStore;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;


/*******************************************************************************
 ** Test base: an instance with the Marketing QBit on a memory backend, the
 ** fake CRM gateway, core permission enforcement on, and helpers for sessions
 ** and fixtures.
 *******************************************************************************/
public abstract class BaseMarketingTest
{
   public static final String BACKEND = "memory";

   protected QInstance           qInstance;
   protected MarketingQBitConfig config;
   protected FakeCrmGateway      crm;



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void baseBeforeEach() throws Exception
   {
      MemoryRecordStore.getInstance().reset();
      crm = new FakeCrmGateway();
      config = new MarketingQBitConfig().withBackendName(BACKEND).withCrmGateway(crm).withRetryBackoffSeconds(60);
      customizeConfig(config);
      qInstance = defineInstance(config);
      QContext.init(qInstance, admin());
      MarketingStore.insert(new com.kingsrook.qbits.marketing.model.MarketingPolicy().withName(MarketingRuntime.POLICY_NAME).withAgentEnabled(true).withExecutionEnabled(true));
   }



   /*******************************************************************************
    ** The default policy row's id.
    *******************************************************************************/
   protected Integer policyId() throws QException
   {
      return (MarketingStore.queryBy(com.kingsrook.qbits.marketing.model.MarketingPolicy.TABLE_NAME, "name", MarketingRuntime.POLICY_NAME).get(0).getValueInteger("id"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void baseAfterEach()
   {
      QContext.clear();
   }



   /*******************************************************************************
    ** Override to change the config before production.
    *******************************************************************************/
   protected void customizeConfig(MarketingQBitConfig config)
   {
   }



   /*******************************************************************************
    ** An instance with the qbit produced and validated.
    *******************************************************************************/
   public static QInstance defineInstance(MarketingQBitConfig config) throws QException
   {
      QInstance qInstance = new QInstance();
      qInstance.addBackend(new QBackendMetaData().withName(BACKEND).withBackendType(MemoryBackendModule.class));
      qInstance.withInstanceDefaultAuthentication(new QAuthenticationMetaData().withName("anonymous").withType(QAuthenticationType.FULLY_ANONYMOUS));
      qInstance.setEnforcePermissionsInCoreActions(true);
      new MarketingQBitProducer().withQBitConfig(config).produce(qInstance);
      new QInstanceValidator().validate(qInstance);
      return (qInstance);
   }



   /*******************************************************************************
    ** A user with the given permissions.
    *******************************************************************************/
   public static QSession user(String name, String... permissions)
   {
      QSession session = new QSession().withUser(new QUser().withIdReference(name).withFullName(name));
      session.withPermissions(new ArrayList<>(List.of(permissions)));
      return (session);
   }



   /*******************************************************************************
    ** Every marketing permission.
    *******************************************************************************/
   public static QSession admin()
   {
      return (user("admin", MarketingPermissions.ALL.toArray(new String[0])));
   }



   /*******************************************************************************
    ** Run as a session (QContext swap).
    *******************************************************************************/
   protected void as(QSession session)
   {
      QContext.init(qInstance, session);
   }



   /*******************************************************************************
    ** A product with one released and one planned claim; returns its id.
    *******************************************************************************/
   protected Integer product(String slug) throws QException
   {
      Integer productId = MarketingStore.insert(new MarketingProduct().withName("Product " + slug).withSlug(slug).withMaturity(ProductMaturity.RELEASED.getId())
         .withTagline("A test product.")).getValueInteger("id");
      Integer sourceId = MarketingStore.insert(new MarketingEvidenceSource().withProductId(productId).withTitle("Site").withUrl("https://example.org/" + slug)
         .withSourceType(SourceType.WEBSITE.getId()).withVerifiedDate(java.time.LocalDate.now())).getValueInteger("id");
      MarketingStore.insert(new MarketingClaim().withProductId(productId).withSourceId(sourceId).withStatus(ClaimStatus.RELEASED.getId()).withClaimText("It does X."));
      MarketingStore.insert(new MarketingClaim().withProductId(productId).withSourceId(sourceId).withStatus(ClaimStatus.PLANNED.getId()).withClaimText("It will do Y."));
      return (productId);
   }



   /*******************************************************************************
    ** A campaign; returns its id.
    *******************************************************************************/
   protected Integer campaign(Integer productId) throws QException
   {
      return (MarketingStore.insert(new MarketingCampaign().withProductId(productId).withName("Campaign").withGoal(CampaignGoal.OPEN_SOURCE_ADOPTION.getId())
         .withStatus(CampaignStatus.ACTIVE.getId()).withCallToAction("Try it").withDestinationUrl("https://example.org/try").withUtmCampaign("test-campaign"))
         .getValueInteger("id"));
   }



   /*******************************************************************************
    ** The controlled test sink channel; returns its id.
    *******************************************************************************/
   protected Integer testChannel() throws QException
   {
      return (channel("Test sink", ChannelType.TEST_SINK, ControlledTestTransport.NAME, AiContentPolicy.ALLOWED, true));
   }



   /*******************************************************************************
    ** A channel; returns its id.
    *******************************************************************************/
   protected Integer channel(String name, ChannelType type, String transport, AiContentPolicy aiPolicy, boolean automation) throws QException
   {
      return (MarketingStore.insert(new MarketingChannel().withName(name).withChannelType(type.getId()).withTransportName(transport).withIsActive(true)
         .withAllowsAutomation(automation).withAiContentPolicy(aiPolicy.getId()).withRulesVerifiedDate(java.time.LocalDate.now())).getValueInteger("id"));
   }



   /*******************************************************************************
    ** Draft content ready for review (schedule in the past = due now).
    *******************************************************************************/
   protected Integer content(Integer campaignId, Integer channelId, String body) throws QException
   {
      return (MarketingStore.insert(new MarketingContent().withCampaignId(campaignId).withChannelId(channelId).withTitle("Hello").withBody(body)
         .withDestinationUrl("https://example.org/try").withCallToAction("Try it").withScheduledFor(Instant.now().minusSeconds(60))).getValueInteger("id"));
   }



   /*******************************************************************************
    ** Re-read a record.
    *******************************************************************************/
   protected QRecord reload(String table, Integer id) throws QException
   {
      return (MarketingStore.get(table, id));
   }

}
