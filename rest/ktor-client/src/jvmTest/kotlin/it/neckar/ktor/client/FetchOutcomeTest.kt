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

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isSameInstanceAs
import assertk.assertions.prop
import io.ktor.client.engine.mock.*
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import io.ktor.http.*
import org.junit.jupiter.api.Test
import java.net.ConnectException

class FetchOutcomeTest {
  @Test
  suspend fun `tryFetch reports a 401 as unauthorized`() {
    createHttpClient(MockEngine { respondError(HttpStatusCode.Unauthorized) }, "test-client").use { client ->
      val outcome = tryFetch { client.get("http://localhost/").bodyAsText() }

      assertThat(outcome).isEqualTo(FetchOutcome.Unauthorized)
    }
  }

  @Test
  suspend fun `tryFetch rethrows a 4xx other than 401 and a 5xx`() {
    createHttpClient(MockEngine { respondError(HttpStatusCode.Forbidden) }, "test-client").use { client ->
      assertFailure { tryFetch { client.get("http://localhost/").bodyAsText() } }.isInstanceOf(ClientRequestException::class)
    }

    createHttpClient(MockEngine { respondError(HttpStatusCode.InternalServerError) }, "test-client").use { client ->
      assertFailure { tryFetch { client.get("http://localhost/").bodyAsText() } }.isInstanceOf(ServerResponseException::class)
    }
  }

  @Test
  suspend fun `tryFetch delivers the response of a server that answered`() {
    createHttpClient(MockEngine { respondOk("OK") }, "test-client").use { client ->
      val outcome = tryFetch { client.get("http://localhost/").bodyAsText() }

      assertThat(outcome.getDelivered()).isEqualTo("OK")
    }
  }

  @Test
  fun `getDelivered throws with the network failure as cause for an unreachable server`() {
    val networkFailure = ConnectException("Connection refused")

    assertFailure { FetchOutcome.Unreachable(networkFailure).getDelivered() }
      .isInstanceOf(IllegalStateException::class)
      .prop(Throwable::cause).isSameInstanceAs(networkFailure)
  }

  @Test
  fun `getDelivered throws for a 401`() {
    assertFailure { FetchOutcome.Unauthorized.getDelivered() }
      .isInstanceOf(IllegalStateException::class)
      .hasMessage("The server answered 401")
  }
}
