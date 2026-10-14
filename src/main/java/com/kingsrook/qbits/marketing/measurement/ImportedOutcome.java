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
import com.kingsrook.qbits.marketing.model.enums.FunnelStage;


/*******************************************************************************
 ** One outcome reported by a MarketingOutcomeImporter.
 **
 ** @param stage           the funnel stage (required)
 ** @param eventType       the event type, matching a metric of the measurement plan (required)
 ** @param externalEventId the importer's own id for the outcome, stable across fetches (required)
 ** @param actorKey        an anonymous, stable actor id (null when unknown)
 ** @param occurredAt      when it happened (null: when it was imported)
 ** @param quantity        how many (null: 1)
 ** @param isTestData      true for a fixture (kept out of the real numbers)
 ** @param note            a short note (null for none)
 *******************************************************************************/
public record ImportedOutcome(FunnelStage stage, String eventType, String externalEventId, String actorKey, Instant occurredAt, Integer quantity,
                              Boolean isTestData, String note)
{
}
