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

package com.kingsrook.qbits.marketing.agent;


import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qbits.assistant.engine.ModelPricing;
import com.kingsrook.qbits.assistant.provider.AssistantModelProvider;
import com.kingsrook.qbits.assistant.provider.CacheHint;
import com.kingsrook.qbits.assistant.provider.ModelEventListener;
import com.kingsrook.qbits.assistant.provider.ModelMessage;
import com.kingsrook.qbits.assistant.provider.ModelPart;
import com.kingsrook.qbits.assistant.provider.ModelProviderException;
import com.kingsrook.qbits.assistant.provider.ModelRequest;
import com.kingsrook.qbits.assistant.provider.ModelResponse;
import com.kingsrook.qbits.assistant.provider.ModelToolDefinition;
import com.kingsrook.qbits.assistant.provider.ModelUsage;
import com.kingsrook.qbits.assistant.provider.SystemBlock;
import com.kingsrook.qbits.marketing.MarketingQBitConfig;
import com.kingsrook.qbits.marketing.MarketingRuntime;
import com.kingsrook.qbits.marketing.MarketingStore;
import com.kingsrook.qbits.marketing.agent.tools.MarketingAgentTool;
import com.kingsrook.qbits.marketing.agent.tools.MarketingToolContext;
import com.kingsrook.qbits.marketing.agent.tools.MarketingTools;
import com.kingsrook.qbits.marketing.model.MarketingAgentRun;
import com.kingsrook.qbits.marketing.model.MarketingAgentStep;
import com.kingsrook.qbits.marketing.model.MarketingCampaign;
import com.kingsrook.qbits.marketing.model.MarketingPolicy;
import com.kingsrook.qbits.marketing.model.enums.AgentRunStatus;
import com.kingsrook.qbits.marketing.model.enums.AgentStepKind;
import com.kingsrook.qbits.marketing.model.enums.AgentTask;
import com.kingsrook.qbits.marketing.readiness.ReadinessChecker;
import com.kingsrook.qbits.marketing.security.MarketingPermissions;
import com.kingsrook.qqq.backend.core.actions.ai.AiToolKind;
import com.kingsrook.qqq.backend.core.actions.ai.AiToolOutcome;
import com.kingsrook.qqq.backend.core.actions.ai.output.AiRedactor;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.model.session.QSystemUserSession;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** The marketing agent: a bounded tool-use loop over a model provider
 ** (qbit-assistant's AssistantModelProvider SPI), recorded in
 ** marketing_agent_run and marketing_agent_step.
 **
 ** A run:
 ** 1. checks the policy (kill switch), the provider and model, the daily
 **    token and monthly cost budgets, and the concurrency limit - failing the
 **    run with a plain reason when any is missing (missing-provider behavior);
 ** 2. runs the deterministic readiness checks (a CHECK step);
 ** 3. loops model call -> tool calls, bounded by model calls, tool calls,
 **    wall-clock time and the per-minute model-call rate; it re-reads its own
 **    status before every model call so Cancel Run stops it;
 ** 4. records every model call (tokens, stop reason) and tool call (input,
 **    outcome, enveloped result), the usage and cost, the drafts and findings
 **    it created, and its final summary.
 **
 ** The run executes as the requesting user: the tools read with that user's
 ** permissions and record locks, and can only create drafts and findings.
 ** Retryable provider errors (rate limit, overload, network) are retried
 ** twice with backoff - model calls have no side effects.
 *******************************************************************************/
public class MarketingAgentService
{
   private static final QLogger LOG = QLogger.getLogger(MarketingAgentService.class);

   static final int MAX_STEP_JSON = 20_000;

   private final AiRedactor redactor = new AiRedactor(List.of());



   /*******************************************************************************
    ** Create and run an agent run. Returns the finished run (SUCCEEDED or
    ** FAILED, with the reason); throws only for invalid requests.
    *******************************************************************************/
   public MarketingAgentRun run(AgentTask task, Integer campaignId, Integer contentId, String instructions) throws QException
   {
      ///////////////////////////////////////////////////////////////////////
      // the run's own bookkeeping is trusted internal work; the tools it  //
      // gives the model still read with PermissionEnforcement.ENFORCE     //
      ///////////////////////////////////////////////////////////////////////
      return (MarketingStore.trusted(() -> runImpl(task, campaignId, contentId, instructions)));
   }



   /*******************************************************************************
    ** See run.
    *******************************************************************************/
   private MarketingAgentRun runImpl(AgentTask task, Integer campaignId, Integer contentId, String instructions) throws QException
   {
      QSession session = QContext.getQSession();
      if(session == null || session instanceof QSystemUserSession || !session.hasPermission(MarketingPermissions.AGENT))
      {
         throw (new QUserFacingException("You do not have permission to run the marketing agent (" + MarketingPermissions.AGENT + ")."));
      }

      ////////////////////////////////////////////////////////////////
      // the user must be able to see the campaign (record locks)   //
      ////////////////////////////////////////////////////////////////
      MarketingCampaign campaign = new MarketingCampaign(new MarketingToolContext(null, 1).require(MarketingCampaign.TABLE_NAME, campaignId));

      MarketingRuntime runtime = MarketingRuntime.current();
      MarketingPolicy  policy  = runtime.readPolicy();
      AssistantModelProvider provider = runtime.resolveProvider(policy);
      String                 model    = runtime.resolveModel(policy);

      QRecord stored = MarketingStore.insertInternal(new MarketingAgentRun()
         .withProductId(campaign.getProductId())
         .withCampaignId(campaignId)
         .withContentId(contentId)
         .withTask(task.getId())
         .withStatus(AgentRunStatus.QUEUED.getId())
         .withInstructions(instructions)
         .withProviderName(provider == null ? null : provider.getName())
         .withModel(model)
         .withRequestedByUserId(MarketingStore.currentUserId())
         .withModelCalls(0)
         .withToolCalls(0));
      MarketingAgentRun run = new MarketingAgentRun(stored);

      String refusal = preflight(runtime, policy, provider, model);
      if(refusal != null)
      {
         return (finish(run, AgentRunStatus.FAILED, null, refusal, null, new MarketingToolContext(run, 1), new ModelUsage(), 0, 0));
      }

      if(!runtime.tryAcquireAgentPermit())
      {
         return (finish(run, AgentRunStatus.FAILED, null, "Too many agent runs at once (limit " + runtime.getConfig().getMaxConcurrentAgentRuns() + "); start it again shortly.",
            null, new MarketingToolContext(run, 1), new ModelUsage(), 0, 0));
      }
      try
      {
         return (execute(runtime, policy, provider, model, run, task));
      }
      finally
      {
         runtime.releaseAgentPermit();
      }
   }



   /*******************************************************************************
    ** Why a run can't start (null = it can).
    *******************************************************************************/
   String preflight(MarketingRuntime runtime, MarketingPolicy policy, AssistantModelProvider provider, String model) throws QException
   {
      if(policy == null)
      {
         return ("There is no Marketing Policy row named " + MarketingRuntime.POLICY_NAME + ", so the agent is off.");
      }
      if(!Boolean.TRUE.equals(policy.getAgentEnabled()))
      {
         return ("The marketing agent is switched off in Marketing Policy" + (StringUtils.hasContent(policy.getKillSwitchReason()) ? " (" + policy.getKillSwitchReason() + ")" : "") + ".");
      }
      if(provider == null)
      {
         return ("No model provider is configured, so the agent cannot run. Manual campaign work is unaffected; an administrator can register a provider and its credentials.");
      }
      if(!StringUtils.hasContent(model))
      {
         return ("No model is configured for provider " + provider.getName() + ".");
      }

      if(policy != null && policy.getDailyTokenLimit() != null)
      {
         long today = tokensSince(LocalDate.now(ZoneOffset.UTC).atStartOfDay().toInstant(ZoneOffset.UTC));
         if(today >= policy.getDailyTokenLimit())
         {
            return ("Today's agent token budget (" + policy.getDailyTokenLimit() + ") is used up (" + today + ").");
         }
      }
      if(policy != null && policy.getMonthlyCostLimitMicros() != null)
      {
         long month = costSince(LocalDate.now(ZoneOffset.UTC).withDayOfMonth(1).atStartOfDay().toInstant(ZoneOffset.UTC));
         if(month >= policy.getMonthlyCostLimitMicros())
         {
            return ("This month's agent cost budget is used up.");
         }
      }
      return (null);
   }



   /*******************************************************************************
    ** The loop.
    *******************************************************************************/
   private MarketingAgentRun execute(MarketingRuntime runtime, MarketingPolicy policy, AssistantModelProvider provider, String model, MarketingAgentRun run, AgentTask task)
      throws QException
   {
      MarketingQBitConfig config   = runtime.getConfig();
      Instant             deadline = Instant.now().plusSeconds(config.getMaxRunSeconds());
      int                 maxTools = policy != null && policy.getMaxToolCallsPerRun() != null ? policy.getMaxToolCallsPerRun() : config.getMaxToolCallsPerRun();

      MarketingStore.updateInternal(MarketingAgentRun.TABLE_NAME, new QRecord().withValue("id", run.getId())
         .withValue("status", AgentRunStatus.RUNNING.getId()).withValue("startedAt", Instant.now()));

      MarketingToolContext context = new MarketingToolContext(run, config.getMaxRowsToModel());
      ModelUsage           usage   = new ModelUsage();
      int                  seq     = 0;
      int                  modelCalls = 0;
      int                  toolCalls  = 0;

      try
      {
         ///////////////////////////////////////////
         // 1. deterministic checks (no model)    //
         ///////////////////////////////////////////
         ReadinessChecker checker  = new ReadinessChecker();
         int              findings = checker.checkCampaign(run.getCampaignId());
         if(task == AgentTask.REVIEW_CLAIMS || task == AgentTask.PLAN_CAMPAIGN)
         {
            findings += new ReadinessChecker().checkProduct(run.getProductId());
         }
         step(run, ++seq, AgentStepKind.CHECK, "readiness_check", "OK", findings + " open finding(s) from the deterministic readiness checks.", null, null, 0, 0, 0);

         ///////////////////////////
         // 2. the model loop     //
         ///////////////////////////
         MarketingToolRunner runner = new MarketingToolRunner(toolsFor(task));
         List<ModelToolDefinition> definitions = new ArrayList<>();
         for(MarketingAgentTool tool : runner.getTools().values())
         {
            definitions.add(new ModelToolDefinition().withName(tool.getName()).withDescription(tool.getDescription()).withInputSchema(tool.getInputSchema()).withStrict(true));
         }

         List<ModelMessage> messages = new ArrayList<>();
         messages.add(ModelMessage.user(MarketingAgentPrompts.task(task, run.getCampaignId(), run.getContentId(), run.getInstructions())));
         String finalText = null;

         while(true)
         {
            if(isCancelled(run.getId()))
            {
               return (finish(run, AgentRunStatus.CANCELLED, null, "Cancelled by a person.", null, context, usage, modelCalls, toolCalls));
            }
            if(Instant.now().isAfter(deadline))
            {
               return (finish(run, AgentRunStatus.FAILED, null, "Stopped: the run exceeded " + config.getMaxRunSeconds() + " seconds.", null, context, usage, modelCalls, toolCalls));
            }
            if(modelCalls >= config.getMaxModelCallsPerRun())
            {
               return (finish(run, AgentRunStatus.FAILED, null, "Stopped: the run reached its limit of " + config.getMaxModelCallsPerRun() + " model calls.", null, context, usage, modelCalls, toolCalls));
            }
            if(!runtime.tryReserveModelCall())
            {
               return (finish(run, AgentRunStatus.FAILED, null, "Stopped: the model-call rate limit (" + config.getMaxModelCallsPerMinute() + " per minute) was reached.", null, context, usage, modelCalls,
                  toolCalls));
            }

            ModelRequest request = new ModelRequest()
               .withModel(model)
               .withEffort(config.getEffort())
               .withMaxTokens(config.getMaxOutputTokens())
               .withParallelToolCalls(false)
               .withSystem(List.of(new SystemBlock(MarketingAgentPrompts.SYSTEM, CacheHint.SHORT)))
               .withTools(definitions)
               .withMessages(new ArrayList<>(messages));

            long          start    = System.currentTimeMillis();
            ModelResponse response = callWithRetry(provider, request, deadline);
            modelCalls++;
            usage.add(response.getUsage());
            step(run, ++seq, AgentStepKind.MODEL_CALL, null, response.getStopReason(), "Model " + model + ": " + response.getToolUses().size() + " tool call(s), stop " + response.getStopReason(),
               null, capped(redact(response.getText())), nz(response.getUsage().getInputTokens()), nz(response.getUsage().getOutputTokens()), System.currentTimeMillis() - start);

            List<ModelPart> toolUses = response.getToolUses();
            if(toolUses.isEmpty())
            {
               finalText = response.getText();
               if(ModelResponse.STOP_REFUSAL.equals(response.getStopReason()))
               {
                  return (finish(run, AgentRunStatus.FAILED, redact(finalText), "The model refused the task.", null, context, usage, modelCalls, toolCalls));
               }
               break;
            }

            messages.add(new ModelMessage().withRole(ModelMessage.Role.ASSISTANT).withParts(new ArrayList<>(response.getParts())).withProviderState(response.getProviderState()));
            ModelMessage results = new ModelMessage().withRole(ModelMessage.Role.USER).withCacheHint(CacheHint.SHORT);
            for(ModelPart use : toolUses)
            {
               if(toolCalls >= maxTools)
               {
                  results.withPart(ModelPart.toolResult(use.getToolUseId(), "{\"outcome\":\"LIMIT\",\"message\":\"The tool-call limit for this run is reached; finish now.\"}", true));
                  continue;
               }
               toolCalls++;
               MarketingToolRunner.Result result = runner.execute(use.getToolName(), use.getInput(), context);
               step(run, ++seq, AgentStepKind.TOOL_CALL, use.getToolName(), result.outcome().name(), capped(result.summary()), capped(JsonUtils.toJson(use.getInput())),
                  capped(result.modelContent()), 0, 0, result.durationMs());
               results.withPart(ModelPart.toolResult(use.getToolUseId(), result.modelContent(), result.outcome() != AiToolOutcome.OK));
            }
            messages.add(results);
         }

         Map<String, Object> output = new LinkedHashMap<>();
         output.put("draftContentIds", context.getDraftIds());
         output.put("findingsCreated", context.getFindingsCreated());
         output.put("plan", context.getPlan());
         return (finish(run, AgentRunStatus.SUCCEEDED, redact(finalText), null, JsonUtils.toPrettyJson(output), context, usage, modelCalls, toolCalls));
      }
      catch(ModelProviderException e)
      {
         usage.add(e.getPartialUsage());
         return (finish(run, AgentRunStatus.FAILED, null, "The model provider failed (" + e.getKind() + (e.getHttpStatus() == null ? "" : ", HTTP " + e.getHttpStatus()) + ").", null, context, usage,
            modelCalls, toolCalls));
      }
      catch(InterruptedException e)
      {
         Thread.currentThread().interrupt();
         return (finish(run, AgentRunStatus.FAILED, null, "Interrupted.", null, context, usage, modelCalls, toolCalls));
      }
      catch(Exception e)
      {
         LOG.warn("Marketing agent run failed", e, logPair("agentRunId", run.getId()));
         return (finish(run, AgentRunStatus.FAILED, null, "The run failed: " + (e instanceof QUserFacingException ? e.getMessage() : "an internal error occurred (see the server log)."), null, context,
            usage, modelCalls, toolCalls));
      }
   }



   /*******************************************************************************
    ** The tools a task may use: every read tool and record_finding; drafts only
    ** when drafting; the plan only when planning.
    *******************************************************************************/
   static List<MarketingAgentTool> toolsFor(AgentTask task)
   {
      List<MarketingAgentTool> list = new ArrayList<>();
      for(MarketingAgentTool tool : MarketingTools.all())
      {
         if(tool.getKind() == AiToolKind.READ || MarketingTools.RECORD_FINDING.equals(tool.getName())
            || (task == AgentTask.DRAFT_CONTENT && MarketingTools.SAVE_DRAFT.equals(tool.getName()))
            || (task == AgentTask.PLAN_CAMPAIGN && MarketingTools.SUBMIT_PLAN.equals(tool.getName())))
         {
            list.add(tool);
         }
      }
      return (list);
   }



   /*******************************************************************************
    ** One model call, retrying retryable provider errors twice.
    *******************************************************************************/
   private ModelResponse callWithRetry(AssistantModelProvider provider, ModelRequest request, Instant deadline) throws ModelProviderException, InterruptedException
   {
      ModelProviderException last = null;
      for(int attempt = 0; attempt < 3; attempt++)
      {
         Duration left = Duration.between(Instant.now(), deadline);
         if(left.isNegative() || left.isZero())
         {
            break;
         }
         try
         {
            return (provider.stream(request, NOOP_LISTENER).await(left));
         }
         catch(ModelProviderException e)
         {
            last = e;
            if(!e.isRetryable())
            {
               throw (e);
            }
            Thread.sleep(1000L << attempt);
         }
      }
      throw (last != null ? last : new ModelProviderException(ModelProviderException.Kind.TIMEOUT, null, "The run's time ran out waiting for the model."));
   }



   private static final ModelEventListener NOOP_LISTENER = text ->
   {
   };



   /*******************************************************************************
    ** Has a person cancelled the run?
    *******************************************************************************/
   private static boolean isCancelled(Integer runId) throws QException
   {
      QRecord current = MarketingStore.get(MarketingAgentRun.TABLE_NAME, runId);
      return (current != null && AgentRunStatus.CANCELLED.getId().equals(current.getValueInteger("status")));
   }



   /*******************************************************************************
    ** Finish a run: status, summary or error, usage and cost, counters.
    *******************************************************************************/
   private MarketingAgentRun finish(MarketingAgentRun run, AgentRunStatus status, String summary, String error, String outputJson, MarketingToolContext context, ModelUsage usage,
      int modelCalls, int toolCalls) throws QException
   {
      MarketingRuntime runtime = MarketingRuntime.current();
      ModelPricing     pricing = run.getModel() == null ? null : runtime.getConfig().getPricing().get(run.getModel());

      QRecord update = new QRecord().withValue("id", run.getId())
         .withValue("status", status.getId())
         .withValue("finishedAt", Instant.now())
         .withValue("modelCalls", modelCalls)
         .withValue("toolCalls", toolCalls)
         .withValue("inputTokens", nz(usage.getInputTokens()) + nz(usage.getCacheWriteTokens()))
         .withValue("outputTokens", nz(usage.getOutputTokens()))
         .withValue("cacheReadTokens", nz(usage.getCacheReadTokens()))
         .withValue("costMicros", pricing == null || modelCalls == 0 ? null : pricing.costMicros(usage))
         .withValue("draftsCreated", context.getDraftsCreated())
         .withValue("findingsCreated", context.getFindingsCreated())
         .withValue("summary", summary)
         .withValue("outputJson", outputJson)
         .withValue("errorMessage", error);
      MarketingStore.updateInternal(MarketingAgentRun.TABLE_NAME, update);
      LOG.info("Marketing agent run finished", logPair("agentRunId", run.getId()), logPair("status", status), logPair("modelCalls", modelCalls), logPair("toolCalls", toolCalls));
      return (new MarketingAgentRun(MarketingStore.get(MarketingAgentRun.TABLE_NAME, run.getId())));
   }



   /*******************************************************************************
    ** Runs left RUNNING by a stopped process (restart, crash) are failed, so
    ** they don't look alive; a person can start them again.
    *******************************************************************************/
   public static int failInterruptedRuns(Instant olderThan) throws QException
   {
      int count = 0;
      for(QRecord record : MarketingStore.query(MarketingAgentRun.TABLE_NAME, new QQueryFilter(
         new QFilterCriteria("status", QCriteriaOperator.IN, List.of(AgentRunStatus.RUNNING.getId(), AgentRunStatus.QUEUED.getId())),
         new QFilterCriteria("createDate", QCriteriaOperator.LESS_THAN, olderThan))))
      {
         MarketingStore.updateInternal(MarketingAgentRun.TABLE_NAME, new QRecord().withValue("id", record.getValue("id"))
            .withValue("status", AgentRunStatus.FAILED.getId()).withValue("finishedAt", Instant.now())
            .withValue("errorMessage", "Interrupted: the application stopped while this run was in progress. Start it again if needed."));
         count++;
      }
      return (count);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void step(MarketingAgentRun run, int seq, AgentStepKind kind, String toolName, String outcome, String summary, String inputJson, String resultJson, long inTokens,
      long outTokens, long millis) throws QException
   {
      MarketingStore.insertInternal(new MarketingAgentStep()
         .withProductId(run.getProductId())
         .withAgentRunId(run.getId())
         .withSeq(seq)
         .withKind(kind.getId())
         .withToolName(toolName)
         .withOutcome(outcome == null ? null : (outcome.length() > 32 ? outcome.substring(0, 32) : outcome))
         .withSummary(summary == null ? null : (summary.length() > 1000 ? summary.substring(0, 997) + "..." : summary))
         .withInputJson(inputJson)
         .withResultJson(resultJson)
         .withInputTokens(inTokens)
         .withOutputTokens(outTokens)
         .withDurationMs(millis));
   }



   /*******************************************************************************
    ** Agent tokens (input + cache writes + output) of runs created since.
    *******************************************************************************/
   static long tokensSince(Instant since) throws QException
   {
      long total = 0;
      for(QRecord record : MarketingStore.asSystem(() -> MarketingStore.query(MarketingAgentRun.TABLE_NAME, new QQueryFilter(new QFilterCriteria("createDate", QCriteriaOperator.GREATER_THAN_OR_EQUALS, since)))))
      {
         total += nz(record.getValueLong("inputTokens")) + nz(record.getValueLong("outputTokens"));
      }
      return (total);
   }



   /*******************************************************************************
    ** Known agent cost (micro-USD) of runs created since.
    *******************************************************************************/
   static long costSince(Instant since) throws QException
   {
      long total = 0;
      for(QRecord record : MarketingStore.asSystem(() -> MarketingStore.query(MarketingAgentRun.TABLE_NAME, new QQueryFilter(new QFilterCriteria("createDate", QCriteriaOperator.GREATER_THAN_OR_EQUALS, since)))))
      {
         total += nz(record.getValueLong("costMicros"));
      }
      return (total);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private String redact(String text)
   {
      return (text == null ? null : redactor.redact(text, new java.util.TreeSet<>()));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static String capped(String text)
   {
      return (text == null ? null : (text.length() > MAX_STEP_JSON ? text.substring(0, MAX_STEP_JSON) + "...[truncated]" : text));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static long nz(Long value)
   {
      return (value == null ? 0 : value);
   }

}
