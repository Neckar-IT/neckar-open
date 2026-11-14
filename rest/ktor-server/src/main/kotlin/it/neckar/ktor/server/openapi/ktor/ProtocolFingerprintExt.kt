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

import io.ktor.server.application.*
import io.ktor.util.*
import it.neckar.open.kotlin.lang.checkNotNull
import it.neckar.rest.openapi.openApiFingerprint
import it.neckar.rest.version.ProtocolFingerprint
import kotlinx.serialization.json.jsonObject

/**
 * The fingerprint of the interface this application serves, computed once and kept on the
 * application.
 *
 * It covers the category documents [OpenApiPlugin] serves; Orval generates one client file from each. Computing it walks
 * every route and reflects over every type it references, so it happens once per application.
 *
 * Read it after [OpenApiPlugin] is installed and the routes are registered: without the plugin it throws, and the value
 * is cached from the first call on, so an earlier call would freeze a fingerprint over a half-empty routing tree.
 */
fun Application.protocolFingerprint(): ProtocolFingerprint = attributes.computeIfAbsent(ProtocolFingerprintKey) {
  val documentSource = attributes.getOrNull(OpenApiDocumentSourceKey).checkNotNull {
    "OpenApiPlugin is not installed - the fingerprint hashes the documents the plugin serves, so install the plugin first"
  }
  val routes = openApiRoutes()

  val documentsByCategory = documentSource.categories(routes).associateWith { category ->
    documentSource.generateCategory(category = category, routes = routes).jsonObject
  }

  openApiFingerprint(documentsByCategory)
}

internal val ProtocolFingerprintKey: AttributeKey<ProtocolFingerprint> = AttributeKey("ProtocolFingerprint")
