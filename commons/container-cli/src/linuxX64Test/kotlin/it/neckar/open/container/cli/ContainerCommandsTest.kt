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

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isGreaterThan
import assertk.assertions.isInstanceOf
import assertk.assertions.isLessThan
import assertk.assertions.messageContains
import it.neckar.open.container.AnyContent
import it.neckar.open.container.CompressionLevel
import it.neckar.open.container.ContainerReader
import it.neckar.open.container.ContainerType
import it.neckar.open.container.ContainerTypeName
import it.neckar.open.container.ContainerWriter
import it.neckar.open.container.EntryName
import it.neckar.open.container.InvalidContainerException
import it.neckar.open.container.VersionedEntryName
import it.neckar.open.version.Version
import kotlinx.io.Buffer
import kotlinx.io.IOException
import kotlinx.io.RawSink
import kotlinx.io.buffered
import kotlinx.io.readByteArray
import kotlinx.io.readString
import kotlinx.io.writeString
import platform.posix.F_OK
import platform.posix.access
import kotlin.random.Random
import kotlin.test.Test

/** PKWARE APPNOTE 6.3.10, central directory file header. */
private const val CentralDirectoryHeaderSignature: Int = 0x02014b50

class ContainerCommandsTest {
  private val backupType = ContainerType(ContainerTypeName("it.neckar.backup.archive"), Version.of(2, 3, 0))

  @Test
  fun writesTheStdoutOfEveryCommand() {
    val buffer = Buffer()
    writeContainer(
      Invocation.Write(
        backupType,
        listOf(
          CommandEntry(VersionedEntryName.parse("mongodb/1.0.0"), CompressionLevel.Precompressed, "printf dump"),
          CommandEntry(VersionedEntryName.parse("volume/2.1.0"), CompressionLevel.Default, "head -c 3000000 /dev/zero"),
        ),
      ),
      buffer,
    )

    ContainerReader(buffer, AnyContent).use { reader ->
      assertThat(reader.containerType).isEqualTo(backupType)
      val entries = generateSequence { reader.nextEntry()?.let { it.versionedName.zipPath to it.content.readByteArray() } }.toList()
      assertThat(entries.map { it.first }).containsExactly("mongodb/1.0.0", "volume/2.1.0")
      assertThat(entries[0].second.decodeToString()).isEqualTo("dump")
      assertThat(entries[1].second.size).isEqualTo(3_000_000)
    }
  }

  /** 1 MB of zeros deflates to a few KiB with level 6 and stays 1 MB with level 0. */
  @Test
  fun passesTheCompressionLevelOfTheEntry() {
    fun containerSize(compressionLevel: CompressionLevel): Long {
      val buffer = Buffer()
      writeContainer(Invocation.Write(backupType, listOf(CommandEntry(VersionedEntryName.parse("volume/2.1.0"), compressionLevel, "head -c 1000000 /dev/zero"))), buffer)
      return buffer.size
    }

    assertThat(containerSize(CompressionLevel.Default)).isLessThan(100_000)
    assertThat(containerSize(CompressionLevel.Precompressed)).isGreaterThan(1_000_000)
  }

  @Test
  fun failsWhenACommandFails() {
    val write = Invocation.Write(
      backupType,
      listOf(
        CommandEntry(VersionedEntryName.parse("mongodb/1.0.0"), CompressionLevel.Default, "printf dump"),
        CommandEntry(VersionedEntryName.parse("volume/2.1.0"), CompressionLevel.Default, "printf partial; exit 3"),
      ),
    )

    val buffer = Buffer()
    assertFailure { writeContainer(write, buffer) }.hasMessage("Entry <volume/2.1.0> failed: Command exited with code 3")
    assertFailure { ContainerReader(buffer, AnyContent).use { reader -> generateSequence { reader.nextEntry() }.toList() } }.isInstanceOf<InvalidContainerException>()
  }

  @Test
  fun failsWhenTheFirstCommandOfAPipelineFails() {
    val write = Invocation.Write(backupType, listOf(CommandEntry(VersionedEntryName.parse("mongodb/1.0.0"), CompressionLevel.Precompressed, "{ printf dump; false; } | gzip")))

    assertFailure { writeContainer(write, Buffer()) }.hasMessage("Entry <mongodb/1.0.0> failed: Command exited with code 1")
  }

  @Test
  fun keepsSingleQuotesOfTheCommand() {
    val buffer = Buffer()
    writeContainer(Invocation.Write(backupType, listOf(CommandEntry(VersionedEntryName.parse("volume/2.1.0"), CompressionLevel.Default, "printf '%s' \"it's\""))), buffer)

    val out = Buffer()
    extractEntry(Invocation.Extract(EntryName("volume"), emptySet()), buffer, out)
    assertThat(out.readString()).isEqualTo("it's")
  }

  @Test
  fun runsNoCommandAfterAFailedOne() {
    val marker = "/tmp/nit-container-test-${Random.nextLong().toULong()}"
    val write = Invocation.Write(
      backupType,
      listOf(
        CommandEntry(VersionedEntryName.parse("mongodb/1.0.0"), CompressionLevel.Default, "false"),
        CommandEntry(VersionedEntryName.parse("volume/2.1.0"), CompressionLevel.Default, "touch $marker"),
      ),
    )

    assertFailure { writeContainer(write, Buffer()) }.hasMessage("Entry <mongodb/1.0.0> failed: Command exited with code 1")
    assertThat(access(marker, F_OK)).isEqualTo(-1)
  }

