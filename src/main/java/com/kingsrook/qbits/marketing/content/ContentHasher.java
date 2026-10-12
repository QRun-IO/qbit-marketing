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

package com.kingsrook.qbits.marketing.content;


import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.List;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.utils.ValueUtils;


/*******************************************************************************
 ** The fingerprint of what a person approves: the exact text, the destination,
 ** the recipient scope, the schedule and the channel of a content item.
 **
 ** An approval stores this hash. Any change to one of these fields changes the
 ** hash, which invalidates the approval (MarketingContentCustomizer) and is
 ** re-checked again when the execution runs.
 *******************************************************************************/
public final class ContentHasher
{
   /** The fields an approval covers. */
   public static final List<String> APPROVED_FIELDS = List.of("title", "body", "destinationUrl", "callToAction", "recipientScope", "scheduledFor", "channelId");

   private static final char SEPARATOR = '\u001f';



   /*******************************************************************************
    ** Utility class - no instances.
    *******************************************************************************/
   private ContentHasher()
   {
   }



   /*******************************************************************************
    ** SHA-256 (hex) of the approved fields of a content record.
    *******************************************************************************/
   public static String hash(QRecord content)
   {
      StringBuilder canonical = new StringBuilder("marketing-content-v1");
      for(String field : APPROVED_FIELDS)
      {
         Object value = content.getValue(field);
         canonical.append(SEPARATOR).append(field).append('=');
         String string = normalize(field, value);
         if(string != null)
         {
            canonical.append(string.length()).append(':').append(string);
         }
      }

      try
      {
         MessageDigest digest = MessageDigest.getInstance("SHA-256");
         return (HexFormat.of().formatHex(digest.digest(canonical.toString().getBytes(StandardCharsets.UTF_8))));
      }
      catch(NoSuchAlgorithmException e)
      {
         throw (new IllegalStateException("SHA-256 is not available", e));
      }
   }



   /*******************************************************************************
    ** Values may still be strings when customizers run (e.g. from an API
    ** request), so normalize types: the same content always hashes the same.
    ** Blank strings count as absent. Schedules count to the second (databases
    ** store different sub-second precision).
    *******************************************************************************/
   static String normalize(String field, Object value)
   {
      if(value == null || (value instanceof String s && s.isBlank()))
      {
         return (null);
      }
      if("scheduledFor".equals(field))
      {
         return (ValueUtils.getValueAsInstant(value).truncatedTo(java.time.temporal.ChronoUnit.SECONDS).toString());
      }
      if("channelId".equals(field))
      {
         return (String.valueOf(ValueUtils.getValueAsInteger(value)));
      }
      return (ValueUtils.getValueAsString(value));
   }

}
