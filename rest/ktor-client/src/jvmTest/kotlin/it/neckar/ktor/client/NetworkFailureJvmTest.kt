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

import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isFalse
import assertk.assertions.isInstanceOf
import assertk.assertions.isTrue
import io.ktor.client.*
import io.ktor.client.engine.*
import io.ktor.client.engine.apache.*
import io.ktor.client.engine.cio.*
import io.ktor.client.engine.jetty.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.network.sockets.ConnectTimeoutException
import io.ktor.client.plugins.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import org.junit.jupiter.api.Test
import org.junit.jupiter.params.ParameterizedTest
import org.junit.jupiter.params.provider.MethodSource
import java.io.IOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.ServerSocket
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.nio.channels.UnresolvedAddressException

class NetworkFailureJvmTest {
  @ParameterizedTest
  @MethodSource("engines")
  suspend fun `reports a refused connection as unreachable`(engineFactory: HttpClientEngineFactory<*>) {
    HttpClient(engineFactory).use { client ->
      val outcome = tryFetch {
        client.get("http://127.0.0.1:${closedPort()}/").bodyAsText()
      }

      assertThat(outcome).isInstanceOf(FetchOutcome.Unreachable::class)
    }
  }

  @Test
  suspend fun `safeTryFetch returns onFailure for a refused connection`() {
    HttpClient(Apache).use { client ->
      val result = safeTryFetch("unreachable") {
        client.get("http://127.0.0.1:${closedPort()}/").bodyAsText()
      }

      assertThat(result).isEqualTo("unreachable")
    }
  }

  @ParameterizedTest
  @MethodSource("networkFailures")
  fun `matches a socket exception directly and wrapped`(networkFailure: Throwable) {
    assertThat(networkFailure.isNetworkFailure()).isTrue()
    assertThat(IllegalStateException("wrapped", networkFailure).isNetworkFailure()).isTrue()
  }

  @ParameterizedTest
  @MethodSource("answeredFailures")
  fun `does not match a failure after the server was reached`(answeredFailure: Throwable) {
    assertThat(answeredFailure.isNetworkFailure()).isFalse()
  }

  /**
   * A port that was free a moment ago; nothing listens on it, so a connect is refused unless another process binds it in between.
   */
  private fun closedPort(): Int = ServerSocket(0).use { it.localPort }

  companion object {
    @JvmStatic
    fun engines(): List<HttpClientEngineFactory<*>> = listOf(Apache, CIO, Jetty, OkHttp)

    @JvmStatic
    fun networkFailures(): List<Throwable> = listOf(
      ConnectException("Connection refused"),
      ConnectTimeoutException("Connect timeout has expired", null),
      NoRouteToHostException("No route to host"),
      UnknownHostException("unreachable.invalid"),
      UnresolvedAddressException(),
    )

    @JvmStatic
    fun answeredFailures(): List<Throwable> = listOf(
      IOException("Premature end of chunk coded message body"),
      SocketTimeoutException("Read timed out"),
      HttpRequestTimeoutException("http://127.0.0.1/", 1000),
    )
  }
}
