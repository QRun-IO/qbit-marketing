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

package com.kingsrook.qbits.marketing.widgets;


import java.io.Serializable;
import java.time.Instant;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import com.kingsrook.qbits.marketing.MarketingRuntime;
import com.kingsrook.qbits.marketing.MarketingStore;
import com.kingsrook.qbits.marketing.measurement.MeasurementReport;
import com.kingsrook.qbits.marketing.model.MarketingAgentRun;
import com.kingsrook.qbits.marketing.model.MarketingApproval;
import com.kingsrook.qbits.marketing.model.MarketingCampaign;
import com.kingsrook.qbits.marketing.model.MarketingChannel;
import com.kingsrook.qbits.marketing.model.MarketingContent;
import com.kingsrook.qbits.marketing.model.MarketingExecution;
import com.kingsrook.qbits.marketing.model.MarketingFinding;
import com.kingsrook.qbits.marketing.model.MarketingProduct;
import com.kingsrook.qbits.marketing.model.enums.AgentRunStatus;
import com.kingsrook.qbits.marketing.model.enums.AgentTask;
import com.kingsrook.qbits.marketing.model.enums.AiContentPolicy;
import com.kingsrook.qbits.marketing.model.enums.ApprovalStatus;
import com.kingsrook.qbits.marketing.model.enums.AuthorType;
import com.kingsrook.qbits.marketing.model.enums.CampaignGoal;
import com.kingsrook.qbits.marketing.model.enums.CampaignStatus;
import com.kingsrook.qbits.marketing.model.enums.ContentStatus;
import com.kingsrook.qbits.marketing.model.enums.ExecutionStatus;
import com.kingsrook.qbits.marketing.model.enums.FindingSeverity;
import com.kingsrook.qbits.marketing.model.enums.FindingStatus;
import com.kingsrook.qbits.marketing.security.MarketingPermissionChecker;
import com.kingsrook.qbits.marketing.security.MarketingPermissions;
import com.kingsrook.qbits.marketing.transport.MarketingTransport;
import com.kingsrook.qqq.backend.core.actions.ai.output.AiLinkBuilder;
import com.kingsrook.qqq.backend.core.actions.dashboard.widgets.AbstractWidgetRenderer;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterOrderBy;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetInput;
import com.kingsrook.qqq.backend.core.model.actions.widgets.RenderWidgetOutput;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.RawHTML;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.TableData;
import com.kingsrook.qqq.backend.core.model.dashboard.widgets.WidgetType;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaData;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;


/*******************************************************************************
 ** The Marketing widgets: the app home dashboard (the one Marketing
 ** overview - other apps only link to it) and two record widgets.
 **
 ** - Home: Needs Attention, Campaigns (business content only).
 ** - Admin-placeable (not on the home): Agent Activity (model/tool calls and
 **   tokens) and Configuration Status (provider configuration) are system
 **   content; a host places ADMIN_WIDGETS on its Admin area. They need
 **   marketing.admin.read.
 ** - Campaign record: Results (per stage; "Not measured" is never shown as 0).
 ** - Content record: Proposed Publication (the exact text, destination,
 **   recipients and schedule a reviewer approves).
 **
 ** All are full-width tables of at most three columns (or one HTML block),
 ** so they read at every width without inner scrolling. All data is real;
 ** with none, they say so. Home widgets require marketing.read (omitted, never
 ** shown locked, without it) and read with the viewer's record locks.
 *******************************************************************************/
public final class MarketingWidgets
{
   public static final String NEEDS_ATTENTION  = "marketing_needs_attention";
   public static final String CAMPAIGNS        = "marketing_campaigns";
   public static final String AGENT_ACTIVITY   = "marketing_agent_activity";
   public static final String CONFIGURATION    = "marketing_configuration_status";
   public static final String CAMPAIGN_RESULTS = "marketing_campaign_results";
   public static final String CONTENT_PROPOSAL = "marketing_content_proposal";

   private static final DateTimeFormatter WHEN_FORMAT = DateTimeFormatter.ofPattern("MMM d, yyyy h:mm a z", Locale.US);

