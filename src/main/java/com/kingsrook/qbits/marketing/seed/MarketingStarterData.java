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

package com.kingsrook.qbits.marketing.seed;


import java.io.InputStream;
import java.io.Serializable;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.fasterxml.jackson.core.type.TypeReference;
import com.kingsrook.qbits.marketing.MarketingStore;
import com.kingsrook.qbits.marketing.model.MarketingAudience;
import com.kingsrook.qbits.marketing.model.MarketingCampaign;
import com.kingsrook.qbits.marketing.model.MarketingChannel;
import com.kingsrook.qbits.marketing.model.MarketingClaim;
import com.kingsrook.qbits.marketing.model.MarketingEvidenceSource;
import com.kingsrook.qbits.marketing.model.MarketingMetric;
import com.kingsrook.qbits.marketing.model.MarketingPolicy;
import com.kingsrook.qbits.marketing.model.MarketingProduct;
import com.kingsrook.qbits.marketing.model.enums.AiContentPolicy;
import com.kingsrook.qbits.marketing.model.enums.CampaignGoal;
import com.kingsrook.qbits.marketing.model.enums.CampaignStatus;
import com.kingsrook.qbits.marketing.model.enums.ChannelType;
import com.kingsrook.qbits.marketing.model.enums.ClaimStatus;
import com.kingsrook.qbits.marketing.model.enums.CountMode;
import com.kingsrook.qbits.marketing.model.enums.FunnelStage;
import com.kingsrook.qbits.marketing.model.enums.InstrumentationStatus;
import com.kingsrook.qbits.marketing.model.enums.ProductMaturity;
import com.kingsrook.qbits.marketing.model.enums.SourceType;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.PossibleValueEnum;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;


/*******************************************************************************
 ** Loads src/main/resources/marketing-starter-data.json: channels, the policy
 ** row, and the three starter products (QQQ, gclo, MarkdownViewr for Windows)
 ** with their evidence sources, claims, audiences, draft campaigns and
 ** measurement plans - all verified on the date in the file.
 **
 ** Idempotent: a channel, policy or product (by slug) that already exists is
 ** left alone, with everything under it.
 *******************************************************************************/
public class MarketingStarterData
{
   public static final String RESOURCE = "/marketing-starter-data.json";

   private static final Map<String, PossibleValueEnum<Integer>[]> ENUMS = new HashMap<>();

   static
   {
      ENUMS.put(ProductMaturity.NAME, ProductMaturity.values());
      ENUMS.put(SourceType.NAME, SourceType.values());
      ENUMS.put(ClaimStatus.NAME, ClaimStatus.values());
      ENUMS.put(CampaignGoal.NAME, CampaignGoal.values());
      ENUMS.put(CampaignStatus.NAME, CampaignStatus.values());
      ENUMS.put(ChannelType.NAME, ChannelType.values());
      ENUMS.put(AiContentPolicy.NAME, AiContentPolicy.values());
      ENUMS.put(FunnelStage.NAME, FunnelStage.values());
      ENUMS.put(CountMode.NAME, CountMode.values());
      ENUMS.put(InstrumentationStatus.NAME, InstrumentationStatus.values());
   }



   /*******************************************************************************
    ** What was loaded.
    *******************************************************************************/
   public record Result(int channels, int products, int sources, int claims, int audiences, int campaigns, int metrics, List<String> skipped)
   {
      /*******************************************************************************
       ** One line.
       *******************************************************************************/
      public String summary()
      {
         return (products + " product(s), " + sources + " source(s), " + claims + " claim(s), " + audiences + " audience(s), " + campaigns + " campaign(s), "
            + metrics + " metric(s), " + channels + " channel(s) loaded" + (skipped.isEmpty() ? "" : "; already present: " + String.join(", ", skipped)));
      }
   }



