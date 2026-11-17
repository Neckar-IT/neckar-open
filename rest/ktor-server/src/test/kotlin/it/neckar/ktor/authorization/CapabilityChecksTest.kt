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
package it.neckar.ktor.authorization

import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.isEqualTo
import io.ktor.client.request.get
import io.ktor.client.statement.bodyAsText
import io.ktor.http.HttpStatusCode
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.routing
import io.ktor.server.testing.testApplication
import io.mockk.mockk
import io.mockk.verify
import it.neckar.ktor.installJson
import it.neckar.ktor.status.installStatusPages
import it.neckar.open.authorization.Capability
import it.neckar.open.authorization.CapabilityKey
import it.neckar.open.authorization.UserCapabilities
import it.neckar.open.test.utils.DisableLogging
import org.junit.jupiter.api.Test
import org.slf4j.Logger

/**
 * A route states the capability it demands; what the caller holds is resolved by whatever the
 * service installed. These tests pin both directions of that gate and the case a service forgets
 * the installation — which must fail loudly rather than let every caller through.
 */
@DisableLogging
class CapabilityChecksTest {

  @Test
  fun `a caller holding the capability passes the gate`() = testApplication {
    application {
      installStatusPages(logger = mockk(relaxed = true))
      installJson { }
      installCapabilityChecks { UserCapabilities(setOf(ReadBooks.key)) }

      routing {
        get("/books") {
          requireCapability(ReadBooks)
          call.respondText("the books")
        }
      }
    }

    val response = client.get("/books")

    assertThat(response.status).isEqualTo(HttpStatusCode.OK)
    assertThat(response.bodyAsText()).isEqualTo("the books")
  }

  @Test
  fun `a caller without the capability is answered 403 naming what is missing`() = testApplication {
    application {
      installStatusPages(logger = mockk(relaxed = true))
      installJson { }
      installCapabilityChecks { UserCapabilities.Empty }

      routing {
        get("/books") {
          requireCapability(ReadBooks)
          call.respondText("the books")
        }
      }
    }

    val response = client.get("/books")

    assertThat(response.status).isEqualTo(HttpStatusCode.Forbidden)
    assertThat(response.bodyAsText()).contains("demo:read-books")
  }

  @Test
  fun `a service that installed no resolver fails instead of letting the caller through`() = testApplication {
    val logger: Logger = mockk(relaxed = true)
    application {
      installStatusPages(logger = logger)
      installJson { }

      routing {
        get("/books") {
          requireCapability(ReadBooks)
          call.respondText("the books")
        }
      }
    }

    val response = client.get("/books")

    assertThat(response.status).isEqualTo(HttpStatusCode.InternalServerError)
    // The 500 body leaves the message out; the log names the missing installation.
    verify { logger.error(any<String>(), match<Throwable> { it.message.orEmpty().contains("installCapabilityChecks") }) }
  }

  private data object ReadBooks : Capability {
    override val key: CapabilityKey = CapabilityKey("demo:read-books")
  }
}
