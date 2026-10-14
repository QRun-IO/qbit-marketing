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


import java.time.Instant;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import com.kingsrook.qbits.marketing.model.enums.FindingSeverity;
import com.kingsrook.qbits.marketing.model.enums.FindingStatus;
import com.kingsrook.qbits.marketing.security.MarketingInternalWrite;
import com.kingsrook.qbits.marketing.security.MarketingPermissions;
import com.kingsrook.qqq.backend.core.model.statusmessages.BadInputStatusMessage;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;
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
 ** product-level reports cover them. Outcome events also get quantity 1, the
 ** current time and isTestData false when none is given.
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
         if("marketing_outcome_event".equals(insertInput.getTableName()))
         {
            ////////////////////////////////////////////////////////////////////
            // an event always has a time (the daily rollup is by its UTC day) //
            // and is real unless marked as test data                          //
            ////////////////////////////////////////////////////////////////////
            if(record.getValue("occurredAt") == null)
            {
               record.setValue("occurredAt", Instant.now());
            }
            if(record.getValue("isTestData") == null)
            {
               record.setValue("isTestData", false);
            }
            blankActorToNull(record);
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
      boolean findings = "marketing_finding".equals(updateInput.getTableName());
      Map<Integer, QRecord> oldById = new HashMap<>();
      for(QRecord old : oldRecordList.orElse(List.of()))
      {
         oldById.put(ValueUtils.getValueAsInteger(old.getValue("id")), old);
      }

      for(QRecord record : records)
      {
         if(record.getValues().containsKey("campaignId"))
         {
            fill(record);
         }
         if("marketing_outcome_event".equals(updateInput.getTableName()))
         {
            if(record.getValues().containsKey("occurredAt") && record.getValue("occurredAt") == null)
            {
               record.addError(new BadInputStatusMessage("An outcome event needs the time it occurred."));
            }
            if(record.getValues().containsKey("isTestData") && record.getValue("isTestData") == null)
            {
               record.setValue("isTestData", false);
            }
            blankActorToNull(record);
         }
         if(findings)
         {
            guardBlocker(record, oldById.get(ValueUtils.getValueAsInteger(record.getValue("id"))));
         }
      }
      return (records);
   }



   /*******************************************************************************
    ** An open BLOCKER finding (e.g. an unsupported claim) stops approval, so
    ** closing it or lowering its severity needs approval authority
    ** (marketing.approve.hasAccess) - the author can't wave it away. The
    ** readiness checker's own re-runs are internal.
    *******************************************************************************/
   private static void guardBlocker(QRecord record, QRecord old) throws QException
   {
      if(MarketingInternalWrite.isActive() || MarketingPermissions.has(MarketingPermissions.APPROVE))
      {
         return;
      }
      if(old == null)
      {
         old = MarketingStore.get("marketing_finding", ValueUtils.getValueAsInteger(record.getValue("id")));
      }
      if(old == null || !FindingSeverity.BLOCKER.getId().equals(old.getValueInteger("severity")) || !FindingStatus.OPEN.getId().equals(old.getValueInteger("status")))
      {
         return;
      }

      boolean closes    = record.getValues().containsKey("status") && !FindingStatus.OPEN.getId().equals(ValueUtils.getValueAsInteger(record.getValue("status")));
      boolean downgrade = record.getValues().containsKey("severity") && !FindingSeverity.BLOCKER.getId().equals(ValueUtils.getValueAsInteger(record.getValue("severity")));
      if(closes || downgrade)
      {
         record.addError(new BadInputStatusMessage("Closing or downgrading an open blocker finding needs " + MarketingPermissions.APPROVE + "."));
      }
   }



   /*******************************************************************************
    ** A blank actor key is no actor: store null, so the rollup, the report and
    ** the backend's distinct count all agree.
    *******************************************************************************/
   private static void blankActorToNull(QRecord record)
   {
      String actor = record.getValueString("actorKey");
      if(record.getValues().containsKey("actorKey") && actor != null && actor.isBlank())
      {
         record.setValue("actorKey", null);
      }
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
