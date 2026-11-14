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
package it.neckar.financial.priced

import it.neckar.commons.tags.Tags
import it.neckar.financial.currency.Money
import it.neckar.financial.currency.VatRate
import it.neckar.financial.currency.sum
import it.neckar.financial.quote.Amount
import it.neckar.financial.quote.Optionality
import it.neckar.financial.quote.Visibility
import it.neckar.financial.quote.query.Query
import it.neckar.open.collections.fastMap
import it.neckar.open.time.sum
import it.neckar.open.unit.currency.EUR
import it.neckar.open.unit.other.pct
import it.neckar.uuid.NativeUuid
import it.neckar.uuid.randomForEnvironment
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.uuid.Uuid

/**
 * A section within a quote that contains further [PricedElement]s.
 *
 * Both values are stated rather than derived on read, so every reader sees the same numbers; [copy] carries them over unchanged.
 */
@Serializable
@SerialName("PricedSection")
data class PricedSection(
  override val uuid: NativeUuid,

  override val headline: String,

  override val amount: Amount? = null,

  /**
   * Only carried, never applied: [netPrice] sums children that have their own factor applied already.
   */
  override val factor: @pct Double? = null,

  override val visibility: Visibility,

  override val details: String? = null,

  /**
   * All children share one visibility.
   */
  val children: List<PricedElement>,

  override val optionality: Optionality,

  override val netPrice: @EUR Money.Net,

  ) : PricedElement {

  init {
    require(headline.isNotBlank()) {
      "Headline must not be blank"
    }

    require(
      children.asSequence()
        .distinctBy { it.visibility }
        .count() <= 1
    ) {
      "Only children with the same visibility are supported! But was:\n${children.joinToString("\n") { "- ${it.visibility.name}: ${it.headline}" }}"
    }
  }

  override fun queryPricedElement(query: Query): PricedSection? {
    val queriedChildren = children.mapNotNull { it.queryPricedElement(query) }
    if (queriedChildren.isEmpty()) return null
    if (queriedChildren == children) return this
    //A section over other children gets its own values - a value set here describes the unfiltered section
    return copy(children = queriedChildren, optionality = calculateOptionality(queriedChildren), netPrice = calculateNetPrice(queriedChildren))
  }

  override fun listPricedItems(query: Query): List<PricedItem> {
    return children.flatMap { it.listPricedItems(query) }
  }

  override fun vats(query: Query): List<VatRate> {
    return children.flatMap { it.vats(query) }.distinct().sortedBy { it.vat }
  }

  override fun workingTimes(query: Query): Duration {
    return children.fastMap { it.workingTimes(query) }.sum()
  }

  override fun relevanceOrNull(): Tags? {
    return null
  }

  companion object {
    /**
     * Creates a section, calculating the values left null.
     */
    operator fun invoke(
      uuid: NativeUuid = Uuid.randomForEnvironment(),
      headline: String,
      amount: Amount? = null,
      factor: @pct Double? = null,
      visibility: Visibility,
      details: String? = null,
      children: List<PricedElement>,
      optionality: Optionality? = null,
      netPrice: @EUR Money.Net? = null,
    ): PricedSection = PricedSection(
      uuid = uuid,
      headline = headline,
      amount = amount,
      factor = factor,
      visibility = visibility,
      details = details,
      children = children,
      optionality = optionality ?: calculateOptionality(children),
      netPrice = netPrice ?: calculateNetPrice(children),
    )

    /**
     * A single mandatory child makes the whole section mandatory, and a section without children counts as optional.
     */
    fun calculateOptionality(children: List<PricedElement>): Optionality =
      if (children.all { it.optionality == Optionality.Optional }) Optionality.Optional else Optionality.Mandatory

    /**
     * The sum over all children - unfiltered, and without applying [PricedSection.factor].
     */
    fun calculateNetPrice(children: List<PricedElement>): @EUR Money.Net = children.fastMap { it.netPrice }.sum()
  }
}
