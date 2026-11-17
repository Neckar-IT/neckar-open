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
package it.neckar.open.xml

import assertk.*
import assertk.assertions.*

import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.io.StringWriter
import java.nio.charset.StandardCharsets
import javax.xml.XMLConstants
import javax.xml.stream.XMLOutputFactory
import javax.xml.transform.dom.DOMResult
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult

/**
 *
 */
class XmlCommonsTest {
  private lateinit var lineSeparator: String

  @BeforeEach
  @Throws(Exception::class)
  fun setUp() {
    lineSeparator = System.getProperty("line.separator")
  }

  @Test
  fun testFormat() {
    Assertions.assertEquals("", XmlCommons.format(""))
    assertThat(XmlCommons.format("<xml><a/><b>asdf</b></xml>")).contains(lineSeparator)
  }

  @Test
  @Throws(Exception::class)
  fun testOutDom() {
    val doc = XmlCommons.documentBuilder.newDocument()
    val root = doc.createElement("root")
    doc.appendChild(root)
    root.appendChild(doc.createElement("child"))
    val child = doc.createElement("child")
    root.appendChild(child)
    child.appendChild(doc.createElement("another"))
    val out = ByteArrayOutputStream()
    val transformer = XmlCommons.createTransformer()
    transformer.transform(DOMSource(doc), StreamResult(out))
    assertThat(out.toString()).isEqualTo(
      "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>" + lineSeparator +
        "<root>" + lineSeparator +
        "  <child/>" + lineSeparator +
        "  <child>" + lineSeparator +
        "    <another/>" + lineSeparator +
        "  </child>" + lineSeparator +
        "</root>" + lineSeparator
    )
  }

  @Test
  @Throws(Exception::class)
  fun testOutStream() {
    val factory = XMLOutputFactory.newFactory()
    val doc = XmlCommons.documentBuilder.newDocument()
    val writer = factory.createXMLStreamWriter(DOMResult(doc))
    writer.writeStartElement("daRoot")
    writer.writeStartElement("daChild")
    writer.writeEndElement()
    writer.writeEndElement()
    writer.close()
    assertThat(doc.xmlEncoding).isEqualTo(null)
    assertThat(doc.xmlStandalone).isEqualTo(false)
    assertThat(doc.xmlVersion).isEqualTo("1.0")
    val out = ByteArrayOutputStream()
    XmlCommons.out(doc, out)
    Assertions.assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>" + lineSeparator +
      "<daRoot>" + lineSeparator +
      "  <daChild/>" + lineSeparator +
      "</daRoot>", out.toString().trim { it <= ' ' })
  }

  @Test
  @Throws(Exception::class)
  fun testOutWriter() {
    val factory = XMLOutputFactory.newFactory()
    val doc = XmlCommons.documentBuilder.newDocument()
    val writer = factory.createXMLStreamWriter(DOMResult(doc))
    writer.writeStartElement("daRoot")
    writer.writeStartElement("daChild")
    writer.writeEndElement()
    writer.writeEndElement()
    val out = StringWriter()
    XmlCommons.out(doc, out)
    Assertions.assertEquals("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"no\"?>" + lineSeparator +
      "<daRoot>" + lineSeparator +
      "  <daChild/>" + lineSeparator +
      "</daRoot>", out.toString().trim { it <= ' ' })
  }

  @Test
  @Throws(Exception::class)
  fun testParse() {
    val document = XmlCommons.parse("<xml/>".toByteArray(StandardCharsets.UTF_8))
    Assertions.assertEquals("xml", document.documentElement.nodeName)
  }

  @Test
  fun `unsupported secure processing is reported`() {
    assertUnsupportedOptionIsReported(XMLConstants.FEATURE_SECURE_PROCESSING)
  }

  @Test
  fun `unsupported external DTD restriction is reported`() {
    assertUnsupportedOptionIsReported(XMLConstants.ACCESS_EXTERNAL_DTD)
  }

  @Test
  fun `unsupported external stylesheet restriction is reported`() {
    assertUnsupportedOptionIsReported(XMLConstants.ACCESS_EXTERNAL_STYLESHEET)
  }

  /**
   * Verifies that a transformer factory which does not support [optionName] makes
   * [XmlCommons.createTransformer] fail, naming the option and the factory - instead of returning a
   * transformer the caller believes to be hardened.
   */
  private fun assertUnsupportedOptionIsReported(optionName: String) {
    RejectingTransformerFactory.withRejectedOption(optionName) {
      assertFailure {
        XmlCommons.createTransformer()
      }
        .isInstanceOf<IllegalStateException>()
        .all {
          messageContains(optionName)
          messageContains(RejectingTransformerFactory::class.java.name)
        }
    }
  }
}
