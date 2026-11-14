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
package it.neckar.open.time

import assertk.assertThat
import assertk.assertions.isEqualTo
import kotlin.test.AfterTest
import kotlin.test.Test

class MonotonicMillisTest {
  @AfterTest
  fun reset() {
    resetNowProvider()
    resetMonotonicTimeSource()
  }

  @Test
  fun `the values follow the monotonic time source and ignore a wall clock jump`() {
    val monotonic = VirtualNowProvider(1_000.0)
    monotonicTimeSource = monotonic
    val wallClock = VirtualNowProvider(5_000.0)
    nowProvider = wallClock

    val first = monotonicMillis()
    monotonic.advanceBy(250.0)
    wallClock.advanceBy(60 * 60_000.0)

    assertThat(monotonicMillis() - first).isEqualTo(250.0)
  }

  @Test
  fun `a replaced time source restarts the values at 0`() {
    val first = VirtualNowProvider(1_000.0)
    monotonicTimeSource = first
    assertThat(monotonicMillis()).isEqualTo(0.0)
    first.advanceBy(400.0)
    assertThat(monotonicMillis()).isEqualTo(400.0)

    val second = VirtualNowProvider(9_000.0)
    monotonicTimeSource = second
    assertThat(monotonicMillis()).isEqualTo(0.0)
    second.advanceBy(30.0)
    assertThat(monotonicMillis()).isEqualTo(30.0)
  }
}
