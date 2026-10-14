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


import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.kingsrook.qbits.marketing.metadata.MarketingMetaDataBuilder;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSource;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSourceType;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QFieldSection;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;


/*******************************************************************************
 ** STAND-IN(core=QRun-IO/qqq#1198, removeBy=4.1.0-RC.8): replace with
 ** HelpBundleAssertions.forInstance(qInstance).bundles("help/qbit-marketing.yaml").owning(...).assertComplete()
 ** once qqq#1198 ships.
 **
 ** The help bundle (help/qbit-marketing.yaml) covers everything the qbit
 ** produces, and every entry is sound:
 ** - the app, every marketing_* table, every non-trivial field (all but the
 **   primary key, createDate, modifyDate and hidden fields), every process,
 **   every widget and every possible-value source has an entry;
 ** - every entry key resolves to a real object;
 ** - summaries are at most 120 characters, with no markup;
 ** - every {{...}} reference resolves.
 **
 ** Each test fails with the full list of missing or broken keys.
 *******************************************************************************/
class HelpBundleCoverageTest extends BaseMarketingTest
{
   private static final String BUNDLE_RESOURCE = "help/qbit-marketing.yaml";

   private static final int    MAX_SUMMARY_LENGTH = 120;
   private static final String MARKUP_CHARACTERS  = "<>*_`[]#";

   private static final Pattern REFERENCE = Pattern.compile("\\{\\{([A-Za-z]+):([^}]*)}}");



   /*******************************************************************************
    ** One bundle entry.
    *******************************************************************************/
   private record Entry(String key, String summary, String content)
   {
   }



   /*******************************************************************************
    ** The bundle header names this qbit, at its layer.
    *******************************************************************************/
   @Test
   void testHeader() throws IOException
   {
      Map<String, Object> bundle = loadBundle();
      assertThat(bundle.get("bundle")).isEqualTo("qbit-marketing");
      assertThat(bundle.get("layer")).isEqualTo("QBIT");
      assertThat(bundle.get("version")).isNotNull();
   }



   /*******************************************************************************
    ** Every object the qbit owns has an entry.
    *******************************************************************************/
   @Test
   void testEveryOwnedObjectHasAnEntry() throws IOException
   {
      Set<String>  keys     = new LinkedHashSet<>();
      List<String> problems = new ArrayList<>();
      for(Entry entry : loadEntries())
      {
         if(!keys.add(entry.key()))
         {
            problems.add("duplicate entry: " + entry.key());
         }
      }

      for(String expected : expectedKeys())
      {
         if(!keys.contains(expected))
         {
            problems.add("missing entry: " + expected);
         }
      }
      assertNoProblems(problems);
   }



   /*******************************************************************************
    ** Every entry key names something that exists.
    *******************************************************************************/
   @Test
   void testEveryKeyResolves() throws IOException
   {
      List<String> problems = new ArrayList<>();
      for(Entry entry : loadEntries())
      {
         String problem = resolveKey(entry.key());
         if(problem != null)
         {
            problems.add(entry.key() + ": " + problem);
         }
      }
      assertNoProblems(problems);
   }



   /*******************************************************************************
    ** Summaries are short, plain text; every entry has content.
    *******************************************************************************/
   @Test
   void testSummariesAndContent() throws IOException
   {
      List<String> problems = new ArrayList<>();
      for(Entry entry : loadEntries())
      {
         String summary = entry.summary();
         if(summary == null || summary.isBlank())
         {
            problems.add(entry.key() + ": no summary");
            continue;
         }
         if(summary.length() > MAX_SUMMARY_LENGTH)
         {
            problems.add(entry.key() + ": summary is " + summary.length() + " characters (limit " + MAX_SUMMARY_LENGTH + ")");
         }
         for(char c : MARKUP_CHARACTERS.toCharArray())
         {
            if(summary.indexOf(c) >= 0)
            {
               problems.add(entry.key() + ": summary contains the markup character " + c);
            }
         }
         if(entry.content() == null || entry.content().isBlank())
         {
            problems.add(entry.key() + ": no content");
         }
      }
      assertNoProblems(problems);
   }



