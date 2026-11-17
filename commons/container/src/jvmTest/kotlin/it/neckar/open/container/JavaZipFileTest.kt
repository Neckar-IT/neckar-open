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
import kotlinx.io.writeString
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayInputStream
import java.io.File
import java.util.zip.ZipFile
import java.util.zip.ZipInputStream

/** `java.util.zip.ZipFile` reads the central directory, as `unzip` does with a finished file. */
class JavaZipFileTest {
  @TempDir
  lateinit var directory: File

  @Test
  fun readsAWrittenContainerAsFile() {
    val file = File(directory, "container.zip")
    file.writeBytes(container {
      writeEntry(versionedName("mongodb/1.0.0"), CompressionLevel.Precompressed) { it.writeString("dump") }
      writeEntry(versionedName("config/users/1.2.0")) { it.write(randomContent) }
    })

    ZipFile(file).use { zip ->
      assertThat(zip.entries().toList().map { it.name }).containsExactly("meta/version", "meta/type/1.0.0", "mongodb/1.0.0", "config/users/1.2.0")
      assertThat(zip.getInputStream(zip.getEntry("meta/version")).readBytes().decodeToString()).isEqualTo("1.0.0")
      assertThat(zip.getInputStream(zip.getEntry("mongodb/1.0.0")).readBytes().decodeToString()).isEqualTo("dump")
      assertThat(zip.getInputStream(zip.getEntry("config/users/1.2.0")).readBytes().contentEquals(randomContent)).isTrue()
    }
  }

  /** `java.util.zip.ZipInputStream` reads without central directory and finds the 8-byte sizes through the Zip64 extra field. */
  @Test
  fun readsAWrittenContainerAsStream() {
    val bytes = container {
      writeEntry(versionedName("config/users/1.2.0")) { it.write(randomContent) }
    }

    ZipInputStream(ByteArrayInputStream(bytes)).use { zip ->
      val names = generateSequence { zip.nextEntry }.map { it.name to zip.readBytes() }.toList()
      assertThat(names.map { it.first }).containsExactly("meta/version", "meta/type/1.0.0", "config/users/1.2.0")
      assertThat(names.last().second.contentEquals(randomContent)).isTrue()
    }
  }
}
