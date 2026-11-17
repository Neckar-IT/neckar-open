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
package it.neckar.financial.currency

import it.neckar.financial.currency.io.MoneyFractionAsCentsSerializer
import it.neckar.open.annotations.CrossLayer
import it.neckar.open.collections.fastSumByLong
import it.neckar.open.formatting.formatCents
import it.neckar.open.formatting.formatEuro
import it.neckar.open.i18n.CurrentI18nConfiguration
import it.neckar.open.i18n.I18nConfiguration
import it.neckar.open.kotlin.lang.WhitespaceConfig
import it.neckar.open.kotlin.lang.requireFinite
import it.neckar.open.unit.currency.EUR
import it.neckar.open.unit.currency.cct
import it.neckar.open.unit.currency.ct
import it.neckar.open.unit.number.IsFinite
import it.neckar.open.unit.other.pct
import kotlinx.serialization.Serializable

/**
 * Serialized as CENTS (`179.0` for 1,79 €) by [MoneyFractionAsCentsSerializer] — the base unit money
 * travels in here, the same one [Money] uses and the one the frontend declares in
 * `quantities/conversionFactors.ts`. A value class wrapping this one inherits that format and needs
 * no serializer of its own.
 */
@CrossLayer("Money value object — identical shape across Domain, Mongo entity, and REST layers")
@Serializable(with = MoneyFractionAsCentsSerializer::class)
data class MoneyFraction
/**
 * Do not call this constructor directly.
 * Instead, use the factory methods.
 */
internal constructor(
  /**
   * The value in centi-cents (1/100 of a cent)
   */
  val centiCents: @cct Long,
) : Comparable<MoneyFraction> {

  /**
   * Returns a double value that represents the cents value.
   *
   * ATTENTION: Do *NOT* use this value for calculations to avoid rounding errors.
   */
  val cents: @ct Double
    get() = centiCents / CentFactor.toDouble()

  /**
   * Returns a double value that represents the euro value.
   *
   * ATTENTION: Do *NOT* use this value for calculations to avoid rounding errors.
   */
  val euros: @EUR Double
    get() = centiCents / EuroFactor.toDouble()

  /**
   * Formats this value as cents
   */
  @Deprecated("Formats as cents - use the method that makes it clear that it formats as cents")
  fun format(i18nConfiguration: I18nConfiguration = CurrentI18nConfiguration, whitespaceConfig: WhitespaceConfig = WhitespaceConfig.NonBreaking): String {
    return formatAsCents(i18nConfiguration, whitespaceConfig)
  }

  fun formatAsCents(i18nConfiguration: I18nConfiguration, whitespaceConfig: WhitespaceConfig): String {
    return cents.formatCents(i18nConfiguration, whitespaceConfig)
  }

  fun formatAsEuros(i18nConfiguration: I18nConfiguration = CurrentI18nConfiguration, whitespaceConfig: WhitespaceConfig = WhitespaceConfig.NonBreaking): String {
    return euros.formatEuro(i18nConfiguration, whitespaceConfig)
  }

  /**
   * Returns the fallback value if the value is "0.0"
   */
  fun formatOrIfZero(fallbackIfZero: String, i18nConfiguration: I18nConfiguration = CurrentI18nConfiguration, whitespaceConfig: WhitespaceConfig = WhitespaceConfig.NonBreaking): String {
    if (euros == 0.0) return fallbackIfZero
    return format(i18nConfiguration, whitespaceConfig)
  }

  override operator fun compareTo(other: MoneyFraction): Int {
    return this.centiCents.compareTo(other.centiCents)
  }

  operator fun minus(other: MoneyFraction): MoneyFraction {
    return centiCents(this.centiCents - other.centiCents)
  }

  operator fun plus(other: MoneyFraction): MoneyFraction {
    return centiCents(this.centiCents + other.centiCents)
  }

  operator fun div(other: MoneyFraction): @pct Double {
    require(other.centiCents != 0L) { "Cannot divide by zero money fraction" }
    return this.centiCents.toDouble() / other.centiCents.toDouble()
  }

  fun divAndRound(other: Double): MoneyFraction {
    other.requireFinite()
    require(other != 0.0) { "Cannot divide money by zero" }
    return centiCents((centiCents.toExactDouble() / other).roundInExactRange(RoundingStrategy.Default))
  }

  fun timesAndRound(factor: Double): MoneyFraction {
    factor.requireFinite()
    return centiCents((centiCents.toExactDouble() * factor).roundInExactRange(RoundingStrategy.Default))
  }

  operator fun times(factor: Int): MoneyFraction {
    return centiCents(centiCents.timesExact(factor))
  }

  operator fun unaryMinus(): MoneyFraction {
    return centiCents(-centiCents)
  }

  /**
   * Calculates the delta value - for debugging purposes
   */
  internal fun delta(other: MoneyFraction): @cct Long {
    val deltaInCentiCents = this.centiCents - other.centiCents
    return deltaInCentiCents
  }

  fun isZero(): Boolean {
    return centiCents == 0L
  }

  fun roundToCents(): Money {
    return Money(RoundingStrategy.Default.roundQuotient(centiCents, CentFactor.toLong()))
  }

  override fun toString(): String {
    return formatAsCents(I18nConfiguration.US, WhitespaceConfig.Spaces)
  }

  companion object {
    val Zero: MoneyFraction = centiCents(0)

    const val CentFactor: Int = 100

    const val EuroFactor: Int = CentFactor * 100

    fun centiCents(centiCents: @cct Int): MoneyFraction {
      return MoneyFraction(centiCents = centiCents.toLong())
    }

    fun centiCents(centiCents: @cct Long): MoneyFraction {
      return MoneyFraction(centiCents = centiCents)
    }

    fun cents(cents: @IsFinite @ct Double): MoneyFraction {
      cents.requireFinite { "Finite value required but was <$cents>" }

      val centiCents = RoundingStrategy.Default.roundToLong(cents * CentFactor)
      return MoneyFraction(centiCents = centiCents)
    }

    fun cents(cents: @ct Int): MoneyFraction {
      val centiCents = cents.toLong() * CentFactor
      return MoneyFraction(centiCents = centiCents)
    }

    fun cents(cents: @ct Long): MoneyFraction {
      return MoneyFraction(centiCents = cents.timesExact(CentFactor))
    }

    /**
     * Rounds to a centi-cent.
     */
    fun euros(euros: @IsFinite @EUR Double): MoneyFraction {
      euros.requireFinite { "Finite value required but was <$euros>" }

      @cct val centiCents = RoundingStrategy.Default.roundToLong(euros * EuroFactor)
      return MoneyFraction(centiCents)
    }

    /**
     * Calculates the sum for the given elements
     */
    inline fun <T> fastSumBy(elements: List<T>, callback: (value: T) -> MoneyFraction): MoneyFraction {
      val centiCents = elements.fastSumByLong { callback(it).centiCents }
      return centiCents(centiCents)
    }
  }
}

val @EUR Double.euro100: MoneyFraction
  get() = MoneyFraction.euros(this)

val @EUR Int.euro100: MoneyFraction
  get() = MoneyFraction.euros(this.toDouble())

val @ct Double.cents100: MoneyFraction
  get() = MoneyFraction.cents(this)

val @ct Int.cents100: MoneyFraction
  get() = MoneyFraction.cents(this)

fun Iterable<MoneyFraction>.sum(): MoneyFraction {
  val totalCentiCents = sumOf { it.centiCents }
  return MoneyFraction.centiCents(totalCentiCents)
}
