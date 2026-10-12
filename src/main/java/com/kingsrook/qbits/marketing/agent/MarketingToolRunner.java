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


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import com.kingsrook.qbits.marketing.agent.tools.MarketingAgentTool;
import com.kingsrook.qbits.marketing.agent.tools.MarketingToolContext;
import com.kingsrook.qqq.backend.core.actions.ai.AiToolOutcome;
import com.kingsrook.qqq.backend.core.actions.ai.output.AiDataEnvelope;
import com.kingsrook.qqq.backend.core.actions.ai.output.AiRedactor;
import com.kingsrook.qqq.backend.core.actions.ai.tools.AiToolException;
import com.kingsrook.qqq.backend.core.exceptions.QPermissionDeniedException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Runs one marketing tool call the way the QQQ AI tool kernel runs its own
 ** tools (and the way the kernel runs these tools once QQQ has AiToolRegistry):
 **
 ** - in the requesting user's session (the caller's QContext; the tools read
 **   with PermissionEnforcement.ENFORCE and record locks apply);
 ** - strict input: unknown keys are refused;
 ** - every string in the result redacted (AiRedactor: keys, tokens, card
 **   numbers, ...) and capped;
 ** - the result wrapped in the qqq_data untrusted-data envelope;
 ** - never throws: problems are outcomes (DENIED, NOT_FOUND, INVALID_INPUT,
 **   ERROR) with a short message for the model; internal details are logged
 **   only.
 *******************************************************************************/
public class MarketingToolRunner
{
   private static final QLogger LOG = QLogger.getLogger(MarketingToolRunner.class);

   static final int MAX_STRING_CHARS = 8000;

   private final Map<String, MarketingAgentTool> tools = new LinkedHashMap<>();
   private final AiRedactor                      redactor = new AiRedactor(List.of());



   /*******************************************************************************
    ** The result of one call.
    *******************************************************************************/
   public record Result(AiToolOutcome outcome, String modelContent, String summary, long durationMs, Set<String> redactions)
   {
   }



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   public MarketingToolRunner(List<MarketingAgentTool> tools)
   {
      for(MarketingAgentTool tool : tools)
      {
         this.tools.put(tool.getName(), tool);
      }
   }



   /*******************************************************************************
    ** The tools by name.
    *******************************************************************************/
   public Map<String, MarketingAgentTool> getTools()
   {
      return (tools);
   }



   /*******************************************************************************
    ** Run one call. Never throws.
    *******************************************************************************/
   @SuppressWarnings("unchecked")
   public Result execute(String toolName, Map<String, Object> input, MarketingToolContext context)
   {
      long        start      = System.currentTimeMillis();
      Set<String> redactions = new TreeSet<>();
      String      source     = toolName == null ? "tool" : toolName;
      try
      {
         MarketingAgentTool tool = toolName == null ? null : tools.get(toolName);
         if(tool == null)
         {
            throw (AiToolException.invalidInput("Unknown tool '" + toolName + "'. Available tools are: " + String.join(", ", tools.keySet()) + "."));
         }

         Map<String, Object> safeInput  = input == null ? new LinkedHashMap<>() : input;
         Map<String, Object> properties = (Map<String, Object>) tool.getInputSchema().get("properties");
         TreeSet<String>     unknown    = new TreeSet<>(safeInput.keySet());
         unknown.removeAll(properties == null ? Set.of() : properties.keySet());
         if(!unknown.isEmpty())
         {
            throw (AiToolException.invalidInput("Unknown input properties: " + String.join(", ", unknown) + "."));
         }

         Map<String, Object> data    = tool.run(context, safeInput);
         Object              cleaned = clean(data, redactions);
         return (new Result(AiToolOutcome.OK, AiDataEnvelope.wrap(source, null, JsonUtils.toJson(cleaned)), summarize(tool, data), System.currentTimeMillis() - start, redactions));
      }
      catch(AiToolException e)
      {
         return (failure(e.getOutcome(), e.getMessage(), source, start, redactions));
      }
      catch(QPermissionDeniedException e)
      {
         return (failure(AiToolOutcome.DENIED, "You do not have permission to do that.", source, start, redactions));
      }
      catch(QUserFacingException e)
      {
         return (failure(AiToolOutcome.INVALID_INPUT, e.getMessage(), source, start, redactions));
      }
      catch(Exception e)
      {
         LOG.warn("Error running a marketing tool", e, logPair("toolName", source));
         return (failure(AiToolOutcome.ERROR, "An unexpected error occurred while running this tool.", source, start, redactions));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static Result failure(AiToolOutcome outcome, String message, String source, long start, Set<String> redactions)
   {
      Map<String, Object> data = new LinkedHashMap<>();
      data.put("outcome", outcome.name());
      data.put("message", message);
      return (new Result(outcome, AiDataEnvelope.wrap(source, null, JsonUtils.toJson(data)), message, System.currentTimeMillis() - start, redactions));
   }



   /*******************************************************************************
    ** Redact and cap every string, recursively.
    *******************************************************************************/
   @SuppressWarnings("unchecked")
   Object clean(Object value, Set<String> redactions)
   {
      if(value instanceof String string)
      {
         String redacted = redactor.redact(string, redactions);
         return (redacted.length() > MAX_STRING_CHARS ? redacted.substring(0, MAX_STRING_CHARS) + "...[truncated]" : redacted);
      }
      if(value instanceof Map<?, ?> map)
      {
         Map<String, Object> copy = new LinkedHashMap<>();
         map.forEach((k, v) -> copy.put(String.valueOf(k), clean(v, redactions)));
         return (copy);
      }
      if(value instanceof List<?> list)
      {
         List<Object> copy = new ArrayList<>();
         list.forEach(v -> copy.add(clean(v, redactions)));
         return (copy);
      }
      return (value);
   }



   /*******************************************************************************
    ** A one-line summary for the step log (no data values).
    *******************************************************************************/
   private static String summarize(MarketingAgentTool tool, Map<String, Object> data)
   {
      StringBuilder summary = new StringBuilder(tool.getName()).append(" OK");
      for(Map.Entry<String, Object> entry : data.entrySet())
      {
         if(entry.getValue() instanceof List<?> list)
         {
            summary.append(", ").append(entry.getKey()).append(": ").append(list.size());
         }
         else if(entry.getKey().endsWith("_id") && entry.getValue() != null)
         {
            summary.append(", ").append(entry.getKey()).append("=").append(entry.getValue());
         }
      }
      return (summary.toString());
   }

}
