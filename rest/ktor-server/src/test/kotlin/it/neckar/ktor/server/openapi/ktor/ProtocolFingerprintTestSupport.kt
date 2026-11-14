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

import io.ktor.server.testing.*
import it.neckar.ktor.AuthenticationRequirement
import it.neckar.open.kotlin.lang.requireNotNull
import it.neckar.rest.openapi.OpenApiMetaData
import it.neckar.rest.version.ProtocolFingerprint

/**
 * The fingerprint of an application with [OpenApiPlugin] installed; [configure] sets what the test varies.
 */
internal fun fingerprintOfPlugin(configure: OpenApiPluginConfiguration.() -> Unit): ProtocolFingerprint {
  var fingerprint: ProtocolFingerprint? = null

  testApplication {
    install(OpenApiPlugin) {
      metadata = OpenApiMetaData(title = "Test API", description = "Fingerprinted")
      authenticationRequirement = AuthenticationRequirement.None
      scalarUi = ScalarUiMode.Disabled
      configure()
    }
    application { fingerprint = protocolFingerprint() }
  }

  return fingerprint.requireNotNull { "The application did not start, so no fingerprint was taken" }
}
