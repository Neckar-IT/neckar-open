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

import it.neckar.financial.currency.QuantityUnit
import it.neckar.open.unit.number.Positive
import it.neckar.open.unit.other.B
import it.neckar.price.ArticleConditions
import it.neckar.price.ArticleConditionsWithAccessory
import it.neckar.price.ArticleConditionsWithInstallationTime
import it.neckar.price.BareArticleConditions
import it.neckar.price.FullArticleConditions
import it.neckar.price.rest.views.ArticleConditionsPriceSummary
import it.neckar.rest.RestRequest
import kotlinx.serialization.Serializable

/**
 * The article conditions an add-conditions form currently holds, for its price-overview companion
 * ([ArticleConditionsPriceSummary.calculate]). Every value may still be missing, see
 * [ArticleConditionsPriceSummaryResponse].
 */
@Serializable
data class ArticleConditionsPriceSummaryRequest(
  val part: EnteredPriceWithProfit,
  /** `null` is per piece, as in [it.neckar.price.ArticleConditions.unit]. */
  val unit: QuantityUnit?,
  /** `null` while the accessory checkbox is unchecked. */
  val accessory: EnteredPriceWithProfit?,
  /** `null` while the installation-time checkbox is unchecked. */
  val installation: EnteredInstallationTime?,
) : RestRequest {
  companion object {
    /** The largest body the price-summary route accepts: three entered prices and a unit, with headroom */
    val MaximumBodySize: @B @Positive Long = 16L * 1024

    /**
     * The request a form holding [conditions] would send: a stored `null` accessory or installation time is
     * an unchecked checkbox, so companion and conditions table price the same conditions alike.
     */
    fun of(conditions: ArticleConditions): ArticleConditionsPriceSummaryRequest {
      val (accessory, installationTime) = when (conditions) {
        is BareArticleConditions -> null to null
        is ArticleConditionsWithAccessory -> conditions.accessory to null
        is ArticleConditionsWithInstallationTime -> null to conditions.installationTime
        is FullArticleConditions -> conditions.accessory to conditions.installationTime
        else -> throw IllegalArgumentException("No price summary for the conditions implementation <${conditions::class.simpleName}>")
      }

      return ArticleConditionsPriceSummaryRequest(
        part = EnteredPriceWithProfit.of(conditions.part),
        unit = conditions.unit,
        accessory = accessory?.let { EnteredPriceWithProfit.of(it) },
        installation = installationTime?.let { EnteredInstallationTime(it) },
      )
    }
  }
}
