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

package com.kingsrook.qbits.marketing.processes;


import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import com.kingsrook.qbits.marketing.MarketingStore;
import com.kingsrook.qbits.marketing.agent.MarketingAgentService;
import com.kingsrook.qbits.marketing.approval.ApprovalService;
import com.kingsrook.qbits.marketing.execution.ExecutionService;
import com.kingsrook.qbits.marketing.measurement.CrmResultsService;
import com.kingsrook.qbits.marketing.model.MarketingAgentRun;
import com.kingsrook.qbits.marketing.model.MarketingCampaign;
import com.kingsrook.qbits.marketing.model.MarketingContent;
import com.kingsrook.qbits.marketing.model.MarketingExecution;
import com.kingsrook.qbits.marketing.model.MarketingFinding;
import com.kingsrook.qbits.marketing.model.MarketingProduct;
import com.kingsrook.qbits.marketing.model.enums.AgentRunStatus;
import com.kingsrook.qbits.marketing.model.enums.AgentTask;
import com.kingsrook.qbits.marketing.model.enums.ContentStatus;
import com.kingsrook.qbits.marketing.model.enums.ExecutionStatus;
import com.kingsrook.qbits.marketing.model.enums.FindingStatus;
import com.kingsrook.qbits.marketing.readiness.ReadinessChecker;
import com.kingsrook.qbits.marketing.security.MarketingPermissions;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;


/*******************************************************************************
 ** Every Marketing record action. Labels are plain verbs; each runs from its
 ** table's / record's actions (owner rule: processes are never nav items).
 *******************************************************************************/
public final class MarketingActions
{
   public static final String CHECK_PRODUCT_READINESS  = "marketing_check_product_readiness";
   public static final String CHECK_CAMPAIGN_READINESS = "marketing_check_campaign_readiness";
   public static final String AGENT_PLAN               = "marketing_agent_plan";
   public static final String AGENT_DRAFT              = "marketing_agent_draft";
   public static final String AGENT_REVIEW             = "marketing_agent_review";
   public static final String AGENT_ANALYZE            = "marketing_agent_analyze";
   public static final String UPDATE_RESULTS_FROM_CRM  = "marketing_update_results_from_crm";
   public static final String SUBMIT_FOR_REVIEW        = "marketing_submit_for_review";
   public static final String APPROVE                  = "marketing_approve";
   public static final String RETURN_TO_DRAFT          = "marketing_return_to_draft";
   public static final String SCHEDULE                 = "marketing_schedule";
   public static final String RUN_NOW                  = "marketing_run_now";
   public static final String PAUSE                    = "marketing_pause";
   public static final String RESUME                   = "marketing_resume";
   public static final String CANCEL                   = "marketing_cancel";
   public static final String RECONCILE                = "marketing_reconcile";
   public static final String MARK_ACCEPTED            = "marketing_mark_accepted";
   public static final String MARK_NOT_SENT            = "marketing_mark_not_sent";
   public static final String CANCEL_AGENT_RUN         = "marketing_cancel_agent_run";
   public static final String RESOLVE_FINDING          = "marketing_resolve_finding";
   public static final String DISMISS_FINDING          = "marketing_dismiss_finding";



   /*******************************************************************************
    ** Utility class - no instances.
    *******************************************************************************/
   private MarketingActions()
   {
   }



