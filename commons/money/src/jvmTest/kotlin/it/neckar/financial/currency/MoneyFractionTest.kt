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
import it.neckar.financial.currency.io.MoneyFractionAsEuroSerializer
import it.neckar.open.i18n.I18nConfiguration
import it.neckar.open.kotlin.lang.WhitespaceConfig
import it.neckar.open.kotlin.lang.roundDecimalPlaces
import it.neckar.open.kotlin.lang.roundDecimalPlacesHalfUp
import it.neckar.open.serialization.roundTrip
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test
import kotlin.math.roundToInt

class MoneyFractionTest {
  @Test
  fun testCentiCents() {
    assertThat(MoneyFraction.centiCents(10_000)).isEqualTo(MoneyFraction.centiCents(10_000))
    assertThat(MoneyFraction.cents(100)).isEqualTo(MoneyFraction.centiCents(10_000))
    assertThat(MoneyFraction.euros(1.0)).isEqualTo(MoneyFraction.centiCents(10_000))

    assertThat(MoneyFraction.centiCents(10_000)).isEqualTo(MoneyFraction.centiCents(10_000))
    assertThat(MoneyFraction.centiCents(10_000)).isEqualTo(MoneyFraction.cents(100))
    assertThat(MoneyFraction.centiCents(10_000)).isEqualTo(MoneyFraction.euros(1.0))

    assertThat(MoneyFraction.centiCents(10_000)).isEqualTo(100.cents100)
    assertThat(MoneyFraction.centiCents(10_000)).isEqualTo(1.euro100)

    assertThat(100.cents100).isEqualTo(MoneyFraction.centiCents(10_000))
    assertThat(1.euro100).isEqualTo(MoneyFraction.centiCents(10_000))

    assertThat(MoneyFraction.centiCents(10_000).centiCents).isEqualTo(10_000)
    assertThat(MoneyFraction.centiCents(10_000).cents).isEqualTo(100.0)
    assertThat(MoneyFraction.centiCents(10_000).euros).isEqualTo(1.0)
  }

  @Test
  fun `cents(Int) must not overflow for large amounts`() {
    //25_000_000 ct = 250,000.00 EUR. cents * CentFactor = 2_500_000_000 exceeds Int.MAX_VALUE,
    //so the multiplication must be done in Long width, not Int.
    assertThat(MoneyFraction.cents(25_000_000)).isEqualTo(MoneyFraction.centiCents(2_500_000_000L))
    assertThat(MoneyFraction.cents(25_000_000).euros).isEqualTo(250_000.0)
    assertThat(25_000_000.cents100).isEqualTo(MoneyFraction.centiCents(2_500_000_000L))
  }


  @Test
  fun testRoundingProblem() {
    assertThat(119.5 * 1.19).isEqualTo(142.20499999999998)
    assertThat(119.50 * 1.19).isEqualTo(142.20499999999998)

    assertThat(MoneyFraction.euros(119.50).timesAndRound(1.19)).isEqualTo(MoneyFraction.euros(142.205))
    assertThat(MoneyFraction.euros(119.5).timesAndRound(1.19)).isEqualTo(MoneyFraction.euros(142.205))

    //Sometimes this is the result of the multiplication!
    //142.20499999999999999
  }

  @Test
  fun testMultiplicationWithRounding() {
    assertThat((MoneyFraction.euros(21.13).timesAndRound(1.19)).toString()).isEqualTo("2,514.47 ct")
    assertThat((MoneyFraction.euros(21.13).timesAndRound(1.19)).formatAsEuros(whitespaceConfig = WhitespaceConfig.Spaces)).isEqualTo("25.14 €")
    assertThat((MoneyFraction.euros(21.14).timesAndRound(1.19)).toString()).isEqualTo("2,515.66 ct")
    assertThat((MoneyFraction.euros(21.14).timesAndRound(1.19)).formatAsEuros(whitespaceConfig = WhitespaceConfig.Spaces)).isEqualTo("25.16 €")

    assertThat((MoneyFraction.euros(21.13).timesAndRound(0.19)).toString()).isEqualTo("401.47 ct")
    assertThat((MoneyFraction.euros(21.13).timesAndRound(0.19)).formatAsEuros(whitespaceConfig = WhitespaceConfig.Spaces)).isEqualTo("4.01 €")
    assertThat((MoneyFraction.euros(21.14).timesAndRound(0.19)).toString()).isEqualTo("401.66 ct")
    assertThat((MoneyFraction.euros(21.14).timesAndRound(0.19)).formatAsEuros(whitespaceConfig = WhitespaceConfig.Spaces)).isEqualTo("4.02 €")

    assertThat((MoneyFraction.euros(119.50).timesAndRound(1.19)).toString()).isEqualTo("14,220.50 ct")
    assertThat((MoneyFraction.euros(119.50).timesAndRound(1.19)).formatAsEuros(whitespaceConfig = WhitespaceConfig.Spaces)).isEqualTo("142.21 €")
    assertThat((MoneyFraction.euros(119.50).timesAndRound(0.19)).toString()).isEqualTo("2,270.50 ct")
    assertThat((MoneyFraction.euros(119.50).timesAndRound(0.19)).formatAsEuros(whitespaceConfig = WhitespaceConfig.Spaces)).isEqualTo("22.70 €")
  }