   public static final List<String> HOME_WIDGETS = List.of(NEEDS_ATTENTION, CAMPAIGNS);

   /////////////////////////////////////////////////////////////////////////
   // system content: for a host to place on its Admin area, not the home //
   /////////////////////////////////////////////////////////////////////////
   public static final List<String> ADMIN_WIDGETS = List.of(AGENT_ACTIVITY, CONFIGURATION);



   /*******************************************************************************
    ** Utility class - no instances.
    *******************************************************************************/
   private MarketingWidgets()
   {
   }



   /*******************************************************************************
    ** The widget meta-data.
    *******************************************************************************/
   public static List<QWidgetMetaData> buildWidgets()
   {
      return (List.of(
         widget(NEEDS_ATTENTION, "Needs Attention", WidgetType.TABLE, NeedsAttention.class, MarketingPermissions.READ),
         widget(CAMPAIGNS, "Campaigns", WidgetType.TABLE, Campaigns.class, MarketingPermissions.READ),
         widget(AGENT_ACTIVITY, "Agent Activity", WidgetType.TABLE, AgentActivity.class, MarketingPermissions.ADMIN_READ),
         widget(CONFIGURATION, "Configuration Status", WidgetType.TABLE, Configuration.class, MarketingPermissions.ADMIN_READ),
         widget(CAMPAIGN_RESULTS, "Results", WidgetType.TABLE, CampaignResults.class, MarketingPermissions.READ),
         widget(CONTENT_PROPOSAL, "Proposed Publication", WidgetType.HTML, ContentProposal.class, MarketingPermissions.READ)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static QWidgetMetaData widget(String name, String label, WidgetType type, Class<? extends AbstractWidgetRenderer> renderer, String permission)
   {
      return (new QWidgetMetaData()
         .withName(name)
         .withLabel(label)
         .withType(type.getType())
         .withGridColumns(12)
         .withIsCard(true)
         .withPermissionRules(MarketingPermissionChecker.rules(permission))
         .withCodeReference(new QCodeReference(renderer)));
   }



   /*******************************************************************************
    ** Base: the read check, columns and helpers.
    *******************************************************************************/
   abstract static class Base extends AbstractWidgetRenderer
   {
      /*******************************************************************************
       ** The permission this widget needs (rendering is refused without it, behind
       ** the widget's own rules: no "you need ..." text is ever returned).
       *******************************************************************************/
      String requiredPermission()
      {
         return (MarketingPermissions.READ);
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public RenderWidgetOutput render(RenderWidgetInput input) throws QException
      {
         if(!MarketingPermissions.has(requiredPermission()))
         {
            throw (new QPermissionDeniedException("Permission denied."));
         }
         return (renderFor(input));
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      abstract RenderWidgetOutput renderFor(RenderWidgetInput input) throws QException;
   }



   /*******************************************************************************
    ** A top-aligned column without a fixed width (wraps instead of scrolling).
    *******************************************************************************/
   static TableData.Column column(String header, String accessor, boolean html)
   {
      return (new TableData.Column(html ? "html" : "text", header, accessor, null, "left").withVerticalAlign("top"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   static TableData table(List<TableData.Column> columns, List<Map<String, Object>> rows, String empty)
   {
      return (new TableData(null, columns, rows).withRowsPerPage(10).withHidePaginationDropdown(true).withNoRowsFoundHTML("<p>" + escape(empty) + "</p>"));
   }



   /*******************************************************************************
    ** A link to a record (Next route grammar), HTML-escaped label.
    *******************************************************************************/
   static String link(String tableName, Serializable id, String label)
   {
      return ("<a href=\"" + AiLinkBuilder.recordHref(tableName, id) + "\">" + escape(label) + "</a>");
   }



   /*******************************************************************************
    ** A date-time for people, in the user's time zone (the session's, else the
    ** instance's), as the record screens show it: "Oct 14, 2026 11:53 AM CDT"
    ** (empty when null).
    *******************************************************************************/
   static String when(Object value)
   {
      Instant instant = ValueUtils.getValueAsInstant(value);
      return (instant == null ? "" : WHEN_FORMAT.withZone(ValueUtils.getSessionOrInstanceZoneId()).format(instant));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   static String escape(String text)
   {
      if(text == null)
      {
         return ("");
      }
      return (text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;").replace("\"", "&quot;").replace("'", "&#39;"));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   static Map<String, Object> row(Object... pairs)
   {
      Map<String, Object> row = new LinkedHashMap<>();
      for(int i = 0; i < pairs.length; i += 2)
      {
         row.put((String) pairs[i], pairs[i + 1]);
      }
      return (row);
   }



   /*******************************************************************************
    ** Needs Attention: content waiting for review, executions blocked, failed
    ** or ambiguous, open blocker findings, recently failed agent runs.
    *******************************************************************************/
   public static class NeedsAttention extends Base
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      RenderWidgetOutput renderFor(RenderWidgetInput input) throws QException
      {
         List<Map<String, Object>> rows = new ArrayList<>();

         for(QRecord r : MarketingStore.query(MarketingContent.TABLE_NAME, new QQueryFilter(new QFilterCriteria("status", QCriteriaOperator.EQUALS, ContentStatus.IN_REVIEW.getId())).withLimit(25)))
         {
            rows.add(row("item", link(MarketingContent.TABLE_NAME, r.getValue("id"), r.getValueString("title")), "what", "Waiting for review (revision " + r.getValueInteger("revisionNumber") + ")",
               "since", when(r.getValue("modifyDate"))));
         }
         for(QRecord r : MarketingStore.query(MarketingExecution.TABLE_NAME, new QQueryFilter(new QFilterCriteria("status", QCriteriaOperator.IN,
            List.of(ExecutionStatus.BLOCKED.getId(), ExecutionStatus.NEEDS_RECONCILIATION.getId(), ExecutionStatus.FAILED.getId()))).withLimit(25)))
         {
            rows.add(row("item", link(MarketingExecution.TABLE_NAME, r.getValue("id"), "Execution #" + r.getValue("id")),
               "what", ExecutionStatus.getById(r.getValueInteger("status")).getLabel() + ": " + StringUtils.safeTruncate(r.getValueString("statusDetail"), 160, "..."),
               "since", when(r.getValue("modifyDate"))));
         }
         for(QRecord r : MarketingStore.query(MarketingFinding.TABLE_NAME, new QQueryFilter(
            new QFilterCriteria("status", QCriteriaOperator.EQUALS, FindingStatus.OPEN.getId()),
            new QFilterCriteria("severity", QCriteriaOperator.EQUALS, FindingSeverity.BLOCKER.getId())).withLimit(25)))
         {
            rows.add(row("item", link(MarketingFinding.TABLE_NAME, r.getValue("id"), r.getValueString("title")), "what", "Open blocker finding", "since", when(r.getValue("createDate"))));
         }
         for(QRecord r : MarketingStore.query(MarketingAgentRun.TABLE_NAME, new QQueryFilter(
            new QFilterCriteria("status", QCriteriaOperator.EQUALS, AgentRunStatus.FAILED.getId()),
            new QFilterCriteria("createDate", QCriteriaOperator.GREATER_THAN, Instant.now().minus(7, ChronoUnit.DAYS))).withLimit(10)))
         {
            rows.add(row("item", link(MarketingAgentRun.TABLE_NAME, r.getValue("id"), "Agent run #" + r.getValue("id")),
               "what", "Failed: " + StringUtils.safeTruncate(r.getValueString("errorMessage"), 160, "..."), "since", when(r.getValue("createDate"))));
         }

         ///////////////////////////////////////////////////////////////////////////////////
         // a channel that cannot send: said in business words (the provider detail is in //
         // the admin-only Configuration Status widget)                                    //
         ///////////////////////////////////////////////////////////////////////////////////
         for(QRecord r : MarketingStore.query(MarketingChannel.TABLE_NAME, new QQueryFilter(new QFilterCriteria("isActive", QCriteriaOperator.EQUALS, true)).withOrderBy(new QFilterOrderBy("id")).withLimit(50)))
         {
            if(StringUtils.hasContent(r.getValueString("transportName")) && !canSend(r.getValueString("transportName")))
            {
               rows.add(row("item", link(MarketingChannel.TABLE_NAME, r.getValue("id"), r.getValueString("name")), "what", "Sends on " + r.getValueString("name") + " are paused - ask an admin",
                  "since", ""));
            }
         }

         return (new RenderWidgetOutput(table(List.of(column("Item", "item", true), column("What", "what", false), column("Since", "since", false)), rows,
            "Nothing needs attention.")));
      }



      /*******************************************************************************
       ** Can the named transport send (registered, and operational or test-only)?
       *******************************************************************************/
      private static boolean canSend(String transportName) throws QException
      {
         MarketingTransport transport = MarketingRuntime.current().getTransport(transportName);
         return (transport != null && transport.getStatus().isUsable());
      }
   }



   /*******************************************************************************
    ** Campaigns: every campaign not completed or cancelled, with its content
    ** pipeline.
    *******************************************************************************/
   public static class Campaigns extends Base
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      RenderWidgetOutput renderFor(RenderWidgetInput input) throws QException
      {
         Map<Serializable, String> products = new LinkedHashMap<>();
         for(QRecord p : MarketingStore.query(MarketingProduct.TABLE_NAME, new QQueryFilter()))
         {
            products.put(p.getValue("id"), p.getValueString("name"));
         }

         List<Map<String, Object>> rows = new ArrayList<>();
         for(QRecord c : MarketingStore.query(MarketingCampaign.TABLE_NAME, new QQueryFilter(new QFilterCriteria("status", QCriteriaOperator.NOT_IN,
            List.of(CampaignStatus.COMPLETED.getId(), CampaignStatus.CANCELLED.getId()))).withOrderBy(new QFilterOrderBy("id")).withLimit(50)))
         {
            Map<ContentStatus, Integer> pipeline = new LinkedHashMap<>();
            for(QRecord content : MarketingStore.queryBy(MarketingContent.TABLE_NAME, "campaignId", c.getValueInteger("id")))
            {
               pipeline.merge(ContentStatus.getById(content.getValueInteger("status")), 1, Integer::sum);
            }
            StringBuilder content = new StringBuilder();
            pipeline.forEach((status, count) -> content.append(content.length() == 0 ? "" : ", ").append(count).append(" ").append(status == null ? "?" : status.getLabel().toLowerCase()));

            CampaignGoal   goal   = CampaignGoal.getById(c.getValueInteger("goal"));
            CampaignStatus status = CampaignStatus.getById(c.getValueInteger("status"));
            rows.add(row("campaign", link(MarketingCampaign.TABLE_NAME, c.getValue("id"), c.getValueString("name")) + "<br>" + escape(products.get(c.getValue("productId"))),
               "goal", (goal == null ? "" : goal.getLabel()) + " - " + (status == null ? "" : status.getLabel()),
               "content", content.length() == 0 ? "No content yet" : content.toString()));
         }
         return (new RenderWidgetOutput(table(List.of(column("Campaign", "campaign", true), column("Goal and status", "goal", false), column("Content", "content", false)), rows,
            "No open campaigns.")));
      }
   }



   /*******************************************************************************
    ** Agent Activity: the latest runs.
    *******************************************************************************/
   public static class AgentActivity extends Base
   {
      /*******************************************************************************
       ** System content: marketing.admin.read.
       *******************************************************************************/
      @Override
      String requiredPermission()
      {
         return (MarketingPermissions.ADMIN_READ);
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      RenderWidgetOutput renderFor(RenderWidgetInput input) throws QException
      {
         List<Map<String, Object>> rows = new ArrayList<>();
         for(QRecord r : MarketingStore.query(MarketingAgentRun.TABLE_NAME, new QQueryFilter().withOrderBy(new QFilterOrderBy("id", false)).withLimit(10)))
         {
            AgentTask      task   = AgentTask.getById(r.getValueInteger("task"));
            AgentRunStatus status = AgentRunStatus.getById(r.getValueInteger("status"));
            String         usage  = r.getValueInteger("modelCalls") == null || r.getValueInteger("modelCalls") == 0 ? "no model calls"
               : r.getValueInteger("modelCalls") + " model / " + r.getValueInteger("toolCalls") + " tool calls, " + (nz(r.getValueLong("inputTokens")) + nz(r.getValueLong("outputTokens"))) + " tokens";
            String outcome = status == AgentRunStatus.SUCCEEDED
               ? r.getValueInteger("draftsCreated") + " draft(s), " + r.getValueInteger("findingsCreated") + " finding(s)"
               : StringUtils.safeTruncate(r.getValueString("errorMessage"), 160, "...");
            rows.add(row("run", link(MarketingAgentRun.TABLE_NAME, r.getValue("id"), "#" + r.getValue("id") + " " + (task == null ? "" : task.getLabel())),
               "status", (status == null ? "" : status.getLabel()) + " - " + usage, "outcome", outcome));
         }
         return (new RenderWidgetOutput(table(List.of(column("Run", "run", true), column("Status and usage", "status", false), column("Outcome", "outcome", false)), rows,
            "The agent has not run yet.")));
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      private static long nz(Long value)
      {
         return (value == null ? 0 : value);
      }
   }



   /*******************************************************************************
    ** Configuration Status: providers, transports and CRM, honestly.
    *******************************************************************************/
   public static class Configuration extends Base
   {
      /*******************************************************************************
       ** System content: marketing.admin.read.
       *******************************************************************************/
      @Override
      String requiredPermission()
      {
         return (MarketingPermissions.ADMIN_READ);
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      RenderWidgetOutput renderFor(RenderWidgetInput input) throws QException
      {
         List<Map<String, Object>> rows = new ArrayList<>();
         for(MarketingRuntime.IntegrationStatus status : MarketingRuntime.current().describeIntegrations())
         {
            rows.add(row("integration", "<strong>" + escape(status.kind()) + "</strong><br>" + escape(status.name()), "status", status.status(), "detail", status.detail()));
         }
         return (new RenderWidgetOutput(table(List.of(column("Integration", "integration", true), column("Status", "status", false), column("Detail", "detail", false)), rows,
            "No integrations.")));
      }
   }



   /*******************************************************************************
    ** A campaign's results (record widget).
    *******************************************************************************/
   public static class CampaignResults extends Base
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      RenderWidgetOutput renderFor(RenderWidgetInput input) throws QException
      {
         Integer campaignId = id(input);
         List<Map<String, Object>> rows = new ArrayList<>();
         if(campaignId != null)
         {
            MeasurementReport report = MeasurementReport.forCampaign(campaignId);
            for(MeasurementReport.Row r : report.getRows())
            {
               String values = r.rawEvents() == null ? "" : r.rawEvents() + " raw events, " + r.uniqueActors() + " unique actors";
               rows.add(row("metric", "<strong>" + escape(r.stage().getLabel()) + "</strong><br>" + escape(r.metric()),
                  "result", r.headline() + (r.rawEvents() == null ? "" : " (" + values + ")") + (r.target() == null ? "" : "; target " + r.target()),
                  "note", r.note() == null ? "" : r.note()));
            }
            for(String limitation : report.getLimitations())
            {
               rows.add(row("metric", "<em>Limitation</em>", "result", "", "note", limitation));
            }
         }
         return (new RenderWidgetOutput(table(List.of(column("Metric", "metric", true), column("Result", "result", false), column("Notes", "note", false)), rows,
            "No measurement plan yet.").withRowsPerPage(50)));
      }
   }



   /*******************************************************************************
    ** The exact proposed publication of a content item (record widget).
    *******************************************************************************/
   public static class ContentProposal extends Base
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      RenderWidgetOutput renderFor(RenderWidgetInput input) throws QException
      {
         Integer contentId = id(input);
         QRecord record    = contentId == null ? null : MarketingStore.get(MarketingContent.TABLE_NAME, contentId);
         if(record == null)
         {
            return (new RenderWidgetOutput(new RawHTML(null, "<p>Save the content to see its proposed publication.</p>")));
         }

         MarketingContent content = new MarketingContent(record);
         QRecord          channel = content.getChannelId() == null ? null : MarketingStore.get(MarketingChannel.TABLE_NAME, content.getChannelId());
         List<QRecord>    active  = MarketingStore.query(MarketingApproval.TABLE_NAME, new QQueryFilter(
            new QFilterCriteria("contentId", QCriteriaOperator.EQUALS, contentId),
            new QFilterCriteria("status", QCriteriaOperator.EQUALS, ApprovalStatus.ACTIVE.getId())));

         String approval;
         if(!active.isEmpty())
         {
            approval = "Approved by " + escape(active.get(0).getValueString("approvedByUserId")) + " on " + when(active.get(0).getValue("approvedAt"))
               + (content.getContentHash() != null && content.getContentHash().equals(active.get(0).getValueString("contentHash")) ? " (matches this revision)." : " (does NOT match this revision).");
         }
         else
         {
            //////////////////////////////////////////////////////////////////////
            // an approval an execution used up is not "not approved": say so //
            //////////////////////////////////////////////////////////////////////
            List<QRecord> consumed = MarketingStore.query(MarketingApproval.TABLE_NAME, new QQueryFilter(
               new QFilterCriteria("contentId", QCriteriaOperator.EQUALS, contentId),
               new QFilterCriteria("status", QCriteriaOperator.EQUALS, ApprovalStatus.CONSUMED.getId()),
               new QFilterCriteria("contentHash", QCriteriaOperator.EQUALS, content.getContentHash())));
            approval = consumed.isEmpty() ? "Not approved."
               : "Approved by " + escape(consumed.get(0).getValueString("approvedByUserId")) + " on " + when(consumed.get(0).getValue("approvedAt"))
                  + ", and used by an execution. Approve again to send this revision again.";
         }

         StringBuilder html = new StringBuilder("<div style=\"max-width:100%;overflow-wrap:anywhere\">");
         html.append("<p><strong>").append(escape(content.getTitle())).append("</strong></p>");
         html.append("<pre style=\"white-space:pre-wrap;overflow-wrap:anywhere;font-family:inherit;margin:0 0 1em 0\">").append(escape(content.getBody())).append("</pre>");
         html.append("<ul style=\"padding-left:1.2em;margin:0\">");
         item(html, "Channel", channel == null ? "(none)" : channel.getValueString("name") + " - AI content: " + label(AiContentPolicy.getById(channel.getValueInteger("aiContentPolicy"))));
         item(html, "Call to action", content.getCallToAction());
         item(html, "Destination", content.getDestinationUrl());
         item(html, "Recipients", StringUtils.hasContent(content.getRecipientScope()) ? content.getRecipientScope() : "public (no recipient list)");
         item(html, "Scheduled for", content.getScheduledFor() == null ? "(not set)" : when(content.getScheduledFor()));
         item(html, "Revision", content.getRevisionNumber() + " by " + label(AuthorType.getById(content.getAuthorType())));
         item(html, "Content hash", content.getContentHash() == null ? "" : content.getContentHash().substring(0, 12));
         item(html, "Status", label(ContentStatus.getById(content.getStatus())));
         html.append("<li><strong>Approval:</strong> ").append(approval).append("</li>");
         html.append("</ul>");
         html.append("<p style=\"margin-top:1em\"><em>Approval covers exactly this text, destination, recipients and schedule. Any edit voids it.</em></p></div>");
         return (new RenderWidgetOutput(new RawHTML(null, html.toString())));
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      private static void item(StringBuilder html, String label, String value)
      {
         html.append("<li><strong>").append(escape(label)).append(":</strong> ").append(escape(value == null ? "" : value)).append("</li>");
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      private static String label(Object value)
      {
         if(value instanceof ContentStatus s)
         {
            return (s.getLabel());
         }
         if(value instanceof AuthorType a)
         {
            return (a.getLabel());
         }
         if(value instanceof AiContentPolicy p)
         {
            return (p.getLabel());
         }
         return ("");
      }
   }



   /*******************************************************************************
    ** The record id of a record widget.
    *******************************************************************************/
   static Integer id(RenderWidgetInput input)
   {
      String id = input.getQueryParams() == null ? null : input.getQueryParams().get("id");
      try
      {
         return (StringUtils.hasContent(id) ? Integer.valueOf(id) : null);
      }
      catch(NumberFormatException e)
      {
         return (null);
      }
   }

}
