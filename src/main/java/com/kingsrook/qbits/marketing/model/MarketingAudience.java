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
 ** An audience of a product: who they are, the real problem and the task the product helps complete.
 ** Table marketing_audience.
 *******************************************************************************/
public class MarketingAudience extends QRecordEntity
{
   public static final String TABLE_NAME = "marketing_audience";

   @QField(isEditable = false, isPrimaryKey = true)
   private Integer id;

   @QField(isRequired = true, possibleValueSourceName = MarketingProduct.TABLE_NAME)
   private Integer productId;

   @QField(maxLength = 150, valueTooLongBehavior = ValueTooLongBehavior.ERROR, isRequired = true)
   private String name;

   @QField()
   private String problem;

   @QField()
   private String usefulTask;

   @QField()
   private String description;

   @QField()
   private String evidenceNote;

   @QField(isEditable = false)
   private Instant createDate;

   @QField(isEditable = false)
   private Instant modifyDate;



   /*******************************************************************************
    ** Default constructor.
    *******************************************************************************/
   public MarketingAudience()
   {
   }



   /*******************************************************************************
    ** Constructor that populates from a QRecord.
    *******************************************************************************/
   public MarketingAudience(QRecord record) throws QException
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
   public MarketingAudience withId(Integer id)
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
   public MarketingAudience withProductId(Integer productId)
   {
      this.productId = productId;
      return (this);
   }


   /*******************************************************************************
    ** Getter for name
    *******************************************************************************/
   public String getName()
   {
      return (this.name);
   }



   /*******************************************************************************
    ** Setter for name
    *******************************************************************************/
   public void setName(String name)
   {
      this.name = name;
   }



   /*******************************************************************************
    ** Fluent setter for name
    *******************************************************************************/
   public MarketingAudience withName(String name)
   {
      this.name = name;
      return (this);
   }


   /*******************************************************************************
    ** Getter for problem
    *******************************************************************************/
   public String getProblem()
   {
      return (this.problem);
   }



   /*******************************************************************************
    ** Setter for problem
    *******************************************************************************/
   public void setProblem(String problem)
   {
      this.problem = problem;
   }



   /*******************************************************************************
    ** Fluent setter for problem
    *******************************************************************************/
   public MarketingAudience withProblem(String problem)
   {
      this.problem = problem;
      return (this);
   }


   /*******************************************************************************
    ** Getter for usefulTask
    *******************************************************************************/
   public String getUsefulTask()
   {
      return (this.usefulTask);
   }



   /*******************************************************************************
    ** Setter for usefulTask
    *******************************************************************************/
   public void setUsefulTask(String usefulTask)
   {
      this.usefulTask = usefulTask;
   }



   /*******************************************************************************
    ** Fluent setter for usefulTask
    *******************************************************************************/
   public MarketingAudience withUsefulTask(String usefulTask)
   {
      this.usefulTask = usefulTask;
      return (this);
   }


   /*******************************************************************************
    ** Getter for description
    *******************************************************************************/
   public String getDescription()
   {
      return (this.description);
   }



   /*******************************************************************************
    ** Setter for description
    *******************************************************************************/
   public void setDescription(String description)
   {
      this.description = description;
   }



   /*******************************************************************************
    ** Fluent setter for description
    *******************************************************************************/
   public MarketingAudience withDescription(String description)
   {
      this.description = description;
      return (this);
   }


   /*******************************************************************************
    ** Getter for evidenceNote
    *******************************************************************************/
   public String getEvidenceNote()
   {
      return (this.evidenceNote);
   }



   /*******************************************************************************
    ** Setter for evidenceNote
    *******************************************************************************/
   public void setEvidenceNote(String evidenceNote)
   {
      this.evidenceNote = evidenceNote;
   }



   /*******************************************************************************
    ** Fluent setter for evidenceNote
    *******************************************************************************/
   public MarketingAudience withEvidenceNote(String evidenceNote)
   {
      this.evidenceNote = evidenceNote;
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
   public MarketingAudience withCreateDate(Instant createDate)
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
   public MarketingAudience withModifyDate(Instant modifyDate)
   {
      this.modifyDate = modifyDate;
      return (this);
   }

}
