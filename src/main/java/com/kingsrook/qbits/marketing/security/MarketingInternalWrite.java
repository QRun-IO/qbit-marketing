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

package com.kingsrook.qbits.marketing.security;


import java.util.List;
import java.util.Optional;
import com.kingsrook.qqq.backend.core.actions.customizers.TableCustomizerInterface;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.exceptions.QUserFacingException;
import com.kingsrook.qqq.backend.core.model.actions.tables.delete.DeleteInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.insert.InsertInput;
import com.kingsrook.qqq.backend.core.model.actions.tables.update.UpdateInput;
import com.kingsrook.qqq.backend.core.model.data.QRecord;


/*******************************************************************************
 ** The write guard of the system-written tables (revisions, approvals,
 ** executions, attempts, test deliveries, agent runs and steps).
 **
 ** Those rows are history and authority: an approval is what lets content be
 ** published. So nobody - not the generic table UI or API, not a model's tool,
 ** not a script - may insert, update or delete them directly. Only the
 ** Marketing services write them, inside run(...), after their own checks
 ** (permission, state, hash). The tables also have their insert / edit /
 ** delete capabilities turned off, so the UI offers no such buttons.
 *******************************************************************************/
public final class MarketingInternalWrite implements TableCustomizerInterface
{
   private static final ThreadLocal<Integer> DEPTH = ThreadLocal.withInitial(() -> 0);



   /*******************************************************************************
    ** Code that writes.
    *******************************************************************************/
   @FunctionalInterface
   public interface Body<T>
   {
      /*******************************************************************************
       **
       *******************************************************************************/
      T run() throws QException;
   }



   /*******************************************************************************
    ** Run code that may write the guarded tables (re-entrant).
    *******************************************************************************/
   public static <T> T run(Body<T> body) throws QException
   {
      DEPTH.set(DEPTH.get() + 1);
      try
      {
         return (body.run());
      }
      finally
      {
         int depth = DEPTH.get() - 1;
         if(depth <= 0)
         {
            DEPTH.remove();
         }
         else
         {
            DEPTH.set(depth);
         }
      }
   }



   /*******************************************************************************
    ** Is the current thread inside run(...)?
    *******************************************************************************/
   public static boolean isActive()
   {
      return (DEPTH.get() > 0);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static void requireActive(String tableName) throws QException
   {
      if(!isActive())
      {
         throw (new QUserFacingException("Records in " + tableName + " are written only by Marketing's own actions (approve, schedule, run, agent runs); they cannot be changed directly."));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public List<QRecord> preInsert(InsertInput insertInput, List<QRecord> records, boolean isPreview) throws QException
   {
      requireActive(insertInput.getTableName());
      return (records);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public List<QRecord> preUpdate(UpdateInput updateInput, List<QRecord> records, boolean isPreview, Optional<List<QRecord>> oldRecordList) throws QException
   {
      requireActive(updateInput.getTableName());
      return (records);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public List<QRecord> preDelete(DeleteInput deleteInput, List<QRecord> records, boolean isPreview) throws QException
   {
      requireActive(deleteInput.getTableName());
      return (records);
   }

}
