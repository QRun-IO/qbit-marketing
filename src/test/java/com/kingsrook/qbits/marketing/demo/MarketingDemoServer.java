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

package com.kingsrook.qbits.marketing.demo;


import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.sql.Connection;
import java.sql.Statement;
import java.time.Instant;
import java.util.List;
import java.util.Map;
import com.kingsrook.qbits.assistant.spi.ModelUsage;
import com.kingsrook.qbits.marketing.MarketingQBitConfig;
import com.kingsrook.qbits.marketing.MarketingQBitProducer;
import com.kingsrook.qbits.marketing.MarketingStore;
import com.kingsrook.qbits.marketing.ScriptedModelProvider;
import com.kingsrook.qbits.marketing.agent.MarketingAgentService;
import com.kingsrook.qbits.marketing.agent.tools.MarketingTools;
import com.kingsrook.qbits.marketing.model.MarketingCampaign;
import com.kingsrook.qbits.marketing.model.MarketingChannel;
import com.kingsrook.qbits.marketing.model.MarketingClaim;
import com.kingsrook.qbits.marketing.model.MarketingContent;
import com.kingsrook.qbits.marketing.model.MarketingOutcomeEvent;
import com.kingsrook.qbits.marketing.model.MarketingProduct;
import com.kingsrook.qbits.marketing.model.enums.AgentTask;
import com.kingsrook.qbits.marketing.model.enums.ClaimStatus;
import com.kingsrook.qbits.marketing.model.enums.FunnelStage;
import com.kingsrook.qbits.marketing.processes.MarketingActions;
import com.kingsrook.qbits.marketing.processes.MarketingTableProcesses;
import com.kingsrook.qbits.marketing.security.MarketingPermissions;
import com.kingsrook.qqq.backend.core.actions.processes.QProcessCallbackFactory;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.instances.AbstractQQQApplication;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QAuthenticationType;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.QAuthenticationMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.branding.QBrandingMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QUser;
import com.kingsrook.qqq.backend.core.modules.authentication.QAuthenticationModuleCustomizerInterface;
import com.kingsrook.qqq.backend.module.rdbms.jdbc.ConnectionManager;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import com.kingsrook.qqq.middleware.javalin.QApplicationJavalinServer;


/*******************************************************************************
 ** A local demo of the Marketing qbit for screenshots and manual checks (not a
 ** test, not shipped): an in-memory H2 database created from the shipped DDL
 ** (src/test/resources/marketing-h2.sql), a mock login as a user holding every
 ** marketing permission, the verified starter data, and some history made
 ** through the real services: a readiness check, an agent draft (offline,
 ** scripted model), review, approval, scheduling, a controlled test execution
 ** with its attempt and test delivery, and a few outcome events.
 **
 ** Run it (from the repository root), then the Next UI dev server against it:
 **
 **   ./mvnw -q test-compile
 **   ./mvnw -q dependency:build-classpath -Dmdep.outputFile=target/cp.txt -Dmdep.includeScope=test
 **   java -cp "target/test-classes;target/classes;$(cat target/cp.txt)" com.kingsrook.qbits.marketing.demo.MarketingDemoServer
 **
 ** (Use ':' instead of ';' in the classpath on Linux/macOS. The server listens
 ** on port 8000, or MARKETING_DEMO_PORT.)
 **
 **   QQQ_BACKEND_URL=http://localhost:8000 pnpm dev      (in qqq-frontend-next; port 3000)
 *******************************************************************************/
public class MarketingDemoServer extends AbstractQQQApplication
{
   static final String BACKEND = "demo";
   static final String USER    = "dana";

   private static final String BODY = "QQQ 4.0 is on Maven Central: you can try it with the quickstart in a few minutes. Java 21, Docker and Bash are required.";



   /*******************************************************************************
    ** Start the demo.
    *******************************************************************************/
   public static void main(String[] args) throws Exception
   {
      MarketingDemoServer application = new MarketingDemoServer();
      new QApplicationJavalinServer(application)
         .withServeFrontendNext(false)
         .withServeFrontendMaterialDashboard(false)
         .withPort(Integer.parseInt(System.getenv().getOrDefault("MARKETING_DEMO_PORT", "8000")))
         .start();
   }



