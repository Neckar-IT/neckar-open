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

import java.nio.file.Path
import kotlin.io.path.createDirectories
import kotlin.io.path.writeBytes

private const val KiB: Int = 1024
private const val MiB: Int = 1024 * KiB

/**
 * The sample PDFs of the directory [DirectoryName]: valid one-page A4 PDFs, each exactly as large as its name says, the largest above 100 MiB
 */
object SamplePdfs {
  /**
   * The directory of this module that holds the files
   */
  const val DirectoryName: String = "sample-pdfs"

  val All: List<SamplePdf> = listOf(
    SamplePdf("sample-50-kib.pdf", 50 * KiB, SamplePdfSizeClass.Small),
    SamplePdf("sample-250-kib.pdf", 250 * KiB, SamplePdfSizeClass.Small),
    SamplePdf("sample-1-mib.pdf", 1 * MiB, SamplePdfSizeClass.Small),
    SamplePdf("sample-5-mib.pdf", 5 * MiB, SamplePdfSizeClass.Medium),
    SamplePdf("sample-20-mib.pdf", 20 * MiB, SamplePdfSizeClass.Medium),
    SamplePdf("sample-50-mib.pdf", 50 * MiB, SamplePdfSizeClass.VeryLarge),
    SamplePdf("sample-99-mib.pdf", 99 * MiB, SamplePdfSizeClass.VeryLarge),
    SamplePdf("sample-110-mib.pdf", 110 * MiB, SamplePdfSizeClass.OverLimit),
  )

  fun of(sizeClass: SamplePdfSizeClass): List<SamplePdf> {
    return All.filter { it.sizeClass == sizeClass }
  }

  /**
   * Writes every sample PDF into [directory] and overwrites files of the same name
   */
  fun writeAll(directory: Path) {
    directory.createDirectories()
    All.forEach { samplePdf ->
      directory.resolve(samplePdf.fileName).writeBytes(TestFiles.pdf(samplePdf.sizeInBytes))
    }
  }
}