   /*******************************************************************************
    ** Every {{...}} reference resolves.
    *******************************************************************************/
   @Test
   void testEveryReferenceResolves() throws IOException
   {
      List<String> problems = new ArrayList<>();
      for(Entry entry : loadEntries())
      {
         for(String text : new String[] { entry.summary(), entry.content() })
         {
            if(text == null)
            {
               continue;
            }
            Matcher matcher = REFERENCE.matcher(text);
            while(matcher.find())
            {
               String problem = resolveReference(matcher.group(1), matcher.group(2));
               if(problem != null)
               {
                  problems.add(entry.key() + ": {{" + matcher.group(1) + ":" + matcher.group(2) + "}} " + problem);
               }
            }
            if(text.replaceAll(REFERENCE.pattern(), "").contains("{{"))
            {
               problems.add(entry.key() + ": malformed {{ reference");
            }
         }
      }
      assertNoProblems(problems);
   }



   /*******************************************************************************
    ** The keys the bundle must contain, from the produced instance.
    *******************************************************************************/
   private Set<String> expectedKeys()
   {
      Set<String> keys = new TreeSet<>();
      keys.add("app:" + MarketingMetaDataBuilder.APP_NAME + ";slot:help");

      for(QTableMetaData table : qInstance.getTables().values())
      {
         if(!table.getName().startsWith("marketing_"))
         {
            continue;
         }
         keys.add("table:" + table.getName() + ";slot:help");
         for(QFieldMetaData field : table.getFields().values())
         {
            if(isNonTrivial(table, field))
            {
               keys.add("table:" + table.getName() + ";field:" + field.getName());
            }
         }
      }

      /////////////////////////////////////////////////////////////////////////
      // the qbit's own processes (QQQ also generates bulk table processes)  //
      /////////////////////////////////////////////////////////////////////////
      for(QProcessMetaData process : qInstance.getProcesses().values())
      {
         if(process.getSourceQBitName() != null)
         {
            keys.add("process:" + process.getName() + ";slot:help");
         }
      }

      qInstance.getWidgets().keySet().stream().filter(n -> n.startsWith("marketing_")).forEach(n -> keys.add("widget:" + n + ";slot:label"));
      qInstance.getPossibleValueSources().keySet().stream().filter(n -> n.startsWith("marketing_")).forEach(n -> keys.add("possibleValueSource:" + n + ";slot:help"));
      return (keys);
   }



   /*******************************************************************************
    ** Every field except the primary key, createDate, modifyDate and hidden ones.
    *******************************************************************************/
   private static boolean isNonTrivial(QTableMetaData table, QFieldMetaData field)
   {
      String name = field.getName();
      return (!name.equals(table.getPrimaryKeyField()) && !name.equals("createDate") && !name.equals("modifyDate") && !Boolean.TRUE.equals(field.getIsHidden()));
   }



   /*******************************************************************************
    ** Null if the key names a real object, else why not.
    *******************************************************************************/
   private String resolveKey(String key)
   {
      Map<String, String> parts = new LinkedHashMap<>();
      for(String part : key.split(";"))
      {
         int colon = part.indexOf(':');
         if(colon < 1)
         {
            return ("malformed key part [" + part + "]");
         }
         parts.put(part.substring(0, colon), part.substring(colon + 1));
      }

      if(parts.containsKey("app"))
      {
         return (qInstance.getApp(parts.get("app")) == null ? "no such app" : slotProblem(parts, "help"));
      }
      if(parts.containsKey("table"))
      {
         QTableMetaData table = qInstance.getTable(parts.get("table"));
         if(table == null)
         {
            return ("no such table");
         }
         if(parts.containsKey("field"))
         {
            return (table.getFields().containsKey(parts.get("field")) ? null : "no such field");
         }
         if(parts.containsKey("section"))
         {
            return (table.getSections() != null && table.getSections().stream().map(QFieldSection::getName).anyMatch(parts.get("section")::equals) ? null : "no such section");
         }
         return (slotProblem(parts, "help"));
      }
      if(parts.containsKey("process"))
      {
         QProcessMetaData process = qInstance.getProcess(parts.get("process"));
         if(process == null)
         {
            return ("no such process");
         }
         if(parts.containsKey("step"))
         {
            return (process.getStep(parts.get("step")) == null ? "no such step" : null);
         }
         return (parts.containsKey("field") ? "process fields are not covered by this bundle" : slotProblem(parts, "help"));
      }
      if(parts.containsKey("widget"))
      {
         return (qInstance.getWidget(parts.get("widget")) == null ? "no such widget" : slotProblem(parts, "label"));
      }
      if(parts.containsKey("possibleValueSource"))
      {
         return (qInstance.getPossibleValueSource(parts.get("possibleValueSource")) == null ? "no such possible-value source" : slotProblem(parts, "help"));
      }
      return ("unsupported key");
   }



