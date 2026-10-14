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


import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeMap;
import com.kingsrook.qbits.marketing.measurement.ImportedOutcome;
import com.kingsrook.qbits.marketing.measurement.MarketingOutcomeImporter;
import com.kingsrook.qbits.marketing.measurement.MeasurementReport;
import com.kingsrook.qbits.marketing.measurement.MeasurementRollupService;
import com.kingsrook.qbits.marketing.measurement.OutcomeImportService;
import com.kingsrook.qbits.marketing.model.MarketingMetric;
import com.kingsrook.qbits.marketing.model.MarketingMetricDaily;
import com.kingsrook.qbits.marketing.model.MarketingOutcomeEvent;
import com.kingsrook.qbits.marketing.model.enums.CountMode;
import com.kingsrook.qbits.marketing.model.enums.FunnelStage;
import com.kingsrook.qbits.marketing.model.enums.InstrumentationStatus;
import com.kingsrook.qbits.marketing.processes.MarketingActions;
import com.kingsrook.qbits.marketing.security.MarketingPermissions;
import com.kingsrook.qbits.marketing.widgets.MarketingWidgets;
import com.kingsrook.qqq.backend.core.actions.dashboard.RenderWidgetAction;
import com.kingsrook.qqq.backend.core.actions.processes.QProcessCallbackFactory;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLine;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.TableData;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** The persisted daily rollup of outcome events (marketing_metric_daily): it
 ** matches the raw computation across inserts, edits, deletes and test data;
 ** Rebuild Results is idempotent; the campaign-wide unique count is not summed
 ** from days; and the importer SPI stores outcomes through the normal dedupe.
 *******************************************************************************/
class MeasurementRollupTest extends BaseMarketingTest
{
   private static final Instant DAY_ONE = Instant.parse("2026-10-01T10:00:00Z");
   private static final Instant DAY_TWO = Instant.parse("2026-10-02T23:59:59Z");

   private final FakeImporter importer = new FakeImporter("fake-analytics");

   private Integer campaignId;



   /*******************************************************************************
    ** An importer that returns what the test queued, and remembers its "since".
    *******************************************************************************/
   static class FakeImporter implements MarketingOutcomeImporter
   {
      private final String name;
      final List<ImportedOutcome> outcomes = new ArrayList<>();
      final List<Instant>         sinces   = new ArrayList<>();
      boolean                     fail     = false;



