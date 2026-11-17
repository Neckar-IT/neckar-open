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

/** What an application understands; [ContainerReader] rejects or skips everything else. */
data class ExpectedContent(
  /** The expected [ContainerType.name]. */
  val containerTypeName: ContainerTypeName,
  /** The majors of [ContainerType.version] the reader understands. */
  val containerTypeMajors: Set<Int>,
  /** The entries the reader understands; entries of other names are skipped. */
  val entries: List<ExpectedEntry>,
) : ContainerExpectation {
  init {
    require(containerTypeMajors.isNotEmpty()) { "No major listed for <$containerTypeName>" }
    val duplicates = entries.groupBy { it.name }.filterValues { it.size > 1 }.keys
    require(duplicates.isEmpty()) { "Entry names listed twice: $duplicates" }
    val metaNames = entries.map { it.name }.filter { it.isInMetaDirectory }
    require(metaNames.isEmpty()) { "Entry names in <${ContainerFormat.MetaDirectory}/> belong to the container format: $metaNames" }
  }

  /** True when the reader understands the version of [versionedName]. */
  fun understands(versionedName: VersionedEntryName): Boolean {
    val expectedEntry = entries.find { it.name == versionedName.name } ?: return false
    return versionedName.version.major in expectedEntry.majors
  }
}

