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
 ** Where a content item is between drafting and execution.
 ** Possible-value source marketing_content_status (integer ids).
 *******************************************************************************/
public enum ContentStatus implements PossibleValueEnum<Integer>
{
   DRAFT(1, "Draft"),
   IN_REVIEW(2, "In review"),
   APPROVED(3, "Approved"),
   SCHEDULED(4, "Scheduled"),
   PAUSED(5, "Paused"),
   ACCEPTED(6, "Accepted by provider"),
   FAILED(7, "Failed"),
   CANCELLED(8, "Cancelled"),
   NEEDS_RECONCILIATION(9, "Ambiguous - needs reconciliation");

   public static final String NAME = "marketing_content_status";

   private final Integer id;
   private final String  label;



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   ContentStatus(Integer id, String label)
   {
      this.id = id;
      this.label = label;
   }



   /*******************************************************************************
    ** The value with this id, or null.
    *******************************************************************************/
   public static ContentStatus getById(Integer id)
   {
      if(id == null)
      {
         return (null);
      }

      for(ContentStatus value : ContentStatus.values())
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
