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

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isNotEqualTo
import assertk.assertions.isTrue
import assertk.assertions.messageContains
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import it.neckar.ktor.AuthenticationRequirement
import it.neckar.open.http.Url
import it.neckar.rest.openapi.OAuth2Config
import it.neckar.rest.openapi.OpenApiMetaData
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test

/**
 * Computing the fingerprint generates every category document the plugin serves: every route walked, every type
 * reflected over. It happens once per application.
 */
class ProtocolFingerprintExtTest {

  @Test
  fun `the fingerprint is computed once and kept on the application`() = testApplication {
    application {
      install(OpenApiPlugin) {
        metadata = OpenApiMetaData(title = "Test API", description = "Fingerprinted")
        authenticationRequirement = AuthenticationRequirement.None
        scalarUi = ScalarUiMode.Disabled
      }
      routing {
        get("/jobs") { call.respondText("[]") }
      }

      assertThat(attributes.contains(ProtocolFingerprintKey)).isFalse()

      protocolFingerprint()

      assertThat(attributes.contains(ProtocolFingerprintKey)).isTrue()
    }
  }

  @Test
  fun `a changed additional type changes the fingerprint`() {
    // Orval generates a client file from the category document that carries the additional types.
    assertThat(fingerprintOfPlugin { additionalTypes = OpenApiAdditionalTypes.environment(listOf(RegionSettings::class)) })
      .isNotEqualTo(fingerprintOfPlugin { additionalTypes = OpenApiAdditionalTypes.environment(listOf(ZoneSettings::class)) })
  }

  @Test
  fun `the deployment's metadata leaves the fingerprint alone`() {
    // Title, servers and the OAuth2 URLs differ per deployment; a client of one deployment works against every other.
    val deployed = OpenApiMetaData(
      title = "Test API on staging",
      description = "Fingerprinted",
      servers = listOf(Url.absolute("https://staging.neckar.it")),
      oAuth2Config = OAuth2Config(
        authorizationUrl = Url.absolute("https://auth.staging.neckar.it/auth"),
        tokenUrl = Url.absolute("https://auth.staging.neckar.it/token"),
        clientId = "staging-client",
      ),
    )

    assertThat(fingerprintOfPlugin { metadata = deployed }).isEqualTo(fingerprintOfPlugin { })
  }

  @Test
  fun `without the plugin there is no served document to fingerprint`() = testApplication {
    application {
      assertFailure { protocolFingerprint() }.messageContains("OpenApiPlugin is not installed")
    }
  }

  @Serializable
  private data class RegionSettings(val region: String)

  @Serializable
  private data class ZoneSettings(val zone: String)
}
