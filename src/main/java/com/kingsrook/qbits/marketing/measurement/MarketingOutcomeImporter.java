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

package com.kingsrook.qbits.marketing.measurement;


import java.time.Instant;
import java.util.List;
import com.kingsrook.qqq.backend.core.exceptions.QException;
import com.kingsrook.qqq.backend.core.model.data.QRecord;


/*******************************************************************************
 ** A source of outcome events (an analytics export, a store listing, a package
 ** registry, ...) that the host registers with
 ** MarketingQBitConfig.withOutcomeImporter(...). The Import Outcomes campaign
 ** action runs every registered importer.
 **
 ** The qbit ships no concrete network importer: there is no live analytics
 ** provider in 0.1.0, and nothing is "measured" until one records events.
 **
 ** An importer only reports what it observed; Marketing stores each outcome
 ** through the normal insert path, so the (source, external id) dedupe applies:
 ** the source is the importer's name, and an outcome fetched again is dropped,
 ** not counted twice. So fetch may safely return events it returned before.
 *******************************************************************************/
public interface MarketingOutcomeImporter
{

   /*******************************************************************************
    ** The importer's name: unique among importers, and the "source" of every
    ** event it imports (so keep it stable). "marketing" and "crm" are reserved.
    *******************************************************************************/
   String getName();

   /*******************************************************************************
    ** The outcomes of a campaign observed at or after since (null: all it can
    ** provide). Marketing passes the occurredAt of the newest event this
    ** importer already recorded for the campaign. Return an empty list when
    ** there is nothing. Never return personal data: the actorKey must be an
    ** anonymous, stable id.
    *******************************************************************************/
   List<ImportedOutcome> fetch(QRecord campaign, Instant since) throws QException;

}
