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

import it.neckar.projects.common.ServiceName
import assertk.*
import assertk.assertions.*
import io.ktor.client.statement.*
import io.ktor.server.application.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import it.neckar.ktor.KtorService
import it.neckar.ktor.baseUrl
import it.neckar.ktor.client.createHttpClient
import it.neckar.ktor.client.get
import it.neckar.open.http.Url
import it.neckar.projects.common.Port
import it.neckar.projects.common.ServiceDescriptor
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass

/**
 * Ensures the service factory can be configured in a setup method before the server is started.
 */
@WithLiveKtorServer(service = ConfigurableKtorService::class, serviceFactory = ConfigurableKtorServiceFactory::class)
class LiveKtorServerExtensionFactoryTest {
  @BeforeEach
  fun setUp(factory: ConfigurableKtorServiceFactory) {
    factory.responseText = "configured-before-start"
  }

  @Test
  suspend fun `factory configuration is applied before service creation`(runningKtorServer: RunningKtorServer) {
    val client = createHttpClient("factory-test") {}

    val response = client.get(runningKtorServer.baseUrl + "configured")

    assertThat(response.status.value).isEqualTo(200)
    assertThat(response.bodyAsText()).isEqualTo("configured-before-start")
  }
}

class ConfigurableKtorService(
  private val responseText: String,
  portOverride: Port? = null,
) : KtorService {

  override fun Application.configure() {
    routing {
      get("/configured") {
        call.respondText(responseText)
      }
    }
  }

  override val serviceDescriptor: ServiceDescriptor = object : ServiceDescriptor {
    override val serviceName: ServiceName = ServiceName("Configurable Test Service")
    override val port: Port = Port(9_999)
    override val apiPrefix: String = ""
  }

  override val baseUrl: Url.Absolute = serviceDescriptor.baseUrl(portOverride)
}

class ConfigurableKtorServiceFactory : KtorServiceFactory<ConfigurableKtorService> {
  var responseText: String = "default"

  override fun create(serviceClass: KClass<out KtorService>, portOverride: Port?): ConfigurableKtorService {
    return ConfigurableKtorService(responseText, portOverride)
  }
}
