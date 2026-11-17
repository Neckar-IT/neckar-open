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

import it.neckar.financial.currency.PricePerHour
import it.neckar.open.annotations.DataView
import it.neckar.open.kotlin.serializers.NativeInstant
import it.neckar.price.ArticleConditions
import it.neckar.timeline.TimelineEntry
import kotlinx.serialization.Serializable

/**
 * One row of the conditions table. Kept apart from [ArticleConditionsTimelineEntryView], which the owner stores
 * in its summary: a price stored there could not follow a change of the hourly rate.
 */
@DataView("Row of the article-conditions timeline table: the entry with its price overview")
@Serializable
data class ArticleConditionsTimelineTableEntry<Conditions : ArticleConditions>(
  val conditions: Conditions,
  val validFrom: NativeInstant,
  val priceSummary: ArticleConditionsPriceSummary,
)

fun <Conditions : ArticleConditions> TimelineEntry<Conditions>.toTableEntry(hourlyRate: PricePerHour?): ArticleConditionsTimelineTableEntry<Conditions> =
  tableEntryOf(value, validFrom, hourlyRate)

fun <Conditions : ArticleConditions> ArticleConditionsTimelineEntryView<Conditions>.toTableEntry(hourlyRate: PricePerHour?): ArticleConditionsTimelineTableEntry<Conditions> =
  tableEntryOf(conditions, validFrom, hourlyRate)

private fun <Conditions : ArticleConditions> tableEntryOf(conditions: Conditions, validFrom: NativeInstant, hourlyRate: PricePerHour?): ArticleConditionsTimelineTableEntry<Conditions> {
  return ArticleConditionsTimelineTableEntry(
    conditions = conditions,
    validFrom = validFrom,
    priceSummary = ArticleConditionsPriceSummary.calculate(conditions, hourlyRate),
  )
}
