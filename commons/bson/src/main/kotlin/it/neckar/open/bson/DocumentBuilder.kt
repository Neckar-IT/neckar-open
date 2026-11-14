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
package it.neckar.open.bson

import org.bson.Document

/**
 * Creates a new document using a DSL-style builder.
 */
fun bson(block: DocumentBuilder.() -> Unit): Document {
  val builder = DocumentBuilder()
  builder.block()
  return builder.build()
}

/**
 * Builder for BSON documents.
 */
class DocumentBuilder {
  private val properties = mutableMapOf<String, Any?>()

  fun put(key: String, value: String?) {
    properties[key] = value
  }

  fun put(key: String, value: Int?) {
    properties[key] = value
  }

  fun put(key: String, value: Double?) {
    properties[key] = value
  }

  fun put(key: String, value: Enum<*>?) {
    properties[key] = value?.name
  }

  fun put(key: String, value: Document?) {
    properties[key] = value
  }

  /**
   * Adds a list of documents to the document under the specified key.
   */
  fun nested(key: String, value: List<Document>) {
    properties[key] = value
  }

  /**
   * Adds a list of strings to the document under the specified key.
   */
  fun strings(key: String, value: List<String>) {
    properties[key] = value
  }

  operator fun set(key: String, value: Any?) {
    properties[key] = value
  }

  fun nested(key: String, block: DocumentBuilder.() -> Unit) {
    val builder = DocumentBuilder()
    builder.block()
    properties[key] = builder.build()
  }

  fun build(): Document = Document(properties)
}
