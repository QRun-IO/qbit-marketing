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

package com.kingsrook.qbits.marketing.agent.tools;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qbits.marketing.model.MarketingAgentRun;
import com.kingsrook.qqq.backend.core.actions.ai.AiToolOutcome;
import com.kingsrook.qqq.backend.core.actions.ai.tools.AiToolException;
import com.kingsrook.qqq.backend.core.actions.permissions.PermissionEnforcement;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.tables.QInputSource;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;


/*******************************************************************************
 ** What a marketing tool runs with: the agent run (null when the tool runs
 ** through the QQQ AI tool kernel), the row cap, and helpers that read as the
 ** current (requesting) user with core permission enforcement ON, input
 ** source USER, hidden fields omitted and passwords masked - the same as the
 ** kernel's own tools.
 *******************************************************************************/
public class MarketingToolContext
{
   private final MarketingAgentRun agentRun;
   private final int               maxRows;

   private int                       draftsCreated   = 0;
   private int                       findingsCreated = 0;
   private final List<Integer>       draftIds        = new ArrayList<>();
   private Map<String, Object>       plan;



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   public MarketingToolContext(MarketingAgentRun agentRun, int maxRows)
   {
      this.agentRun = agentRun;
      this.maxRows = maxRows;
   }



   /*******************************************************************************
    ** Query as the user (enforced, capped at maxRows).
    *******************************************************************************/
   public List<QRecord> query(String tableName, QQueryFilter filter) throws QException
   {
      QueryInput input = new QueryInput(tableName).withFilter(filter.withLimit(maxRows));
      input.setInputSource(QInputSource.USER);
      input.setShouldOmitHiddenFields(true);
      input.setShouldMaskPasswords(true);
      input.withPermissionEnforcement(PermissionEnforcement.ENFORCE);
      return (new QueryAction().execute(input).getRecords());
   }



   /*******************************************************************************
    ** Get as the user (enforced); a NOT_FOUND tool error when it isn't
    ** visible.
    *******************************************************************************/
   public QRecord require(String tableName, Serializable id) throws QException
   {
      GetInput input = new GetInput(tableName).withPrimaryKey(id);
      input.setInputSource(QInputSource.USER);
      input.setShouldOmitHiddenFields(true);
      input.setShouldMaskPasswords(true);
      input.withPermissionEnforcement(PermissionEnforcement.ENFORCE);
      QRecord record = new GetAction().executeForRecord(input);
      if(record == null)
      {
         throw (new AiToolException(AiToolOutcome.NOT_FOUND, "No record " + id + " in " + tableName + " is available to you."));
      }
      return (record);
   }



   /*******************************************************************************
    ** A required integer input.
    *******************************************************************************/
   public static Integer requireInt(Map<String, Object> input, String key) throws AiToolException
   {
      Integer value = optionalInt(input, key);
      if(value == null)
      {
         throw (AiToolException.invalidInput("The input property '" + key + "' is required."));
      }
      return (value);
   }



   /*******************************************************************************
    ** An optional integer input.
    *******************************************************************************/
   public static Integer optionalInt(Map<String, Object> input, String key) throws AiToolException
   {
      Object value = input.get(key);
      if(value == null)
      {
         return (null);
      }
      if(value instanceof Number number && number.doubleValue() == Math.rint(number.doubleValue()) && Math.abs(number.doubleValue()) <= Integer.MAX_VALUE)
      {
         return (number.intValue());
      }
      throw (AiToolException.invalidInput("The input property '" + key + "' must be an integer."));
   }



   /*******************************************************************************
    ** A required string input of at most maxLength characters.
    *******************************************************************************/
   public static String requireString(Map<String, Object> input, String key, int maxLength) throws AiToolException
   {
      String value = optionalString(input, key, maxLength);
      if(value == null || value.isBlank())
      {
         throw (AiToolException.invalidInput("The input property '" + key + "' is required."));
      }
      return (value);
   }



   /*******************************************************************************
    ** An optional string input of at most maxLength characters.
    *******************************************************************************/
   public static String optionalString(Map<String, Object> input, String key, int maxLength) throws AiToolException
   {
      Object value = input.get(key);
      if(value == null)
      {
         return (null);
      }
      if(!(value instanceof String string))
      {
         throw (AiToolException.invalidInput("The input property '" + key + "' must be a string."));
      }
      if(string.length() > maxLength)
      {
         throw (AiToolException.invalidInput("The input property '" + key + "' is longer than " + maxLength + " characters."));
      }
      return (string);
   }



   /*******************************************************************************
    ** An optional list of integers.
    *******************************************************************************/
   public static List<Integer> optionalIntList(Map<String, Object> input, String key, int maxItems) throws AiToolException
   {
      Object value = input.get(key);
      List<Integer> list = new ArrayList<>();
      if(value == null)
      {
         return (list);
      }
      if(!(value instanceof List<?> raw) || raw.size() > maxItems)
      {
         throw (AiToolException.invalidInput("The input property '" + key + "' must be a list of at most " + maxItems + " integers."));
      }
      for(Object item : raw)
      {
         list.add(requireInt(Map.of("item", item == null ? "" : item), "item"));
      }
      return (list);
   }



   /*******************************************************************************
    ** An optional list of strings, each at most maxLength.
    *******************************************************************************/
   public static List<String> optionalStringList(Map<String, Object> input, String key, int maxItems, int maxLength) throws AiToolException
   {
      Object       value = input.get(key);
      List<String> list  = new ArrayList<>();
      if(value == null)
      {
         return (list);
      }
      if(!(value instanceof List<?> raw) || raw.size() > maxItems)
      {
         throw (AiToolException.invalidInput("The input property '" + key + "' must be a list of at most " + maxItems + " strings."));
      }
      for(Object item : raw)
      {
         list.add(requireString(Map.of("item", item == null ? "" : item), "item", maxLength));
      }
      return (list);
   }



   /*******************************************************************************
    ** A map that keeps insertion order (tool outputs).
    *******************************************************************************/
   public static Map<String, Object> map()
   {
      return (new LinkedHashMap<>());
   }



   /*******************************************************************************
    ** Getter for agentRun
    *******************************************************************************/
   public MarketingAgentRun getAgentRun()
   {
      return (agentRun);
   }



   /*******************************************************************************
    ** Getter for maxRows
    *******************************************************************************/
   public int getMaxRows()
   {
      return (maxRows);
   }



   /*******************************************************************************
    ** Count a draft.
    *******************************************************************************/
   public void addDraft(Integer contentId)
   {
      draftsCreated++;
      draftIds.add(contentId);
   }



   /*******************************************************************************
    ** Count a finding.
    *******************************************************************************/
   public void addFinding()
   {
      findingsCreated++;
   }



   /*******************************************************************************
    ** Getter for draftsCreated
    *******************************************************************************/
   public int getDraftsCreated()
   {
      return (draftsCreated);
   }



   /*******************************************************************************
    ** Getter for findingsCreated
    *******************************************************************************/
   public int getFindingsCreated()
   {
      return (findingsCreated);
   }



   /*******************************************************************************
    ** Getter for draftIds
    *******************************************************************************/
   public List<Integer> getDraftIds()
   {
      return (draftIds);
   }



   /*******************************************************************************
    ** Getter for plan
    *******************************************************************************/
   public Map<String, Object> getPlan()
   {
      return (plan);
   }



   /*******************************************************************************
    ** Setter for plan
    *******************************************************************************/
   public void setPlan(Map<String, Object> plan)
   {
      this.plan = plan;
   }

}
