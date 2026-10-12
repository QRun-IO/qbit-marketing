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
import com.kingsrook.qbits.marketing.model.enums.AttemptOutcome;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.data.QField;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;
import com.kingsrook.qqq.backend.core.model.metadata.fields.ValueTooLongBehavior;


/*******************************************************************************
 ** One attempt to run an execution, with the provider's receipt or failure.
 ** Table marketing_execution_attempt.
 *******************************************************************************/
public class MarketingExecutionAttempt extends QRecordEntity
{
   public static final String TABLE_NAME = "marketing_execution_attempt";

   @QField(isEditable = false, isPrimaryKey = true)
   private Integer id;

   @QField(possibleValueSourceName = MarketingProduct.TABLE_NAME)
   private Integer productId;

   @QField(isRequired = true, possibleValueSourceName = MarketingExecution.TABLE_NAME)
   private Integer executionId;

   @QField()
   private Integer attemptNumber;

   @QField()
   private Instant startedAt;

   @QField()
   private Instant finishedAt;

   @QField(isRequired = true, possibleValueSourceName = AttemptOutcome.NAME)
   private Integer outcome;

   @QField()
   private Integer recipientCount;

   @QField(maxLength = 255, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String providerReceiptId;

   @QField(maxLength = 100, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String providerStatus;

   @QField()
   private String message;

   @QField()
   private Long durationMs;

   @QField(maxLength = 100, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String workerName;

   @QField(isEditable = false)
   private Instant createDate;

   @QField(isEditable = false)
   private Instant modifyDate;



   /*******************************************************************************
    ** Default constructor.
    *******************************************************************************/
   public MarketingExecutionAttempt()
   {
   }



   /*******************************************************************************
    ** Constructor that populates from a QRecord.
    *******************************************************************************/
   public MarketingExecutionAttempt(QRecord record) throws QException
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
   public MarketingExecutionAttempt withId(Integer id)
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
   public MarketingExecutionAttempt withProductId(Integer productId)
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
   public MarketingExecutionAttempt withExecutionId(Integer executionId)
   {
      this.executionId = executionId;
      return (this);
   }


   /*******************************************************************************
    ** Getter for attemptNumber
    *******************************************************************************/
   public Integer getAttemptNumber()
   {
      return (this.attemptNumber);
   }



   /*******************************************************************************
    ** Setter for attemptNumber
    *******************************************************************************/
   public void setAttemptNumber(Integer attemptNumber)
   {
      this.attemptNumber = attemptNumber;
   }



   /*******************************************************************************
    ** Fluent setter for attemptNumber
    *******************************************************************************/
   public MarketingExecutionAttempt withAttemptNumber(Integer attemptNumber)
   {
      this.attemptNumber = attemptNumber;
      return (this);
   }


   /*******************************************************************************
    ** Getter for startedAt
    *******************************************************************************/
   public Instant getStartedAt()
   {
      return (this.startedAt);
   }



   /*******************************************************************************
    ** Setter for startedAt
    *******************************************************************************/
   public void setStartedAt(Instant startedAt)
   {
      this.startedAt = startedAt;
   }



   /*******************************************************************************
    ** Fluent setter for startedAt
    *******************************************************************************/
   public MarketingExecutionAttempt withStartedAt(Instant startedAt)
   {
      this.startedAt = startedAt;
      return (this);
   }


   /*******************************************************************************
    ** Getter for finishedAt
    *******************************************************************************/
   public Instant getFinishedAt()
   {
      return (this.finishedAt);
   }



   /*******************************************************************************
    ** Setter for finishedAt
    *******************************************************************************/
   public void setFinishedAt(Instant finishedAt)
   {
      this.finishedAt = finishedAt;
   }



   /*******************************************************************************
    ** Fluent setter for finishedAt
    *******************************************************************************/
   public MarketingExecutionAttempt withFinishedAt(Instant finishedAt)
   {
      this.finishedAt = finishedAt;
      return (this);
   }


   /*******************************************************************************
    ** Getter for outcome
    *******************************************************************************/
   public Integer getOutcome()
   {
      return (this.outcome);
   }



   /*******************************************************************************
    ** Setter for outcome
    *******************************************************************************/
   public void setOutcome(Integer outcome)
   {
      this.outcome = outcome;
   }



   /*******************************************************************************
    ** Fluent setter for outcome
    *******************************************************************************/
   public MarketingExecutionAttempt withOutcome(Integer outcome)
   {
      this.outcome = outcome;
      return (this);
   }


   /*******************************************************************************
    ** Getter for recipientCount
    *******************************************************************************/
   public Integer getRecipientCount()
   {
      return (this.recipientCount);
   }



   /*******************************************************************************
    ** Setter for recipientCount
    *******************************************************************************/
   public void setRecipientCount(Integer recipientCount)
   {
      this.recipientCount = recipientCount;
   }



   /*******************************************************************************
    ** Fluent setter for recipientCount
    *******************************************************************************/
   public MarketingExecutionAttempt withRecipientCount(Integer recipientCount)
   {
      this.recipientCount = recipientCount;
      return (this);
   }


   /*******************************************************************************
    ** Getter for providerReceiptId
    *******************************************************************************/
   public String getProviderReceiptId()
   {
      return (this.providerReceiptId);
   }



   /*******************************************************************************
    ** Setter for providerReceiptId
    *******************************************************************************/
   public void setProviderReceiptId(String providerReceiptId)
   {
      this.providerReceiptId = providerReceiptId;
   }



   /*******************************************************************************
    ** Fluent setter for providerReceiptId
    *******************************************************************************/
   public MarketingExecutionAttempt withProviderReceiptId(String providerReceiptId)
   {
      this.providerReceiptId = providerReceiptId;
      return (this);
   }


   /*******************************************************************************
    ** Getter for providerStatus
    *******************************************************************************/
   public String getProviderStatus()
   {
      return (this.providerStatus);
   }



   /*******************************************************************************
    ** Setter for providerStatus
    *******************************************************************************/
   public void setProviderStatus(String providerStatus)
   {
      this.providerStatus = providerStatus;
   }



   /*******************************************************************************
    ** Fluent setter for providerStatus
    *******************************************************************************/
   public MarketingExecutionAttempt withProviderStatus(String providerStatus)
   {
      this.providerStatus = providerStatus;
      return (this);
   }


   /*******************************************************************************
    ** Getter for message
    *******************************************************************************/
   public String getMessage()
   {
      return (this.message);
   }



   /*******************************************************************************
    ** Setter for message
    *******************************************************************************/
   public void setMessage(String message)
   {
      this.message = message;
   }



   /*******************************************************************************
    ** Fluent setter for message
    *******************************************************************************/
   public MarketingExecutionAttempt withMessage(String message)
   {
      this.message = message;
      return (this);
   }


   /*******************************************************************************
    ** Getter for durationMs
    *******************************************************************************/
   public Long getDurationMs()
   {
      return (this.durationMs);
   }



   /*******************************************************************************
    ** Setter for durationMs
    *******************************************************************************/
   public void setDurationMs(Long durationMs)
   {
      this.durationMs = durationMs;
   }



   /*******************************************************************************
    ** Fluent setter for durationMs
    *******************************************************************************/
   public MarketingExecutionAttempt withDurationMs(Long durationMs)
   {
      this.durationMs = durationMs;
      return (this);
   }


   /*******************************************************************************
    ** Getter for workerName
    *******************************************************************************/
   public String getWorkerName()
   {
      return (this.workerName);
   }



   /*******************************************************************************
    ** Setter for workerName
    *******************************************************************************/
   public void setWorkerName(String workerName)
   {
      this.workerName = workerName;
   }



   /*******************************************************************************
    ** Fluent setter for workerName
    *******************************************************************************/
   public MarketingExecutionAttempt withWorkerName(String workerName)
   {
      this.workerName = workerName;
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
   public MarketingExecutionAttempt withCreateDate(Instant createDate)
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
   public MarketingExecutionAttempt withModifyDate(Instant modifyDate)
   {
      this.modifyDate = modifyDate;
      return (this);
   }

}
