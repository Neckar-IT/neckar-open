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
package it.neckar.html

import assertk.*
import assertk.assertions.*
import org.junit.jupiter.api.Test
import java.util.Base64

/**
 * The regex-level behaviour of the inlining: which tags are rewritten, which are left alone, and
 * that a missing resource is a failure rather than a hole in the document.
 */
class HtmlInlineSupportTest {
  @Test
  fun testSimpleCss() {
    assertThat(
      HtmlInlineSupport.inlineHtmlResources(
        """<html><head><link rel="stylesheet" href="styles/main.css"/></head><body></body></html>"""
      ) { path ->
        assertThat(path).isEqualTo("styles/main.css")
        "inline-css-content".toByteArray()
      }).isEqualTo(
      //language=html
      """
      <html><head><style>
      inline-css-content
      </style></head><body></body></html>
      """.trimIndent()
    )
  }

  @Test
  fun testSimpleJs() {
    assertThat(
      HtmlInlineSupport.inlineHtmlResources(
        """<html><head>
          |<body><
          |<script src="pdf-layout.js"></script>
          |/body>
          |</html>""".trimMargin()
      ) { path ->
        assertThat(path).isEqualTo("pdf-layout.js")
        "inline-js-content".toByteArray()
      }).isEqualTo(
      //language=html
      """
        <html><head>
        <body><
        <script>
        inline-js-content
        </script>
        /body>
        </html>
      """.trimIndent()
    )
  }

  @Test
  fun testCssNotFoundThrows() {
    org.junit.jupiter.api.assertThrows<IllegalStateException> {
      HtmlInlineSupport.inlineHtmlResources(
        """<html><head><link rel="stylesheet" href="missing.css"/></head></html>"""
      ) { null }
    }.also {
      assertThat(it.message).isNotNull().contains("missing.css")
    }
  }

  @Test
  fun testInlineImagePng() {
    val imageBytes = byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47) // PNG magic bytes
    val expectedBase64 = Base64.getEncoder().encodeToString(imageBytes)

    val result = HtmlInlineSupport.inlineImages(
      """<html><body><img src="img/logo/company.png" alt="Logo"/></body></html>"""
    ) { path ->
      assertThat(path).isEqualTo("img/logo/company.png")
      imageBytes
    }

    assertThat(result).isEqualTo(
      """<html><body><img src="data:image/png;base64,$expectedBase64" alt="Logo"/></body></html>"""
    )
  }

  @Test
  fun testInlineImageJpeg() {
    val imageBytes = byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte()) // JPEG magic bytes
    val expectedBase64 = Base64.getEncoder().encodeToString(imageBytes)

    val result = HtmlInlineSupport.inlineImages(
      """<img src="photo.jpg">"""
    ) { imageBytes }

    assertThat(result).isEqualTo(
      """<img src="data:image/jpeg;base64,$expectedBase64">"""
    )
  }

  @Test
  fun testInlineImageSvg() {
    val svgBytes = "<svg></svg>".toByteArray()
    val expectedBase64 = Base64.getEncoder().encodeToString(svgBytes)

    val result = HtmlInlineSupport.inlineImages(
      """<img src="icon.svg" class="icon"/>"""
    ) { svgBytes }

    assertThat(result).isEqualTo(
      """<img src="data:image/svg+xml;base64,$expectedBase64" class="icon"/>"""
    )
  }

  @Test
  fun testInlineImageWithStyleAttributes() {
    val imageBytes = byteArrayOf(1, 2, 3)
    val expectedBase64 = Base64.getEncoder().encodeToString(imageBytes)

    val result = HtmlInlineSupport.inlineImages(
      """<img src="logo.png" alt="Logo" style="max-height: 16mm; max-width:50mm;"/>"""
    ) { imageBytes }

    assertThat(result).isEqualTo(
      """<img src="data:image/png;base64,$expectedBase64" alt="Logo" style="max-height: 16mm; max-width:50mm;"/>"""
    )
  }

  @Test
  fun testInlineMultipleImages() {
    val imageBytes1 = byteArrayOf(1, 2, 3)
    val imageBytes2 = byteArrayOf(4, 5, 6)
    val base64_1 = Base64.getEncoder().encodeToString(imageBytes1)
    val base64_2 = Base64.getEncoder().encodeToString(imageBytes2)

    val result = HtmlInlineSupport.inlineImages(
      """<img src="logo1.png"><img src="logo2.png">"""
    ) { path ->
      when (path) {
        "logo1.png" -> imageBytes1
        "logo2.png" -> imageBytes2
        else -> error("Unexpected resource: $path")
      }
    }

    assertThat(result).isEqualTo(
      """<img src="data:image/png;base64,$base64_1"><img src="data:image/png;base64,$base64_2">"""
    )
  }

  @Test
  fun testInlineImageNotFoundThrows() {
    org.junit.jupiter.api.assertThrows<IllegalStateException> {
      HtmlInlineSupport.inlineImages(
        """<img src="missing.png" alt="Missing"/>"""
      ) { null }
    }.also {
      assertThat(it.message).isNotNull().contains("missing.png")
    }
  }

  @Test
  fun testImgTagRegexMatchesVariousFormats() {
    val testCases = listOf(
      """<img src="test.png">""",
      """<img src='test.png'>""",
      """<img src="test.jpg"/>""",
      """<img src="test.jpeg" alt="test"/>""",
      """<img src="path/to/test.gif">""",
      """<img src="test.webp" class="cls">""",
      """<IMG SRC="test.PNG">""", // uppercase
      """<img   src="test.png"  >""", // extra spaces
    )

    testCases.forEach { html ->
      val containsImgTag = HtmlInlineSupport.ImgTagRegex.containsMatchIn(html)
      assertThat(containsImgTag).isTrue()
    }
  }

  @Test
  fun testImgTagRegexDoesNotMatchNonImages() {
    val testCases = listOf(
      """<img src="test.pdf">""",
      """<img src="test.txt">""",
      """<img src="test">""",
      """<script src="test.png">""",
      """<link href="test.png">""",
    )

    testCases.forEach { html ->
      val containsImgTag = HtmlInlineSupport.ImgTagRegex.containsMatchIn(html)
      assertThat(containsImgTag).isFalse()
    }
  }
}
