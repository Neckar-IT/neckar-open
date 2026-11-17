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

import it.neckar.open.version.Version
import kotlinx.serialization.json.Json

/** The fixed part of every container: `docs/container-format.html` in this module. */
object ContainerFormat {
  /** The directory of the entries the container format itself defines; no data entry lies in it. */
  const val MetaDirectory: String = "meta"

  /** The content of [FormatVersionPath] this library writes. */
  val formatVersion: Version = Version.of(1, 0, 0)

  /** The first entry; its name carries no version, so every reader finds it. */
  const val FormatVersionPath: String = "$MetaDirectory/version"

  /** The type entry this library writes and reads; its version is the one of the JSON layout of [ContainerType]. */
  val containerTypeEntry: VersionedEntryName = VersionedEntryName(EntryName("$MetaDirectory/type"), Version.of(1, 0, 0))

  /** Upper bound of the content of a meta entry, which a reader holds in memory. */
  const val MaxMetaEntryBytes: Long = 1024 * 1024

  /** Ignores unknown keys: a minor version may add fields. */
  internal val json: Json = Json { ignoreUnknownKeys = true }
}
