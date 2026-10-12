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

package com.kingsrook.qbits.marketing.readiness;


import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qbits.marketing.MarketingRuntime;
import com.kingsrook.qbits.marketing.MarketingStore;
import com.kingsrook.qbits.marketing.model.MarketingAudience;
import com.kingsrook.qbits.marketing.model.MarketingCampaign;
import com.kingsrook.qbits.marketing.model.MarketingChannel;
import com.kingsrook.qbits.marketing.model.MarketingClaim;
import com.kingsrook.qbits.marketing.model.MarketingContent;
import com.kingsrook.qbits.marketing.model.MarketingEvidenceSource;
import com.kingsrook.qbits.marketing.model.MarketingFinding;
import com.kingsrook.qbits.marketing.model.MarketingMetric;
import com.kingsrook.qbits.marketing.model.MarketingProduct;
import com.kingsrook.qbits.marketing.model.enums.AiContentPolicy;
import com.kingsrook.qbits.marketing.model.enums.CampaignGoal;
import com.kingsrook.qbits.marketing.model.enums.ClaimStatus;
import com.kingsrook.qbits.marketing.model.enums.FindingSeverity;
import com.kingsrook.qbits.marketing.model.enums.FindingStatus;
import com.kingsrook.qbits.marketing.model.enums.FindingType;
import com.kingsrook.qbits.marketing.model.enums.InstrumentationStatus;
import com.kingsrook.qbits.marketing.model.enums.ProductMaturity;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;


/*******************************************************************************
 ** Deterministic checks of evidence and launch readiness - no model needed.
 **
 ** Product: missing evidence, stale sources (older than staleSourceDays),
 ** unverified, unsourced and disputed claims, missing audiences, prelaunch
 ** maturity. Campaign: goal vs maturity, missing audience / call to action /
 ** destination / success criteria, measurement gaps, channel rules.
 **
 ** Each run replaces its own previous open findings for the same scope (so a
 ** fixed problem disappears and a remaining one stays visible), and never
 ** touches findings written by people or the agent.
 *******************************************************************************/
public class ReadinessChecker
{
   public static final String ORIGIN = "CHECK";

   private final List<MarketingFinding> findings = new ArrayList<>();



   /*******************************************************************************
    ** Check a product; returns the number of open findings recorded.
    *******************************************************************************/
   public int checkProduct(Integer productId) throws QException
   {
      MarketingProduct product = new MarketingProduct(MarketingStore.require(MarketingProduct.TABLE_NAME, productId));
      findings.clear();
      int staleDays = MarketingRuntime.current().getConfig().getStaleSourceDays();

      List<QRecord> sources = MarketingStore.queryBy(MarketingEvidenceSource.TABLE_NAME, "productId", productId);
      if(sources.isEmpty())
      {
         add(productId, null, null, null, FindingType.MISSING_EVIDENCE, FindingSeverity.BLOCKER, "No evidence sources for " + product.getName(),
            "Nothing about this product is verified against a source.", "Add the official site, repository, documentation and release pages as evidence sources.");
      }

      Map<Integer, MarketingEvidenceSource> sourceById = new HashMap<>();
      for(QRecord record : sources)
      {
         MarketingEvidenceSource source = new MarketingEvidenceSource(record);
         sourceById.put(source.getId(), source);
         if(isStale(source.getVerifiedDate(), staleDays))
         {
            add(productId, null, null, source.getId(), FindingType.STALE_SOURCE, FindingSeverity.WARNING, "Stale source: " + source.getTitle(),
               source.getVerifiedDate() == null ? "Never verified." : "Last verified " + source.getVerifiedDate() + " (over " + staleDays + " days ago).",
               "Re-read " + source.getUrl() + " and update the verified date and claims.");
         }
      }

      for(QRecord record : MarketingStore.queryBy(MarketingClaim.TABLE_NAME, "productId", productId))
      {
         MarketingClaim claim  = new MarketingClaim(record);
         ClaimStatus    status = ClaimStatus.getById(claim.getStatus());
         if(status == ClaimStatus.DISPUTED)
         {
            add(productId, null, claim.getId(), claim.getSourceId(), FindingType.SOURCE_DISCREPANCY, FindingSeverity.WARNING, "Sources disagree: " + truncate(claim.getClaimText()),
               claim.getDiscrepancyNote(), "Decide which source is right (do not pick the more promotional one) and fix the other, then mark the claim.");
         }
         else if(status == ClaimStatus.UNVERIFIED)
         {
            add(productId, null, claim.getId(), null, FindingType.MISSING_EVIDENCE, FindingSeverity.WARNING, "Unverified claim: " + truncate(claim.getClaimText()),
               "Not usable in content until verified.", "Verify it against an official source.");
         }
         else if(status == ClaimStatus.RELEASED && claim.getSourceId() == null)
         {
            add(productId, null, claim.getId(), null, FindingType.MISSING_EVIDENCE, FindingSeverity.WARNING, "Claim without a source: " + truncate(claim.getClaimText()),
               "Marked released, but no source is recorded.", "Link the source it was verified against.");
         }
         else if(status == ClaimStatus.RELEASED && sourceById.containsKey(claim.getSourceId()) && isStale(sourceById.get(claim.getSourceId()).getVerifiedDate(), staleDays))
         {
            add(productId, null, claim.getId(), claim.getSourceId(), FindingType.STALE_SOURCE, FindingSeverity.WARNING, "Claim rests on a stale source: " + truncate(claim.getClaimText()),
               null, "Re-verify before using it in new content.");
         }
      }

      if(MarketingStore.queryBy(MarketingAudience.TABLE_NAME, "productId", productId).isEmpty())
      {
         add(productId, null, null, null, FindingType.READINESS_GAP, FindingSeverity.WARNING, "No audience defined for " + product.getName(),
            "Campaigns should start from a specific audience, a real problem and a useful task.", "Add at least one audience.");
      }

      ProductMaturity maturity = ProductMaturity.getById(product.getMaturity());
      if(maturity == ProductMaturity.PRELAUNCH || maturity == ProductMaturity.CONCEPT)
      {
         add(productId, null, null, null, FindingType.READINESS_GAP, FindingSeverity.INFO, product.getName() + " is " + maturity.getLabel(),
            "Adoption and demand campaigns would promote something people cannot get yet.", "Use prelaunch research / tester-interest campaigns until a release is public.");
      }

      return (save(productId, null));
   }



