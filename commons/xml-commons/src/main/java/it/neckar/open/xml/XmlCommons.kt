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

import org.w3c.dom.Document
import java.io.ByteArrayInputStream
import java.io.InputStream
import java.io.OutputStream
import java.io.StringReader
import java.io.StringWriter
import java.io.Writer
import javax.xml.XMLConstants
import javax.xml.parsers.DocumentBuilder
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.parsers.ParserConfigurationException
import javax.xml.transform.OutputKeys
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import javax.xml.transform.stream.StreamSource

/**
 * Common xml methods
 *
 */
object XmlCommons {
  /**
   *
   * format
   *
   * @param xml a String object.
   * @return a String object.
   */
  fun format(xml: String): String {
    if (xml.isBlank()) {
      return ""
    }

    val stringWriter = StringWriter()
    createTransformer().transform(StreamSource(StringReader(xml)), StreamResult(stringWriter))
    return stringWriter.toString()
  }

  val documentBuilder: DocumentBuilder
    /**
     * Returns an XXE-hardened [DocumentBuilder]: external DTDs, external general/parameter
     * entities, network-loaded DTDs, XInclude and entity expansion are all disabled, so
     * parsing untrusted XML cannot exfiltrate local files, trigger SSRF, or expand into
     * a billion-laughs DoS.
     */
    get() = try {
      val factory = DocumentBuilderFactory.newInstance().apply {
        isNamespaceAware = true
        // OWASP-recommended XXE hardening for DocumentBuilderFactory.
        setFeature("http://apache.org/xml/features/disallow-doctype-decl", true)
        setFeature("http://xml.org/sax/features/external-general-entities", false)
        setFeature("http://xml.org/sax/features/external-parameter-entities", false)
        setFeature("http://apache.org/xml/features/nonvalidating/load-external-dtd", false)
        isXIncludeAware = false
        isExpandEntityReferences = false
      }
      factory.newDocumentBuilder()
    } catch (e: ParserConfigurationException) {
      throw IllegalStateException("The XML parser does not support the required XXE hardening features", e)
    }

  /**
   *
   * out
   *
   * @param document a Document object.
   * @param out      a OutputStream object.
   */
  fun out(document: Document, out: OutputStream) {
    createTransformer().transform(DOMSource(document), StreamResult(out))
  }

  /**
   *
   * out
   *
   * @param document a Document object.
   * @param out      a Writer object.
   */
  fun out(document: Document, out: Writer) {
    createTransformer().transform(DOMSource(document), StreamResult(out))
  }

  /**
   * Returns a [Transformer] that is hardened the same way [documentBuilder] is: secure processing is
   * on, external DTDs and external stylesheets are denied.
   *
   * An implementation that does not support one of these three options fails here instead of being
   * handed out: a transformer that silently kept its external access open would resolve external
   * entities while every caller believes it cannot.
   */
  fun createTransformer(): Transformer {
    val transformerFactory = TransformerFactory.newInstance()
    // FEATURE_SECURE_PROCESSING constrains entity expansion and disables loading of
    // external DTD/stylesheets via the default XMLConstants policy.
    try {
      transformerFactory.setFeature(XMLConstants.FEATURE_SECURE_PROCESSING, true)
    } catch (e: Exception) {
      throw IllegalStateException("The transformer factory <${transformerFactory.javaClass.name}> does not support the feature <${XMLConstants.FEATURE_SECURE_PROCESSING}>", e)
    }
    transformerFactory.denyExternalAccess(XMLConstants.ACCESS_EXTERNAL_DTD)
    transformerFactory.denyExternalAccess(XMLConstants.ACCESS_EXTERNAL_STYLESHEET)

    try {
      transformerFactory.setAttribute("indent-number", 2) //Used in some XML implementations
    } catch (_: IllegalArgumentException) {
      //Ignored on purpose: OutputKeys.INDENT below is the portable switch, this attribute only picks the indentation width
    }

    val transformer = transformerFactory.newTransformer()
    transformer.setOutputProperty(OutputKeys.INDENT, "yes")
    try {
      transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2") //Used for XALAN(?)
    } catch (_: IllegalArgumentException) {
      //Ignored on purpose: the output stays indented, just not with two spaces
    }
    return transformer
  }

  /**
   * Denies external access for the given attribute ([XMLConstants.ACCESS_EXTERNAL_DTD],
   * [XMLConstants.ACCESS_EXTERNAL_STYLESHEET]) by allowing no protocol at all.
   *
   * @throws IllegalStateException if this factory does not know the attribute - naming the attribute
   * and the factory implementation, since that is what has to be looked at on the classpath.
   */
  private fun TransformerFactory.denyExternalAccess(attributeName: String) {
    try {
      setAttribute(attributeName, "")
    } catch (e: IllegalArgumentException) {
      throw IllegalStateException("The transformer factory <${this.javaClass.name}> does not support the attribute <$attributeName>, external access cannot be denied", e)
    }
  }

  fun parse(bytes: ByteArray): Document {
    return parse(ByteArrayInputStream(bytes))
  }

  fun parse(inputStream: InputStream): Document {
    return documentBuilder.parse(inputStream)
  }

  fun toString(document: Document): String {
    val stringWriter = StringWriter()
    out(document, stringWriter)
    return stringWriter.toString()
  }
}
