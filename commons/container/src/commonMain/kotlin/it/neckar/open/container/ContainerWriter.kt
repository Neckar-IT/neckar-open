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
import kotlinx.io.Sink
import kotlinx.io.writeString

/**
 * Writes a container to [sink]: `meta/version` and `meta/type/1.0.0` on construction, then the data entries.
 * [close] writes the central directory and closes [sink]; a failing constructor closes [sink] as well.
 */
class ContainerWriter(
  sink: Sink,
  containerType: ContainerType,
) : AutoCloseable {
  private val zip = ZipStreamWriter.to(sink)
  private val order = EntryOrder()

  private var closed = false

  init {
    try {
      writeZipEntry(ContainerFormat.FormatVersionPath, CompressionLevel.Default) {
        it.writeString(ContainerFormat.formatVersion.format())
      }
      writeZipEntry(ContainerFormat.containerTypeEntry.zipPath, CompressionLevel.Default) {
        it.writeString(ContainerFormat.json.encodeToString(ContainerType.serializer(), containerType))
      }
    } catch (e: Throwable) {
      runCatching { zip.close() }.exceptionOrNull()?.let { e.addSuppressed(it) }
      throw e
    }
  }

  /**
   * Writes the entry [versionedName] with the bytes [content] writes.
   * All versions of one entry name follow each other, the highest first.
   */
  fun writeEntry(versionedName: VersionedEntryName, compressionLevel: CompressionLevel = CompressionLevel.Default, content: (Sink) -> Unit) {
    check(closed.not()) { "The container is closed" }
    check(zip.aborted.not()) { "An earlier entry failed" }
    acceptDataEntry(versionedName, order)
    writeZipEntry(versionedName.zipPath, compressionLevel, content)
  }

  private fun writeZipEntry(path: String, compressionLevel: CompressionLevel, content: (Sink) -> Unit) {
    try {
      val entrySink = zip.startEntry(path, compressionLevel.deflateLevel)
      content(entrySink)
      entrySink.close()
    } catch (e: Throwable) {
      zip.abortEntry()
      throw e
    }
  }

  companion object {
    /** Throws [IllegalArgumentException] for the first of [versionedNames] that [writeEntry] would reject in this order. */
    fun verifyEntryNames(versionedNames: List<VersionedEntryName>) {
      val order = EntryOrder()
      versionedNames.forEach { acceptDataEntry(it, order) }
    }

    /** Rejects a name in the meta directory and a break of [order]. */
    private fun acceptDataEntry(versionedName: VersionedEntryName, order: EntryOrder) {
      require(versionedName.name.isInMetaDirectory.not()) { "Entry <$versionedName> lies in <${ContainerFormat.MetaDirectory}/>, which the container format defines" }
      order.accept(versionedName) { throw IllegalArgumentException(it) }
    }
  }

  /** Writes the central directory and closes the sink; after a failed entry it closes the sink and throws. */
  override fun close() {
    if (closed) return
    closed = true
    zip.use {
      check(it.aborted.not()) { "Container is incomplete: an entry failed, so no central directory was written" }
      it.finish()
    }
  }
}
