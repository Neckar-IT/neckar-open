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
package it.neckar.open.concurrent.repeating

import assertk.*
import assertk.assertions.*
import it.neckar.open.test.utils.DisableLogging
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test
import kotlin.time.Duration.Companion.milliseconds

@DisableLogging
class RepeatingJobsTest {
  @Test
  fun `cancelAll stops every job and stays quiet the second time`() = runTest {
    var firstCycles = 0
    var secondCycles = 0
    val jobs = RepeatingJobs()

    jobs.add(Repeating.every(20.milliseconds) { firstCycles++ }.start(this, this))
    jobs.add(Repeating.every(20.milliseconds) { secondCycles++ }.start(this, this))
    assertThat(jobs.size).isEqualTo(2)

    delay(50)
    jobs.cancelAll()
    jobs.cancelAll()
    val cyclesAtShutdown = firstCycles to secondCycles

    delay(200)

    assertThat(jobs.size).isEqualTo(0)
    assertThat(firstCycles to secondCycles).isEqualTo(cyclesAtShutdown)
  }

  @Test
  suspend fun `add returns the job so a caller can keep it`(): Unit = coroutineScope {
    val jobs = RepeatingJobs()
    val repeating = Repeating.every(20.milliseconds) {}.start(this, this)

    assertThat(jobs.add(repeating)).isSameInstanceAs(repeating)

    jobs.cancelAll()
  }

  @Test
  fun `dispose cancels every job`() = runTest {
    var cycles = 0
    val jobs = RepeatingJobs()
    jobs.add(Repeating.every(20.milliseconds) { cycles++ }.start(this, this))

    delay(50)
    jobs.dispose()
    val cyclesAtShutdown = cycles
    delay(200)

    assertThat(cycles).isEqualTo(cyclesAtShutdown)
  }
}
