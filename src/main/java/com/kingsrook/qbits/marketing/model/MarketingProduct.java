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
import com.kingsrook.qbits.marketing.model.enums.ProductMaturity;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.data.QField;
import com.kingsrook.qqq.backend.core.model.data.QRecord;
import com.kingsrook.qqq.backend.core.model.data.QRecordEntity;
import com.kingsrook.qqq.backend.core.model.metadata.fields.ValueTooLongBehavior;


/*******************************************************************************
 ** A product the marketing team promotes: its brief, maturity and official links.
 ** Table marketing_product.
 *******************************************************************************/
public class MarketingProduct extends QRecordEntity
{
   public static final String TABLE_NAME = "marketing_product";

   @QField(isEditable = false, isPrimaryKey = true)
   private Integer id;

   @QField(maxLength = 100, valueTooLongBehavior = ValueTooLongBehavior.ERROR, isRequired = true)
   private String name;

   @QField(maxLength = 64, valueTooLongBehavior = ValueTooLongBehavior.ERROR, isRequired = true)
   private String slug;

   @QField(maxLength = 255, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String tagline;

   @QField()
   private String summary;

   @QField(isRequired = true, possibleValueSourceName = ProductMaturity.NAME)
   private Integer maturity;

   @QField()
   private String readinessNotes;

   @QField(maxLength = 1000, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String primaryUrl;

   @QField(maxLength = 1000, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String repositoryUrl;

   @QField(maxLength = 1000, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String documentationUrl;

   @QField(maxLength = 1000, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String releasesUrl;

   @QField(maxLength = 100, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String license;

   @QField(maxLength = 255, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String platforms;

   @QField(maxLength = 100, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String latestRelease;

   @QField()
   private String adoptionPath;

   @QField()
   private String valuePropositions;

   @QField(maxLength = 100, valueTooLongBehavior = ValueTooLongBehavior.ERROR)
   private String ownerUserId;

   @QField()
   private LocalDate lastVerifiedDate;

   @QField(isEditable = false)
   private Instant createDate;

   @QField(isEditable = false)
   private Instant modifyDate;



   /*******************************************************************************
    ** Default constructor.
    *******************************************************************************/
   public MarketingProduct()
   {
   }



   /*******************************************************************************
    ** Constructor that populates from a QRecord.
    *******************************************************************************/
   public MarketingProduct(QRecord record) throws QException
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
   public MarketingProduct withId(Integer id)
   {
      this.id = id;
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
   public MarketingProduct withName(String name)
   {
      this.name = name;
      return (this);
   }


   /*******************************************************************************
    ** Getter for slug
    *******************************************************************************/
   public String getSlug()
   {
      return (this.slug);
   }



   /*******************************************************************************
    ** Setter for slug
    *******************************************************************************/
   public void setSlug(String slug)
   {
      this.slug = slug;
   }



   /*******************************************************************************
    ** Fluent setter for slug
    *******************************************************************************/
   public MarketingProduct withSlug(String slug)
   {
      this.slug = slug;
      return (this);
   }


   /*******************************************************************************
    ** Getter for tagline
    *******************************************************************************/
   public String getTagline()
   {
      return (this.tagline);
   }



   /*******************************************************************************
    ** Setter for tagline
    *******************************************************************************/
   public void setTagline(String tagline)
   {
      this.tagline = tagline;
   }



   /*******************************************************************************
    ** Fluent setter for tagline
    *******************************************************************************/
   public MarketingProduct withTagline(String tagline)
   {
      this.tagline = tagline;
      return (this);
   }


   /*******************************************************************************
    ** Getter for summary
    *******************************************************************************/
   public String getSummary()
   {
      return (this.summary);
   }



   /*******************************************************************************
    ** Setter for summary
    *******************************************************************************/
   public void setSummary(String summary)
   {
      this.summary = summary;
   }



   /*******************************************************************************
    ** Fluent setter for summary
    *******************************************************************************/
   public MarketingProduct withSummary(String summary)
   {
      this.summary = summary;
      return (this);
   }


   /*******************************************************************************
    ** Getter for maturity
    *******************************************************************************/
   public Integer getMaturity()
   {
      return (this.maturity);
   }



   /*******************************************************************************
    ** Setter for maturity
    *******************************************************************************/
   public void setMaturity(Integer maturity)
   {
      this.maturity = maturity;
   }



   /*******************************************************************************
    ** Fluent setter for maturity
    *******************************************************************************/
   public MarketingProduct withMaturity(Integer maturity)
   {
      this.maturity = maturity;
      return (this);
   }


   /*******************************************************************************
    ** Getter for readinessNotes
    *******************************************************************************/
   public String getReadinessNotes()
   {
      return (this.readinessNotes);
   }



   /*******************************************************************************
    ** Setter for readinessNotes
    *******************************************************************************/
   public void setReadinessNotes(String readinessNotes)
   {
      this.readinessNotes = readinessNotes;
   }



   /*******************************************************************************
    ** Fluent setter for readinessNotes
    *******************************************************************************/
   public MarketingProduct withReadinessNotes(String readinessNotes)
   {
      this.readinessNotes = readinessNotes;
      return (this);
   }


   /*******************************************************************************
    ** Getter for primaryUrl
    *******************************************************************************/
   public String getPrimaryUrl()
   {
      return (this.primaryUrl);
   }



   /*******************************************************************************
    ** Setter for primaryUrl
    *******************************************************************************/
   public void setPrimaryUrl(String primaryUrl)
   {
      this.primaryUrl = primaryUrl;
   }



   /*******************************************************************************
    ** Fluent setter for primaryUrl
    *******************************************************************************/
   public MarketingProduct withPrimaryUrl(String primaryUrl)
   {
      this.primaryUrl = primaryUrl;
      return (this);
   }


   /*******************************************************************************
    ** Getter for repositoryUrl
    *******************************************************************************/
   public String getRepositoryUrl()
   {
      return (this.repositoryUrl);
   }



   /*******************************************************************************
    ** Setter for repositoryUrl
    *******************************************************************************/
   public void setRepositoryUrl(String repositoryUrl)
   {
      this.repositoryUrl = repositoryUrl;
   }



   /*******************************************************************************
    ** Fluent setter for repositoryUrl
    *******************************************************************************/
   public MarketingProduct withRepositoryUrl(String repositoryUrl)
   {
      this.repositoryUrl = repositoryUrl;
      return (this);
   }


   /*******************************************************************************
    ** Getter for documentationUrl
    *******************************************************************************/
   public String getDocumentationUrl()
   {
      return (this.documentationUrl);
   }



   /*******************************************************************************
    ** Setter for documentationUrl
    *******************************************************************************/
   public void setDocumentationUrl(String documentationUrl)
   {
      this.documentationUrl = documentationUrl;
   }



   /*******************************************************************************
    ** Fluent setter for documentationUrl
    *******************************************************************************/
   public MarketingProduct withDocumentationUrl(String documentationUrl)
   {
      this.documentationUrl = documentationUrl;
      return (this);
   }


   /*******************************************************************************
    ** Getter for releasesUrl
    *******************************************************************************/
   public String getReleasesUrl()
   {
      return (this.releasesUrl);
   }



   /*******************************************************************************
    ** Setter for releasesUrl
    *******************************************************************************/
   public void setReleasesUrl(String releasesUrl)
   {
      this.releasesUrl = releasesUrl;
   }



   /*******************************************************************************
    ** Fluent setter for releasesUrl
    *******************************************************************************/
   public MarketingProduct withReleasesUrl(String releasesUrl)
   {
      this.releasesUrl = releasesUrl;
      return (this);
   }


   /*******************************************************************************
    ** Getter for license
    *******************************************************************************/
   public String getLicense()
   {
      return (this.license);
   }



   /*******************************************************************************
    ** Setter for license
    *******************************************************************************/
   public void setLicense(String license)
   {
      this.license = license;
   }



   /*******************************************************************************
    ** Fluent setter for license
    *******************************************************************************/
   public MarketingProduct withLicense(String license)
   {
      this.license = license;
      return (this);
   }


   /*******************************************************************************
    ** Getter for platforms
    *******************************************************************************/
   public String getPlatforms()
   {
      return (this.platforms);
   }



   /*******************************************************************************
    ** Setter for platforms
    *******************************************************************************/
   public void setPlatforms(String platforms)
   {
      this.platforms = platforms;
   }



   /*******************************************************************************
    ** Fluent setter for platforms
    *******************************************************************************/
   public MarketingProduct withPlatforms(String platforms)
   {
      this.platforms = platforms;
      return (this);
   }


   /*******************************************************************************
    ** Getter for latestRelease
    *******************************************************************************/
   public String getLatestRelease()
   {
      return (this.latestRelease);
   }



   /*******************************************************************************
    ** Setter for latestRelease
    *******************************************************************************/
   public void setLatestRelease(String latestRelease)
   {
      this.latestRelease = latestRelease;
   }



   /*******************************************************************************
    ** Fluent setter for latestRelease
    *******************************************************************************/
   public MarketingProduct withLatestRelease(String latestRelease)
   {
      this.latestRelease = latestRelease;
      return (this);
   }


   /*******************************************************************************
    ** Getter for adoptionPath
    *******************************************************************************/
   public String getAdoptionPath()
   {
      return (this.adoptionPath);
   }



   /*******************************************************************************
    ** Setter for adoptionPath
    *******************************************************************************/
   public void setAdoptionPath(String adoptionPath)
   {
      this.adoptionPath = adoptionPath;
   }



   /*******************************************************************************
    ** Fluent setter for adoptionPath
    *******************************************************************************/
   public MarketingProduct withAdoptionPath(String adoptionPath)
   {
      this.adoptionPath = adoptionPath;
      return (this);
   }


   /*******************************************************************************
    ** Getter for valuePropositions
    *******************************************************************************/
   public String getValuePropositions()
   {
      return (this.valuePropositions);
   }



   /*******************************************************************************
    ** Setter for valuePropositions
    *******************************************************************************/
   public void setValuePropositions(String valuePropositions)
   {
      this.valuePropositions = valuePropositions;
   }



   /*******************************************************************************
    ** Fluent setter for valuePropositions
    *******************************************************************************/
   public MarketingProduct withValuePropositions(String valuePropositions)
   {
      this.valuePropositions = valuePropositions;
      return (this);
   }


   /*******************************************************************************
    ** Getter for ownerUserId
    *******************************************************************************/
   public String getOwnerUserId()
   {
      return (this.ownerUserId);
   }



   /*******************************************************************************
    ** Setter for ownerUserId
    *******************************************************************************/
   public void setOwnerUserId(String ownerUserId)
   {
      this.ownerUserId = ownerUserId;
   }



   /*******************************************************************************
    ** Fluent setter for ownerUserId
    *******************************************************************************/
   public MarketingProduct withOwnerUserId(String ownerUserId)
   {
      this.ownerUserId = ownerUserId;
      return (this);
   }


   /*******************************************************************************
    ** Getter for lastVerifiedDate
    *******************************************************************************/
   public LocalDate getLastVerifiedDate()
   {
      return (this.lastVerifiedDate);
   }



   /*******************************************************************************
    ** Setter for lastVerifiedDate
    *******************************************************************************/
   public void setLastVerifiedDate(LocalDate lastVerifiedDate)
   {
      this.lastVerifiedDate = lastVerifiedDate;
   }



   /*******************************************************************************
    ** Fluent setter for lastVerifiedDate
    *******************************************************************************/
   public MarketingProduct withLastVerifiedDate(LocalDate lastVerifiedDate)
   {
      this.lastVerifiedDate = lastVerifiedDate;
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
   public MarketingProduct withCreateDate(Instant createDate)
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
   public MarketingProduct withModifyDate(Instant modifyDate)
   {
      this.modifyDate = modifyDate;
      return (this);
   }

}
