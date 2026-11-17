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
import assertk.assertions.containsExactly
import assertk.assertions.hasMessage
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isTrue
import assertk.assertions.messageContains
import it.neckar.open.container.zip.ZipStreamReader
import it.neckar.open.container.zip.ZipStreamWriter
import it.neckar.open.version.Version
import kotlinx.io.Buffer
import kotlinx.io.RawSink
import kotlinx.io.buffered
import kotlinx.io.readByteArray
import kotlinx.io.writeString
import kotlin.test.Test

class ContainerWriterTest {
  @Test
  fun closesTheSinkWithoutCentralDirectoryWhenAnEntryFails() {
    val target = Buffer()
    val sink = RecordingSink(target)

    assertFailure {
      ContainerWriter(sink.buffered(), backupType).use { writer ->
        writer.writeEntry(versionedName("volume/2.1.0")) { error("tar failed") }
      }
    }.isInstanceOf<IllegalStateException>().hasMessage("tar failed")

    assertThat(sink.closed).isTrue()
    val bytes = target.readByteArray()
    assertThat(bytes.containsSignature(ZipSignatures.CentralDirectoryHeaderSignature)).isFalse()
    assertThat(bytes.containsSignature(ZipSignatures.EndOfCentralDirectorySignature)).isFalse()
  }

  @Test
  fun rejectsANameInTheMetaDirectory() {
    assertFailure { container { writeEntry(versionedName("meta/info/1.0.0")) { } } }.isInstanceOf<IllegalArgumentException>().messageContains("<meta/>")
  }

  @Test
  fun rejectsANameThatReturns() {
    assertFailure {
      container {
        writeEntry(versionedName("mongodb/2.0.0")) { }
        writeEntry(versionedName("volume/2.1.0")) { }
        writeEntry(versionedName("mongodb/1.0.0")) { }
      }
    }.isInstanceOf<IllegalArgumentException>().messageContains("all versions of <mongodb>")
  }

  @Test
  fun rejectsTheSameVersionTwice() {
    assertFailure {
      container {
        writeEntry(versionedName("mongodb/1.0.0")) { }
        writeEntry(versionedName("mongodb/1.0.0")) { }
      }
    }.isInstanceOf<IllegalArgumentException>().hasMessage("Entry <mongodb/1.0.0> is given twice")
  }

  /** A blank suffix formats away, so both names write the ZIP path mongodb/1.0.0. */
  @Test
  fun rejectsTheSameVersionWithABlankSuffix() {
    val blankSuffix = VersionedEntryName(EntryName("mongodb"), Version.of(1, 0, 0, ""))

    assertFailure { ContainerWriter.verifyEntryNames(listOf(blankSuffix, versionedName("mongodb/1.0.0"))) }
      .isInstanceOf<IllegalArgumentException>().hasMessage("Entry <mongodb/1.0.0> is given twice")
  }

  @Test
  fun verifiesEntryNamesWithTheRulesOfWriteEntry() {
    ContainerWriter.verifyEntryNames(listOf(versionedName("mongodb/2.0.0"), versionedName("mongodb/1.0.0"), versionedName("volume/2.1.0")))

    assertFailure { ContainerWriter.verifyEntryNames(listOf(versionedName("mongodb/1.0.0"), versionedName("volume/2.1.0"), versionedName("mongodb/0.9.0"))) }
      .isInstanceOf<IllegalArgumentException>().hasMessage("Entry <mongodb/0.9.0> follows another name; all versions of <mongodb> follow each other")
    assertFailure { ContainerWriter.verifyEntryNames(listOf(versionedName("meta/info/1.0.0"))) }.isInstanceOf<IllegalArgumentException>().messageContains("<meta/>")
  }

  @Test
  fun rejectsAnEntryAfterAFailedOne() {
    assertFailure {
      container {
        runCatching { writeEntry(versionedName("volume/2.1.0")) { error("tar failed") } }
        writeEntry(versionedName("mongodb/1.0.0")) { }
      }
    }.isInstanceOf<IllegalStateException>().hasMessage("An earlier entry failed")
  }

