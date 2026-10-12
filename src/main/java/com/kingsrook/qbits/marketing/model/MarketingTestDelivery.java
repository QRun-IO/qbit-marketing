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

package com.kingsrook.qbits.marketing.model;


import java.time.Instant;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.data.QField;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;
import com.kingsrook.qqq.backend.core.model.metadata.fields.ValueTooLongBehavior;


/*******************************************************************************
 ** What the controlled test transport accepted: the exact payload, never sent anywhere.
 ** Table marketing_test_delivery.
 *******************************************************************************/
public class MarketingTestDelivery extends QRecordEntity
{
   public static final String TABLE_NAME = "marketing_test_delivery";

   @QField(isEditable = false, isPrimaryKey = true)
   private Integer id;

   @QField(possibleValueSourceName = MarketingProduct.TABLE_NAME)
   private Integer productId;

   @QField(possibleValueSourceName = MarketingExecution.TABLE_NAME)
   private Integer executionId;

   @QField(maxLength = 200, valueTooLongBehavior = ValueTooLongBehavior.ERROR, isRequired = true)
   private String idempotencyKey;

   @QField(maxLength = 200, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String recipientKey;

   @QField(maxLength = 100, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String channelName;

   @QField(maxLength = 255, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String title;

   @QField()
   private String body;

   @QField(maxLength = 1000, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String destinationUrl;

   @QField(maxLength = 100, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String receiptId;

   @QField()
   private Instant acceptedAt;

   @QField()
   private Integer duplicateCount;

   @QField(isEditable = false)
   private Instant createDate;

   @QField(isEditable = false)
   private Instant modifyDate;



   /*******************************************************************************
    ** Default constructor.
    *******************************************************************************/
   public MarketingTestDelivery()
   {
   }



   /*******************************************************************************
    ** Constructor that populates from a QRecord.
    *******************************************************************************/
   public MarketingTestDelivery(QRecord record) throws QException
   {
      populateFromQRecord(record);
   }


   /*******************************************************************************
    ** Getter for id
    *******************************************************************************/
   public Integer getId()
   {
      return (this.id);
   }



   /*******************************************************************************
    ** Setter for id
    *******************************************************************************/
   public void setId(Integer id)
   {
      this.id = id;
   }



   /*******************************************************************************
    ** Fluent setter for id
    *******************************************************************************/
   public MarketingTestDelivery withId(Integer id)
   {
      this.id = id;
      return (this);
   }


   /*******************************************************************************
    ** Getter for productId
    *******************************************************************************/
   public Integer getProductId()
   {
      return (this.productId);
   }



   /*******************************************************************************
    ** Setter for productId
    *******************************************************************************/
   public void setProductId(Integer productId)
   {
      this.productId = productId;
   }



   /*******************************************************************************
    ** Fluent setter for productId
    *******************************************************************************/
   public MarketingTestDelivery withProductId(Integer productId)
   {
      this.productId = productId;
      return (this);
   }


   /*******************************************************************************
    ** Getter for executionId
    *******************************************************************************/
   public Integer getExecutionId()
   {
      return (this.executionId);
   }



   /*******************************************************************************
    ** Setter for executionId
    *******************************************************************************/
   public void setExecutionId(Integer executionId)
   {
      this.executionId = executionId;
   }



   /*******************************************************************************
    ** Fluent setter for executionId
    *******************************************************************************/
   public MarketingTestDelivery withExecutionId(Integer executionId)
   {
      this.executionId = executionId;
      return (this);
   }


   /*******************************************************************************
    ** Getter for idempotencyKey
    *******************************************************************************/
   public String getIdempotencyKey()
   {
      return (this.idempotencyKey);
   }



   /*******************************************************************************
    ** Setter for idempotencyKey
    *******************************************************************************/
   public void setIdempotencyKey(String idempotencyKey)
   {
      this.idempotencyKey = idempotencyKey;
   }



   /*******************************************************************************
    ** Fluent setter for idempotencyKey
    *******************************************************************************/
   public MarketingTestDelivery withIdempotencyKey(String idempotencyKey)
   {
      this.idempotencyKey = idempotencyKey;
      return (this);
   }


   /*******************************************************************************
    ** Getter for recipientKey
    *******************************************************************************/
   public String getRecipientKey()
   {
      return (this.recipientKey);
   }



   /*******************************************************************************
    ** Setter for recipientKey
    *******************************************************************************/
   public void setRecipientKey(String recipientKey)
   {
      this.recipientKey = recipientKey;
   }



   /*******************************************************************************
    ** Fluent setter for recipientKey
    *******************************************************************************/
   public MarketingTestDelivery withRecipientKey(String recipientKey)
   {
      this.recipientKey = recipientKey;
      return (this);
   }


   /*******************************************************************************
    ** Getter for channelName
    *******************************************************************************/
   public String getChannelName()
   {
      return (this.channelName);
   }



   /*******************************************************************************
    ** Setter for channelName
    *******************************************************************************/
   public void setChannelName(String channelName)
   {
      this.channelName = channelName;
   }



   /*******************************************************************************
    ** Fluent setter for channelName
    *******************************************************************************/
   public MarketingTestDelivery withChannelName(String channelName)
   {
      this.channelName = channelName;
      return (this);
   }


   /*******************************************************************************
    ** Getter for title
    *******************************************************************************/
   public String getTitle()
   {
      return (this.title);
   }



   /*******************************************************************************
    ** Setter for title
    *******************************************************************************/
   public void setTitle(String title)
   {
      this.title = title;
   }



   /*******************************************************************************
    ** Fluent setter for title
    *******************************************************************************/
   public MarketingTestDelivery withTitle(String title)
   {
      this.title = title;
      return (this);
   }


   /*******************************************************************************
    ** Getter for body
    *******************************************************************************/
   public String getBody()
   {
      return (this.body);
   }



   /*******************************************************************************
    ** Setter for body
    *******************************************************************************/
   public void setBody(String body)
   {
      this.body = body;
   }



   /*******************************************************************************
    ** Fluent setter for body
    *******************************************************************************/
   public MarketingTestDelivery withBody(String body)
   {
      this.body = body;
      return (this);
   }


   /*******************************************************************************
    ** Getter for destinationUrl
    *******************************************************************************/
   public String getDestinationUrl()
   {
      return (this.destinationUrl);
   }



   /*******************************************************************************
    ** Setter for destinationUrl
    *******************************************************************************/
   public void setDestinationUrl(String destinationUrl)
   {
      this.destinationUrl = destinationUrl;
   }



   /*******************************************************************************
    ** Fluent setter for destinationUrl
    *******************************************************************************/
   public MarketingTestDelivery withDestinationUrl(String destinationUrl)
   {
      this.destinationUrl = destinationUrl;
      return (this);
   }


   /*******************************************************************************
    ** Getter for receiptId
    *******************************************************************************/
   public String getReceiptId()
   {
      return (this.receiptId);
   }



   /*******************************************************************************
    ** Setter for receiptId
    *******************************************************************************/
   public void setReceiptId(String receiptId)
   {
      this.receiptId = receiptId;
   }



   /*******************************************************************************
    ** Fluent setter for receiptId
    *******************************************************************************/
   public MarketingTestDelivery withReceiptId(String receiptId)
   {
      this.receiptId = receiptId;
      return (this);
   }


   /*******************************************************************************
    ** Getter for acceptedAt
    *******************************************************************************/
   public Instant getAcceptedAt()
   {
      return (this.acceptedAt);
   }



   /*******************************************************************************
    ** Setter for acceptedAt
    *******************************************************************************/
   public void setAcceptedAt(Instant acceptedAt)
   {
      this.acceptedAt = acceptedAt;
   }



   /*******************************************************************************
    ** Fluent setter for acceptedAt
    *******************************************************************************/
   public MarketingTestDelivery withAcceptedAt(Instant acceptedAt)
   {
      this.acceptedAt = acceptedAt;
      return (this);
   }


   /*******************************************************************************
    ** Getter for duplicateCount
    *******************************************************************************/
   public Integer getDuplicateCount()
   {
      return (this.duplicateCount);
   }



   /*******************************************************************************
    ** Setter for duplicateCount
    *******************************************************************************/
   public void setDuplicateCount(Integer duplicateCount)
   {
      this.duplicateCount = duplicateCount;
   }



   /*******************************************************************************
    ** Fluent setter for duplicateCount
    *******************************************************************************/
   public MarketingTestDelivery withDuplicateCount(Integer duplicateCount)
   {
      this.duplicateCount = duplicateCount;
      return (this);
   }


   /*******************************************************************************
    ** Getter for createDate
    *******************************************************************************/
   public Instant getCreateDate()
   {
      return (this.createDate);
   }



   /*******************************************************************************
    ** Setter for createDate
    *******************************************************************************/
   public void setCreateDate(Instant createDate)
   {
      this.createDate = createDate;
   }



   /*******************************************************************************
    ** Fluent setter for createDate
    *******************************************************************************/
   public MarketingTestDelivery withCreateDate(Instant createDate)
   {
      this.createDate = createDate;
      return (this);
   }


   /*******************************************************************************
    ** Getter for modifyDate
    *******************************************************************************/
   public Instant getModifyDate()
   {
      return (this.modifyDate);
   }



   /*******************************************************************************
    ** Setter for modifyDate
    *******************************************************************************/
   public void setModifyDate(Instant modifyDate)
   {
      this.modifyDate = modifyDate;
   }



   /*******************************************************************************
    ** Fluent setter for modifyDate
    *******************************************************************************/
   public MarketingTestDelivery withModifyDate(Instant modifyDate)
   {
      this.modifyDate = modifyDate;
      return (this);
   }

}
