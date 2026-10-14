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


import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import com.kingsrook.qbits.assistant.spi.ModelPricing;
import com.kingsrook.qbits.assistant.spi.AssistantModelProvider;
import com.kingsrook.qbits.marketing.crm.MarketingCrmGateway;
import com.kingsrook.qbits.marketing.measurement.MarketingOutcomeImporter;
import com.kingsrook.qbits.marketing.transport.MarketingTransport;
import com.kingsrook.qqq.backend.core.model.metadata.producers.MetaDataCustomizerInterface;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.qbits.QBitConfig;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.StringUtils;


/*******************************************************************************
 ** Configuration of the Marketing QBit.
 **
 ** Everything with a live external effect is optional and starts unconfigured:
 ** model providers (the agent), transports other than the controlled test
 ** transport, and the CRM gateway. The qbit stays usable for manual campaign
 ** work without any of them, and reports what is and isn't configured.
 **
 ** Credentials never go here: a provider or transport reads its own secret
 ** (for example a "${env.NAME}" reference) and never prints it.
 *******************************************************************************/
public class MarketingQBitConfig implements QBitConfig
{
   public static final String TABLE_NAME_PREFIX    = "marketing_";
   public static final String PERMISSION_BASE_NAME = "marketing";

   private String backendName;

   private transient MetaDataCustomizerInterface<QTableMetaData> tableMetaDataCustomizer;

   private Boolean inferBackendNames = true;
   private String  productSecurityKeyType;

   private transient Map<String, AssistantModelProvider> modelProviders         = new LinkedHashMap<>();
   private String                                        providerName;
   private String                                        model;
   private String                                        effort;
   private Integer                                       maxOutputTokens        = 8000;
   private Integer                                       maxToolCallsPerRun     = 24;
   private Integer                                       maxModelCallsPerRun    = 16;
   private Integer                                       maxRunSeconds          = 300;
   private Integer                                       maxConcurrentAgentRuns = 2;
   private Integer                                       maxModelCallsPerMinute = 30;
   private Integer                                       maxRowsToModel         = 50;
   private Map<String, ModelPricing>                     pricing                = new LinkedHashMap<>(ModelPricing.defaults());
   private Boolean                                       registerKernelTools    = true;

   private transient List<MarketingTransport> transports                = new ArrayList<>();
   private Boolean                            includeTestTransport      = true;
   private String                             schedulerName;
   private Integer                            workerRepeatSeconds       = 60;
   private Integer                            workerBatchSize           = 25;
   private Integer                            defaultMaxAttempts        = 3;
   private Integer                            retryBackoffSeconds       = 60;
   private Integer                            leaseSeconds              = 300;
   private Integer                            maxRecipientsPerExecution = 50;
   private Boolean                            requireDifferentApprover  = false;
   private String                             aiDisclosureText          = "Prepared with AI assistance.";

   private Integer                       staleSourceDays = 90;
   private transient MarketingCrmGateway crmGateway;

   /** Sources that are Marketing's own and cannot be an importer's name. */
   private static final List<String> RESERVED_SOURCES = List.of("marketing", "crm");

   private transient List<MarketingOutcomeImporter> outcomeImporters = new ArrayList<>();



