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

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.messageContains
import it.neckar.open.container.zip.ZipStreamWriter
import it.neckar.open.version.Version
import kotlinx.io.Buffer
import kotlinx.io.readByteArray
import kotlinx.io.writeString
import kotlin.test.Test

class ContainerRejectionTest {
  @Test
  fun rejectsAnEmptyContainer() {
    val buffer = Buffer()
    ZipStreamWriter.to(buffer).use { it.finish() }

    assertFailure { readAll(buffer.readByteArray()) }.isInstanceOf<InvalidContainerException>().hasMessage("Container has no entry, expected <meta/version>")
  }

  @Test
  fun rejectsAFirstEntryOtherThanTheVersion() {
    val bytes = rawContainer("meta/type/1.0.0" to BackupTypeJson)

    assertFailure { readAll(bytes) }.isInstanceOf<InvalidContainerException>().hasMessage("First entry is <meta/type/1.0.0>, expected <meta/version>")
  }

  @Test
  fun rejectsAnUnparsableFormatVersion() {
    val bytes = rawContainer("meta/version" to "one")

    assertFailure { readAll(bytes) }.isInstanceOf<InvalidContainerException>().hasMessage("<meta/version> contains no version <major>.<minor>.<patch>: Version <one> is no plain <major>.<minor>.<patch>")
  }

  @Test
  fun rejectsAnUnknownContainerFormatMajor() {
    val bytes = rawContainer("meta/version" to "2.0.0")

    assertFailure { readAll(bytes) }.isInstanceOf<InvalidContainerException>().messageContains("<2.0.0>")
  }

  @Test
  fun rejectsAMetaEntryAboveTheSizeLimit() {
    val bytes = rawContainer("meta/version" to "1".repeat(ContainerFormat.MaxMetaEntryBytes.toInt() + 1))

    assertFailure { readAll(bytes) }.isInstanceOf<InvalidContainerException>().hasMessage("<meta/version> exceeds 1048576 bytes")
  }

  @Test
  fun acceptsAMetaEntryAtTheSizeLimit() {
    val bytes = rawContainer("meta/version" to "1.0.0", "meta/type/1.0.0" to BackupTypeJson.padEnd(ContainerFormat.MaxMetaEntryBytes.toInt()))

    assertThat(readAll(bytes)).isEmpty()
  }

  @Test
  fun refusesTheNextEntryAfterAnInvalidName() {
    val bytes = rawContainer("meta/version" to "1.0.0", "meta/type/1.0.0" to BackupTypeJson, "volume/2.1.0" to "tar", "mongodb/dump" to "x", "mongodb/1.0.0" to "dump")

    ContainerReader(bytes.toSource(), backupExpectation).use { reader ->
      assertThat(reader.nextEntry()!!.versionedName.zipPath).isEqualTo("volume/2.1.0")
      assertFailure { reader.nextEntry() }.isInstanceOf<InvalidContainerException>()
      assertFailure { reader.nextEntry() }.isInstanceOf<IllegalStateException>()
    }
  }

  @Test
  fun rejectsANameThatReturnsAfterAnotherName() {
    val bytes = rawContainer("meta/version" to "1.0.0", "meta/type/1.0.0" to BackupTypeJson, "mongodb/1.0.0" to "a", "volume/2.1.0" to "b", "mongodb/1.1.0" to "c")

    assertFailure { readAll(bytes) }.isInstanceOf<InvalidContainerException>().hasMessage("Entry <mongodb/1.1.0> follows another name; all versions of <mongodb> follow each other")
  }

  @Test
  fun rejectsAscendingVersionsOfAName() {
    val bytes = rawContainer("meta/version" to "1.0.0", "meta/type/1.0.0" to BackupTypeJson, "mongodb/1.0.0" to "a", "mongodb/1.5.0" to "b")

    assertFailure { readAll(bytes) }.isInstanceOf<InvalidContainerException>().hasMessage("Entry <mongodb/1.5.0> follows <mongodb/1.0.0>; the versions of a name stand highest first")
  }

  @Test
  fun rejectsAscendingVersionsOfANameForAnyContent() {
    val bytes = rawContainer("meta/version" to "1.0.0", "meta/type/1.0.0" to BackupTypeJson, "mongodb/1.0.0" to "a", "mongodb/1.5.0" to "b")

    assertFailure { readAll(bytes, AnyContent) }.isInstanceOf<InvalidContainerException>().hasMessage("Entry <mongodb/1.5.0> follows <mongodb/1.0.0>; the versions of a name stand highest first")
  }

