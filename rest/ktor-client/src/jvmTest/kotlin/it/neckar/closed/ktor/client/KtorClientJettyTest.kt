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
import io.ktor.client.engine.jetty.*
import io.ktor.client.request.*
import it.neckar.ktor.client.createHttpClient
import org.eclipse.jetty.util.ssl.SslContextFactory
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

/**
 * Does *NOT* work with Java 8
 */
@Disabled
class KtorClientJettyTest {
  @Test
  suspend fun testPocJetty() {
    val client = createHttpClient(Jetty, "test") {
      engine {
        configureClient {
          sslContextFactory = SslContextFactory.Client()
          clientCacheSize = 12
        }

      }
    }

    assertThat(client.engine::class.qualifiedName.toString()).contains("Jetty")

    val response = client.get("https://google.de")
    println("response: $response")
  }
}
