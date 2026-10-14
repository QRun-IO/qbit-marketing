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


import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qbits.marketing.MarketingRuntime;
import com.kingsrook.qbits.marketing.MarketingStore;
import com.kingsrook.qbits.marketing.model.MarketingCampaign;
import com.kingsrook.qbits.marketing.model.MarketingOutcomeEvent;
import com.kingsrook.qbits.marketing.security.MarketingPermissions;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Runs a campaign's registered MarketingOutcomeImporters and stores what they
 ** report as outcome events, through the normal insert path: the source is the
 ** importer's name, so an outcome fetched twice is stored once (the unique
 ** (source, external id)). The daily rollup follows from the insert.
 **
 ** One failing importer does not stop the others; the summary names it and the
 ** action reports the campaign as not fully done.
 *******************************************************************************/
public class OutcomeImportService
{
   private static final QLogger LOG = QLogger.getLogger(OutcomeImportService.class);


   /*******************************************************************************
    ** Why nothing can be imported (no importer is registered), or null.
    *******************************************************************************/
   public String blocker() throws QException
   {
      return (MarketingRuntime.current().getConfig().getOutcomeImporters().isEmpty() ? "no outcome importer is registered" : null);
   }



   /*******************************************************************************
    ** Entry point: runs as trusted internal work after its permission check.
    ** Returns a one-line summary.
    *******************************************************************************/
   public String importOutcomes(Integer campaignId) throws QException
   {
      MarketingPermissions.require("import outcomes", MarketingPermissions.PREPARE);
      return (MarketingStore.trusted(() -> importOutcomesImpl(campaignId)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private String importOutcomesImpl(Integer campaignId) throws QException
   {
      List<MarketingOutcomeImporter> importers = MarketingRuntime.current().getConfig().getOutcomeImporters();
      if(importers.isEmpty())
      {
         throw (new QUserFacingException("No outcome importer is registered in this application."));
      }

      QRecord      campaign = MarketingStore.require(MarketingCampaign.TABLE_NAME, campaignId);
      List<String> parts    = new ArrayList<>();
      boolean      failed   = false;
      for(MarketingOutcomeImporter importer : importers)
      {
         try
         {
            parts.add(importer.getName() + ": " + importOne(importer, campaign));
         }
         catch(QException | RuntimeException e)
         {
            failed = true;
            LOG.warn("Outcome importer failed", e, logPair("importer", importer.getName()), logPair("campaignId", campaignId));
            parts.add(importer.getName() + ": failed (" + (e instanceof QUserFacingException ? e.getMessage() : "see the server log") + ")");
         }
      }

      String summary = String.join("; ", parts);
      if(failed)
      {
         throw (new QUserFacingException(summary));
      }
      return (summary);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private String importOne(MarketingOutcomeImporter importer, QRecord campaign) throws QException
   {
      Integer               campaignId = campaign.getValueInteger("id");
      List<ImportedOutcome> outcomes   = importer.fetch(campaign, newestOccurredAt(importer.getName(), campaignId));

      int imported   = 0;
      int duplicates = 0;
      int invalid    = 0;
      for(ImportedOutcome outcome : outcomes == null ? List.<ImportedOutcome>of() : outcomes)
      {
         if(outcome == null || outcome.stage() == null || !StringUtils.hasContent(outcome.eventType()) || !StringUtils.hasContent(outcome.externalEventId()))
         {
            invalid++;
            continue;
         }

         if(MarketingStore.count(MarketingOutcomeEvent.TABLE_NAME, new QQueryFilter(
            new QFilterCriteria("source", QCriteriaOperator.EQUALS, importer.getName()),
            new QFilterCriteria("externalEventId", QCriteriaOperator.EQUALS, outcome.externalEventId()))) > 0)
         {
            duplicates++;
            continue;
         }

         MarketingStore.insert(new MarketingOutcomeEvent()
            .withCampaignId(campaignId)
            .withStage(outcome.stage().getId())
            .withEventType(outcome.eventType())
            .withSource(importer.getName())
            .withExternalEventId(outcome.externalEventId())
            .withActorKey(outcome.actorKey())
            .withOccurredAt(outcome.occurredAt() == null ? Instant.now() : outcome.occurredAt())
            .withQuantity(outcome.quantity() == null ? 1 : outcome.quantity())
            .withIsTestData(Boolean.TRUE.equals(outcome.isTestData()))
            .withNote(outcome.note()));
         imported++;
      }
      return (imported + " imported, " + duplicates + " already recorded" + (invalid > 0 ? ", " + invalid + " invalid (skipped)" : ""));
   }



   /*******************************************************************************
    ** When the newest event this importer recorded for the campaign occurred, or
    ** null when it has recorded none.
    *******************************************************************************/
   private static Instant newestOccurredAt(String importerName, Integer campaignId) throws QException
   {
      List<QRecord> newest = MarketingStore.query(MarketingOutcomeEvent.TABLE_NAME, new QQueryFilter(
         new QFilterCriteria("campaignId", QCriteriaOperator.EQUALS, campaignId),
         new QFilterCriteria("source", QCriteriaOperator.EQUALS, importerName))
         .withOrderBy(new QFilterOrderBy("occurredAt", false)).withLimit(1));
      return (newest.isEmpty() ? null : newest.get(0).getValueInstant("occurredAt"));
   }

}
