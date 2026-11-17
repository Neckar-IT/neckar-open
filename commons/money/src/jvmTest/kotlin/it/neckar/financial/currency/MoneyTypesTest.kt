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

import assertk.*
import assertk.assertions.*
import it.neckar.open.kotlin.lang.WhitespaceConfig
import it.neckar.open.kotlin.lang.percent
import it.neckar.open.serialization.roundTrip
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

class MoneyTypesTest {
  @Test
  fun `net times vat rate is the tax rounded to cents`() {
    assertThat(Money.Net(85_500) * VatRate.NineteenPercent).isEqualTo(Money.Vat(16_245))
    assertThat(Money.Net(2_113) * VatRate.NineteenPercent).isEqualTo(Money.Vat(401))
    assertThat(Money.Net(2_114) * VatRate.NineteenPercent).isEqualTo(Money.Vat(402))
    assertThat(Money.Net(85_500) * VatRate.Zero).isEqualTo(Money.Vat.Zero)
  }

  @Test
  fun `the tax of a credit note is the negated tax of the invoice`() {
    assertThat(Money.Net(1_050) * VatRate.NineteenPercent).isEqualTo(Money.Vat(200))
    assertThat(Money.Net(-1_050) * VatRate.NineteenPercent).isEqualTo(Money.Vat(-200))
  }

  @Test
  fun `timesAndRound, divAndRound, euros and vatAmount take another rounding strategy`() {
    assertThat(Money.Net(103).timesAndRound(1.5, RoundingStrategy.HalfToEven)).isEqualTo(Money.Net(154))
    assertThat(Money.Net(-103).timesAndRound(1.5, RoundingStrategy.HalfToEven)).isEqualTo(Money.Net(-154))
    assertThat(Money.Net(105).divAndRound(2.0, RoundingStrategy.HalfToEven)).isEqualTo(Money.Net(52))
    assertThat(Money.Gross.euros(1.125, RoundingStrategy.HalfToEven)).isEqualTo(Money.Gross(112))
    assertThat(Money.Net(1_150).vatAmount(VatRate.NineteenPercent, RoundingStrategy.HalfToEven)).isEqualTo(Money.Vat(218))
  }

  @Test
  fun `the tax is decided in integers, so a binary rate never moves a tie`() {
    // 150 * 0.07 is 10.500000000000002 as a double, 50 * 0.29 is 14.499999999999998
    assertThat(Money.Net(150).vatAmount(VatRate.SevenPercent, RoundingStrategy.HalfToEven)).isEqualTo(Money.Vat(10))
    assertThat(Money.Net(50) * VatRate(29.0.percent)).isEqualTo(Money.Vat(15))
    assertThat(Money.Net(-50) * VatRate(29.0.percent)).isEqualTo(Money.Vat(-15))
  }

  @Test
  fun `a rate finer than a basis point, outside 0 to 1 or not finite is refused`() {
    assertFailure { VatRate(9.975.percent) }.isInstanceOf(IllegalArgumentException::class).messageContains("basis points")
    assertFailure { VatRate(-19.0.percent) }.isInstanceOf(IllegalArgumentException::class).messageContains("between 0 and 1")
    assertFailure { VatRate(119.0.percent) }.isInstanceOf(IllegalArgumentException::class).messageContains("between 0 and 1")
    assertFailure { VatRate(Double.NaN) }.isInstanceOf(IllegalArgumentException::class).messageContains("finite")
  }

  @Test
  fun `the binary error of a rate stays within the basis point tolerance`() {
    assertThat(VatRate(1.19 - 1.0).basisPoints).isEqualTo(1900)
  }

  @Test
  fun `a rate formats with the decimals its basis points need`() {
    assertThat(VatRate.SevenPercent.format()).doesNotContain(",")
    assertThat(VatRate(7.5.percent).format()).contains("7,5")
    assertThat(VatRate(7.5.percent).format()).doesNotContain("7,50")
    assertThat(VatRate(9.99.percent).format()).contains("9,99")
  }