   /*******************************************************************************
    ** A body that may throw.
    *******************************************************************************/
   @FunctionalInterface
   interface RecordFunction
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      String apply(QRecord record) throws QException;
   }



   /*******************************************************************************
    ** An action from two functions.
    *******************************************************************************/
   static class SimpleAction extends MarketingAction
   {
      private final RecordFunction check;
      private final RecordFunction perform;



      /*******************************************************************************
       **
       *******************************************************************************/
      SimpleAction(String processName, String label, String tableName, String icon, String permissionBase, String okLabel, RecordFunction check, RecordFunction perform)
      {
         super(processName, label, tableName, icon, permissionBase, okLabel);
         this.check = check;
         this.perform = perform;
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public String check(QRecord record) throws QException
      {
         return (check.apply(record));
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public String perform(QRecord record) throws QException
      {
         return (perform.apply(record));
      }
   }



   /*******************************************************************************
    ** The processes.
    *******************************************************************************/
   public static List<QProcessMetaData> buildProcesses()
   {
      ApprovalService  approvals  = new ApprovalService();
      ExecutionService executions = new ExecutionService();

      List<MarketingAction> actions = new ArrayList<>();

      //////////////
      // product  //
      //////////////
      actions.add(new SimpleAction(CHECK_PRODUCT_READINESS, "Check Readiness", MarketingProduct.TABLE_NAME, "fact_check", MarketingPermissions.PREPARE_BASE,
         "product(s) will be checked (evidence, claims, sources, audiences)", r -> null,
         r -> new ReadinessChecker().checkProduct(r.getValueInteger("id")) + " open finding(s) recorded"));

      ///////////////
      // campaign  //
      ///////////////
      actions.add(new SimpleAction(CHECK_CAMPAIGN_READINESS, "Check Readiness", MarketingCampaign.TABLE_NAME, "fact_check", MarketingPermissions.PREPARE_BASE,
         "campaign(s) will be checked (goal, audience, call to action, measurement, channel rules)", r -> null,
         r -> new ReadinessChecker().checkCampaign(r.getValueInteger("id")) + " open finding(s) recorded"));
      actions.add(agentAction(AGENT_PLAN, "Plan with Agent", AgentTask.PLAN_CAMPAIGN, "campaign(s) will be planned by the agent (a proposal for review)"));
      actions.add(agentAction(AGENT_DRAFT, "Draft with Agent", AgentTask.DRAFT_CONTENT, "campaign(s) will get agent drafts (saved as drafts for review)"));
      actions.add(agentAction(AGENT_REVIEW, "Check Claims with Agent", AgentTask.REVIEW_CLAIMS, "campaign(s) will be checked by the agent (findings only)"));
      actions.add(agentAction(AGENT_ANALYZE, "Analyze Results with Agent", AgentTask.ANALYZE_RESULTS, "campaign(s) will be analyzed by the agent (findings only)"));
      actions.add(new SimpleAction(UPDATE_RESULTS_FROM_CRM, "Update Results from CRM", MarketingCampaign.TABLE_NAME, "sync", MarketingPermissions.PREPARE_BASE,
         "campaign(s) will import CRM form submissions and apply their lead rule",
         r -> r.getValueString("utmCampaign") == null || r.getValueString("utmCampaign").isBlank() ? "no UTM campaign set" : null,
         r -> new CrmResultsService().updateFromCrm(r.getValueInteger("id"))));

      //////////////
      // content  //
      //////////////
      actions.add(new SimpleAction(SUBMIT_FOR_REVIEW, "Submit for Review", MarketingContent.TABLE_NAME, "rate_review", MarketingPermissions.PREPARE_BASE,
         "item(s) will be submitted for review", r -> statusIs(r, ContentStatus.DRAFT), r -> approvals.submitForReview(r.getValueInteger("id"))));
      actions.add(new SimpleAction(APPROVE, "Approve", MarketingContent.TABLE_NAME, "verified", MarketingPermissions.APPROVE_BASE,
         "item(s) will be approved exactly as shown (text, destination, recipients, schedule)", r -> statusIs(r, ContentStatus.IN_REVIEW),
         r -> approvals.approve(r.getValueInteger("id"), null)));
      actions.add(new SimpleAction(RETURN_TO_DRAFT, "Return to Draft", MarketingContent.TABLE_NAME, "undo", MarketingPermissions.APPROVE_BASE,
         "item(s) will return to draft (approvals revoked, pending executions cancelled)",
         r -> statusIn(r, Set.of(ContentStatus.IN_REVIEW, ContentStatus.APPROVED, ContentStatus.SCHEDULED, ContentStatus.PAUSED, ContentStatus.FAILED)),
         r -> approvals.returnToDraft(r.getValueInteger("id"), null)));
      actions.add(new SimpleAction(SCHEDULE, "Schedule", MarketingContent.TABLE_NAME, "event", MarketingPermissions.EXECUTE_BASE,
         "item(s) will be scheduled for their approved time", r -> statusIs(r, ContentStatus.APPROVED), r -> approvals.schedule(r.getValueInteger("id"))));

      /////////////////
      // execution   //
      /////////////////
      actions.add(new SimpleAction(RUN_NOW, "Run Now", MarketingExecution.TABLE_NAME, "play_arrow", MarketingPermissions.EXECUTE_BASE,
         "execution(s) will run now (if due; all checks repeat first)", r -> executionStatusIs(r, ExecutionStatus.SCHEDULED),
         r -> executions.runOne(r.getValueInteger("id"), Instant.now())));
      actions.add(new SimpleAction(PAUSE, "Pause", MarketingExecution.TABLE_NAME, "pause", MarketingPermissions.EXECUTE_BASE,
         "execution(s) will be paused", r -> executionStatusIs(r, ExecutionStatus.SCHEDULED), r -> executions.pause(r.getValueInteger("id"))));
      actions.add(new SimpleAction(RESUME, "Resume", MarketingExecution.TABLE_NAME, "play_circle", MarketingPermissions.EXECUTE_BASE,
         "execution(s) will be resumed", r -> executionStatusIs(r, ExecutionStatus.PAUSED), r -> executions.resume(r.getValueInteger("id"))));
      actions.add(new SimpleAction(CANCEL, "Cancel", MarketingExecution.TABLE_NAME, "cancel", MarketingPermissions.EXECUTE_BASE,
         "execution(s) will be cancelled (their approvals revoked)",
         r -> executionStatusIn(r, Set.of(ExecutionStatus.SCHEDULED, ExecutionStatus.PAUSED, ExecutionStatus.BLOCKED, ExecutionStatus.NEEDS_RECONCILIATION, ExecutionStatus.FAILED)),
         r -> executions.cancel(r.getValueInteger("id"))));
      actions.add(new SimpleAction(RECONCILE, "Reconcile with Provider", MarketingExecution.TABLE_NAME, "find_replace", MarketingPermissions.EXECUTE_BASE,
         "ambiguous execution(s) will be looked up with their provider", r -> executionStatusIs(r, ExecutionStatus.NEEDS_RECONCILIATION),
         r -> executions.reconcile(r.getValueInteger("id"))));
      actions.add(new SimpleAction(MARK_ACCEPTED, "Mark as Accepted", MarketingExecution.TABLE_NAME, "task_alt", MarketingPermissions.EXECUTE_BASE,
         "ambiguous execution(s) will be recorded as accepted by the provider (your decision)", r -> executionStatusIs(r, ExecutionStatus.NEEDS_RECONCILIATION),
         r -> executions.resolveAsAccepted(r.getValueInteger("id"))));
      actions.add(new SimpleAction(MARK_NOT_SENT, "Mark Not Sent and Retry", MarketingExecution.TABLE_NAME, "replay", MarketingPermissions.EXECUTE_BASE,
         "ambiguous execution(s) will be scheduled again with the same idempotency key (your decision)", r -> executionStatusIs(r, ExecutionStatus.NEEDS_RECONCILIATION),
         r -> executions.resolveAsNotSent(r.getValueInteger("id"))));

      ///////////////////////////////
      // agent runs and findings   //
      ///////////////////////////////
      actions.add(new SimpleAction(CANCEL_AGENT_RUN, "Cancel Run", MarketingAgentRun.TABLE_NAME, "stop_circle", MarketingPermissions.AGENT_BASE,
         "agent run(s) will be cancelled",
         r -> Set.of(AgentRunStatus.QUEUED.getId(), AgentRunStatus.RUNNING.getId()).contains(r.getValueInteger("status")) ? null : "it is not running",
         r ->
         {
            MarketingStore.updateInternal(MarketingAgentRun.TABLE_NAME, new QRecord().withValue("id", r.getValue("id")).withValue("status", AgentRunStatus.CANCELLED.getId())
               .withValue("errorMessage", "Cancelled by " + MarketingStore.currentUserId() + "."));
            return ("cancelled (a running run stops before its next model call)");
         }));
      actions.add(findingAction(RESOLVE_FINDING, "Mark Resolved", FindingStatus.RESOLVED, "task_alt"));
      actions.add(findingAction(DISMISS_FINDING, "Dismiss", FindingStatus.DISMISSED, "do_not_disturb_on"));

      List<QProcessMetaData> processes = new ArrayList<>();
      for(MarketingAction action : actions)
      {
         processes.add(action.buildProcess());
      }
      return (processes);
   }



   /*******************************************************************************
    ** An agent action on campaigns (at most 3 per run).
    *******************************************************************************/
   private static MarketingAction agentAction(String name, String label, AgentTask task, String okLabel)
   {
      return (new SimpleAction(name, label, MarketingCampaign.TABLE_NAME, "smart_toy", MarketingPermissions.AGENT_BASE, okLabel, r -> null, r ->
      {
         MarketingAgentRun run = new MarketingAgentService().run(task, r.getValueInteger("id"), null, null);
         AgentRunStatus    status = AgentRunStatus.getById(run.getStatus());
         if(status != AgentRunStatus.SUCCEEDED)
         {
            throw (new com.kingsrook.qqq.backend.core.exceptions.QUserFacingException("agent run #" + run.getId() + " " + status.getLabel().toLowerCase() + ": " + run.getErrorMessage()));
         }
         return ("agent run #" + run.getId() + " succeeded: " + run.getDraftsCreated() + " draft(s), " + run.getFindingsCreated() + " finding(s)");
      }).withMaxRecords(3));
   }



   /*******************************************************************************
    ** A finding status action (open findings only).
    *******************************************************************************/
   private static MarketingAction findingAction(String name, String label, FindingStatus status, String icon)
   {
      return (new SimpleAction(name, label, MarketingFinding.TABLE_NAME, icon, MarketingPermissions.PREPARE_BASE, "finding(s) will be marked " + status.getLabel().toLowerCase(),
         r -> FindingStatus.OPEN.getId().equals(r.getValueInteger("status")) ? null : "it is not open",
         r ->
         {
            MarketingStore.update(MarketingFinding.TABLE_NAME, new QRecord().withValue("id", r.getValue("id")).withValue("status", status.getId()));
            return ("marked " + status.getLabel().toLowerCase());
         }));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static String statusIs(QRecord record, ContentStatus status)
   {
      return (statusIn(record, Set.of(status)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static String statusIn(QRecord record, Set<ContentStatus> statuses)
   {
      ContentStatus current = ContentStatus.getById(record.getValueInteger("status"));
      return (statuses.contains(current) ? null : "it is " + (current == null ? "in no state" : current.getLabel()));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static String executionStatusIs(QRecord record, ExecutionStatus status)
   {
      return (executionStatusIn(record, Set.of(status)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static String executionStatusIn(QRecord record, Set<ExecutionStatus> statuses)
   {
      ExecutionStatus current = ExecutionStatus.getById(record.getValueInteger("status"));
      return (statuses.contains(current) ? null : "it is " + (current == null ? "in no state" : current.getLabel()));
   }

}
