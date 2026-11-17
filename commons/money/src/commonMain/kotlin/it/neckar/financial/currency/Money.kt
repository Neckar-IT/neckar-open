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

import it.neckar.open.collections.fastSumByLong
import it.neckar.open.i18n.I18nConfiguration
import it.neckar.open.kotlin.lang.WhitespaceConfig
import it.neckar.open.unit.currency.EUR
import it.neckar.open.unit.currency.ct
import it.neckar.open.unit.number.IsFinite
import it.neckar.open.unit.other.pct
import kotlinx.serialization.EncodeDefault
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlin.jvm.JvmInline
import kotlin.jvm.JvmName
import kotlin.math.abs

/**
 * An amount without a tax basis: a loan sum, a subsidy, an earning. A price is a [Net] or a [Gross]; a tax amount is a [Vat].
 */
@Serializable
@SerialName("Money")
@JvmInline
value class Money(override val cents: @ct Long) : MoneyAmount, Comparable<Money> {
  override operator fun compareTo(other: Money): Int {
    return cents.compareTo(other.cents)
  }

  operator fun plus(other: Money): Money {
    return Money(cents + other.cents)
  }

  operator fun minus(other: Money): Money {
    return Money(cents - other.cents)
  }

  operator fun unaryMinus(): Money {
    return Money(-cents)
  }

  operator fun times(factor: Int): Money {
    return Money(cents.timesExact(factor))
  }

  fun timesAndRound(factor: Double, rounding: RoundingStrategy = RoundingStrategy.Default): Money {
    return Money(cents.timesAndRound(factor, rounding))
  }

  fun divAndRound(divisor: Double, rounding: RoundingStrategy = RoundingStrategy.Default): Money {
    return Money(cents.divAndRound(divisor, rounding))
  }

  operator fun div(other: Money): @pct Double {
    return cents.ratioTo(other.cents)
  }

  override fun toString(): String {
    return format(I18nConfiguration.US, WhitespaceConfig.Spaces)
  }

  companion object {
    val Zero: Money = Money(0)

    fun cents(cents: @ct Int): Money {
      return Money(cents.toLong())
    }

    fun cents(cents: @ct Long): Money {
      return Money(cents)
    }

    fun euros(euros: @IsFinite @EUR Double, rounding: RoundingStrategy = RoundingStrategy.Default): Money {
      return Money(euros.toCents(rounding))
    }

    inline fun <T> fastSumBy(elements: List<T>, callback: (value: T) -> Money): Money {
      return Money(elements.fastSumByLong { callback(it).cents })
    }
  }

  /**
   * A net amount: a unit price, a line or document net, a discount, a catalog or cost price, an hourly rate.
   */
  @Serializable
  @SerialName("Money.Net")
  @JvmInline
  value class Net(override val cents: @ct Long) : MoneyAmount, Comparable<Net> {
    override operator fun compareTo(other: Net): Int {
      return cents.compareTo(other.cents)
    }

    operator fun plus(other: Net): Net {
      return Net(cents + other.cents)
    }

    operator fun plus(vatAmount: Vat): Gross {
      return Gross(cents + vatAmount.cents)
    }

    operator fun minus(other: Net): Net {
      return Net(cents - other.cents)
    }

    operator fun unaryMinus(): Net {
      return Net(-cents)
    }

    operator fun times(factor: Int): Net {
      return Net(cents.timesExact(factor))
    }

    operator fun times(vatRate: VatRate): Vat {
      return vatAmount(vatRate)
    }

    fun vatAmount(vatRate: VatRate, rounding: RoundingStrategy = RoundingStrategy.Default): Vat {
      return Vat(rounding.roundQuotient(cents.timesExact(vatRate.basisPoints), VatRate.BasisPointsPerUnit.toLong()))
    }

    fun timesAndRound(factor: Double, rounding: RoundingStrategy = RoundingStrategy.Default): Net {
      return Net(cents.timesAndRound(factor, rounding))
    }

    fun divAndRound(divisor: Double, rounding: RoundingStrategy = RoundingStrategy.Default): Net {
      return Net(cents.divAndRound(divisor, rounding))
    }

    operator fun div(other: Net): @pct Double {
      return cents.ratioTo(other.cents)
    }

    override fun toString(): String {
      return format(I18nConfiguration.US, WhitespaceConfig.Spaces)
    }

    companion object {
      val Zero: Net = Net(0)

      fun euros(euros: @IsFinite @EUR Double, rounding: RoundingStrategy = RoundingStrategy.Default): Net {
        return Net(euros.toCents(rounding))
      }

      inline fun <T> fastSumBy(elements: List<T>, callback: (value: T) -> Net): Net {
        return Net(elements.fastSumByLong { callback(it).cents })
      }
    }
  }

  /**
   * A gross amount: a document gross, a payable, an investment sum. No operation leads back to a net.
   */
  @Serializable
  @SerialName("Money.Gross")
  @JvmInline
  value class Gross(override val cents: @ct Long) : MoneyAmount, Comparable<Gross> {
    override operator fun compareTo(other: Gross): Int {
      return cents.compareTo(other.cents)
    }

    operator fun plus(other: Gross): Gross {
      return Gross(cents + other.cents)
    }

    operator fun minus(other: Gross): Gross {
      return Gross(cents - other.cents)
    }

    operator fun unaryMinus(): Gross {
      return Gross(-cents)
    }

    operator fun times(factor: Int): Gross {
      return Gross(cents.timesExact(factor))
    }

    fun timesAndRound(factor: Double, rounding: RoundingStrategy = RoundingStrategy.Default): Gross {
      return Gross(cents.timesAndRound(factor, rounding))
    }

    fun divAndRound(divisor: Double, rounding: RoundingStrategy = RoundingStrategy.Default): Gross {
      return Gross(cents.divAndRound(divisor, rounding))
    }

    operator fun div(other: Gross): @pct Double {
      return cents.ratioTo(other.cents)
    }

    override fun toString(): String {
      return format(I18nConfiguration.US, WhitespaceConfig.Spaces)
    }

    companion object {
      val Zero: Gross = Gross(0)

      fun euros(euros: @IsFinite @EUR Double, rounding: RoundingStrategy = RoundingStrategy.Default): Gross {
        return Gross(euros.toCents(rounding))
      }

      inline fun <T> fastSumBy(elements: List<T>, callback: (value: T) -> Gross): Gross {
        return Gross(elements.fastSumByLong { callback(it).cents })
      }
    }
  }

  @Serializable
  @SerialName("Money.Vat")
  @JvmInline
  value class Vat(override val cents: @ct Long) : MoneyAmount, Comparable<Vat> {
    override operator fun compareTo(other: Vat): Int {
      return cents.compareTo(other.cents)
    }

    operator fun plus(other: Vat): Vat {
      return Vat(cents + other.cents)
    }

    operator fun minus(other: Vat): Vat {
      return Vat(cents - other.cents)
    }

    operator fun unaryMinus(): Vat {
      return Vat(-cents)
    }

    operator fun times(factor: Int): Vat {
      return Vat(cents.timesExact(factor))
    }

    fun timesAndRound(factor: Double, rounding: RoundingStrategy = RoundingStrategy.Default): Vat {
      return Vat(cents.timesAndRound(factor, rounding))
    }

    fun divAndRound(divisor: Double, rounding: RoundingStrategy = RoundingStrategy.Default): Vat {
      return Vat(cents.divAndRound(divisor, rounding))
    }

    operator fun div(other: Vat): @pct Double {
      return cents.ratioTo(other.cents)
    }

    override fun toString(): String {
      return format(I18nConfiguration.US, WhitespaceConfig.Spaces)
    }

    companion object {
      val Zero: Vat = Vat(0)

      fun euros(euros: @IsFinite @EUR Double, rounding: RoundingStrategy = RoundingStrategy.Default): Vat {
        return Vat(euros.toCents(rounding))
      }

      inline fun <T> fastSumBy(elements: List<T>, callback: (value: T) -> Vat): Vat {
        return Vat(elements.fastSumByLong { callback(it).cents })
      }
    }
  }

  /**
   * A net amount with the rate it is taxed at. [vatAmount] and [gross] follow from the two, and are written out with
   * them so a reader of the stored document and a REST client see the numbers the block was calculated with.
   */
  @Serializable
  @SerialName("Money.WithVat")
  data class WithVat(
    /**
     * The taxable base: the net sum of the items taxed at [vatRate].
     */
    val net: Net,

    val vatRate: VatRate,
  ) {
    /**
     * [net] times [vatRate], rounded once (EN 16931, BR-CO-17).
     */
    @EncodeDefault
    val vatAmount: Vat = expectedVatAmount()

    /**
     * [net] plus [vatAmount] (EN 16931, BR-CO-15). A gross rounded on its own would be a cent off the recalculation a
     * reader of the document performs.
     */
    @EncodeDefault
    val gross: Gross = expectedGross()

    // Only a decoded document can fail here: the constructor derives both from net and rate.
    init {
      require(vatAmount == expectedVatAmount()) { "Tax <${vatAmount.cents} ct> does not match net <${net.cents} ct> at ${vatRate.vat}: expected <${expectedVatAmount().cents} ct>" }
      require(gross == expectedGross()) { "Gross <${gross.cents} ct> does not match net <${net.cents} ct> plus tax <${vatAmount.cents} ct>: expected <${expectedGross().cents} ct>" }
    }

    private fun expectedVatAmount(): Vat {
      return net * vatRate
    }

    private fun expectedGross(): Gross {
      return net + vatAmount
    }
  }
}

