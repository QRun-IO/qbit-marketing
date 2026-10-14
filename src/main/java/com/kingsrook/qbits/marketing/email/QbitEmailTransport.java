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

package com.kingsrook.qbits.marketing.email;


import java.util.EnumSet;
import java.util.List;
import java.util.Set;
import com.kingsrook.qbits.email.api.EmailApi;
import com.kingsrook.qbits.email.api.EmailApiCategory;
import com.kingsrook.qbits.email.api.EmailApiMessage;
import com.kingsrook.qbits.email.api.EmailApiReadiness;
import com.kingsrook.qbits.email.api.EmailApiReadinessCheck;
import com.kingsrook.qbits.email.api.EmailApiRequest;
import com.kingsrook.qbits.email.api.EmailApiResult;
import com.kingsrook.qbits.marketing.model.MarketingExecution;
import com.kingsrook.qbits.marketing.model.enums.ChannelType;
import com.kingsrook.qbits.marketing.transport.MarketingTransport;
import com.kingsrook.qqq.backend.core.context.QContext;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.logging.QLogger;
import com.kingsrook.qqq.backend.core.model.metadata.QInstance;
import com.kingsrook.qqq.backend.core.utils.StringUtils;
import static com.kingsrook.qqq.backend.core.logging.LogUtils.logPair;


/*******************************************************************************
 ** Email through qbit-email: the durable outbox, the RFC 8058 unsubscribe
 ** link and headers, and suppression enforcement all live there.
 **
 ** This is the ONLY class in this qbit that references qbit-email classes
 ** (the dependency is optional). Callers reach it only after
 ** isInstalled(...) is true; otherwise QqqMessagingEmailTransport is the
 ** fallback.
 **
 ** Content: a campaign's content row holds a title and a plain body (no
 ** template), so each message is an ad-hoc Markdown email: subject = title,
 ** body = body plus the call to action as a link. No qbit-email template is
 ** registered (nothing here needs a variable schema).
 **
 ** One message per recipient, category MARKETING, idempotency key
 ** marketing:CONTENT:APPROVAL:RECIPIENT-KEY (the execution's own stable key
 ** plus the recipient key, never an address), and the execution row as the
 ** linked record.
 **
 ** Outcomes: QUEUED is ACCEPTED (in qbit-email's outbox, not delivered).
 ** SUPPRESSED is a skip, not a failure: the address opted out or bounced, so
 ** nothing is sent for it (the result says how many were skipped; if every
 ** recipient was suppressed the result is ACCEPTED with providerStatus
 ** all_suppressed). REFUSED is REJECTED with qbit-email's reasons. An
 ** exception is AMBIGUOUS, reconcilable through lookup (the idempotency key
 ** means a repeat can never send twice).
 *******************************************************************************/
public class QbitEmailTransport implements MarketingTransport
{
   private static final QLogger LOG = QLogger.getLogger(QbitEmailTransport.class);

   public static final String DEFAULT_NAME = "qbit_email";

   private static final String UNSUBSCRIBE_KEY_NAME = "QQQ_EMAIL_UNSUBSCRIBE_KEY";

   private String name = DEFAULT_NAME;



   /*******************************************************************************
    ** Is qbit-email on the classpath AND produced in this instance?
    *******************************************************************************/
   public static boolean isInstalled(QInstance qInstance)
   {
      try
      {
         Class.forName("com.kingsrook.qbits.email.api.EmailApi");
         return (EmailApi.isInstalled(qInstance));
      }
      catch(ClassNotFoundException | LinkageError e)
      {
         return (false);
      }
   }



   /*******************************************************************************
    **
    *******************************************************************************/
   @Override
   public String getName()
   {
      return (name);
   }