   /*******************************************************************************
    ** Check a campaign; returns the number of open findings recorded.
    *******************************************************************************/
   public int checkCampaign(Integer campaignId) throws QException
   {
      MarketingCampaign campaign = new MarketingCampaign(MarketingStore.require(MarketingCampaign.TABLE_NAME, campaignId));
      MarketingProduct  product  = new MarketingProduct(MarketingStore.require(MarketingProduct.TABLE_NAME, campaign.getProductId()));
      Integer           pid      = campaign.getProductId();
      findings.clear();

      ProductMaturity maturity = ProductMaturity.getById(product.getMaturity());
      CampaignGoal    goal     = CampaignGoal.getById(campaign.getGoal());
      if((maturity == ProductMaturity.PRELAUNCH || maturity == ProductMaturity.CONCEPT) && (goal == CampaignGoal.OPEN_SOURCE_ADOPTION || goal == CampaignGoal.COMMERCIAL_DEMAND))
      {
         add(pid, campaignId, null, null, FindingType.READINESS_GAP, FindingSeverity.BLOCKER, "Goal does not fit a " + maturity.getLabel() + " product",
            goal.getLabel() + " needs something people can install or buy.", "Change the goal to Prelaunch research / tester interest, or wait for a public release.");
      }
      if(campaign.getAudienceId() == null)
      {
         add(pid, campaignId, null, null, FindingType.READINESS_GAP, FindingSeverity.WARNING, "No audience", "The campaign does not name its audience.", "Pick one audience.");
      }
      if(!StringUtils.hasContent(campaign.getCallToAction()) || !StringUtils.hasContent(campaign.getDestinationUrl()))
      {
         add(pid, campaignId, null, null, FindingType.READINESS_GAP, FindingSeverity.WARNING, "No call to action or destination",
            "Content should lead to one concrete next action.", "Set the call to action and its destination URL.");
      }
      if(!StringUtils.hasContent(campaign.getSuccessCriteria()))
      {
         add(pid, campaignId, null, null, FindingType.READINESS_GAP, FindingSeverity.WARNING, "No success criteria",
            "Without them the result can't be judged.", "Write measurable success criteria.");
      }

      List<QRecord> metrics = MarketingStore.queryBy(MarketingMetric.TABLE_NAME, "campaignId", campaignId);
      if(metrics.isEmpty())
      {
         add(pid, campaignId, null, null, FindingType.MEASUREMENT_LIMITATION, FindingSeverity.WARNING, "No measurement plan",
            "Nothing will be measured.", "Add metrics per stage, and say which ones are instrumented.");
      }
      for(QRecord metric : metrics)
      {
         if(InstrumentationStatus.NOT_INSTRUMENTED.getId().equals(metric.getValueInteger("instrumentationStatus")))
         {
            add(pid, campaignId, null, null, FindingType.MEASUREMENT_LIMITATION, FindingSeverity.INFO, "Not instrumented: " + metric.getValueString("name"),
               "Its result will be shown as unknown, not zero.", "Add instrumentation if this metric matters for the success criteria.");
         }
      }
      if(goal == CampaignGoal.COMMERCIAL_DEMAND && !Boolean.TRUE.equals(campaign.getLeadQualificationEnabled()))
      {
         add(pid, campaignId, null, null, FindingType.MEASUREMENT_LIMITATION, FindingSeverity.WARNING, "No lead qualification rule",
            "Qualified demand cannot be measured: visitors and form submissions are not leads by themselves.", "Turn on lead qualification and set its rule.");
      }

      for(QRecord contentRecord : MarketingStore.queryBy(MarketingContent.TABLE_NAME, "campaignId", campaignId))
      {
         Integer channelId = contentRecord.getValueInteger("channelId");
         QRecord channel   = channelId == null ? null : MarketingStore.get(MarketingChannel.TABLE_NAME, channelId);
         if(channel == null)
         {
            continue;
         }
         if(AiContentPolicy.UNKNOWN.getId().equals(channel.getValueInteger("aiContentPolicy")) || channel.getValue("rulesVerifiedDate") == null)
         {
            add(pid, campaignId, contentRecord.getValueInteger("id"), null, FindingType.CHANNEL_RULE, FindingSeverity.WARNING,
               "Channel rules not verified: " + channel.getValueString("name"),
               "The channel's rules (including on AI-generated content and automation) are not recorded as verified.", "Read the channel's current rules and record them on the channel.");
         }
      }

      return (save(pid, campaignId));
   }