  @Test
  fun rejectsAnEntryNameWithoutValidUtf8() {
    val bytes = container { writeEntry(versionedName("volume/2.1.0")) { it.writeString("tar") } }
    bytes[bytes.lastSignature(ZipSignatures.LocalFileHeaderSignature) + 30] = 0xFF.toByte()

    assertFailure { readAll(bytes) }.isInstanceOf<InvalidContainerException>().messageContains("is no valid UTF-8")
  }

  /** A name of an untrusted container must not write a line break or terminal escape to stderr. */
  @Test
  fun escapesAControlCharacterOfARejectedName() {
    val bytes = rawContainer("meta/version" to "1.0.0", "meta/type/1.0.0" to BackupTypeJson, "volume\n../2.1.0" to "tar")

    assertFailure { readAll(bytes) }.isInstanceOf<InvalidContainerException>().messageContains("Entry <volume\\u000a../2.1.0> is no <name>/<version>")
  }

  @Test
  fun rejectsAContainerThatEndsAfterTheFormatVersion() {
    val bytes = rawContainer("meta/version" to "1.0.0")

    assertFailure { readAll(bytes) }.isInstanceOf<InvalidContainerException>().hasMessage("Second entry is <end of container>, expected <meta/type/<version>>")
  }

  @Test
  fun rejectsAContainerWithoutTypeEntry() {
    val bytes = rawContainer("meta/version" to "1.0.0", "volume/2.1.0" to "tar")

    assertFailure { readAll(bytes) }.isInstanceOf<InvalidContainerException>().hasMessage("Second entry is <volume/2.1.0>, expected <meta/type/<version>>")
  }

  @Test
  fun rejectsTypeEntriesWithUnknownMajorsOnly() {
    val bytes = rawContainer("meta/version" to "1.0.0", "meta/type/2.0.0" to "{}")

    assertFailure { readAll(bytes) }.isInstanceOf<InvalidContainerException>().messageContains("meta/type/2.0.0")
  }

  @Test
  fun rejectsATypeEntryWithoutValidUtf8() {
    val bytes = rawContainerOfBytes("meta/version" to "1.0.0".encodeToByteArray(), "meta/type/1.0.0" to byteArrayOf(0xFF.toByte()))

    assertFailure { readAll(bytes) }.isInstanceOf<InvalidContainerException>().hasMessage("<meta/type/1.0.0> is no valid UTF-8")
  }

  @Test
  fun rejectsAscendingTypeEntries() {
    val bytes = rawContainer("meta/version" to "1.0.0", "meta/type/1.0.0" to BackupTypeJson, "meta/type/1.5.0" to BackupTypeJson)

    assertFailure { readAll(bytes) }.isInstanceOf<InvalidContainerException>().hasMessage("Entry <meta/type/1.5.0> follows <meta/type/1.0.0>; the versions of a name stand highest first")
  }

  @Test
  fun rejectsATypeEntryTwice() {
    val bytes = rawContainer("meta/version" to "1.0.0", "meta/type/1.0.0" to BackupTypeJson, "meta/type/1.0.0" to BackupTypeJson)

    assertFailure { readAll(bytes) }.isInstanceOf<InvalidContainerException>().hasMessage("Entry <meta/type/1.0.0> is given twice")
  }

  @Test
  fun rejectsATypeVersionThatIsNoPlainVersion() {
    val bytes = rawContainer("meta/version" to "1.0.0", "meta/type/1.0.0" to """{"name": "it.neckar.backup.archive", "version": "02.3.0"}""")

    assertFailure { readAll(bytes) }.isInstanceOf<InvalidContainerException>().messageContains("<meta/type/1.0.0> contains no valid type")
  }

  @Test
  fun rejectsATypeEntryWithoutVersion() {
    val bytes = rawContainer("meta/version" to "1.0.0", "meta/type/1.0.0" to """{"name": "it.neckar.backup.archive"}""")

    assertFailure { readAll(bytes) }.isInstanceOf<InvalidContainerException>().messageContains("<meta/type/1.0.0> contains no valid type")
  }

  @Test
  fun rejectsAnotherType() {
    val bytes = container(ContainerType(ContainerTypeName("it.neckar.invoice.export"), Version.of(1, 0, 0))) { }

    assertFailure { readAll(bytes) }.isInstanceOf<InvalidContainerException>().hasMessage("Container type is <it.neckar.invoice.export>, expected <it.neckar.backup.archive>")
  }

