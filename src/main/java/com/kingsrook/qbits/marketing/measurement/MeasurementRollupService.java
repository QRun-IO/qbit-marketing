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


import java.io.Serializable;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import com.kingsrook.qbits.marketing.MarketingStore;
import com.kingsrook.qbits.marketing.model.MarketingCampaign;
import com.kingsrook.qbits.marketing.model.MarketingMetricDaily;
import com.kingsrook.qbits.marketing.model.MarketingOutcomeEvent;
import com.kingsrook.qbits.marketing.security.MarketingInternalWrite;
import com.kingsrook.qbits.marketing.security.MarketingPermissions;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;


/*******************************************************************************
 ** Keeps the persisted daily rollup (marketing_metric_daily) of a campaign's
 ** outcome events, so a results view reads a few rows per day instead of every
 ** raw event.
 **
 ** - A day is the UTC date of the event's occurredAt.
 ** - Recomputing a (campaign, day) reads that day's raw events and makes the
 **   rollup rows match them exactly: rows are inserted or updated by their
 **   unique key (campaign, day, stage, event type, test data) and rows whose
 **   events are gone are deleted. It is idempotent: running it again changes
 **   nothing (a row is rewritten only when its numbers differ).
 ** - Raw count is the sum of quantities; unique actors are the distinct
 **   actorKeys of that day (a campaign-wide unique count cannot be summed from
 **   days, so MeasurementReport counts it in the backend); events without an
 **   actor are counted apart, as in the report.
 ** - The rollup is system-written: only this service writes it, inside
 **   MarketingInternalWrite.
 *******************************************************************************/
public class MeasurementRollupService
{
   private static final int PAGE_SIZE = 1000;



   /*******************************************************************************
    ** One campaign's day.
    *******************************************************************************/
   public record CampaignDay(Integer campaignId, LocalDate day)
   {
   }



   /*******************************************************************************
    ** What a recompute did: the days it covered and the rollup rows they hold.
    *******************************************************************************/
   public record Result(int days, int rows)
   {
   }



   /*******************************************************************************
    ** The sums of one (stage, test data, event type) of a day.
    *******************************************************************************/
   private static final class Sums
   {
      private int               rawCount           = 0;
      private int               eventsWithoutActor = 0;
      private final Set<String> actors             = new HashSet<>();
   }



   /*******************************************************************************
    ** The UTC day of an event: its occurredAt (the insert fills it in, so it is
    ** never null for a stored event; createDate is only a safety net).
    *******************************************************************************/
   public static LocalDate dayOf(QRecord event)
   {
      Instant occurredAt = event.getValueInstant("occurredAt");
      if(occurredAt == null)
      {
         occurredAt = event.getValueInstant("createDate");
      }
      return (occurredAt == null ? null : occurredAt.atZone(ZoneOffset.UTC).toLocalDate());
   }



   /*******************************************************************************
    ** Recompute every given (campaign, day), as trusted internal work: this runs
    ** from the customizer of a write that was itself permitted, and from Rebuild
    ** Results after its own permission check.
    *******************************************************************************/
   public Result recompute(Collection<CampaignDay> campaignDays) throws QException
   {
      Set<CampaignDay> distinct = new LinkedHashSet<>();
      for(CampaignDay campaignDay : campaignDays)
      {
         if(campaignDay.campaignId() != null && campaignDay.day() != null)
         {
            distinct.add(campaignDay);
         }
      }

      return (MarketingStore.trusted(() -> MarketingInternalWrite.run(() ->
      {
         int rows = 0;
         for(CampaignDay campaignDay : distinct)
         {
            rows += recomputeDay(campaignDay.campaignId(), campaignDay.day());
         }
         return (new Result(distinct.size(), rows));
      })));
   }



   /*******************************************************************************
    ** Rebuild the whole rollup of a campaign: every day of its events, and every
    ** day of rollup rows that no longer have events. Returns a one-line summary.
    ** Needs the same permission as Update Results from CRM.
    *******************************************************************************/
   public String rebuild(Integer campaignId) throws QException
   {
      MarketingPermissions.require("rebuild results", MarketingPermissions.PREPARE);
      MarketingStore.require(MarketingCampaign.TABLE_NAME, campaignId);

      Result result = MarketingStore.trusted(() ->
      {
         Set<CampaignDay> days = new LinkedHashSet<>();

         ///////////////////////////////////////////////////////////////////////
         // the days of the raw events, a page at a time (memory is bounded   //
         // by the number of distinct days, not the number of events)         //
         ///////////////////////////////////////////////////////////////////////
         int lastId = 0;
         while(true)
         {
            List<QRecord> page = MarketingStore.query(MarketingOutcomeEvent.TABLE_NAME, new QQueryFilter(
               new QFilterCriteria("campaignId", QCriteriaOperator.EQUALS, campaignId),
               new QFilterCriteria("id", QCriteriaOperator.GREATER_THAN, lastId))
               .withOrderBy(new QFilterOrderBy("id")).withLimit(PAGE_SIZE));
            for(QRecord event : page)
            {
               days.add(new CampaignDay(campaignId, dayOf(event)));
               lastId = event.getValueInteger("id");
            }
            if(page.size() < PAGE_SIZE)
            {
               break;
            }
         }

         for(QRecord existing : MarketingStore.queryBy(MarketingMetricDaily.TABLE_NAME, "campaignId", campaignId))
         {
            days.add(new CampaignDay(campaignId, existing.getValueLocalDate("dayUtc")));
         }
         return (recompute(days));
      });
      return ("rebuilt " + result.rows() + " daily result row(s) over " + result.days() + " day(s)");
   }



