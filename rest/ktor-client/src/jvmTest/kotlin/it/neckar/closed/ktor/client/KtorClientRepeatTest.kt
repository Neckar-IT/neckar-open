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
package it.neckar.closed.ktor.client

import assertk.*
import assertk.assertions.*
import io.ktor.client.engine.okhttp.*
import io.ktor.client.plugins.*
import it.neckar.ktor.client.createHttpClient
import it.neckar.ktor.client.get
import it.neckar.open.http.Url
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

/**
 *
 */
@Disabled
class KtorClientRepeatTest {
  @Test
  suspend fun testTimeout() {
    val client = createHttpClient(OkHttp, "test") {
      install(HttpTimeout) {
        requestTimeoutMillis = 100
        socketTimeoutMillis = 100
        connectTimeoutMillis = 100
      }
    }

    assertFailure {
      client.get(Url.absolute("https://httpbin.org/delay/2"))
    }.isInstanceOf(HttpRequestTimeoutException::class)
      .all {
        messageContains("Request timeout has expired")
        messageContains("request_timeout=100 ms")
      }
  }

  @Test
  suspend fun test500() {
    val client = createHttpClient(OkHttp, "test") {}

    assertFailure {
      client.get(Url.absolute("https://httpbin.org/status/500"))
    }.isInstanceOf(ServerResponseException::class)
  }

}
