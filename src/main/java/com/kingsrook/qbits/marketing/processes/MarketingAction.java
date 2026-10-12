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

package com.kingsrook.qbits.marketing.processes;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLine;
import com.kingsrook.qqq.backend.core.model.actions.processes.ProcessSummaryLineInterface;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.actions.processes.Status;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.AbstractLoadStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.AbstractTransformStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.ExtractViaQueryStep;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.ProcessSummaryProviderInterface;
import com.kingsrook.qqq.backend.core.processes.implementations.etl.streamedwithfrontend.StreamedETLWithFrontendProcess;
import com.kingsrook.qbits.marketing.security.MarketingPermissions;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** A Marketing record action: a standard streamed-ETL process (record input,
 ** Review, Result - with standard summary lines; no custom forms) on one
 ** table, run from that table's or record's actions - never from the nav.
 **
 ** - check(record) runs in Review and again in Result: null means the record
 **   will be processed; otherwise the reason it is skipped (a WARNING line).
 ** - perform(record) runs only at execute time and returns a short result
 **   (lines are grouped by it); a QUserFacingException becomes an ERROR line
 **   with its message.
 *******************************************************************************/
public abstract class MarketingAction
{
   private static final QLogger LOG = QLogger.getLogger(MarketingAction.class);

   private static final Map<String, MarketingAction> REGISTRY = new HashMap<>();

   private final String  processName;
   private final String  label;
   private final String  tableName;
   private final String  icon;
   private final String  permissionBase;
   private final String  okLabel;
   private Integer       maxRecords;



   /*******************************************************************************
    ** Constructor
    **
    ** @param okLabel what happens to each record, for the Review screen (e.g.
    **                "will be approved")
    *******************************************************************************/
   protected MarketingAction(String processName, String label, String tableName, String icon, String permissionBase, String okLabel)
   {
      this.processName = processName;
      this.label = label;
      this.tableName = tableName;
      this.icon = icon;
      this.permissionBase = permissionBase;
      this.okLabel = okLabel;
   }



   /*******************************************************************************
    ** Why this record will be skipped, or null.
    *******************************************************************************/
   public abstract String check(QRecord record) throws QException;

   /*******************************************************************************
    ** Do it; returns a short result.
    *******************************************************************************/
   public abstract String perform(QRecord record) throws QException;



   /*******************************************************************************
    ** Cap the records one run may take (e.g. agent runs).
    *******************************************************************************/
   public MarketingAction withMaxRecords(Integer maxRecords)
   {
      this.maxRecords = maxRecords;
      return (this);
   }



   /*******************************************************************************
    ** The process meta-data (and registration for its steps).
    *******************************************************************************/
   public QProcessMetaData buildProcess()
   {
      REGISTRY.put(processName, this);

      Map<String, Serializable> values = new LinkedHashMap<>();
      values.put(StreamedETLWithFrontendProcess.FIELD_SOURCE_TABLE, tableName);
      values.put(StreamedETLWithFrontendProcess.FIELD_DESTINATION_TABLE, tableName);
      values.put(StreamedETLWithFrontendProcess.FIELD_PREVIEW_MESSAGE, "These are the records this action will process.");
      values.put(StreamedETLWithFrontendProcess.FIELD_TRANSACTION_LEVEL, StreamedETLWithFrontendProcess.TRANSACTION_LEVEL_AUTO_COMMIT);

      QProcessMetaData process = StreamedETLWithFrontendProcess.defineProcessMetaData(ExtractViaQueryStep.class, Transform.class, Load.class, values);
      process.setName(processName);
      process.setLabel(label);
      process.setTableName(tableName);
      process.setIcon(new QIcon(icon));
      process.setPermissionRules(MarketingPermissions.hasAccessRules(permissionBase));
      return (process);
   }



   /*******************************************************************************
    ** The action of a process (registered by buildProcess).
    *******************************************************************************/
   static MarketingAction forProcess(String processName) throws QException
   {
      MarketingAction action = REGISTRY.get(processName);
      if(action == null)
      {
         throw (new QException("No marketing action is registered for process " + processName));
      }
      return (action);
   }



