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
import it.neckar.rest.draft.UsedBy
import it.neckar.rest.draft.UsedByCount
import org.junit.jupiter.api.Test

class ArchiveResultTest {
  @Test
  fun `fromPrevious archives an active entity and resolves usedBy once`() {
    val usedBy = UsedBy(listOf(UsedByCount(type = "Book", activeCount = 2)))
    var calls = 0

    val result = ArchiveResult.fromPrevious(ArchiveState.Active) {
      calls++
      usedBy
    }

    assertThat(result).isEqualTo(ArchiveResult.Archived(usedBy))
    assertThat(calls).isEqualTo(1)
  }

  @Test
  fun `fromPrevious reports an archived entity as already archived without resolving usedBy`() {
    val result = ArchiveResult.fromPrevious<String>(ArchiveState.Archived) { throw AssertionError("onArchived must not run") }

    assertThat(result).isSameInstanceAs(ArchiveResult.AlreadyArchived)
  }

  @Test
  fun `fromPrevious reports a missing entity as not found without resolving usedBy`() {
    val result = ArchiveResult.fromPrevious<String>(null) { throw AssertionError("onArchived must not run") }

    assertThat(result).isSameInstanceAs(ArchiveResult.NotFound)
  }
}
