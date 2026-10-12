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


import java.time.Instant;
import java.util.List;
import java.util.Map;
import com.kingsrook.qbits.marketing.agent.KernelToolRegistration;
import com.kingsrook.qbits.marketing.agent.tools.MarketingTools;
import com.kingsrook.qbits.marketing.approval.ApprovalService;
import com.kingsrook.qbits.marketing.model.MarketingCampaign;
import com.kingsrook.qbits.marketing.model.MarketingClaim;
import com.kingsrook.qbits.marketing.model.MarketingContent;
import com.kingsrook.qbits.marketing.model.MarketingExecution;
import com.kingsrook.qbits.marketing.model.MarketingFinding;
import com.kingsrook.qbits.marketing.model.MarketingProduct;
import com.kingsrook.qbits.marketing.model.enums.CampaignGoal;
import com.kingsrook.qbits.marketing.model.enums.ClaimStatus;
import com.kingsrook.qbits.marketing.model.enums.FindingSeverity;
import com.kingsrook.qbits.marketing.model.enums.FindingStatus;
import com.kingsrook.qbits.marketing.model.enums.FindingType;
import com.kingsrook.qbits.marketing.processes.MarketingActions;
import com.kingsrook.qbits.marketing.readiness.ReadinessChecker;
import com.kingsrook.qbits.marketing.seed.MarketingStarterData;
import com.kingsrook.qbits.marketing.transport.MarketingTransport;
import com.kingsrook.qbits.marketing.transport.QqqMessagingEmailTransport;
import com.kingsrook.qbits.marketing.widgets.MarketingWidgets;
import com.kingsrook.qqq.backend.core.actions.ai.AiToolResult;
import com.kingsrook.qqq.backend.core.actions.ai.AiToolKernelSettings;
import com.kingsrook.qqq.backend.core.actions.ai.exposure.AiExposurePolicy;
import com.kingsrook.qqq.backend.core.actions.ai.output.AiRedactor;
import com.kingsrook.qqq.backend.core.actions.ai.tools.AbstractAiTool;
import com.kingsrook.qqq.backend.core.actions.ai.tools.AiToolContext;
import com.kingsrook.qqq.backend.core.actions.ai.tools.AiToolInput;
import com.kingsrook.qqq.backend.core.actions.dashboard.RenderWidgetAction;
import com.kingsrook.qqq.backend.core.actions.processes.QProcessCallbackFactory;
import com.kingsrook.qqq.backend.core.actions.processes.RunProcessAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLine;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunProcessOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetOutput;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.RawHTML;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.TableData;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Widgets, readiness checks, the kernel adapter, transport status and the
 ** record actions' standard summary lines.
 *******************************************************************************/
