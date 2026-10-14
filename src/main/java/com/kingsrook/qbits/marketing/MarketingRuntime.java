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


import java.net.InetAddress;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Semaphore;
import java.util.concurrent.locks.ReentrantLock;
import com.kingsrook.qbits.assistant.provider.AssistantModelProvider;
import com.kingsrook.qbits.marketing.crm.MarketingCrmGateway;
import com.kingsrook.qbits.marketing.email.QbitEmailTransport;
import com.kingsrook.qbits.marketing.measurement.MarketingOutcomeImporter;
import com.kingsrook.qbits.marketing.model.MarketingPolicy;
import com.kingsrook.qbits.marketing.transport.ControlledTestTransport;
import com.kingsrook.qbits.marketing.transport.MarketingTransport;
import com.kingsrook.qbits.marketing.transport.QqqMessagingEmailTransport;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.QSupplementalInstanceMetaData;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 ** The live parts of the Marketing QBit for one instance: the transports, the
 ** model providers, the CRM gateway, and the in-process limits (agent
 ** concurrency, model-call rate, the single execution worker).
 **
 ** Registered as supplemental instance meta-data (not published to
 ** frontends), so process steps and widgets find it from QContext.
 **
 ** Scale note: the limits here are per JVM. One replica is the supported shape
 ** today; see docs/OPERATIONS.md ("Scale and limits").
 *******************************************************************************/
public class MarketingRuntime implements QSupplementalInstanceMetaData
{
   private static final QLogger LOG = QLogger.getLogger(MarketingRuntime.class);

   public static final String NAME = "marketingRuntime";

   public static final String POLICY_NAME = "default";

   private final MarketingQBitConfig             config;
   private final Map<String, MarketingTransport> transports = new LinkedHashMap<>();
   private final Semaphore                       agentPermits;
   private final ReentrantLock                   workerLock = new ReentrantLock();
   private final Deque<Long>                     modelCallTimes = new ArrayDeque<>();
   private final String                          workerName;
   private volatile MarketingTransport             qbitEmailTransport;



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   public MarketingRuntime(MarketingQBitConfig config)
   {
      this.config = config;
      if(Boolean.TRUE.equals(config.getIncludeTestTransport()))
      {
         transports.put(ControlledTestTransport.NAME, new ControlledTestTransport());
      }
      for(MarketingTransport transport : config.getTransports())
      {
         transports.put(transport.getName(), transport);
      }
      this.agentPermits = new Semaphore(config.getMaxConcurrentAgentRuns());
      this.workerName = hostName() + ":" + UUID.randomUUID().toString().substring(0, 8);
   }



   /*******************************************************************************
    ** The runtime of an instance (or null when the qbit isn't produced).
    *******************************************************************************/
   public static MarketingRuntime of(QInstance qInstance)
   {
      return (qInstance == null ? null : QSupplementalInstanceMetaData.of(qInstance, NAME));
   }



