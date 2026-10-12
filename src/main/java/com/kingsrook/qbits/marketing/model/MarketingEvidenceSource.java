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
import com.kingsrook.qbits.marketing.model.enums.SourceType;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.data.QField;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;
import com.kingsrook.qqq.backend.core.model.metadata.fields.ValueTooLongBehavior;


/*******************************************************************************
 ** A source that product facts were verified against, with its verification date.
 ** Table marketing_evidence_source.
 *******************************************************************************/
public class MarketingEvidenceSource extends QRecordEntity
{
   public static final String TABLE_NAME = "marketing_evidence_source";

   @QField(isEditable = false, isPrimaryKey = true)
   private Integer id;

   @QField(isRequired = true, possibleValueSourceName = MarketingProduct.TABLE_NAME)
   private Integer productId;

   @QField(maxLength = 255, valueTooLongBehavior = ValueTooLongBehavior.ERROR, isRequired = true)
   private String title;

   @QField(maxLength = 1000, valueTooLongBehavior = ValueTooLongBehavior.ERROR, isRequired = true)
   private String url;

   @QField(isRequired = true, possibleValueSourceName = SourceType.NAME)
   private Integer sourceType;

   @QField(maxLength = 150, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String publisher;

   @QField()
   private LocalDate verifiedDate;

   @QField(maxLength = 100, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String verifiedBy;

   @QField()
   private String excerpt;

   @QField()
   private String notes;

   @QField(isEditable = false)
   private Instant createDate;

   @QField(isEditable = false)
   private Instant modifyDate;



   /*******************************************************************************
    ** Default constructor.
    *******************************************************************************/
   public MarketingEvidenceSource()
   {
   }



   /*******************************************************************************
    ** Constructor that populates from a QRecord.
    *******************************************************************************/
   public MarketingEvidenceSource(QRecord record) throws QException
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
   public MarketingEvidenceSource withId(Integer id)
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
   public MarketingEvidenceSource withProductId(Integer productId)
   {
      this.productId = productId;
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
   public MarketingEvidenceSource withTitle(String title)
   {
      this.title = title;
      return (this);
   }


   /*******************************************************************************
    ** Getter for url
    *******************************************************************************/
   public String getUrl()
   {
      return (this.url);
   }



   /*******************************************************************************
    ** Setter for url
    *******************************************************************************/
   public void setUrl(String url)
   {
      this.url = url;
   }



   /*******************************************************************************
    ** Fluent setter for url
    *******************************************************************************/
   public MarketingEvidenceSource withUrl(String url)
   {
      this.url = url;
      return (this);
   }


   /*******************************************************************************
    ** Getter for sourceType
    *******************************************************************************/
   public Integer getSourceType()
   {
      return (this.sourceType);
   }



   /*******************************************************************************
    ** Setter for sourceType
    *******************************************************************************/
   public void setSourceType(Integer sourceType)
   {
      this.sourceType = sourceType;
   }



   /*******************************************************************************
    ** Fluent setter for sourceType
    *******************************************************************************/
   public MarketingEvidenceSource withSourceType(Integer sourceType)
   {
      this.sourceType = sourceType;
      return (this);
   }


   /*******************************************************************************
    ** Getter for publisher
    *******************************************************************************/
   public String getPublisher()
   {
      return (this.publisher);
   }



   /*******************************************************************************
    ** Setter for publisher
    *******************************************************************************/
   public void setPublisher(String publisher)
   {
      this.publisher = publisher;
   }



   /*******************************************************************************
    ** Fluent setter for publisher
    *******************************************************************************/
   public MarketingEvidenceSource withPublisher(String publisher)
   {
      this.publisher = publisher;
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
   public MarketingEvidenceSource withVerifiedDate(LocalDate verifiedDate)
   {
      this.verifiedDate = verifiedDate;
      return (this);
   }


   /*******************************************************************************
    ** Getter for verifiedBy
    *******************************************************************************/
   public String getVerifiedBy()
   {
      return (this.verifiedBy);
   }



   /*******************************************************************************
    ** Setter for verifiedBy
    *******************************************************************************/
   public void setVerifiedBy(String verifiedBy)
   {
      this.verifiedBy = verifiedBy;
   }



   /*******************************************************************************
    ** Fluent setter for verifiedBy
    *******************************************************************************/
   public MarketingEvidenceSource withVerifiedBy(String verifiedBy)
   {
      this.verifiedBy = verifiedBy;
      return (this);
   }


   /*******************************************************************************
    ** Getter for excerpt
    *******************************************************************************/
   public String getExcerpt()
   {
      return (this.excerpt);
   }



   /*******************************************************************************
    ** Setter for excerpt
    *******************************************************************************/
   public void setExcerpt(String excerpt)
   {
      this.excerpt = excerpt;
   }



   /*******************************************************************************
    ** Fluent setter for excerpt
    *******************************************************************************/
   public MarketingEvidenceSource withExcerpt(String excerpt)
   {
      this.excerpt = excerpt;
      return (this);
   }


   /*******************************************************************************
    ** Getter for notes
    *******************************************************************************/
   public String getNotes()
   {
      return (this.notes);
   }



   /*******************************************************************************
    ** Setter for notes
    *******************************************************************************/
   public void setNotes(String notes)
   {
      this.notes = notes;
   }



   /*******************************************************************************
    ** Fluent setter for notes
    *******************************************************************************/
   public MarketingEvidenceSource withNotes(String notes)
   {
      this.notes = notes;
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
   public MarketingEvidenceSource withCreateDate(Instant createDate)
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
   public MarketingEvidenceSource withModifyDate(Instant modifyDate)
   {
      this.modifyDate = modifyDate;
      return (this);
   }

}
