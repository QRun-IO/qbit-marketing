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
 ** The kind of a channel.
 ** Possible-value source marketing_channel_type (integer ids).
 *******************************************************************************/
public enum ChannelType implements PossibleValueEnum<Integer>
{
   TEST_SINK(1, "Controlled test sink"),
   EMAIL(2, "Email"),
   BLOG(3, "Blog / website"),
   SOCIAL(4, "Social network"),
   COMMUNITY(5, "Community / forum"),
   RELEASE_NOTES(6, "Release notes"),
   OTHER(7, "Other");

   public static final String NAME = "marketing_channel_type";

   private final Integer id;
   private final String  label;



   /*******************************************************************************
    ** Constructor
    *******************************************************************************/
   ChannelType(Integer id, String label)
   {
      this.id = id;
      this.label = label;
   }



   /*******************************************************************************
    ** The value with this id, or null.
    *******************************************************************************/
   public static ChannelType getById(Integer id)
   {
      if(id == null)
      {
         return (null);
      }

      for(ChannelType value : ChannelType.values())
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
