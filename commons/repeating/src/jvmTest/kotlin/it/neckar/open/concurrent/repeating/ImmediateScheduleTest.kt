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
import it.neckar.open.time.now
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.currentTime
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Test

@DisableLogging
class ImmediateScheduleTest {
  @Test
  fun `the schedule never waits`() = runTest {
    repeat(5) { ImmediateSchedule.awaitCycle(now()) }

    assertThat(currentTime).isEqualTo(0)
  }

  /**
   * The case this schedule exists for: the work blocks, so the loop is paced by the source, and the
   * source running dry ends it.
   */
  @Test
  fun `a job consumes its source until the source ends it`() = runTest {
    val lines = Channel<String?>(capacity = 4)
    val consumed = mutableListOf<String>()

    val repeating = Repeating.immediately {
      val line = lines.receive() ?: stopRepeating()
      consumed.add(line)
    }.start(this, this)

    lines.send("first")
    lines.send("second")
    lines.send(null)
    delay(50)
    repeating.cancel()

    assertThat(consumed).isEqualTo(listOf("first", "second"))
  }
}
