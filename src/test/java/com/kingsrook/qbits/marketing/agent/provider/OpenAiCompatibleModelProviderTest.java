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


import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;
import com.kingsrook.qbits.assistant.provider.ModelMessage;
import com.kingsrook.qbits.assistant.provider.ModelPart;
import com.kingsrook.qbits.assistant.provider.ModelProviderException;
import com.kingsrook.qbits.assistant.provider.ModelRequest;
import com.kingsrook.qbits.assistant.provider.ModelResponse;
import com.kingsrook.qbits.assistant.provider.ModelToolDefinition;
import com.kingsrook.qbits.assistant.provider.ProviderSettings;
import com.kingsrook.qbits.assistant.provider.SystemBlock;
import com.kingsrook.qbits.assistant.provider.openai.OpenAiCompatibleModelProvider;
import com.kingsrook.qbits.marketing.MarketingQBitConfig;
import com.kingsrook.qqq.backend.core.instances.QMetaDataVariableInterpreter;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** The published OpenAI-compatible provider against a local fixture server
 ** with no real key: request mapping, tool calls, usage, error kinds, and the key
 ** never appearing in toString.
 *******************************************************************************/
class OpenAiCompatibleModelProviderTest
{
   private HttpServer                server;
   private final AtomicReference<String> lastBody  = new AtomicReference<>();
   private final AtomicReference<String> lastAuth  = new AtomicReference<>();
   private final List<Object[]>          responses = new ArrayList<>();



