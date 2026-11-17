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
package it.neckar.rest.draft

import kotlinx.serialization.Serializable

/**
 * Counts how many entities of a specific [type] reference this entity, split by archive state.
 *
 * Used in detail responses to inform the frontend which dependents would be affected by actions
 * on the parent entity. Every field names its scope explicitly — `active*` fields describe
 * non-archived references (the primary warning count), [archivedCount] describes historical
 * references that stay around but do not contribute to the main impact warning.
 *
 * [activeCount] is always present. [activeConfirmedCount] and [activeDraftCount] are only set
 * when the referencing entity follows the Draft/Confirmed pattern; when both are present,
 * `activeCount == activeConfirmedCount + activeDraftCount`.
 *
 * [archivedCount] is only set when the referencing entity type has an archive state.
 *
 * @param T the enum type that identifies the referencing entity type
 */
@Serializable
data class UsedByCount<out T>(
  /**
   * The type of the referencing entity (e.g., Book).
   */
  val type: T,

  /**
   * Number of **active** (non-archived) entities of this type referencing this entity.
   * The primary count for archive/confirm warnings.
   */
  val activeCount: Int,

  /**
   * Number of active Confirmed entities of this type referencing this entity.
   * Null if the referencing entity type does not follow the Draft/Confirmed pattern.
   */
  val activeConfirmedCount: Int? = null,

  /**
   * Number of active Draft entities of this type referencing this entity.
   * Null if the referencing entity type does not follow the Draft/Confirmed pattern.
   */
  val activeDraftCount: Int? = null,

  /**
   * Number of **archived** entities of this type referencing this entity. Separate from
   * [activeCount] so the frontend can surface archived references as a secondary, informational
   * hint without inflating the primary warning. Null if the referencing entity type has no
   * archive state.
   */
  val archivedCount: Int? = null,
)
