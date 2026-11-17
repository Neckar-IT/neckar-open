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
package it.neckar.rest

import it.neckar.open.annotations.ExportAsObject
import kotlinx.serialization.Serializable

/**
 * Single source of truth for the platform HTTP header names that flow between Ktor backends
 * and the React frontends.
 *
 * The enum is anchored in `RestModelOpenApiGeneratorTest` so the values flow through
 * the OpenAPI / Orval pipeline into `@neckar-hub/typescript-utils/openapi-types`.
 * `@ExportAsObject` keeps the `headerName` literal in the generated TypeScript schema —
 * the `platform-headers` re-export then binds the wire values via `satisfies`.
 */
@Serializable
@ExportAsObject
enum class PlatformHeader(
  /**
   * The wire value used as the HTTP header name.
   */
  val headerName: String,
) {
  SessionId("NeckarIT-Session-Id"),
  ClientId("NeckarIT-Client-Id"),
  ClientRole("NeckarIT-Client-Role"),
  ProtocolVersion("NeckarIT-Protocol-Version"),
  FeatureFlags("NeckarIT-Feature-Flags"),
  TraceId("NeckarIT-Trace-Id"),

  /**
   * Response header carrying the git hash of a server inside a fat-jar CLI. Absent in a container: an image names its digest,
   * see [AppImageDigest].
   */
  AppGitHash("X-App-Git-Hash"),

  /**
   * Response header carrying the digest of the image the service runs. Set by Ktor services via
   * `installNeckarServiceDefaults`; static frontends carry the same value in `<meta name="imageDigest">`.
   */
  AppImageDigest("X-App-Image-Digest"),
}
