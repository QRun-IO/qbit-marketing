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
 ** The kind of an evidence source.
 ** Possible-value source marketing_source_type (integer ids).
 *******************************************************************************/
public enum SourceType implements PossibleValueEnum<Integer>
{
   WEBSITE(1, "Website"),
   DOCUMENTATION(2, "Documentation"),
   REPOSITORY(3, "Source repository"),
   RELEASE(4, "Release / changelog"),
   PACKAGE_REGISTRY(5, "Package registry"),
   STORE_LISTING(6, "Store listing"),
   CHANNEL_RULES(7, "Channel / community rules"),
   OTHER(8, "Other");

   public static final String NAME = "marketing_source_type";

   private final Integer id;
   private final String  label;



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   SourceType(Integer id, String label)
   {
      this.id = id;
      this.label = label;
   }



   /*******************************************************************************
    ** The value with this id, or null.
    *******************************************************************************/
   public static SourceType getById(Integer id)
   {
      if(id == null)
      {
         return (null);
      }

      for(SourceType value : SourceType.values())
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
