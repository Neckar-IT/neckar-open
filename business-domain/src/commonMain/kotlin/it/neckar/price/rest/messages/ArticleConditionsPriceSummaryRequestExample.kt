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

import it.neckar.financial.currency.PricePerHour
import it.neckar.financial.currency.euroNet
import kotlin.time.Duration.Companion.minutes

object ArticleConditionsPriceSummaryRequestExample {
  /** The rate [full] is priced at, and the one every conditions-table OpenAPI example prices labour at. */
  val hourlyRate: PricePerHour = PricePerHour.euros(75.0)

  /** Part, accessory and half an hour of installation — 25,00 € + 4,50 € + 37,50 € = 67,00 € at [hourlyRate]. */
  val full: ArticleConditionsPriceSummaryRequest = ArticleConditionsPriceSummaryRequest(
    part = EnteredPriceWithProfit(inputPrice = 20.00.euroNet, sellingPrice = 25.00.euroNet),
    unit = null,
    accessory = EnteredPriceWithProfit(inputPrice = 3.00.euroNet, sellingPrice = 4.50.euroNet),
    installation = EnteredInstallationTime(installationTime = 30.minutes),
  )

  /** An article that is neither shipped with accessory nor mounted — a book. */
  val partOnly: ArticleConditionsPriceSummaryRequest = ArticleConditionsPriceSummaryRequest(
    part = EnteredPriceWithProfit(inputPrice = 12.00.euroNet, sellingPrice = 18.00.euroNet),
    unit = null,
    accessory = null,
    installation = null,
  )
}
