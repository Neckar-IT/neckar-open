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

import assertk.assertThat
import assertk.assertions.isEqualTo
import io.ktor.server.application.Application
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import it.neckar.open.test.utils.DisableLogging
import it.neckar.projects.common.Port
import it.neckar.projects.common.ServiceDescriptor
import org.junit.jupiter.api.Test
import java.net.ServerSocket
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse

@DisableLogging
class AdditionalPortsTest {
  @Test
  fun `the server answers on the port of its descriptor and on every additional port`() {
    val port = freePort()
    val formerPort = freePort()
    val service = movingService(port, formerPort)

    val embeddedServer = server(service, port).start(wait = false)
    try {
      assertThat(listOf(port, formerPort).map { bound -> ping(bound) }).isEqualTo(listOf("pong", "pong"))
    } finally {
      embeddedServer.stop()
    }
  }

  @Test
  fun `a port override binds the override alone`() {
    val override = freePort()
    val service = movingService(port = freePort(), formerPort = freePort(), portOverride = override)

    assertThat(server(service, override).engineConfig.connectors.map { it.port }).isEqualTo(listOf(override.value))
  }

  private fun movingService(port: Port, formerPort: Port, portOverride: Port? = null): KtorService = object : KtorService {
    override val serviceDescriptor: ServiceDescriptor = ServiceDescriptor("moving-service", apiPrefix = "moving", port)
    override val additionalPorts: List<Port> = listOf(formerPort)
    override val portOverride: Port? = portOverride

    override fun Application.configure() {
      routing {
        get("/ping") { call.respondText("pong") }
      }
    }
  }

  private fun freePort(): Port = ServerSocket(0).use { Port(it.localPort) }

  private fun ping(port: Port): String {
    HttpClient.newHttpClient().use { client ->
      return client.send(HttpRequest.newBuilder(URI("http://localhost:${port.value}/ping")).build(), HttpResponse.BodyHandlers.ofString()).body()
    }
  }
}
