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
package it.neckar.rest.test.ktor

import io.ktor.server.engine.*
import io.ktor.server.netty.*
import it.neckar.ktor.KtorService
import it.neckar.open.http.Url
import it.neckar.projects.common.Port

/**
 * Holds the running server and related information for tests.
 */
data class RunningKtorServer(
  /**
   * The service that is running
   */
  val service: KtorService,

  /**
   * The base URL of the running server (including port information)
   * e.g., http://localhost:42079
   */
  val baseUrl: Url.Absolute,

  /**
   * The port the server is running on.
   * ATTENTION: The [baseUrl] already contains the port information.
   */
  val port: Port,

  /**
   * The server that is running the service
   */
  val server: EmbeddedServer<NettyApplicationEngine, NettyApplicationEngine.Configuration>,
) {

  /**
   * Stops the server
   */
  fun stop(gracePeriodMillis: Long = 500, timeoutMillis: Long = 2_000) {
    server.stop(gracePeriodMillis, timeoutMillis)
  }
}
