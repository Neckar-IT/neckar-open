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

import it.neckar.open.container.zip.ZipStreamWriter
import it.neckar.open.version.Version

/** The name `<name>/<version>` of a ZIP entry; the version is the last segment. */
data class VersionedEntryName(
  val name: EntryName,
  /** The version of the content of this entry. */
  val version: Version,
) {
  init {
    require(version.isContainerVersion) { "Version <${version.format()}> of <$name> is no <major>.<minor>.<patch>" }
    require(zipPath.encodeToByteArray().size <= ZipStreamWriter.MaxEntryNameBytes) { "ZIP path of <$name> exceeds ${ZipStreamWriter.MaxEntryNameBytes} bytes" }
  }

  /** The name of the ZIP entry. */
  val zipPath: String
    get() = "$name/${version.format()}"

  override fun toString(): String = zipPath

  companion object {
    /** Parses `<name>/<version>`; throws [IllegalArgumentException] for any other ZIP path. */
    fun parse(zipPath: String): VersionedEntryName {
      val separator = zipPath.lastIndexOf('/')
      require(separator >= 0) { "<${zipPath.escapeControlCharacters()}> has no version segment after a slash" }
      require(separator > 0) { "<${zipPath.escapeControlCharacters()}> has no name before its version" }
      return VersionedEntryName(EntryName(zipPath.substring(0, separator)), parseContainerVersion(zipPath.substring(separator + 1)))
    }
  }
}
