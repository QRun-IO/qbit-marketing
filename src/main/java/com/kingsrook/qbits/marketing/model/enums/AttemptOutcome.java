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
 ** The outcome of one execution attempt.
 ** Possible-value source marketing_attempt_outcome (integer ids).
 *******************************************************************************/
public enum AttemptOutcome implements PossibleValueEnum<Integer>
{
   ACCEPTED(1, "Accepted by provider"),
   REJECTED(2, "Rejected by provider"),
   RETRYABLE_FAILURE(3, "Failed before sending (retryable)"),
   AMBIGUOUS(4, "Ambiguous (may have been sent)"),
   BLOCKED(5, "Blocked by checks"),
   DUPLICATE(6, "Duplicate - not sent again");

   public static final String NAME = "marketing_attempt_outcome";

   private final Integer id;
   private final String  label;



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   AttemptOutcome(Integer id, String label)
   {
      this.id = id;
      this.label = label;
   }



   /*******************************************************************************
    ** The value with this id, or null.
    *******************************************************************************/
   public static AttemptOutcome getById(Integer id)
   {
      if(id == null)
      {
         return (null);
      }

      for(AttemptOutcome value : AttemptOutcome.values())
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