   /*******************************************************************************
    ** Check the settings.
    *******************************************************************************/
   @Override
   public void validate(QInstance qInstance, List<String> errors)
   {
      assertCondition(StringUtils.hasContent(backendName), "backendName is required for the Marketing QBit", errors);
      if(StringUtils.hasContent(backendName) && qInstance != null && qInstance.getBackend(backendName) == null)
      {
         errors.add("The Marketing QBit backend [" + backendName + "] is not defined in the instance");
      }
      assertCondition(positive(maxToolCallsPerRun) && positive(maxModelCallsPerRun) && positive(maxRunSeconds) && positive(maxConcurrentAgentRuns),
         "agent limits (maxToolCallsPerRun, maxModelCallsPerRun, maxRunSeconds, maxConcurrentAgentRuns) must be positive", errors);
      assertCondition(positive(defaultMaxAttempts) && positive(workerBatchSize) && positive(leaseSeconds) && positive(maxRecipientsPerExecution),
         "execution limits (defaultMaxAttempts, workerBatchSize, leaseSeconds, maxRecipientsPerExecution) must be positive", errors);
      if(StringUtils.hasContent(providerName))
      {
         assertCondition(modelProviders.containsKey(providerName), "providerName [" + providerName + "] is not one of the registered model providers " + modelProviders.keySet(), errors);
      }
      if(StringUtils.hasContent(schedulerName) && qInstance != null)
      {
         assertCondition(qInstance.getScheduler(schedulerName) != null, "schedulerName [" + schedulerName + "] is not defined in the instance", errors);
      }
      List<String> names = new ArrayList<>();
      for(MarketingTransport transport : transports)
      {
         if(assertCondition(transport != null && StringUtils.hasContent(transport.getName()), "every transport needs a name", errors))
         {
            assertCondition(!names.contains(transport.getName()), "duplicate transport name [" + transport.getName() + "]", errors);
            names.add(transport.getName());
         }
      }

      List<String> importerNames = new ArrayList<>();
      for(MarketingOutcomeImporter importer : outcomeImporters)
      {
         if(assertCondition(importer != null && StringUtils.hasContent(importer.getName()), "every outcome importer needs a name", errors))
         {
            assertCondition(!importerNames.contains(importer.getName()), "duplicate outcome importer name [" + importer.getName() + "]", errors);
            assertCondition(!RESERVED_SOURCES.contains(importer.getName()), "outcome importer name [" + importer.getName() + "] is reserved (it is the source of Marketing's own events)", errors);
            importerNames.add(importer.getName());
         }
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static boolean positive(Integer value)
   {
      return (value != null && value > 0);
   }



   /*******************************************************************************
    ** Tables use the configured backend.
    *******************************************************************************/
   @Override
   public String getDefaultBackendNameForTables()
   {
      return (backendName);
   }



   /*******************************************************************************
    ** Register a model provider under its own name (getName()).
    *******************************************************************************/
   public MarketingQBitConfig withModelProvider(AssistantModelProvider provider)
   {
      if(provider != null)
      {
         this.modelProviders.put(provider.getName(), provider);
      }
      return (this);
   }



   /*******************************************************************************
    ** Register a transport.
    *******************************************************************************/
   public MarketingQBitConfig withTransport(MarketingTransport transport)
   {
      this.transports.add(transport);
      return (this);
   }



   /*******************************************************************************
    ** Register an outcome importer (run by the Import Outcomes campaign action).
    *******************************************************************************/
   public MarketingQBitConfig withOutcomeImporter(MarketingOutcomeImporter importer)
   {
      this.outcomeImporters.add(importer);
      return (this);
   }



   /*******************************************************************************
    ** Getter for outcomeImporters
    *******************************************************************************/
   public List<MarketingOutcomeImporter> getOutcomeImporters()
   {
      return (this.outcomeImporters);
   }



   /*******************************************************************************
    ** Getter for backendName
    *******************************************************************************/
   public String getBackendName()
   {
      return (this.backendName);
   }



   /*******************************************************************************
    ** Setter for backendName
    *******************************************************************************/
   public void setBackendName(String backendName)
   {
      this.backendName = backendName;
   }



   /*******************************************************************************
    ** Fluent setter for backendName
    *******************************************************************************/
   public MarketingQBitConfig withBackendName(String backendName)
   {
      this.backendName = backendName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for tableMetaDataCustomizer
    *******************************************************************************/
   @Override
   public MetaDataCustomizerInterface<QTableMetaData> getTableMetaDataCustomizer()
   {
      return (this.tableMetaDataCustomizer);
   }



   /*******************************************************************************
    ** Fluent setter for tableMetaDataCustomizer
    *******************************************************************************/
   public MarketingQBitConfig withTableMetaDataCustomizer(MetaDataCustomizerInterface<QTableMetaData> tableMetaDataCustomizer)
   {
      this.tableMetaDataCustomizer = tableMetaDataCustomizer;
      return (this);
   }



   /*******************************************************************************
    ** Getter for inferBackendNames: set snake_case column names (the reference
    ** DDL in docs/ddl uses them).
    *******************************************************************************/
   public Boolean getInferBackendNames()
   {
      return (this.inferBackendNames);
   }



   /*******************************************************************************
    ** Fluent setter for inferBackendNames
    *******************************************************************************/
   public MarketingQBitConfig withInferBackendNames(Boolean inferBackendNames)
   {
      this.inferBackendNames = inferBackendNames;
      return (this);
   }



   /*******************************************************************************
    ** Getter for productSecurityKeyType: the optional tenant boundary. When set,
    ** every marketing table with a productId is locked on it (products on their
    ** id) with this key type, so users - and the agent, which always runs as the
    ** requesting user - see only the products they hold keys for.
    *******************************************************************************/
   public String getProductSecurityKeyType()
   {
      return (this.productSecurityKeyType);
   }



   /*******************************************************************************
    ** Fluent setter for productSecurityKeyType
    *******************************************************************************/
   public MarketingQBitConfig withProductSecurityKeyType(String productSecurityKeyType)
   {
      this.productSecurityKeyType = productSecurityKeyType;
      return (this);
   }



   /*******************************************************************************
    ** Getter for modelProviders
    *******************************************************************************/
   public Map<String, AssistantModelProvider> getModelProviders()
   {
      return (this.modelProviders);
   }



   /*******************************************************************************
    ** Getter for providerName
    *******************************************************************************/
   public String getProviderName()
   {
      return (this.providerName);
   }



   /*******************************************************************************
    ** Fluent setter for providerName
    *******************************************************************************/
   public MarketingQBitConfig withProviderName(String providerName)
   {
      this.providerName = providerName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for model
    *******************************************************************************/
   public String getModel()
   {
      return (this.model);
   }



   /*******************************************************************************
    ** Fluent setter for model
    *******************************************************************************/
   public MarketingQBitConfig withModel(String model)
   {
      this.model = model;
      return (this);
   }



   /*******************************************************************************
    ** Getter for effort
    *******************************************************************************/
   public String getEffort()
   {
      return (this.effort);
   }



   /*******************************************************************************
    ** Fluent setter for effort
    *******************************************************************************/
   public MarketingQBitConfig withEffort(String effort)
   {
      this.effort = effort;
      return (this);
   }



   /*******************************************************************************
    ** Getter for maxOutputTokens
    *******************************************************************************/
   public Integer getMaxOutputTokens()
   {
      return (this.maxOutputTokens);
   }



   /*******************************************************************************
    ** Fluent setter for maxOutputTokens
    *******************************************************************************/
   public MarketingQBitConfig withMaxOutputTokens(Integer maxOutputTokens)
   {
      this.maxOutputTokens = maxOutputTokens;
      return (this);
   }



   /*******************************************************************************
    ** Getter for maxToolCallsPerRun
    *******************************************************************************/
   public Integer getMaxToolCallsPerRun()
   {
      return (this.maxToolCallsPerRun);
   }



   /*******************************************************************************
    ** Fluent setter for maxToolCallsPerRun
    *******************************************************************************/
   public MarketingQBitConfig withMaxToolCallsPerRun(Integer maxToolCallsPerRun)
   {
      this.maxToolCallsPerRun = maxToolCallsPerRun;
      return (this);
   }



   /*******************************************************************************
    ** Getter for maxModelCallsPerRun
    *******************************************************************************/
   public Integer getMaxModelCallsPerRun()
   {
      return (this.maxModelCallsPerRun);
   }



   /*******************************************************************************
    ** Fluent setter for maxModelCallsPerRun
    *******************************************************************************/
   public MarketingQBitConfig withMaxModelCallsPerRun(Integer maxModelCallsPerRun)
   {
      this.maxModelCallsPerRun = maxModelCallsPerRun;
      return (this);
   }



   /*******************************************************************************
    ** Getter for maxRunSeconds
    *******************************************************************************/
   public Integer getMaxRunSeconds()
   {
      return (this.maxRunSeconds);
   }



   /*******************************************************************************
    ** Fluent setter for maxRunSeconds
    *******************************************************************************/
   public MarketingQBitConfig withMaxRunSeconds(Integer maxRunSeconds)
   {
      this.maxRunSeconds = maxRunSeconds;
      return (this);
   }



   /*******************************************************************************
    ** Getter for maxConcurrentAgentRuns
    *******************************************************************************/
   public Integer getMaxConcurrentAgentRuns()
   {
      return (this.maxConcurrentAgentRuns);
   }



   /*******************************************************************************
    ** Fluent setter for maxConcurrentAgentRuns
    *******************************************************************************/
   public MarketingQBitConfig withMaxConcurrentAgentRuns(Integer maxConcurrentAgentRuns)
   {
      this.maxConcurrentAgentRuns = maxConcurrentAgentRuns;
      return (this);
   }



   /*******************************************************************************
    ** Getter for maxModelCallsPerMinute
    *******************************************************************************/
   public Integer getMaxModelCallsPerMinute()
   {
      return (this.maxModelCallsPerMinute);
   }



   /*******************************************************************************
    ** Fluent setter for maxModelCallsPerMinute
    *******************************************************************************/
   public MarketingQBitConfig withMaxModelCallsPerMinute(Integer maxModelCallsPerMinute)
   {
      this.maxModelCallsPerMinute = maxModelCallsPerMinute;
      return (this);
   }



   /*******************************************************************************
    ** Getter for maxRowsToModel
    *******************************************************************************/
   public Integer getMaxRowsToModel()
   {
      return (this.maxRowsToModel);
   }



   /*******************************************************************************
    ** Fluent setter for maxRowsToModel
    *******************************************************************************/
   public MarketingQBitConfig withMaxRowsToModel(Integer maxRowsToModel)
   {
      this.maxRowsToModel = maxRowsToModel;
      return (this);
   }



   /*******************************************************************************
    ** Getter for pricing (USD per million tokens, by model id). Models without an
    ** entry get no cost (shown as unknown), never an invented price.
    *******************************************************************************/
   public Map<String, ModelPricing> getPricing()
   {
      return (this.pricing);
   }



   /*******************************************************************************
    ** Fluent setter for one model's pricing
    *******************************************************************************/
   public MarketingQBitConfig withPricing(String modelId, ModelPricing modelPricing)
   {
      this.pricing.put(modelId, modelPricing);
      return (this);
   }



   /*******************************************************************************
    ** Getter for registerKernelTools: also contribute the read tools to the QQQ
    ** AI tool kernel (AiToolRegistry, when the running QQQ has it), so the
    ** assistant panel can use them.
    *******************************************************************************/
   public Boolean getRegisterKernelTools()
   {
      return (this.registerKernelTools);
   }



   /*******************************************************************************
    ** Fluent setter for registerKernelTools
    *******************************************************************************/
   public MarketingQBitConfig withRegisterKernelTools(Boolean registerKernelTools)
   {
      this.registerKernelTools = registerKernelTools;
      return (this);
   }



   /*******************************************************************************
    ** Getter for transports
    *******************************************************************************/
   public List<MarketingTransport> getTransports()
   {
      return (this.transports);
   }



   /*******************************************************************************
    ** Getter for includeTestTransport
    *******************************************************************************/
   public Boolean getIncludeTestTransport()
   {
      return (this.includeTestTransport);
   }



   /*******************************************************************************
    ** Fluent setter for includeTestTransport
    *******************************************************************************/
   public MarketingQBitConfig withIncludeTestTransport(Boolean includeTestTransport)
   {
      this.includeTestTransport = includeTestTransport;
      return (this);
   }



   /*******************************************************************************
    ** Getter for schedulerName
    *******************************************************************************/
   public String getSchedulerName()
   {
      return (this.schedulerName);
   }



   /*******************************************************************************
    ** Fluent setter for schedulerName
    *******************************************************************************/
   public MarketingQBitConfig withSchedulerName(String schedulerName)
   {
      this.schedulerName = schedulerName;
      return (this);
   }



   /*******************************************************************************
    ** Getter for workerRepeatSeconds
    *******************************************************************************/
   public Integer getWorkerRepeatSeconds()
   {
      return (this.workerRepeatSeconds);
   }



   /*******************************************************************************
    ** Fluent setter for workerRepeatSeconds
    *******************************************************************************/
   public MarketingQBitConfig withWorkerRepeatSeconds(Integer workerRepeatSeconds)
   {
      this.workerRepeatSeconds = workerRepeatSeconds;
      return (this);
   }



   /*******************************************************************************
    ** Getter for workerBatchSize
    *******************************************************************************/
   public Integer getWorkerBatchSize()
   {
      return (this.workerBatchSize);
   }



   /*******************************************************************************
    ** Fluent setter for workerBatchSize
    *******************************************************************************/
   public MarketingQBitConfig withWorkerBatchSize(Integer workerBatchSize)
   {
      this.workerBatchSize = workerBatchSize;
      return (this);
   }



   /*******************************************************************************
    ** Getter for defaultMaxAttempts
    *******************************************************************************/
   public Integer getDefaultMaxAttempts()
   {
      return (this.defaultMaxAttempts);
   }



   /*******************************************************************************
    ** Fluent setter for defaultMaxAttempts
    *******************************************************************************/
   public MarketingQBitConfig withDefaultMaxAttempts(Integer defaultMaxAttempts)
   {
      this.defaultMaxAttempts = defaultMaxAttempts;
      return (this);
   }



   /*******************************************************************************
    ** Getter for retryBackoffSeconds
    *******************************************************************************/
   public Integer getRetryBackoffSeconds()
   {
      return (this.retryBackoffSeconds);
   }



   /*******************************************************************************
    ** Fluent setter for retryBackoffSeconds
    *******************************************************************************/
   public MarketingQBitConfig withRetryBackoffSeconds(Integer retryBackoffSeconds)
   {
      this.retryBackoffSeconds = retryBackoffSeconds;
      return (this);
   }



   /*******************************************************************************
    ** Getter for leaseSeconds
    *******************************************************************************/
   public Integer getLeaseSeconds()
   {
      return (this.leaseSeconds);
   }



   /*******************************************************************************
    ** Fluent setter for leaseSeconds
    *******************************************************************************/
   public MarketingQBitConfig withLeaseSeconds(Integer leaseSeconds)
   {
      this.leaseSeconds = leaseSeconds;
      return (this);
   }



   /*******************************************************************************
    ** Getter for maxRecipientsPerExecution
    *******************************************************************************/
   public Integer getMaxRecipientsPerExecution()
   {
      return (this.maxRecipientsPerExecution);
   }



   /*******************************************************************************
    ** Fluent setter for maxRecipientsPerExecution
    *******************************************************************************/
   public MarketingQBitConfig withMaxRecipientsPerExecution(Integer maxRecipientsPerExecution)
   {
      this.maxRecipientsPerExecution = maxRecipientsPerExecution;
      return (this);
   }



   /*******************************************************************************
    ** Getter for requireDifferentApprover
    *******************************************************************************/
   public Boolean getRequireDifferentApprover()
   {
      return (this.requireDifferentApprover);
   }



   /*******************************************************************************
    ** Fluent setter for requireDifferentApprover
    *******************************************************************************/
   public MarketingQBitConfig withRequireDifferentApprover(Boolean requireDifferentApprover)
   {
      this.requireDifferentApprover = requireDifferentApprover;
      return (this);
   }



   /*******************************************************************************
    ** Getter for aiDisclosureText
    *******************************************************************************/
   public String getAiDisclosureText()
   {
      return (this.aiDisclosureText);
   }



   /*******************************************************************************
    ** Fluent setter for aiDisclosureText
    *******************************************************************************/
   public MarketingQBitConfig withAiDisclosureText(String aiDisclosureText)
   {
      this.aiDisclosureText = aiDisclosureText;
      return (this);
   }



   /*******************************************************************************
    ** Getter for staleSourceDays
    *******************************************************************************/
   public Integer getStaleSourceDays()
   {
      return (this.staleSourceDays);
   }



   /*******************************************************************************
    ** Fluent setter for staleSourceDays
    *******************************************************************************/
   public MarketingQBitConfig withStaleSourceDays(Integer staleSourceDays)
   {
      this.staleSourceDays = staleSourceDays;
      return (this);
   }



   /*******************************************************************************
    ** Getter for crmGateway
    *******************************************************************************/
   public MarketingCrmGateway getCrmGateway()
   {
      return (this.crmGateway);
   }



   /*******************************************************************************
    ** Fluent setter for crmGateway
    *******************************************************************************/
   public MarketingQBitConfig withCrmGateway(MarketingCrmGateway crmGateway)
   {
      this.crmGateway = crmGateway;
      return (this);
   }

}