  @Test
  fun testRoundingBug() {
    assertThat(119.50 * 0.19).isEqualTo(22.705000000000002)
    assertThat((119.50 * 0.19).roundToInt()).isEqualTo(23)
    assertThat((119.50 * 0.19).roundDecimalPlaces(2)).isEqualTo(22.70)
    assertThat((119.50 * 0.19).roundDecimalPlacesHalfUp(2)).isEqualTo(22.71)

    assertThat((MoneyFraction.euros(119.50).timesAndRound(0.19)).toString()).isEqualTo("2,270.50 ct")
    assertThat((MoneyFraction.euros(119.50).timesAndRound(0.19)).formatAsEuros(whitespaceConfig = WhitespaceConfig.Spaces)).isEqualTo("22.70 €")
  }

  @Test
  fun testFormat() {
    assertThat(MoneyFraction.cents(173).format(I18nConfiguration.Germany)).isEqualTo("173,00 ct")
    assertThat(MoneyFraction.cents(173).formatAsEuros(I18nConfiguration.Germany)).isEqualTo("1,73 €")
    assertThat(MoneyFraction.cents(173).format(I18nConfiguration.US)).isEqualTo("173.00 ct")
    assertThat(MoneyFraction.cents(173).formatAsEuros(I18nConfiguration.US)).isEqualTo("1.73 €")

    assertThat(MoneyFraction.cents(173).format(I18nConfiguration.Germany, WhitespaceConfig.Spaces)).isEqualTo("173,00 ct")
    assertThat(MoneyFraction.cents(173).formatAsEuros(I18nConfiguration.Germany, WhitespaceConfig.Spaces)).isEqualTo("1,73 €")
    assertThat(MoneyFraction.cents(173).format(I18nConfiguration.US, WhitespaceConfig.Spaces)).isEqualTo("173.00 ct")
    assertThat(MoneyFraction.cents(173).formatAsEuros(I18nConfiguration.US, WhitespaceConfig.Spaces)).isEqualTo("1.73 €")
  }

  @Test
  fun testNegative() {
    assertThat(MoneyFraction.cents(0).euros).isEqualTo(0.0)
    assertThat(MoneyFraction.cents(-0).euros).isEqualTo(0.0)
  }

  @Test
  fun testCalculations() {
    assertThat(MoneyFraction.cents(170).euros).isEqualTo(1.7)
    assertThat(MoneyFraction.cents(170).euros).isEqualTo(MoneyFraction.euros(1.70).euros)
    assertThat(MoneyFraction.cents(170).euros).isEqualTo(MoneyFraction.euros(1.70).euros)
  }

  @Test
  fun testSerialization() {
    roundTrip(MoneyFraction.cents(179)) {
      //language=JSON
      """
        179.0
      """.trimIndent()
    }
  }

  @Test
  fun `sub-cent precision survives the round trip`() {
    //The reason this type exists rather than Money: the centi-cent grid, two decimals of a cent.
    roundTrip(MoneyFraction.centiCents(820)) {
      //language=JSON
      """
        8.2
      """.trimIndent()
    }
  }

  @Test
  fun testSum() {
    assertThat(listOf(MoneyFraction.cents(179), MoneyFraction.cents(179)).sum()).isEqualTo(MoneyFraction.cents(358))
    assertThat(listOf(MoneyFraction.cents(179), MoneyFraction.cents(179)).sum().euros).isEqualTo(3.58)
  }

  @Test
  fun `the euro format documents were written in before is still readable`() {
    //MoneyFractionAsEuroSerializer stays for exactly this: a migration reading a document that
    //holds euros. Applied explicitly, never as the type default.
    assertThat(Json.decodeFromString(MoneyFractionAsEuroSerializer, "1.79")).isEqualTo(MoneyFraction.cents(179))
  }

