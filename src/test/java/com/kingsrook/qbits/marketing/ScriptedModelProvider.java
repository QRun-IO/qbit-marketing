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


import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import com.kingsrook.qbits.assistant.provider.AssistantModelProvider;
import com.kingsrook.qbits.assistant.provider.CompletableStreamHandle;
import com.kingsrook.qbits.assistant.provider.ModelCapabilities;
import com.kingsrook.qbits.assistant.provider.ModelEventListener;
import com.kingsrook.qbits.assistant.provider.ModelPart;
import com.kingsrook.qbits.assistant.provider.ModelProviderException;
import com.kingsrook.qbits.assistant.provider.ModelRequest;
import com.kingsrook.qbits.assistant.provider.ModelResponse;
import com.kingsrook.qbits.assistant.provider.ModelUsage;
import com.kingsrook.qbits.assistant.provider.StreamHandle;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;


/*******************************************************************************
 ** Synchronous scripted responses and request snapshots for marketing tests.
 *******************************************************************************/
class ScriptedModelProvider implements AssistantModelProvider
{
   private final String name;
   private final Deque<Step> steps = new ArrayDeque<>();
   private final List<ModelRequest> requests = new ArrayList<>();
   private final List<String> requestJson = new ArrayList<>();
   private ModelUsage usagePerCall = new ModelUsage();



   /*******************************************************************************
    ** Register under the name selected by the test configuration.
    *******************************************************************************/
   ScriptedModelProvider(String name)
   {
      this.name = name;
   }



   /*******************************************************************************
    ** Queue a complete answer.
    *******************************************************************************/
   ScriptedModelProvider thenText(String text)
   {
      return (thenRespond(request -> new ModelResponse().withParts(List.of(ModelPart.text(text))).withStopReason(ModelResponse.STOP_END_TURN)));
   }



   /*******************************************************************************
    ** Queue a single tool call.
    *******************************************************************************/
   ScriptedModelProvider thenToolUse(String toolName, Map<String, Object> input)
   {
      return (thenToolUses(null, List.<Object[]>of(new Object[] { toolName, input })));
   }



   /*******************************************************************************
    ** Queue parallel tool calls using the test fixtures' name/input pairs.
    *******************************************************************************/
   @SuppressWarnings("unchecked")
   ScriptedModelProvider thenToolUses(String preamble, List<Object[]> calls)
   {
      return (thenRespond(request ->
      {
         List<ModelPart> parts = new ArrayList<>();
         if(preamble != null)
         {
            parts.add(ModelPart.text(preamble));
         }
         for(Object[] call : calls)
         {
            parts.add(ModelPart.toolUse("tool_" + requests.size() + "_" + parts.size(), (String) call[0], (Map<String, Object>) call[1]));
         }
         return (new ModelResponse().withParts(parts).withStopReason(ModelResponse.STOP_TOOL_USE));
      }));
   }



   /*******************************************************************************
    ** Queue a provider failure, including usage already incurred.
    *******************************************************************************/
   ScriptedModelProvider thenError(ModelProviderException.Kind kind, String message)
   {
      return (thenRespond(request ->
      {
         throw (new ModelProviderException(kind, null, message).withPartialUsage(new ModelUsage().add(usagePerCall)));
      }));
   }



   /*******************************************************************************
    ** Queue a response that can inspect the request or change test state.
    *******************************************************************************/
   ScriptedModelProvider thenRespond(Step step)
   {
      steps.add(step);
      return (this);
   }



   /*******************************************************************************
    ** Set usage for budget and cost assertions.
    *******************************************************************************/
   ScriptedModelProvider withUsagePerCall(ModelUsage usage)
   {
      usagePerCall = usage;
      return (this);
   }



   /*******************************************************************************
    ** Provider registration name.
    *******************************************************************************/
   @Override
   public String getName()
   {
      return (name);
   }



   /*******************************************************************************
    ** Exercise the agent's prompt-cache hints.
    *******************************************************************************/
   @Override
   public ModelCapabilities getCapabilities(String modelId)
   {
      return (new ModelCapabilities().withPromptCaching(true).withThinking(false));
   }



   /*******************************************************************************
    ** Record before responding, preserving each turn before the agent mutates it.
    *******************************************************************************/
   @Override
   public StreamHandle stream(ModelRequest request, ModelEventListener listener)
   {
      requests.add(request.copy());
      requestJson.add(JsonUtils.toJson(request));
      CompletableStreamHandle handle = new CompletableStreamHandle();
      try
      {
         ModelResponse response = steps.removeFirst().respond(request);
         listener.onTextDelta(response.getText());
         response.setUsage(new ModelUsage().add(usagePerCall));
         response.setRequestId("request_" + requests.size());
         handle.complete(response);
      }
      catch(ModelProviderException e)
      {
         handle.fail(e);
      }
      return (handle);
   }



   /*******************************************************************************
    ** Request copies for tool exposure assertions.
    *******************************************************************************/
   List<ModelRequest> getRequests()
   {
      return (requests);
   }



   /*******************************************************************************
    ** Serialized snapshots for data-boundary assertions.
    *******************************************************************************/
   List<String> getRequestJson()
   {
      return (requestJson);
   }



   /*******************************************************************************
    ** A scripted step may return a response or a checked provider failure.
    *******************************************************************************/
   @FunctionalInterface
   interface Step
   {
      /*******************************************************************************
       ** Respond to one model request.
       *******************************************************************************/
      ModelResponse respond(ModelRequest request) throws ModelProviderException;
   }
}
