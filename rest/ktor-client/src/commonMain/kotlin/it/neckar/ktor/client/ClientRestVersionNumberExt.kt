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
package it.neckar.ktor.client

import io.ktor.client.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import it.neckar.rest.version.ProtocolFingerprint
import it.neckar.rest.version.RestProtocolVersionInfo


/**
 * Adds the protocol version header to the request.
 *
 * The [versionProvider] lambda must explicitly provide the protocol version string.
 * On both JVM and JS targets, use `{ VersionInformation.gitHash }` - resolved at runtime from
 * the injected deploy metadata (image env on the JVM, `window.__APP_GIT_INFO__` in the browser).
 * Without injected metadata the value is `"unknown"`, which the server-side check
 * treats as "check skipped".
 *
 * See [RestProtocolVersionInfo.ProtocolVersionHeaderKey] for the header key.
 */
inline fun HttpClientConfig<*>.installProtocolVersionHeader(crossinline versionProvider: () -> String) {
  install(DefaultRequest) {
    headers {
      append(RestProtocolVersionInfo.ProtocolVersionHeaderKey, versionProvider())
    }
  }
}

/**
 * Adds [fingerprint] to every request of this client — the interface this build was made against.
 */
fun HttpClientConfig<*>.installProtocolVersionHeader(fingerprint: ProtocolFingerprint) {
  installProtocolVersionHeader { fingerprint.value }
}

/**
 * Adds [fingerprint] to this request — for a client that installs no [DefaultRequest] of its own.
 */
fun HttpRequestBuilder.protocolVersion(fingerprint: ProtocolFingerprint) {
  header(RestProtocolVersionInfo.ProtocolVersionHeaderKey, fingerprint.value)
}
