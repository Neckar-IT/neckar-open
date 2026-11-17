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

import it.neckar.commons.tags.Tag
import it.neckar.commons.tags.Tags
import it.neckar.financial.currency.Money
import it.neckar.financial.currency.PriceWithProfit
import it.neckar.financial.currency.VatRate
import it.neckar.financial.quote.Amount
import it.neckar.financial.quote.AmountUnit
import it.neckar.financial.quote.Optionality
import it.neckar.financial.quote.Visibility
import it.neckar.financial.quote.times
import it.neckar.financial.quote.query.Query
import it.neckar.open.kotlin.lang.percent
import it.neckar.open.unit.currency.EUR
import it.neckar.open.unit.other.pct
import it.neckar.uuid.NativeUuid
import it.neckar.uuid.randomForEnvironment
import it.neckar.uuid.randomUuid4
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.uuid.Uuid

/**
 * Represents an item that has a concrete price ("leaf" of the tree).
 *
 * Both prices are stated rather than derived on read, so every reader sees the same numbers; [copy] carries them over unchanged.
 */
@Serializable
@SerialName("PricedItem")
data class PricedItem(
  override val uuid: NativeUuid,

  /**
   * The (bold) headline
   */
  override val headline: String,
  override val details: String?,

  val priceForOneElement: PriceWithProfit,

  override val amount: Amount,

  /**
   * Multiplies the selling price for the [netPrice]: a discount below 100 %, a surcharge above it.
   * Both belong here rather than in [priceForOneElement], where they would round once per element instead of once.
   */
  override val factor: @pct Double = 100.percent,

  override val visibility: Visibility,

  /**
   * Selects the partial sums this item counts into, see [it.neckar.financial.quote.query.CalculationRelevanceQuery].
   */
  val relevance: Tags,

  override val optionality: Optionality = Optionality.Mandatory,

  val vatRate: VatRate,

  /**
   * The price for all elements, as opposed to [priceForOneElement] for one.
   */
  val sellingPrice: @EUR Money.Net,

  override val netPrice: @EUR Money.Net,

  ) : PricedElement {

  init {
    //"<-empty->" is a placeholder from older documents, never a headline
    check(headline != "<-empty->") { "Invalid headline: [$headline]" }
  }

  val priceWithProfit: @EUR PriceWithProfit
    get() = PriceWithProfit(
      inputPrice = priceForOneElement.inputPrice * amount,
      sellingPrice = netPrice,
    )

  /**
   * How far [netPrice] sits below [sellingPrice], so a reduction is a *negative* amount - as in [PricedElements.discount].
   */
  val discount: @EUR Money.Net
    get() = netPrice - sellingPrice

  override fun relevanceOrNull(): Tags {
    return relevance
  }

  override fun queryPricedElement(query: Query): PricedItem? {
    return if (query.matches(this)) this else null
  }

  override fun listPricedItems(query: Query): List<PricedItem> {
    return if (query.matches(this)) listOf(this) else emptyList()
  }

  override fun vats(query: Query): List<VatRate> {
    return if (query.matches(this)) listOf(vatRate) else emptyList()
  }

  override fun workingTimes(query: Query): Duration {
    return amount.let { if (it.unit == AmountUnit.Hours) it.amount.hours else Duration.ZERO }
  }

  fun isRelevantFor(tag: Tag): Boolean = this.relevance.contains(tag)


  companion object {
    /**
     * Creates an item, calculating the prices left null.
     */
    operator fun invoke(
      uuid: NativeUuid = Uuid.randomForEnvironment(),
      headline: String,
      details: String?,
      priceForOneElement: PriceWithProfit,
      amount: Amount,
      factor: @pct Double = 100.percent,
      visibility: Visibility,
      relevance: Tags,
      optionality: Optionality = Optionality.Mandatory,
      vatRate: VatRate,
      // Null rather than a calculated default: a default would leave the parameter nullable and the
      // argument null, so the call below would resolve back to this factory instead of the constructor.
      sellingPrice: @EUR Money.Net? = null,
      netPrice: @EUR Money.Net? = null,
    ): PricedItem {
      val resolvedSellingPrice = sellingPrice ?: calculateSellingPrice(priceForOneElement, amount)

      return PricedItem(
        uuid = uuid,
        headline = headline,
        details = details,
        priceForOneElement = priceForOneElement,
        amount = amount,
        factor = factor,
        visibility = visibility,
        relevance = relevance,
        optionality = optionality,
        vatRate = vatRate,
        sellingPrice = resolvedSellingPrice,
        netPrice = netPrice ?: calculateNetPrice(resolvedSellingPrice, factor),
      )
    }

    fun createEmpty(uuid: Uuid = randomUuid4()): PricedItem = PricedItem(
      uuid = uuid,
      headline = "",
      details = "",
      priceForOneElement = PriceWithProfit.Zero,
      amount = Amount(1),
      visibility = Visibility.Public,
      relevance = Tags.empty,
      vatRate = VatRate.Zero,
      optionality = Optionality.Optional,
    )

    fun calculateSellingPrice(priceForOneElement: PriceWithProfit, amount: Amount): @EUR Money.Net = priceForOneElement.sellingPrice * amount

    fun calculateNetPrice(sellingPrice: @EUR Money.Net, factor: @pct Double): @EUR Money.Net = sellingPrice.timesAndRound(factor)
  }
}
