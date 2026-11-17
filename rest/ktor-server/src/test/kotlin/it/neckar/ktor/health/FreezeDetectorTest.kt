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
package it.neckar.ktor.health

import assertk.*
import assertk.assertions.*
import it.neckar.open.time.VirtualNowProvider
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

class FreezeDetectorTest {
  private val clock = VirtualNowProvider(1_000_000_000.0)
  private val detector = FreezeDetector(tickInterval = 30.seconds)

  @Test
  fun `the first tick and ticks on time are no freeze`() {
    val start = clock.markNow()

    assertThat(detector.frozenBefore(start)).isNull()
    assertThat(detector.frozenBefore(start + 31.seconds)).isNull()
    assertThat(detector.frozenBefore(start + 31.seconds + 59.seconds)).isNull()
  }

  @Test
  fun `a tick more than two intervals late reports the frozen time beyond one interval`() {
    val start = clock.markNow()
    assertThat(detector.frozenBefore(start)).isNull()

    assertThat(detector.frozenBefore(start + 10.minutes)).isEqualTo(10.minutes - 30.seconds)
  }

  @Test
  fun `late ticks one after another are no freeze`() {
    val start = clock.markNow()
    assertThat(detector.frozenBefore(start)).isNull()
    assertThat(detector.frozenBefore(start + 2.minutes)).isNotNull()

    assertThat(detector.frozenBefore(start + 4.minutes)).isNull()
    assertThat(detector.frozenBefore(start + 6.minutes)).isNull()
  }
}
