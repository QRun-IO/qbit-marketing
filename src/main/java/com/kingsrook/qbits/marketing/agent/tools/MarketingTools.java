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

package com.kingsrook.qbits.marketing.agent.tools;


import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qbits.marketing.MarketingRuntime;
import com.kingsrook.qbits.marketing.MarketingStore;
import com.kingsrook.qbits.marketing.measurement.MeasurementReport;
import com.kingsrook.qbits.marketing.model.MarketingAudience;
import com.kingsrook.qbits.marketing.model.MarketingCampaign;
import com.kingsrook.qbits.marketing.model.MarketingChannel;
import com.kingsrook.qbits.marketing.model.MarketingClaim;
import com.kingsrook.qbits.marketing.model.MarketingContent;
import com.kingsrook.qbits.marketing.model.MarketingEvidenceSource;
import com.kingsrook.qbits.marketing.model.MarketingFinding;
import com.kingsrook.qbits.marketing.model.MarketingProduct;
import com.kingsrook.qbits.marketing.model.enums.AiContentPolicy;
import com.kingsrook.qbits.marketing.model.enums.AuthorType;
import com.kingsrook.qbits.marketing.model.enums.CampaignGoal;
import com.kingsrook.qbits.marketing.model.enums.CampaignStatus;
import com.kingsrook.qbits.marketing.model.enums.ChannelType;
import com.kingsrook.qbits.marketing.model.enums.ClaimStatus;
import com.kingsrook.qbits.marketing.model.enums.ContentStatus;
import com.kingsrook.qbits.marketing.model.enums.FindingSeverity;
import com.kingsrook.qbits.marketing.model.enums.FindingStatus;
import com.kingsrook.qbits.marketing.model.enums.FindingType;
import com.kingsrook.qbits.marketing.model.enums.ProductMaturity;
import com.kingsrook.qbits.marketing.security.MarketingInternalWrite;
import com.kingsrook.qbits.marketing.transport.MarketingTransport;
import com.kingsrook.qqq.backend.core.actions.ai.AiToolKind;
import com.kingsrook.qqq.backend.core.actions.ai.tools.AiJsonSchema;
import com.kingsrook.qqq.backend.core.actions.ai.tools.AiToolException;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import static com.kingsrook.qbits.marketing.agent.tools.MarketingToolContext.map;
import static com.kingsrook.qbits.marketing.agent.tools.MarketingToolContext.optionalInt;
import static com.kingsrook.qbits.marketing.agent.tools.MarketingToolContext.optionalIntList;
import static com.kingsrook.qbits.marketing.agent.tools.MarketingToolContext.optionalString;
import static com.kingsrook.qbits.marketing.agent.tools.MarketingToolContext.optionalStringList;
import static com.kingsrook.qbits.marketing.agent.tools.MarketingToolContext.requireInt;
import static com.kingsrook.qbits.marketing.agent.tools.MarketingToolContext.requireString;


/*******************************************************************************
 ** The marketing agent's tools.
 **
 ** Read tools give the model product evidence, campaign goals, content,
 ** channel rules, findings and results - never CRM personal data (results
 ** carry counts and anonymous actor counts only).
 **
 ** Write-proposal tools create drafts and findings. A draft is always a
 ** DRAFT authored by the agent: the tools cannot approve, schedule or send,
 ** and the approval and execution tables are not writable from here. A draft
 ** that cites a claim which is not released (verified) gets a blocker finding,
 ** which stops approval until a person resolves it.
 *******************************************************************************/
public final class MarketingTools
{
   public static final String GET_CAMPAIGN_BRIEF    = "marketing_get_campaign_brief";
   public static final String LIST_PRODUCT_CLAIMS   = "marketing_list_product_claims";
   public static final String LIST_CAMPAIGN_CONTENT = "marketing_list_campaign_content";
   public static final String GET_CAMPAIGN_RESULTS  = "marketing_get_campaign_results";
   public static final String LIST_CHANNELS         = "marketing_list_channels";
   public static final String LIST_FINDINGS         = "marketing_list_findings";
   public static final String SAVE_DRAFT            = "marketing_save_draft";
   public static final String RECORD_FINDING        = "marketing_record_finding";
   public static final String SUBMIT_PLAN           = "marketing_submit_plan";

