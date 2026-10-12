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
 ** The kind of a finding.
 ** Possible-value source marketing_finding_type (integer ids).
 *******************************************************************************/
public enum FindingType implements PossibleValueEnum<Integer>
{
   UNSUPPORTED_CLAIM(1, "Unsupported claim"),
   STALE_SOURCE(2, "Stale source"),
   MISSING_EVIDENCE(3, "Missing evidence"),
   READINESS_GAP(4, "Launch-readiness gap"),
   SOURCE_DISCREPANCY(5, "Source discrepancy"),
   CHANNEL_RULE(6, "Channel rule"),
   MEASUREMENT_LIMITATION(7, "Measurement limitation"),
   RECOMMENDATION(8, "Recommendation"),
   PLAN(9, "Campaign plan");

   public static final String NAME = "marketing_finding_type";

   private final Integer id;
   private final String  label;



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   FindingType(Integer id, String label)
   {
      this.id = id;
      this.label = label;
   }



   /*******************************************************************************
    ** The value with this id, or null.
    *******************************************************************************/
   public static FindingType getById(Integer id)
   {
      if(id == null)
      {
         return (null);
      }

      for(FindingType value : FindingType.values())
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
