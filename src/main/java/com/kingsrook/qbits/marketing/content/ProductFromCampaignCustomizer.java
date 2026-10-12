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

package com.kingsrook.qbits.marketing.content;


import java.util.List;
import java.util.Optional;
import com.kingsrook.qbits.marketing.MarketingStore;
import com.kingsrook.qbits.marketing.model.MarketingCampaign;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizerInterface;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;


/*******************************************************************************
 ** Sets productId from campaignId on campaign-owned rows (metrics, outcome
 ** events, findings), so the product security lock (when configured) and
 ** product-level reports cover them. Outcome events also get quantity 1 when
 ** none is given.
 *******************************************************************************/
public class ProductFromCampaignCustomizer implements TableCustomizerInterface
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public List<QRecord> preInsert(InsertInput insertInput, List<QRecord> records, boolean isPreview) throws QException
   {
      for(QRecord record : records)
      {
         fill(record);
         if(record.getValues().containsKey("quantity") || "marketing_outcome_event".equals(insertInput.getTableName()))
         {
            if(record.getValue("quantity") == null)
            {
               record.setValue("quantity", 1);
            }
         }
      }
      return (records);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public List<QRecord> preUpdate(UpdateInput updateInput, List<QRecord> records, boolean isPreview, Optional<List<QRecord>> oldRecordList) throws QException
   {
      for(QRecord record : records)
      {
         if(record.getValues().containsKey("campaignId"))
         {
            fill(record);
         }
      }
      return (records);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void fill(QRecord record) throws QException
   {
      Integer campaignId = record.getValueInteger("campaignId");
      if(campaignId != null)
      {
         QRecord campaign = MarketingStore.get(MarketingCampaign.TABLE_NAME, campaignId);
         if(campaign != null)
         {
            record.setValue("productId", campaign.getValueInteger("productId"));
         }
      }
   }

}
