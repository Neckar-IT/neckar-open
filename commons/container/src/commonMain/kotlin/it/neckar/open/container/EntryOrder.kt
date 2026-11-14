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
package it.neckar.open.container

/** The order of entries: all versions of one name follow each other, the highest first; checked for the data entries and for `meta/type`. */
internal class EntryOrder {
  private var last: VersionedEntryName? = null
  private val completedNames = mutableSetOf<EntryName>()

  /** Accepts [versionedName] as the next entry; calls [fail] with the reason when it breaks the order. */
  fun accept(versionedName: VersionedEntryName, fail: (String) -> Nothing) {
    val name = versionedName.name
    val previous = last
    if (previous != null && previous.name == name) {
      val comparison = versionedName.version.compareTo(previous.version)
      if (comparison == 0) fail("Entry <$versionedName> is given twice")
      if (comparison > 0) fail("Entry <$versionedName> follows <$previous>; the versions of a name stand highest first")
    } else {
      previous?.let { completedNames += it.name }
      if (name in completedNames) fail("Entry <$versionedName> follows another name; all versions of <$name> follow each other")
    }
    last = versionedName
  }
}
