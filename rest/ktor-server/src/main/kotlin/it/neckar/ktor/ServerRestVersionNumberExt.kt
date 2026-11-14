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
package it.neckar.ktor

import it.neckar.projects.common.ServiceName
import io.ktor.http.*
import io.ktor.server.plugins.defaultheaders.*
import it.neckar.open.version.VersionInformation
import it.neckar.rest.PlatformHeader
import it.neckar.rest.tracking.XServedBy
import it.neckar.rest.version.RestProtocolVersionInfo

/**
 * Adds a custom server name
 */
fun DefaultHeadersConfig.neckarITServerHeader() {
  header(HttpHeaders.Server, "Neckar IT")
}

/**
 * Adds the protocol version header where the service knows its commit (a server inside a fat-jar CLI); a service in an image
 * sends none rather than `unknown`.
 */
fun DefaultHeadersConfig.restVersionNumberHeader() {
  VersionInformation.gitHashOrNull?.let { gitHash ->
    header(RestProtocolVersionInfo.ProtocolVersionHeaderKey, gitHash)
  }
}

/**
 * Adds an `X-Served-By` header to identify the responding service.
 * Helps with debugging when multiple services sit behind a reverse proxy.
 */
fun DefaultHeadersConfig.servedByHeader(serviceName: ServiceName) {
  header(HttpHeaders.XServedBy, serviceName.value)
}

/**
 * Adds the `X-App-Git-Hash` response header where the service knows its commit (a server inside a fat-jar CLI); a service in an
 * image sends none.
 */
fun DefaultHeadersConfig.appGitHashHeader() {
  VersionInformation.gitHashOrNull?.let { gitHash ->
    header(PlatformHeader.AppGitHash.headerName, gitHash)
  }
}

/**
 * The digest of the image the service runs (compose env `APP_IMAGE_DIGEST`), absent without a pinned digest; deploy-check and
 * `curl -sI` read it.
 */
fun DefaultHeadersConfig.appImageDigestHeader() {
  VersionInformation.imageDigestOrNull?.let { imageDigest ->
    header(PlatformHeader.AppImageDigest.headerName, imageDigest)
  }
}
