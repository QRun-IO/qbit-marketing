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

package com.kingsrook.qbits.marketing.agent.provider;


import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.net.http.HttpTimeoutException;
import java.time.Duration;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import com.fasterxml.jackson.core.type.TypeReference;
import com.kingsrook.qbits.assistant.provider.AssistantModelProvider;
import com.kingsrook.qbits.assistant.provider.CompletableStreamHandle;
import com.kingsrook.qbits.assistant.provider.ModelCapabilities;
import com.kingsrook.qbits.assistant.provider.ModelEventListener;
import com.kingsrook.qbits.assistant.provider.ModelMessage;
import com.kingsrook.qbits.assistant.provider.ModelPart;
import com.kingsrook.qbits.assistant.provider.ModelProviderException;
import com.kingsrook.qbits.assistant.provider.ModelRequest;
import com.kingsrook.qbits.assistant.provider.ModelResponse;
import com.kingsrook.qbits.assistant.provider.ModelToolDefinition;
import com.kingsrook.qbits.assistant.provider.ModelUsage;
import com.kingsrook.qbits.assistant.provider.StreamHandle;
import com.kingsrook.qbits.assistant.provider.SystemBlock;
import com.kingsrook.qqq.backend.core.instances.QMetaDataVariableInterpreter;
import com.kingsrook.qqq.backend.core.utils.JsonUtils;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 ** A model provider for any OpenAI-compatible Chat Completions endpoint
 ** (POST {baseUrl}/chat/completions): OpenAI, and the many servers that speak
 ** the same API (OpenRouter, Ollama, vLLM, LM Studio, LiteLLM, ...).
 ** Implements qbit-assistant's AssistantModelProvider, so it plugs into the
 ** marketing agent (and the assistant) next to the Anthropic provider.
 **
 ** - No SDK: java.net.http. Non-streaming request; the text is delivered to
 **   the listener at the end.
 ** - The API key is read from the environment / secret reference given to
 **   the builder ("${env.NAME}"); it is never logged, and toString never
 **   contains it. A provider without a key is allowed for local servers that
 **   need none (Ollama).
 ** - Errors map to ModelProviderException kinds: 401/403 AUTHENTICATION,
 **   429 RATE_LIMITED, 5xx SERVER / OVERLOADED (503), 400/404/422
 **   INVALID_REQUEST, timeouts TIMEOUT, I/O NETWORK. Messages carry the HTTP
 **   status and the API's error type only.
 *******************************************************************************/
public class OpenAiCompatibleModelProvider implements AssistantModelProvider
{
   private final String     name;
   private final String     baseUrl;
   private final String     apiKey;
   private final Duration   requestTimeout;
   private final HttpClient httpClient;
   private final Boolean    strictTools;



