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

package com.kingsrook.qbits.marketing.transport;


import java.io.Serializable;
import java.util.List;
import java.util.Set;
import com.kingsrook.qbits.marketing.model.enums.ChannelType;


/*******************************************************************************
 ** A way to publish or send approved content: the seam between Marketing and
 ** the outside world.
 **
 ** Contract:
 ** - getStatus says honestly whether the transport can be used. A channel that
 **   names a transport is NOT a working connector unless the transport is
 **   registered and its status is OPERATIONAL or TEST_ONLY.
 ** - send is called only by the execution worker, after every execution-time
 **   check passed, never by the model. It must not throw for provider
 **   outcomes: it returns ACCEPTED, REJECTED (permanent), RETRYABLE_FAILURE
 **   (certainly not sent), AMBIGUOUS (may have been sent: the worker never
 **   retries it blindly) or DUPLICATE (the idempotency key was seen before).
 ** - The idempotency key is stable for an execution: a transport that supports
 **   it passes it to the provider so a repeated request is not sent twice.
 ** - lookup lets the worker reconcile an AMBIGUOUS outcome.
 **
 ** ACCEPTED means the provider accepted the request - not that the message was
 ** delivered, read or acted on.
 *******************************************************************************/
public interface MarketingTransport
{

   /*******************************************************************************
    ** The transport's unique name (channels refer to it).
    *******************************************************************************/
   String getName();

   /*******************************************************************************
    ** Whether the transport is configured and operational.
    *******************************************************************************/
   TransportStatus getStatus();

   /*******************************************************************************
    ** The channel types it can serve.
    *******************************************************************************/
   Set<ChannelType> getSupportedChannelTypes();

   /*******************************************************************************
    ** Publish or send one approved item. Never throws for provider outcomes.
    *******************************************************************************/
   TransportResult send(TransportRequest request);

   /*******************************************************************************
    ** What the provider knows about an idempotency key (for reconciliation).
    *******************************************************************************/
   TransportLookup lookup(String idempotencyKey);



   /*******************************************************************************
    ** Status of a transport.
    **
    ** @param state  NOT_CONFIGURED (missing settings or credentials), CONFIGURED
    **               (settings present, not verified by a live call), OPERATIONAL
    **               (verified) or TEST_ONLY (controlled, never leaves the system)
    ** @param detail a plain explanation, never containing a secret
    *******************************************************************************/
   record TransportStatus(State state, String detail) implements Serializable
   {
      /*******************************************************************************
       ** Transport states.
       *******************************************************************************/
      public enum State
      {
         NOT_CONFIGURED,
         CONFIGURED,
         OPERATIONAL,
         TEST_ONLY
      }



      /*******************************************************************************
       ** May the worker call send?
       *******************************************************************************/
      public boolean isUsable()
      {
         return (state == State.OPERATIONAL || state == State.TEST_ONLY);
      }
   }



   /*******************************************************************************
    ** A recipient: a stable key (e.g. crm-contact:42 or public) and, only for
    ** transports that need it, the resolved address. Addresses are resolved at
    ** execution time and never stored in Marketing tables or sent to a model.
    *******************************************************************************/
   record Recipient(String key, String address) implements Serializable
   {
   }



   /*******************************************************************************
    ** What to publish or send - exactly the approved revision.
    *******************************************************************************/
   record TransportRequest(String idempotencyKey, Integer executionId, Integer productId, String channelName, ChannelType channelType, String title,
                           String body, String destinationUrl, String callToAction, List<Recipient> recipients) implements Serializable
   {
   }



   /*******************************************************************************
    ** The outcome of a send.
    *******************************************************************************/
   record TransportResult(Outcome outcome, String receiptId, String providerStatus, String message) implements Serializable
   {
      /*******************************************************************************
       ** Send outcomes.
       *******************************************************************************/
      public enum Outcome
      {
         ACCEPTED,
         REJECTED,
         RETRYABLE_FAILURE,
         AMBIGUOUS,
         DUPLICATE
      }
   }



   /*******************************************************************************
    ** What a lookup found.
    *******************************************************************************/
   record TransportLookup(State state, String receiptId) implements Serializable
   {
      /*******************************************************************************
       ** Lookup states.
       *******************************************************************************/
      public enum State
      {
         FOUND_ACCEPTED,
         NOT_FOUND,
         UNSUPPORTED
      }
   }

}