// The euro value is a binary double: a half cent such as 1.005 lies on either side of it and rounds either way.
private fun Double.toCents(rounding: RoundingStrategy): @ct Long {
  return (this * 100.0).roundInExactRange(rounding)
}

internal fun Long.timesExact(factor: Int): Long {
  // Long * Int can silently wrap. Dividing back detects it, except for MIN_VALUE * -1, which wraps onto itself.
  require(this != Long.MIN_VALUE || factor != -1) { "Multiplication overflow: $this * $factor" }
  val result = this * factor
  if (factor != 0) {
    require(result / factor == this) { "Multiplication overflow: $this * $factor" }
  }
  return result
}

private fun Long.timesAndRound(factor: Double, rounding: RoundingStrategy): Long {
  return (toExactDouble() * factor).roundInExactRange(rounding)
}

private fun Long.divAndRound(divisor: Double, rounding: RoundingStrategy): Long {
  // An infinite divisor would answer a finite zero.
  require(divisor.isFinite()) { "Divisor must be finite: $divisor" }
  require(divisor != 0.0) { "Cannot divide money by zero" }
  return (toExactDouble() / divisor).roundInExactRange(rounding)
}

/**
 * A double keeps the half unit a rounding decides on only up to 2^52; the input and the result of a rounded
 * calculation stay within it.
 */
