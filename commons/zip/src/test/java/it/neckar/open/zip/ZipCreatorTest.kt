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
package it.neckar.open.zip

import assertk.*
import assertk.assertions.*
import it.neckar.open.file.requireIsDirectory
import it.neckar.open.test.utils.containsOnlyFiles
import it.neckar.open.zip.ZipExtractor.InvertedCondition
import org.apache.commons.io.FileUtils
import org.junit.jupiter.api.Assertions.assertSame
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File
import java.nio.charset.StandardCharsets
import javax.annotation.Nonnull

/**
 *
 */
class ZipCreatorTest {
  @Test
  fun testIt(@TempDir directory: File) {
    val target = directory.resolve("target").apply { mkdirs() }.requireIsDirectory()
    val extractor = ZipExtractor()
    extractor.extract(target, FileUtils.openInputStream(createZip(directory)))
    assertThat(target.listFiles()).hasSize(2)
    assertThat(target).containsOnlyFiles("file1", "subDir/file2")
  }

  @Test
  fun testCondition(@TempDir directory: File) {
    val target = directory.resolve("target").apply { mkdirs() }
    val extractor = ZipExtractor(InvertedCondition { true })
    extractor.extract(target, FileUtils.openInputStream(createZip(directory)))
    assertThat(target.listFiles()).hasSize(0)
  }

  @Nonnull
  private fun createZip(directory: File): File {
    val file = directory.resolve("file.zip").apply { createNewFile() }
    val creator = ZipCreator(file)
    val baseDir1 = directory.resolve("base1").apply { mkdirs() }
    val file1 = File(baseDir1, "file1")
    FileUtils.writeStringToFile(file1, "file1content", StandardCharsets.UTF_8)
    val baseDir2 = directory.resolve("base2").apply { mkdirs() }
    val file2 = File(File(baseDir2, "subDir"), "file2")
    FileUtils.writeStringToFile(file2, "file2content", StandardCharsets.UTF_8)
    assertSame(file, creator.zipFile)
    return creator.zip(baseDir1, baseDir2)
  }
}
