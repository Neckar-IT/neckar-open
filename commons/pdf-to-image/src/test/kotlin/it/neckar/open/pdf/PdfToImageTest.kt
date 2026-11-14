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
package it.neckar.open.pdf

import assertk.*
import assertk.assertions.*
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.pdmodel.PDPage
import org.apache.pdfbox.pdmodel.common.PDRectangle
import org.junit.jupiter.api.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO

class PdfToImageTest {
  @Test
  suspend fun testFirstPageInDefaultWidth() {
    val png = checkNotNull(PdfToImage.renderFirstPageAsPng(pdfOf(PDRectangle(PDRectangle.A4.height, PDRectangle.A4.width), PDRectangle.A4)))

    val image = checkNotNull(ImageIO.read(ByteArrayInputStream(png)))
    assertThat(image.width).isEqualTo(PdfToImage.DefaultWidth)
    // The first page is A4 landscape: 297 x 210 mm
    assertThat(image.height).isEqualTo(Math.round(PdfToImage.DefaultWidth * PDRectangle.A4.width / PDRectangle.A4.height))
  }

  @Test
  suspend fun testGivenWidth() {
    val png = checkNotNull(PdfToImage.renderFirstPageAsPng(pdfOf(PDRectangle.A4), width = 200))

    assertThat(checkNotNull(ImageIO.read(ByteArrayInputStream(png))).width).isEqualTo(200)
  }

  @Test
  suspend fun testUnreadable() {
    assertThat(PdfToImage.renderFirstPageAsPng("%PDF-1.4\nnot a document".toByteArray())).isNull()
    assertThat(PdfToImage.renderFirstPageAsPng(pdfOf())).isNull()
  }

  private fun pdfOf(vararg pageSizes: PDRectangle): ByteArray {
    return PDDocument().use { document ->
      pageSizes.forEach { document.addPage(PDPage(it)) }
      ByteArrayOutputStream().also { document.save(it) }.toByteArray()
    }
  }
}
