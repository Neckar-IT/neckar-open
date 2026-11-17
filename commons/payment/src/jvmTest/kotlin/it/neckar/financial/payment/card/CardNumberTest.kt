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
package it.neckar.financial.payment.card

import assertk.assertThat
import assertk.assertions.endsWith
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test

class CardNumberTest {
  @Test
  fun `masks a 16-digit PAN keeping the last four visible`() {
    assertThat(CardNumber("4111 1111 1111 1111").value).isEqualTo("**** **** **** 1111")
  }

  @Test
  fun `masks a 15-digit Amex PAN`() {
    // 378282246310005 is a well-known Amex test PAN (15 digits).
    assertThat(CardNumber("3782 822463 10005").value).isEqualTo("*** **** **** 0005")
  }

  @Test
  fun `masks a 14-digit Diners PAN`() {
    // 30569309025904 is a well-known Diners Club test PAN (14 digits).
    assertThat(CardNumber("3056 930902 5904").value).isEqualTo("** **** **** 5904")
  }

  @Test
  fun `masks every allowed length without throwing and keeps only the last four digits visible`() {
    (13..19).forEach { length ->
      val raw = "4".repeat(length - 4) + "1234"
      val cardNumber = CardNumber(raw)
      assertThat(cardNumber.value.filter { it.isDigit() }).isEqualTo("1234")
      assertThat(cardNumber.value).endsWith("1234")
    }
  }

  @Test
  fun `re-constructing an already masked value is idempotent`() {
    val masked = CardNumber("3782 822463 10005").value
    assertThat(CardNumber(masked).value).isEqualTo(masked)
  }
}
