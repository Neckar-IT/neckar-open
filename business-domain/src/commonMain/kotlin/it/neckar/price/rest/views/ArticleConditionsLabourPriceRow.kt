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
import it.neckar.open.annotations.DataView
import it.neckar.open.annotations.GermanTerm
import it.neckar.price.rest.messages.EnteredInstallationTime
import kotlinx.serialization.Serializable
import kotlin.time.Duration

/**
 * The labour position of an [ArticleConditionsPriceSummary]. Carries no purchase price and no surcharge:
 * the hourly rate is a selling rate, and labour cost is not modelled.
 */
@DataView("Labour position of the article-conditions price overview — installation time at the hourly rate")
@Serializable
data class ArticleConditionsLabourPriceRow(
  @GermanTerm("Montagezeit")
  val installationTime: Duration?,
  @GermanTerm("Stundensatz")
  val hourlyRate: PricePerHour,
  /** `null` while no installation time is entered. */
  @GermanTerm("Arbeitspreis")
  val sellingPrice: Money.Net?,
) {
  companion object {
    fun of(entered: EnteredInstallationTime, hourlyRate: PricePerHour): ArticleConditionsLabourPriceRow {
      return ArticleConditionsLabourPriceRow(
        installationTime = entered.installationTime,
        hourlyRate = hourlyRate,
        sellingPrice = entered.installationTime?.let { (hourlyRate * it).sellingPrice },
      )
    }
  }
}
