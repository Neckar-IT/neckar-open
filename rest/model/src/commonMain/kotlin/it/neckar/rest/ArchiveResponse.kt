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
package it.neckar.rest

import it.neckar.rest.draft.UsedBy
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

/**
 * @param E the enum type identifying the referencing entity types listed in [Archived.usedBy]
 */
@Serializable
sealed interface ArchiveResponse<out E> : RestResponse<ArchiveResponse.Archived<E>> {

  /**
   * [usedBy] lists the active entities that still reference the archived entity.
   */
  @Serializable
  @SerialName("good")
  data class Archived<out E>(val usedBy: UsedBy<E>) : ArchiveResponse<E>, RestResponse.GoodResponse<Archived<E>> {
    override val data: Archived<E> get() = this
  }

  @Serializable
  @SerialName("bad/AlreadyArchived")
  data object AlreadyArchived : ArchiveResponse<Nothing>, RestResponse.BadResponse<Archived<Nothing>>

  @Serializable
  @SerialName("bad/NotFound")
  data object NotFound : ArchiveResponse<Nothing>, RestResponse.BadResponse<Archived<Nothing>>

  companion object {
    fun <E> archived(usedBy: UsedBy<E>): ArchiveResponse<E> = Archived(usedBy)

    fun alreadyArchived(): ArchiveResponse<Nothing> = AlreadyArchived

    fun notFound(): ArchiveResponse<Nothing> = NotFound
  }
}
