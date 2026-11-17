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

import it.neckar.lifeCycle.ArchiveState
import it.neckar.rest.UnarchiveResponse

/**
 * Business result of an unarchive operation on an archivable entity.
 *
 * Support-layer type. Routing maps this to the REST response.
 *
 * Lives in this shared module because the spec code generator emits support classes that return it:
 * a copy per service would be the hand-written glue the generator exists to avoid.
 *
 * Unlike [ArchiveResult], this type carries no `usedBy` payload: restoring an archived entity
 * does not produce a user warning — there is nothing to warn about when the entity becomes
 * visible again. That is why there is no type parameter here.
 */
sealed interface UnarchiveResult {
  /**
   * Entity was restored to [ArchiveState.Active].
   */
  data object Unarchived : UnarchiveResult

  /**
   * Entity was already active — no change applied.
   */
  data object AlreadyActive : UnarchiveResult

  /**
   * Entity with the given id was not found in the tenant.
   */
  data object NotFound : UnarchiveResult

  companion object {
    /**
     * [previous] is the archive state stored before the write, null if the tenant holds no such entity.
     * [onUnarchived] runs only when this call restored the entity.
     */
    inline fun fromPrevious(previous: ArchiveState?, onUnarchived: () -> Unit = {}): UnarchiveResult = when (previous) {
      null -> NotFound
      ArchiveState.Active -> AlreadyActive
      ArchiveState.Archived -> {
        onUnarchived()
        Unarchived
      }
    }
  }
}

fun UnarchiveResponse.Companion.createFor(result: UnarchiveResult): UnarchiveResponse = when (result) {
  UnarchiveResult.Unarchived -> good()
  UnarchiveResult.AlreadyActive -> alreadyActive()
  UnarchiveResult.NotFound -> notFound()
}
