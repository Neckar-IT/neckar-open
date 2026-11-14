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
import it.neckar.rest.ArchiveResponse
import it.neckar.rest.draft.UsedBy

/**
 * Business result of an archive operation on an archivable entity.
 *
 * Support-layer type. Routing maps this to the REST response.
 *
 * Lives in this shared module because the spec code generator emits support classes that return it:
 * a copy per service would be the hand-written glue the generator exists to avoid.
 *
 * @param E the enum type identifying referencing entity types (for the usedBy warning)
 */
sealed interface ArchiveResult<out E> {
  /**
   * Entity was archived. [usedBy] lists active entities that reference it — shown as a
   * warning to the user; references remain valid because archiving is reversible.
   */
  data class Archived<E>(val usedBy: UsedBy<E>) : ArchiveResult<E>

  /**
   * Entity was already archived (in [ArchiveState.Archived]) — no change applied.
   */
  data object AlreadyArchived : ArchiveResult<Nothing>

  /**
   * Entity with the given id was not found in the tenant.
   */
  data object NotFound : ArchiveResult<Nothing>

  companion object {
    /**
     * [previous] is the archive state stored before the write, null if the tenant holds no such entity.
     * [onArchived] runs only when this call archived the entity.
     */
    inline fun <E> fromPrevious(previous: ArchiveState?, onArchived: () -> UsedBy<E>): ArchiveResult<E> = when (previous) {
      null -> NotFound
      ArchiveState.Archived -> AlreadyArchived
      ArchiveState.Active -> Archived(onArchived())
    }
  }
}

fun <E> ArchiveResponse.Companion.createFor(result: ArchiveResult<E>): ArchiveResponse<E> = when (result) {
  is ArchiveResult.Archived -> archived(result.usedBy)
  ArchiveResult.AlreadyArchived -> alreadyArchived()
  ArchiveResult.NotFound -> notFound()
}