  @Test
  fun rejectsAnUnknownTypeMajor() {
    val bytes = container(backupType.copy(version = Version.of(3, 0, 0))) { }

    assertFailure { readAll(bytes) }.isInstanceOf<InvalidContainerException>().messageContains("<3.0.0>")
  }

  @Test
  fun rejectsADataEntryWithoutVersion() {
    val bytes = rawContainer("meta/version" to "1.0.0", "meta/type/1.0.0" to BackupTypeJson, "mongodb/dump" to "x")

    assertFailure { readAll(bytes) }.isInstanceOf<InvalidContainerException>().messageContains("mongodb/dump")
  }

  @Test
  fun rejectsAChangedChecksum() {
    val bytes = container { writeEntry(versionedName("volume/2.1.0")) { it.writeString("tar") } }
    val descriptor = bytes.lastDataDescriptor()
    bytes[descriptor + 4] = (bytes[descriptor + 4] + 1).toByte()

    assertFailure { readAll(bytes) }.isInstanceOf<InvalidContainerException>().messageContains("CRC mismatch for entry: volume/2.1.0")
  }

  @Test
  fun rejectsAChangedUncompressedSize() {
    val bytes = container { writeEntry(versionedName("volume/2.1.0")) { it.writeString("tar") } }
    val descriptor = bytes.lastDataDescriptor()
    // signature, CRC-32, compressed size, then the uncompressed size; kmp-zip writes 4-byte sizes for a small entry
    bytes[descriptor + 12] = (bytes[descriptor + 12] + 1).toByte()

    assertFailure { readAll(bytes) }.isInstanceOf<InvalidContainerException>().messageContains("Data descriptor sizes do not match entry data: volume/2.1.0")
  }

  @Test
  fun rejectsAChangedCompressedSize() {
    val bytes = container { writeEntry(versionedName("volume/2.1.0")) { it.writeString("tar") } }
    val descriptor = bytes.lastDataDescriptor()
    bytes[descriptor + 8] = (bytes[descriptor + 8] + 1).toByte()

    assertFailure { readAll(bytes) }.isInstanceOf<InvalidContainerException>().messageContains("Data descriptor sizes do not match entry data: volume/2.1.0")
  }

  @Test
  fun rejectsCorruptDeflateData() {
    val bytes = container { writeEntry(versionedName("volume/2.1.0")) { it.writeString("tar") } }
    // BFINAL 1 with the reserved block type 3 is invalid deflate data on every platform.
    bytes[bytes.lastEntryDataStart()] = 0x07

    assertFailure { readAll(bytes) }.isInstanceOf<InvalidContainerException>().messageContains("Corrupt deflate stream for entry: volume/2.1.0")
  }

  @Test
  fun checksASkippedEntry() {
    val bytes = container {
      writeEntry(versionedName("volume/2.1.0")) { it.writeString("tar") }
      writeEntry(versionedName("postgres/1.0.0")) { it.writeString("pg") }
    }
    val descriptor = bytes.lastDataDescriptor()
    bytes[descriptor + 4] = (bytes[descriptor + 4] + 1).toByte()

    assertFailure { readAll(bytes) }.isInstanceOf<InvalidContainerException>()
      .messageContains("CRC mismatch for entry: postgres/1.0.0")
  }

  @Test
  fun rejectsAnUnexpectedSignature() {
    val bytes = container { writeEntry(versionedName("volume/2.1.0")) { it.writeString("tar") } }
    bytes[bytes.lastSignature(ZipSignatures.LocalFileHeaderSignature) + 2] = 9

    assertFailure { readAll(bytes) }.isInstanceOf<InvalidContainerException>().messageContains("Bad header signature 0x04094b50")
  }

  @Test
  fun rejectsAnUnsupportedMethod() {
    val bytes = container { writeEntry(versionedName("volume/2.1.0")) { it.writeString("tar") } }
    // method field of the local file header; 12 is bzip2
    bytes[bytes.lastSignature(ZipSignatures.LocalFileHeaderSignature) + 8] = 12

    assertFailure { readAll(bytes) }.isInstanceOf<InvalidContainerException>().messageContains("Unsupported compression method: 12")
  }

  @Test
  fun rejectsAStoredEntryWithDataDescriptor() {
    val bytes = container { writeEntry(versionedName("volume/2.1.0")) { it.writeString("tar") } }
    bytes[bytes.lastSignature(ZipSignatures.LocalFileHeaderSignature) + 8] = 0

    assertFailure { readAll(bytes) }.isInstanceOf<InvalidContainerException>().messageContains("STORED entry with a data descriptor")
  }