   /*******************************************************************************
    ** Getter for processName
    *******************************************************************************/
   public String getProcessName()
   {
      return (processName);
   }



   /*******************************************************************************
    ** The transform step: decides, per record, whether it will be processed.
    *******************************************************************************/
   public static class Transform extends AbstractTransformStep implements ProcessSummaryProviderInterface
   {
      private final ProcessSummaryLine              ok      = new ProcessSummaryLine(Status.OK);
      private final Map<String, ProcessSummaryLine> skipped = new LinkedHashMap<>();
      private int                                   taken   = 0;
      private MarketingAction                       action;



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void runOnePage(RunBackendStepInput input, RunBackendStepOutput output) throws QException
      {
         action = forProcess(input.getProcessName());
         ok.setMessage(action.okLabel);
         for(QRecord record : input.getRecords())
         {
            String reason;
            try
            {
               reason = action.maxRecords != null && taken >= action.maxRecords ? "over the limit of " + action.maxRecords + " per run" : action.check(record);
            }
            catch(QUserFacingException e)
            {
               reason = e.getMessage();
            }

            if(reason == null)
            {
               taken++;
               ok.incrementCountAndAddPrimaryKey(record.getValue("id"));
               output.getRecords().add(record);
            }
            else
            {
               skipped.computeIfAbsent(reason, r -> new ProcessSummaryLine(Status.WARNING, 0, "skipped: " + r)).incrementCountAndAddPrimaryKey(record.getValue("id"));
            }
         }
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public ArrayList<ProcessSummaryLineInterface> getProcessSummary(RunBackendStepOutput output, boolean isForResultScreen)
      {
         ArrayList<ProcessSummaryLineInterface> lines = new ArrayList<>();
         ok.addSelfToListIfAnyCount(lines);
         skipped.values().forEach(line -> line.addSelfToListIfAnyCount(lines));
         return (lines);
      }
   }



   /*******************************************************************************
    ** The load step: performs the action, per record.
    *******************************************************************************/
   public static class Load extends AbstractLoadStep implements ProcessSummaryProviderInterface
   {
      private final Map<String, ProcessSummaryLine> results = new LinkedHashMap<>();



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void runOnePage(RunBackendStepInput input, RunBackendStepOutput output) throws QException
      {
         MarketingAction action = forProcess(input.getProcessName());
         for(QRecord record : input.getRecords())
         {
            Serializable id = record.getValue("id");
            try
            {
               String result = action.perform(record);
               results.computeIfAbsent("OK:" + result, r -> new ProcessSummaryLine(Status.OK, 0, result)).incrementCountAndAddPrimaryKey(id);
            }
            catch(QUserFacingException e)
            {
               results.computeIfAbsent("ERR:" + e.getMessage(), r -> new ProcessSummaryLine(Status.ERROR, 0, "not done: " + e.getMessage())).incrementCountAndAddPrimaryKey(id);
            }
            catch(Exception e)
            {
               LOG.warn("Marketing action failed", e, logPair("process", action.processName), logPair("id", id));
               results.computeIfAbsent("ERR:internal", r -> new ProcessSummaryLine(Status.ERROR, 0, "failed with an internal error (see the server log)")).incrementCountAndAddPrimaryKey(id);
            }
         }
      }



      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public ArrayList<ProcessSummaryLineInterface> getProcessSummary(RunBackendStepOutput output, boolean isForResultScreen)
      {
         ArrayList<ProcessSummaryLineInterface> lines = new ArrayList<>();
         results.values().forEach(line -> line.addSelfToListIfAnyCount(lines));
         return (lines);
      }
   }



   /*******************************************************************************
    ** A plain (non-ETL) table action step code reference helper.
    *******************************************************************************/
   static QCodeReference code(Class<?> c)
   {
      return (new QCodeReference(c));
   }

}
