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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import com.kingsrook.qbits.marketing.MarketingStore;
import com.kingsrook.qbits.marketing.model.MarketingOutcomeEvent;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizerInterface;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;


/*******************************************************************************
 ** Keeps the daily rollup in step with the raw outcome events: after every
 ** insert, update and delete of events - from the table, the QQQ API, the CRM
 ** import, an importer or the execution worker - it recomputes the affected
 ** (campaign, day)s (MeasurementRollupService). An update recomputes both the
 ** old and the new day, so an event moved to another day or campaign leaves
 ** nothing behind.
 *******************************************************************************/
public class OutcomeEventRollupCustomizer implements TableCustomizerInterface
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public List<QRecord> postInsert(InsertInput insertInput, List<QRecord> records) throws QException
   {
      Set<MeasurementRollupService.CampaignDay> days = new LinkedHashSet<>();
      for(QRecord record : CollectionUtils.nonNullList(records))
      {
         if(!CollectionUtils.nullSafeHasContents(record.getErrors()))
         {
            add(days, record);
         }
      }
      new MeasurementRollupService().recompute(days);
      return (records);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public List<QRecord> postUpdate(UpdateInput updateInput, List<QRecord> records, Optional<List<QRecord>> oldRecordList) throws QException
   {
      Map<Serializable, QRecord> oldById = oldRecordListToMap("id", oldRecordList).orElse(Map.of());

      Set<MeasurementRollupService.CampaignDay> days = new LinkedHashSet<>();
      for(QRecord record : CollectionUtils.nonNullList(records))
      {
         if(CollectionUtils.nullSafeHasContents(record.getErrors()))
         {
            continue;
         }

         //////////////////////////////////////////////////////////////////////
         // the stored row after the update (the record may carry only the   //
         // changed fields), and the row as it was                            //
         //////////////////////////////////////////////////////////////////////
         add(days, MarketingStore.get(MarketingOutcomeEvent.TABLE_NAME, record.getValue("id")));
         add(days, oldById.get(record.getValue("id")));
      }
      new MeasurementRollupService().recompute(days);
      return (records);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public List<QRecord> postDelete(DeleteInput deleteInput, List<QRecord> records) throws QException
   {
      Set<MeasurementRollupService.CampaignDay> days = new LinkedHashSet<>();
      for(QRecord record : CollectionUtils.nonNullList(records))
      {
         if(!CollectionUtils.nullSafeHasContents(record.getErrors()))
         {
            add(days, record);
         }
      }
      new MeasurementRollupService().recompute(days);
      return (records);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void add(Set<MeasurementRollupService.CampaignDay> days, QRecord event)
   {
      if(event != null && event.getValueInteger("campaignId") != null && MeasurementRollupService.dayOf(event) != null)
      {
         days.add(new MeasurementRollupService.CampaignDay(event.getValueInteger("campaignId"), MeasurementRollupService.dayOf(event)));
      }
   }

}