   /*******************************************************************************
    ** Load the bundled file.
    *******************************************************************************/
   @SuppressWarnings("unchecked")
   private Result loadImpl() throws QException
   {
      Map<String, Object> root;
      try(InputStream stream = MarketingStarterData.class.getResourceAsStream(RESOURCE))
      {
         if(stream == null)
         {
            throw (new QException("Missing resource " + RESOURCE));
         }
         root = JsonUtils.toObject(new String(stream.readAllBytes(), StandardCharsets.UTF_8), new TypeReference<>() {});
      }
      catch(QException e)
      {
         throw (e);
      }
      catch(Exception e)
      {
         throw (new QException("Could not read " + RESOURCE, e));
      }

      String       verifiedDate = (String) root.get("verifiedDate");
      String       verifiedBy   = (String) root.get("verifiedBy");
      List<String> skipped      = new ArrayList<>();
      int          channels     = 0;
      int          products     = 0;
      int          sources      = 0;
      int          claims       = 0;
      int          audiences    = 0;
      int          campaigns    = 0;
      int          metrics      = 0;

      for(Map<String, Object> channel : (List<Map<String, Object>>) root.get("channels"))
      {
         if(!MarketingStore.queryBy(MarketingChannel.TABLE_NAME, "name", (String) channel.get("name")).isEmpty())
         {
            skipped.add("channel " + channel.get("name"));
            continue;
         }
         MarketingStore.insert(MarketingChannel.TABLE_NAME, record(MarketingChannel.TABLE_NAME, channel, Map.of()));
         channels++;
      }

      Map<String, Object> policy = (Map<String, Object>) root.get("policy");
      if(policy != null)
      {
         if(MarketingStore.queryBy(MarketingPolicy.TABLE_NAME, "name", (String) policy.get("name")).isEmpty())
         {
            MarketingStore.insert(MarketingPolicy.TABLE_NAME, record(MarketingPolicy.TABLE_NAME, policy, Map.of()));
         }
         else
         {
            skipped.add("policy " + policy.get("name"));
         }
      }

      for(Map<String, Object> group : (List<Map<String, Object>>) root.get("products"))
      {
         Map<String, Object> product = (Map<String, Object>) group.get("product");
         if(!MarketingStore.queryBy(MarketingProduct.TABLE_NAME, "slug", (String) product.get("slug")).isEmpty())
         {
            skipped.add("product " + product.get("slug"));
            continue;
         }
         Integer productId = MarketingStore.insert(MarketingProduct.TABLE_NAME, record(MarketingProduct.TABLE_NAME, product, Map.of())).getValueInteger("id");
         products++;

         Map<String, Integer> refs = new HashMap<>();
         for(Map<String, Object> source : list(group, "sources"))
         {
            QRecord record = record(MarketingEvidenceSource.TABLE_NAME, source, Map.of("productId", productId, "verifiedDate", LocalDate.parse(verifiedDate), "verifiedBy", verifiedBy));
            refs.put("source:" + source.get("ref"), MarketingStore.insert(MarketingEvidenceSource.TABLE_NAME, record).getValueInteger("id"));
            sources++;
         }
         for(Map<String, Object> claim : list(group, "claims"))
         {
            Integer sourceId = refs.get("source:" + claim.get("source"));
            MarketingStore.insert(MarketingClaim.TABLE_NAME, record(MarketingClaim.TABLE_NAME, claim, Map.of("productId", productId, "sourceId", sourceId, "verifiedDate", LocalDate.parse(verifiedDate))));
            claims++;
         }
         for(Map<String, Object> audience : list(group, "audiences"))
         {
            refs.put("audience:" + audience.get("ref"), MarketingStore.insert(MarketingAudience.TABLE_NAME, record(MarketingAudience.TABLE_NAME, audience, Map.of("productId", productId))).getValueInteger("id"));
            audiences++;
         }
         for(Map<String, Object> campaign : list(group, "campaigns"))
         {
            Map<String, Serializable> extra = new HashMap<>();
            extra.put("productId", productId);
            Integer audienceId = refs.get("audience:" + campaign.get("audience"));
            if(audienceId != null)
            {
               extra.put("audienceId", audienceId);
            }
            Integer campaignId = MarketingStore.insert(MarketingCampaign.TABLE_NAME, record(MarketingCampaign.TABLE_NAME, campaign, extra)).getValueInteger("id");
            campaigns++;
            for(Map<String, Object> metric : list(campaign, "metrics"))
            {
               MarketingStore.insert(MarketingMetric.TABLE_NAME, record(MarketingMetric.TABLE_NAME, metric, Map.of("campaignId", campaignId, "productId", productId)));
               metrics++;
            }
         }
      }

      return (new Result(channels, products, sources, claims, audiences, campaigns, metrics, skipped));
   }



   /*******************************************************************************
    ** A record from JSON values: only fields of the table are taken; enum
    ** names become their ids; extra values (ids) win.
    *******************************************************************************/
   static QRecord record(String tableName, Map<String, Object> json, Map<String, ? extends Serializable> extra) throws QException
   {
      QTableMetaData table  = QContext.getQInstance().getTable(tableName);
      QRecord        record = new QRecord();
      for(Map.Entry<String, Object> entry : json.entrySet())
      {
         QFieldMetaData field = table.getFields().get(entry.getKey());
         if(field == null || entry.getValue() instanceof List || entry.getValue() instanceof Map)
         {
            continue;
         }
         Object value = entry.getValue();
         if(value instanceof String name && field.getPossibleValueSourceName() != null && ENUMS.containsKey(field.getPossibleValueSourceName()))
         {
            value = enumId(field.getPossibleValueSourceName(), name);
         }
         record.setValue(entry.getKey(), (Serializable) value);
      }
      extra.forEach(record::setValue);
      return (record);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static Integer enumId(String pvsName, String name) throws QException
   {
      for(PossibleValueEnum<Integer> value : ENUMS.get(pvsName))
      {
         if(((Enum<?>) value).name().equals(name))
         {
            return (value.getPossibleValueId());
         }
      }
      throw (new QException("Unknown " + pvsName + " value " + name + " in " + RESOURCE));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @SuppressWarnings("unchecked")
   private static List<Map<String, Object>> list(Map<String, Object> map, String key)
   {
      Object value = map.get(key);
      return (value == null ? List.of() : (List<Map<String, Object>>) value);
   }


   /*******************************************************************************
    ** Entry point (see the implementation): runs as trusted internal work after
    ** its permission check.
    *******************************************************************************/
   public Result load() throws QException
   {
      com.kingsrook.qbits.marketing.security.MarketingPermissions.require("load starter data", com.kingsrook.qbits.marketing.security.MarketingPermissions.PREPARE);
      return (MarketingStore.trusted(() -> loadImpl()));
   }

}
