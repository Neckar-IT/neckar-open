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
package it.neckar.price.rest.views

import it.neckar.financial.currency.Money
import it.neckar.financial.currency.PricePerHour
import it.neckar.financial.currency.QuantityUnit
import it.neckar.open.annotations.DataView
import it.neckar.open.annotations.GermanTerm
import it.neckar.open.kotlin.lang.requireNotNull
import it.neckar.price.ArticleConditions
import it.neckar.price.rest.messages.ArticleConditionsPriceSummaryRequest
import kotlinx.serialization.Serializable

/**
 * The price overview of article conditions: one row per position they hold, and the customer price over all
 * of them, per [unit] and net. Calculated for a form companion and for each row of the conditions table,
 * never stored.
 */
@DataView("Price overview of article conditions: one row per position plus the customer price")
@Serializable
@GermanTerm("Konditionen-Übersicht")
data class ArticleConditionsPriceSummary(
  /** `null` is per piece, as in [it.neckar.price.ArticleConditions.unit]. */
  val unit: QuantityUnit?,
  @GermanTerm("Teil")
  val part: ArticleConditionsPriceRow,
  /** `null` where the conditions state no accessory. */
  @GermanTerm("Zubehör")
  val accessory: ArticleConditionsPriceRow?,
  /** `null` where the conditions state no installation time. */
  @GermanTerm("Arbeitszeit")
  val labour: ArticleConditionsLabourPriceRow?,
  /** Sum of every row's selling price; `null` while one of them is missing. */
  @GermanTerm("Endkundenpreis")
  val customerPrice: Money.Net?,
) {
  companion object {
    /**
     * Declared by every product's companion route, so each generated client names its function
     * `calculateArticleConditionsPriceSummary` and one shared page component serves all products.
     */
    const val CalculateOperationId: String = "CalculateArticleConditionsPriceSummary"

    /**
     * [hourlyRate] prices the installation time. A product whose articles are never mounted has none; a request
     * carrying an installation time there is a contradiction and fails.
     */
    fun calculate(request: ArticleConditionsPriceSummaryRequest, hourlyRate: PricePerHour?): ArticleConditionsPriceSummary {
      val part = ArticleConditionsPriceRow.of(request.part)
      val accessory = request.accessory?.let { ArticleConditionsPriceRow.of(it) }
      val labour = request.installation?.let { installation ->
        ArticleConditionsLabourPriceRow.of(installation, hourlyRate.requireNotNull { "Installation time <${installation.installationTime}> cannot be priced without an hourly rate" })
      }

      val sellingPrices: List<Money.Net?> = listOfNotNull(part, accessory).map { it.sellingPrice } + listOfNotNull(labour).map { it.sellingPrice }
      val customerPrice = if (sellingPrices.contains(null)) null else Money.Net.fastSumBy(sellingPrices.filterNotNull()) { it }

      return ArticleConditionsPriceSummary(unit = request.unit, part = part, accessory = accessory, labour = labour, customerPrice = customerPrice)
    }

    /** Stored [conditions], see [ArticleConditionsPriceSummaryRequest.of]. */
    fun calculate(conditions: ArticleConditions, hourlyRate: PricePerHour?): ArticleConditionsPriceSummary {
      return calculate(ArticleConditionsPriceSummaryRequest.of(conditions), hourlyRate)
    }
  }
}