   /*******************************************************************************
    ** The demo instance (seeded on definition).
    *******************************************************************************/
   @Override
   public QInstance defineQInstance() throws QException
   {
      QInstance qInstance = new QInstance();
      RDBMSBackendMetaData backend = new RDBMSBackendMetaData().withName(BACKEND).withVendor("h2").withHostName("mem").withDatabaseName("marketing_demo").withUsername("sa");
      backend.setJdbcUrl(ConnectionManager.getJdbcUrl(backend) + ";DATABASE_TO_UPPER=FALSE;DB_CLOSE_DELAY=-1");
      qInstance.addBackend(backend);
      qInstance.withInstanceDefaultAuthentication(new QAuthenticationMetaData().withName("mock").withType(QAuthenticationType.MOCK)
         .withCustomizer(new QCodeReference(DemoSessionCustomizer.class)));
      qInstance.setBranding(new QBrandingMetaData().withAppName("Marketing demo").withCompanyName("QRun-IO"));
      qInstance.setEnforcePermissionsInCoreActions(true);

      createTables(backend);

      ScriptedModelProvider model = new ScriptedModelProvider("scripted").withUsagePerCall(new ModelUsage().withInputTokens(1200L).withOutputTokens(300L));
      new MarketingQBitProducer().withQBitConfig(new MarketingQBitConfig().withBackendName(BACKEND).withModelProvider(model).withModel("scripted-model")).produce(qInstance);

      seed(qInstance, model);
      return (qInstance);
   }



   /*******************************************************************************
    ** Create the tables from the shipped H2 DDL.
    *******************************************************************************/
   static void createTables(RDBMSBackendMetaData backend) throws QException
   {
      try(Connection connection = ConnectionManager.getConnection(backend); Statement statement = connection.createStatement();
          InputStream ddl = MarketingDemoServer.class.getResourceAsStream("/marketing-h2.sql"))
      {
         String sql = new String(ddl.readAllBytes(), StandardCharsets.UTF_8);
         for(String part : sql.split(";"))
         {
            String trimmed = part.lines().filter(l -> !l.trim().startsWith("--")).reduce("", (a, b) -> a + "\n" + b).trim();
            if(!trimmed.isEmpty())
            {
               statement.execute(trimmed);
            }
         }
      }
      catch(Exception e)
      {
         throw (new QException("Could not create the demo tables", e));
      }
   }



   /*******************************************************************************
    ** Starter data, then history through the real services and processes.
    *******************************************************************************/
   static void seed(QInstance qInstance, ScriptedModelProvider model) throws QException
   {
      try
      {
         QContext.init(qInstance, DemoSessionCustomizer.sessionFor(USER));

         ////////////////////////////////////////////////////////////////
         // the qbit's own "Load Verified Starter Data" table action    //
         ////////////////////////////////////////////////////////////////
         requireNoException(new RunProcessAction().execute(new RunProcessInput().withProcessName(MarketingTableProcesses.LOAD_STARTER_DATA)
            .withFrontendStepBehavior(RunProcessInput.FrontendStepBehavior.SKIP)), "starter data");

         QRecord product    = first(MarketingProduct.TABLE_NAME, "slug", "qqq");
         QRecord campaign   = first(MarketingCampaign.TABLE_NAME, "name", "QQQ 4.0: from quickstart to your first table");
         QRecord sink       = first(MarketingChannel.TABLE_NAME, "name", "Controlled test sink");
         Integer campaignId = campaign.getValueInteger("id");

         requireNoException(runAction(MarketingActions.CHECK_CAMPAIGN_READINESS, campaignId), "campaign readiness");
         requireNoException(runAction(MarketingActions.CHECK_CAMPAIGN_READINESS, first(MarketingCampaign.TABLE_NAME, "name", "gclo 1.1 for Windows: mirror a whole GitHub org").getValue("id")), "campaign readiness");

         ///////////////////////////////////////////////////////////////////////////
         // Draft with Agent, offline: the scripted model reads, then saves a draft //
         ///////////////////////////////////////////////////////////////////////////
         Integer released = MarketingStore.query(MarketingClaim.TABLE_NAME, new QQueryFilter(
            new QFilterCriteria("productId", QCriteriaOperator.EQUALS, product.getValue("id")),
            new QFilterCriteria("status", QCriteriaOperator.EQUALS, ClaimStatus.RELEASED.getId()))).get(0).getValueInteger("id");
         model.thenToolUses("Reading the brief first.", List.of(
               new Object[] { MarketingTools.GET_CAMPAIGN_BRIEF, Map.of("campaign_id", campaignId) },
               new Object[] { MarketingTools.LIST_PRODUCT_CLAIMS, Map.of("product_id", product.getValueInteger("id")) },
               new Object[] { MarketingTools.LIST_CHANNELS, Map.of() }))
            .thenToolUse(MarketingTools.SAVE_DRAFT, Map.of("campaign_id", campaignId, "channel_id", sink.getValueInteger("id"), "title", "Try QQQ 4.0 in a few minutes",
               "body", BODY, "claim_ids", List.of(released)))
            .thenText("I saved one draft for review on the controlled test sink.");
         new MarketingAgentService().run(AgentTask.DRAFT_CONTENT, campaignId, null, null);

         ///////////////////////////////////////////////////////////////////////////
         // a person schedules it, submits it, approves it and schedules it; the   //
         // worker then runs the due execution on the controlled test sink         //
         ///////////////////////////////////////////////////////////////////////////
         Integer contentId = first(MarketingContent.TABLE_NAME, "campaignId", campaignId).getValueInteger("id");
         MarketingStore.update(MarketingContent.TABLE_NAME, new QRecord().withValue("id", contentId).withValue("scheduledFor", Instant.now().minusSeconds(5)));
         requireNoException(runAction(MarketingActions.SUBMIT_FOR_REVIEW, contentId), "submit for review");
         requireNoException(runAction(MarketingActions.APPROVE, contentId), "approve");
         requireNoException(runAction(MarketingActions.SCHEDULE, contentId), "schedule");
         requireNoException(new RunProcessAction().execute(new RunProcessInput().withProcessName(MarketingTableProcesses.RUN_DUE_EXECUTIONS)
            .withFrontendStepBehavior(RunProcessInput.FrontendStepBehavior.SKIP)), "run due executions");

         ///////////////////////////////////////////////////
         // a second piece of content, waiting for review  //
         ///////////////////////////////////////////////////
         Integer waiting = MarketingStore.insert(new MarketingContent().withCampaignId(campaignId).withChannelId(sink.getValueInteger("id")).withTitle("Your first table, step by step")
            .withBody("The quickstart takes you from an empty project to your first table. Java 21, Docker and Bash are required.")
            .withScheduledFor(Instant.now().plusSeconds(3600L * 24))).getValueInteger("id");
         requireNoException(runAction(MarketingActions.SUBMIT_FOR_REVIEW, waiting), "submit for review");

         /////////////////////////////////////////////////////////////////////////
         // controlled fixture events (test data, never counted as real numbers) //
         /////////////////////////////////////////////////////////////////////////
         for(int i = 1; i <= 5; i++)
         {
            MarketingStore.insert(new MarketingOutcomeEvent().withCampaignId(campaignId).withStage(FunnelStage.AWARENESS.getId()).withEventType("try_page_visit")
               .withSource("fixture").withExternalEventId("visit-" + i).withActorKey("visitor-" + (i % 3)).withIsTestData(true));
         }
         MarketingStore.insert(new MarketingOutcomeEvent().withCampaignId(campaignId).withStage(FunnelStage.ENGAGEMENT.getId()).withEventType("quickstart_opened")
            .withSource("fixture").withExternalEventId("open-1").withActorKey("visitor-1").withIsTestData(true));
      }
      finally
      {
         QContext.clear();
      }
   }



