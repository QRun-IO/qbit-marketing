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


import java.lang.reflect.Method;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qbits.marketing.agent.tools.MarketingAgentTool;
import com.kingsrook.qbits.marketing.agent.tools.MarketingToolContext;
import com.kingsrook.qqq.backend.core.actions.ai.AiToolKind;
import com.kingsrook.qqq.backend.core.actions.ai.tools.AbstractAiTool;
import com.kingsrook.qqq.backend.core.actions.ai.tools.AiToolContext;
import com.kingsrook.qqq.backend.core.actions.ai.tools.AiToolInput;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Contributes the marketing READ tools to the QQQ AI tool kernel, so the
 ** assistant panel (qbit-assistant) can answer marketing questions with them.
 **
 ** The registration hook (AiToolRegistry, QRun-IO/qqq#1096) is not in QQQ
 ** 4.1.0-RC.4, so this looks it up by name: with it, the tools are
 ** registered; without it, nothing happens and the marketing agent still
 ** runs them through MarketingToolRunner (the same contract).
 *******************************************************************************/
public final class KernelToolRegistration
{
   private static final QLogger LOG = QLogger.getLogger(KernelToolRegistration.class);

   static final String REGISTRY_CLASS = "com.kingsrook.qqq.backend.core.actions.ai.AiToolRegistry";



   /*******************************************************************************
    ** Utility class - no instances.
    *******************************************************************************/
   private KernelToolRegistration()
   {
   }



   /*******************************************************************************
    ** Is the kernel's registration hook present in this QQQ?
    *******************************************************************************/
   public static boolean isAvailable()
   {
      try
      {
         Class.forName(REGISTRY_CLASS, false, KernelToolRegistration.class.getClassLoader());
         return (true);
      }
      catch(ClassNotFoundException | LinkageError e)
      {
         return (false);
      }
   }



   /*******************************************************************************
    ** Register the READ tools when the hook exists. Returns how many were
    ** registered (0 without the hook).
    *******************************************************************************/
   public static int register(QInstance qInstance, List<MarketingAgentTool> tools)
   {
      if(!isAvailable())
      {
         LOG.info("QQQ has no AiToolRegistry; marketing tools run only inside the marketing agent");
         return (0);
      }

      try
      {
         Class<?> registryClass = Class.forName(REGISTRY_CLASS);
         Object   registry      = registryClass.getMethod("ofOrWithNew", QInstance.class).invoke(null, qInstance);
         Method   withTool      = registryClass.getMethod("withTool", AbstractAiTool.class);
         Method   getTools      = registryClass.getMethod("getTools");
         int      count         = 0;
         for(MarketingAgentTool tool : tools)
         {
            if(tool.getKind() != AiToolKind.READ || ((Map<?, ?>) getTools.invoke(registry)).containsKey(tool.getName()))
            {
               continue;
            }
            withTool.invoke(registry, new KernelAdapter(tool));
            count++;
         }
         LOG.info("Registered marketing tools with the AI tool kernel", logPair("count", count));
         return (count);
      }
      catch(Exception e)
      {
         LOG.warn("Could not register marketing tools with the AI tool kernel", e);
         return (0);
      }
   }



   /*******************************************************************************
    ** A marketing READ tool as a kernel tool. The kernel supplies the session,
    ** strict keys, envelope and outcome mapping; this redacts and caps strings
    ** with the kernel's own sanitizer.
    *******************************************************************************/
   static class KernelAdapter extends AbstractAiTool
   {
      private final MarketingAgentTool tool;



      /*******************************************************************************
       **
       *******************************************************************************/
      KernelAdapter(MarketingAgentTool tool)
      {
         this.tool = tool;
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public String getName()
      {
         return (tool.getName());
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public String getDescription()
      {
         return (tool.getDescription());
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public AiToolKind getKind()
      {
         return (tool.getKind());
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public Map<String, Object> getInputSchema()
      {
         return (tool.getInputSchema());
      }



      /*******************************************************************************
       ** The read tools take only integer ids.
       *******************************************************************************/
      @Override
      @SuppressWarnings("unchecked")
      public Map<String, Object> execute(AiToolContext context, AiToolInput input) throws QException
      {
         Map<String, Object> raw = new LinkedHashMap<>();
         for(String key : getInputPropertyNames())
         {
            Integer value = input.getOptionalInteger(key);
            if(value != null)
            {
               raw.put(key, value);
            }
         }
         Map<String, Object> data = tool.run(new MarketingToolContext(null, context.getMaxRowsToModel()), raw);
         return ((Map<String, Object>) sanitize(context, data));
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      private static Object sanitize(AiToolContext context, Object value)
      {
         if(value instanceof String string)
         {
            return (context.sanitize(string));
         }
         if(value instanceof Map<?, ?> map)
         {
            Map<String, Object> copy = new LinkedHashMap<>();
            map.forEach((k, v) -> copy.put(String.valueOf(k), sanitize(context, v)));
            return (copy);
         }
         if(value instanceof List<?> list)
         {
            return (list.stream().map(v -> sanitize(context, v)).toList());
         }
         return (value);
      }
   }

}