  @Test
  fun `a rate formats as a bare percentage without the tax prefix`() {
    assertThat(VatRate.NineteenPercent.formatAsPercentage(whitespaceConfig = WhitespaceConfig.Spaces)).isEqualTo("19 %")
    assertThat(VatRate(5.5.percent).formatAsPercentage(whitespaceConfig = WhitespaceConfig.Spaces)).isEqualTo("5,5 %")
    assertThat(VatRate.NineteenPercent.format(whitespaceConfig = WhitespaceConfig.Spaces)).isEqualTo("MwSt. 19 %")
  }

  @Test
  fun `a fraction formats with as many decimals as it carries, at most two`() {
    assertThat(0.05.formatAsPercentageUpToTwoDecimals(whitespaceConfig = WhitespaceConfig.Spaces)).isEqualTo("5 %")
    assertThat(0.025.formatAsPercentageUpToTwoDecimals(whitespaceConfig = WhitespaceConfig.Spaces)).isEqualTo("2,5 %")
    assertThat(0.0275.formatAsPercentageUpToTwoDecimals(whitespaceConfig = WhitespaceConfig.Spaces)).isEqualTo("2,75 %")
    assertThat(0.57.formatAsPercentageUpToTwoDecimals(whitespaceConfig = WhitespaceConfig.Spaces)).isEqualTo("57 %")
    assertThat(0.12346.formatAsPercentageUpToTwoDecimals(whitespaceConfig = WhitespaceConfig.Spaces)).isEqualTo("12,35 %")
    assertThat(0.12999.formatAsPercentageUpToTwoDecimals(whitespaceConfig = WhitespaceConfig.Spaces)).isEqualTo("13 %")
  }

  @Test
  fun `net plus vat is the gross`() {
    assertThat(Money.Net(85_500) + Money.Vat(16_245)).isEqualTo(Money.Gross(101_745))
  }

  @Test
  fun `arithmetic within a type answers cents, and div answers the ratio`() {
    assertThat(Money(100) + Money(200)).isEqualTo(Money(300))
    assertThat(Money.Net(100) - Money.Net(200)).isEqualTo(Money.Net(-100))
    assertThat(Money.Gross(100) * 3).isEqualTo(Money.Gross(300))
    assertThat(Money.Vat(101).timesAndRound(1.5)).isEqualTo(Money.Vat(152))
    assertThat(Money.Vat(101).divAndRound(2.0)).isEqualTo(Money.Vat(51))
    assertThat(-Money.Net(100)).isEqualTo(Money.Net(-100))
    assertThat(Money.Net(300) / Money.Net(100)).isEqualTo(3.0)
  }

  @Test
  fun `an overflow, an out-of-range value or a division by zero fails instead of answering an amount`() {
    assertFailure { Money(Long.MAX_VALUE) * 2 }.isInstanceOf(IllegalArgumentException::class).messageContains("overflow")
    assertFailure { Money(Long.MIN_VALUE) * -1 }.isInstanceOf(IllegalArgumentException::class).messageContains("overflow")
    assertFailure { Money.Net(1).timesAndRound(1e30) }.isInstanceOf(IllegalArgumentException::class).messageContains("exact range")
    assertFailure { Money.Net((1L shl 52) + 3).timesAndRound(1.5) }.isInstanceOf(IllegalArgumentException::class).messageContains("exact double range")
    assertFailure { Money.Net(Long.MIN_VALUE).divAndRound(1e6) }.isInstanceOf(IllegalArgumentException::class).messageContains("exact double range")
    assertFailure { Money.euros(1e14) }.isInstanceOf(IllegalArgumentException::class).messageContains("exact range")
    assertFailure { Money.euros(Double.NaN) }.isInstanceOf(IllegalArgumentException::class).messageContains("NaN")
    assertFailure { Money.Net(1).divAndRound(0.0) }.isInstanceOf(IllegalArgumentException::class).messageContains("zero")
    assertFailure { Money.Net(1) / Money.Net.Zero }.isInstanceOf(IllegalArgumentException::class).messageContains("zero")
  }

