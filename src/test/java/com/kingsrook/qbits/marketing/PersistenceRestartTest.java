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


import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.Statement;
import java.time.Instant;
import java.util.List;
import com.kingsrook.qbits.marketing.approval.ApprovalService;
import com.kingsrook.qbits.marketing.measurement.ImportedOutcome;
import com.kingsrook.qbits.marketing.measurement.MeasurementReport;
import com.kingsrook.qbits.marketing.measurement.MeasurementRollupService;
import com.kingsrook.qbits.marketing.measurement.OutcomeImportService;
import com.kingsrook.qbits.marketing.execution.ExecutionService;
import com.kingsrook.qbits.marketing.model.MarketingAgentRun;
import com.kingsrook.qbits.marketing.model.MarketingApproval;
import com.kingsrook.qbits.marketing.model.MarketingCampaign;
import com.kingsrook.qbits.marketing.model.MarketingContent;
import com.kingsrook.qbits.marketing.model.MarketingContentRevision;
import com.kingsrook.qbits.marketing.model.MarketingExecution;
import com.kingsrook.qbits.marketing.model.MarketingMetric;
import com.kingsrook.qbits.marketing.model.MarketingMetricDaily;
import com.kingsrook.qbits.marketing.model.MarketingOutcomeEvent;
import com.kingsrook.qbits.marketing.model.MarketingProduct;
import com.kingsrook.qbits.marketing.model.MarketingTestDelivery;
import com.kingsrook.qbits.marketing.model.enums.AgentRunStatus;
import com.kingsrook.qbits.marketing.model.enums.AgentTask;
import com.kingsrook.qbits.marketing.model.enums.ApprovalStatus;
import com.kingsrook.qbits.marketing.model.enums.CampaignGoal;
import com.kingsrook.qbits.marketing.model.enums.CampaignStatus;
import com.kingsrook.qbits.marketing.model.enums.CountMode;
import com.kingsrook.qbits.marketing.model.enums.ExecutionStatus;
import com.kingsrook.qbits.marketing.model.enums.FunnelStage;
import com.kingsrook.qbits.marketing.model.enums.InstrumentationStatus;
import com.kingsrook.qbits.marketing.model.enums.ProductMaturity;
import com.kingsrook.qbits.marketing.processes.MarketingTableProcesses;
import com.kingsrook.qbits.marketing.seed.MarketingStarterData;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.instances.QInstanceValidator;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QAuthenticationType;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.authentication.QAuthenticationMetaData;
import com.kingsrook.qqq.backend.module.rdbms.model.metadata.RDBMSBackendMetaData;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Persistence across a restart, on the real RDBMS backend module with a
 ** file-backed H2 database created from the shipped DDL (the H2 twin of
 ** docs/ddl/postgres.sql):
 **
 ** 1. first "process": starter data, a content item approved and scheduled,
 **    one execution left RUNNING (worker died mid-send), one agent run left
 **    RUNNING;
 ** 2. the database is shut down (closing the files) and a new QInstance and
 **    runtime are built - nothing survives but the files;
 ** 3. after the restart: every record is back, the approval still covers the
 **    content, the scheduled execution runs, the dead worker's execution is
 **    flagged for reconciliation (not retried), and the interrupted agent run
 **    is marked failed.
 *******************************************************************************/
class PersistenceRestartTest
{
   private Path directory;



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void afterEach()
   {
      QContext.clear();
   }



   /*******************************************************************************
    ** A fresh "process": a new instance and runtime over the database files.
    *******************************************************************************/
   private QInstance boot() throws Exception
   {
      return (boot(new MarketingQBitConfig()));
   }



