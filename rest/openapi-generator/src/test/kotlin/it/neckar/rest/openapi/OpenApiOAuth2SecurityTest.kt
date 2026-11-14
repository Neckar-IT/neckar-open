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
import it.neckar.open.http.Url
import it.neckar.open.kotlin.serializers.JsonPath
import it.neckar.open.serialization.atPath
import it.neckar.open.serialization.isJsonEqualTo
import it.neckar.rest.openapi.documentation.DocumentationProvider
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Test

/**
 * Verifies the `components/securitySchemes` output.
 *
 * `bearerAuth` is always present; an [OAuth2Config] on the [OpenApiMetaData] adds an `oauth2`
 * scheme with an `authorizationCode` flow next to it.
 */
class OpenApiOAuth2SecurityTest {
  private val generator = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None)

  @Test
  fun `without oauth2 config only bearerAuth is emitted`() {
    val securitySchemes = generator
      .generate(routes = emptyList(), metaData = OpenApiMetaData("Test", "Test"))
      .jsonObject.atPath(JsonPath("components", "securitySchemes"))

    assertThat(securitySchemes).isJsonEqualTo(
      //language=JSON
      """
      {
        "bearerAuth": { "type": "http", "scheme": "bearer", "bearerFormat": "JWT" }
      }
      """.trimIndent()
    )
  }

  @Test
  fun `oauth2 config adds an oauth2 authorizationCode scheme next to bearerAuth`() {
    val metaData = OpenApiMetaData(
      title = "Test",
      description = "Test",
      oAuth2Config = OAuth2Config(
        authorizationUrl = Url.absolute("https://idp.example.com/auth"),
        tokenUrl = Url.absolute("https://idp.example.com/token"),
        clientId = "test-client",
        scopes = mapOf("openid" to "OpenID Connect"),
      ),
    )

    val securitySchemes = generator
      .generate(routes = emptyList(), metaData = metaData)
      .jsonObject.atPath(JsonPath("components", "securitySchemes"))

    assertThat(securitySchemes).isJsonEqualTo(
      //language=JSON
      """
      {
        "bearerAuth": { "type": "http", "scheme": "bearer", "bearerFormat": "JWT" },
        "oauth2": {
          "type": "oauth2",
          "flows": {
            "authorizationCode": {
              "authorizationUrl": "https://idp.example.com/auth",
              "tokenUrl": "https://idp.example.com/token",
              "x-scalar-client-id": "test-client",
              "scopes": { "openid": "OpenID Connect" }
            }
          }
        }
      }
      """.trimIndent()
    )
  }
}
