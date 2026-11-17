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

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.apache.pdfbox.Loader
import org.apache.pdfbox.pdmodel.PDDocument
import org.apache.pdfbox.rendering.PDFRenderer
import java.awt.RenderingHints
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import java.io.IOException
import javax.imageio.ImageIO

/**
 * Renders a page of a PDF as a PNG image.
 */
object PdfToImage {

  /** The default width of a rendered image in pixels; the height follows the aspect ratio of the page. */
  const val DefaultWidth: Int = 480

  /**
   * The PNG of the first page of [pdfBytes], [width] pixels wide, or `null` if PDFBox cannot open or render the file:
   * it is damaged, protected by a password or has no pages.
   */
  suspend fun renderFirstPageAsPng(pdfBytes: ByteArray, width: Int = DefaultWidth): ByteArray? = withContext(Dispatchers.Default) {
    try {
      Loader.loadPDF(pdfBytes).use { document ->
        if (document.numberOfPages == 0) {
          return@withContext null
        }
        toPng(renderFirstPage(document, width))
      }
    } catch (_: IOException) {
      null
    }
  }

  private fun renderFirstPage(document: PDDocument, width: Int): BufferedImage {
    val page = document.getPage(0)
    val box = page.cropBox
    val pageWidth = if (page.rotation % 180 == 0) box.width else box.height

    val rendered = PDFRenderer(document).renderImage(0, width / pageWidth)
    return if (rendered.width == width) rendered else rendered.scaledToWidth(width)
  }

  /** PDFBox rounds the rendered width up, so a page can come out one pixel off. */
  private fun BufferedImage.scaledToWidth(targetWidth: Int): BufferedImage {
    val scaledHeight = maxOf(1, Math.round(height.toDouble() * targetWidth / width).toInt())
    return BufferedImage(targetWidth, scaledHeight, BufferedImage.TYPE_INT_RGB).also { scaled ->
      val graphics = scaled.createGraphics()
      try {
        graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR)
        graphics.drawImage(this, 0, 0, targetWidth, scaledHeight, null)
      } finally {
        graphics.dispose()
      }
    }
  }

  private fun toPng(image: BufferedImage): ByteArray {
    return ByteArrayOutputStream().use { output ->
      check(ImageIO.write(image, "png", output)) { "No PNG writer is available" }
      output.toByteArray()
    }
  }
}
