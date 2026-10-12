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


import java.util.Map;
import com.kingsrook.qbits.marketing.agent.MarketingToolRunner;
import com.kingsrook.qbits.marketing.agent.tools.MarketingToolContext;
import com.kingsrook.qbits.marketing.agent.tools.MarketingTools;
import com.kingsrook.qbits.marketing.model.MarketingCampaign;
import com.kingsrook.qbits.marketing.model.MarketingClaim;
import com.kingsrook.qbits.marketing.model.MarketingProduct;
import com.kingsrook.qbits.marketing.security.MarketingPermissions;
import com.kingsrook.qqq.backend.core.actions.ai.AiToolOutcome;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** The optional product (tenant) boundary: with productSecurityKeyType, a user
 ** sees - and the agent's tools return - only the products the user holds
 ** keys for.
 *******************************************************************************/
class ProductBoundaryTest extends BaseMarketingTest
{
   static final String KEY = "marketing_product_id";



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   protected void customizeConfig(MarketingQBitConfig config)
   {
      config.withProductSecurityKeyType(KEY);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Test
   void testBoundary() throws QException
   {
      QSession all = admin();
      all.withSecurityKeyValue(KEY + "_all_access", true);
      as(all);
      Integer a = product("a");
      Integer b = product("b");
      Integer campaignB = campaign(b);

      QSession onlyA = user("only-a", MarketingPermissions.READ, MarketingPermissions.AGENT);
      onlyA.withSecurityKeyValue(KEY, a);
      as(onlyA);
      assertThat(MarketingStore.query(MarketingProduct.TABLE_NAME, new QQueryFilter())).extracting(r -> r.getValueInteger("id")).containsExactly(a);
      assertThat(MarketingStore.query(MarketingClaim.TABLE_NAME, new QQueryFilter())).allMatch(r -> a.equals(r.getValueInteger("productId")));
      assertThat(MarketingStore.get(MarketingCampaign.TABLE_NAME, campaignB)).isNull();

      MarketingToolRunner runner = new MarketingToolRunner(MarketingTools.all());
      MarketingToolRunner.Result result = runner.execute(MarketingTools.GET_CAMPAIGN_BRIEF, Map.of("campaign_id", campaignB), new MarketingToolContext(null, 50));
      assertThat(result.outcome()).isIn(AiToolOutcome.NOT_FOUND, AiToolOutcome.DENIED);
      assertThat(result.modelContent()).doesNotContain("Product b");
   }

}