   /*******************************************************************************
    ** Close this checker's previous open findings for the scope, then store the
    ** new ones.
    *******************************************************************************/
   private int save(Integer productId, Integer campaignId) throws QException
   {
      QQueryFilter previous = new QQueryFilter(
         new QFilterCriteria("origin", QCriteriaOperator.EQUALS, ORIGIN),
         new QFilterCriteria("status", QCriteriaOperator.EQUALS, FindingStatus.OPEN.getId()));
      if(campaignId != null)
      {
         previous.addCriteria(new QFilterCriteria("campaignId", QCriteriaOperator.EQUALS, campaignId));
      }
      else
      {
         previous.addCriteria(new QFilterCriteria("productId", QCriteriaOperator.EQUALS, productId));
         previous.addCriteria(new QFilterCriteria("campaignId", QCriteriaOperator.IS_BLANK));
      }
      for(QRecord old : MarketingStore.query(MarketingFinding.TABLE_NAME, previous))
      {
         MarketingStore.update(MarketingFinding.TABLE_NAME, new QRecord().withValue("id", old.getValue("id")).withValue("status", FindingStatus.RESOLVED.getId()));
      }

      for(MarketingFinding finding : findings)
      {
         MarketingStore.insert(finding);
      }
      return (findings.size());
   }



   /*******************************************************************************
    ** The findings of the last check.
    *******************************************************************************/
   public List<MarketingFinding> getFindings()
   {
      return (findings);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private void add(Integer productId, Integer campaignId, Integer claimOrContentId, Integer sourceId, FindingType type, FindingSeverity severity, String title, String detail,
      String recommendation)
   {
      MarketingFinding finding = new MarketingFinding()
         .withProductId(productId)
         .withCampaignId(campaignId)
         .withSourceId(sourceId)
         .withFindingType(type.getId())
         .withSeverity(severity.getId())
         .withStatus(FindingStatus.OPEN.getId())
         .withTitle(truncate(title))
         .withDetail(detail)
         .withRecommendation(recommendation)
         .withOrigin(ORIGIN);
      if(type == FindingType.CHANNEL_RULE)
      {
         finding.setContentId(claimOrContentId);
      }
      else
      {
         finding.setClaimId(claimOrContentId);
      }
      findings.add(finding);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   static boolean isStale(LocalDate verified, int staleDays)
   {
      return (verified == null || verified.isBefore(LocalDate.now().minusDays(staleDays)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static String truncate(String text)
   {
      String value = ValueUtils.getValueAsString(text);
      if(value == null)
      {
         return ("");
      }
      return (value.length() > 200 ? value.substring(0, 197) + "..." : value);
   }

}
