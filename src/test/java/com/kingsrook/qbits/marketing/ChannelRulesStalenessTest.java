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


import java.time.LocalDate;
import java.util.List;
import com.kingsrook.qbits.marketing.model.MarketingChannel;
import com.kingsrook.qbits.marketing.model.MarketingFinding;
import com.kingsrook.qbits.marketing.model.enums.AiContentPolicy;
import com.kingsrook.qbits.marketing.model.enums.ChannelType;
import com.kingsrook.qbits.marketing.model.enums.FindingSeverity;
import com.kingsrook.qbits.marketing.model.enums.FindingStatus;
import com.kingsrook.qbits.marketing.model.enums.FindingType;
import com.kingsrook.qbits.marketing.readiness.ReadinessChecker;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** A channel's rulesVerifiedDate goes stale like an evidence source does:
 ** Check Readiness on a campaign warns when content uses a channel whose rules
 ** were last read more than staleChannelRulesDays ago, and points at the
 ** channel's rulesSourceUrl. The window is configuration (default 90).
 *******************************************************************************/
class ChannelRulesStalenessTest extends BaseMarketingTest
{
   /*******************************************************************************
    ** Open channel-rule findings for the campaign.
    *******************************************************************************/
   private List<QRecord> ruleFindings(Integer campaignId) throws QException
   {
      return (MarketingStore.queryBy(MarketingFinding.TABLE_NAME, "campaignId", campaignId).stream()
         .filter(f -> FindingType.CHANNEL_RULE.getId().equals(f.getValueInteger("findingType")) && FindingStatus.OPEN.getId().equals(f.getValueInteger("status"))).toList());
   }



   /*******************************************************************************
    ** Fresh rules: no finding. Rules last read 91 days ago: a warning that names
    ** the date and the source URL. Re-reading them (a new date) clears it.
    *******************************************************************************/
   @Test
   void staleChannelRulesRaiseAWarningAndRereadingClearsIt() throws QException
   {
      Integer campaignId = campaign(product("stale-rules"));
      Integer channelId  = channel("Forum", ChannelType.COMMUNITY, null, AiContentPolicy.ALLOWED_WITH_DISCLOSURE, false);
      content(campaignId, channelId, "Body");
      MarketingStore.update(MarketingChannel.TABLE_NAME, new QRecord().withValue("id", channelId).withValue("rulesSourceUrl", "https://forum.example/rules"));

      new ReadinessChecker().checkCampaign(campaignId);
      assertThat(ruleFindings(campaignId)).as("rules verified today").isEmpty();

      LocalDate old = LocalDate.now().minusDays(91);
      MarketingStore.update(MarketingChannel.TABLE_NAME, new QRecord().withValue("id", channelId).withValue("rulesVerifiedDate", old));
      new ReadinessChecker().checkCampaign(campaignId);
      assertThat(ruleFindings(campaignId)).singleElement().satisfies(f ->
      {
         assertThat(f.getValueString("title")).contains("Channel rules may be out of date").contains("Forum");
         assertThat(f.getValueString("detail")).contains(old.toString()).contains("90 days");
         assertThat(f.getValueString("recommendation")).contains("https://forum.example/rules");
         assertThat(f.getValueInteger("severity")).isEqualTo(FindingSeverity.WARNING.getId());
      });

      MarketingStore.update(MarketingChannel.TABLE_NAME, new QRecord().withValue("id", channelId).withValue("rulesVerifiedDate", LocalDate.now().minusDays(89)));
      new ReadinessChecker().checkCampaign(campaignId);
      assertThat(ruleFindings(campaignId)).as("89 days is still current").isEmpty();
   }



   /*******************************************************************************
    ** The window is configuration, not a constant.
    *******************************************************************************/
   @Test
   void windowIsConfigurable() throws QException
   {
      config.withStaleChannelRulesDays(7);
      Integer campaignId = campaign(product("window"));
      Integer channelId  = channel("Blog", ChannelType.BLOG, null, AiContentPolicy.ALLOWED, false);
      content(campaignId, channelId, "Body");
      MarketingStore.update(MarketingChannel.TABLE_NAME, new QRecord().withValue("id", channelId).withValue("rulesVerifiedDate", LocalDate.now().minusDays(10)));
      new ReadinessChecker().checkCampaign(campaignId);
      assertThat(ruleFindings(campaignId)).singleElement().satisfies(f -> assertThat(f.getValueString("detail")).contains("7 days"));
   }



   /*******************************************************************************
    ** A channel with no rule date or an Unknown AI policy keeps the existing
    ** "not verified" warning (not the stale one), and it names the source URL.
    *******************************************************************************/
   @Test
   void unverifiedChannelKeepsItsOwnWarning() throws QException
   {
      Integer campaignId = campaign(product("unverified"));
      Integer channelId  = channel("Unknown place", ChannelType.COMMUNITY, null, AiContentPolicy.UNKNOWN, false);
      content(campaignId, channelId, "Body");
      MarketingStore.update(MarketingChannel.TABLE_NAME, new QRecord().withValue("id", channelId).withValue("rulesSourceUrl", "https://place.example/rules"));
      new ReadinessChecker().checkCampaign(campaignId);
      assertThat(ruleFindings(campaignId)).singleElement().satisfies(f ->
      {
         assertThat(f.getValueString("title")).contains("Channel rules not verified");
         assertThat(f.getValueString("recommendation")).contains("https://place.example/rules");
      });
   }

}