      /*******************************************************************************
       **
       *******************************************************************************/
      FakeImporter(String name)
      {
         this.name = name;
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public String getName()
      {
         return (name);
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public List<ImportedOutcome> fetch(QRecord campaign, Instant since) throws QException
      {
         sinces.add(since);
         if(fail)
         {
            throw (new QUserFacingException("the analytics export is unreachable"));
         }
         return (new ArrayList<>(outcomes));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   protected void customizeConfig(MarketingQBitConfig config)
   {
      config.withOutcomeImporter(importer);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void beforeEach() throws QException
   {
      campaignId = campaign(product("p1"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private Integer event(FunnelStage stage, String type, String externalId, String actor, Integer quantity, Instant occurredAt, boolean test) throws QException
   {
      return (MarketingStore.insert(new MarketingOutcomeEvent().withCampaignId(campaignId).withStage(stage.getId()).withEventType(type).withSource("site")
         .withExternalEventId(externalId).withActorKey(actor).withQuantity(quantity).withOccurredAt(occurredAt).withIsTestData(test)).getValueInteger("id"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void metric(FunnelStage stage, String eventType, CountMode mode, InstrumentationStatus status) throws QException
   {
      MarketingStore.insert(new MarketingMetric().withCampaignId(campaignId).withStage(stage.getId()).withName(eventType).withEventType(eventType)
         .withCountMode(mode.getId()).withInstrumentationStatus(status.getId()));
   }



   /*******************************************************************************
    ** The raw computation, straight from the events: "day|stage|test|type" to
    ** "raw|uniqueActors|withoutActor".
    *******************************************************************************/
   private Map<String, String> rawComputation() throws QException
   {
      Map<String, int[]>       counts = new TreeMap<>();
      Map<String, Set<String>> actors = new TreeMap<>();
      for(QRecord event : MarketingStore.queryBy(MarketingOutcomeEvent.TABLE_NAME, "campaignId", campaignId))
      {
         LocalDate day = event.getValueInstant("occurredAt").atZone(ZoneOffset.UTC).toLocalDate();
         String    key = day + "|" + event.getValueInteger("stage") + "|" + Boolean.TRUE.equals(event.getValueBoolean("isTestData")) + "|" + event.getValueString("eventType");
         int[]     sum = counts.computeIfAbsent(key, k -> new int[2]);
         sum[0] += event.getValueInteger("quantity") == null ? 1 : event.getValueInteger("quantity");
         String actor = event.getValueString("actorKey");
         if(actor == null || actor.isBlank())
         {
            sum[1]++;
         }
         else
         {
            actors.computeIfAbsent(key, k -> new HashSet<>()).add(actor);
         }
      }

      Map<String, String> expected = new TreeMap<>();
      counts.forEach((key, sum) -> expected.put(key, sum[0] + "|" + actors.getOrDefault(key, Set.of()).size() + "|" + sum[1]));
      return (expected);
   }



   /*******************************************************************************
    ** What the rollup table holds, in the same shape.
    *******************************************************************************/
   private Map<String, String> rollupRows() throws QException
   {
      Map<String, String> rows = new TreeMap<>();
      for(QRecord row : MarketingStore.queryBy(MarketingMetricDaily.TABLE_NAME, "campaignId", campaignId))
      {
         rows.put(row.getValueLocalDate("dayUtc") + "|" + row.getValueInteger("funnelStage") + "|" + Boolean.TRUE.equals(row.getValueBoolean("isTestData")) + "|" + row.getValueString("eventType"),
            row.getValueInteger("rawCount") + "|" + row.getValueInteger("uniqueActors") + "|" + row.getValueInteger("eventsWithoutActor"));
      }
      return (rows);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void assertRollupMatchesRaw() throws QException
   {
      assertThat(rollupRows()).isEqualTo(rawComputation());
   }



   /*******************************************************************************
    ** The rollup follows every insert, edit (quantity, actor, type, day, test
    ** flag) and delete, including the last event of a day and test-data rows.
    *******************************************************************************/
   @Test
   void testRollupMatchesRawAcrossInsertsEditsAndDeletes() throws QException
   {
      Integer v1 = event(FunnelStage.AWARENESS, "page_view", "v1", "x", null, DAY_ONE, false);
      Integer v2 = event(FunnelStage.AWARENESS, "page_view", "v2", "x", 3, DAY_ONE, false);
      Integer v3 = event(FunnelStage.AWARENESS, "page_view", "v3", null, null, DAY_ONE, false);
      Integer v4 = event(FunnelStage.AWARENESS, "page_view", "v4", "y", 2, DAY_TWO, false);
      Integer r1 = event(FunnelStage.ACTIVATION, "first_run", "r1", "x", null, DAY_ONE, false);
      Integer t1 = event(FunnelStage.AWARENESS, "page_view", "t1", "z", null, DAY_ONE, true);
      assertRollupMatchesRaw();
      assertThat(rollupRows()).containsEntry("2026-10-01|" + FunnelStage.AWARENESS.getId() + "|false|page_view", "5|1|1")
         .containsEntry("2026-10-01|" + FunnelStage.AWARENESS.getId() + "|true|page_view", "1|1|0")
         .containsEntry("2026-10-02|" + FunnelStage.AWARENESS.getId() + "|false|page_view", "2|1|0")
         .hasSize(4);

      /////////////////////////////////////////////////////////////////
      // edits: quantity, actor, type, day, test flag                //
      /////////////////////////////////////////////////////////////////
      MarketingStore.update(MarketingOutcomeEvent.TABLE_NAME, new QRecord().withValue("id", v2).withValue("quantity", 10));
      assertRollupMatchesRaw();
      MarketingStore.update(MarketingOutcomeEvent.TABLE_NAME, new QRecord().withValue("id", v3).withValue("actorKey", "w"));
      assertRollupMatchesRaw();
      MarketingStore.update(MarketingOutcomeEvent.TABLE_NAME, new QRecord().withValue("id", r1).withValue("eventType", "second_run"));
      assertRollupMatchesRaw();
      MarketingStore.update(MarketingOutcomeEvent.TABLE_NAME, new QRecord().withValue("id", v1).withValue("occurredAt", DAY_TWO));
      assertRollupMatchesRaw();
      MarketingStore.update(MarketingOutcomeEvent.TABLE_NAME, new QRecord().withValue("id", t1).withValue("isTestData", false));
      assertRollupMatchesRaw();
      MarketingStore.update(MarketingOutcomeEvent.TABLE_NAME, new QRecord().withValue("id", v4).withValue("isTestData", true));
      assertRollupMatchesRaw();

      ///////////////////////////////////////////////////////////////////////
      // an event cannot lose its time (the rollup is by day)               //
      ///////////////////////////////////////////////////////////////////////
      assertThatThrownBy(() -> MarketingStore.update(MarketingOutcomeEvent.TABLE_NAME, new QRecord().withValue("id", v2).withValue("occurredAt", null)))
         .hasMessageContaining("needs the time");

      /////////////////////////////////////
      // deletes, down to nothing        //
      /////////////////////////////////////
      MarketingStore.delete(MarketingOutcomeEvent.TABLE_NAME, List.of(v2));
      assertRollupMatchesRaw();
      MarketingStore.delete(MarketingOutcomeEvent.TABLE_NAME, List.of(v1, v3, v4, r1, t1));
      assertThat(rollupRows()).isEmpty();
      assertRollupMatchesRaw();
   }



   /*******************************************************************************
    ** The rollup rows are system-written: a direct write is refused, and each
    ** row carries the product of its campaign (for the product record lock).
    *******************************************************************************/
   @Test
   void testRollupIsSystemWrittenAndCarriesTheProduct() throws QException
   {
      event(FunnelStage.AWARENESS, "page_view", "v1", "x", null, DAY_ONE, false);
      QRecord row = MarketingStore.queryBy(MarketingMetricDaily.TABLE_NAME, "campaignId", campaignId).get(0);
      assertThat(row.getValueInteger("productId")).isEqualTo(MarketingStore.get("marketing_campaign", campaignId).getValueInteger("productId"));
      assertThat(row.getValueInstant("recomputedAt")).isNotNull();

      assertThatThrownBy(() -> MarketingStore.insert(new MarketingMetricDaily().withCampaignId(campaignId).withDayUtc(LocalDate.of(2026, 10, 3))
         .withFunnelStage(FunnelStage.AWARENESS.getId()).withEventType("x").withIsTestData(false).withRawCount(99)))
         .hasMessageContaining("written only by Marketing");
      assertThatThrownBy(() -> MarketingStore.update(MarketingMetricDaily.TABLE_NAME, new QRecord().withValue("id", row.getValue("id")).withValue("rawCount", 99)))
         .hasMessageContaining("written only by Marketing");
      assertThatThrownBy(() -> MarketingStore.delete(MarketingMetricDaily.TABLE_NAME, List.of(row.getValue("id"))))
         .hasMessageContaining("written only by Marketing");
   }



   /*******************************************************************************
    ** Rebuild Results repairs a damaged rollup (wrong numbers, missing rows, a
    ** stale row) from the raw events, and a second run changes nothing.
    *******************************************************************************/
   @Test
   void testRebuildIsIdempotent() throws QException
   {
      event(FunnelStage.AWARENESS, "page_view", "v1", "x", null, DAY_ONE, false);
      event(FunnelStage.AWARENESS, "page_view", "v2", "y", 4, DAY_TWO, false);
      event(FunnelStage.ACTIVATION, "first_run", "r1", "x", null, DAY_ONE, true);
      Map<String, String> expected = rawComputation();
      assertThat(rollupRows()).isEqualTo(expected);

      ///////////////////////////////////////////////////////////
      // damage: a wrong number, a missing row, a stale row    //
      ///////////////////////////////////////////////////////////
      List<QRecord> rows = MarketingStore.queryBy(MarketingMetricDaily.TABLE_NAME, "campaignId", campaignId);
      MarketingStore.updateInternal(MarketingMetricDaily.TABLE_NAME, new QRecord().withValue("id", rows.get(0).getValue("id")).withValue("rawCount", 999));
      MarketingStore.deleteInternal(MarketingMetricDaily.TABLE_NAME, List.<Serializable>of(rows.get(1).getValue("id")));
      MarketingStore.insertInternal(new MarketingMetricDaily().withCampaignId(campaignId).withDayUtc(LocalDate.of(2026, 9, 1)).withFunnelStage(FunnelStage.AWARENESS.getId())
         .withEventType("ghost").withIsTestData(false).withRawCount(7).withUniqueActors(7).withEventsWithoutActor(0));
      assertThat(rollupRows()).isNotEqualTo(expected);

      String first = new MeasurementRollupService().rebuild(campaignId);
      assertThat(first).isEqualTo("rebuilt 3 daily result row(s) over 3 day(s)");
      assertThat(rollupRows()).isEqualTo(expected);

      List<QRecord> afterFirst = MarketingStore.queryBy(MarketingMetricDaily.TABLE_NAME, "campaignId", campaignId);
      String        second     = new MeasurementRollupService().rebuild(campaignId);
      List<QRecord> afterSecond = MarketingStore.queryBy(MarketingMetricDaily.TABLE_NAME, "campaignId", campaignId);
      assertThat(second).isEqualTo("rebuilt 3 daily result row(s) over 2 day(s)");
      assertThat(rollupRows()).isEqualTo(expected);
      assertThat(afterSecond).hasSameSizeAs(afterFirst);
      for(int i = 0; i < afterFirst.size(); i++)
      {
         assertThat(afterSecond.get(i).getValue("id")).isEqualTo(afterFirst.get(i).getValue("id"));
         assertThat(afterSecond.get(i).getValueInstant("recomputedAt")).as("an unchanged row is not rewritten").isEqualTo(afterFirst.get(i).getValueInstant("recomputedAt"));
      }
   }



   /*******************************************************************************
    ** Rebuild Results is a campaign record action with standard summary lines,
    ** and needs the same permission as Update Results from CRM.
    *******************************************************************************/
   @Test
   @SuppressWarnings("unchecked")
   void testRebuildResultsAction() throws QException
   {
      event(FunnelStage.AWARENESS, "page_view", "v1", "x", null, DAY_ONE, false);
      MarketingStore.deleteInternal(MarketingMetricDaily.TABLE_NAME, List.<Serializable>of(MarketingStore.queryBy(MarketingMetricDaily.TABLE_NAME, "campaignId", campaignId).get(0).getValue("id")));
      assertThat(rollupRows()).isEmpty();

      RunProcessOutput output = new RunProcessAction().execute(new RunProcessInput().withProcessName(MarketingActions.REBUILD_RESULTS)
         .withCallback(QProcessCallbackFactory.forPrimaryKey("id", campaignId)).withFrontendStepBehavior(RunProcessInput.FrontendStepBehavior.SKIP));
      List<ProcessSummaryLine> lines = (List<ProcessSummaryLine>) (List<?>) output.getValues().get("processResults");
      assertThat(lines).anySatisfy(l -> assertThat(l.getMessage()).isEqualTo("rebuilt 1 daily result row(s) over 1 day(s)"));
      assertRollupMatchesRaw();

      as(user("reader", MarketingPermissions.APP_ACCESS, MarketingPermissions.READ));
      assertThatThrownBy(() -> new MeasurementRollupService().rebuild(campaignId)).hasMessageContaining(MarketingPermissions.PREPARE);
      as(admin());
   }



   /*******************************************************************************
    ** The report reads the rollup and a backend COUNT_DISTINCT: an actor seen
    ** on two days is one unique actor for the campaign (not the sum of days),
    ** and test data and not-measured stay as they were.
    *******************************************************************************/
   @Test
   void testReportUsesTheRollupAndCountsUniqueActorsAcrossDays() throws QException
   {
      metric(FunnelStage.AWARENESS, "page_view", CountMode.UNIQUE_ACTORS, InstrumentationStatus.PARTIAL);
      metric(FunnelStage.ACTIVATION, "first_run", CountMode.RAW_EVENTS, InstrumentationStatus.INSTRUMENTED);
      metric(FunnelStage.REPEAT_USE, "second_run", CountMode.UNIQUE_ACTORS, InstrumentationStatus.NOT_INSTRUMENTED);

      event(FunnelStage.AWARENESS, "page_view", "v1", "x", null, DAY_ONE, false);
      event(FunnelStage.AWARENESS, "page_view", "v2", "x", null, DAY_TWO, false);
      event(FunnelStage.AWARENESS, "page_view", "v3", "y", 2, DAY_TWO, false);
      event(FunnelStage.AWARENESS, "page_view", "v4", null, null, DAY_ONE, false);
      event(FunnelStage.AWARENESS, "page_view", "v5", "   ", null, DAY_ONE, false);
      event(FunnelStage.AWARENESS, "page_view", "tv", "tester", null, DAY_ONE, true);
      event(FunnelStage.ACTIVATION, "first_run", "r1", "x", 5, DAY_ONE, false);
      event(FunnelStage.AWARENESS, "star", "s1", "y", null, DAY_ONE, false);

      MeasurementReport report = MeasurementReport.forCampaign(campaignId);
      MeasurementReport.Row views = report.getRows().stream().filter(r -> r.eventType().equals("page_view")).findFirst().orElseThrow();
      MeasurementReport.Row runs  = report.getRows().stream().filter(r -> r.eventType().equals("first_run")).findFirst().orElseThrow();
      MeasurementReport.Row again = report.getRows().stream().filter(r -> r.eventType().equals("second_run")).findFirst().orElseThrow();

      assertThat(views.rawEvents()).isEqualTo(6);
      assertThat(views.uniqueActors()).as("x on two days is one actor").isEqualTo(2);
      assertThat(views.eventsWithoutActor()).isEqualTo(2);
      assertThat(views.headline()).isEqualTo("2 unique actors");
      assertThat(views.note()).contains("lower bound").contains("2 event(s) have no actor");
      assertThat(runs.rawEvents()).isEqualTo(5);
      assertThat(runs.headline()).isEqualTo("5 events");
      assertThat(again.headline()).isEqualTo(MeasurementReport.NOT_MEASURED);
      assertThat(report.getTestEvents()).isEqualTo(1);
      assertThat(report.getLimitations()).anyMatch(l -> l.contains("1 test event(s)")).anyMatch(l -> l.contains("AWARENESS/star")).anyMatch(l -> l.contains("lower bound"));
      assertThat(report.toMap()).containsEntry("realEventCount", 7).containsEntry("testEventCountExcluded", 1);
   }



   /*******************************************************************************
    ** An importer's outcomes go through the normal insert path: a repeat is
    ** dropped by (source, external id), the importer is told what it already
    ** delivered, and the daily rollup follows.
    *******************************************************************************/
   @Test
   void testImporterRepeatIsDeduplicated() throws QException
   {
      importer.outcomes.add(new ImportedOutcome(FunnelStage.AWARENESS, "page_view", "ga-1", "x", DAY_ONE, 4, false, null));
      importer.outcomes.add(new ImportedOutcome(FunnelStage.AWARENESS, "page_view", "ga-2", "y", DAY_TWO, null, false, "second day"));
      importer.outcomes.add(new ImportedOutcome(FunnelStage.ENGAGEMENT, "signup", "ga-3", "x", DAY_TWO, 1, true, null));
      importer.outcomes.add(new ImportedOutcome(null, "bad", "ga-4", null, null, null, null, null));

      OutcomeImportService service = new OutcomeImportService();
      assertThat(service.blocker()).isNull();
      assertThat(service.importOutcomes(campaignId)).isEqualTo("fake-analytics: 3 imported, 0 already recorded, 1 invalid (skipped)");
      assertThat(service.importOutcomes(campaignId)).isEqualTo("fake-analytics: 0 imported, 3 already recorded, 1 invalid (skipped)");

      List<QRecord> events = MarketingStore.queryBy(MarketingOutcomeEvent.TABLE_NAME, "campaignId", campaignId);
      assertThat(events).hasSize(3).allSatisfy(e -> assertThat(e.getValueString("source")).isEqualTo("fake-analytics"));
      assertThat(events).anySatisfy(e -> assertThat(e.getValueInteger("quantity")).isEqualTo(4));
      assertThat(importer.sinces).containsExactly(null, DAY_TWO);
      assertRollupMatchesRaw();
      assertThat(rollupRows()).hasSize(3);
   }



   /*******************************************************************************
    ** Import Outcomes is a campaign record action with standard lines; one
    ** failing importer is named and does not stop the others.
    *******************************************************************************/
   @Test
   @SuppressWarnings("unchecked")
   void testImportOutcomesActionAndFailingImporter() throws QException
   {
      importer.outcomes.add(new ImportedOutcome(FunnelStage.AWARENESS, "page_view", "ga-1", "x", DAY_ONE, null, false, null));

      RunProcessOutput output = new RunProcessAction().execute(new RunProcessInput().withProcessName(MarketingActions.IMPORT_OUTCOMES)
         .withCallback(QProcessCallbackFactory.forPrimaryKey("id", campaignId)).withFrontendStepBehavior(RunProcessInput.FrontendStepBehavior.SKIP));
      List<ProcessSummaryLine> lines = (List<ProcessSummaryLine>) (List<?>) output.getValues().get("processResults");
      assertThat(lines).anySatisfy(l -> assertThat(l.getMessage()).isEqualTo("fake-analytics: 1 imported, 0 already recorded"));

      importer.fail = true;
      output = new RunProcessAction().execute(new RunProcessInput().withProcessName(MarketingActions.IMPORT_OUTCOMES)
         .withCallback(QProcessCallbackFactory.forPrimaryKey("id", campaignId)).withFrontendStepBehavior(RunProcessInput.FrontendStepBehavior.SKIP));
      lines = (List<ProcessSummaryLine>) (List<?>) output.getValues().get("processResults");
      assertThat(lines).anySatisfy(l -> assertThat(l.getMessage()).contains("not done: fake-analytics: failed (the analytics export is unreachable)"));
      assertThat(MarketingStore.queryBy(MarketingOutcomeEvent.TABLE_NAME, "campaignId", campaignId)).hasSize(1);
   }



   /*******************************************************************************
    ** Registered importers are listed by the Configuration Status widget (and
    ** its absence says so); the names Marketing uses itself are reserved.
    *******************************************************************************/
   @Test
   void testConfigurationStatusListsImportersAndNamesAreChecked() throws QException
   {
      TableData status = (TableData) new RenderWidgetAction().execute(new RenderWidgetInput().withWidgetMetaData(qInstance.getWidget(MarketingWidgets.CONFIGURATION))).getWidgetData();
      assertThat(status.getRows()).anySatisfy(r -> assertThat(String.valueOf(r.get("integration"))).contains("Outcome importer").contains("fake-analytics"));

      List<String> errors = new ArrayList<>();
      new MarketingQBitConfig().withBackendName(BACKEND).withOutcomeImporter(new FakeImporter("crm")).withOutcomeImporter(new FakeImporter("dup"))
         .withOutcomeImporter(new FakeImporter("dup")).validate(qInstance, errors);
      assertThat(errors).anyMatch(e -> e.contains("[crm] is reserved")).anyMatch(e -> e.contains("duplicate outcome importer name [dup]"));
   }

}