   /*******************************************************************************
    ** A fresh "process" with the given config (its backend is set here).
    *******************************************************************************/
   private QInstance boot(MarketingQBitConfig config) throws Exception
   {
      QInstance qInstance = new QInstance();
      qInstance.addBackend(new RDBMSBackendMetaData().withName("rdbms").withVendor("h2").withHostName("file").withDatabaseName(directory.resolve("marketing").toString())
         .withUsername("sa").withPassword(""));
      qInstance.withInstanceDefaultAuthentication(new QAuthenticationMetaData().withName("anonymous").withType(QAuthenticationType.FULLY_ANONYMOUS));
      qInstance.setEnforcePermissionsInCoreActions(true);
      new MarketingQBitProducer().withQBitConfig(config.withBackendName("rdbms")).produce(qInstance);
      new QInstanceValidator().validate(qInstance);
      QContext.init(qInstance, BaseMarketingTest.admin());
      return (qInstance);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private String jdbcUrl()
   {
      return ("jdbc:h2:file:" + directory.resolve("marketing") + ";MODE=MySQL");
   }



   /*******************************************************************************
    ** A new database directory with the shipped H2 DDL applied.
    *******************************************************************************/
   private void createSchema() throws Exception
   {
      directory = Files.createDirectories(Path.of("target", "h2-restart", String.valueOf(System.nanoTime())).toAbsolutePath());
      try(Connection connection = DriverManager.getConnection(jdbcUrl(), "sa", ""); Statement statement = connection.createStatement();
          InputStream ddl = getClass().getResourceAsStream("/marketing-h2.sql"))
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
   }



   /*******************************************************************************
    ** Close the database files (the restart).
    *******************************************************************************/
   private void shutdown() throws Exception
   {
      try(Connection connection = DriverManager.getConnection(jdbcUrl(), "sa", ""); Statement statement = connection.createStatement())
      {
         statement.execute("SHUTDOWN");
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testSurvivesRestart() throws Exception
   {
      createSchema();

      ///////////////////////////////
      // 1. before the restart     //
      ///////////////////////////////
      boot();
      new MarketingStarterData().load();
      Integer campaignId = MarketingStore.query(com.kingsrook.qbits.marketing.model.MarketingCampaign.TABLE_NAME, new QQueryFilter()).get(0).getValueInteger("id");
      Integer channelId  = MarketingStore.queryBy(com.kingsrook.qbits.marketing.model.MarketingChannel.TABLE_NAME, "name", "Controlled test sink").get(0).getValueInteger("id");

      Integer contentId = MarketingStore.insert(new MarketingContent().withCampaignId(campaignId).withChannelId(channelId).withTitle("Survives").withBody("Text that survives a restart.")
         .withScheduledFor(Instant.now().minusSeconds(30))).getValueInteger("id");
      ApprovalService approvals = new ApprovalService();
      approvals.submitForReview(contentId);
      approvals.approve(contentId, "ok");
      approvals.schedule(contentId);

      Integer deadContent = MarketingStore.insert(new MarketingContent().withCampaignId(campaignId).withChannelId(channelId).withTitle("Dead worker").withBody("Mid-send.")
         .withScheduledFor(Instant.now().minusSeconds(30))).getValueInteger("id");
      approvals.submitForReview(deadContent);
      approvals.approve(deadContent, null);
      approvals.schedule(deadContent);
      Integer deadExecution = MarketingStore.queryBy(MarketingExecution.TABLE_NAME, "contentId", deadContent).get(0).getValueInteger("id");
      MarketingStore.updateInternal(MarketingExecution.TABLE_NAME, new QRecord().withValue("id", deadExecution).withValue("status", ExecutionStatus.RUNNING.getId())
         .withValue("leaseOwner", "old-process").withValue("leaseExpiresAt", Instant.now().minusSeconds(5)));

      Integer runId = MarketingStore.insertInternal(new MarketingAgentRun().withCampaignId(campaignId).withTask(AgentTask.PLAN_CAMPAIGN.getId())
         .withStatus(AgentRunStatus.RUNNING.getId())).getValueInteger("id");

      int products = MarketingStore.query(MarketingProduct.TABLE_NAME, new QQueryFilter()).size();
      QContext.clear();

      ///////////////////////////////////////////////////////
      // 2. the restart: close the database files          //
      ///////////////////////////////////////////////////////
      try(Connection connection = DriverManager.getConnection(jdbcUrl(), "sa", ""); Statement statement = connection.createStatement())
      {
         statement.execute("SHUTDOWN");
      }

      ///////////////////////////////
      // 3. after the restart      //
      ///////////////////////////////
      boot();
      assertThat(MarketingStore.query(MarketingProduct.TABLE_NAME, new QQueryFilter())).hasSize(products);
      assertThat(MarketingStore.queryBy(MarketingContentRevision.TABLE_NAME, "contentId", contentId)).hasSize(1);
      QRecord approval = MarketingStore.queryBy(MarketingApproval.TABLE_NAME, "contentId", contentId).get(0);
      assertThat(approval.getValueInteger("status")).isEqualTo(ApprovalStatus.ACTIVE.getId());
      assertThat(approval.getValueString("note")).isEqualTo("ok");

      Thread.sleep(10);
      new RunProcessAction().execute(new RunProcessInput().withProcessName(MarketingTableProcesses.RUN_DUE_EXECUTIONS).withFrontendStepBehavior(RunProcessInput.FrontendStepBehavior.SKIP));
      QRecord execution = MarketingStore.queryBy(MarketingExecution.TABLE_NAME, "contentId", contentId).get(0);
      assertThat(execution.getValueInteger("status")).isEqualTo(ExecutionStatus.ACCEPTED.getId());
      assertThat(MarketingStore.queryBy(MarketingTestDelivery.TABLE_NAME, "executionId", execution.getValue("id")).get(0).getValueString("body"))
         .isEqualTo("Text that survives a restart.");

      assertThat(MarketingStore.get(MarketingExecution.TABLE_NAME, deadExecution).getValueInteger("status")).isEqualTo(ExecutionStatus.NEEDS_RECONCILIATION.getId());
      assertThat(MarketingStore.queryBy(MarketingTestDelivery.TABLE_NAME, "executionId", deadExecution)).as("not retried blindly").isEmpty();

      new ExecutionService();
      assertThat(com.kingsrook.qbits.marketing.agent.MarketingAgentService.failInterruptedRuns(Instant.now().plusSeconds(1))).isEqualTo(1);
      assertThat(MarketingStore.get(MarketingAgentRun.TABLE_NAME, runId).getValueInteger("status")).isEqualTo(AgentRunStatus.FAILED.getId());
   }

   /*******************************************************************************
    ** The daily rollup on the real RDBMS backend: it follows inserts, edits and
    ** deletes in SQL, the report's COUNT_DISTINCT and SUMs run in the database,
    ** and the rows survive a restart (and Rebuild Results changes nothing).
    *******************************************************************************/
   @Test
   void testRollupAndImporterOverRdbmsSurviveRestart() throws Exception
   {
      createSchema();
      MeasurementRollupTest.FakeImporter importer = new MeasurementRollupTest.FakeImporter("fake-analytics");
      Instant dayOne = Instant.parse("2026-10-01T10:00:00Z");
      Instant dayTwo = Instant.parse("2026-10-02T23:59:59Z");
      importer.outcomes.add(new ImportedOutcome(FunnelStage.AWARENESS, "page_view", "ga-1", "x", dayOne, 4, false, null));
      importer.outcomes.add(new ImportedOutcome(FunnelStage.AWARENESS, "page_view", "ga-2", "x", dayTwo, null, false, null));
      importer.outcomes.add(new ImportedOutcome(FunnelStage.AWARENESS, "page_view", "ga-3", "y", dayTwo, null, false, null));
      importer.outcomes.add(new ImportedOutcome(FunnelStage.AWARENESS, "page_view", "ga-4", null, dayTwo, null, true, null));

      boot(new MarketingQBitConfig().withOutcomeImporter(importer));
      Integer productId  = MarketingStore.insert(new MarketingProduct().withName("P").withSlug("p").withMaturity(ProductMaturity.RELEASED.getId())).getValueInteger("id");
      Integer campaignId = MarketingStore.insert(new MarketingCampaign().withProductId(productId).withName("C").withGoal(CampaignGoal.AWARENESS.getId())
         .withStatus(CampaignStatus.ACTIVE.getId())).getValueInteger("id");
      MarketingStore.insert(new MarketingMetric().withCampaignId(campaignId).withStage(FunnelStage.AWARENESS.getId()).withName("Views").withEventType("page_view")
         .withCountMode(CountMode.UNIQUE_ACTORS.getId()).withInstrumentationStatus(InstrumentationStatus.INSTRUMENTED.getId()));

      OutcomeImportService importService = new OutcomeImportService();
      assertThat(importService.importOutcomes(campaignId)).isEqualTo("fake-analytics: 4 imported, 0 already recorded");
      assertThat(importService.importOutcomes(campaignId)).isEqualTo("fake-analytics: 0 imported, 4 already recorded");

      MeasurementReport.Row views = MeasurementReport.forCampaign(campaignId).getRows().get(0);
      assertThat(views.rawEvents()).isEqualTo(6);
      assertThat(views.uniqueActors()).as("x on two days and y: distinct in the database").isEqualTo(2);
      assertThat(views.eventsWithoutActor()).isZero();
      List<QRecord> before = MarketingStore.queryBy(MarketingMetricDaily.TABLE_NAME, "campaignId", campaignId);
      assertThat(before).hasSize(3);
      QContext.clear();

      shutdown();

      /////////////////////////////////////
      // after the restart: same rows    //
      /////////////////////////////////////
      boot(new MarketingQBitConfig().withOutcomeImporter(importer));
      List<QRecord> after = MarketingStore.queryBy(MarketingMetricDaily.TABLE_NAME, "campaignId", campaignId);
      assertThat(after).hasSize(3);
      assertThat(after).extracting(r -> r.getValueLocalDate("dayUtc") + "|" + r.getValueBoolean("isTestData") + "|" + r.getValueInteger("rawCount") + "|" + r.getValueInteger("uniqueActors")
         + "|" + r.getValueInteger("eventsWithoutActor")).containsExactlyInAnyOrder("2026-10-01|false|4|1|0", "2026-10-02|false|2|2|0", "2026-10-02|true|1|0|1");
      assertThat(after).allSatisfy(r -> assertThat(r.getValueInteger("productId")).isEqualTo(productId));

      String rebuilt = new MeasurementRollupService().rebuild(campaignId);
      assertThat(rebuilt).isEqualTo("rebuilt 3 daily result row(s) over 2 day(s)");
      assertThat(MarketingStore.queryBy(MarketingMetricDaily.TABLE_NAME, "campaignId", campaignId)).extracting(r -> r.getValue("id"))
         .containsExactlyInAnyOrderElementsOf(after.stream().map(r -> r.getValue("id")).toList());

      /////////////////////////////////////////////////////////////
      // an edit and a delete are followed in SQL                //
      /////////////////////////////////////////////////////////////
      Integer ga2 = MarketingStore.queryBy(MarketingOutcomeEvent.TABLE_NAME, "externalEventId", "ga-2").get(0).getValueInteger("id");
      Integer ga3 = MarketingStore.queryBy(MarketingOutcomeEvent.TABLE_NAME, "externalEventId", "ga-3").get(0).getValueInteger("id");
      MarketingStore.update(MarketingOutcomeEvent.TABLE_NAME, new QRecord().withValue("id", ga2).withValue("occurredAt", dayOne));
      MarketingStore.delete(MarketingOutcomeEvent.TABLE_NAME, List.of(ga3));
      assertThat(MarketingStore.queryBy(MarketingMetricDaily.TABLE_NAME, "campaignId", campaignId)).extracting(r -> r.getValueLocalDate("dayUtc") + "|" + r.getValueBoolean("isTestData")
         + "|" + r.getValueInteger("rawCount") + "|" + r.getValueInteger("uniqueActors")).containsExactlyInAnyOrder("2026-10-01|false|5|1", "2026-10-02|true|1|0");
      MeasurementReport.Row afterViews = MeasurementReport.forCampaign(campaignId).getRows().get(0);
      assertThat(afterViews.rawEvents()).isEqualTo(5);
      assertThat(afterViews.uniqueActors()).isEqualTo(1);
   }

}