   /*******************************************************************************
    ** Use the builder.
    *******************************************************************************/
   private OpenAiCompatibleModelProvider(Builder builder)
   {
      this.name = builder.name;
      this.baseUrl = builder.baseUrl.endsWith("/") ? builder.baseUrl.substring(0, builder.baseUrl.length() - 1) : builder.baseUrl;
      String key = builder.apiKey;
      if(key != null && key.startsWith("${"))
      {
         key = new QMetaDataVariableInterpreter().interpret(key);
      }
      this.apiKey = StringUtils.hasContent(key) ? key : null;
      this.requestTimeout = builder.requestTimeout;
      this.strictTools = builder.strictTools;
      this.httpClient = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(10)).build();
   }



   /*******************************************************************************
    ** A builder.
    *******************************************************************************/
   public static Builder builder()
   {
      return (new Builder());
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
   public ModelCapabilities getCapabilities(String modelId)
   {
      return (new ModelCapabilities().withTools(true).withParallelToolCalls(true).withPromptCaching(false).withThinking(false).withMaxContextTokens(128_000));
   }



   /*******************************************************************************
    ** Does it have a key? (Local servers may need none.)
    *******************************************************************************/
   public boolean hasApiKey()
   {
      return (apiKey != null);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public StreamHandle stream(ModelRequest request, ModelEventListener listener)
   {
      CompletableStreamHandle handle = new CompletableStreamHandle();
      CompletableFuture<HttpResponse<String>> future;
      try
      {
         HttpRequest.Builder http = HttpRequest.newBuilder(URI.create(baseUrl + "/chat/completions"))
            .timeout(requestTimeout)
            .header("Content-Type", "application/json")
            .POST(HttpRequest.BodyPublishers.ofString(JsonUtils.toJson(toRequestBody(request))));
         if(apiKey != null)
         {
            http.header("Authorization", "Bearer " + apiKey);
         }
         future = httpClient.sendAsync(http.build(), HttpResponse.BodyHandlers.ofString());
      }
      catch(Exception e)
      {
         handle.fail(new ModelProviderException(ModelProviderException.Kind.INVALID_REQUEST, null, "Could not build the request: " + e.getClass().getSimpleName()));
         return (handle);
      }

      handle.setOnCancel(() -> future.cancel(true));
      future.whenComplete((response, error) ->
      {
         if(error != null)
         {
            Throwable cause = error.getCause() != null ? error.getCause() : error;
            if(cause instanceof HttpTimeoutException)
            {
               handle.fail(new ModelProviderException(ModelProviderException.Kind.TIMEOUT, null, "The request timed out."));
            }
            else if(cause instanceof IOException)
            {
               handle.fail(new ModelProviderException(ModelProviderException.Kind.NETWORK, null, "Network error: " + cause.getClass().getSimpleName()));
            }
            else
            {
               handle.fail(new ModelProviderException(ModelProviderException.Kind.OTHER, null, "Request failed: " + cause.getClass().getSimpleName()));
            }
            return;
         }

         try
         {
            if(response.statusCode() >= 300)
            {
               handle.fail(toException(response.statusCode(), response.body()));
               return;
            }
            ModelResponse modelResponse = fromResponseBody(response.body());
            String        text          = modelResponse.getText();
            if(StringUtils.hasContent(text))
            {
               listener.onTextDelta(text);
            }
            handle.complete(modelResponse);
         }
         catch(Exception e)
         {
            handle.fail(new ModelProviderException(ModelProviderException.Kind.OTHER, response.statusCode(), "Unreadable response: " + e.getClass().getSimpleName()));
         }
      });
      return (handle);
   }



   /*******************************************************************************
    ** ModelRequest -> Chat Completions request body.
    *******************************************************************************/
   Map<String, Object> toRequestBody(ModelRequest request)
   {
      List<Map<String, Object>> messages = new ArrayList<>();
      StringBuilder             system   = new StringBuilder();
      for(SystemBlock block : request.getSystem())
      {
         if(system.length() > 0)
         {
            system.append("\n\n");
         }
         system.append(block.getText());
      }
      if(system.length() > 0)
      {
         messages.add(Map.of("role", "system", "content", system.toString()));
      }

      for(ModelMessage message : request.getMessages())
      {
         if(message.getRole() == ModelMessage.Role.ASSISTANT)
         {
            Map<String, Object>       assistant = new LinkedHashMap<>();
            StringBuilder             text      = new StringBuilder();
            List<Map<String, Object>> toolCalls = new ArrayList<>();
            for(ModelPart part : message.getParts())
            {
               if(part.getType() == ModelPart.Type.TEXT && part.getText() != null)
               {
                  text.append(part.getText());
               }
               else if(part.getType() == ModelPart.Type.TOOL_USE)
               {
                  toolCalls.add(Map.of("id", part.getToolUseId(), "type", "function",
                     "function", Map.of("name", part.getToolName(), "arguments", JsonUtils.toJson(part.getInput() == null ? Map.of() : part.getInput()))));
               }
            }
            assistant.put("role", "assistant");
            assistant.put("content", text.length() == 0 ? null : text.toString());
            if(!toolCalls.isEmpty())
            {
               assistant.put("tool_calls", toolCalls);
            }
            messages.add(assistant);
         }
         else
         {
            StringBuilder text = new StringBuilder();
            for(ModelPart part : message.getParts())
            {
               if(part.getType() == ModelPart.Type.TOOL_RESULT)
               {
                  messages.add(Map.of("role", "tool", "tool_call_id", part.getToolUseId(), "content", part.getText() == null ? "" : part.getText()));
               }
               else if(part.getText() != null)
               {
                  text.append(part.getText());
               }
            }
            if(text.length() > 0)
            {
               messages.add(Map.of("role", "user", "content", text.toString()));
            }
         }
      }

      Map<String, Object> body = new LinkedHashMap<>();
      body.put("model", request.getModel());
      body.put("messages", messages);
      if(request.getMaxTokens() != null)
      {
         body.put("max_completion_tokens", request.getMaxTokens());
      }
      if(!request.getTools().isEmpty())
      {
         List<Map<String, Object>> tools = new ArrayList<>();
         for(ModelToolDefinition tool : request.getTools())
         {
            Map<String, Object> function = new LinkedHashMap<>();
            function.put("name", tool.getName());
            function.put("description", tool.getDescription());
            function.put("parameters", tool.getInputSchema());
            ////////////////////////////////////////////////////////////////////
            // OpenAI's strict mode needs every property required; the        //
            // agent validates inputs itself, so strict is off by default.    //
            ////////////////////////////////////////////////////////////////////
            if(Boolean.TRUE.equals(strictTools))
            {
               function.put("strict", true);
            }
            tools.add(Map.of("type", "function", "function", function));
         }
         body.put("tools", tools);
         body.put("parallel_tool_calls", Boolean.TRUE.equals(request.getParallelToolCalls()));
      }
      return (body);
   }



   /*******************************************************************************
    ** Chat Completions response body -> ModelResponse.
    *******************************************************************************/
   @SuppressWarnings("unchecked")
   ModelResponse fromResponseBody(String json) throws Exception
   {
      Map<String, Object>       root    = JsonUtils.toObject(json, new TypeReference<>() {});
      List<Map<String, Object>> choices = (List<Map<String, Object>>) root.get("choices");
      if(choices == null || choices.isEmpty())
      {
         throw (new IllegalStateException("no choices"));
      }
      Map<String, Object> choice  = choices.get(0);
      Map<String, Object> message = (Map<String, Object>) choice.get("message");

      ModelResponse response = new ModelResponse();
      Object        content  = message == null ? null : message.get("content");
      if(content instanceof String text && !text.isEmpty())
      {
         response.getParts().add(ModelPart.text(text));
      }
      List<Map<String, Object>> toolCalls = message == null ? null : (List<Map<String, Object>>) message.get("tool_calls");
      if(toolCalls != null)
      {
         for(Map<String, Object> call : toolCalls)
         {
            Map<String, Object> function  = (Map<String, Object>) call.get("function");
            String              arguments = function == null ? null : (String) function.get("arguments");
            Map<String, Object> input     = StringUtils.hasContent(arguments) ? JsonUtils.toObject(arguments, new TypeReference<Map<String, Object>>() {}) : new LinkedHashMap<>();
            response.getParts().add(ModelPart.toolUse((String) call.get("id"), function == null ? null : (String) function.get("name"), input));
         }
      }

      String finish = (String) choice.get("finish_reason");
      response.setStopReason(switch(finish == null ? "" : finish)
      {
         case "tool_calls", "function_call" -> ModelResponse.STOP_TOOL_USE;
         case "length" -> ModelResponse.STOP_MAX_TOKENS;
         case "content_filter" -> ModelResponse.STOP_REFUSAL;
         default -> response.getToolUses().isEmpty() ? ModelResponse.STOP_END_TURN : ModelResponse.STOP_TOOL_USE;
      });

      Map<String, Object> usage = (Map<String, Object>) root.get("usage");
      if(usage != null)
      {
         long prompt = number(usage.get("prompt_tokens"));
         long cached = 0;
         if(usage.get("prompt_tokens_details") instanceof Map<?, ?> details)
         {
            cached = number(details.get("cached_tokens"));
         }
         response.setUsage(new ModelUsage().withInputTokens(Math.max(0, prompt - cached)).withCacheReadTokens(cached).withOutputTokens(number(usage.get("completion_tokens"))));
      }
      response.setRequestId((String) root.get("id"));
      return (response);
   }



   /*******************************************************************************
    ** An HTTP error -> a provider exception (status and error type only).
    *******************************************************************************/
   @SuppressWarnings("unchecked")
   static ModelProviderException toException(int status, String body)
   {
      String type = null;
      try
      {
         Map<String, Object> root  = JsonUtils.toObject(body, new TypeReference<>() {});
         Object              error = root.get("error");
         if(error instanceof Map<?, ?> map)
         {
            type = map.get("type") instanceof String s ? s : (map.get("code") instanceof String c ? c : null);
         }
      }
      catch(Exception e)
      {
         // not JSON: the status alone
      }

      ModelProviderException.Kind kind = switch(status)
      {
         case 401, 403 -> ModelProviderException.Kind.AUTHENTICATION;
         case 429 -> ModelProviderException.Kind.RATE_LIMITED;
         case 503, 529 -> ModelProviderException.Kind.OVERLOADED;
         case 400, 404, 413, 422 -> ModelProviderException.Kind.INVALID_REQUEST;
         case 408 -> ModelProviderException.Kind.TIMEOUT;
         default -> status >= 500 ? ModelProviderException.Kind.SERVER : ModelProviderException.Kind.OTHER;
      };
      return (new ModelProviderException(kind, status, "HTTP " + status + (type == null ? "" : " (" + type + ")")));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static long number(Object value)
   {
      return (value instanceof Number n ? n.longValue() : 0);
   }



   /*******************************************************************************
    ** Never includes the key.
    *******************************************************************************/
   @Override
   public String toString()
   {
      return ("OpenAiCompatibleModelProvider{name=" + name + ", baseUrl=" + baseUrl + ", apiKey=" + (apiKey == null ? "none" : "***") + "}");
   }



   /*******************************************************************************
    ** Builder.
    *******************************************************************************/
   public static class Builder
   {
      private String   name           = "openai";
      private String   baseUrl        = "https://api.openai.com/v1";
      private String   apiKey;
      private Duration requestTimeout = Duration.ofSeconds(120);
      private Boolean  strictTools    = false;



      /*******************************************************************************
       ** The provider's registration name.
       *******************************************************************************/
      public Builder name(String name)
      {
         this.name = name;
         return (this);
      }



      /*******************************************************************************
       ** e.g. https://api.openai.com/v1 or http://localhost:11434/v1
       *******************************************************************************/
      public Builder baseUrl(String baseUrl)
      {
         this.baseUrl = baseUrl;
         return (this);
      }



      /*******************************************************************************
       ** The key, or a reference such as "${env.OPENAI_API_KEY}".
       *******************************************************************************/
      public Builder apiKey(String apiKey)
      {
         this.apiKey = apiKey;
         return (this);
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      public Builder requestTimeout(Duration requestTimeout)
      {
         this.requestTimeout = requestTimeout;
         return (this);
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      public Builder strictTools(Boolean strictTools)
      {
         this.strictTools = strictTools;
         return (this);
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      public OpenAiCompatibleModelProvider build()
      {
         if(!StringUtils.hasContent(name) || !StringUtils.hasContent(baseUrl))
         {
            throw (new IllegalArgumentException("name and baseUrl are required"));
         }
         return (new OpenAiCompatibleModelProvider(this));
      }
   }

}