   static final int MAX_BODY  = 8000;
   static final int MAX_TEXT  = 2000;
   static final int MAX_SHORT = 255;



   /*******************************************************************************
    ** Utility class - no instances.
    *******************************************************************************/
   private MarketingTools()
   {
   }



   /*******************************************************************************
    ** Every tool.
    *******************************************************************************/
   public static List<MarketingAgentTool> all()
   {
      return (List.of(new GetCampaignBrief(), new ListProductClaims(), new ListCampaignContent(), new GetCampaignResults(), new ListChannels(), new ListFindings(),
         new SaveDraft(), new RecordFinding(), new SubmitPlan()));
   }



   /*******************************************************************************
    ** The read tools (also contributed to the QQQ AI tool kernel).
    *******************************************************************************/
   public static List<MarketingAgentTool> readTools()
   {
      return (all().stream().filter(t -> t.getKind() == AiToolKind.READ).toList());
   }



   /*******************************************************************************
    ** Base: name, description, kind and schema as fields.
    *******************************************************************************/
   abstract static class Base implements MarketingAgentTool
   {
      private final String              name;
      private final String              description;
      private final AiToolKind          kind;
      private final Map<String, Object> schema;



      /*******************************************************************************
       **
       *******************************************************************************/
      Base(String name, String description, AiToolKind kind, Map<String, Object> schema)
      {
         this.name = name;
         this.description = description;
         this.kind = kind;
         this.schema = schema;
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public String getName()
      {
         return (name);
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public String getDescription()
      {
         return (description);
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public AiToolKind getKind()
      {
         return (kind);
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public Map<String, Object> getInputSchema()
      {
         return (schema);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   static Map<String, Object> idSchema(String key, String description)
   {
      return (AiJsonSchema.object(AiJsonSchema.properties(key, AiJsonSchema.integer(description)), List.of(key)));
   }



   /*******************************************************************************
    ** marketing_get_campaign_brief
    *******************************************************************************/
   static class GetCampaignBrief extends Base
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      GetCampaignBrief()
      {
         super(GET_CAMPAIGN_BRIEF, "The campaign's goal, audience, problem, messaging, channels, call to action, destination, schedule and success criteria, "
            + "with its product's brief (maturity, positioning, license, platforms, latest release, adoption path).", AiToolKind.READ, idSchema("campaign_id", "The campaign id."));
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public Map<String, Object> run(MarketingToolContext context, Map<String, Object> input) throws QException
      {
         MarketingCampaign campaign = new MarketingCampaign(context.require(MarketingCampaign.TABLE_NAME, requireInt(input, "campaign_id")));
         MarketingProduct  product  = new MarketingProduct(context.require(MarketingProduct.TABLE_NAME, campaign.getProductId()));

         Map<String, Object> c = map();
         c.put("campaign_id", campaign.getId());
         c.put("name", campaign.getName());
         c.put("goal", label(CampaignGoal.getById(campaign.getGoal())));
         c.put("status", label(CampaignStatus.getById(campaign.getStatus())));
         c.put("objective", campaign.getObjective());
         c.put("problem", campaign.getProblem());
         c.put("messaging", campaign.getMessaging());
         c.put("channel_plan", campaign.getChannelPlan());
         c.put("call_to_action", campaign.getCallToAction());
         c.put("destination_url", campaign.getDestinationUrl());
         c.put("schedule_start", str(campaign.getScheduleStart()));
         c.put("schedule_end", str(campaign.getScheduleEnd()));
         c.put("success_criteria", campaign.getSuccessCriteria());
         c.put("lead_qualification", Boolean.TRUE.equals(campaign.getLeadQualificationEnabled())
            ? "on: at least " + campaign.getQualifyMinFormSubmissions() + " attributed form submission(s)" + (Boolean.TRUE.equals(campaign.getQualifyRequireEmailConsent()) ? " and email consent" : "")
            : "off (no leads are qualified)");

         if(campaign.getAudienceId() != null)
         {
            MarketingAudience audience = new MarketingAudience(context.require(MarketingAudience.TABLE_NAME, campaign.getAudienceId()));
            Map<String, Object> a = map();
            a.put("name", audience.getName());
            a.put("problem", audience.getProblem());
            a.put("useful_task", audience.getUsefulTask());
            a.put("evidence_note", audience.getEvidenceNote());
            c.put("audience", a);
         }
         else
         {
            c.put("audience", null);
         }

         Map<String, Object> p = map();
         p.put("product_id", product.getId());
         p.put("name", product.getName());
         p.put("maturity", label(ProductMaturity.getById(product.getMaturity())));
         p.put("tagline", product.getTagline());
         p.put("summary", product.getSummary());
         p.put("value_propositions", product.getValuePropositions());
         p.put("license", product.getLicense());
         p.put("platforms", product.getPlatforms());
         p.put("latest_release", product.getLatestRelease());
         p.put("adoption_path", product.getAdoptionPath());
         p.put("readiness_notes", product.getReadinessNotes());
         p.put("primary_url", product.getPrimaryUrl());
         p.put("repository_url", product.getRepositoryUrl());
         p.put("documentation_url", product.getDocumentationUrl());
         p.put("last_verified_date", str(product.getLastVerifiedDate()));

         Map<String, Object> rs = map();
         rs.put("campaign", c);
         rs.put("product", p);
         return (rs);
      }
   }



   /*******************************************************************************
    ** marketing_list_product_claims
    *******************************************************************************/
   static class ListProductClaims extends Base
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      ListProductClaims()
      {
         super(LIST_PRODUCT_CLAIMS, "The product's factual claims with status (Released = verified, Planned, Unverified, Disputed), source, verification date "
            + "and whether the source is stale. Only Released claims with a fresh source may be stated as fact.", AiToolKind.READ, idSchema("product_id", "The product id."));
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public Map<String, Object> run(MarketingToolContext context, Map<String, Object> input) throws QException
      {
         Integer productId = requireInt(input, "product_id");
         context.require(MarketingProduct.TABLE_NAME, productId);
         int staleDays = MarketingRuntime.current().getConfig().getStaleSourceDays();

         Map<Integer, MarketingEvidenceSource> sources = new HashMap<>();
         for(QRecord record : context.query(MarketingEvidenceSource.TABLE_NAME, new QQueryFilter(new QFilterCriteria("productId", QCriteriaOperator.EQUALS, productId))))
         {
            MarketingEvidenceSource source = new MarketingEvidenceSource(record);
            sources.put(source.getId(), source);
         }

         List<Map<String, Object>> claims = new ArrayList<>();
         for(QRecord record : context.query(MarketingClaim.TABLE_NAME, new QQueryFilter(new QFilterCriteria("productId", QCriteriaOperator.EQUALS, productId)).withOrderBy(new QFilterOrderBy("id"))))
         {
            MarketingClaim          claim  = new MarketingClaim(record);
            MarketingEvidenceSource source = sources.get(claim.getSourceId());
            Map<String, Object>     m      = map();
            m.put("claim_id", claim.getId());
            m.put("text", claim.getClaimText());
            m.put("status", label(ClaimStatus.getById(claim.getStatus())));
            m.put("category", claim.getCategory());
            m.put("source_title", source == null ? null : source.getTitle());
            m.put("source_url", source == null ? null : source.getUrl());
            m.put("verified_date", str(claim.getVerifiedDate()));
            m.put("source_stale", source == null || source.getVerifiedDate() == null || source.getVerifiedDate().isBefore(LocalDate.now().minusDays(staleDays)));
            m.put("discrepancy", claim.getDiscrepancyNote());
            claims.add(m);
         }

         Map<String, Object> rs = map();
         rs.put("product_id", productId);
         rs.put("source_count", sources.size());
         rs.put("claims", claims);
         rs.put("rows_capped_at", context.getMaxRows());
         return (rs);
      }
   }



   /*******************************************************************************
    ** marketing_list_campaign_content
    *******************************************************************************/
   static class ListCampaignContent extends Base
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      ListCampaignContent()
      {
         super(LIST_CAMPAIGN_CONTENT, "The campaign's existing content items: title, channel, status, revision, author and current text.", AiToolKind.READ,
            idSchema("campaign_id", "The campaign id."));
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public Map<String, Object> run(MarketingToolContext context, Map<String, Object> input) throws QException
      {
         Integer campaignId = requireInt(input, "campaign_id");
         context.require(MarketingCampaign.TABLE_NAME, campaignId);

         List<Map<String, Object>> list = new ArrayList<>();
         for(QRecord record : context.query(MarketingContent.TABLE_NAME, new QQueryFilter(new QFilterCriteria("campaignId", QCriteriaOperator.EQUALS, campaignId)).withOrderBy(new QFilterOrderBy("id"))))
         {
            MarketingContent    content = new MarketingContent(record);
            Map<String, Object> m       = map();
            m.put("content_id", content.getId());
            m.put("title", content.getTitle());
            m.put("channel_id", content.getChannelId());
            m.put("status", label(ContentStatus.getById(content.getStatus())));
            m.put("revision", content.getRevisionNumber());
            m.put("author", label(AuthorType.getById(content.getAuthorType())));
            m.put("body", content.getBody());
            m.put("call_to_action", content.getCallToAction());
            m.put("destination_url", content.getDestinationUrl());
            list.add(m);
         }
         Map<String, Object> rs = map();
         rs.put("campaign_id", campaignId);
         rs.put("content", list);
         return (rs);
      }
   }



   /*******************************************************************************
    ** marketing_get_campaign_results
    *******************************************************************************/
   static class GetCampaignResults extends Base
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      GetCampaignResults()
      {
         super(GET_CAMPAIGN_RESULTS, "The campaign's measured results per stage (raw events and unique actors), which metrics are not instrumented (unknown, not zero), "
            + "and the measurement limitations. Test data is excluded. No personal data.", AiToolKind.READ, idSchema("campaign_id", "The campaign id."));
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public Map<String, Object> run(MarketingToolContext context, Map<String, Object> input) throws QException
      {
         Integer campaignId = requireInt(input, "campaign_id");
         context.require(MarketingCampaign.TABLE_NAME, campaignId);
         Map<String, Object> rs = map();
         rs.put("campaign_id", campaignId);
         rs.putAll(MeasurementReport.forCampaign(campaignId).toMap());
         return (rs);
      }
   }



   /*******************************************************************************
    ** marketing_list_channels
    *******************************************************************************/
   static class ListChannels extends Base
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      ListChannels()
      {
         super(LIST_CHANNELS, "The channels: type, whether active, whether automation and AI-generated content are allowed by their rules, the rules summary and "
            + "source, and whether the channel's transport is working.", AiToolKind.READ, AiJsonSchema.object(new java.util.LinkedHashMap<>(), List.of()));
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public Map<String, Object> run(MarketingToolContext context, Map<String, Object> input) throws QException
      {
         MarketingRuntime          runtime = MarketingRuntime.current();
         List<Map<String, Object>> list    = new ArrayList<>();
         for(QRecord record : context.query(MarketingChannel.TABLE_NAME, new QQueryFilter().withOrderBy(new QFilterOrderBy("id"))))
         {
            MarketingChannel    channel   = new MarketingChannel(record);
            MarketingTransport  transport = runtime.getTransport(channel.getTransportName());
            Map<String, Object> m         = map();
            m.put("channel_id", channel.getId());
            m.put("name", channel.getName());
            m.put("type", label(ChannelType.getById(channel.getChannelType())));
            m.put("active", channel.getIsActive());
            m.put("allows_automation", channel.getAllowsAutomation());
            m.put("ai_content_policy", label(AiContentPolicy.getById(channel.getAiContentPolicy())));
            m.put("rules_summary", channel.getRulesSummary());
            m.put("rules_source_url", channel.getRulesSourceUrl());
            m.put("rules_verified_date", str(channel.getRulesVerifiedDate()));
            m.put("transport", transport == null ? "not registered (not a working connector)" : transport.getStatus().state().name());
            list.add(m);
         }
         Map<String, Object> rs = map();
         rs.put("channels", list);
         return (rs);
      }
   }



   /*******************************************************************************
    ** marketing_list_findings
    *******************************************************************************/
   static class ListFindings extends Base
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      ListFindings()
      {
         super(LIST_FINDINGS, "The campaign's open findings (gaps, unsupported claims, stale sources, channel rules, measurement limitations).", AiToolKind.READ,
            idSchema("campaign_id", "The campaign id."));
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public Map<String, Object> run(MarketingToolContext context, Map<String, Object> input) throws QException
      {
         Integer campaignId = requireInt(input, "campaign_id");
         MarketingCampaign campaign = new MarketingCampaign(context.require(MarketingCampaign.TABLE_NAME, campaignId));

         List<Map<String, Object>> list = new ArrayList<>();
         //////////////////////////////////////////////////////////////////
         // open AND (this campaign's OR the product's own findings)     //
         //////////////////////////////////////////////////////////////////
         QQueryFilter scope = new QQueryFilter()
            .withBooleanOperator(QQueryFilter.BooleanOperator.OR)
            .withSubFilter(new QQueryFilter(new QFilterCriteria("campaignId", QCriteriaOperator.EQUALS, campaignId)))
            .withSubFilter(new QQueryFilter(new QFilterCriteria("productId", QCriteriaOperator.EQUALS, campaign.getProductId()), new QFilterCriteria("campaignId", QCriteriaOperator.IS_BLANK)));
         QQueryFilter filter = new QQueryFilter(new QFilterCriteria("status", QCriteriaOperator.EQUALS, FindingStatus.OPEN.getId()))
            .withSubFilter(scope)
            .withOrderBy(new QFilterOrderBy("id"));

         for(QRecord record : context.query(MarketingFinding.TABLE_NAME, filter))
         {
            MarketingFinding    finding = new MarketingFinding(record);
            Map<String, Object> m       = map();
            m.put("finding_id", finding.getId());
            m.put("type", label(FindingType.getById(finding.getFindingType())));
            m.put("severity", label(FindingSeverity.getById(finding.getSeverity())));
            m.put("title", finding.getTitle());
            m.put("detail", finding.getDetail());
            m.put("recommendation", finding.getRecommendation());
            m.put("scope", finding.getCampaignId() == null ? "product" : "campaign");
            list.add(m);
         }
         Map<String, Object> rs = map();
         rs.put("campaign_id", campaignId);
         rs.put("open_findings", list);
         return (rs);
      }
   }



   /*******************************************************************************
    ** marketing_save_draft
    *******************************************************************************/
   static class SaveDraft extends Base
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      SaveDraft()
      {
         super(SAVE_DRAFT, "Save a channel-appropriate draft (or a new revision of an existing draft) for human review. It is stored as a Draft written by the agent; "
            + "it is never approved, scheduled or published by this tool. Cite the claim_ids of the facts it states; only Released claims may be stated as fact.",
            AiToolKind.WRITE_PROPOSAL,
            AiJsonSchema.object(AiJsonSchema.properties(
               "campaign_id", AiJsonSchema.integer("The campaign id."),
               "channel_id", AiJsonSchema.integer("The channel id (see marketing_list_channels)."),
               "title", AiJsonSchema.string("Title or subject, at most 255 characters."),
               "body", AiJsonSchema.string("The exact text to publish or send, at most 8000 characters."),
               "call_to_action", AiJsonSchema.string("The call to action, at most 500 characters."),
               "destination_url", AiJsonSchema.string("Where the call to action leads."),
               "claim_ids", AiJsonSchema.array(AiJsonSchema.integer("A claim id."), "The claims whose facts the text states."),
               "content_id", AiJsonSchema.integer("To revise an existing draft instead of creating one."),
               "change_note", AiJsonSchema.string("What changed and why, at most 500 characters.")),
               List.of("campaign_id", "channel_id", "title", "body", "claim_ids")));
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public Map<String, Object> run(MarketingToolContext context, Map<String, Object> input) throws QException
      {
         Integer       campaignId  = requireInt(input, "campaign_id");
         Integer       channelId   = requireInt(input, "channel_id");
         String        title       = requireString(input, "title", MAX_SHORT);
         String        body        = requireString(input, "body", MAX_BODY);
         String        cta         = optionalString(input, "call_to_action", 500);
         String        destination = optionalString(input, "destination_url", 1000);
         List<Integer> claimIds    = optionalIntList(input, "claim_ids", 50);
         Integer       contentId   = optionalInt(input, "content_id");
         String        changeNote  = optionalString(input, "change_note", 500);

         if(destination != null && !(destination.startsWith("https://") || destination.startsWith("http://")))
         {
            throw (AiToolException.invalidInput("destination_url must be an http(s) URL."));
         }

         MarketingCampaign campaign = new MarketingCampaign(context.require(MarketingCampaign.TABLE_NAME, campaignId));
         MarketingChannel  channel  = new MarketingChannel(context.require(MarketingChannel.TABLE_NAME, channelId));
         if(!Boolean.TRUE.equals(channel.getIsActive()))
         {
            throw (AiToolException.invalidInput("Channel " + channel.getName() + " is not active."));
         }
         if(AiContentPolicy.NOT_ALLOWED.getId().equals(channel.getAiContentPolicy()))
         {
            throw (AiToolException.invalidInput("Channel " + channel.getName() + " does not allow AI-generated content; a person must write for it. Record a finding instead."));
         }

         //////////////////////////////////////////////////////////////////
         // cited claims must belong to the product; non-released ones   //
         // become blocker findings on the draft                         //
         //////////////////////////////////////////////////////////////////
         List<MarketingClaim> unsupported = new ArrayList<>();
         for(Integer claimId : claimIds)
         {
            MarketingClaim claim = new MarketingClaim(context.require(MarketingClaim.TABLE_NAME, claimId));
            if(!campaign.getProductId().equals(claim.getProductId()))
            {
               throw (AiToolException.invalidInput("Claim " + claimId + " belongs to another product."));
            }
            if(!ClaimStatus.RELEASED.getId().equals(claim.getStatus()))
            {
               unsupported.add(claim);
            }
         }

         MarketingAgentRunRef run = new MarketingAgentRunRef(context);
         QRecord stored = MarketingInternalWrite.run(() ->
         {
            if(contentId == null)
            {
               return (MarketingStore.insert(new MarketingContent()
                  .withCampaignId(campaignId)
                  .withChannelId(channelId)
                  .withTitle(title)
                  .withBody(body)
                  .withCallToAction(cta == null ? campaign.getCallToAction() : cta)
                  .withDestinationUrl(destination == null ? campaign.getDestinationUrl() : destination)
                  .withStatus(ContentStatus.DRAFT.getId())
                  .withAuthorType(AuthorType.AGENT.getId())
                  .withAgentRunId(run.id())));
            }

            MarketingContent existing = new MarketingContent(context.require(MarketingContent.TABLE_NAME, contentId));
            if(!campaignId.equals(existing.getCampaignId()))
            {
               throw (AiToolException.invalidInput("Content " + contentId + " belongs to another campaign."));
            }
            MarketingStore.update(MarketingContent.TABLE_NAME, new QRecord()
               .withValue("id", contentId)
               .withValue("channelId", channelId)
               .withValue("title", title)
               .withValue("body", body)
               .withValue("callToAction", cta == null ? existing.getCallToAction() : cta)
               .withValue("destinationUrl", destination == null ? existing.getDestinationUrl() : destination)
               .withValue("authorType", AuthorType.AGENT.getId())
               .withValue("agentRunId", run.id()));
            return (MarketingStore.get(MarketingContent.TABLE_NAME, contentId));
         });
         Integer savedId = stored.getValueInteger("id");
         context.addDraft(savedId);

         for(MarketingClaim claim : unsupported)
         {
            MarketingStore.insert(new MarketingFinding()
               .withProductId(campaign.getProductId())
               .withCampaignId(campaignId)
               .withContentId(savedId)
               .withClaimId(claim.getId())
               .withAgentRunId(run.id())
               .withFindingType(FindingType.UNSUPPORTED_CLAIM.getId())
               .withSeverity(FindingSeverity.BLOCKER.getId())
               .withStatus(FindingStatus.OPEN.getId())
               .withTitle(truncate("Draft cites a claim that is " + ClaimStatus.getById(claim.getStatus()).getLabel() + ": " + claim.getClaimText()))
               .withDetail("Only released, verified claims may be stated as fact.")
               .withRecommendation("Remove or rephrase the claim (e.g. as planned), or verify it first.")
               .withOrigin("AGENT"));
            context.addFinding();
         }

         Map<String, Object> rs = map();
         rs.put("content_id", savedId);
         rs.put("status", "Draft (awaiting human review; not approved, scheduled or published)");
         rs.put("revision", stored.getValueInteger("revisionNumber"));
         rs.put("unsupported_claims_flagged", unsupported.size());
         if(changeNote != null)
         {
            rs.put("change_note", changeNote);
         }
         return (rs);
      }
   }



   /*******************************************************************************
    ** The agent run id of a context (null outside an agent run).
    *******************************************************************************/
   record MarketingAgentRunRef(Integer id)
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      MarketingAgentRunRef(MarketingToolContext context)
      {
         this(context.getAgentRun() == null ? null : context.getAgentRun().getId());
      }
   }



   /*******************************************************************************
    ** marketing_record_finding
    *******************************************************************************/
   static class RecordFinding extends Base
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      RecordFinding()
      {
         super(RECORD_FINDING, "Record a finding for a person: an unsupported claim, stale source, missing evidence, launch-readiness gap, source discrepancy, channel rule, "
            + "measurement limitation, or a recommended next action or experiment.", AiToolKind.WRITE_PROPOSAL,
            AiJsonSchema.object(AiJsonSchema.properties(
               "campaign_id", AiJsonSchema.integer("The campaign id."),
               "finding_type", AiJsonSchema.enumOf(names(FindingType.values()), "The kind of finding."),
               "severity", AiJsonSchema.enumOf(names(FindingSeverity.values()), "BLOCKER stops approval of the content it names."),
               "title", AiJsonSchema.string("One line, at most 255 characters."),
               "detail", AiJsonSchema.string("What and why, citing claim ids or sources, at most 2000 characters."),
               "recommendation", AiJsonSchema.string("The concrete next action or experiment, at most 2000 characters."),
               "claim_id", AiJsonSchema.integer("The claim it is about, if any."),
               "content_id", AiJsonSchema.integer("The content it is about, if any.")),
               List.of("campaign_id", "finding_type", "severity", "title", "detail")));
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public Map<String, Object> run(MarketingToolContext context, Map<String, Object> input) throws QException
      {
         Integer           campaignId = requireInt(input, "campaign_id");
         MarketingCampaign campaign   = new MarketingCampaign(context.require(MarketingCampaign.TABLE_NAME, campaignId));
         FindingType       type       = enumValue(FindingType.class, requireString(input, "finding_type", 64), "finding_type");
         FindingSeverity   severity   = enumValue(FindingSeverity.class, requireString(input, "severity", 16), "severity");
         Integer           claimId    = optionalInt(input, "claim_id");
         Integer           contentId  = optionalInt(input, "content_id");
         if(claimId != null && !campaign.getProductId().equals(new MarketingClaim(context.require(MarketingClaim.TABLE_NAME, claimId)).getProductId()))
         {
            throw (AiToolException.invalidInput("Claim " + claimId + " belongs to another product."));
         }
         if(contentId != null && !campaignId.equals(new MarketingContent(context.require(MarketingContent.TABLE_NAME, contentId)).getCampaignId()))
         {
            throw (AiToolException.invalidInput("Content " + contentId + " belongs to another campaign."));
         }

         QRecord stored = MarketingStore.insert(new MarketingFinding()
            .withProductId(campaign.getProductId())
            .withCampaignId(campaignId)
            .withClaimId(claimId)
            .withContentId(contentId)
            .withAgentRunId(new MarketingAgentRunRef(context).id())
            .withFindingType(type.getId())
            .withSeverity(severity.getId())
            .withStatus(FindingStatus.OPEN.getId())
            .withTitle(requireString(input, "title", MAX_SHORT))
            .withDetail(requireString(input, "detail", MAX_TEXT))
            .withRecommendation(optionalString(input, "recommendation", MAX_TEXT))
            .withOrigin("AGENT"));
         context.addFinding();

         Map<String, Object> rs = map();
         rs.put("finding_id", stored.getValueInteger("id"));
         rs.put("status", "Open (for a person to review)");
         return (rs);
      }
   }



   /*******************************************************************************
    ** marketing_submit_plan
    *******************************************************************************/
   static class SubmitPlan extends Base
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      SubmitPlan()
      {
         super(SUBMIT_PLAN, "Submit the proposed campaign plan for a person to review. It is stored on the agent run and as a Plan finding; it changes nothing else.",
            AiToolKind.WRITE_PROPOSAL,
            AiJsonSchema.object(AiJsonSchema.properties(
               "campaign_id", AiJsonSchema.integer("The campaign id."),
               "summary", AiJsonSchema.string("The plan in a few sentences."),
               "audience_and_problem", AiJsonSchema.string("Who, their real problem, and the useful task the product helps complete."),
               "channels", AiJsonSchema.array(AiJsonSchema.string("A channel and why it fits (respecting its rules)."), "Proposed channels."),
               "next_actions", AiJsonSchema.array(AiJsonSchema.string("A concrete next action."), "Next actions, in order."),
               "success_criteria", AiJsonSchema.string("How success will be measured, and which metrics need instrumentation."),
               "risks", AiJsonSchema.array(AiJsonSchema.string("A risk or open question."), "Risks and open questions.")),
               List.of("campaign_id", "summary", "audience_and_problem", "channels", "next_actions", "success_criteria")));
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public Map<String, Object> run(MarketingToolContext context, Map<String, Object> input) throws QException
      {
         Integer           campaignId = requireInt(input, "campaign_id");
         MarketingCampaign campaign   = new MarketingCampaign(context.require(MarketingCampaign.TABLE_NAME, campaignId));

         Map<String, Object> plan = map();
         plan.put("campaign_id", campaignId);
         plan.put("summary", requireString(input, "summary", MAX_TEXT));
         plan.put("audience_and_problem", requireString(input, "audience_and_problem", MAX_TEXT));
         plan.put("channels", optionalStringList(input, "channels", 10, 500));
         plan.put("next_actions", optionalStringList(input, "next_actions", 15, 500));
         plan.put("success_criteria", requireString(input, "success_criteria", MAX_TEXT));
         plan.put("risks", optionalStringList(input, "risks", 15, 500));
         context.setPlan(plan);

         QRecord stored = MarketingStore.insert(new MarketingFinding()
            .withProductId(campaign.getProductId())
            .withCampaignId(campaignId)
            .withAgentRunId(new MarketingAgentRunRef(context).id())
            .withFindingType(FindingType.PLAN.getId())
            .withSeverity(FindingSeverity.INFO.getId())
            .withStatus(FindingStatus.OPEN.getId())
            .withTitle(truncate("Proposed plan: " + plan.get("summary")))
            .withDetail(JsonUtils.toPrettyJson(plan))
            .withRecommendation(String.join("\n", optionalStringList(input, "next_actions", 15, 500)))
            .withOrigin("AGENT"));
         context.addFinding();

         Map<String, Object> rs = map();
         rs.put("plan_finding_id", stored.getValueInteger("id"));
         rs.put("status", "Stored for review; nothing else changed.");
         return (rs);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   static <E extends Enum<E>> E enumValue(Class<E> type, String value, String key) throws AiToolException
   {
      try
      {
         return (Enum.valueOf(type, value));
      }
      catch(IllegalArgumentException e)
      {
         throw (AiToolException.invalidInput("The input property '" + key + "' must be one of " + names(type.getEnumConstants()) + "."));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   static List<String> names(Enum<?>[] values)
   {
      List<String> names = new ArrayList<>();
      for(Enum<?> value : values)
      {
         names.add(value.name());
      }
      return (names);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   static String label(Object pvsEnum)
   {
      if(pvsEnum == null)
      {
         return (null);
      }
      if(pvsEnum instanceof com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.PossibleValueEnum<?> e)
      {
         return (e.getPossibleValueLabel());
      }
      return (String.valueOf(pvsEnum));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   static String str(Object value)
   {
      return (value == null ? null : String.valueOf(value));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   static String truncate(String text)
   {
      return (text == null ? null : (text.length() > 255 ? text.substring(0, 252) + "..." : text));
   }

}
