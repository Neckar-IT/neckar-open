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

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import assertk.assertions.isInstanceOf
import kotlin.test.AfterTest
import kotlin.test.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class VirtualNowProviderTimeSourceTest {
  private val start: Instant = Instant.parse("2026-01-15T03:00:00Z")
  private val provider = VirtualNowProvider(start.toEpochMilliseconds().toDouble())

  @AfterTest
  fun reset() {
    resetNowProvider()
    resetMonotonicTimeSource()
  }

  @Test
  fun `advanceBy moves now and every mark by the same amount`() {
    nowProvider = provider
    monotonicTimeSource = provider
    val mark = monotonicTimeSource.markNow()

    provider.advanceBy(6 * 60_000.0)

    assertThat(now() - start).isEqualTo(6.minutes)
    assertThat(mark.elapsedNow()).isEqualTo(6.minutes)
  }

  @Test
  fun `marks compare and subtract by their virtual time`() {
    val first = provider.markNow()
    provider.advanceBy(1_500.0)
    val second = provider.markNow()

    assertThat(second - first).isEqualTo(1_500.milliseconds)
    assertThat(second).isGreaterThan(first)
    assertThat(first + 1_500.milliseconds).isEqualTo(second)
    assertThat((first + 10.seconds).elapsedNow()).isEqualTo((-8_500).milliseconds)
  }

  @Test
  fun `a mark taken before a wall clock jump keeps its elapsed time`() {
    val wallClock = VirtualNowProvider(start.toEpochMilliseconds().toDouble())
    nowProvider = wallClock
    monotonicTimeSource = provider
    val mark = monotonicTimeSource.markNow()

    wallClock.advanceBy(24 * 60 * 60_000.0)

    assertThat(mark.elapsedNow()).isEqualTo(Duration.ZERO)
  }

  @Test
  fun `marks of different providers cannot be subtracted`() {
    val other = VirtualNowProvider(start.toEpochMilliseconds().toDouble())

    assertFailure { provider.markNow() - other.markNow() }.isInstanceOf(IllegalArgumentException::class)
  }

  @Test
  fun `marks shifted by an infinite duration behave like those of TimeSource Monotonic`() {
    val mark = provider.markNow()
    val infinite = mark + Duration.INFINITE

    assertThat(infinite - (mark + Duration.INFINITE)).isEqualTo(Duration.ZERO)
    assertThat(infinite - mark).isEqualTo(Duration.INFINITE)
    assertThat(infinite.elapsedNow()).isEqualTo(-Duration.INFINITE)
    assertFailure { infinite + -Duration.INFINITE }.isInstanceOf(IllegalArgumentException::class)
  }

  @Test
  fun `marks at negative and positive zero are equal and share their hash code`() {
    val negativeZero = VirtualNowProvider(-0.0).markNow()
    val positiveZero = negativeZero + Duration.ZERO + 1.milliseconds + (-1).milliseconds

    assertThat(positiveZero).isEqualTo(negativeZero)
    assertThat(positiveZero.hashCode()).isEqualTo(negativeZero.hashCode())
  }
}
