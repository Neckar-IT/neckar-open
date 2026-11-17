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
package it.neckar.open.html

import assertk.*
import assertk.assertions.*
import com.sun.org.apache.xerces.internal.dom.ElementImpl
import it.neckar.open.test.utils.isEqualIgnoringWhitespacesEmptyLines
import kotlinx.html.FlowContent
import kotlinx.html.TagConsumer
import kotlinx.html.body
import kotlinx.html.div
import kotlinx.html.dom.append
import kotlinx.html.dom.document
import kotlinx.html.head
import kotlinx.html.html
import kotlinx.html.lang
import kotlinx.html.meta
import kotlinx.html.span
import kotlinx.html.stream.createHTML
import org.junit.jupiter.api.Test
import org.w3c.dom.Document
import org.w3c.dom.Node


class KotlinXHtmlApiTest {
  @Test
  fun testCreateHtml() {
    val html = createHTML()

    assertThat(html).isNotNull()
    assertThat(html).isInstanceOf(kotlinx.html.consumers.DelayedConsumer::class)
    assertThat(html.toString()).contains("kotlinx.html.consumers.DelayedConsumer@")
  }

  @Test
  fun testCreateNewDocument() {
    //Document is a class from the JVM DOM API
    val document: Document = document {
    }

    assertThat(document).isNotNull()

    assertThat(document.documentURI).isEqualTo(null)
    assertThat(document.doctype).isEqualTo(null)
    assertThat(document.documentElement).isEqualTo(null)

    //Append is a kotlin.xhtml function
    document.append {
      assertThat(document.documentElement).isEqualTo(null) //not yet added

      html {
        lang = "en"

        head {
          meta(charset = "UTF-8")
        }

        body {
          span {
            +"Hello"
            assertThat(document.documentElement).isEqualTo(null) //not yet added
          }

          addContent()
          addContent2()

          val element = this@append.div {
            +"da element"
          }

          assertThat(element.javaClass.name).isEqualTo("com.sun.org.apache.xerces.internal.dom.ElementImpl")
          assertThat(element).isInstanceOf(Node::class)

          (element as Node).append {
            span {
              +"Hello inner"
            }
          }

        }
      }

      assertThat(document.documentElement).isNotNull() //added(!)
      assertThat(document.documentElement).isInstanceOf(org.w3c.dom.Element::class)
    }

    assertThat(document.render().trim()).isEqualIgnoringWhitespacesEmptyLines(
      """
        <!DOCTYPE html>
        <html lang="en">
          <head>
            <META http-equiv="Content-Type" content="text/html; charset=UTF-8">
            <meta charset="UTF-8">
          </head>
          <body>
            <span>Hello</span>
            <div>in addContent</div>
            <div>in addContent2</div>
            <div>
            da element<span>Hello inner</span>
            </div>
          </body>
        </html>
    """.trimIndent()
    )

    if (false) {
      println(document.render())
    }

    assertThat(document.documentElement.childNodes.length).isEqualTo(2) //head + body
  }
}

private fun FlowContent.addContent() {
  div {
    +"in addContent"
  }
}

private fun TagConsumer<*>.addContent2() {
  div {
    +"in addContent2"
  }
}
