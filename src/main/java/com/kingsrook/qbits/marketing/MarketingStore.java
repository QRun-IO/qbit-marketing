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


import java.io.Serializable;
import java.util.List;
import java.util.stream.Collectors;
import com.kingsrook.qbits.marketing.security.MarketingInternalWrite;
import com.kingsrook.qqq.backend.core.actions.permissions.CorePermissionEnforcer;
import com.kingsrook.qqq.backend.core.actions.tables.CountAction;
import com.kingsrook.qqq.backend.core.actions.tables.GetAction;
import com.kingsrook.qqq.backend.core.actions.tables.InsertAction;
import com.kingsrook.qqq.backend.core.actions.tables.QueryAction;
import com.kingsrook.qqq.backend.core.actions.tables.UpdateAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.model.actions.tables.count.CountInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.get.GetInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertOutput;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QueryInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateOutput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;
import com.kingsrook.qqq.backend.core.model.session.QSession;
import com.kingsrook.qqq.backend.core.utils.CollectionUtils;


/*******************************************************************************
 ** Small helpers over the standard QQQ table actions, used by the Marketing
 ** services. Every read and write goes through the core actions - so record
 ** security locks, customizers, audits, record-change listeners (the ESB) and
 ** search indexing all apply - never raw SQL.
 **
 ** The "internal" writes open MarketingInternalWrite, the only way to write the
 ** system-written tables.
 *******************************************************************************/
public final class MarketingStore
{

   /*******************************************************************************
    ** Utility class - no instances.
    *******************************************************************************/
   private MarketingStore()
   {
   }



   /*******************************************************************************
    ** One record by primary key, or null.
    *******************************************************************************/
   public static QRecord get(String tableName, Serializable id) throws QException
   {
      if(id == null)
      {
         return (null);
      }
      return (new GetAction().executeForRecord(new GetInput(tableName).withPrimaryKey(id)));
   }



   /*******************************************************************************
    ** One record by primary key, or a user-facing error.
    *******************************************************************************/
   public static QRecord require(String tableName, Serializable id) throws QException
   {
      QRecord record = get(tableName, id);
      if(record == null)
      {
         throw (new QUserFacingException("Record " + id + " was not found in " + tableName + "."));
      }
      return (record);
   }



   /*******************************************************************************
    ** Records matching a filter.
    *******************************************************************************/
   public static List<QRecord> query(String tableName, QQueryFilter filter) throws QException
   {
      return (new QueryAction().execute(new QueryInput(tableName).withFilter(filter)).getRecords());
   }



   /*******************************************************************************
    ** Records where a field equals a value.
    *******************************************************************************/
   public static List<QRecord> queryBy(String tableName, String fieldName, Serializable value) throws QException
   {
      return (query(tableName, new QQueryFilter(new QFilterCriteria(fieldName, QCriteriaOperator.EQUALS, value))));
   }



   /*******************************************************************************
    ** Count records matching a filter.
    *******************************************************************************/
   public static int count(String tableName, QQueryFilter filter) throws QException
   {
      Integer count = new CountAction().execute(new CountInput(tableName).withFilter(filter)).getCount();
      return (count == null ? 0 : count);
   }



   /*******************************************************************************
    ** Insert an entity; returns the stored record. Errors become exceptions.
    *******************************************************************************/
   public static QRecord insert(QRecordEntity entity) throws QException
   {
      return (insert(tableNameOf(entity), entity.toQRecord()));
   }



   /*******************************************************************************
    ** Insert a record; returns the stored record. Errors become exceptions.
    *******************************************************************************/
   public static QRecord insert(String tableName, QRecord record) throws QException
   {
      InsertOutput output = new InsertAction().execute(new InsertInput(tableName).withRecord(record));
      QRecord      stored = output.getRecords().get(0);
      if(CollectionUtils.nullSafeHasContents(stored.getErrors()))
      {
         throw (new QUserFacingException(stored.getErrors().stream().map(e -> e.getMessage()).collect(Collectors.joining("; "))));
      }
      return (stored);
   }



   /*******************************************************************************
    ** Update a record (primary key plus changed fields). Errors become
    ** exceptions.
    *******************************************************************************/
   public static void update(String tableName, QRecord record) throws QException
   {
      UpdateOutput output = new UpdateAction().execute(new UpdateInput(tableName).withRecord(record));
      QRecord      stored = output.getRecords().get(0);
      if(CollectionUtils.nullSafeHasContents(stored.getErrors()))
      {
         throw (new QUserFacingException(stored.getErrors().stream().map(e -> e.getMessage()).collect(Collectors.joining("; "))));
      }
   }



   /*******************************************************************************
    ** Insert into a system-written table.
    *******************************************************************************/
   public static QRecord insertInternal(QRecordEntity entity) throws QException
   {
      return (MarketingInternalWrite.run(() -> insert(entity)));
   }



   /*******************************************************************************
    ** Update a system-written table.
    *******************************************************************************/
   public static void updateInternal(String tableName, QRecord record) throws QException
   {
      MarketingInternalWrite.run(() ->
      {
         update(tableName, record);
         return (null);
      });
   }



   /*******************************************************************************
    ** Run a service body as framework-internal work: after the service checked
    ** the permission its step needs, its own reads and writes are trusted the
    ** same way QQQ trusts calls nested inside a core action (an authorized
    ** scope). Record security locks still apply, and anything that requests
    ** PermissionEnforcement.ENFORCE (the agent's tools) is still checked.
    *******************************************************************************/
   public static <T> T trusted(MarketingInternalWrite.Body<T> body) throws QException
   {
      try(CorePermissionEnforcer.AuthorizedScope ignored = CorePermissionEnforcer.openAuthorizedScope())
      {
         return (body.run());
      }
   }



   /*******************************************************************************
    ** The current user's id reference, or "system".
    *******************************************************************************/
   public static String currentUserId()
   {
      QSession session = QContext.getQSession();
      if(session == null || session.getUser() == null || session.getUser().getIdReference() == null)
      {
         return ("system");
      }
      return (session.getUser().getIdReference());
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static String tableNameOf(QRecordEntity entity) throws QException
   {
      try
      {
         return ((String) entity.getClass().getField("TABLE_NAME").get(null));
      }
      catch(ReflectiveOperationException e)
      {
         throw (new QException("Entity " + entity.getClass().getSimpleName() + " has no TABLE_NAME", e));
      }
   }

}
