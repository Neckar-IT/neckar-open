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

import assertk.*
import assertk.assertions.*
import io.ktor.client.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.server.testing.*
import it.neckar.open.test.utils.RandomWithSeed
import it.neckar.open.test.utils.isFalse
import it.neckar.open.test.utils.isTrue
import it.neckar.rest.ClientRole
import it.neckar.rest.tracking.XClientRole
import org.junit.jupiter.api.Test
import java.util.concurrent.atomic.AtomicBoolean
import it.neckar.open.test.utils.DisableLogging


/**
 *
 */
@DisableLogging
class ClientRoleExtKtTest {
  @RandomWithSeed
  @Test
  fun testInstallClientRoleHeader() = testApplication {
    val clientRole = ClientRole("my-role")

    val client: HttpClient = createClient {
      installClientRoleHeader(clientRole)
    }

    val called = AtomicBoolean(false)

    application {
      routing {
        get("foobar") {
          assertThat(called).isFalse()
          called.set(true)
          assertThat(call.request.header(HttpHeaders.XClientRole)).isEqualTo(clientRole.value)
          call.respondText("Hello World")
        }
      }
    }

    client.get("foobar").let {
      assertThat(it.bodyAsText()).isEqualTo("Hello World")
    }

    assertThat(called).isTrue()
  }
}
