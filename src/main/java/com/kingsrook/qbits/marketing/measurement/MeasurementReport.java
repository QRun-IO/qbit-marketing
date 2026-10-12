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

package com.kingsrook.qbits.marketing.measurement;


import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import com.kingsrook.qbits.marketing.MarketingStore;
import com.kingsrook.qbits.marketing.model.MarketingMetric;
import com.kingsrook.qbits.marketing.model.MarketingOutcomeEvent;
import com.kingsrook.qbits.marketing.model.enums.CountMode;
import com.kingsrook.qbits.marketing.model.enums.FunnelStage;
import com.kingsrook.qbits.marketing.model.enums.InstrumentationStatus;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.data.QRecord;


/*******************************************************************************
 ** A campaign's results, per stage, as honestly as the data allows:
 **
 ** - Awareness, engagement, activation, repeat use and qualified demand are
 **   reported separately; delivery (provider acceptance) is its own stage and
 **   never counts as reach or engagement.
 ** - Each metric shows raw events and unique actors (distinct actorKey);
 **   events without an actor are counted as raw events only.
 ** - A metric that is not instrumented shows "Not measured", never 0.
 ** - Stages with no metric say so.
 ** - Test data (controlled test transport, fixtures) is reported apart and
 **   never mixed into real numbers.
 *******************************************************************************/
public class MeasurementReport
{
   public static final String NOT_MEASURED = "Not measured";

   private final List<Row>    rows        = new ArrayList<>();
   private final List<String> limitations = new ArrayList<>();
   private int                testEvents  = 0;
   private int                realEvents  = 0;



   /*******************************************************************************
    ** One metric's line.
    **
    ** @param rawEvents    sum of quantities (null when not measured)
    ** @param uniqueActors distinct actor keys (null when not measured)
    *******************************************************************************/
   public record Row(FunnelStage stage, String metric, String eventType, CountMode countMode, InstrumentationStatus instrumentation, Integer target,
                     Integer rawEvents, Integer uniqueActors, Integer eventsWithoutActor, String note)
   {
      /*******************************************************************************
       ** The headline value as text ("Not measured" for missing instrumentation).
       *******************************************************************************/
      public String headline()
      {
         if(rawEvents == null)
         {
            return (NOT_MEASURED);
         }
         return (countMode == CountMode.UNIQUE_ACTORS ? uniqueActors + " unique actors" : rawEvents + " events");
      }
   }



