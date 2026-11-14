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
import assertk.assertions.isEmpty
import assertk.assertions.isEqualTo
import assertk.assertions.isTrue
import io.github.optimumcode.json.schema.JsonSchema
import io.github.optimumcode.json.schema.ValidationError
import it.neckar.rest.openapi.documentation.DocumentationProvider
import it.neckar.rest.openapi.schema.SchemaDescriptor
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Test

/**
 * Since OpenAPI 3.1 a schema object *is* a JSON Schema (draft 2020-12). This test holds the generator
 * to that: its output is fed to a plain JSON Schema validator, which then accepts a matching document
 * and rejects a broken one.
 *
 * The one thing that has to be adapted is the reference target. The generator writes OpenAPI-style
 * `#/components/schemas/…`, and a JSON Schema validator resolves pointers into schema locations only -
 * `components` is an unknown keyword to it, so its content is not addressable.
 */
class OpenApiSchemaIsJsonSchemaTest {
  @Serializable
  private data class Address(val zip: String, val city: String?)

  @Serializable
  private enum class Kind {
    @SerialName("a")
    A,

    @SerialName("b")
    B,
  }

  @Serializable
  private data class Customer(
    val name: String,
    val kind: Kind,
    val age: Int,
    val tags: List<String>,
    val address: Address,
    val attributes: Map<String, Int>,
  )

  private val schema: JsonSchema = JsonSchema.fromJsonElement(generatedSchemaDocument())

  @Test
  fun `a matching document passes the generated schema`() {
    //language=JSON
    val document = """
      {
        "name": "Meier",
        "kind": "a",
        "age": 42,
        "tags": ["x"],
        "address": { "zip": "72116", "city": null },
        "attributes": { "a": 1 }
      }
    """.trimIndent()

    val errors = mutableListOf<ValidationError>()
    assertThat(schema.validate(Json.parseToJsonElement(document), errors::add)).isTrue()
    assertThat(errors).isEmpty()
  }

  @Test
  fun `a broken document is rejected with a path per violation`() {
    //language=JSON
    val document = """
      {
        "name": "Meier",
        "kind": "c",
        "age": "alt",
        "tags": "x",
        "address": {},
        "attributes": { "a": "1" }
      }
    """.trimIndent()

    val errors = mutableListOf<ValidationError>()
    schema.validate(Json.parseToJsonElement(document), errors::add)

    assertThat(errors.map { "${it.objectPath} ${it.message}" }.sorted()).isEqualTo(
      listOf(
        "/address missing required properties: [zip, city]",
        "/age element is not a integer",
        "/attributes/a element is not a integer",
        "/kind element is not in enum",
        "/tags element is not a array",
      )
    )
  }

  /**
   * The generated component schemas, wrapped into a schema document whose root points at [Customer].
   */
  private fun generatedSchemaDocument(): JsonObject {
    val schemas: JsonObject = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None)
      .generateComponentsSchemas(listOf(SchemaDescriptor.of<Customer>()))

    val withRewrittenReferences: JsonObject =
      Json.parseToJsonElement(schemas.toString().replace("#/components/schemas/", $$"#/$defs/")).jsonObject

    return buildJsonObject {
      put($$"$schema", JsonPrimitive("https://json-schema.org/draft/2020-12/schema"))
      put($$"$defs", withRewrittenReferences)
      put($$"$ref", JsonPrimitive($$"#/$defs/OpenApiSchemaIsJsonSchemaTest.Customer"))
    }
  }
}