private const val MaxExactCents: @ct Long = 1L shl 52

internal fun Long.toExactDouble(): Double {
  require(this in -MaxExactCents..MaxExactCents) { "Cents beyond exact double range: $this" }
  return toDouble()
}

internal fun Double.roundInExactRange(rounding: RoundingStrategy): Long {
  require(abs(this) <= MaxExactCents.toDouble()) { "Out of exact range: $this" }
  return rounding.roundToLong(this)
}

private fun Long.ratioTo(other: Long): @pct Double {
  require(other != 0L) { "Cannot divide by zero money" }
  return this.toDouble() / other.toDouble()
}

val Double.euro: Money
  get() = Money.euros(this)

val Int.euro: Money
  get() = Money(this.toLong() * 100)

val Long.euro: Money
  get() = Money(this.timesExact(100))

val Int.cents: Money
  get() = Money(this.toLong())

val Long.cents: Money
  get() = Money(this)

val Double.euroNet: Money.Net
  get() = Money.Net.euros(this)

val Int.euroNet: Money.Net
  get() = Money.Net(this.toLong() * 100)

val Double.euroGross: Money.Gross
  get() = Money.Gross.euros(this)

val Int.euroGross: Money.Gross
  get() = Money.Gross(this.toLong() * 100)

val Double.euroVat: Money.Vat
  get() = Money.Vat.euros(this)

val Int.euroVat: Money.Vat
  get() = Money.Vat(this.toLong() * 100)

fun Iterable<Money>.sum(): Money {
  return Money(sumOf { it.cents })
}

@JvmName("sumNet")
fun Iterable<Money.Net>.sum(): Money.Net {
  return Money.Net(sumOf { it.cents })
}

@JvmName("sumGross")
fun Iterable<Money.Gross>.sum(): Money.Gross {
  return Money.Gross(sumOf { it.cents })
}

@JvmName("sumVat")
fun Iterable<Money.Vat>.sum(): Money.Vat {
  return Money.Vat(sumOf { it.cents })
}

fun Money?.orZero(): Money {
  return this ?: Money.Zero
}

@JvmName("orZeroNet")
fun Money.Net?.orZero(): Money.Net {
  return this ?: Money.Net.Zero
}

@JvmName("orZeroGross")
fun Money.Gross?.orZero(): Money.Gross {
  return this ?: Money.Gross.Zero
}

@JvmName("orZeroVat")
fun Money.Vat?.orZero(): Money.Vat {
  return this ?: Money.Vat.Zero
}
