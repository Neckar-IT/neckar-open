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

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.doesNotContain
import it.neckar.html.csp.CspNonce
import org.junit.jupiter.api.Test

/**
 * Inlining turns a resource reference into exactly the shape a Content Security Policy blocks, so
 * the nonce has to land on the blocks inlining creates (#840).
 */
class HtmlInlineSupportNonceTest {
  private val resourceProvider = HtmlInlineSupport.ResourceProvider { path ->
    when (path) {
      "app.js" -> "console.log('hi');".encodeToByteArray()
      "styles.css" -> "body { color: red; }".encodeToByteArray()
      else -> null
    }
  }

  @Test
  fun `marks an inlined script with the nonce`() {
    val html = HtmlInlineSupport.inlineHtmlResources(
      htmlSource = """<script src="app.js"></script>""",
      resourceProvider = resourceProvider,
      nonce = CspNonce("r4nd0m"),
    )

    assertThat(html).contains("""<script nonce="r4nd0m">""")
    assertThat(html).contains("console.log('hi');")
  }

  @Test
  fun `marks an inlined stylesheet with the nonce`() {
    val html = HtmlInlineSupport.inlineHtmlResources(
      htmlSource = """<link href="styles.css" rel="stylesheet">""",
      resourceProvider = resourceProvider,
      nonce = CspNonce("r4nd0m"),
    )

    assertThat(html).contains("""<style nonce="r4nd0m">""")
    assertThat(html).contains("body { color: red; }")
  }

  /**
   * The pdf-layout example test inlines the same HTML for its local previews, where no response and
   * hence no nonce exists — those must stay plain rather than gain an attribute no policy announces.
   */
  @Test
  fun `leaves the blocks unmarked without a nonce`() {
    val html = HtmlInlineSupport.inlineHtmlResources(
      htmlSource = """<script src="app.js"></script><link href="styles.css" rel="stylesheet">""",
      resourceProvider = resourceProvider,
    )

    assertThat(html).contains("<script>")
    assertThat(html).contains("<style>")
    assertThat(html).doesNotContain("nonce")
  }
}