   /*******************************************************************************
    ** The first record matching one field.
    *******************************************************************************/
   private static QRecord first(String table, String field, Serializable value) throws QException
   {
      return (MarketingStore.queryBy(table, field, value).get(0));
   }



   /*******************************************************************************
    ** Run a record action headlessly on one record.
    *******************************************************************************/
   private static RunProcessOutput runAction(String processName, Serializable id) throws QException
   {
      return (new RunProcessAction().execute(new RunProcessInput().withProcessName(processName).withCallback(QProcessCallbackFactory.forPrimaryKey("id", id))
         .withFrontendStepBehavior(RunProcessInput.FrontendStepBehavior.SKIP)));
   }



   /*******************************************************************************
    ** Fail the demo start-up if a seeding step failed.
    *******************************************************************************/
   private static void requireNoException(RunProcessOutput output, String what) throws QException
   {
      if(output.getException().isPresent())
      {
         throw (new QException("Demo seeding failed at [" + what + "]", output.getException().get()));
      }
   }



   /*******************************************************************************
    ** Every mock session is Dana, who holds every marketing permission.
    *******************************************************************************/
   public static class DemoSessionCustomizer implements QAuthenticationModuleCustomizerInterface
   {
      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public void customizeSession(QInstance qInstance, QSession qSession, Map<String, Object> context)
      {
         QSession granted = sessionFor(USER);
         qSession.setUser(granted.getUser());
         qSession.setPermissions(granted.getPermissions());
      }



      /***************************************************************************
       **
       ***************************************************************************/
      @Override
      public void customizeAutomatedSessionForUser(QInstance qInstance, QSession automatedSessionForUser, Serializable userId)
      {
         automatedSessionForUser.setPermissions(sessionFor(String.valueOf(userId)).getPermissions());
      }



      /***************************************************************************
       ** Dana's session.
       ***************************************************************************/
      static QSession sessionFor(String userId)
      {
         QSession session = new QSession().withUser(new QUser().withIdReference(userId).withFullName("Dana Demo"));
         session.setPermissions(new java.util.HashSet<>(MarketingPermissions.ALL));
         return (session);
      }
   }
}