  @Test
  fun `sum, fastSumBy and orZero answer the type of their elements`() {
    assertThat(listOf(Money.Net(179), Money.Net(179)).sum()).isEqualTo(Money.Net(358))
    assertThat(listOf(Money.Gross(1), Money.Gross(2)).sum()).isEqualTo(Money.Gross(3))
    assertThat(listOf(Money.Vat(1), Money.Vat(2)).sum()).isEqualTo(Money.Vat(3))
    assertThat(emptyList<Money>().sum()).isEqualTo(Money.Zero)

    val missing: Money.Gross? = null
    assertThat(missing.orZero()).isEqualTo(Money.Gross.Zero)
    assertThat(Money.Net.fastSumBy(listOf(1, 2, 3)) { Money.Net(it.toLong()) }).isEqualTo(Money.Net(6))
  }

  @Test
  fun `each amount serializes as its cents`() {
    roundTrip(Money(179)) { "179" }
    roundTrip(Money.Net(179)) { "179" }
    roundTrip(Money.Gross(-179)) { "-179" }
    roundTrip(Money.Vat(0)) { "0" }
  }

  @Test
  fun `with vat serializes net, rate, tax and gross under its own serial name even where defaults are skipped`() {
    assertThat(Money.WithVat.serializer().descriptor.serialName).isEqualTo("Money.WithVat")

    val withVat = Money.WithVat(net = Money.Net(85_500), vatRate = VatRate.NineteenPercent)

    val json: String = Json.encodeToString(withVat)
    assertThat(json).isEqualTo("""{"net":85500,"vatRate":0.19,"vatAmount":16245,"gross":101745}""")

    val decoded: Money.WithVat = Json.decodeFromString(json)
    assertThat(decoded).isEqualTo(withVat)
  }

  @Test
  fun `with vat rejects a document whose tax or gross contradicts its net and rate`() {
    assertFailure { Json.decodeFromString<Money.WithVat>("""{"net":85500,"vatRate":0.19,"vatAmount":1,"gross":85501}""") }
      .isInstanceOf(IllegalArgumentException::class)
      .messageContains("Tax")
    assertFailure { Json.decodeFromString<Money.WithVat>("""{"net":85500,"vatRate":0.19,"vatAmount":16245,"gross":1}""") }
      .isInstanceOf(IllegalArgumentException::class)
      .messageContains("Gross")
  }

  @Test
  fun `each amount's descriptor is an inline long`() {
    listOf(Money.serializer(), Money.Net.serializer(), Money.Gross.serializer(), Money.Vat.serializer()).forEach { serializer ->
      assertThat(serializer.descriptor.isInline, serializer.descriptor.serialName).isTrue()
      assertThat(serializer.descriptor.getElementDescriptor(0).kind, serializer.descriptor.serialName).isEqualTo(PrimitiveKind.LONG)
    }
  }

  @Test
  fun `with vat derives tax and gross from a document that states only net and rate`() {
    val withVat: Money.WithVat = Json.decodeFromString("""{"net":85500,"vatRate":0.19}""")
    assertThat(withVat.vatAmount).isEqualTo(Money.Vat(16_245))
    assertThat(withVat.gross).isEqualTo(Money.Gross(101_745))
  }

  @Test
  fun `formats an amount in euros, in cents, or as the fallback for zero`() {
    assertThat(Money.Net(173).toString()).isEqualTo("1.73 €")
    assertThat(Money.Gross(0).formatOrIfZero("-")).isEqualTo("-")
    assertThat(Money.Vat(173).formatAsCents()).isEqualTo("173${WhitespaceConfig.NonBreaking.smallSpace}ct")
  }

  @Test
  fun `the typed literals take a double or a whole number of euros`() {
    assertThat(12.5.euroNet).isEqualTo(Money.Net(1_250))
    assertThat(12.euroNet).isEqualTo(Money.Net(1_200))
    assertThat(12.5.euroGross).isEqualTo(Money.Gross(1_250))
    assertThat(12.euroGross).isEqualTo(Money.Gross(1_200))
    assertThat(12.5.euroVat).isEqualTo(Money.Vat(1_250))
    assertThat(12.euroVat).isEqualTo(Money.Vat(1_200))
  }
}
