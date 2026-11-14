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
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import it.neckar.open.test.utils.WithVirtualTime
import it.neckar.open.test.utils.advance
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.days
import kotlin.time.Duration.Companion.milliseconds

class BaseTimerImplementationTest {
  private class TestTimer : BaseTimerImplementation()

  @WithVirtualTime(1_000.0)
  @Test
  fun `repeat callbacks with differing delays keep firing after reordering`(nowProvider: VirtualNowProvider) {
    val timer = TestTimer()

    var countA = 0
    var countB = 0

    //A fires at 1000+100=1100, B at 1000+150=1150.
    val disposableA = timer.repeat(100.milliseconds) { countA++ }
    val disposableB = timer.repeat(150.milliseconds) { countB++ }

    //now=1120: A due (→1200), B (1150) not yet. After this, the list order is disturbed (A=1200 > B=1150).
    nowProvider.advance(120.milliseconds)
    timer.update()
    assertThat(countA).isEqualTo(1)
    assertThat(countB).isEqualTo(0)

    //now=1160: B (1150) is due and must fire even though A (1200) sorts before it now.
    nowProvider.advance(40.milliseconds)
    timer.update()
    assertThat(countA).isEqualTo(1)
    assertThat(countB).isEqualTo(1)

    disposableA.dispose()
    disposableB.dispose()
  }

  @WithVirtualTime(1_000.0)
  @Test
  fun `a wall clock jump neither fires nor stalls a delay`(monotonic: VirtualNowProvider) {
    val timer = TestTimer()
    var called = false
    val disposable = timer.delay(100.milliseconds) { called = true }

    val wallClock = VirtualNowProvider(monotonic.nowMillis())
    nowProvider = wallClock
    wallClock.advance(1.days)
    timer.update()
    assertThat(called).isFalse()

    monotonic.advance(100.milliseconds)
    timer.update()
    assertThat(called).isTrue()

    disposable.dispose()
  }

  @WithVirtualTime(1_000.0)
  @Test
  fun `a pending delay keeps its remaining time when the time source is replaced`(first: VirtualNowProvider) {
    val timer = TestTimer()
    var called = false
    val disposable = timer.delay(100.milliseconds) { called = true }
    first.advance(40.milliseconds)

    val second = VirtualNowProvider(0.0)
    monotonicTimeSource = second
    timer.update()
    second.advance(59.milliseconds)
    timer.update()
    assertThat(called).isFalse()

    second.advance(1.milliseconds)
    timer.update()
    assertThat(called).isTrue()

    disposable.dispose()
  }
}
