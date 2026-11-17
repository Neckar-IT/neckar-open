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

import javax.xml.transform.ErrorListener
import javax.xml.transform.Source
import javax.xml.transform.Templates
import javax.xml.transform.Transformer
import javax.xml.transform.TransformerConfigurationException
import javax.xml.transform.TransformerFactory
import javax.xml.transform.URIResolver

/**
 * A [TransformerFactory] that rejects exactly one option - [rejectedOptionName] - and delegates
 * everything else to the default implementation. Stands in for an XML implementation that does not
 * support the hardening [XmlCommons.createTransformer] asks for.
 *
 * Instantiated by [TransformerFactory.newInstance] through the system property set in [withRejectedOption].
 */
class RejectingTransformerFactory : TransformerFactory() {
  private val delegate: TransformerFactory = TransformerFactory.newDefaultInstance()

  override fun setFeature(name: String, value: Boolean) {
    if (name == rejectedOptionName) {
      throw TransformerConfigurationException("Feature <$name> is not recognized")
    }
    delegate.setFeature(name, value)
  }

  override fun setAttribute(name: String, value: Any?) {
    require(name != rejectedOptionName) { "Attribute <$name> is not recognized" } //IllegalArgumentException - what an unknown attribute name raises
    delegate.setAttribute(name, value)
  }

  override fun newTransformer(): Transformer = delegate.newTransformer()

  override fun newTransformer(source: Source?): Transformer = delegate.newTransformer(source)

  override fun newTemplates(source: Source?): Templates = delegate.newTemplates(source)

  override fun getAssociatedStylesheet(source: Source, media: String?, title: String?, charset: String?): Source? = delegate.getAssociatedStylesheet(source, media, title, charset)

  override fun getAttribute(name: String?): Any? = delegate.getAttribute(name)

  override fun getFeature(name: String?): Boolean = delegate.getFeature(name)

  override fun setURIResolver(resolver: URIResolver?) {
    delegate.uriResolver = resolver
  }

  override fun getURIResolver(): URIResolver? = delegate.uriResolver

  override fun setErrorListener(listener: ErrorListener?) {
    delegate.errorListener = listener
  }

  override fun getErrorListener(): ErrorListener? = delegate.errorListener

  companion object {
    /**
     * The system property [TransformerFactory.newInstance] reads first to pick an implementation.
     */
    private const val TransformerFactoryProperty: String = "javax.xml.transform.TransformerFactory"

    /**
     * The option this factory rejects. Set for the duration of [withRejectedOption] only.
     */
    private var rejectedOptionName: String? = null

    /**
     * Runs [action] with this factory installed as the JVM wide [TransformerFactory], rejecting [optionName].
     */
    fun <T> withRejectedOption(optionName: String, action: () -> T): T {
      rejectedOptionName = optionName
      val previousFactoryClassName: String? = System.setProperty(TransformerFactoryProperty, RejectingTransformerFactory::class.java.name)

      try {
        return action()
      } finally {
        rejectedOptionName = null
        if (previousFactoryClassName == null) {
          System.clearProperty(TransformerFactoryProperty)
        } else {
          System.setProperty(TransformerFactoryProperty, previousFactoryClassName)
        }
      }
    }
  }
}
