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
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import org.junit.jupiter.api.Test

class SchemaShapeTest {
  @Test
  fun `a discriminator value named like a documentation key is part of the shape`() {
    val first = json("""{ "discriminator": { "propertyName": "type", "mapping": { "description": "#/components/schemas/A" } } }""")
    val second = json("""{ "discriminator": { "propertyName": "type", "mapping": { "description": "#/components/schemas/B" } } }""")

    assertThat(SchemaShape.of(first)).isNotEqualTo(SchemaShape.of(second))
  }

  @Test
  fun `a default value is data down to its fields`() {
    val first = json("""{ "type": "object", "default": { "description": "first" } }""")
    val second = json("""{ "type": "object", "default": { "description": "second" } }""")

    assertThat(SchemaShape.of(first)).isNotEqualTo(SchemaShape.of(second))
  }

  @Test
  fun `a merge keeps the documentation every schema agrees on`() {
    val first = json("""{ "type": "string", "description": "first", "x-german-term": "Auftrag" }""")
    val second = json("""{ "type": "string", "description": "second", "x-german-term": "Auftrag" }""")

    assertThat(SchemaShape.merge(listOf(first, second)))
      .isEqualTo(json("""{ "type": "string", "x-german-term": "Auftrag" }"""))
  }

  @Test
  fun `schemas of different shapes do not merge`() {
    assertFailure { SchemaShape.merge(listOf(json("""{ "type": "string" }"""), json("""{ "type": "integer" }"""))) }
      .messageContains("Only schemas of one shape merge")
  }

  private fun json(text: String): JsonElement {
    return Json.parseToJsonElement(text)
  }
}
