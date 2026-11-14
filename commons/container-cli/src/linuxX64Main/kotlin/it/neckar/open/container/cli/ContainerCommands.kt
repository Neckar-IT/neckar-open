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
package it.neckar.open.container.cli

import it.neckar.open.container.AnyContent
import it.neckar.open.container.ContainerReader
import it.neckar.open.container.ContainerType
import it.neckar.open.container.ContainerWriter
import it.neckar.open.container.VersionedEntryName
import it.neckar.open.version.Version
import kotlinx.io.IOException
import kotlinx.io.Sink
import kotlinx.io.Source
import kotlinx.io.discardingSink
import kotlinx.io.writeString
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Writes the container of [invocation] to [sink] and closes [sink]; after a failed command [sink] holds no central directory. */
internal fun writeContainer(invocation: Invocation.Write, sink: Sink) {
  ContainerWriter(sink, invocation.containerType).use { writer ->
    invocation.entries.forEach { entry ->
      try {
        writer.writeEntry(entry.versionedName, entry.compressionLevel) { entrySink -> copyCommandOutput(entry.command, entrySink) }
      } catch (e: Exception) {
        throw IOException("Entry <${entry.versionedName}> failed: ${e.message}", e)
      }
    }
  }
}

/** Writes the header of the container in [source] as JSON to [out], then one line `<name>/<version>\t<uncompressed bytes>` per data entry. */
internal fun listContainer(source: Source, out: Sink) {
  ContainerReader(source, AnyContent).use { reader ->
    out.writeString(Json.encodeToString(ListingHeader.serializer(), ListingHeader(reader.formatVersion, reader.containerType)) + "\n")
    out.flush()
    while (true) {
      val entry = reader.nextEntry() ?: break
      val size = entry.content.transferTo(discardingSink())
      out.writeString("${entry.versionedName}\t$size\n")
      out.flush()
    }
  }
}

/** The first line of [listContainer]. */
@Serializable
private data class ListingHeader(
  val formatVersion: Version,
  val type: ContainerType,
)

/** Copies the content of the entry [invocation] names from the container in [source] to [out], then reads [source] to the end of the container. */
internal fun extractEntry(invocation: Invocation.Extract, source: Source, out: Sink) {
  ContainerReader(source, AnyContent).use { reader ->
    val passed = mutableListOf<VersionedEntryName>()
    while (true) {
      val entry = reader.nextEntry() ?: throw IOException(notFoundMessage(invocation, passed))
      val versionedName = entry.versionedName
      if (versionedName.name == invocation.name && (invocation.majors.isEmpty() || versionedName.version.major in invocation.majors)) {
        entry.content.transferTo(out)
        out.flush()
        // Reading to the end checks CRC-32 and size of every entry and lets the writer of stdin finish without SIGPIPE.
        while (reader.nextEntry() != null) {
          continue
        }
        return
      }
      passed += versionedName
    }
  }
}

private fun notFoundMessage(invocation: Invocation.Extract, passed: List<VersionedEntryName>): String {
  val majors = if (invocation.majors.isEmpty()) "" else " with major ${invocation.majors.sorted().joinToString(" or ")}"
  val found = if (passed.isEmpty()) "none" else passed.joinToString()
  return "Container has no entry <${invocation.name}>$majors; entries: $found"
}
