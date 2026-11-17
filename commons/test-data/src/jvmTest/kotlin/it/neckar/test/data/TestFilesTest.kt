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

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.doesNotContain
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isLessThan
import org.apache.pdfbox.Loader
import org.apache.pdfbox.rendering.PDFRenderer
import org.junit.jupiter.api.Test
import java.io.IOException
import javax.imageio.ImageIO

class TestFilesTest {
  @Test
  fun `a PDF has exactly the requested size and renders one page`() {
    listOf(TestFiles.MinimumPdfSize, 1_000, 10 * 1024 * 1024 + 1).forEach { size ->
      val pdf = TestFiles.pdf(size)

      assertThat(pdf.size).isEqualTo(size)
      Loader.loadPDF(pdf).use { document ->
        assertThat(document.numberOfPages).isEqualTo(1)
        assertThat(PDFRenderer(document).renderImage(0).width).isEqualTo(595)
      }
    }
  }

  @Test
  fun `a PDF below the minimum size is refused`() {
    assertFailure { TestFiles.pdf(TestFiles.MinimumPdfSize - 1) }.isInstanceOf<IllegalArgumentException>()
  }

  @Test
  fun `the same size gives the same bytes`() {
    assertThat(TestFiles.pdf(5_000)).isEqualTo(TestFiles.pdf(5_000))
    assertThat(TestFiles.randomBytes(5_000)).isEqualTo(TestFiles.randomBytes(5_000))
  }

  @Test
  fun `a truncated PDF starts like a PDF and fails to load`() {
    val truncated = TestFiles.truncatedPdf(100_000)

    assertThat(truncated.size).isEqualTo(100_000)
    assertThat(truncated.copyOf(8).decodeToString()).isEqualTo("%PDF-1.4")
    assertThat(truncated.decodeToString()).doesNotContain("%%EOF")
    assertFailure { Loader.loadPDF(truncated).use { PDFRenderer(it).renderImage(0) } }.isInstanceOf<IOException>()
  }

  @Test
  fun `a truncated PDF ending before the page content is refused`() {
    assertFailure { TestFiles.truncatedPdf(20) }.isInstanceOf<IllegalArgumentException>()
  }

  @Test
  fun `random bytes fail to load as PDF`() {
    val random = TestFiles.randomBytes(10_000)

    assertThat(random.size).isEqualTo(10_000)
    assertFailure { Loader.loadPDF(random) }.isInstanceOf<IOException>()
  }

  @Test
  fun `a PNG has the requested dimensions`() {
    val image = ImageIO.read(TestFiles.png(400, 300).inputStream())

    assertThat(image.width).isEqualTo(400)
    assertThat(image.height).isEqualTo(300)
  }

  @Test
  fun `a one-colour PNG of 40 megapixels stays below one megabyte`() {
    val png = TestFiles.image(8_000, 5_000, "png")
    val image = ImageIO.read(png.inputStream())

    assertThat(png.size).isLessThan(1_000_000)
    assertThat(image.width).isEqualTo(8_000)
    assertThat(image.height).isEqualTo(5_000)
  }

  @Test
  fun `a one-colour JPEG has the requested dimensions`() {
    val image = ImageIO.read(TestFiles.image(640, 480, "jpg").inputStream())

    assertThat(image.width).isEqualTo(640)
    assertThat(image.height).isEqualTo(480)
  }
}
