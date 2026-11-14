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

import it.neckar.open.kotlin.lang.toCheckboxChar
import kotlinx.serialization.Serializable

/**
 * Describes the archive state for an element.
 *
 * Elements marked as [Archived] must not be selected / published anymore, but they remain in the
 * database so existing references stay valid. Archiving is reversible. User-facing terminology is
 * "Aktiv" (Active) and "Archiviert" (Archived).
 */
@Serializable
enum class ArchiveState {
  /**
   * The element is active. It can be used.
   */
  Active,

  /**
   * The element is archived. It is hidden from default listings but remains in the database so
   * existing references stay valid; archiving is reversible.
   */
  Archived,

  ;

  /**
   * Returns the combined ("worst") state from two states: [Active] only if both are [Active],
   * otherwise [Archived].
   *
   * A `null` [other] contributes nothing and leaves `this` unchanged — it does not count as
   * [Archived].
   */
  fun and(other: ArchiveState?): ArchiveState {
    if (other == null) {
      return this
    }

    if (this == Archived || other == Archived) {
      return Archived
    }

    // Guards a state added later: without it an unknown constant would silently combine to Active.
    require(this == Active && other == Active) {
      "Unexpected combination this: <$this> other: <$other>"
    }

    return Active
  }

  fun isActive(): Boolean {
    return this == Active
  }

  /**
   * Returns `true` for every state except [Archived]. Equivalent to [isActive] as long as the enum
   * has exactly these two states; it expresses the criterion "not archived" rather than one state.
   */
  fun isNotArchived(): Boolean {
    return this != Archived
  }

  fun isArchived(): Boolean {
    return this == Archived
  }

  /**
   * Renders the state as a checkbox character: `☑` for [Active], `☐` for [Archived].
   * Not localized, therefore no substitute for a user-facing label.
   */
  fun format(): String {
    //TODO add i18n
    return when (this) {
      Active -> true.toCheckboxChar()
      Archived -> false.toCheckboxChar()
    }
  }

}
