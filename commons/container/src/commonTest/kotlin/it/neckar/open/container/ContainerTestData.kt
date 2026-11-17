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
import kotlinx.io.Buffer
import kotlinx.io.readByteArray
import kotlin.random.Random

/** Record signatures and flags of PKWARE APPNOTE 6.3.10 that the tests look for in written bytes. */
internal object ZipSignatures {
  const val LocalFileHeaderSignature: Int = 0x04034b50
  const val DataDescriptorSignature: Int = 0x08074b50
  const val CentralDirectoryHeaderSignature: Int = 0x02014b50
  const val EndOfCentralDirectorySignature: Int = 0x06054b50
  const val FlagEncrypted: Int = 1
}

internal val backupType: ContainerType = ContainerType(ContainerTypeName("it.neckar.backup.archive"), Version.of(2, 3, 0))

internal val backupExpectation: ExpectedContent = ExpectedContent(
  containerTypeName = ContainerTypeName("it.neckar.backup.archive"),
  containerTypeMajors = setOf(2),
  entries = listOf(ExpectedEntry(EntryName("mongodb"), setOf(1)), ExpectedEntry(EntryName("volume"), setOf(2)), ExpectedEntry(EntryName("config/users"), setOf(1))),
)

/** 3 MiB that deflate cannot shrink, so the entry spans many buffers. */
internal val randomContent: ByteArray = Random(42).nextBytes(3 * 1024 * 1024)

internal fun versionedName(path: String): VersionedEntryName = VersionedEntryName.parse(path)

/** Writes a container with [ContainerWriter] and returns its bytes. */
internal fun container(containerType: ContainerType = backupType, entries: ContainerWriter.() -> Unit): ByteArray {
  val buffer = Buffer()
  ContainerWriter(buffer, containerType).use { it.entries() }
  return buffer.readByteArray()
}

/** Writes raw ZIP entries, for containers [ContainerWriter] refuses to write. */
internal fun rawContainer(vararg entries: Pair<String, String>): ByteArray = rawContainerOfBytes(*entries.map { (name, content) -> name to content.encodeToByteArray() }.toTypedArray())

/** Writes raw ZIP entries with arbitrary bytes, e.g. invalid UTF-8. */
internal fun rawContainerOfBytes(vararg entries: Pair<String, ByteArray>): ByteArray {
  val buffer = Buffer()
  ZipStreamWriter.to(buffer).use { zip ->
    entries.forEach { (name, content) ->
      zip.startEntry(name, 6).use { it.write(content) }
    }
    zip.finish()
  }
  return buffer.readByteArray()
}

/** The JSON of [backupType] in `meta/type/1.0.0`. */
internal const val BackupTypeJson: String = """{"name": "it.neckar.backup.archive", "version": "2.3.0"}"""

/** True when the bytes contain [signature] as little-endian int, e.g. a ZIP record signature. */
internal fun ByteArray.containsSignature(signature: Int): Boolean = (0..size - 4).any { hasSignatureAt(it, signature) }

/** The offset of the last occurrence of [signature] as little-endian int. */
internal fun ByteArray.lastSignature(signature: Int): Int = (size - 4 downTo 0).first { hasSignatureAt(it, signature) }

/** The offset of the first occurrence of [signature] as little-endian int. */
internal fun ByteArray.firstSignature(signature: Int): Int = (0..size - 4).first { hasSignatureAt(it, signature) }

private fun ByteArray.hasSignatureAt(offset: Int, signature: Int): Boolean = (0 until 4).all { this[offset + it] == (signature shr (8 * it)).toByte() }

internal fun ByteArray.readShortLe(offset: Int): Int = (this[offset].toInt() and 0xFF) or ((this[offset + 1].toInt() and 0xFF) shl 8)

internal fun ByteArray.toSource(): Buffer = Buffer().also { it.write(this) }

/** Reads every entry [expected] covers, as ZIP path to content. */
internal fun readAll(bytes: ByteArray, expected: ContainerExpectation = backupExpectation): List<Pair<String, ByteArray>> {
  return ContainerReader(bytes.toSource(), expected).use { reader ->
    generateSequence { reader.nextEntry()?.let { it.versionedName.zipPath to it.content.readByteArray() } }.toList()
  }
}
