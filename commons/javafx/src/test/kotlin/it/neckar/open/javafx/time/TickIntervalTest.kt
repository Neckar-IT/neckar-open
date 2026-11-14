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
package it.neckar.open.javafx.time

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isNotEqualTo
import org.junit.jupiter.api.Test
import java.time.LocalDateTime

class TickIntervalTest {
  @Test
  fun `millisecond ticks render distinct labels including the millisecond field`() {
    val at100 = LocalDateTime.of(2020, 1, 1, 14, 30, 45, 100_000_000)
    val at200 = LocalDateTime.of(2020, 1, 1, 14, 30, 45, 200_000_000)

    val label100 = TickInterval.MILLISECOND.format(at100)
    val label200 = TickInterval.MILLISECOND.format(at200)

    //Both ticks fall in the same second - the millisecond field must keep them distinct.
    assertThat(label100).isEqualTo("14:30:45.100")
    assertThat(label200).isEqualTo("14:30:45.200")
    assertThat(label100).isNotEqualTo(label200)
  }
}
