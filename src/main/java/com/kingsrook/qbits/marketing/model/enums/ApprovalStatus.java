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

package com.kingsrook.qbits.marketing.model.enums;


import com.kingsrook.qqq.backend.core.model.metadata.possiblevalues.PossibleValueEnum;


/*******************************************************************************
 ** The state of an approval.
 ** Possible-value source marketing_approval_status (integer ids).
 *******************************************************************************/
public enum ApprovalStatus implements PossibleValueEnum<Integer>
{
   ACTIVE(1, "Active"),
   INVALIDATED(2, "Invalidated by an edit"),
   REVOKED(3, "Revoked"),
   CONSUMED(4, "Used by an execution");

   public static final String NAME = "marketing_approval_status";

   private final Integer id;
   private final String  label;



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   ApprovalStatus(Integer id, String label)
   {
      this.id = id;
      this.label = label;
   }



   /*******************************************************************************
    ** The value with this id, or null.
    *******************************************************************************/
   public static ApprovalStatus getById(Integer id)
   {
      if(id == null)
      {
         return (null);
      }

      for(ApprovalStatus value : ApprovalStatus.values())
      {
         if(value.id.equals(id))
         {
            return (value);
         }
      }
      return (null);
   }



   /*******************************************************************************
    ** Getter for id
    *******************************************************************************/
   public Integer getId()
   {
      return (id);
   }



   /*******************************************************************************
    ** Getter for label
    *******************************************************************************/
   public String getLabel()
   {
      return (label);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public Integer getPossibleValueId()
   {
      return (id);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String getPossibleValueLabel()
   {
      return (label);
   }
}
