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


import java.time.Instant;
import java.util.ArrayList;
import java.util.List;
import com.kingsrook.qbits.marketing.MarketingQBitConfig;
import com.kingsrook.qbits.marketing.MarketingRuntime;
import com.kingsrook.qbits.marketing.agent.MarketingAgentService;
import com.kingsrook.qbits.marketing.execution.ExecutionService;
import com.kingsrook.qbits.marketing.model.MarketingExecution;
import com.kingsrook.qbits.marketing.model.MarketingProduct;
import com.kingsrook.qbits.marketing.security.MarketingPermissions;
import com.kingsrook.qbits.marketing.seed.MarketingStarterData;
import com.kingsrook.qqq.backend.core.actions.processes.BackendStep;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepInput;
import com.kingsrook.qqq.backend.core.model.actions.processes.RunBackendStepOutput;
import com.kingsrook.qqq.backend.core.model.metadata.code.QCodeReference;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.fields.QFieldType;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QIcon;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QBackendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QFrontendStepMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.scheduleing.QScheduleMetaData;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 ** The two table-level actions that start without selected records:
 **
 ** - Run Due Executions (on Marketing Execution): the execution worker. It
 **   also turns expired leases into "needs reconciliation" and fails agent
 **   runs a restart interrupted. Scheduled on the host's scheduler when
 **   schedulerName is configured (it then runs as the system user).
 ** - Load Verified Starter Data (on Marketing Product): the bundled products,
 **   evidence, claims, audiences, draft campaigns and channels.
 **
 ** Both show their outcome on a result screen; neither is a nav item.
 *******************************************************************************/
public final class MarketingTableProcesses
{
   public static final String RUN_DUE_EXECUTIONS = "marketing_run_due_executions";
   public static final String LOAD_STARTER_DATA  = "marketing_load_starter_data";



   /*******************************************************************************
    ** Utility class - no instances.
    *******************************************************************************/
   private MarketingTableProcesses()
   {
   }



   /*******************************************************************************
    ** The processes.
    *******************************************************************************/
   public static List<QProcessMetaData> buildProcesses(MarketingQBitConfig config)
   {
      QProcessMetaData worker = process(RUN_DUE_EXECUTIONS, "Run Due Executions", MarketingExecution.TABLE_NAME, "schedule_send", MarketingPermissions.EXECUTE_BASE, WorkerStep.class);
      if(StringUtils.hasContent(config.getSchedulerName()))
      {
         worker.withSchedule(new QScheduleMetaData().withSchedulerName(config.getSchedulerName()).withRepeatSeconds(config.getWorkerRepeatSeconds()));
      }

      QProcessMetaData starter = process(LOAD_STARTER_DATA, "Load Verified Starter Data", MarketingProduct.TABLE_NAME, "playlist_add", MarketingPermissions.PREPARE_BASE, StarterStep.class);
      return (List.of(worker, starter));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static QProcessMetaData process(String name, String label, String tableName, String icon, String permissionBase, Class<? extends BackendStep> step)
   {
      return (new QProcessMetaData()
         .withName(name)
         .withLabel(label)
         .withTableName(tableName)
         .withIcon(new QIcon(icon))
         .withPermissionRules(MarketingPermissions.hasAccessRules(permissionBase))
         .withStep(new QBackendStepMetaData().withName("run").withCode(new QCodeReference(step)))
         .withStep(new QFrontendStepMetaData().withName("result").withViewField(new QFieldMetaData("summary", QFieldType.TEXT).withLabel("Result"))));
   }



   /*******************************************************************************
    ** The execution worker.
    *******************************************************************************/
   public static class WorkerStep implements BackendStep
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
      {
         Instant      now   = Instant.now();
         List<String> lines = new ArrayList<>(new ExecutionService().runDue(now));
         int          stale = MarketingAgentService.failInterruptedRuns(now.minusSeconds(2L * MarketingRuntime.current().getConfig().getMaxRunSeconds()));
         if(stale > 0)
         {
            lines.add(stale + " interrupted agent run(s) marked failed.");
         }
         output.addValue("summary", lines.isEmpty() ? "No executions were due." : String.join("\n", lines));
      }
   }



   /*******************************************************************************
    ** The starter-data loader.
    *******************************************************************************/
   public static class StarterStep implements BackendStep
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      @Override
      public void run(RunBackendStepInput input, RunBackendStepOutput output) throws QException
      {
         output.addValue("summary", new MarketingStarterData().load().summary());
      }
   }

}