   /*******************************************************************************
    ** Make one day's rollup rows match its raw events. Returns the number of rows
    ** the day has afterwards. Must run inside MarketingInternalWrite.
    *******************************************************************************/
   private int recomputeDay(Integer campaignId, LocalDate day) throws QException
   {
      Instant start = day.atStartOfDay(ZoneOffset.UTC).toInstant();
      Instant end   = day.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant();

      Map<String, Sums> sums = new LinkedHashMap<>();
      for(QRecord event : MarketingStore.query(MarketingOutcomeEvent.TABLE_NAME, new QQueryFilter(
         new QFilterCriteria("campaignId", QCriteriaOperator.EQUALS, campaignId),
         new QFilterCriteria("occurredAt", QCriteriaOperator.GREATER_THAN_OR_EQUALS, start),
         new QFilterCriteria("occurredAt", QCriteriaOperator.LESS_THAN, end))))
      {
         Sums sum = sums.computeIfAbsent(key(event.getValueInteger("stage"), Boolean.TRUE.equals(event.getValueBoolean("isTestData")), event.getValueString("eventType")), k -> new Sums());

         Integer quantity = event.getValueInteger("quantity");
         sum.rawCount += quantity == null ? 1 : quantity;
         String actor = event.getValueString("actorKey");
         if(actor == null || actor.isBlank())
         {
            sum.eventsWithoutActor++;
         }
         else
         {
            sum.actors.add(actor);
         }
      }

      QRecord campaign  = MarketingStore.get(MarketingCampaign.TABLE_NAME, campaignId);
      Integer productId = campaign == null ? null : campaign.getValueInteger("productId");

      Map<String, QRecord> existing = new LinkedHashMap<>();
      for(QRecord row : MarketingStore.query(MarketingMetricDaily.TABLE_NAME, new QQueryFilter(
         new QFilterCriteria("campaignId", QCriteriaOperator.EQUALS, campaignId),
         new QFilterCriteria("dayUtc", QCriteriaOperator.EQUALS, day))))
      {
         existing.put(key(row.getValueInteger("funnelStage"), Boolean.TRUE.equals(row.getValueBoolean("isTestData")), row.getValueString("eventType")), row);
      }

      for(Map.Entry<String, Sums> entry : sums.entrySet())
      {
         String[] parts = entry.getKey().split("\\|", 3);
         Sums     sum   = entry.getValue();
         QRecord  row   = existing.remove(entry.getKey());
         if(row == null)
         {
            upsertNew(new MarketingMetricDaily().withCampaignId(campaignId).withProductId(productId).withDayUtc(day)
               .withFunnelStage(Integer.valueOf(parts[0])).withIsTestData(Boolean.valueOf(parts[1])).withEventType(parts[2])
               .withRawCount(sum.rawCount).withUniqueActors(sum.actors.size()).withEventsWithoutActor(sum.eventsWithoutActor).withRecomputedAt(Instant.now()));
         }
         else if(!Objects.equals(productId, row.getValueInteger("productId")) || !Objects.equals(sum.rawCount, row.getValueInteger("rawCount"))
            || !Objects.equals(sum.actors.size(), row.getValueInteger("uniqueActors")) || !Objects.equals(sum.eventsWithoutActor, row.getValueInteger("eventsWithoutActor")))
         {
            MarketingStore.update(MarketingMetricDaily.TABLE_NAME, new QRecord().withValue("id", row.getValue("id")).withValue("productId", productId)
               .withValue("rawCount", sum.rawCount).withValue("uniqueActors", sum.actors.size()).withValue("eventsWithoutActor", sum.eventsWithoutActor)
               .withValue("recomputedAt", Instant.now()));
         }
      }

      ///////////////////////////////////
      // rows whose events are gone    //
      ///////////////////////////////////
      List<Serializable> stale = new ArrayList<>();
      existing.values().forEach(row -> stale.add(row.getValue("id")));
      MarketingStore.delete(MarketingMetricDaily.TABLE_NAME, stale);
      return (sums.size());
   }



   /*******************************************************************************
    ** Insert a new rollup row. If a concurrent recompute inserted the same key
    ** first (the unique key rejects ours), update that row instead.
    *******************************************************************************/
   private void upsertNew(MarketingMetricDaily row) throws QException
   {
      try
      {
         MarketingStore.insert(row);
      }
      catch(QUserFacingException e)
      {
         List<QRecord> winners = MarketingStore.query(MarketingMetricDaily.TABLE_NAME, new QQueryFilter(
            new QFilterCriteria("campaignId", QCriteriaOperator.EQUALS, row.getCampaignId()),
            new QFilterCriteria("dayUtc", QCriteriaOperator.EQUALS, row.getDayUtc()),
            new QFilterCriteria("funnelStage", QCriteriaOperator.EQUALS, row.getFunnelStage()),
            new QFilterCriteria("eventType", QCriteriaOperator.EQUALS, row.getEventType()),
            new QFilterCriteria("isTestData", QCriteriaOperator.EQUALS, row.getIsTestData())));
         if(winners.isEmpty())
         {
            throw (e);
         }
         MarketingStore.update(MarketingMetricDaily.TABLE_NAME, new QRecord().withValue("id", winners.get(0).getValue("id")).withValue("productId", row.getProductId())
            .withValue("rawCount", row.getRawCount()).withValue("uniqueActors", row.getUniqueActors()).withValue("eventsWithoutActor", row.getEventsWithoutActor())
            .withValue("recomputedAt", row.getRecomputedAt()));
      }
   }



   /*******************************************************************************
    ** The map key of a rollup row: stage, test data, then the event type (last,
    ** so the type may contain the separator).
    *******************************************************************************/
   private static String key(Integer stage, boolean isTestData, String eventType)
   {
      return (stage + "|" + isTestData + "|" + eventType);
   }

}
