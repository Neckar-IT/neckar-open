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
import it.neckar.open.i18n.I18nConfiguration
import it.neckar.open.kotlin.lang.WhitespaceConfig
import org.junit.jupiter.api.Test

class MoneyTest {
  @Test
  fun testMultiplicationWithRounding() {
    assertThat((Money.euros(21.13).timesAndRound(1.19)).toString()).isEqualTo("25.14 €")
    assertThat((Money.euros(21.14).timesAndRound(1.19)).toString()).isEqualTo("25.16 €")

    assertThat((Money.euros(21.13).timesAndRound(0.19)).toString()).isEqualTo("4.01 €")
    assertThat((Money.euros(21.14).timesAndRound(0.19)).toString()).isEqualTo("4.02 €")

    assertThat((Money.euros(119.50).timesAndRound(1.19)).toString()).isEqualTo("142.21 €")
    assertThat((Money.euros(119.50).timesAndRound(0.19)).toString()).isEqualTo("22.71 €")
  }

  @Test
  fun testFormat() {
    assertThat(Money(173).format(I18nConfiguration.Germany)).isEqualTo("1,73 €")
    assertThat(Money(173).format(I18nConfiguration.US)).isEqualTo("1.73 €")

    assertThat(Money(173).format(I18nConfiguration.Germany, WhitespaceConfig.Spaces)).isEqualTo("1,73 €")
    assertThat(Money(173).format(I18nConfiguration.US, WhitespaceConfig.Spaces)).isEqualTo("1.73 €")
  }

  @Test
  fun testCalculations() {
    assertThat(Money(170).euros).isEqualTo(1.7)
    assertThat(Money(170).euros).isEqualTo(Money.euros(1.70).euros)
    assertThat(Money(170).euros).isEqualTo(Money.euros(1.704).euros)
  }

  @Test
  fun testRoundingErrors() {
    assertThat((Money.euros(0.1) + Money.euros(0.2)).euros).isEqualTo(0.3)
    assertThat((Money.euros(0.03) - Money.euros(0.01)).euros).isEqualTo(0.02)
    assertThat((Money.euros(0.1).divAndRound(0.3)).euros).isEqualTo(0.33)
  }
}
