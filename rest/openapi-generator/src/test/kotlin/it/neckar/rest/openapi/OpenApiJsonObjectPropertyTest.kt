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
import it.neckar.open.kotlin.serializers.JsonPath
import it.neckar.open.serialization.atPath
import it.neckar.open.serialization.isJsonEqualTo
import it.neckar.rest.openapi.documentation.DocumentationProvider
import it.neckar.rest.openapi.schema.SchemaDescriptor
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import org.junit.jupiter.api.Test

/**
 * A free-form JSON area is an open object in the spec - the generated TypeScript type therefore ends
 * at the boundary of the area and never carries the keys a customer puts inside it.
 */
class OpenApiJsonObjectPropertyTest {
  private val generator = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None)

  @Serializable
  private data class WithJsonObject(
    val area: JsonObject,
    val optionalArea: JsonObject?,
  )

  @Test
  fun `a JsonObject property is an object that accepts any content`() {
    assertThat(schemaOf("area")).isJsonEqualTo(
      //language=JSON
      """
        {
          "type": "object",
          "additionalProperties": true
        }
      """.trimIndent()
    )
  }

  @Test
  fun `a nullable JsonObject property carries null in its type`() {
    assertThat(schemaOf("optionalArea")).isJsonEqualTo(
      //language=JSON
      """
        {
          "type": ["object", "null"],
          "additionalProperties": true
        }
      """.trimIndent()
    )
  }

  private fun schemaOf(property: String) =
    generator.generateComponentsSchemas(listOf(SchemaDescriptor.of<WithJsonObject>()))
      .atPath(JsonPath("OpenApiJsonObjectPropertyTest.WithJsonObject", "properties", property))
}
