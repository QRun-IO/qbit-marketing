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


import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import com.kingsrook.qbits.marketing.agent.KernelToolRegistration;
import com.kingsrook.qbits.marketing.agent.tools.MarketingTools;
import com.kingsrook.qbits.marketing.metadata.MarketingHelp;
import com.kingsrook.qbits.marketing.metadata.MarketingMetaDataBuilder;
import com.kingsrook.qbits.marketing.processes.MarketingActions;
import com.kingsrook.qbits.marketing.processes.MarketingTableProcesses;
import com.kingsrook.qbits.marketing.widgets.MarketingWidgets;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.MetaDataProducerMultiOutput;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.dashboard.QWidgetMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.joins.QJoinMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.QPossibleValueSource;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.qbits.QBitConfigValidationException;
import com.kingsrook.qqq.backend.core.model.metadata.qbits.QBitMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.qbits.QBitMetaDataProducer;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Produces the Marketing QBit into a host application:
 **
 ** - 19 standard tables (marketing_*), their joins, child-list widgets and
 **   possible-value sources, and the optional product security key type;
 ** - the record actions (streamed ETL) and two table actions;
 ** - the Marketing app, whose home is the Marketing dashboard;
 ** - the runtime (transports, model providers, CRM gateway, limits);
 ** - the marketing read tools in the QQQ AI tool kernel, when QQQ has the
 **   registration hook.
 **
 ** Usage:
 ** <pre>
 ** new MarketingQBitProducer().withQBitConfig(new MarketingQBitConfig()
 **    .withBackendName("rdbms")
 **    .withCrmGateway(new QbitCrmGateway())
 **    .withSchedulerName("scheduler"))
 **    .produce(qInstance);
 ** </pre>
 *******************************************************************************/
public class MarketingQBitProducer implements QBitMetaDataProducer<MarketingQBitConfig>
{
   private static final QLogger LOG = QLogger.getLogger(MarketingQBitProducer.class);

   public static final String GROUP_ID    = "com.kingsrook.qbits";
   public static final String ARTIFACT_ID = "qbit-marketing";
   public static final String VERSION     = loadVersion();

   private MarketingQBitConfig qBitConfig;
   private String              namespace;



   /*******************************************************************************
    ** Fluent setter for the config.
    *******************************************************************************/
   public MarketingQBitProducer withQBitConfig(MarketingQBitConfig config)
   {
      this.qBitConfig = config;
      return (this);
   }



   /*******************************************************************************
    ** Getter for the config.
    *******************************************************************************/
   @Override
   public MarketingQBitConfig getQBitConfig()
   {
      return (qBitConfig);
   }



   /*******************************************************************************
    ** Fluent setter for the namespace.
    *******************************************************************************/
   public MarketingQBitProducer withNamespace(String namespace)
   {
      this.namespace = namespace;
      return (this);
   }



   /*******************************************************************************
    ** Getter for the namespace.
    *******************************************************************************/
   @Override
   public String getNamespace()
   {
      return (namespace);
   }



   /*******************************************************************************
    ** The qbit's identity.
    *******************************************************************************/
   @Override
   public QBitMetaData getQBitMetaData()
   {
      return (new QBitMetaData().withGroupId(GROUP_ID).withArtifactId(ARTIFACT_ID).withVersion(VERSION).withNamespace(namespace).withConfig(qBitConfig));
   }



   /*******************************************************************************
    ** Producers found by package scanning have no config: skip them.
    *******************************************************************************/
   @Override
   public boolean isEnabled()
   {
      return (qBitConfig != null);
   }



   /*******************************************************************************
    ** Produce everything into the instance.
    *******************************************************************************/
   @Override
   public MetaDataProducerMultiOutput produce(QInstance qInstance) throws QException
   {
      if(qBitConfig == null)
      {
         throw (new QException("Marketing QBit configuration validation failed: no config was supplied (call withQBitConfig)"));
      }
      List<String> errors = new ArrayList<>();
      qBitConfig.validate(qInstance, errors);
      if(!errors.isEmpty())
      {
         throw (new QBitConfigValidationException(qBitConfig, errors));
      }

      QBitMetaData qBitMetaData = getQBitMetaData();
      qInstance.addQBit(qBitMetaData);

      MarketingMetaDataBuilder builder = new MarketingMetaDataBuilder(qBitConfig);
      if(StringUtils.hasContent(qBitConfig.getProductSecurityKeyType()) && qInstance.getSecurityKeyType(qBitConfig.getProductSecurityKeyType()) == null)
      {
         qInstance.addSecurityKeyType(builder.buildProductSecurityKeyType());
      }

      for(QTableMetaData table : builder.buildTables())
      {
         QTableMetaData customized = qBitConfig.getTableMetaDataCustomizer() == null ? table : qBitConfig.getTableMetaDataCustomizer().customizeMetaData(qInstance, table);
         customized.setSourceQBitName(qBitMetaData.getName());
         qInstance.addTable(customized);
      }
      for(QPossibleValueSource possibleValueSource : builder.buildPossibleValueSources())
      {
         qInstance.addPossibleValueSource(possibleValueSource);
      }
      for(QJoinMetaData join : builder.getJoins())
      {
         qInstance.addJoin(join);
      }
      for(QWidgetMetaData widget : builder.getChildListWidgets())
      {
         qInstance.addWidget(widget);
      }
      for(QWidgetMetaData widget : MarketingWidgets.buildWidgets())
      {
         qInstance.addWidget(widget);
      }

      List<QProcessMetaData> processes = new ArrayList<>(MarketingActions.buildProcesses());
      processes.addAll(MarketingTableProcesses.buildProcesses(qBitConfig));
      for(QProcessMetaData process : processes)
      {
         process.setSourceQBitName(qBitMetaData.getName());
         qInstance.addProcess(process);
      }

      qInstance.addApp(builder.buildApp(MarketingWidgets.HOME_WIDGETS));
      MarketingHelp.apply(qInstance);

      ////////////////////////////////////
      // runtime and kernel tools       //
      ////////////////////////////////////
      new MarketingRuntime(qBitConfig).addSelfToInstance(qInstance);
      int kernelTools = Boolean.TRUE.equals(qBitConfig.getRegisterKernelTools()) ? KernelToolRegistration.register(qInstance, MarketingTools.readTools()) : 0;

      LOG.info("Marketing QBit produced", logPair("version", VERSION), logPair("backendName", qBitConfig.getBackendName()),
         logPair("modelProviders", qBitConfig.getModelProviders().keySet()), logPair("transports", MarketingRuntime.of(qInstance).getTransports().keySet()),
         logPair("crmGateway", qBitConfig.getCrmGateway() == null ? null : qBitConfig.getCrmGateway().getName()), logPair("kernelTools", kernelTools));
      return (new MetaDataProducerMultiOutput());
   }



   /*******************************************************************************
    ** The version from qbit-marketing.properties (filtered by Maven).
    *******************************************************************************/
   static String loadVersion()
   {
      try(InputStream inputStream = MarketingQBitProducer.class.getClassLoader().getResourceAsStream("qbit-marketing.properties"))
      {
         if(inputStream != null)
         {
            Properties properties = new Properties();
            properties.load(inputStream);
            String version = properties.getProperty("version");
            if(StringUtils.hasContent(version) && !version.contains("${"))
            {
               return (version);
            }
         }
      }
      catch(Exception e)
      {
         LOG.debug("Could not read qbit-marketing.properties", e);
      }
      return ("unknown");
   }

}
