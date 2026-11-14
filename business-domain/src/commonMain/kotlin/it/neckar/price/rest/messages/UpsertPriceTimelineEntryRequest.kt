/*
 * Copyright (C) 2013-2026 Neckar IT GmbH, Mössingen, Germany
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Linking this library statically or dynamically with other modules is
 * making a combined work based on this library. Thus, the terms and
 * conditions of the GNU General Public License cover the whole combination.
 *
 * As a special exception, the copyright holders of this library give you
 * permission to link this library with independent modules, regardless of
 * the license terms of these independent modules, and to copy and distribute
 * the resulting combined work under terms of your choice, provided that every
 * copy of the combined work is accompanied by a complete copy of the source
 * code of this library.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 */
package it.neckar.price.rest.messages

import it.neckar.financial.currency.Money
import it.neckar.open.kotlin.serializers.NativeInstant
import it.neckar.rest.RestRequest
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * Request body for `POST /api/<parent>/{id}/price-timeline` — shared across all parents
 * with a price timeline (e.g. `MembershipPlan`).
 *
 * The timing of the new entry is a discriminated union — matching the radio-button UI
 * ("ab sofort" vs. "ab einem bestimmten Datum"):
 *
 * - [Now] — the entry takes effect at the server's `now()`. The server is the single
 *   source of truth for "now"; the client does not send a timestamp.
 * - [ScheduledFor] — the caller explicitly chose a point in time. The server rejects
 *   values in the past with a `ValidFromInPast` failure on the parent's response,
 *   unless the parent's price timeline is still empty (initial seeding is allowed
 *   to backfill historical prices).
 *
 * An existing `validFrom` is never silently overwritten — duplicates return a
 * `DuplicateValidFrom` failure on the parent's response.
 */
@Serializable
sealed interface UpsertPriceTimelineEntryRequest : RestRequest {

  /** New price (common to both variants). */
  val price: Money.Net

  @Serializable
  @SerialName("now")
  data class Now(override val price: Money.Net) : UpsertPriceTimelineEntryRequest

  @Serializable
  @SerialName("scheduled")
  data class ScheduledFor(
    val validFrom: NativeInstant,
    override val price: Money.Net,
  ) : UpsertPriceTimelineEntryRequest
}
