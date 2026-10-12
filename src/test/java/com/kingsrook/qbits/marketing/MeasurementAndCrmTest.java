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


import java.util.List;
import com.kingsrook.qbits.marketing.measurement.CrmResultsService;
import com.kingsrook.qbits.marketing.measurement.MeasurementReport;
import com.kingsrook.qbits.marketing.model.MarketingCampaign;
import com.kingsrook.qbits.marketing.model.MarketingMetric;
import com.kingsrook.qbits.marketing.model.MarketingOutcomeEvent;
import com.kingsrook.qbits.marketing.model.enums.CountMode;
import com.kingsrook.qbits.marketing.model.enums.FunnelStage;
import com.kingsrook.qbits.marketing.model.enums.InstrumentationStatus;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Measurement honesty and the CRM integration: event deduplication, raw
 ** events vs unique actors, missing instrumentation, and leads only by an
 ** explicit rule.
 *******************************************************************************/
class MeasurementAndCrmTest extends BaseMarketingTest
{
   private Integer campaignId;



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
   private void metric(FunnelStage stage, String eventType, CountMode mode, InstrumentationStatus status) throws QException
   {
      MarketingStore.insert(new MarketingMetric().withCampaignId(campaignId).withStage(stage.getId()).withName(eventType).withEventType(eventType)
         .withCountMode(mode.getId()).withInstrumentationStatus(status.getId()));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private QRecord event(FunnelStage stage, String type, String source, String externalId, String actor)
   {
      return (new MarketingOutcomeEvent().withCampaignId(campaignId).withStage(stage.getId()).withEventType(type).withSource(source).withExternalEventId(externalId)
         .withActorKey(actor).withIsTestData(false).toQRecord());
   }



   /*******************************************************************************
    ** The same (source, external id) is stored once, through the standard
    ** insert (the API and admin path).
    *******************************************************************************/
   @Test
   void testEventDeduplication() throws QException
   {
      InsertOutput first  = new InsertAction().execute(new InsertInput(MarketingOutcomeEvent.TABLE_NAME).withRecords(List.of(
         event(FunnelStage.ENGAGEMENT, "signup", "site", "evt-1", "a"),
         event(FunnelStage.ENGAGEMENT, "signup", "site", "evt-2", "b"))));
      InsertOutput replay = new InsertAction().execute(new InsertInput(MarketingOutcomeEvent.TABLE_NAME).withRecords(List.of(
         event(FunnelStage.ENGAGEMENT, "signup", "site", "evt-1", "a"),
         event(FunnelStage.ENGAGEMENT, "signup", "other-source", "evt-1", "a"))));

      assertThat(first.getRecords()).allMatch(r -> r.getErrors() == null || r.getErrors().isEmpty());
      assertThat(replay.getRecords().get(0).getErrors()).isNotEmpty();
      assertThat(replay.getRecords().get(1).getErrors()).as("another source is another event").isNullOrEmpty();
      assertThat(MarketingStore.queryBy(MarketingOutcomeEvent.TABLE_NAME, "campaignId", campaignId)).hasSize(3);
   }



   /*******************************************************************************
    ** Raw events and unique actors are separate; an uninstrumented metric is
    ** unknown (never 0); stages without a metric are listed as limitations.
    *******************************************************************************/
   @Test
   void testRawVersusUniqueAndMissingInstrumentation() throws QException
   {
      metric(FunnelStage.AWARENESS, "page_view", CountMode.RAW_EVENTS, InstrumentationStatus.INSTRUMENTED);
      metric(FunnelStage.ACTIVATION, "first_run", CountMode.UNIQUE_ACTORS, InstrumentationStatus.INSTRUMENTED);
      metric(FunnelStage.REPEAT_USE, "second_run", CountMode.UNIQUE_ACTORS, InstrumentationStatus.NOT_INSTRUMENTED);

      MarketingStore.insert(MarketingOutcomeEvent.TABLE_NAME, event(FunnelStage.AWARENESS, "page_view", "site", "v1", "x"));
      MarketingStore.insert(MarketingOutcomeEvent.TABLE_NAME, event(FunnelStage.AWARENESS, "page_view", "site", "v2", "x"));
      MarketingStore.insert(MarketingOutcomeEvent.TABLE_NAME, event(FunnelStage.AWARENESS, "page_view", "site", "v3", null));
      MarketingStore.insert(MarketingOutcomeEvent.TABLE_NAME, event(FunnelStage.ACTIVATION, "first_run", "app", "r1", "x"));
      MarketingStore.insert(MarketingOutcomeEvent.TABLE_NAME, event(FunnelStage.ACTIVATION, "first_run", "app", "r2", "x"));
      MarketingStore.insert(MarketingOutcomeEvent.TABLE_NAME, event(FunnelStage.AWARENESS, "star", "github", "s1", "y"));

      MeasurementReport report = MeasurementReport.forCampaign(campaignId);
      MeasurementReport.Row views = report.getRows().stream().filter(r -> r.eventType().equals("page_view")).findFirst().orElseThrow();
      MeasurementReport.Row runs  = report.getRows().stream().filter(r -> r.eventType().equals("first_run")).findFirst().orElseThrow();
      MeasurementReport.Row again = report.getRows().stream().filter(r -> r.eventType().equals("second_run")).findFirst().orElseThrow();

      assertThat(views.rawEvents()).isEqualTo(3);
      assertThat(views.uniqueActors()).isEqualTo(1);
      assertThat(runs.rawEvents()).isEqualTo(2);
      assertThat(runs.headline()).isEqualTo("1 unique actors");
      assertThat(again.rawEvents()).isNull();
      assertThat(again.headline()).isEqualTo(MeasurementReport.NOT_MEASURED);
      assertThat(report.getLimitations()).anyMatch(l -> l.contains("unknown (not zero)"))
         .anyMatch(l -> l.contains("No metric is defined for Engagement"))
         .anyMatch(l -> l.contains("AWARENESS/star"))
         .anyMatch(l -> l.contains("not adoption"));
   }



   /*******************************************************************************
    ** CRM form submissions are imported once; leads only by the explicit
    ** rule, through the CRM's activity workflow.
    *******************************************************************************/
   @Test
   void testCrmImportAndLeadQualification() throws QException
   {
      crm.withContact(1, "a@example.org", true, false).withContact(2, "b@example.org", false, false)
         .withSubmission(100, 1, "test-campaign").withSubmission(101, 1, "test-campaign").withSubmission(102, 2, "test-campaign")
         .withSubmission(103, null, "test-campaign").withSubmission(104, 1, "another-campaign");

      CrmResultsService service = new CrmResultsService();
      String first  = service.updateFromCrm(campaignId);
      String second = service.updateFromCrm(campaignId);
      assertThat(first).contains("4 form submission(s) imported").contains("Lead qualification is off");
      assertThat(second).contains("0 form submission(s) imported, 4 already recorded");
      assertThat(crm.activities).as("no rule, no leads").isEmpty();

      MarketingStore.update(MarketingCampaign.TABLE_NAME, new QRecord().withValue("id", campaignId).withValue("leadQualificationEnabled", true)
         .withValue("qualifyMinFormSubmissions", 2).withValue("qualifyRequireEmailConsent", true));
      String qualified = service.updateFromCrm(campaignId);
      assertThat(qualified).contains("1 lead(s) newly qualified").contains("1 contact(s) below the rule");
      assertThat(crm.activities).containsExactly("1|Marketing-qualified lead: Campaign");

      assertThat(service.updateFromCrm(campaignId)).contains("0 lead(s) newly qualified");
      assertThat(crm.activities).hasSize(1);

      List<QRecord> leads = MarketingStore.queryBy(MarketingOutcomeEvent.TABLE_NAME, "eventType", CrmResultsService.LEAD_EVENT);
      assertThat(leads).hasSize(1);
      assertThat(leads.get(0).getValueString("actorKey")).isEqualTo("crm-contact:1");
   }

}
