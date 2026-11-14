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
@file:UseSerializers(UuidSerializer::class)

package it.neckar.financial.priced

import it.neckar.commons.tags.Tags
import it.neckar.financial.currency.Money
import it.neckar.financial.currency.VatRate
import it.neckar.financial.quote.Amount
import it.neckar.financial.quote.Optionality
import it.neckar.financial.quote.Visibility
import it.neckar.financial.quote.query.Query
import it.neckar.open.unit.currency.EUR
import it.neckar.open.unit.other.pct
import it.neckar.uuid.HasUuid
import it.neckar.uuid.UuidSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.UseSerializers
import kotlin.time.Duration

/**
 * A node of the priced tree: a [PricedSection] or a [PricedItem].
 */
@Serializable
sealed interface PricedElement : HasUuid {
  /**
   * The (bold) headline
   */
  val headline: String

  val amount: Amount?

  /**
   * The multiplier for the price - a fraction, *not* a number out of 100: `1.0` means 100 %, `0.9` means 90 %.
   * That is what [it.neckar.open.kotlin.lang.percent] produces (`90.percent == 0.9`).
   *
   * Null means no factor has been set.
   */
  val factor: @pct Double?

  /**
   * A [Visibility.Internal] element is summed up but never shown to the customer.
   */
  val visibility: Visibility

  val optionality: Optionality

  val details: String?


  /**
   * The net price of this element. Unlike the query methods below this is never filtered:
   * a [PricedSection] covers *all* its children, optional and internal ones included.
   */
  val netPrice: @EUR Money.Net


  /**
   * Filters this element by [query] and returns what is left of it - a [PricedItem] returns itself,
   * a [PricedSection] a copy that holds only its matching children.
   *
   * Null means nothing here matched, so the caller drops the element. For a [PricedSection] this is
   * the only way it can be dropped: the query is never applied to the section itself, only to its
   * children, so a section survives whenever at least one child does.
   */
  fun queryPricedElement(query: Query): PricedElement?

  /**
   * Flattens this element into the [PricedItem] leaves matching [query] - empty if none match.
   * This is the method to use for sums: a [PricedSection] contributes its items, never itself.
   */
  fun listPricedItems(query: Query): List<PricedItem>

  /**
   * The distinct VAT rates of the items matching [query], ascending by rate.
   */
  fun vats(query: Query): List<VatRate>

  fun workingTimes(query: Query): Duration

  /**
   * The relevance of a [PricedItem], null for a [PricedSection].
   */
  fun relevanceOrNull(): Tags?
}
