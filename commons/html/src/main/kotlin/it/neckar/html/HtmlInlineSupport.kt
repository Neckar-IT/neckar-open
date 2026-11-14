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

import it.neckar.html.csp.CspNonce
import java.util.Base64

/**
 * Inlines CSS and JavaScript references in HTML files to create self-contained documents
 * suitable for PDF rendering via Chrome Headless.
 *
 * Chrome Headless requires all resources to be embedded inline because external `<link>` and
 * `<script>` references may not resolve correctly when rendering from a URL.
 *
 * ## Usage
 *
 * ```kotlin
 * val resourceProvider = HtmlInlineSupport.ResourceProvider { path ->
 *   javaClass.getResourceAsStream("/assets/$path")?.readBytes()
 * }
 * //nonce: the CspNonce of the response, or null when the HTML is written to a file
 * val inlinedHtml = HtmlInlineSupport.inlineHtmlResources(htmlSource, nonce, resourceProvider)
 * val fullyInlined = HtmlInlineSupport.inlineImages(inlinedHtml, resourceProvider)
 * ```
 *
 * ## Transformations
 *
 * - CSS: `<link href="styles.css">` → `<style>/* CSS content */</style>`
 * - JS: `<script src="app.js"></script>` → `<script>/* JS content */</script>`
 *
 * ## Architecture Context
 *
 * Layer 2 of the three-layer PDF generation pattern:
 * 1. Client-side rendering — the browser turns data into a DOM
 * 2. **Service HTML inlining (this class)** — the document becomes self-contained
 * 3. PDF creation — Chrome Headless renders it
 *
 * The pattern and its living reference: `internal/patterns/docs/kotlin/pdf-generation.md`.
 */
object HtmlInlineSupport {
  /**
   * Regex pattern matching CSS `<link>` elements.
   *
   * Matches: `<link ... href="file.css" ...>` or `<link ... href="file.css" .../>`
   *
   * Captures the CSS file path in group 1.
   */
  val CssLinkRegex: Regex = Regex("""<link\s+[^>]*href=["']([^"']+\.css)["'][^>]*/?>""", RegexOption.IGNORE_CASE)

  /**
   * Regex pattern matching JavaScript `<script>` elements with `src` attribute.
   *
   * Matches: `<script ... src="file.js" ...></script>`
   *
   * Captures the JS file path in group 1.
   */
  val ScriptTagRegex: Regex = Regex("""<script\s+[^>]*src=["']([^"']+\.js)["'][^>]*></script>""", RegexOption.IGNORE_CASE)

  /**
   * Regex pattern matching `<img>` elements with image file extensions.
   *
   * Matches: `<img ... src="file.png" ...>` or `<img ... src="file.jpg" .../>`
   *
   * Captures:
   * - Group 1: Tag prefix including `src="`
   * - Group 2: Image file path
   * - Group 3: Tag suffix after the path
   */
  val ImgTagRegex: Regex = Regex(
    """(<img\s+[^>]*src=["'])([^"']+\.(?:png|jpg|jpeg|gif|svg|webp))(["'][^>]*/?>)""",
    RegexOption.IGNORE_CASE
  )

  /**
   * Inlines all CSS and JavaScript resources referenced in the given HTML source.
   *
   * Inlining turns a reference a Content Security Policy allows into an inline block it blocks —
   * hence the [nonce], which marks the created blocks as ours (#840).
   *
   * @param htmlSource the HTML string containing `<link>` and `<script>` tags
   * @param nonce the CSP nonce of the response this HTML is rendered for, or null when no policy
   *   governs the document: it is handed straight to Chrome as page content
   *   (`ChromeRenderPdfSupport.renderPdfInlineHtml`) or written to a file, as the pdf-layout example
   *   test does. A nonce no policy announces marks nothing; it is not a safe default
   * @param resourceProvider callback that loads the content of a referenced resource by its path;
   *   stays last so it can be passed as a trailing lambda, like [inlineImages] takes it
   * @return the HTML with all CSS/JS resources embedded inline
   * @throws IllegalStateException if a referenced resource is not found
   */
  fun inlineHtmlResources(htmlSource: String, nonce: CspNonce? = null, resourceProvider: ResourceProvider): String {
    val nonceAttribute = if (nonce != null) " nonce=\"${nonce.value}\"" else ""

    return htmlSource
      // inline CSS
      .replace(CssLinkRegex) {
        val css = resourceProvider.getResource(it.groupValues[1]).decodeToString()
        "<style$nonceAttribute>\n$css\n</style>"
      }
      // inline JS
      .replace(ScriptTagRegex) {
        val js = resourceProvider.getResource(it.groupValues[1]).decodeToString()
        "<script$nonceAttribute>\n$js\n</script>"
      }
  }

  /**
   * Inlines image files as Base64 data URIs.
   *
   * Replaces `<img src="path/to/image.png">` with `<img src="data:image/png;base64,...">`.
   *
   * @param htmlSource The HTML source containing img tags
   * @param resourceProvider Provider to load image data
   * @return HTML with images inlined as Base64 data URIs
   * @throws IllegalStateException if a referenced image is not found
   */
  fun inlineImages(htmlSource: String, resourceProvider: ResourceProvider): String {
    return htmlSource.replace(ImgTagRegex) { matchResult ->
      val prefix = matchResult.groupValues[1]
      val imagePath = matchResult.groupValues[2]
      val suffix = matchResult.groupValues[3]

      val imageBytes = resourceProvider.getResource(imagePath)
      val mimeType = getMimeType(imagePath)
      val base64Data = Base64.getEncoder().encodeToString(imageBytes)
      "$prefix${"data:$mimeType;base64,$base64Data"}$suffix"
    }
  }

  /**
   * Builds a data URI string for the given image bytes and file path.
   * The MIME type is determined from the file extension.
   */
  fun buildDataUri(path: String, bytes: ByteArray): String {
    val mimeType = getMimeType(path)
    val base64Data = Base64.getEncoder().encodeToString(bytes)
    return "data:$mimeType;base64,$base64Data"
  }

  /**
   * Determines the MIME type from the file extension.
   */
  fun getMimeType(path: String): String {
    val extension = path.substringAfterLast('.').lowercase()
    return when (extension) {
      "png" -> "image/png"
      "jpg", "jpeg" -> "image/jpeg"
      "gif" -> "image/gif"
      "svg" -> "image/svg+xml"
      "webp" -> "image/webp"
      else -> "application/octet-stream"
    }
  }

  /**
   * Provides resources to inline (CSS, JS, images).
   */
  fun interface ResourceProvider {
    /**
     * Finds a resource by the path the HTML references it under, e.g. `styles/main.css`.
     * @return the resource bytes, or null if not found
     */
    fun findResource(path: String): ByteArray?

    /**
     * Loads a resource, throwing an exception if not found.
     */
    fun getResource(path: String): ByteArray = findResource(path) ?: error("Resource not found: $path")
  }
}