   /*******************************************************************************
    **
    *******************************************************************************/
   @BeforeEach
   void beforeEach() throws Exception
   {
      server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
      server.createContext("/v1/chat/completions", exchange ->
      {
         lastBody.set(new String(exchange.getRequestBody().readAllBytes(), StandardCharsets.UTF_8));
         lastAuth.set(exchange.getRequestHeaders().getFirst("Authorization"));
         Object[] next = responses.remove(0);
         byte[]   out  = ((String) next[1]).getBytes(StandardCharsets.UTF_8);
         exchange.sendResponseHeaders((Integer) next[0], out.length);
         try(OutputStream os = exchange.getResponseBody())
         {
            os.write(out);
         }
      });
      server.start();
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @AfterEach
   void afterEach()
   {
      server.stop(0);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private OpenAiCompatibleModelProvider provider(String key)
   {
      return (new OpenAiCompatibleModelProvider(new ProviderSettings().withKind("OPENAI_COMPATIBLE")
         .withBaseUrl("http://127.0.0.1:" + server.getAddress().getPort() + "/v1")
         .withApiKey(key == null ? null : new QMetaDataVariableInterpreter().interpret(key)).withRequestTimeoutSeconds(5)));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static ModelRequest request()
   {
      return (new ModelRequest().withModel("test-model").withMaxTokens(100).withStreaming(false)
         .withSystem(List.of(new SystemBlock("Be truthful.", null)))
         .withTools(List.of(new ModelToolDefinition().withName("marketing_list_channels").withDescription("Channels")
            .withInputSchema(Map.of("type", "object", "properties", Map.of()))))
         .withMessages(new ArrayList<>(List.of(
            ModelMessage.user("Plan it."),
            new ModelMessage().withRole(ModelMessage.Role.ASSISTANT).withPart(ModelPart.text("Reading.")).withPart(ModelPart.toolUse("call_1", "marketing_list_channels", Map.of())),
            new ModelMessage().withRole(ModelMessage.Role.USER).withPart(ModelPart.toolResult("call_1", "<qqq_data>{}</qqq_data>", false))))));
   }



   /*******************************************************************************
    ** Tool calls, text and usage map both ways.
    *******************************************************************************/
   @Test
   void testToolCallsAndUsage() throws Exception
   {
      responses.add(new Object[] { 200, """
         {"id":"chatcmpl-1","choices":[{"finish_reason":"tool_calls","message":{"role":"assistant","content":"Checking.",
           "tool_calls":[{"id":"call_2","type":"function","function":{"name":"marketing_get_campaign_brief","arguments":"{\\"campaign_id\\":7}"}}]}}],
          "usage":{"prompt_tokens":120,"completion_tokens":30,"prompt_tokens_details":{"cached_tokens":20}}}
         """ });

      MarketingQBitConfig config = new MarketingQBitConfig()
         .withModelProvider(provider("${env.MARKETING_TEST_KEY_THAT_IS_NOT_SET}"))
         .withProviderName("openai-compatible").withModel("configured-model");
      ModelResponse response = config.getModelProviders().get(config.getProviderName())
         .stream(request().withModel(config.getModel()), text -> {}).await(Duration.ofSeconds(5));
      assertThat(response.getStopReason()).isEqualTo(ModelResponse.STOP_TOOL_USE);
      assertThat(response.getText()).isEqualTo("Checking.");
      assertThat(response.getToolUses()).hasSize(1);
      assertThat(response.getToolUses().get(0).getToolName()).isEqualTo("marketing_get_campaign_brief");
      assertThat(response.getToolUses().get(0).getInput()).containsEntry("campaign_id", 7);
      assertThat(response.getUsage().getInputTokens()).isEqualTo(100);
      assertThat(response.getUsage().getCacheReadTokens()).isEqualTo(20);
      assertThat(response.getUsage().getOutputTokens()).isEqualTo(30);

      String body = lastBody.get();
      assertThat(body).contains("\"role\":\"system\"").contains("Be truthful.").contains("\"tool_calls\"").contains("\"role\":\"tool\"").contains("\"tool_call_id\":\"call_1\"")
         .contains("\"max_tokens\":100").contains("marketing_list_channels").contains("\"model\":\"configured-model\"");
      assertThat(lastAuth.get()).as("an unset key reference means no Authorization header").isNull();
   }



   /*******************************************************************************
    ** HTTP errors map to kinds with the status and error type only; the key is
    ** sent as a bearer token and never shown.
    *******************************************************************************/
   @Test
   void testErrorsAndSecrecy()
   {
      OpenAiCompatibleModelProvider provider = provider("sk-FAKE-TEST-KEY-000000");
      assertThat(provider.toString()).doesNotContain("sk-FAKE-TEST-KEY-000000");

      responses.add(new Object[] { 401, "{\"error\":{\"type\":\"invalid_api_key\",\"message\":\"Incorrect API key provided: sk-FAKE-TEST-KEY-000000\"}}" });
      assertThatThrownBy(() -> provider.stream(request(), text -> {}).await(Duration.ofSeconds(5)))
         .isInstanceOfSatisfying(ModelProviderException.class, e ->
         {
            assertThat(e.getKind()).isEqualTo(ModelProviderException.Kind.AUTHENTICATION);
            assertThat(e.getHttpStatus()).isEqualTo(401);
            assertThat(e.getMessage()).doesNotContain("sk-FAKE-TEST-KEY-000000");
         });
      assertThat(lastAuth.get()).isEqualTo("Bearer sk-FAKE-TEST-KEY-000000");

      responses.add(new Object[] { 429, "{\"error\":{\"type\":\"rate_limit_exceeded\"}}" });
      assertThatThrownBy(() -> provider.stream(request(), text -> {}).await(Duration.ofSeconds(5)))
         .isInstanceOfSatisfying(ModelProviderException.class, e -> assertThat(e.isRetryable()).isTrue());

      responses.add(new Object[] { 400, "not json" });
      assertThatThrownBy(() -> provider.stream(request(), text -> {}).await(Duration.ofSeconds(5)))
         .isInstanceOfSatisfying(ModelProviderException.class, e -> assertThat(e.getKind()).isEqualTo(ModelProviderException.Kind.INVALID_REQUEST));

      responses.add(new Object[] { 503, "" });
      assertThatThrownBy(() -> provider.stream(request(), text -> {}).await(Duration.ofSeconds(5)))
         .isInstanceOfSatisfying(ModelProviderException.class, e -> assertThat(e.getKind()).isEqualTo(ModelProviderException.Kind.OVERLOADED));
   }



   /*******************************************************************************
    ** A refusal and a plain answer.
    *******************************************************************************/
   @Test
   void testStopReasons() throws Exception
   {
      responses.add(new Object[] { 200, "{\"choices\":[{\"finish_reason\":\"content_filter\",\"message\":{\"content\":null}}]}" });
      assertThat(provider(null).stream(request(), text -> {}).await(Duration.ofSeconds(5)).getStopReason()).isEqualTo(ModelResponse.STOP_REFUSAL);

      responses.add(new Object[] { 200, "{\"choices\":[{\"finish_reason\":\"stop\",\"message\":{\"content\":\"Done.\"}}]}" });
      List<String> streamed = new ArrayList<>();
      ModelResponse response = provider(null).stream(request(), streamed::add).await(Duration.ofSeconds(5));
      assertThat(response.getStopReason()).isEqualTo(ModelResponse.STOP_END_TURN);
      assertThat(streamed).containsExactly("Done.");
   }



   /*******************************************************************************
    ** The agent's default streaming mode receives text and usage from SSE.
    *******************************************************************************/
   @Test
   void testStreaming() throws Exception
   {
      responses.add(new Object[] { 200, """
         data: {"choices":[{"index":0,"delta":{"content":"Done."},"finish_reason":null}]}

         data: {"choices":[{"index":0,"delta":{},"finish_reason":"stop"}],"usage":{"prompt_tokens":12,"completion_tokens":3}}

         data: [DONE]

         """ });
      List<String> streamed = new ArrayList<>();
      ModelResponse response = provider(null).stream(request().withStreaming(true), streamed::add).await(Duration.ofSeconds(5));
      assertThat(streamed).containsExactly("Done.");
      assertThat(response.getText()).isEqualTo("Done.");
      assertThat(response.getUsage().getInputTokens()).isEqualTo(12);
      assertThat(response.getUsage().getOutputTokens()).isEqualTo(3);
      assertThat(lastBody.get()).contains("\"stream\":true");
   }

}
