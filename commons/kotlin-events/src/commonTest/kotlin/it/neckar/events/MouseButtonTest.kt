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
package it.neckar.events

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import kotlin.test.Test

class MouseButtonTest {
  @Test
  fun `fromDomButtonCode maps the standard DOM button codes`() {
    //https://developer.mozilla.org/en-US/docs/Web/API/MouseEvent/button
    assertThat(MouseButton.fromDomButtonCode(0)).isEqualTo(MouseButton.Primary)
    assertThat(MouseButton.fromDomButtonCode(1)).isEqualTo(MouseButton.Middle)
    assertThat(MouseButton.fromDomButtonCode(2)).isEqualTo(MouseButton.Secondary)
    assertThat(MouseButton.fromDomButtonCode(3)).isEqualTo(MouseButton.Back)
    assertThat(MouseButton.fromDomButtonCode(4)).isEqualTo(MouseButton.Forward)
  }

  @Test
  fun `fromDomButtonCode maps every code outside 0 to 4 to Other`() {
    //The whole input domain, not just a sample: only the five DOM codes are allowed to yield a named button
    val namedButtonCodes = (Short.MIN_VALUE..Short.MAX_VALUE)
      .map { it.toShort() }
      .filter { MouseButton.fromDomButtonCode(it) != MouseButton.Other }

    assertThat(namedButtonCodes).containsExactly(0.toShort(), 1.toShort(), 2.toShort(), 3.toShort(), 4.toShort())
  }

  @Test
  fun `fromDomButtonCode reaches every button but Other`() {
    val mapped = (0..4).map { MouseButton.fromDomButtonCode(it.toShort()) }

    //A new constant without a DOM code makes this fail - which is exactly when fromDomButtonCode needs a look
    assertThat(mapped.toSet().sortedBy { it.ordinal })
      .containsExactly(*MouseButton.entries.filter { it != MouseButton.Other }.toTypedArray())
  }

  @Test
  fun `isPrimary holds for the primary button only`() {
    assertThat(MouseButton.entries.filter { it.isPrimary() }).containsExactly(MouseButton.Primary)
  }

  @Test
  fun `isSecondary holds for the secondary button only`() {
    assertThat(MouseButton.entries.filter { it.isSecondary() }).containsExactly(MouseButton.Secondary)
  }
}