  @Test
  fun refusesFurtherEntriesAfterTheSinkFailed() {
    val sink = FailingOnceSink(failingWrite = 3)
    val writer = ContainerWriter(sink.buffered(), backupType)

    assertFailure { writer.writeEntry(versionedName("config/users/1.2.0")) { it.write(randomContent) } }.isInstanceOf<IllegalStateException>().hasMessage("disk full")
    assertFailure { writer.writeEntry(versionedName("volume/2.1.0")) { } }.isInstanceOf<IllegalStateException>().hasMessage("An earlier entry failed")
    assertFailure { writer.close() }.isInstanceOf<IllegalStateException>().messageContains("no central directory")
  }

  @Test
  fun throwsOnCloseAfterAFailedEntry() {
    val writer = ContainerWriter(Buffer(), backupType)
    assertFailure { writer.writeEntry(versionedName("volume/2.1.0")) { error("tar failed") } }.isInstanceOf<IllegalStateException>()

    assertFailure { writer.close() }.isInstanceOf<IllegalStateException>().messageContains("no central directory")
  }

  @Test
  fun abortsAnEntryWhoseWriteFailedAlthoughTheContentSwallowedIt() {
    val sink = FailingOnceSink(failingWrite = 3)
    val writer = ContainerWriter(sink.buffered(), backupType)

    assertFailure {
      writer.writeEntry(versionedName("config/users/1.2.0")) { entrySink -> runCatching { entrySink.write(randomContent) } }
    }.isInstanceOf<IllegalStateException>()
    assertFailure { writer.close() }.isInstanceOf<IllegalStateException>().messageContains("no central directory")
  }

  @Test
  fun rejectsAnEntryAfterClose() {
    val writer = ContainerWriter(Buffer(), backupType)
    writer.close()

    assertFailure { writer.writeEntry(versionedName("volume/2.1.0")) { } }.isInstanceOf<IllegalStateException>().hasMessage("The container is closed")
  }

  @Test
  fun keepsTheZipWriterUsableWhenANameIsTooLong() {
    val buffer = Buffer()
    ZipStreamWriter.to(buffer).use { zip ->
      assertFailure { zip.startEntry("x".repeat(ZipStreamWriter.MaxEntryNameBytes + 1), 6) }.isInstanceOf<IllegalArgumentException>()
      zip.abortEntry()
      zip.startEntry("volume/2.1.0", 6).use { it.writeString("tar") }
      zip.finish()
    }

    val names = ZipStreamReader.from(buffer).use { zip -> generateSequence { zip.nextEntry() }.map { it.name }.toList() }
    assertThat(names).containsExactly("volume/2.1.0")
  }

  @Test
  fun closesTheSinkAfterTheCentralDirectory() {
    val target = Buffer()
    val sink = RecordingSink(target)

    ContainerWriter(sink.buffered(), backupType).close()

    assertThat(target.readByteArray().containsSignature(ZipSignatures.EndOfCentralDirectorySignature)).isTrue()
    assertThat(sink.closed).isTrue()
  }

  @Test
  fun rejectsAscendingVersions() {
    assertFailure {
      container {
        writeEntry(versionedName("mongodb/1.0.0")) { }
        writeEntry(versionedName("mongodb/2.0.0")) { }
      }
    }.isInstanceOf<IllegalArgumentException>().messageContains("highest first")
  }

  /** Throws once on the write with index [failingWrite], as a disk that runs full and recovers. */
  private class FailingOnceSink(private val failingWrite: Int) : RawSink {
    private var writes = 0

    override fun write(source: Buffer, byteCount: Long) {
      writes++
      if (writes == failingWrite) error("disk full")
      source.skip(byteCount)
    }

    override fun flush() {
    }

    override fun close() {
    }
  }

  /** Records [close]; a [Buffer] cannot tell whether it was closed. */
  private class RecordingSink(private val target: Buffer) : RawSink {
    var closed: Boolean = false
      private set

    override fun write(source: Buffer, byteCount: Long) {
      target.write(source, byteCount)
    }

    override fun flush() {
    }

    override fun close() {
      closed = true
    }
  }
}
