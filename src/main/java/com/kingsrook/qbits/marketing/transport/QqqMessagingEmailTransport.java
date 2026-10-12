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


import java.util.EnumSet;
import java.util.Set;
import com.kingsrook.qbits.marketing.model.enums.ChannelType;
import com.kingsrook.qqq.backend.core.actions.messaging.SendMessageAction;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.actions.messaging.Content;
import com.kingsrook.qqq.backend.core.model.actions.messaging.Party;
import com.kingsrook.qqq.backend.core.model.actions.messaging.SendMessageInput;
import com.kingsrook.qqq.backend.core.model.actions.messaging.email.EmailContentRole;
import com.kingsrook.qqq.backend.core.model.actions.messaging.email.EmailPartyRole;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Email through the application's own QQQ messaging provider (QQQ's existing
 ** convention: a QMessagingProviderMetaData - SMTP or SES - registered in the
 ** instance, used through SendMessageAction).
 **
 ** It is NOT_CONFIGURED until the instance has a messaging provider with the
 ** configured name and a from-address is set; it then reports CONFIGURED (not
 ** OPERATIONAL): nothing verifies the provider without sending a real
 ** message, and the worker only uses usable (OPERATIONAL / TEST_ONLY)
 ** transports. A host that has verified its provider says so with
 ** withVerifiedOperational(true).
 **
 ** One message per recipient (no bulk lists in one envelope). The worker
 ** resolves recipient addresses through the CRM gateway at execution time,
 ** after consent and suppression checks; addresses are never stored here.
 **
 ** Outcomes: a successful SendMessageAction is ACCEPTED (handed to the provider
 ** - not delivered). An exception is AMBIGUOUS: SMTP/SES failures can happen
 ** after the provider took the message, and QQQ messaging has no idempotency
 ** or lookup, so the worker never retries it blindly and a person reconciles.
 *******************************************************************************/
public class QqqMessagingEmailTransport implements MarketingTransport
{
   private static final QLogger LOG = QLogger.getLogger(QqqMessagingEmailTransport.class);

   public static final String DEFAULT_NAME = "qqq_email";

   private String  name                 = DEFAULT_NAME;
   private String  messagingProviderName;
   private String  fromAddress;
   private Boolean verifiedOperational  = false;



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String getName()
   {
      return (name);
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public TransportStatus getStatus()
   {
      if(!StringUtils.hasContent(messagingProviderName) || !StringUtils.hasContent(fromAddress))
      {
         return (new TransportStatus(TransportStatus.State.NOT_CONFIGURED, "No messaging provider name or from-address is configured."));
      }

      QInstance qInstance = QContext.getQInstance();
      if(qInstance == null || qInstance.getMessagingProvider(messagingProviderName) == null)
      {
         return (new TransportStatus(TransportStatus.State.NOT_CONFIGURED, "The application has no QQQ messaging provider named [" + messagingProviderName + "]."));
      }

      if(Boolean.TRUE.equals(verifiedOperational))
      {
         return (new TransportStatus(TransportStatus.State.OPERATIONAL, "Uses QQQ messaging provider [" + messagingProviderName + "], marked verified by the host."));
      }
      return (new TransportStatus(TransportStatus.State.CONFIGURED, "QQQ messaging provider [" + messagingProviderName + "] is registered but not verified; executions stay blocked until the host marks it operational."));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public Set<ChannelType> getSupportedChannelTypes()
   {
      return (EnumSet.of(ChannelType.EMAIL));
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public TransportResult send(TransportRequest request)
   {
      if(!getStatus().isUsable())
      {
         return (new TransportResult(TransportResult.Outcome.RETRYABLE_FAILURE, null, "not_configured", getStatus().detail()));
      }

      int sent = 0;
      for(Recipient recipient : request.recipients())
      {
         if(!StringUtils.hasContent(recipient.address()))
         {
            return (new TransportResult(sent == 0 ? TransportResult.Outcome.REJECTED : TransportResult.Outcome.AMBIGUOUS, null, "no_address",
               "A recipient has no address" + (sent == 0 ? "." : " after " + sent + " message(s) were handed to the provider.")));
         }

         try
         {
            String body = request.body() == null ? "" : request.body();
            if(StringUtils.hasContent(request.destinationUrl()))
            {
               body = body + "\n\n" + (StringUtils.hasContent(request.callToAction()) ? request.callToAction() + ": " : "") + request.destinationUrl();
            }

            new SendMessageAction().execute(new SendMessageInput()
               .withMessagingProviderName(messagingProviderName)
               .withFrom(new Party().withAddress(fromAddress).withRole(EmailPartyRole.FROM))
               .withTo(new Party().withAddress(recipient.address()).withRole(EmailPartyRole.TO))
               .withSubject(request.title())
               .withContent(new Content().withBody(body).withContentRole(EmailContentRole.TEXT)));
            sent++;
         }
         catch(Exception e)
         {
            LOG.warn("Email transport send failed; outcome unknown", logPair("executionId", request.executionId()), logPair("recipientKey", recipient.key()),
               logPair("error", e.getClass().getSimpleName()));
            return (new TransportResult(TransportResult.Outcome.AMBIGUOUS, null, "error",
               "The messaging provider raised " + e.getClass().getSimpleName() + " after " + sent + " of " + request.recipients().size() + " message(s); it may have accepted some."));
         }
      }
      return (new TransportResult(TransportResult.Outcome.ACCEPTED, "qqq-email:" + request.idempotencyKey(), "handed_to_provider",
         sent + " message(s) handed to messaging provider [" + messagingProviderName + "] (not proof of delivery)."));
   }



   /*******************************************************************************
    ** QQQ messaging keeps no record of sent messages.
    *******************************************************************************/
   @Override
   public TransportLookup lookup(String idempotencyKey)
   {
      return (new TransportLookup(TransportLookup.State.UNSUPPORTED, null));
   }



   /*******************************************************************************
    ** Fluent setter for name
    *******************************************************************************/
   public QqqMessagingEmailTransport withName(String name)
   {
      this.name = name;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for messagingProviderName
    *******************************************************************************/
   public QqqMessagingEmailTransport withMessagingProviderName(String messagingProviderName)
   {
      this.messagingProviderName = messagingProviderName;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for fromAddress
    *******************************************************************************/
   public QqqMessagingEmailTransport withFromAddress(String fromAddress)
   {
      this.fromAddress = fromAddress;
      return (this);
   }



   /*******************************************************************************
    ** Fluent setter for verifiedOperational: set only after the host verified
    ** its messaging provider end to end.
    *******************************************************************************/
   public QqqMessagingEmailTransport withVerifiedOperational(Boolean verifiedOperational)
   {
      this.verifiedOperational = verifiedOperational;
      return (this);
   }

}
