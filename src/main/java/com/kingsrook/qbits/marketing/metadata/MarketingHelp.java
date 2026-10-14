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

package com.kingsrook.qbits.marketing.metadata;


import java.io.IOException;
import java.io.InputStream;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.dataformat.yaml.YAMLFactory;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.help.QHelpContent;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QAppMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSource;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;


/*******************************************************************************
 ** Applies help/qbit-marketing.yaml (the QQQ help-bundle format) to the
 ** Marketing app and its marketing_* tables: what each table is for and
 ** how its rows get created, shown on the table screen and when it is empty.
 ** STAND-IN(core=QRun-IO/qqq#1198, removeBy=4.1.0-RC.8): replace with HelpBundles.register once the pinned
 ** QQQ release includes it; the bundle file stays the same.
 *******************************************************************************/
public final class MarketingHelp
{
   public static final String RESOURCE = "/help/qbit-marketing.yaml";
   public static final String SLOT     = "help";

   private static final Pattern REFERENCE = Pattern.compile("\\{\\{([A-Za-z]+):([^}]*)}}");



   /*******************************************************************************
    ** Utility class.
    *******************************************************************************/
   private MarketingHelp()
   {
   }



   /*******************************************************************************
    ** The bundle's entries by key, validated (fails startup if malformed).
    *******************************************************************************/
   public static Map<String, JsonNode> entries() throws QException
   {
      try(InputStream stream = MarketingHelp.class.getResourceAsStream(RESOURCE))
      {
         if(stream == null)
         {
            throw (new IOException("missing resource"));
         }
         JsonNode bundle = new ObjectMapper(new YAMLFactory()).readTree(stream);
         if(!"qbit-marketing".equals(bundle.path("bundle").asText()) || !"QBIT".equals(bundle.path("layer").asText()))
         {
            throw (new IOException("invalid bundle header"));
         }
         Map<String, JsonNode> entries = new LinkedHashMap<>();
         for(JsonNode entry : bundle.path("entries"))
         {
            String key     = entry.path("key").asText();
            String summary = entry.path("summary").asText();
            if(key.isBlank() || summary.isBlank() || summary.length() > 120 || entry.path("content").asText().isBlank() || entries.put(key, entry) != null)
            {
               throw (new IOException("invalid or duplicate entry: " + key));
            }
         }
         return (entries);
      }
      catch(IOException | RuntimeException e)
      {
         throw (new QException("Could not load " + RESOURCE, e));
      }
   }



   /*******************************************************************************
    ** Apply the help to the app and every marketing_* table in the instance.
    ** A host's own help on a table is kept (the bundle only fills the slot).
    *******************************************************************************/
   public static void apply(QInstance instance) throws QException
   {
      Map<String, JsonNode> entries = entries();
      for(QTableMetaData table : instance.getTables().values())
      {
         JsonNode entry = entries.get("table:" + table.getName() + ";slot:" + SLOT);
         if(entry != null && (table.getHelpContent() == null || !table.getHelpContent().containsKey(SLOT)))
         {
            table.withHelpContent(SLOT, content(instance, entry));
         }
      }
      QAppMetaData app      = instance.getApp(MarketingMetaDataBuilder.APP_NAME);
      JsonNode     appEntry = entries.get("app:marketing;slot:" + SLOT);
      if(app != null && appEntry != null && (app.getHelpContent() == null || !app.getHelpContent().containsKey(SLOT)))
      {
         app.withHelpContent(SLOT, content(instance, appEntry));
      }
   }



   /*******************************************************************************
    ** Summary as the first paragraph, then the content, as markdown.
    *******************************************************************************/
   static QHelpContent content(QInstance instance, JsonNode entry)
   {
      return (new QHelpContent().withContentAsMarkdown(resolve(instance, entry.path("summary").asText().strip()) + "\n\n" + resolve(instance, entry.path("content").asText().strip())));
   }



   /*******************************************************************************
    ** Resolve the bundle's {{label:..}}, {{default:..}}, {{maxLength:..}} and
    ** {{values:..}} references against the instance, so the text is plain.
    *******************************************************************************/
   static String resolve(QInstance instance, String text)
   {
      Matcher       matcher = REFERENCE.matcher(text);
      StringBuilder out     = new StringBuilder();
      while(matcher.find())
      {
         matcher.appendReplacement(out, Matcher.quoteReplacement(referenceText(instance, matcher.group(1), matcher.group(2))));
      }
      matcher.appendTail(out);
      return (out.toString());
   }



   /*******************************************************************************
    ** One reference's text; unresolvable ones keep their target name.
    *******************************************************************************/
   private static String referenceText(QInstance instance, String kind, String target)
   {
      String[]       tableAndField = target.split("\\.", 2);
      QTableMetaData table         = instance.getTable(tableAndField[0]);
      QFieldMetaData field         = table == null || tableAndField.length < 2 ? null : table.getFields().get(tableAndField[1]);
      switch(kind)
      {
         case "label":
            if(field != null)
            {
               return (field.getLabel());
            }
            if(table != null)
            {
               return (table.getLabel());
            }
            if(instance.getProcess(target) != null)
            {
               return (instance.getProcess(target).getLabel());
            }
            return (instance.getApp(target) != null ? instance.getApp(target).getLabel() : target);
         case "default":
            return (field == null || field.getDefaultValue() == null ? target : String.valueOf(field.getDefaultValue()));
         case "maxLength":
            return (field == null || field.getMaxLength() == null ? target : String.valueOf(field.getMaxLength()));
         case "values":
            QPossibleValueSource source = instance.getPossibleValueSource(target);
            return (source == null || source.getEnumValues() == null ? target : source.getEnumValues().stream().map(value -> String.valueOf(value.getLabel())).collect(Collectors.joining(", ")));
         default:
            return (target);
      }
   }

}