   /*******************************************************************************
    ** The runtime of the current context's instance; a clear error without it.
    *******************************************************************************/
   public static MarketingRuntime current() throws QException
   {
      MarketingRuntime runtime = of(QContext.getQInstance());
      if(runtime == null)
      {
         throw (new QException("The Marketing QBit is not produced in this instance"));
      }
      return (runtime);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String getName()
   {
      return (NAME);
   }



   /*******************************************************************************
    ** A transport by name, or null. When the host registered no transport named
    ** qqq_email (the default email channel name) and qbit-email is produced in the
    ** instance, that name resolves to QbitEmailTransport; a host-registered
    ** transport always wins, and QqqMessagingEmailTransport remains the fallback
    ** the host can register.
    *******************************************************************************/
   public MarketingTransport getTransport(String name)
   {
      if(name == null)
      {
         return (null);
      }
      MarketingTransport transport = transports.get(name);
      if(transport == null && QqqMessagingEmailTransport.DEFAULT_NAME.equals(name) && emailQBitClassPresent() && QbitEmailTransport.isInstalled(QContext.getQInstance()))
      {
         if(qbitEmailTransport == null)
         {
            qbitEmailTransport = new QbitEmailTransport();
         }
         return (qbitEmailTransport);
      }
      return (transport);
   }



   /*******************************************************************************
    ** qbit-email is an optional dependency: touch QbitEmailTransport only when
    ** its classes are present.
    *******************************************************************************/
   private static boolean emailQBitClassPresent()
   {
      try
      {
         Class.forName("com.kingsrook.qbits.email.api.EmailApi");
         return (true);
      }
      catch(ClassNotFoundException | LinkageError e)
      {
         return (false);
      }
   }



   /*******************************************************************************
    ** All transports.
    *******************************************************************************/
   public Map<String, MarketingTransport> getTransports()
   {
      return (transports);
   }



   /*******************************************************************************
    ** The controlled test transport (null when excluded).
    *******************************************************************************/
   public ControlledTestTransport getTestTransport()
   {
      return ((ControlledTestTransport) transports.get(ControlledTestTransport.NAME));
   }



   /*******************************************************************************
    ** The CRM gateway when configured and its CRM is present, else null.
    *******************************************************************************/
   public MarketingCrmGateway getAvailableCrmGateway()
   {
      MarketingCrmGateway gateway = config.getCrmGateway();
      return (gateway != null && gateway.isAvailable() ? gateway : null);
   }



   /*******************************************************************************
    ** The policy row (name "default"), read without the caller's permissions:
    ** it is the application's own configuration. Null when there is none.
    *******************************************************************************/
   public MarketingPolicy readPolicy() throws QException
   {
      QueryInput input = new QueryInput(MarketingPolicy.TABLE_NAME).withFilter(new QQueryFilter(new QFilterCriteria("name", QCriteriaOperator.EQUALS, POLICY_NAME)));
      input.withPermissionBypass("Marketing reads its own runtime policy");
      List<QRecord> records = new QueryAction().execute(input).getRecords();
      return (records.isEmpty() ? null : new MarketingPolicy(records.get(0)));
   }



   /*******************************************************************************
    ** The provider the agent uses now: the policy's choice, else the config's,
    ** else the only registered provider. Null when none is usable.
    *******************************************************************************/
   public AssistantModelProvider resolveProvider(MarketingPolicy policy)
   {
      Map<String, AssistantModelProvider> providers = config.getModelProviders();
      String                              name      = policy != null && StringUtils.hasContent(policy.getProviderName()) ? policy.getProviderName() : config.getProviderName();
      if(StringUtils.hasContent(name))
      {
         return (providers.get(name));
      }
      return (providers.size() == 1 ? providers.values().iterator().next() : null);
   }



   /*******************************************************************************
    ** The model id the agent uses now (policy, then config), or null.
    *******************************************************************************/
   public String resolveModel(MarketingPolicy policy)
   {
      if(policy != null && StringUtils.hasContent(policy.getModel()))
      {
         return (policy.getModel());
      }
      return (config.getModel());
   }



   /*******************************************************************************
    ** One line per integration for the configuration-status view. Honest about
    ** the difference between registered, configured and verified.
    *******************************************************************************/
   public List<IntegrationStatus> describeIntegrations()
   {
      List<IntegrationStatus> list = new ArrayList<>();

      MarketingPolicy policy = null;
      try
      {
         policy = readPolicy();
      }
      catch(QException e)
      {
         LOG.warn("Could not read the marketing policy for the status view", e);
      }

      if(policy == null)
      {
         list.add(new IntegrationStatus("Policy", POLICY_NAME, "Missing: agent and execution are off",
            "Create the Marketing Policy row named " + POLICY_NAME + " (or run Load Verified Starter Data) to switch them on."));
      }
      else
      {
         list.add(new IntegrationStatus("Policy", POLICY_NAME,
            "Agent " + (Boolean.TRUE.equals(policy.getAgentEnabled()) ? "on" : "off") + ", execution " + (Boolean.TRUE.equals(policy.getExecutionEnabled()) ? "on" : "off"),
            StringUtils.hasContent(policy.getKillSwitchReason()) ? policy.getKillSwitchReason() : "Switches and budgets are in Marketing Policy."));
      }

      AssistantModelProvider active = resolveProvider(policy);
      String                 model  = resolveModel(policy);
      if(config.getModelProviders().isEmpty())
      {
         list.add(new IntegrationStatus("Model provider", "(none)", "Not configured",
            "No model provider is registered: the agent is unavailable. Manual campaign work is unaffected."));
      }
      for(AssistantModelProvider provider : config.getModelProviders().values())
      {
         boolean isActive = provider == active;
         list.add(new IntegrationStatus("Model provider", provider.getName(),
            isActive && StringUtils.hasContent(model) ? "Configured (not verified by a live call)" : (isActive ? "Not configured: no model id" : "Registered (not selected)"),
            isActive ? ("Model: " + (StringUtils.hasContent(model) ? model : "(none)")) : "Select it in Marketing Policy or the qbit config."));
      }

      for(MarketingTransport transport : transports.values())
      {
         MarketingTransport.TransportStatus status = transport.getStatus();
         String label = switch(status.state())
         {
            case NOT_CONFIGURED -> "Not configured";
            case CONFIGURED -> "Configured, not verified (executions blocked)";
            case OPERATIONAL -> "Operational";
            case TEST_ONLY -> "Test only (nothing leaves the application)";
         };
         list.add(new IntegrationStatus("Transport", transport.getName(), label, status.detail()));
      }

      MarketingCrmGateway gateway = config.getCrmGateway();
      if(gateway == null)
      {
         list.add(new IntegrationStatus("CRM", "(none)", "Not configured", "No CRM gateway: CRM recipients, consent checks and lead qualification are unavailable."));
      }
      else
      {
         list.add(new IntegrationStatus("CRM", gateway.getName(), gateway.isAvailable() ? "Available" : "Not available",
            gateway.isAvailable() ? "Uses the CRM's own contact, consent, list and activity tables." : "The CRM's tables are not in this application."));
      }

      if(config.getOutcomeImporters().isEmpty())
      {
         list.add(new IntegrationStatus("Outcome importer", "(none)", "Not configured",
            "No importer: outcomes are recorded by hand, through the API or by the CRM import, so other metrics stay Not measured."));
      }
      for(MarketingOutcomeImporter importer : config.getOutcomeImporters())
      {
         list.add(new IntegrationStatus("Outcome importer", importer.getName(), "Registered (not verified by a live call)",
            "Runs from Import Outcomes on a campaign; events are deduplicated by source and external id."));
      }
      return (list);
   }



   /*******************************************************************************
    ** Try to start an agent run (bounded concurrency). Release with
    ** releaseAgentPermit.
    *******************************************************************************/
   public boolean tryAcquireAgentPermit()
   {
      return (agentPermits.tryAcquire());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   public void releaseAgentPermit()
   {
      agentPermits.release();
   }



   /*******************************************************************************
    ** Reserve a model call within the per-minute rate (false = over the limit).
    *******************************************************************************/
   public synchronized boolean tryReserveModelCall()
   {
      long now = System.currentTimeMillis();
      while(!modelCallTimes.isEmpty() && modelCallTimes.peekFirst() < now - 60_000)
      {
         modelCallTimes.pollFirst();
      }
      if(modelCallTimes.size() >= config.getMaxModelCallsPerMinute())
      {
         return (false);
      }
      modelCallTimes.addLast(now);
      return (true);
   }



   /*******************************************************************************
    ** The single execution worker's lock in this JVM.
    *******************************************************************************/
   public ReentrantLock getWorkerLock()
   {
      return (workerLock);
   }



   /*******************************************************************************
    ** This worker's name (host and a random suffix).
    *******************************************************************************/
   public String getWorkerName()
   {
      return (workerName);
   }



   /*******************************************************************************
    ** Getter for config
    *******************************************************************************/
   public MarketingQBitConfig getConfig()
   {
      return (config);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static String hostName()
   {
      try
      {
         return (InetAddress.getLocalHost().getHostName());
      }
      catch(Exception e)
      {
         return ("worker");
      }
   }



   /*******************************************************************************
    ** One integration's status.
    *******************************************************************************/
   public record IntegrationStatus(String kind, String name, String status, String detail)
   {
   }

}