  @Test
  fun rejectsAnEncryptedEntry() {
    val bytes = container { writeEntry(versionedName("volume/2.1.0")) { it.writeString("tar") } }
    val flags = bytes.lastSignature(ZipSignatures.LocalFileHeaderSignature) + 6
    bytes[flags] = (bytes[flags].toInt() or ZipSignatures.FlagEncrypted).toByte()

    assertFailure { readAll(bytes) }.isInstanceOf<InvalidContainerException>().messageContains("Password required for encrypted entry: volume/2.1.0")
  }

  @Test
  fun rejectsAContainerWithoutCentralDirectory() {
    val bytes = container { writeEntry(versionedName("volume/2.1.0")) { it.writeString("tar") } }

    assertFailure { readAll(bytes.copyOf(bytes.firstSignature(ZipSignatures.CentralDirectoryHeaderSignature))) }
      .isInstanceOf<InvalidContainerException>().messageContains("input ended before the central directory")
  }

  @Test
  fun rejectsAContainerWithoutEndOfCentralDirectoryRecord() {
    val bytes = container { writeEntry(versionedName("volume/2.1.0")) { it.writeString("tar") } }

    assertFailure { readAll(bytes.copyOf(bytes.lastSignature(ZipSignatures.EndOfCentralDirectorySignature))) }
      .isInstanceOf<InvalidContainerException>().messageContains("Unexpected end of ZIP stream")
  }

  @Test
  fun rejectsAnEndOfCentralDirectoryWithAnotherEntryCount() {
    val bytes = container { writeEntry(versionedName("volume/2.1.0")) { it.writeString("tar") } }
    // total entry count field of the end of central directory record
    val totalEntries = bytes.lastSignature(ZipSignatures.EndOfCentralDirectorySignature) + 10
    bytes[totalEntries] = (bytes[totalEntries] + 1).toByte()

    assertFailure { readAll(bytes) }.isInstanceOf<InvalidContainerException>().messageContains("End of central directory declares 4 entries, central directory has 3")
  }

  @Test
  fun rejectsACentralDirectoryNameOtherThanTheLocalName() {
    val bytes = container { writeEntry(versionedName("volume/2.1.0")) { it.writeString("tar") } }
    // first byte of the name, which follows the 46 fixed bytes of the last central directory header
    bytes[bytes.lastSignature(ZipSignatures.CentralDirectoryHeaderSignature) + 46] = 'w'.code.toByte()

    assertFailure { readAll(bytes) }.isInstanceOf<InvalidContainerException>().messageContains("Central directory name does not match local entry: wolume/2.1.0")
  }

  @Test
  fun refusesTheNextEntryAfterAFailedRead() {
    val bytes = container {
      writeEntry(versionedName("volume/2.1.0")) { it.writeString("tar") }
      writeEntry(versionedName("mongodb/1.0.0")) { it.writeString("dump") }
    }
    // The data descriptor of volume/2.1.0 ends at the local header of mongodb/1.0.0: signature, CRC-32 and two 4-byte sizes.
    val volumeDescriptor = bytes.lastSignature(ZipSignatures.LocalFileHeaderSignature) - 16
    bytes[volumeDescriptor + 4] = (bytes[volumeDescriptor + 4] + 1).toByte()

    ContainerReader(bytes.toSource(), backupExpectation).use { reader ->
      val volume = reader.nextEntry()!!
      assertFailure { volume.content.readByteArray() }.isInstanceOf<InvalidContainerException>().messageContains("CRC mismatch for entry: volume/2.1.0")
      assertFailure { volume.content.readByteArray() }.isInstanceOf<IllegalStateException>()
      assertFailure { reader.nextEntry() }.isInstanceOf<IllegalStateException>()
    }
  }

  @Test
  fun rejectsATruncatedContainer() {
    val bytes = container { writeEntry(versionedName("config/users/1.2.0")) { it.write(randomContent) } }

    assertFailure { readAll(bytes.copyOf(bytes.size / 2)) }.isInstanceOf<InvalidContainerException>().messageContains("entry <config/users/1.2.0>: Unexpected end of ZIP stream")
  }

  /** The offset of the deflate data of the last entry: behind its local file header, name and extra field. */
  private fun ByteArray.lastEntryDataStart(): Int {
    val header = lastSignature(ZipSignatures.LocalFileHeaderSignature)
    return header + 30 + readShortLe(header + 26) + readShortLe(header + 28)
  }

  private fun ByteArray.lastDataDescriptor(): Int = lastSignature(ZipSignatures.DataDescriptorSignature)
}
