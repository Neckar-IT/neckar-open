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
import it.neckar.open.test.utils.DisableLogging
import org.junit.jupiter.api.Test

@DisableLogging
class StallReactionTest {
  private val processExit = RecordingProcessExit()

  @Test
  fun `an armed reaction ends the process with 70`() {
    StallReaction(WatchdogMode.Exit, processExit).stalled(stall = "auto-merge-cycle", cause = "Essential background job [auto-merge-cycle] ended")

    assertThat(processExit.statuses).containsExactly(70)
  }

  @Test
  fun `an unarmed reaction keeps the process running`() {
    StallReaction(WatchdogMode.LogOnly, processExit).stalled(stall = "auto-merge-cycle", cause = "Essential background job [auto-merge-cycle] ended")

    assertThat(processExit.statuses).isEmpty()
  }

  @Test
  fun `an attached debugger keeps the process running`() {
    StallReaction(WatchdogMode.DebuggerAttached, processExit).stalled(stall = "auto-merge-cycle", cause = "Essential background job [auto-merge-cycle] ended")

    assertThat(processExit.statuses).isEmpty()
  }

  @Test
  fun `a stopping service keeps the process running`() {
    val reaction = StallReaction(WatchdogMode.Exit, processExit)
    reaction.serviceStopping()

    reaction.stalled(stall = "auto-merge-cycle", cause = "Essential background job [auto-merge-cycle] ended")

    assertThat(reaction.judging).isFalse()
    assertThat(processExit.statuses).isEmpty()
  }
}
