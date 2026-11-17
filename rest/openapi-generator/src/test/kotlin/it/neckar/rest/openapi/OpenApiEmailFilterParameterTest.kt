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

import assertk.*
import assertk.assertions.*
import io.ktor.http.*
import it.neckar.rest.filtering.FilterKey
import it.neckar.rest.filtering.HasTypedFilterKey
import it.neckar.rest.filtering.TypedFilterKey
import it.neckar.rest.filtering.email
import it.neckar.rest.openapi.documentation.DocumentationProvider
import it.neckar.rest.openapi.dsl.filteredBy
import it.neckar.rest.openapi.dsl.openApiRoute
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import org.junit.jupiter.api.Test

/**
 * An email filter is stated as the address it compares: a string of `format: email` whose source type is `EmailAddress`.
 */
class OpenApiEmailFilterParameterTest {
  enum class MyFilterKeys(override val typedFilterKey: TypedFilterKey) : HasTypedFilterKey {
    ReviewerEmail(FilterKey("reviewerEmail").email()),
  }

  @Test
  fun `an email filter is a string of format email`() {
    val route = openApiRoute("/reviews", HttpMethod.Get, emptyList()) {
      request {
        filteredBy(MyFilterKeys.entries.toList(), MyFilterKeys::class)
      }

      response("Success") {
        statusCode(HttpStatusCode.OK)
        noExamples()
      }
    }

    val json = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None)
      .generate(listOf(route), additionalTypes = emptyList(), metaData = OpenApiMetaData.default)
      .toString()

    val parameter: JsonObject = Json.parseToJsonElement(json).jsonObject
      .getValue("paths").jsonObject
      .getValue("/reviews").jsonObject
      .getValue("get").jsonObject
      .getValue("parameters").jsonArray
      .map { it.jsonObject }
      .single { it.getValue("name").jsonPrimitive.content == "reviewerEmail" }
    val schema: JsonObject = parameter.getValue("schema").jsonObject

    assertThat(schema.getValue("type").jsonPrimitive.content).isEqualTo("string")
    assertThat(schema.getValue("format").jsonPrimitive.content).isEqualTo("email")
    assertThat(schema.getValue("x-source-kotlin-type").jsonPrimitive.content).isEqualTo("it.neckar.contact.EmailAddress")
  }
}