  @Test
  fun testEurosRange() {
    assertThat(MoneyFraction.cents(Double.MIN_VALUE).cents).isEqualTo(0.0) //rounded
    assertThat(MoneyFraction.centiCents(Int.MAX_VALUE).cents).isEqualTo(Int.MAX_VALUE / 100.0)
  }

  @Test
  fun testMoneyOperations() {
    val money1 = MoneyFraction.cents(100)
    val money2 = MoneyFraction.cents(200)

    val sum = money1 + money2
    val difference = money1 - money2
    val product = money1 * 2
    val quotient = money1.divAndRound(2.0)

    assertThat(sum.euros).isEqualTo(3.00)
    assertThat(difference.euros).isEqualTo(-1.00)
    assertThat(product.euros).isEqualTo(2.00)
    assertThat(quotient.euros).isEqualTo(0.50)
  }

  @Test
  fun `times fails fast on overflow`() {
    val huge = MoneyFraction.centiCents(Long.MAX_VALUE / 2)
    assertFailure {
      huge * 3
    }.isInstanceOf(IllegalArgumentException::class).messageContains("overflow")
  }

  @Test
  fun `a negative half rounds away from zero`() {
    assertThat(MoneyFraction.centiCents(-250).roundToCents()).isEqualTo(Money(-3))
    assertThat(MoneyFraction.cents(-0.005)).isEqualTo(MoneyFraction.centiCents(-1))
  }

  @Test
  fun `times with zero factor is zero`() {
    assertThat(MoneyFraction.centiCents(Long.MAX_VALUE) * 0).isEqualTo(MoneyFraction.centiCents(0L))
  }

  @Test
  fun testCentsOverflow() {
    assertThat(MoneyFraction.centiCents(Int.MAX_VALUE)).isEqualTo(MoneyFraction.cents(Int.MAX_VALUE.toDouble() / 100.0))

    val first = MoneyFraction.centiCents(Int.MAX_VALUE) + MoneyFraction.centiCents(1)
    assertThat(first.cents).isGreaterThan(Int.MAX_VALUE.toDouble() / 100.0)
    assertThat(first.cents).isEqualTo(Int.MAX_VALUE.toDouble() / 100.0 + 0.01)

    val second = MoneyFraction.cents(Int.MAX_VALUE.toDouble() / 100.0 + 0.01)
    assertThat(first.cents).isGreaterThan(Int.MAX_VALUE.toDouble() / 100.0)
    assertThat(first.cents).isEqualTo(Int.MAX_VALUE.toDouble() / 100.0 + 0.01)

    assertThat(first.cents).isEqualTo(21_474_836.48)
    assertThat(second.cents).isEqualTo(21_474_836.48)

    assertThat(first.cents).isEqualTo(second.cents)
    assertThat(first).isEqualTo(second)

    assertThat(second.delta(first)).isEqualTo(0)

    assertThat(first.toString()).isEqualTo("21,474,836.48 ct")
    assertThat(second.toString()).isEqualTo("21,474,836.48 ct")

    assertThat(first.format(I18nConfiguration.US, WhitespaceConfig.Spaces)).isEqualTo("21,474,836.48 ct")
    assertThat(second.format(I18nConfiguration.US, WhitespaceConfig.Spaces)).isEqualTo("21,474,836.48 ct")


    assertThat(MoneyFraction.centiCents(Int.MIN_VALUE) - MoneyFraction.centiCents(1)).isEqualTo(MoneyFraction.cents(Int.MIN_VALUE.toDouble() / 100.0 - 0.01))
  }

  @Test
  fun testRoundingDouble() {
    assertThat((0.1 + 0.2)).isGreaterThan(0.3)
    assertThat((0.03 - 0.01)).isLessThan(0.02)
    assertThat((0.1 * 0.2)).isGreaterThan(0.02)
    assertThat(0.1 / 0.3).isGreaterThan(0.3333333333333333)
  }

  @Test
  fun testRoundingErrors() {
    assertThat((MoneyFraction.euros(0.1) + MoneyFraction.euros(0.2)).euros).isEqualTo(0.3)
    assertThat((MoneyFraction.euros(0.03) - MoneyFraction.euros(0.01)).euros).isEqualTo(0.02)
    assertThat((MoneyFraction.euros(0.03) - MoneyFraction.euros(0.01)).euros).isEqualTo(0.02)
    assertThat((MoneyFraction.euros(0.1).divAndRound(0.3)).euros).isEqualTo(0.3333)
  }
}