   /*******************************************************************************
    ** The slot must be the expected one.
    *******************************************************************************/
   private static String slotProblem(Map<String, String> parts, String expectedSlot)
   {
      return (expectedSlot.equals(parts.get("slot")) ? null : "expected slot:" + expectedSlot);
   }



   /*******************************************************************************
    ** Null if the live reference resolves, else why not.
    *******************************************************************************/
   private String resolveReference(String kind, String target)
   {
      String[]       tableAndField = target.split("\\.", 2);
      QTableMetaData table         = qInstance.getTable(tableAndField[0]);
      QFieldMetaData field         = table == null || tableAndField.length < 2 ? null : table.getFields().get(tableAndField[1]);

      switch(kind)
      {
         case "label":
         {
            if(tableAndField.length == 2)
            {
               return (field == null ? "does not resolve to a table field" : null);
            }
            boolean found = table != null || qInstance.getProcess(target) != null || qInstance.getApp(target) != null;
            return (found ? null : "does not resolve to a table, process or app");
         }
         case "default":
         {
            return (field == null ? "does not resolve to a table field" : (field.getDefaultValue() == null ? "the field has no default value" : null));
         }
         case "maxLength":
         {
            return (field == null ? "does not resolve to a table field" : (field.getMaxLength() == null ? "the field has no maximum length" : null));
         }
         case "values":
         {
            QPossibleValueSource source = qInstance.getPossibleValueSource(target);
            return (source == null ? "does not resolve to a possible-value source" : (source.getType() == QPossibleValueSourceType.ENUM ? null : "is not an enum source"));
         }
         default:
         {
            return ("unsupported reference kind");
         }
      }
   }



   /*******************************************************************************
    ** The bundle, as parsed YAML.
    *******************************************************************************/
   private static Map<String, Object> loadBundle() throws IOException
   {
      try(InputStream inputStream = HelpBundleCoverageTest.class.getClassLoader().getResourceAsStream(BUNDLE_RESOURCE))
      {
         assertThat(inputStream).as("classpath resource " + BUNDLE_RESOURCE).isNotNull();
         @SuppressWarnings("unchecked")
         Map<String, Object> bundle = new ObjectMapper(new YAMLFactory()).readValue(inputStream, Map.class);
         return (bundle);
      }
   }



   /*******************************************************************************
    ** The bundle's entries.
    *******************************************************************************/
   @SuppressWarnings("unchecked")
   private static List<Entry> loadEntries() throws IOException
   {
      List<Entry> entries = new ArrayList<>();
      for(Map<String, Object> row : (List<Map<String, Object>>) loadBundle().get("entries"))
      {
         entries.add(new Entry(String.valueOf(row.get("key")), (String) row.get("summary"), (String) row.get("content")));
      }
      return (entries);
   }



   /*******************************************************************************
    ** Fail with every problem at once.
    *******************************************************************************/
   private static void assertNoProblems(List<String> problems)
   {
      assertThat(problems).as(problems.size() + " help bundle problem(s):\n" + String.join("\n", problems)).isEmpty();
   }

}
