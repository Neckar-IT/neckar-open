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
package it.neckar.ktor.archive

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isSameInstanceAs
import it.neckar.lifeCycle.ArchiveState
import it.neckar.rest.UnarchiveResponse
import org.junit.jupiter.api.Test

class UnarchiveResultTest {
  @Test
  fun `createFor maps every unarchive result`() {
    assertThat(UnarchiveResponse.createFor(UnarchiveResult.Unarchived)).isEqualTo(UnarchiveResponse.Good)
    assertThat(UnarchiveResponse.createFor(UnarchiveResult.AlreadyActive)).isEqualTo(UnarchiveResponse.AlreadyActive)
    assertThat(UnarchiveResponse.createFor(UnarchiveResult.NotFound)).isEqualTo(UnarchiveResponse.NotFound)
  }

  @Test
  fun `fromPrevious restores an archived entity and runs onUnarchived once`() {
    var calls = 0

    val result = UnarchiveResult.fromPrevious(ArchiveState.Archived) { calls++ }

    assertThat(result).isSameInstanceAs(UnarchiveResult.Unarchived)
    assertThat(calls).isEqualTo(1)
  }

  @Test
  fun `fromPrevious reports an active entity as already active without running onUnarchived`() {
    val result = UnarchiveResult.fromPrevious(ArchiveState.Active) { throw AssertionError("onUnarchived must not run") }

    assertThat(result).isSameInstanceAs(UnarchiveResult.AlreadyActive)
  }

  @Test
  fun `fromPrevious reports a missing entity as not found without running onUnarchived`() {
    val result = UnarchiveResult.fromPrevious(null) { throw AssertionError("onUnarchived must not run") }

    assertThat(result).isSameInstanceAs(UnarchiveResult.NotFound)
  }
}