   /*******************************************************************************
    ** Build the report of a campaign.
    *******************************************************************************/
   public static MeasurementReport forCampaign(Integer campaignId) throws QException
   {
      MeasurementReport report  = new MeasurementReport();
      List<QRecord>     metrics = MarketingStore.queryBy(MarketingMetric.TABLE_NAME, "campaignId", campaignId);
      List<QRecord>     events  = MarketingStore.queryBy(MarketingOutcomeEvent.TABLE_NAME, "campaignId", campaignId);

      List<QRecord> realEvents = new ArrayList<>();
      for(QRecord event : events)
      {
         if(Boolean.TRUE.equals(event.getValueBoolean("isTestData")))
         {
            report.testEvents++;
         }
         else
         {
            realEvents.add(event);
         }
      }
      report.realEvents = realEvents.size();

      Set<FunnelStage> stagesWithMetrics = new HashSet<>();
      Set<String>      plannedTypes      = new HashSet<>();
      for(QRecord metricRecord : metrics)
      {
         MarketingMetric       metric          = new MarketingMetric(metricRecord);
         FunnelStage           stage           = FunnelStage.getById(metric.getStage());
         InstrumentationStatus instrumentation = InstrumentationStatus.getById(metric.getInstrumentationStatus());
         CountMode             countMode       = CountMode.getById(metric.getCountMode());
         stagesWithMetrics.add(stage);
         plannedTypes.add(stage.name() + "/" + metric.getEventType());

         if(instrumentation == InstrumentationStatus.NOT_INSTRUMENTED)
         {
            report.rows.add(new Row(stage, metric.getName(), metric.getEventType(), countMode, instrumentation, metric.getTarget(), null, null, null,
               "No instrumentation: this is unknown, not zero."));
            report.limitations.add(metric.getName() + " is not instrumented, so its result is unknown (not zero).");
            continue;
         }

         int         raw        = 0;
         int         noActor    = 0;
         Set<String> actors     = new LinkedHashSet<>();
         for(QRecord event : realEvents)
         {
            if(stage.getId().equals(event.getValueInteger("stage")) && metric.getEventType().equals(event.getValueString("eventType")))
            {
               Integer quantity = event.getValueInteger("quantity");
               raw += quantity == null ? 1 : quantity;
               String actor = event.getValueString("actorKey");
               if(actor == null || actor.isBlank())
               {
                  noActor++;
               }
               else
               {
                  actors.add(actor);
               }
            }
         }

         String note = instrumentation == InstrumentationStatus.PARTIAL ? "Partially instrumented: a lower bound." : null;
         if(instrumentation == InstrumentationStatus.PARTIAL)
         {
            report.limitations.add(metric.getName() + " is only partially instrumented; treat it as a lower bound.");
         }
         if(countMode == CountMode.UNIQUE_ACTORS && noActor > 0)
         {
            note = (note == null ? "" : note + " ") + noActor + " event(s) have no actor and are not in the unique count.";
         }
         report.rows.add(new Row(stage, metric.getName(), metric.getEventType(), countMode, instrumentation, metric.getTarget(), raw, actors.size(), noActor, note));
      }

      for(FunnelStage stage : FunnelStage.values())
      {
         if(!stagesWithMetrics.contains(stage))
         {
            report.limitations.add("No metric is defined for " + stage.getLabel() + ".");
         }
      }

      Map<String, Integer> unplanned = new LinkedHashMap<>();
      for(QRecord event : realEvents)
      {
         FunnelStage stage = FunnelStage.getById(event.getValueInteger("stage"));
         String      key   = (stage == null ? "?" : stage.name()) + "/" + event.getValueString("eventType");
         if(!plannedTypes.contains(key))
         {
            unplanned.merge(key, 1, Integer::sum);
         }
      }
      unplanned.forEach((key, count) -> report.limitations.add(count + " recorded event(s) of type " + key + " have no metric in the measurement plan."));

      report.limitations.add("Provider acceptance (Delivery) is not delivery, reading or engagement.");
      report.limitations.add("Visits, stars and downloads are awareness or interest signals, not adoption; activation and repeat use need their own events.");
      if(report.testEvents > 0)
      {
         report.limitations.add(report.testEvents + " test event(s) (controlled test transport or fixtures) are excluded from these numbers.");
      }
      return (report);
   }



   /*******************************************************************************
    ** The report as plain data (for the agent's tool and the widget).
    *******************************************************************************/
   public Map<String, Object> toMap()
   {
      List<Map<String, Object>> list = new ArrayList<>();
      for(Row row : rows)
      {
         Map<String, Object> map = new LinkedHashMap<>();
         map.put("stage", row.stage().getLabel());
         map.put("metric", row.metric());
         map.put("eventType", row.eventType());
         map.put("countMode", row.countMode() == null ? null : row.countMode().getLabel());
         map.put("instrumentation", row.instrumentation() == null ? null : row.instrumentation().getLabel());
         map.put("target", row.target());
         map.put("result", row.headline());
         map.put("rawEvents", row.rawEvents());
         map.put("uniqueActors", row.uniqueActors());
         map.put("note", row.note());
         list.add(map);
      }

      Map<String, Object> map = new LinkedHashMap<>();
      map.put("metrics", list);
      map.put("realEventCount", realEvents);
      map.put("testEventCountExcluded", testEvents);
      map.put("limitations", limitations);
      return (map);
   }



   /*******************************************************************************
    ** Getter for rows
    *******************************************************************************/
   public List<Row> getRows()
   {
      return (rows);
   }



   /*******************************************************************************
    ** Getter for limitations
    *******************************************************************************/
   public List<String> getLimitations()
   {
      return (limitations);
   }



   /*******************************************************************************
    ** Getter for testEvents
    *******************************************************************************/
   public int getTestEvents()
   {
      return (testEvents);
   }

}
