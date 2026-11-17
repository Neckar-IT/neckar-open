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
package it.neckar.price

import it.neckar.timeline.TimelineEntry
import it.neckar.financial.currency.QuantityUnit
import it.neckar.financial.currency.PriceWithProfit
import it.neckar.financial.currency.euroNet
import it.neckar.open.time.TimeInstantExample
import kotlin.time.Duration.Companion.hours

object ArticleConditionsExample {
  /**
   * Conditions with an explicit unit, an accessory and an installation time - e.g. a cable sold per
   * metre. Exercises the explicit-[QuantityUnit] case.
   */
  val withAccessoryAndInstallationTime: FullArticleConditions = FullArticleConditions(
    part = PriceWithProfit(inputPrice = 1.20.euroNet, sellingPrice = 2.50.euroNet),
    unit = QuantityUnit("Meter"),
    accessory = PriceWithProfit(inputPrice = 0.15.euroNet, sellingPrice = 0.21.euroNet),
    installationTime = 8.5.hours,
  )

  /**
   * The same article type without the two optional parts filled in, priced per piece - the cable
   * variant that ships without small material and is not mounted. Exercises the `unit == null` case
   * and the "an article of this type need not have every property" case.
   */
  val withoutAccessoryAndInstallationTime: FullArticleConditions = FullArticleConditions(
    part = PriceWithProfit(inputPrice = 120.00.euroNet, sellingPrice = 168.00.euroNet),
    accessory = null,
    installationTime = null,
  )

  /**
   * A timeline of one article type, so both entries have the same implementation - only the values
   * differ.
   */
  val articleConditionsTimelineExample: ArticleConditionsTimeline<FullArticleConditions> = ArticleConditionsTimeline(
    entries = listOf(
      TimelineEntry(
        value = withAccessoryAndInstallationTime,
        validFrom = TimeInstantExample._2026_04_24_03_10_00_000Z,
      ),
      TimelineEntry(
        value = withoutAccessoryAndInstallationTime,
        validFrom = TimeInstantExample._2020_05_21_15_00_41_500Z,
      ),
    )
  )
}