class ComponentsTest extends BaseMarketingTest
{

   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   protected void customizeConfig(MarketingQBitConfig config)
   {
      config.withTransport(new QqqMessagingEmailTransport().withMessagingProviderName("email").withFromAddress("news@example.org"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private RenderWidgetOutput render(String widget, Integer id) throws QException
   {
      RenderWidgetInput input = new RenderWidgetInput().withWidgetMetaData(qInstance.getWidget(widget));
      if(id != null)
      {
         input.setQueryParams(new java.util.HashMap<>(Map.of("id", String.valueOf(id))));
      }
      return (new RenderWidgetAction().execute(input));
   }



   /*******************************************************************************
    ** The dashboard shows real data, the configuration honestly, and nothing
    ** without marketing.read.
    *******************************************************************************/
   @Test
   void testWidgets() throws QException
   {
      new MarketingStarterData().load();
      Integer campaignId = MarketingStore.queryBy(MarketingCampaign.TABLE_NAME, "name", "QQQ support inquiries").get(0).getValueInteger("id");

      TableData config = (TableData) render(MarketingWidgets.CONFIGURATION, null).getWidgetData();
      assertThat(config.getRows()).anySatisfy(r -> assertThat(String.valueOf(r.get("status"))).isEqualTo("Not configured"))
         .anySatisfy(r -> assertThat(String.valueOf(r.get("status"))).startsWith("Test only"));
      TableData campaigns = (TableData) render(MarketingWidgets.CAMPAIGNS, null).getWidgetData();
      assertThat(campaigns.getRows()).hasSize(4);
      assertThat(campaigns.getColumns()).hasSizeLessThanOrEqualTo(3);
      assertThat(((TableData) render(MarketingWidgets.AGENT_ACTIVITY, null).getWidgetData()).getNoRowsFoundHTML()).contains("not run yet");
      assertThat(((TableData) render(MarketingWidgets.NEEDS_ATTENTION, null).getWidgetData()).getRows()).isEmpty();

      TableData results = (TableData) render(MarketingWidgets.CAMPAIGN_RESULTS, campaignId).getWidgetData();
      assertThat(results.getRows()).anySatisfy(r -> assertThat(String.valueOf(r.get("result"))).startsWith("0 unique actors"));

      Integer channelId = testChannel();
      Integer contentId = content(campaignId, channelId, "<script>alert(1)</script> Exact text.");
      RawHTML proposal  = (RawHTML) render(MarketingWidgets.CONTENT_PROPOSAL, contentId).getWidgetData();
      assertThat(proposal.getHtml()).contains("&lt;script&gt;").doesNotContain("<script>").contains("Not approved");

      as(user("no-read", com.kingsrook.qbits.marketing.security.MarketingPermissions.APP_ACCESS));
      assertThat(((RawHTML) render(MarketingWidgets.CAMPAIGNS, null).getWidgetData()).getHtml()).contains("marketing.read");
   }



   /*******************************************************************************
    ** The deterministic checks flag disputed claims, prelaunch goals and
    ** measurement gaps, and replace their own previous findings.
    *******************************************************************************/
   @Test
   void testReadiness() throws QException
   {
      new MarketingStarterData().load();
      QRecord mdv = MarketingStore.queryBy(MarketingProduct.TABLE_NAME, "slug", "markdownviewr-windows").get(0);
      int     first = new ReadinessChecker().checkProduct(mdv.getValueInteger("id"));
      assertThat(MarketingStore.query(MarketingFinding.TABLE_NAME, new QQueryFilter(new QFilterCriteria("productId", QCriteriaOperator.EQUALS, mdv.getValue("id")),
         new QFilterCriteria("findingType", QCriteriaOperator.EQUALS, FindingType.SOURCE_DISCREPANCY.getId())))).hasSize(2);
      assertThat(new ReadinessChecker().checkProduct(mdv.getValueInteger("id"))).isEqualTo(first);
      assertThat(MarketingStore.count(MarketingFinding.TABLE_NAME, new QQueryFilter(new QFilterCriteria("status", QCriteriaOperator.EQUALS, FindingStatus.OPEN.getId()),
         new QFilterCriteria("productId", QCriteriaOperator.EQUALS, mdv.getValue("id"))))).as("no duplicates on re-run").isEqualTo(first);

      Integer testers = MarketingStore.queryBy(MarketingCampaign.TABLE_NAME, "productId", mdv.getValue("id")).get(0).getValueInteger("id");
      new ReadinessChecker().checkCampaign(testers);
      assertThat(MarketingStore.queryBy(MarketingFinding.TABLE_NAME, "campaignId", testers)).noneMatch(f -> FindingSeverity.BLOCKER.getId().equals(f.getValueInteger("severity")));

      MarketingStore.update(MarketingCampaign.TABLE_NAME, new QRecord().withValue("id", testers).withValue("goal", CampaignGoal.OPEN_SOURCE_ADOPTION.getId()));
      new ReadinessChecker().checkCampaign(testers);
      assertThat(MarketingStore.queryBy(MarketingFinding.TABLE_NAME, "campaignId", testers))
         .anyMatch(f -> FindingSeverity.BLOCKER.getId().equals(f.getValueInteger("severity")) && FindingStatus.OPEN.getId().equals(f.getValueInteger("status")));
   }



   /*******************************************************************************
    ** The kernel adapter runs a marketing read tool through the kernel's own
    ** context (as the assistant would once QQQ has AiToolRegistry).
    *******************************************************************************/
   @Test
   void testKernelAdapter() throws Exception
   {
      Integer productId = product("k");
      MarketingStore.insert(new MarketingClaim().withProductId(productId).withStatus(ClaimStatus.RELEASED.getId()).withClaimText("api_key=sk-live-ABCDEF0123456789abcdef0123"));

      java.lang.reflect.Constructor<?> constructor = Class.forName("com.kingsrook.qbits.marketing.agent.KernelToolRegistration$KernelAdapter")
         .getDeclaredConstructor(com.kingsrook.qbits.marketing.agent.tools.MarketingAgentTool.class);
      constructor.setAccessible(true);
      AbstractAiTool adapter = (AbstractAiTool) constructor.newInstance(MarketingTools.readTools().stream().filter(t -> t.getName().equals(MarketingTools.LIST_PRODUCT_CLAIMS)).findFirst().orElseThrow());

      AiToolKernelSettings settings = new AiToolKernelSettings();
      AiToolContext context = new AiToolContext(qInstance, settings, new AiExposurePolicy(settings), new AiRedactor(List.of()), new AiToolResult());
      Map<String, Object> data = adapter.execute(context, new AiToolInput(Map.of("product_id", productId)));
      assertThat(String.valueOf(data)).contains("It does X.").doesNotContain("sk-live-ABCDEF0123456789abcdef0123");
      assertThat(adapter.getDefinition().getKind()).isEqualTo(com.kingsrook.qqq.backend.core.actions.ai.AiToolKind.READ);
      assertThat(KernelToolRegistration.register(qInstance, MarketingTools.readTools())).as("RC.4 has no registry").isZero();
   }



   /*******************************************************************************
    ** The email transport is honest about its configuration.
    *******************************************************************************/
   @Test
   void testEmailTransportStatus()
   {
      assertThat(new QqqMessagingEmailTransport().getStatus().state()).isEqualTo(MarketingTransport.TransportStatus.State.NOT_CONFIGURED);
      QqqMessagingEmailTransport configured = new QqqMessagingEmailTransport().withMessagingProviderName("email").withFromAddress("a@example.org");
      assertThat(configured.getStatus().state()).as("no such provider in the instance").isEqualTo(MarketingTransport.TransportStatus.State.NOT_CONFIGURED);
      assertThat(configured.send(new MarketingTransport.TransportRequest("k", 1, 1, "c", null, "t", "b", null, null, List.of())).outcome())
         .isEqualTo(MarketingTransport.TransportResult.Outcome.RETRYABLE_FAILURE);
      assertThat(configured.lookup("k").state()).isEqualTo(MarketingTransport.TransportLookup.State.UNSUPPORTED);
   }



   /*******************************************************************************
    ** Record actions report standard Review / Result summary lines, including
    ** skipped records and their reason.
    *******************************************************************************/
   @Test
   @SuppressWarnings("unchecked")
   void testRecordActionSummaries() throws QException
   {
      Integer campaignId = campaign(product("s"));
      Integer channelId  = testChannel();
      Integer ready      = content(campaignId, channelId, "Ready.");
      Integer noSchedule = MarketingStore.insert(new MarketingContent().withCampaignId(campaignId).withChannelId(channelId).withTitle("No schedule").withBody("x"))
         .getValueInteger("id");
      new ApprovalService().submitForReview(ready);

      RunProcessOutput output = new RunProcessAction().execute(new RunProcessInput().withProcessName(MarketingActions.SUBMIT_FOR_REVIEW)
         .withCallback(QProcessCallbackFactory.forPrimaryKeys("id", List.of(ready, noSchedule)))
         .withFrontendStepBehavior(RunProcessInput.FrontendStepBehavior.SKIP));
      List<ProcessSummaryLine> lines = (List<ProcessSummaryLine>) (List<?>) output.getValues().get("processResults");
      assertThat(lines).anySatisfy(l -> assertThat(l.getMessage()).contains("Set the schedule"));
      assertThat(lines).anySatisfy(l -> assertThat(l.getMessage()).isEqualTo("skipped: it is In review"));

      new ApprovalService().approve(ready, null);
      new ApprovalService().returnToDraft(ready, "not yet");
      assertThat(MarketingStore.queryBy(MarketingExecution.TABLE_NAME, "contentId", ready)).isEmpty();
      assertThat(reload(MarketingContent.TABLE_NAME, ready).getValueInteger("status")).isEqualTo(com.kingsrook.qbits.marketing.model.enums.ContentStatus.DRAFT.getId());
      assertThat(Instant.now()).isNotNull();
   }

}
