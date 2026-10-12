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
import java.time.LocalDate;
import com.kingsrook.qbits.marketing.model.enums.ClaimStatus;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.data.QField;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;
import com.kingsrook.qqq.backend.core.model.metadata.fields.ValueTooLongBehavior;


/*******************************************************************************
 ** A factual claim about a product, its status (released, planned, disputed) and its source.
 ** Table marketing_claim.
 *******************************************************************************/
public class MarketingClaim extends QRecordEntity
{
   public static final String TABLE_NAME = "marketing_claim";

   @QField(isEditable = false, isPrimaryKey = true)
   private Integer id;

   @QField(isRequired = true, possibleValueSourceName = MarketingProduct.TABLE_NAME)
   private Integer productId;

   @QField(maxLength = 1000, valueTooLongBehavior = ValueTooLongBehavior.ERROR, isRequired = true)
   private String claimText;

   @QField(isRequired = true, possibleValueSourceName = ClaimStatus.NAME)
   private Integer status;

   @QField(possibleValueSourceName = MarketingEvidenceSource.TABLE_NAME)
   private Integer sourceId;

   @QField(maxLength = 64, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String category;

   @QField()
   private String evidenceQuote;

   @QField()
   private LocalDate verifiedDate;

   @QField()
   private String discrepancyNote;

   @QField(isEditable = false)
   private Instant createDate;

   @QField(isEditable = false)
   private Instant modifyDate;



   /*******************************************************************************
    ** Default constructor.
    *******************************************************************************/
   public MarketingClaim()
   {
   }



   /*******************************************************************************
    ** Constructor that populates from a QRecord.
    *******************************************************************************/
   public MarketingClaim(QRecord record) throws QException
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
   public MarketingClaim withId(Integer id)
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
   public MarketingClaim withProductId(Integer productId)
   {
      this.productId = productId;
      return (this);
   }


   /*******************************************************************************
    ** Getter for claimText
    *******************************************************************************/
   public String getClaimText()
   {
      return (this.claimText);
   }



   /*******************************************************************************
    ** Setter for claimText
    *******************************************************************************/
   public void setClaimText(String claimText)
   {
      this.claimText = claimText;
   }



   /*******************************************************************************
    ** Fluent setter for claimText
    *******************************************************************************/
   public MarketingClaim withClaimText(String claimText)
   {
      this.claimText = claimText;
      return (this);
   }


   /*******************************************************************************
    ** Getter for status
    *******************************************************************************/
   public Integer getStatus()
   {
      return (this.status);
   }



   /*******************************************************************************
    ** Setter for status
    *******************************************************************************/
   public void setStatus(Integer status)
   {
      this.status = status;
   }



   /*******************************************************************************
    ** Fluent setter for status
    *******************************************************************************/
   public MarketingClaim withStatus(Integer status)
   {
      this.status = status;
      return (this);
   }


   /*******************************************************************************
    ** Getter for sourceId
    *******************************************************************************/
   public Integer getSourceId()
   {
      return (this.sourceId);
   }



   /*******************************************************************************
    ** Setter for sourceId
    *******************************************************************************/
   public void setSourceId(Integer sourceId)
   {
      this.sourceId = sourceId;
   }



   /*******************************************************************************
    ** Fluent setter for sourceId
    *******************************************************************************/
   public MarketingClaim withSourceId(Integer sourceId)
   {
      this.sourceId = sourceId;
      return (this);
   }


   /*******************************************************************************
    ** Getter for category
    *******************************************************************************/
   public String getCategory()
   {
      return (this.category);
   }



   /*******************************************************************************
    ** Setter for category
    *******************************************************************************/
   public void setCategory(String category)
   {
      this.category = category;
   }



   /*******************************************************************************
    ** Fluent setter for category
    *******************************************************************************/
   public MarketingClaim withCategory(String category)
   {
      this.category = category;
      return (this);
   }


   /*******************************************************************************
    ** Getter for evidenceQuote
    *******************************************************************************/
   public String getEvidenceQuote()
   {
      return (this.evidenceQuote);
   }



   /*******************************************************************************
    ** Setter for evidenceQuote
    *******************************************************************************/
   public void setEvidenceQuote(String evidenceQuote)
   {
      this.evidenceQuote = evidenceQuote;
   }



   /*******************************************************************************
    ** Fluent setter for evidenceQuote
    *******************************************************************************/
   public MarketingClaim withEvidenceQuote(String evidenceQuote)
   {
      this.evidenceQuote = evidenceQuote;
      return (this);
   }


   /*******************************************************************************
    ** Getter for verifiedDate
    *******************************************************************************/
   public LocalDate getVerifiedDate()
   {
      return (this.verifiedDate);
   }



   /*******************************************************************************
    ** Setter for verifiedDate
    *******************************************************************************/
   public void setVerifiedDate(LocalDate verifiedDate)
   {
      this.verifiedDate = verifiedDate;
   }



   /*******************************************************************************
    ** Fluent setter for verifiedDate
    *******************************************************************************/
   public MarketingClaim withVerifiedDate(LocalDate verifiedDate)
   {
      this.verifiedDate = verifiedDate;
      return (this);
   }


   /*******************************************************************************
    ** Getter for discrepancyNote
    *******************************************************************************/
   public String getDiscrepancyNote()
   {
      return (this.discrepancyNote);
   }



   /*******************************************************************************
    ** Setter for discrepancyNote
    *******************************************************************************/
   public void setDiscrepancyNote(String discrepancyNote)
   {
      this.discrepancyNote = discrepancyNote;
   }



   /*******************************************************************************
    ** Fluent setter for discrepancyNote
    *******************************************************************************/
   public MarketingClaim withDiscrepancyNote(String discrepancyNote)
   {
      this.discrepancyNote = discrepancyNote;
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
   public MarketingClaim withCreateDate(Instant createDate)
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
   public MarketingClaim withModifyDate(Instant modifyDate)
   {
      this.modifyDate = modifyDate;
      return (this);
   }

}
