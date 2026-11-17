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

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import it.neckar.open.container.zip.ZipStreamReader
import it.neckar.open.version.Version
import kotlinx.io.readByteArray
import kotlinx.io.writeString
import kotlin.test.Test

class ContainerRoundTripTest {
  @Test
  fun readsTheTypeAndEveryEntryItSupports() {
    val bytes = container {
      writeEntry(versionedName("mongodb/1.0.0"), CompressionLevel.Precompressed) { it.writeString("dump") }
      writeEntry(versionedName("volume/2.1.0")) { }
      writeEntry(versionedName("config/users/1.2.0")) { it.write(randomContent) }
    }

    ContainerReader(bytes.toSource(), backupExpectation).use { reader ->
      assertThat(reader.containerType).isEqualTo(backupType)
      val entries = generateSequence { reader.nextEntry()?.let { it.versionedName.zipPath to it.content.readByteArray() } }.toList()
      assertThat(entries.map { it.first }).containsExactly("mongodb/1.0.0", "volume/2.1.0", "config/users/1.2.0")
      assertThat(entries[0].second.decodeToString()).isEqualTo("dump")
      assertThat(entries[1].second.size).isEqualTo(0)
      assertThat(entries[2].second.contentEquals(randomContent)).isTrue()
    }
  }

  @Test
  fun skipsAnEntryTheReaderReadsOnlyPartly() {
    val bytes = container {
      writeEntry(versionedName("config/users/1.2.0")) { it.write(randomContent) }
      writeEntry(versionedName("volume/2.1.0")) { it.writeString("tar") }
    }

    ContainerReader(bytes.toSource(), backupExpectation).use { reader ->
      assertThat(reader.nextEntry()!!.content.readByteArray(10).size).isEqualTo(10)
      assertThat(reader.nextEntry()!!.content.readByteArray().decodeToString()).isEqualTo("tar")
    }
  }

  @Test
  fun takesTheFirstVersionWithAKnownMajor() {
    val bytes = container(backupType.copy(version = Version.of(2, 4, 0))) {
      writeEntry(versionedName("mongodb/2.0.0")) { it.writeString("new dump") }
      writeEntry(versionedName("mongodb/1.0.0")) { it.writeString("old dump") }
      writeEntry(versionedName("volume/2.1.0")) { it.writeString("tar") }
    }

    assertThat(readAll(bytes).map { it.first to it.second.decodeToString() })
      .containsExactly("mongodb/1.0.0" to "old dump", "volume/2.1.0" to "tar")

    val newerExpectation = backupExpectation.copy(entries = backupExpectation.entries.filterNot { it.name == EntryName("mongodb") } + ExpectedEntry(EntryName("mongodb"), setOf(1, 2)))
    assertThat(readAll(bytes, newerExpectation).map { it.first to it.second.decodeToString() })
      .containsExactly("mongodb/2.0.0" to "new dump", "volume/2.1.0" to "tar")
  }

  @Test
  fun readsEveryDataEntryOfAnyType() {
    val bytes = rawContainer(
      "meta/version" to "1.1.0",
      "meta/type/1.0.0" to """{"name": "it.neckar.invoice.export", "version": "7.0.0"}""",
      "meta/info/1.0.0" to "new",
      "mongodb/2.0.0" to "new dump",
      "mongodb/1.0.0" to "old dump",
      "postgres/1.0.0" to "pg",
    )

    ContainerReader(bytes.toSource(), AnyContent).use { reader ->
      assertThat(reader.formatVersion).isEqualTo(Version.of(1, 1, 0))
      assertThat(reader.containerType).isEqualTo(ContainerType(ContainerTypeName("it.neckar.invoice.export"), Version.of(7, 0, 0)))
      val entries = generateSequence { reader.nextEntry()?.let { it.versionedName.zipPath to it.content.readByteArray().decodeToString() } }.toList()
      assertThat(entries).containsExactly("mongodb/2.0.0" to "new dump", "mongodb/1.0.0" to "old dump", "postgres/1.0.0" to "pg")
    }
  }

  @Test
  fun readsACompatiblyExtendedEntry() {
    val bytes = container {
      writeEntry(versionedName("mongodb/1.5.0")) { it.writeString("dump") }
    }

    assertThat(readAll(bytes).map { it.first }).containsExactly("mongodb/1.5.0")
  }

  @Test
  fun skipsAnUnknownName() {
    val bytes = container {
      writeEntry(versionedName("postgres/1.0.0")) { it.writeString("pg") }
      writeEntry(versionedName("volume/2.1.0")) { it.writeString("tar") }
    }

    assertThat(readAll(bytes).map { it.first }).containsExactly("volume/2.1.0")
  }

  @Test
  fun readsTheTypeEntryWithMajor1BehindANewerOne() {
    val bytes = rawContainer(
      "meta/version" to "1.0.0",
      "meta/type/2.0.0" to """{"layout": "new"}""",
      "meta/type/1.0.0" to BackupTypeJson,
      "volume/2.1.0" to "tar",
    )

    assertThat(readAll(bytes).map { it.first }).containsExactly("volume/2.1.0")
  }

  @Test
  fun skipsAMetaEntryOfANewerFormatMinor() {
    val bytes = rawContainer("meta/version" to "1.1.0", "meta/type/1.0.0" to BackupTypeJson, "meta/info/1.0.0" to "new", "volume/2.1.0" to "tar")

    assertThat(readAll(bytes).map { it.first }).containsExactly("volume/2.1.0")
  }

  @Test
  fun readsTheFirstTypeEntryWithMajor1() {
    val bytes = rawContainer(
      "meta/version" to "1.0.0",
      "meta/type/1.1.0" to BackupTypeJson,
      "meta/type/1.0.0" to """{"name": "it.neckar.invoice.export", "version": "1.0.0"}""",
    )

    ContainerReader(bytes.toSource(), backupExpectation).use { reader ->
      assertThat(reader.containerType).isEqualTo(backupType)
    }
  }

  @Test
  fun ignoresUnknownFieldsOfTheTypeEntry() {
    val bytes = rawContainer(
      "meta/version" to "1.0.0",
      "meta/type/1.0.0" to """{"name": "it.neckar.backup.archive", "version": "2.3.0", "description": "Backup elektromeister"}""",
    )

    ContainerReader(bytes.toSource(), backupExpectation).use { reader ->
      assertThat(reader.containerType).isEqualTo(backupType)
    }
  }

  @Test
  fun writesTheMetaEntriesFirst() {
    val bytes = container { }

    val names = ZipStreamReader.from(bytes.toSource()).use { zip -> generateSequence { zip.nextEntry() }.map { it.name }.toList() }
    assertThat(names).containsExactly("meta/version", "meta/type/1.0.0")
  }
}
