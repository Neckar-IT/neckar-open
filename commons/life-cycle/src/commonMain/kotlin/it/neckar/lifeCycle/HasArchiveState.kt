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
package it.neckar.lifeCycle

import it.neckar.open.annotations.CrossLayer

/**
 * Describes elements that carry an [ArchiveState].
 */
@CrossLayer("ArchiveState trait carried by every layer")
interface HasArchiveState {
  /**
   * The archive state for the element.
   */
  val archiveState: ArchiveState

  fun isActive(): Boolean {
    return archiveState.isActive()
  }

  fun isNotArchived(): Boolean {
    return archiveState.isNotArchived()
  }

  fun isArchived(): Boolean {
    return archiveState.isArchived()
  }
}

/**
 * Returns a list containing only the active (not [HasArchiveState.isArchived]) elements.
 */
fun <T : HasArchiveState> List<T>.onlyActive(): List<T> {
  return only(ArchiveState::isNotArchived)
}

/**
 * Returns only the elements for which the [selector] returns `true`.
 * If the [selector] is `null`, all elements are returned.
 */
fun <T : HasArchiveState> List<T>.only(selector: ((ArchiveState) -> Boolean)?): List<T> {
  if (selector == null) return this

  return this.filter {
    selector(it.archiveState)
  }
}
