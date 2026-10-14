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


import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import com.kingsrook.qbits.marketing.agent.KernelToolRegistration;
import com.kingsrook.qbits.marketing.metadata.MarketingMetaDataBuilder;
import com.kingsrook.qbits.marketing.security.MarketingPermissions;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QAppChildMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.layout.QAppMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.processes.QProcessMetaData;
import com.kingsrook.qqq.backend.core.model.metadata.qbits.QBitConfigValidationException;
import com.kingsrook.qqq.backend.core.model.metadata.tables.Capability;
import com.kingsrook.qqq.backend.core.model.metadata.tables.QTableMetaData;
import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;


/*******************************************************************************
 ** The produced meta-data follows the owner rules.
 *******************************************************************************/
class MarketingQBitProducerTest extends BaseMarketingTest
{

   /*******************************************************************************
    ** Registered, valid, every table snake_case with the full qbit prefix, and
    ** every permission marketing.*.
    *******************************************************************************/
   @Test
   void testProducedAndNamed()
   {
      assertThat(qInstance.getQBits()).hasSize(1);
      assertThat(qInstance.getQBits().values().iterator().next().getArtifactId()).isEqualTo("qbit-marketing");

      Set<String> tables = qInstance.getTables().keySet();
      assertThat(tables).hasSize(18).allMatch(t -> t.matches("^marketing_[a-z][a-z0-9_]*$"));
      for(QTableMetaData table : qInstance.getTables().values())
      {
         assertThat(table.getPermissionRules().getPermissionBaseName()).startsWith("marketing");
         assertThat(table.getIsHidden()).as(table.getName() + " is a standard visible table").isNotEqualTo(true);
         assertThat(table.getFields().values()).allMatch(f -> f.getBackendName() == null || f.getBackendName().matches("^[a-z][a-z0-9_]*$"));
      }
      //////////////////////////////////////////////////////////////////////
      // the qbit's own processes (QQQ also generates bulk table processes //
      // such as marketing_product.bulkInsert, governed by table rules)   //
      //////////////////////////////////////////////////////////////////////
      List<QProcessMetaData> own = qInstance.getProcesses().values().stream().filter(p -> p.getSourceQBitName() != null).toList();
      assertThat(own).hasSizeGreaterThan(20);
      for(QProcessMetaData process : own)
      {
         assertThat(process.getName()).startsWith("marketing_");
         assertThat(process.getPermissionRules().getPermissionBaseName()).startsWith("marketing.");
      }
      assertThat(MarketingPermissions.ALL).allMatch(p -> p.startsWith("marketing."));
   }



   /*******************************************************************************
    ** The nav lists only the main tables and the settings; processes are never
    ** nav items and every process runs from a table.
    *******************************************************************************/
   @Test
   void testNavAndActions()
   {
      QAppMetaData app = qInstance.getApp(MarketingMetaDataBuilder.APP_NAME);
      List<String> children = app.getChildren().stream().map(QAppChildMetaData::getName).toList();
      assertThat(children).containsExactlyElementsOf(concat(MarketingMetaDataBuilder.MAIN_TABLE_NAMES, MarketingMetaDataBuilder.SETTINGS_TABLE_NAMES));
      assertThat(app.getChildren()).noneMatch(c -> c instanceof QProcessMetaData);
      assertThat(app.getSections()).allMatch(s -> s.getProcesses() == null || s.getProcesses().isEmpty());
      assertThat(app.getWidgets()).isNotEmpty();

      assertThat(qInstance.getProcesses().values()).allMatch(p -> p.getTableName() != null && qInstance.getTable(p.getTableName()) != null);
   }



   /*******************************************************************************
    ** System-written tables can't be inserted, edited or deleted from the UI.
    *******************************************************************************/
   @Test
   void testSystemWrittenTablesAreReadOnly()
   {
      for(String name : MarketingMetaDataBuilder.SYSTEM_WRITTEN_TABLE_NAMES)
      {
         QTableMetaData table = qInstance.getTable(name);
         assertThat(table.isCapabilityEnabled(qInstance.getBackendForTable(name), Capability.TABLE_INSERT)).as(name).isFalse();
         assertThat(table.isCapabilityEnabled(qInstance.getBackendForTable(name), Capability.TABLE_UPDATE)).as(name).isFalse();
      }
   }



   /*******************************************************************************
    ** Whether or not the kernel hook is available (absent on QQQ 4.1.0-RC.4), registering no tools registers nothing.
    *******************************************************************************/
   @Test
   void testKernelHookRegistersNothingWithoutTools()
   {
      assertThat(KernelToolRegistration.register(qInstance, List.of())).isZero();
   }



   /*******************************************************************************
    ** Disabled without config; invalid config fails.
    *******************************************************************************/
   @Test
   void testDisabledAndInvalid()
   {
      assertThat(new MarketingQBitProducer().isEnabled()).isFalse();
      assertThatThrownBy(() -> new MarketingQBitProducer().withQBitConfig(new MarketingQBitConfig()).produce(new QInstance())).isInstanceOf(QBitConfigValidationException.class);
      assertThatThrownBy(() -> new MarketingQBitProducer().withQBitConfig(new MarketingQBitConfig().withBackendName("nope")).produce(new QInstance()))
         .isInstanceOf(QBitConfigValidationException.class);
      assertThat(MarketingQBitConfig.TABLE_NAME_PREFIX).isEqualTo("marketing_");
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static List<String> concat(List<String> a, List<String> b)
   {
      return (java.util.stream.Stream.concat(a.stream(), b.stream()).collect(Collectors.toList()));
   }

}
