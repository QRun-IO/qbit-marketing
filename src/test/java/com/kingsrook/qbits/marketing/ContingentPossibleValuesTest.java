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
import java.util.Map;
import com.kingsrook.qbits.marketing.metadata.MarketingMetaDataBuilder;
import com.kingsrook.qbits.marketing.model.MarketingAudience;
import com.kingsrook.qqq.backend.core.actions.values.SearchPossibleValueSourceAction;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.values.SearchPossibleValueSourceInput;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.PossibleValueSearchFilterUseCase;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValue;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.collections.MapBuilder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Product-contingent metadata and real memory-backend possible-value searches.
 *******************************************************************************/
class ContingentPossibleValuesTest extends BaseMarketingTest
{
   /*******************************************************************************
    ** Every declared dependency uses an editable parent and child and a real
    ** product column on the referenced table.
    *******************************************************************************/
   @ParameterizedTest
   @CsvSource({
      "marketing_campaign, audienceId, marketing_audience",
      "marketing_claim, sourceId, marketing_evidence_source",
      "marketing_finding, campaignId, marketing_campaign",
      "marketing_finding, contentId, marketing_content",
      "marketing_finding, claimId, marketing_claim",
      "marketing_finding, sourceId, marketing_evidence_source",
      "marketing_finding, agentRunId, marketing_agent_run",
      "marketing_metric, campaignId, marketing_campaign",
      "marketing_outcome_event, campaignId, marketing_campaign",
      "marketing_outcome_event, contentId, marketing_content",
      "marketing_outcome_event, executionId, marketing_execution"
   })
   void testMetadata(String tableName, String fieldName, String targetName)
   {
      QTableMetaData table = qInstance.getTable(tableName);
      QFieldMetaData field = table.getField(fieldName);
      assertThat(MarketingMetaDataBuilder.SYSTEM_WRITTEN_TABLE_NAMES).doesNotContain(tableName);
      assertThat(table.getField("productId").getIsEditable()).isTrue();
      assertThat(field.getIsEditable()).isTrue();
      assertThat(field.getPossibleValueSourceName()).isEqualTo(targetName);
      assertThat(qInstance.getTable(targetName).getField("productId")).isNotNull();
      assertThat(field.getPossibleValueSourceFilter()).isEqualTo(
         new QQueryFilter(new QFilterCriteria("productId", QCriteriaOperator.EQUALS, "${input.productId}")));
   }



   /*******************************************************************************
    ** Read-only dependencies stay unfiltered, and the inventory is exhaustive.
    *******************************************************************************/
   @Test
   void testSkippedFields()
   {
      int filterCount = 0;
      for(QTableMetaData table : qInstance.getTables().values())
      {
         for(QFieldMetaData field : table.getFields().values())
         {
            if(MarketingMetaDataBuilder.SYSTEM_WRITTEN_TABLE_NAMES.contains(table.getName()) || "marketing_content".equals(table.getName()))
            {
               assertThat(field.getPossibleValueSourceFilter()).as(table.getName() + "." + field.getName()).isNull();
            }
            if(field.getPossibleValueSourceFilter() != null)
            {
               filterCount++;
            }
         }
      }
      assertThat(filterCount).isEqualTo(11);
   }



   /*******************************************************************************
    ** Three different target tables: selected products isolate choices, while
    ** missing products give no form choices and all filter-builder choices.
    *******************************************************************************/
   @ParameterizedTest
   @CsvSource({"marketing_campaign, audienceId", "marketing_claim, sourceId", "marketing_metric, campaignId"})
   void testSearch(String tableName, String fieldName) throws Exception
   {
      Integer firstProduct = product("first");
      Integer secondProduct = product("second");
      campaign(firstProduct);
      campaign(secondProduct);
      MarketingStore.insert(new MarketingAudience().withProductId(firstProduct).withName("First audience"));
      MarketingStore.insert(new MarketingAudience().withProductId(secondProduct).withName("Second audience"));

      QFieldMetaData field = qInstance.getTable(tableName).getField(fieldName);
      String targetName = field.getPossibleValueSourceName();
      Serializable firstId = MarketingStore.queryBy(targetName, "productId", firstProduct).get(0).getValue("id");
      Serializable secondId = MarketingStore.queryBy(targetName, "productId", secondProduct).get(0).getValue("id");
      assertThat(search(field, Map.of("productId", firstProduct), PossibleValueSearchFilterUseCase.FORM)).containsExactly(firstId);
      assertThat(search(field, Map.of("productId", secondProduct), PossibleValueSearchFilterUseCase.FORM)).containsExactly(secondId);
      assertThat(search(field, Map.of(), PossibleValueSearchFilterUseCase.FORM)).isEmpty();
      assertThat(search(field, Map.of(), PossibleValueSearchFilterUseCase.FILTER)).containsExactlyInAnyOrder(firstId, secondId);
      assertThat(field.getPossibleValueSourceFilter().getCriteria().get(0).getValues()).containsExactly("${input.productId}");
   }



   /*******************************************************************************
    ** Interpret a clone as the UI does, then search the actual table PVS.
    *******************************************************************************/
   private java.util.List<Serializable> search(QFieldMetaData field, Map<String, Serializable> input, PossibleValueSearchFilterUseCase useCase) throws Exception
   {
      QQueryFilter filter = field.getPossibleValueSourceFilter().clone();
      filter.interpretValues(MapBuilder.of("input", input), useCase);
      return (new SearchPossibleValueSourceAction().execute(new SearchPossibleValueSourceInput()
         .withPossibleValueSourceName(field.getPossibleValueSourceName()).withDefaultQueryFilter(filter))
         .getResults().stream().map(QPossibleValue::getId).map(id -> (Serializable) id).toList());
   }
}