  @Test
  fun failsWhenASignalEndsACommand() {
    val write = Invocation.Write(backupType, listOf(CommandEntry(VersionedEntryName.parse("volume/2.1.0"), CompressionLevel.Default, "kill -9 \$\$")))

    assertFailure { writeContainer(write, Buffer()) }.hasMessage("Entry <volume/2.1.0> failed: Command was ended by signal 9")
  }

  /** pclose returns once the closed pipe ends `yes` with SIGPIPE; the copy failure wins over that exit status. */
  @Test
  fun reportsAFailedCopyOfAnEndlessCommand() {
    val fullDisk = object : RawSink {
      override fun write(source: Buffer, byteCount: Long): Unit = throw IOException("disk full")
      override fun flush() {
      }

      override fun close() {
      }
    }

    assertFailure { copyCommandOutput("yes", fullDisk.buffered()) }.hasMessage("disk full")
  }

  @Test
  fun listsTheHeaderAndEveryEntry() {
    val out = Buffer()
    listContainer(sampleContainer(), out)

    assertThat(out.readString().lines()).containsExactly(
      """{"formatVersion":"1.0.0","type":{"name":"it.neckar.backup.archive","version":"2.3.0"}}""",
      "mongodb/2.0.0\t8",
      "mongodb/1.0.0\t8",
      "volume/2.1.0\t3",
      "",
    )
  }

  /** The content of the truncated entry ends early while list counts it, so that entry gets no line. */
  @Test
  fun listsTheEntriesBeforeATruncatedOne() {
    val out = Buffer()
    assertFailure { listContainer(damagedContainer(), out) }.isInstanceOf<InvalidContainerException>()

    assertThat(out.readString().lines().drop(1)).containsExactly("mongodb/1.0.0\t4", "")
  }

  /** kmp-zip checks CRC-32 at the end of the content, so the entry with the wrong checksum gets no line. */
  @Test
  fun listsTheEntriesBeforeOneWithAWrongChecksum() {
    val bytes = sampleContainer().readByteArray()
    // The data descriptor of the last entry, volume/2.1.0, ends at the central directory: signature, CRC-32 and two 4-byte sizes.
    val descriptor = bytes.indexOfSignature(CentralDirectoryHeaderSignature) - 16
    bytes[descriptor + 4] = (bytes[descriptor + 4] + 1).toByte()

    val out = Buffer()
    assertFailure { listContainer(Buffer().apply { write(bytes) }, out) }.isInstanceOf<InvalidContainerException>().messageContains("CRC mismatch")
    assertThat(out.readString().lines().drop(1)).containsExactly("mongodb/2.0.0\t8", "mongodb/1.0.0\t8", "")
  }

  @Test
  fun extractsTheHighestVersion() {
    assertThat(extract(Invocation.Extract(EntryName("mongodb"), emptySet()))).isEqualTo("new dump")
  }

  @Test
  fun extractsTheHighestVersionOfAGivenMajor() {
    assertThat(extract(Invocation.Extract(EntryName("mongodb"), setOf(1)))).isEqualTo("old dump")
  }

  /** A truncated tail shows only when extract reads past the found entry; the next local header alone is intact. */
  @Test
  fun failsForADamagedContainerBehindTheEntry() {
    assertFailure { extractEntry(Invocation.Extract(EntryName("mongodb"), emptySet()), damagedContainer(), Buffer()) }.isInstanceOf<InvalidContainerException>()
  }

  @Test
  fun failsForAMissingEntry() {
    assertFailure { extract(Invocation.Extract(EntryName("mongodb"), setOf(3))) }
      .hasMessage("Container has no entry <mongodb> with major 3; entries: mongodb/2.0.0, mongodb/1.0.0, volume/2.1.0")
  }

  private fun extract(invocation: Invocation.Extract): String {
    val out = Buffer()
    extractEntry(invocation, sampleContainer(), out)
    return out.readString()
  }

  /** `mongodb/1.0.0`, then `volume/2.1.0` with 100 kB of random bytes whose last 1000 bytes and the central directory are cut off. */
  private fun damagedContainer(): Buffer {
    val buffer = Buffer()
    ContainerWriter(buffer, backupType).use { writer ->
      writer.writeEntry(VersionedEntryName.parse("mongodb/1.0.0")) { it.writeString("dump") }
      writer.writeEntry(VersionedEntryName.parse("volume/2.1.0"), CompressionLevel.Precompressed) { it.write(Random(42).nextBytes(100_000)) }
    }
    return Buffer().apply { write(buffer.readByteArray().let { it.copyOf(it.size - 1000) }) }
  }

  /** The offset of the first occurrence of [signature] as little-endian int. */
  private fun ByteArray.indexOfSignature(signature: Int): Int = (0..size - 4).first { offset -> (0 until 4).all { this[offset + it] == (signature shr (8 * it)).toByte() } }

  private fun sampleContainer(): Buffer {
    val buffer = Buffer()
    ContainerWriter(buffer, backupType).use { writer ->
      writer.writeEntry(VersionedEntryName.parse("mongodb/2.0.0")) { it.writeString("new dump") }
      writer.writeEntry(VersionedEntryName.parse("mongodb/1.0.0")) { it.writeString("old dump") }
      writer.writeEntry(VersionedEntryName.parse("volume/2.1.0")) { it.writeString("tar") }
    }
    return buffer
  }
}
