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
package it.neckar.rest.openapi.transform

import assertk.*
import assertk.assertions.*
import it.neckar.open.serialization.isJsonEqualTo
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Test

class OpenApiDocumentTest {
  private val document: OpenApiDocument = OpenApiDocument(
    Json.parseToJsonElement(
      //language=JSON
      $$"""
        {
          "paths": { "/old": { "x-type-ref": "#/components/schemas/Old" } },
          "components": {
            "schemas": {
              "Response": { "oneOf": [ { "$ref": "#/components/schemas/Old" } ], "discriminator": { "propertyName": "type", "mapping": { "bad/Old": "#/components/schemas/Old" } } },
              "Old": { "type": "object", "description": "#/components/schemas/Old" }
            }
          }
        }
      """.trimIndent()
    ).jsonObject
  )

  @Test
  fun `a rewrite reaches every string naming the component and leaves the keys`() {
    val rewritten = document.rewriteReferences(mapOf(ComponentName("Old") to ComponentName("New")))

    assertThat(rewritten.json).isJsonEqualTo(
      //language=JSON
      $$"""
        {
          "paths": { "/old": { "x-type-ref": "#/components/schemas/New" } },
          "components": {
            "schemas": {
              "Response": { "oneOf": [ { "$ref": "#/components/schemas/New" } ], "discriminator": { "propertyName": "type", "mapping": { "bad/Old": "#/components/schemas/New" } } },
              "Old": { "type": "object", "description": "#/components/schemas/New" }
            }
          }
        }
      """.trimIndent()
    )
  }

  @Test
  fun `the schemas keep their order through a replacement`() {
    val reversed = document.withSchemas(document.schemas.entries.reversed().associate { it.key to it.value })

    assertThat(reversed.schemas.keys.toList()).containsExactly(ComponentName("Old"), ComponentName("Response"))
    assertThat(reversed.json.getValue("paths")).isEqualTo(document.json.getValue("paths"))
  }
}