   /*******************************************************************************
    ** OPERATIONAL when the qbit-email outbox is produced and marketing's opt-out
    ** prerequisites (public base URL and unsubscribe key) are configured; the
    ** provider itself is checked by qbit-email at send time and a problem there
    ** comes back as a plain REJECTED reason.
    *******************************************************************************/
   @Override
   public TransportStatus getStatus()
   {
      if(!isInstalled(QContext.getQInstance()))
      {
         return (new TransportStatus(TransportStatus.State.NOT_CONFIGURED, "The email qbit (qbit-email) is not installed in this application."));
      }
      try
      {
         EmailApiReadiness readiness = EmailApi.readiness(QContext.getQInstance(), EmailApiCategory.MARKETING);
         if(readiness.failed(EmailApiReadinessCheck.NOT_INSTALLED))
         {
            return (new TransportStatus(TransportStatus.State.NOT_CONFIGURED, "The email qbit (qbit-email) is not installed in this application."));
         }
         if(readiness.failed(EmailApiReadinessCheck.PUBLIC_BASE_URL))
         {
            return (new TransportStatus(TransportStatus.State.NOT_CONFIGURED, "Marketing email needs the email qbit's publicBaseUrl (the unsubscribe link base)."));
         }
         if(readiness.failed(EmailApiReadinessCheck.UNSUBSCRIBE_KEY))
         {
            return (new TransportStatus(TransportStatus.State.NOT_CONFIGURED, "Marketing email needs the " + UNSUBSCRIBE_KEY_NAME + " secret (it signs the unsubscribe links)."));
         }
      }
      catch(QException e)
      {
         LOG.warn("Could not check email qbit readiness", logPair("error", e.getClass().getSimpleName()));
         return (new TransportStatus(TransportStatus.State.CONFIGURED, "Could not check the email qbit's configuration; retry when its configuration can be read."));
      }
      return (new TransportStatus(TransportStatus.State.OPERATIONAL, "Queues through the email qbit's outbox with its unsubscribe link and suppression list."));
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
      TransportStatus status = getStatus();
      if(!status.isUsable())
      {
         return (new TransportResult(TransportResult.Outcome.RETRYABLE_FAILURE, null, "not_configured", status.detail()));
      }

      int queued     = 0;
      int duplicates = 0;
      int suppressed = 0;
      for(Recipient recipient : request.recipients())
      {
         if(!StringUtils.hasContent(recipient.address()))
         {
            return (new TransportResult(queued + duplicates == 0 ? TransportResult.Outcome.REJECTED : TransportResult.Outcome.AMBIGUOUS, null, "no_address",
               "A recipient has no address" + (queued + duplicates == 0 ? "." : " after " + (queued + duplicates) + " message(s) were queued.")));
         }

         try
         {
            EmailApiResult result = EmailApi.send(emailRequest(request, recipient));
            switch(result.status())
            {
               case QUEUED -> queued++;
               case DUPLICATE -> duplicates++;
               case SUPPRESSED -> suppressed++;
               default ->
               {
                  String reasons = result.reasons() == null || result.reasons().isEmpty() ? "no reason given" : String.join("; ", result.reasons());
                  LOG.info("Email qbit refused a marketing message", logPair("executionId", request.executionId()), logPair("recipientKey", recipient.key()));
                  return (new TransportResult(TransportResult.Outcome.REJECTED, null, "refused",
                     "The email qbit refused the message: " + reasons + " (" + queued + " queued and " + suppressed + " suppressed before it)."));
               }
            }
         }
         catch(Exception e)
         {
            LOG.warn("Email qbit enqueue failed; outcome unknown", logPair("executionId", request.executionId()), logPair("recipientKey", recipient.key()),
               logPair("error", e.getClass().getSimpleName()));
            return (new TransportResult(TransportResult.Outcome.AMBIGUOUS, null, "error",
               "The email qbit raised " + e.getClass().getSimpleName() + " after " + (queued + duplicates) + " of " + request.recipients().size()
                  + " message(s); a repeat cannot send twice, and lookup can confirm what was queued."));
         }
      }

      String receipt = "qbit-email:" + baseKey(request);
      String summary = queued + " queued, " + suppressed + " skipped as suppressed, " + duplicates + " already queued (queued is not proof of delivery).";
      if(queued == 0 && suppressed == 0)
      {
         return (new TransportResult(TransportResult.Outcome.DUPLICATE, receipt, "already_queued", summary));
      }
      return (new TransportResult(TransportResult.Outcome.ACCEPTED, receipt, queued == 0 ? "all_suppressed" : "queued", summary));
   }



   /*******************************************************************************
    ** Finds what qbit-email queued for the execution (any message under its key).
    *******************************************************************************/
   @Override
   public TransportLookup lookup(String idempotencyKey)
   {
      try
      {
         String                prefix = "marketing:" + stripPrefix(idempotencyKey) + ":";
         List<EmailApiMessage> rows   = EmailApi.findMessagesByIdempotencyKeyPrefix(prefix, 1);
         if(rows.isEmpty())
         {
            return (new TransportLookup(TransportLookup.State.NOT_FOUND, null));
         }
         return (new TransportLookup(TransportLookup.State.FOUND_ACCEPTED, "qbit-email:marketing:" + stripPrefix(idempotencyKey)));
      }
      catch(Exception e)
      {
         LOG.warn("Email qbit lookup failed", logPair("error", e.getClass().getSimpleName()));
         return (new TransportLookup(TransportLookup.State.UNSUPPORTED, null));
      }
   }



   /*******************************************************************************
    ** One MARKETING message to one recipient.
    *******************************************************************************/
   private EmailApiRequest emailRequest(TransportRequest request, Recipient recipient)
   {
      String body = request.body() == null ? "" : request.body();
      if(StringUtils.hasContent(request.destinationUrl()) && request.destinationUrl().matches("(?i)https?://\\S+"))
      {
         String label = StringUtils.hasContent(request.callToAction()) ? request.callToAction().replaceAll("[\\[\\]\\r\\n]", " ").trim() : request.destinationUrl();
         body = body + "\n\n[" + label + "](" + request.destinationUrl() + ")";
      }

      return (new EmailApiRequest()
         .withTo(recipient.address())
         .withSubject(request.title() == null ? "" : request.title().replaceAll("[\\r\\n]+", " ").trim())
         .withMarkdownBody(body)
         .withCategory(EmailApiCategory.MARKETING)
         .withIdempotencyKey(baseKey(request) + ":" + recipient.key())
         .withLinkedRecord(MarketingExecution.TABLE_NAME, request.executionId())
         .withSender("marketing", "execution"));
   }



   /*******************************************************************************
    ** The per-execution part of the idempotency key.
    *******************************************************************************/
   private String baseKey(TransportRequest request)
   {
      return ("marketing:" + stripPrefix(request.idempotencyKey()));
   }



   /*******************************************************************************
    ** The execution key is marketing-execution:CONTENT:APPROVAL.
    *******************************************************************************/
   private static String stripPrefix(String key)
   {
      return (key == null ? "" : key.replaceFirst("^marketing-execution:", ""));
   }

}
