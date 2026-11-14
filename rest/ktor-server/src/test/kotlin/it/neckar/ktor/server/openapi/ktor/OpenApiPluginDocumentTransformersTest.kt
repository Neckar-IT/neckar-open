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
package it.neckar.ktor.server.openapi.ktor

import assertk.*
import assertk.assertions.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.testing.*
import it.neckar.ktor.AuthenticationRequirement
import it.neckar.ktor.client.get
import it.neckar.open.http.Url
import it.neckar.open.test.utils.DisableLogging
import it.neckar.rest.openapi.OpenApiMetaData
import it.neckar.rest.openapi.transform.OpenApiDocument
import it.neckar.rest.openapi.transform.OpenApiDocumentTransformer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Test

@DisableLogging
class OpenApiPluginDocumentTransformersTest {
  private val marking: OpenApiDocumentTransformer = OpenApiDocumentTransformer { document ->
    OpenApiDocument(JsonObject(document.json + ("x-marked" to JsonPrimitive(true))))
  }

  @Test
  fun `every served document passes the configured transformers`() = testApplication {
    installOpenApiPlugin(listOf(marking))
    startApplication()

    listOf(
      OpenApiEndpoints.openapiUrl(),
      OpenApiEndpoints.byCategoryUrl(OpenApiMetaData.Category.Meta),
      OpenApiEndpoints.openapiWithOriginUrl(),
    ).forEach { url ->
      val response = client.get(Url.localhostHttp + url) {
        accept(ContentType.Application.Json)
      }
      assertThat(Json.parseToJsonElement(response.bodyAsText()).jsonObject["x-marked"], url.value).isEqualTo(JsonPrimitive(true))
    }
  }

  @Test
  fun `the fingerprint applies the configured transformers`() {
    assertThat(fingerprintOfPlugin { documentTransformers = listOf(marking) })
      .isNotEqualTo(fingerprintOfPlugin { documentTransformers = OpenApiDocumentTransformer.Defaults })
  }

  private fun ApplicationTestBuilder.installOpenApiPlugin(documentTransformers: List<OpenApiDocumentTransformer>) {
    install(OpenApiPlugin) {
      metadata = OpenApiMetaData(title = "Test API", description = "Transformed")
      authenticationRequirement = AuthenticationRequirement.None
      scalarUi = ScalarUiMode.Disabled
      this.documentTransformers = documentTransformers
    }
  }
}
