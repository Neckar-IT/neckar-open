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
package it.neckar.financial.quote

import it.neckar.financial.currency.Money
import it.neckar.financial.currency.PriceWithProfit
import it.neckar.open.formatting.CachedNumberFormat
import it.neckar.open.formatting.appendUnit
import it.neckar.open.formatting.cached
import it.neckar.open.formatting.decimalFormat
import it.neckar.open.i18n.CurrentI18nConfiguration
import it.neckar.open.i18n.I18nConfiguration
import it.neckar.open.kotlin.lang.WhitespaceConfig
import it.neckar.open.time.formatHourAndMinutes
import kotlinx.serialization.Serializable
import kotlin.time.Duration.Companion.hours

/**
 * TODO: Replace with formatter instance/callback?
 */
enum class AmountUnit {
  None,
  Hours,
  KiloMeters,
  Meters,
  SquareMeters,
}

/**
 * Represents an amount (with optional unit) for priced items
 */
@Serializable
data class Amount(
  val amount: Double,
  val unit: AmountUnit = AmountUnit.None,
) {

  constructor(amount: Int, unit: AmountUnit = AmountUnit.None) : this(amount.toDouble(), unit)

  operator fun times(factor: Int): Amount {
    return copy(amount = amount * factor)
  }

  operator fun times(factor: Double): Amount {
    return copy(amount = amount * factor)
  }

  fun format(i18nConfiguration: I18nConfiguration = CurrentI18nConfiguration, whitespaceConfig: WhitespaceConfig = WhitespaceConfig.NonBreaking): String {
    return when (unit) {
      AmountUnit.None -> decimalFormat.format(amount, i18nConfiguration, whitespaceConfig)
      AmountUnit.Hours -> amount.hours.formatHourAndMinutes(whitespaceConfig)
      AmountUnit.KiloMeters -> kiloMetersFormat.format(amount, i18nConfiguration, whitespaceConfig)
      AmountUnit.Meters -> metersFormat.format(amount, i18nConfiguration, whitespaceConfig)
      AmountUnit.SquareMeters -> squareMetersFormat.format(amount, i18nConfiguration, whitespaceConfig)
    }
  }

  companion object {
    val kiloMetersFormat: CachedNumberFormat = decimalFormat.appendUnit("km").cached()
    val metersFormat: CachedNumberFormat = decimalFormat.appendUnit("m").cached()
    val squareMetersFormat: CachedNumberFormat = decimalFormat.appendUnit("m²").cached()
  }
}

operator fun PriceWithProfit.times(amount: Amount): PriceWithProfit {
  return this.timesAndRound(amount.amount)
}

operator fun PriceWithProfit.div(amount: Amount): PriceWithProfit {
  return this.divAndRound(amount.amount)
}

operator fun Money.Net.times(amount: Amount): Money.Net {
  return this.timesAndRound(amount.amount)
}

operator fun Money.Net.div(amount: Amount): Money.Net {
  return this.divAndRound(amount.amount)
}
