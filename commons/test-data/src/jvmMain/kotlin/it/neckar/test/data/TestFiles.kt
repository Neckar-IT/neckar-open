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

import it.neckar.open.unit.other.B
import java.awt.image.BufferedImage
import java.io.ByteArrayOutputStream
import javax.imageio.ImageIO
import javax.imageio.ImageTypeSpecifier
import kotlin.random.Random

/**
 * Generates large and damaged files for limit and error-path tests.
 *
 * Every file is generated from its parameters alone, so Git carries the generator instead of the bytes.
 * Within one Kotlin version PDFs and random bytes are reproduced byte for byte; the PNG encoding follows the JDK.
 */
object TestFiles {
  /**
   * The size of the smallest PDF [pdf] produces: one empty page, no padding
   */
  val MinimumPdfSize: @B Int = pdfWithPadding(0).size

  /**
   * A valid PDF with one empty A4 page, exactly [size] bytes long.
   *
   * The page's content stream carries a comment that fills the file up to [size]. Every number
   * that depends on the size (stream length, xref offsets) has ten digits, so the size grows linearly with the comment.
   */
  fun pdf(size: @B Int): ByteArray {
    require(size >= MinimumPdfSize) { "A PDF needs at least $MinimumPdfSize bytes but $size were requested" }
    return pdfWithPadding(size - MinimumPdfSize).also {
      check(it.size == size) { "Generated ${it.size} bytes instead of $size" }
    }
  }

  /**
   * The first [size] bytes of a valid PDF: ends inside the page content, before catalog, page tree and xref table
   */
  fun truncatedPdf(size: @B Int): ByteArray {
    require(size > ContentStart) { "A truncated PDF needs more than $ContentStart bytes to end inside the page content but $size were requested" }
    return pdf(size + MinimumPdfSize).copyOf(size)
  }

  /**
   * [size] pseudo-random bytes, for a file whose name or content type claims a format its content lacks
   */
  fun randomBytes(size: @B Int): ByteArray {
    return Random(size).nextBytes(size)
  }

  /**
   * A PNG of [width] × [height] pixels with pseudo-random colors, which keeps the compression from shrinking it
   */
  fun png(width: Int, height: Int): ByteArray {
    val random = Random(width * 31 + height)
    val image = BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    for (y in 0 until height) {
      for (x in 0 until width) {
        image.setRGB(x, y, random.nextInt(0x1000000))
      }
    }
    return ByteArrayOutputStream().also { check(ImageIO.write(image, "png", it)) { "No PNG writer available" } }.toByteArray()
  }

  /**
   * A one-colour image of [width] × [height] pixels in [format] (`png`, `jpg`, `gif`), which compresses to a few kilobytes even at many megapixels.
   * The image is one bit deep where the writer of [format] accepts that, else RGB.
   */
  fun image(width: Int, height: Int, format: String): ByteArray {
    val binary = BufferedImage(width, height, BufferedImage.TYPE_BYTE_BINARY)
    val image = if (ImageIO.getImageWriters(ImageTypeSpecifier.createFromRenderedImage(binary), format).hasNext()) binary else BufferedImage(width, height, BufferedImage.TYPE_INT_RGB)
    return ByteArrayOutputStream().also { check(ImageIO.write(image, format, it)) { "No $format writer available" } }.toByteArray()
  }

  /**
   * The offset of the first padding byte: a PDF cut behind it ends inside the page content
   */
  private val ContentStart: @B Int = pdfWithPadding(0).decodeToString().indexOf(PaddingStart) + PaddingStart.length

  private fun pdfWithPadding(paddingLength: Int): ByteArray {
    val pdf = ByteArrayOutputStream(paddingLength + 1024)
    fun write(text: String) = pdf.writeBytes(text.toByteArray(Charsets.US_ASCII))

    val contentLength = paddingLength + 2
    val objects = listOf(
      "<< /Type /Catalog /Pages 2 0 R >>",
      "<< /Type /Pages /Kids [3 0 R] /Count 1 >>",
      "<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Contents 4 0 R >>",
    )

    write("%PDF-1.4\n")
    // The padded content stream comes first: a file cut inside it keeps no catalog and no page to recover.
    val contentOffset = pdf.size()
    write("4 0 obj\n<< /Length ${tenDigits(contentLength)} >>\n$PaddingStart")
    val random = Random(paddingLength)
    repeat(paddingLength) {
      // Printable ASCII without line breaks, so the whole padding stays one comment
      pdf.write(0x21 + random.nextInt(0x7E - 0x21 + 1))
    }
    write("\nendstream\nendobj\n")

    val offsets = objects.mapIndexed { index, body ->
      pdf.size().also { write("${index + 1} 0 obj\n$body\nendobj\n") }
    } + contentOffset

    val xrefOffset = pdf.size()
    write("xref\n0 ${offsets.size + 1}\n")
    write("0000000000 65535 f \n")
    offsets.forEach { write("${tenDigits(it)} 00000 n \n") }
    write("trailer\n<< /Size ${offsets.size + 1} /Root 1 0 R >>\n")
    write("startxref\n${tenDigits(xrefOffset)}\n%%EOF\n")
    return pdf.toByteArray()
  }

  private const val PaddingStart: String = "stream\n% "

  private fun tenDigits(value: Int): String = value.toString().padStart(10, '0')
}
