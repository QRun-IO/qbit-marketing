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


import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import com.kingsrook.qbits.marketing.MarketingStore;
import com.kingsrook.qbits.marketing.model.MarketingTestDelivery;
import com.kingsrook.qbits.marketing.model.enums.ChannelType;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QCriteriaOperator;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QFilterCriteria;
import com.kingsrook.qqq.backend.core.model.actions.tables.query.QQueryFilter;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** The controlled test transport: a complete transport that never leaves the
 ** application. It "accepts" a request by storing the exact payload, per
 ** recipient, in marketing_test_delivery (a visible, read-only table), and it
 ** honors idempotency keys like a careful provider would: a repeated key is
 ** answered with the original receipt and counted, not stored again.
 **
 ** For tests and demonstrations it can be told what to answer next (accept,
 ** reject, fail before sending, or be ambiguous - including "ambiguous, but it
 ** actually accepted", which reconciliation must discover).
 **
 ** Executions on this transport are marked as test data, and its outcomes are
 ** reported separately from real results.
 *******************************************************************************/
public class ControlledTestTransport implements MarketingTransport
{
   private static final QLogger LOG = QLogger.getLogger(ControlledTestTransport.class);

   public static final String NAME = "controlled_test";

   /*******************************************************************************
    ** What the next send does.
    *******************************************************************************/
   public enum Behavior
   {
      ACCEPT,
      REJECT,
      FAIL_BEFORE_SENDING,
      AMBIGUOUS_NOT_STORED,
      AMBIGUOUS_BUT_STORED
   }

   private final Deque<Behavior> script = new ArrayDeque<>();



   /*******************************************************************************
    ** Queue behaviors for the next sends (then it accepts again).
    *******************************************************************************/
   public synchronized ControlledTestTransport thenBehave(Behavior... behaviors)
   {
      script.addAll(List.of(behaviors));
      return (this);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private synchronized Behavior nextBehavior()
   {
      Behavior behavior = script.poll();
      return (behavior == null ? Behavior.ACCEPT : behavior);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String getName()
   {
      return (NAME);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public TransportStatus getStatus()
   {
      return (new TransportStatus(TransportStatus.State.TEST_ONLY, "Stores the exact payload in Marketing Test Deliveries; nothing leaves the application."));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public Set<ChannelType> getSupportedChannelTypes()
   {
      return (EnumSet.allOf(ChannelType.class));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public TransportResult send(TransportRequest request)
   {
      Behavior behavior = nextBehavior();
      try
      {
         switch(behavior)
         {
            case REJECT:
               return (new TransportResult(TransportResult.Outcome.REJECTED, null, "rejected", "The test transport was told to reject this request."));
            case FAIL_BEFORE_SENDING:
               return (new TransportResult(TransportResult.Outcome.RETRYABLE_FAILURE, null, "unavailable", "The test transport was told to fail before sending (nothing was stored)."));
            case AMBIGUOUS_NOT_STORED:
               return (new TransportResult(TransportResult.Outcome.AMBIGUOUS, null, "timeout", "The test transport was told to time out without storing anything."));
            default:
               break;
         }

         String  receiptId  = null;
         boolean duplicate  = true;
         for(Recipient recipient : request.recipients())
         {
            QRecord existing = find(request.idempotencyKey(), recipient.key());
            if(existing != null)
            {
               receiptId = existing.getValueString("receiptId");
               Integer count = existing.getValueInteger("duplicateCount");
               MarketingStore.updateInternal(MarketingTestDelivery.TABLE_NAME, new QRecord().withValue("id", existing.getValue("id")).withValue("duplicateCount", (count == null ? 0 : count) + 1));
               continue;
            }

            duplicate = false;
            if(receiptId == null)
            {
               receiptId = "test-" + UUID.randomUUID();
            }
            MarketingStore.insertInternal(new MarketingTestDelivery()
               .withProductId(request.productId())
               .withExecutionId(request.executionId())
               .withIdempotencyKey(request.idempotencyKey())
               .withRecipientKey(recipient.key())
               .withChannelName(request.channelName())
               .withTitle(request.title())
               .withBody(request.body())
               .withDestinationUrl(request.destinationUrl())
               .withReceiptId(receiptId)
               .withAcceptedAt(Instant.now())
               .withDuplicateCount(0));
         }

         if(behavior == Behavior.AMBIGUOUS_BUT_STORED)
         {
            return (new TransportResult(TransportResult.Outcome.AMBIGUOUS, null, "timeout", "The test transport stored the request but was told to report a timeout."));
         }
         if(duplicate)
         {
            return (new TransportResult(TransportResult.Outcome.DUPLICATE, receiptId, "duplicate", "Already accepted under this idempotency key; nothing was stored again."));
         }
         return (new TransportResult(TransportResult.Outcome.ACCEPTED, receiptId, "accepted", "Stored by the controlled test transport (not delivered anywhere)."));
      }
      catch(QException e)
      {
         LOG.warn("Controlled test transport could not store a delivery", e, logPair("executionId", request.executionId()));
         return (new TransportResult(TransportResult.Outcome.AMBIGUOUS, null, "error", "The test transport failed while storing: the outcome is unknown."));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public TransportLookup lookup(String idempotencyKey)
   {
      try
      {
         List<QRecord> found = MarketingStore.queryBy(MarketingTestDelivery.TABLE_NAME, "idempotencyKey", idempotencyKey);
         if(found.isEmpty())
         {
            return (new TransportLookup(TransportLookup.State.NOT_FOUND, null));
         }
         return (new TransportLookup(TransportLookup.State.FOUND_ACCEPTED, found.get(0).getValueString("receiptId")));
      }
      catch(QException e)
      {
         LOG.warn("Controlled test transport lookup failed", e);
         return (new TransportLookup(TransportLookup.State.UNSUPPORTED, null));
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   private static QRecord find(String idempotencyKey, String recipientKey) throws QException
   {
      List<QRecord> found = MarketingStore.query(MarketingTestDelivery.TABLE_NAME, new QQueryFilter(
         new QFilterCriteria("idempotencyKey", QCriteriaOperator.EQUALS, idempotencyKey),
         new QFilterCriteria("recipientKey", QCriteriaOperator.EQUALS, recipientKey)));
      return (found.isEmpty() ? null : found.get(0));
   }

}
