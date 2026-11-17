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
import it.neckar.open.concurrent.repeating.RepeatingEndReason
import it.neckar.open.health.BackgroundJobName
import it.neckar.open.health.BackgroundJobRegistry
import it.neckar.open.test.utils.DisableLogging
import it.neckar.open.time.VirtualNowProvider
import it.neckar.open.time.monotonicTimeSource
import it.neckar.open.time.nowProvider
import it.neckar.open.time.resetMonotonicTimeSource
import it.neckar.open.time.resetNowProvider
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds

@DisableLogging
class EssentialJobWatchdogTest {
  private val name: BackgroundJobName = BackgroundJobName("auto-merge-cycle")
  private val clock = VirtualNowProvider(1_000_000_000.0)
  private val processExit = RecordingProcessExit()

  @BeforeEach
  fun setUp() {
    nowProvider = clock
    monotonicTimeSource = clock
    BackgroundJobRegistry.clear()
    BackgroundJobRegistry.register(name, stalenessThreshold = 5.minutes)
    BackgroundJobRegistry.recordJobStarted(name, cycleTimeout = 5.minutes, maxGap = 15.seconds)
  }

  @AfterEach
  fun tearDown() {
    BackgroundJobRegistry.clear()
    resetNowProvider()
    resetMonotonicTimeSource()
  }

  private fun advance(duration: Duration) {
    clock.advanceBy(duration.inWholeMilliseconds.toDouble())
  }

  private fun EssentialJobWatchdog.checkEvery30SecondsFor(duration: Duration) {
    repeat((duration / 30.seconds).toInt()) {
      advance(30.seconds)
      check()
    }
  }

  @Test
  fun `a cycle blocked past twice its timeout ends an armed process`() {
    val watchdog = EssentialJobWatchdog(StallReaction(WatchdogMode.Exit, processExit))
    BackgroundJobRegistry.recordCycleStarted(name)

    watchdog.checkEvery30SecondsFor(10.minutes)
    assertThat(processExit.statuses).isEmpty()

    watchdog.checkEvery30SecondsFor(30.seconds)
    assertThat(processExit.statuses).containsExactly(70)
  }

  @Test
  fun `an unarmed watchdog only logs`() {
    val watchdog = EssentialJobWatchdog(StallReaction(WatchdogMode.LogOnly, processExit))
    BackgroundJobRegistry.recordCycleStarted(name)

    watchdog.checkEvery30SecondsFor(10.minutes)

    assertThat(processExit.statuses).isEmpty()
  }

  @Test
  fun `a frozen process is no stall`() {
    val watchdog = EssentialJobWatchdog(StallReaction(WatchdogMode.Exit, processExit))
    BackgroundJobRegistry.recordCycleStarted(name)
    watchdog.check()

    advance(8.hours)
    watchdog.check()
    watchdog.checkEvery30SecondsFor(2.minutes)

    assertThat(processExit.statuses).isEmpty()
  }

  @Test
  fun `a freeze before the first check is no stall`() {
    val watchdog = EssentialJobWatchdog(StallReaction(WatchdogMode.Exit, processExit))
    BackgroundJobRegistry.recordCycleStarted(name)
    watchdog.markStarted()

    advance(8.hours)
    watchdog.check()

    assertThat(processExit.statuses).isEmpty()
  }

  @Test
  fun `a job that ends by itself ends an armed process at once`() {
    val watchdog = EssentialJobWatchdog(StallReaction(WatchdogMode.Exit, processExit))
    watchdog.start()
    try {
      BackgroundJobRegistry.recordJobEnded(name, RepeatingEndReason.StoppedItself)

      //Real time: the watchdog's own thread reports, while monotonicTimeSource is virtual here
      val deadline = System.nanoTime() + 5.seconds.inWholeNanoseconds
      while (processExit.statuses.isEmpty() && System.nanoTime() < deadline) {
        Thread.sleep(10)
      }
      assertThat(processExit.statuses).containsExactly(70)
    } finally {
      watchdog.stop()
    }
  }
}
