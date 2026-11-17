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
package it.neckar.rest.openapi

import assertk.assertThat
import it.neckar.open.serialization.atPath
import it.neckar.open.serialization.isJsonEqualTo
import it.neckar.rest.openapi.documentation.DocumentationProvider
import it.neckar.rest.openapi.documentation.ValueClassMetadata
import it.neckar.rest.openapi.schema.SchemaDescriptor
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass

/**
 * The JSON-Schema constraints of a value class (minLength/maxLength/pattern for strings,
 * minimum/maximum/multipleOf for numbers) are emitted next to the underlying type.
 */
class OpenApiValueClassConstraintsTest {
  @JvmInline
  @Serializable
  value class Isbn(val value: String)

  @JvmInline
  @Serializable
  value class Rating(val value: Int)

  @Serializable
  private data class Book(val isbn: Isbn, val rating: Rating)

  /** Returns constraint metadata for the two value classes, nothing else. */
  private val documentationProvider = object : DocumentationProvider by DocumentationProvider.None {
    override fun getValueClassMetadata(clazz: KClass<*>): ValueClassMetadata? = when (clazz) {
      Isbn::class -> ValueClassMetadata(unitSymbol = null, unitName = null, germanTerm = null, minLength = 10, maxLength = 13, pattern = "[0-9-]+")
      Rating::class -> ValueClassMetadata(unitSymbol = null, unitName = null, germanTerm = null, minimum = 1.0, maximum = 5.0, multipleOf = 1.0)
      else -> null
    }
  }

  private fun schemaOf(property: String): JsonObject {
    val book = SchemaDescriptor.of<Book>()

    return OpenApiJsonGenerator(documentationProvider = documentationProvider)
      .generateComponentsSchemas(listOf(book))
      .atPath(book.componentPath() + "properties" + property)
  }

  @Test
  fun `string value class carries minLength, maxLength and pattern`() {
    assertThat(schemaOf("isbn")).isJsonEqualTo(
      //language=JSON
      """
      {
        "x-source-kotlin-type": "it.neckar.rest.openapi.OpenApiValueClassConstraintsTest.Isbn",
        "minLength": 10,
        "maxLength": 13,
        "pattern": "[0-9-]+",
        "type": "string"
      }
      """.trimIndent()
    )
  }

  @Test
  fun `numeric value class carries minimum, maximum and multipleOf`() {
    assertThat(schemaOf("rating")).isJsonEqualTo(
      //language=JSON
      """
      {
        "x-source-kotlin-type": "it.neckar.rest.openapi.OpenApiValueClassConstraintsTest.Rating",
        "minimum": 1.0,
        "maximum": 5.0,
        "multipleOf": 1.0,
        "type": "integer"
      }
      """.trimIndent()
    )
  }
}
