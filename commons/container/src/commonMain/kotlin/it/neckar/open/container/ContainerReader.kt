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

import it.neckar.open.container.zip.ZipEntry
import it.neckar.open.container.zip.ZipStreamReader
import it.neckar.open.version.Version
import kotlinx.io.Buffer
import kotlinx.io.Source
import kotlinx.io.readByteArray

/**
 * Reads a container from [source] in stream order.
 *
 * The constructor reads `meta/version` and `meta/type/<version>` and rejects a container [expected] does not cover.
 * With [ExpectedContent], [nextEntry] returns per entry name the first version whose major [expected] lists and skips every
 * other entry; with [AnyContent] it returns every data entry.
 * [close] and a rejection in the constructor close [source].
 */
class ContainerReader(
  source: Source,
  private val expected: ContainerExpectation,
) : AutoCloseable {
  private val zip = ZipStreamReader.from(source)

  /** The entry read past the `meta/type` entries. */
  private var pending: ZipEntry? = null
  private val takenNames = mutableSetOf<EntryName>()

  private val order = EntryOrder()

  /** Set when [nextEntry] rejected an entry; the entries behind it are not trustworthy. */
  private var failed = false

  /** The content of `meta/version`. */
  val formatVersion: Version

  /** The content of the `meta/type` entry this reader understands. */
  val containerType: ContainerType

  init {
    try {
      formatVersion = readFormatVersion()
      containerType = readContainerType()
      verifyContainerType()
    } catch (e: Throwable) {
      runCatching { zip.close() }.exceptionOrNull()?.let { e.addSuppressed(it) }
      throw e
    }
  }

  /** The next data entry this reader understands, or null at the end of the container. */
  fun nextEntry(): DataEntry? {
    check(failed.not()) { "An earlier entry of the container was rejected" }
    while (true) {
      val entry = pending ?: zip.nextEntry() ?: return null
      pending = null
      val versionedName = try {
        parseName(entry.name).also { acceptOrder(it) }
      } catch (e: InvalidContainerException) {
        failed = true
        throw e
      }
      if (isWanted(versionedName)) {
        takenNames += versionedName.name
        return DataEntry(versionedName, entry.content)
      }
    }
  }

  override fun close() {
    pending = null
    zip.close()
  }

  /** True for a data entry [expected] covers; a meta entry behind `meta/type`, such as one a later format version adds, never is. */
  private fun isWanted(versionedName: VersionedEntryName): Boolean {
    return when (expected) {
      AnyContent -> versionedName.name.isInMetaDirectory.not()
      is ExpectedContent -> versionedName.name !in takenNames && expected.understands(versionedName)
    }
  }

  /** Checks the order of the data entries; meta entries behind `meta/type` stay outside of it. */
  private fun acceptOrder(versionedName: VersionedEntryName) {
    if (versionedName.name.isInMetaDirectory) return
    order.accept(versionedName) { throw InvalidContainerException(it) }
  }

  private fun readFormatVersion(): Version {
    val entry = zip.nextEntry() ?: throw InvalidContainerException("Container has no entry, expected <${ContainerFormat.FormatVersionPath}>")
    if (entry.name != ContainerFormat.FormatVersionPath) {
      throw InvalidContainerException("First entry is <${entry.name.escapeControlCharacters()}>, expected <${ContainerFormat.FormatVersionPath}>")
    }
    val content = readMetaContent(entry)
    val version = try {
      parseContainerVersion(content)
    } catch (e: IllegalArgumentException) {
      throw InvalidContainerException("<${ContainerFormat.FormatVersionPath}> contains no version <major>.<minor>.<patch>: ${e.message?.take(MaxQuotedContentLength)}", e)
    }
    if (version.major != ContainerFormat.formatVersion.major) {
      throw InvalidContainerException("Container format version <${version.format()}> is unsupported, supported major <${ContainerFormat.formatVersion.major}>")
    }
    return version
  }

  private fun readContainerType(): ContainerType {
    val typeEntryNames = mutableListOf<String>()
    val typeEntryOrder = EntryOrder()
    var understood: ContainerType? = null
    while (true) {
      val entry = zip.nextEntry() ?: break
      val versionedName = parseName(entry.name)
      if (versionedName.name != ContainerFormat.containerTypeEntry.name) {
        pending = entry
        break
      }
      typeEntryOrder.accept(versionedName) { throw InvalidContainerException(it) }
      typeEntryNames += versionedName.zipPath
      if (understood == null && versionedName.version.major == ContainerFormat.containerTypeEntry.version.major) {
        understood = decodeContainerType(entry)
      }
    }
    return understood ?: throw InvalidContainerException(
      if (typeEntryNames.isEmpty()) {
        "Second entry is <${pending?.name?.escapeControlCharacters() ?: "end of container"}>, expected <${ContainerFormat.containerTypeEntry.name}/<version>>"
      } else {
        "No <${ContainerFormat.containerTypeEntry.name}> entry with major <${ContainerFormat.containerTypeEntry.version.major}>, found <${typeEntryNames.joinToString()}>"
      }
    )
  }

  private fun decodeContainerType(entry: ZipEntry): ContainerType {
    val content = readMetaContent(entry)
    return try {
      ContainerFormat.json.decodeFromString(ContainerType.serializer(), content)
    } catch (e: IllegalArgumentException) {
      throw InvalidContainerException("<${entry.name}> contains no valid type: ${e.message?.take(MaxQuotedContentLength)}", e)
    }
  }

  private fun verifyContainerType() {
    if (expected !is ExpectedContent) return
    if (containerType.name != expected.containerTypeName) {
      throw InvalidContainerException("Container type is <${containerType.name}>, expected <${expected.containerTypeName}>")
    }
    if (containerType.version.major !in expected.containerTypeMajors) {
      throw InvalidContainerException("Version <${containerType.version.format()}> of <${containerType.name}> is unsupported, supported majors <${expected.containerTypeMajors.sorted().joinToString()}>")
    }
  }

  private fun readMetaContent(entry: ZipEntry): String {
    val buffer = Buffer()
    while (buffer.size <= ContainerFormat.MaxMetaEntryBytes) {
      if (entry.content.readAtMostTo(buffer, ContainerFormat.MaxMetaEntryBytes + 1 - buffer.size) == -1L) {
        return try {
          buffer.readByteArray().decodeToString(throwOnInvalidSequence = true)
        } catch (e: CharacterCodingException) {
          throw InvalidContainerException("<${entry.name}> is no valid UTF-8", e)
        }
      }
    }
    throw InvalidContainerException("<${entry.name}> exceeds ${ContainerFormat.MaxMetaEntryBytes} bytes")
  }

  private fun parseName(zipPath: String): VersionedEntryName {
    return try {
      VersionedEntryName.parse(zipPath)
    } catch (e: IllegalArgumentException) {
      throw InvalidContainerException("Entry <${zipPath.escapeControlCharacters()}> is no <name>/<version>: ${e.message}", e)
    }
  }
}

/** Quoting more of a rejected meta entry adds nothing to an error message. */
private const val MaxQuotedContentLength: Int = 200
