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


import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import com.kingsrook.qbits.marketing.metadata.MarketingHelp;
import com.kingsrook.qbits.marketing.metadata.MarketingMetaDataBuilder;
import com.kingsrook.qbits.marketing.processes.MarketingTableProcesses;
import com.kingsrook.qqq.backend.core.model.metadata.help.QHelpContent;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** Guardrail (owner requirement 2026-10-14: "all tables should be populated and
 ** the qbit should be fully using them"). For every marketing_* table:
 ** - runtime code (not just its model, metadata, home widgets or the starter
 **   data) reads or writes it, so no table exists only on paper;
 ** - it is either seeded by the starter data ("Load Verified Starter Data") or
 **   listed here with why it starts empty;
 ** - it has bundle help that says how its rows get created, naming the starter
 **   data action where that is one of the ways.
 ** A new table fails this test until it is wired, seeded or explained.
 *******************************************************************************/
class MarketingTablesGuardrailTest extends BaseMarketingTest
{
   private static final Path MAIN = Path.of("src", "main", "java", "com", "kingsrook", "qbits", "marketing");

   /*******************************************************************************
    ** Written by MarketingStarterData (checked against its source below).
    *******************************************************************************/
   static final List<String> SEEDED_BY_STARTER_DATA = List.of("marketing_product", "marketing_evidence_source", "marketing_claim", "marketing_audience",
      "marketing_campaign", "marketing_metric", "marketing_channel", "marketing_policy");

   /*******************************************************************************
    ** Tables that legitimately start empty, and where their rows come from.
    *******************************************************************************/
   static final Map<String, String> STARTS_EMPTY = Map.ofEntries(
      Map.entry("marketing_content", "user content: a person or the agent drafts it (starter data never writes publications)"),
      Map.entry("marketing_content_revision", "runtime: content is created or changed"),
      Map.entry("marketing_approval", "runtime: a person approves content"),
      Map.entry("marketing_execution", "runtime: Schedule on approved content"),
      Map.entry("marketing_execution_attempt", "runtime: an execution is tried"),
      Map.entry("marketing_test_delivery", "runtime: the test transport accepts an execution"),
      Map.entry("marketing_agent_run", "runtime: someone starts an agent action"),
      Map.entry("marketing_agent_step", "runtime: an agent run makes a call"),
      Map.entry("marketing_finding", "runtime: Check Readiness, the agent or a person"),
      Map.entry("marketing_outcome_event", "runtime: an importer, the API, Update Results from CRM or a person"),
      Map.entry("marketing_metric_daily", "runtime: rolled up from outcome events"));



   /*******************************************************************************
    ** Every table is used by runtime code.
    *******************************************************************************/
   @Test
   void everyTableIsUsedAtRuntime() throws Exception
   {
      Map<String, String> entityByTable = entityByTable();
      List<String>        runtimeSource;
      try(Stream<Path> files = Files.walk(MAIN))
      {
         runtimeSource = files.filter(p -> p.toString().endsWith(".java"))
            .filter(p -> !p.startsWith(MAIN.resolve("model")) && !p.startsWith(MAIN.resolve("metadata")) && !p.startsWith(MAIN.resolve("seed")) && !p.startsWith(MAIN.resolve("widgets")))
            .map(MarketingTablesGuardrailTest::read).toList();
      }
      for(String table : marketingTables())
      {
         String entity = entityByTable.get(table);
         assertThat(entity).as("entity class for " + table).isNotNull();
         Pattern use = Pattern.compile("\\b" + entity + "(\\.TABLE_NAME|\\.class|\\(|::)");
         assertThat(runtimeSource.stream().anyMatch(source -> use.matcher(source).find()))
            .as(table + " (" + entity + ") must be read or written by runtime code, not only declared").isTrue();
      }
   }



   /*******************************************************************************
    ** Every table is seeded or explained.
    *******************************************************************************/
   @Test
   void everyTableIsSeededOrExplained() throws Exception
   {
      String starter = read(MAIN.resolve("seed").resolve("MarketingStarterData.java"));
      Map<String, String> entityByTable = entityByTable();
      for(String table : marketingTables())
      {
         boolean seeded = SEEDED_BY_STARTER_DATA.contains(table);
         assertThat(seeded || STARTS_EMPTY.containsKey(table)).as(table + " must be seeded by the starter data or listed in STARTS_EMPTY").isTrue();
         assertThat(seeded && STARTS_EMPTY.containsKey(table)).as(table + " cannot be both seeded and listed as starting empty").isFalse();
         assertThat(starter.contains(entityByTable.get(table) + ".TABLE_NAME")).as(table + " seeded iff MarketingStarterData writes it").isEqualTo(seeded);
      }
      assertThat(STARTS_EMPTY.keySet()).allMatch(table -> qInstance.getTable(table) != null, "STARTS_EMPTY names only real tables");
      assertThat(qInstance.getProcess(MarketingTableProcesses.LOAD_STARTER_DATA).getTableName()).isEqualTo("marketing_product");
   }



   /*******************************************************************************
    ** Every table and the app have help that says how rows get created, and the
    ** seeded tables name the starter-data action.
    *******************************************************************************/
   @Test
   void everyTableHasEmptyStateHelp() throws Exception
   {
      String action = qInstance.getProcess(MarketingTableProcesses.LOAD_STARTER_DATA).getLabel();
      for(String table : marketingTables())
      {
         List<QHelpContent> help = help(table);
         assertThat(help).as(table + " table help").isNotEmpty();
         String content = help.getFirst().getContent();
         assertThat(content).as(table + " help says how rows get created").contains("How rows get created");
         assertThat(content).as(table + " help has no unresolved reference").doesNotContain("{{");
         if(SEEDED_BY_STARTER_DATA.contains(table))
         {
            assertThat(content).as(table + " help names the starter-data action").contains(action);
         }
      }
      assertThat(qInstance.getApp(MarketingMetaDataBuilder.APP_NAME).getHelpContent()).containsKey(MarketingHelp.SLOT);
      assertThat(MarketingHelp.entries().keySet()).anyMatch(key -> key.startsWith("table:marketing_content;slot:"));
      assertThat(help("marketing_content").getFirst().getContent()).as("enum references become the values").contains("Draft");
   }



   /*******************************************************************************
    ** A host's own help on a table is kept.
    *******************************************************************************/
   @Test
   void hostHelpIsNotOverridden() throws Exception
   {
      qInstance.getTable("marketing_claim").getHelpContent().put(MarketingHelp.SLOT, List.of(new QHelpContent().withContentAsMarkdown("host text")));
      MarketingHelp.apply(qInstance);
      assertThat(help("marketing_claim").getFirst().getContent()).isEqualTo("host text");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<QHelpContent> help(String table)
   {
      return (qInstance.getTable(table).getHelpContent() == null ? null : qInstance.getTable(table).getHelpContent().get(MarketingHelp.SLOT));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private List<String> marketingTables()
   {
      List<String> tables = qInstance.getTables().values().stream().map(QTableMetaData::getName).filter(n -> n.startsWith("marketing_")).sorted().toList();
      assertThat(tables).hasSize(19);
      return (tables);
   }



   /*******************************************************************************
    ** model/MarketingX.java declares TABLE_NAME = "marketing_x".
    *******************************************************************************/
   private static Map<String, String> entityByTable() throws Exception
   {
      Map<String, String> rs = new HashMap<>();
      Pattern             p  = Pattern.compile("TABLE_NAME\\s*=\\s*\"([a-z_]+)\"");
      try(Stream<Path> files = Files.list(MAIN.resolve("model")))
      {
         for(Path file : files.filter(f -> f.toString().endsWith(".java")).toList())
         {
            Matcher m = p.matcher(read(file));
            if(m.find())
            {
               rs.put(m.group(1), file.getFileName().toString().replace(".java", ""));
            }
         }
      }
      return (rs);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static String read(Path path)
   {
      try
      {
         return (Files.readString(path, StandardCharsets.UTF_8));
      }
      catch(Exception e)
      {
         throw (new IllegalStateException(e));
      }
   }

}
