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
package it.neckar.test.data

import assertk.*
import assertk.assertions.*
import org.apache.pdfbox.Loader
import org.junit.jupiter.api.Test
import java.nio.file.Path
import kotlin.io.path.listDirectoryEntries
import kotlin.io.path.readBytes

class SamplePdfsTest {
  private val directory: Path = Path.of(SamplePdfs.DirectoryName)

  @Test
  fun `the directory holds exactly the listed PDFs`() {
    assertThat(directory.listDirectoryEntries("*.pdf").map { it.fileName.toString() }.sorted())
      .isEqualTo(SamplePdfs.All.map { it.fileName }.sorted())
  }

  @Test
  fun `every file is the PDF TestFiles produces for its size and has one page`() {
    SamplePdfs.All.forEach { samplePdf ->
      val bytes = directory.resolve(samplePdf.fileName).readBytes()

      assertThat(bytes.size, samplePdf.fileName).isEqualTo(samplePdf.sizeInBytes)
      assertThat(bytes.contentEquals(TestFiles.pdf(samplePdf.sizeInBytes)), samplePdf.fileName).isTrue()
      Loader.loadPDF(bytes).use { document ->
        assertThat(document.numberOfPages, samplePdf.fileName).isEqualTo(1)
      }
    }
  }

  @Test
  fun `only the OverLimit files reach 100 MiB and every size class is present`() {
    val limit = 100 * 1024 * 1024
    assertThat(SamplePdfs.All.filter { it.sizeInBytes >= limit }.map { it.sizeClass }.distinct()).containsExactly(SamplePdfSizeClass.OverLimit)
    assertThat(SamplePdfs.of(SamplePdfSizeClass.OverLimit).map { it.sizeInBytes }).containsExactly(110 * 1024 * 1024)
    SamplePdfSizeClass.entries.forEach { assertThat(SamplePdfs.of(it)).isNotEmpty() }
  }
}
