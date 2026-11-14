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
package it.neckar.ktor.client.advanced

import assertk.*
import assertk.assertions.*
import io.ktor.client.engine.mock.*
import it.neckar.ktor.client.createHttpClient
import it.neckar.logging.Logger
import it.neckar.logging.LoggerFactory
import it.neckar.open.http.Url
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import java.net.ConnectException

/**
 *
 */
class AdvancedClientTest {
  @Test
  suspend fun testAdvancedClient() {
    val client = createHttpClient(MockEngine { throw ConnectException("Could not connect") }, "test-client") {}

    val urlChain = UrlChain(
      listOf(
        Url.absolute("https://firstHost"),
        Url.absolute("https://secondHost"),
      )
    )

    val advancedClient = AdvancedClient(client, urlChain)

    assertFailure {
      advancedClient.putJson(Url.relative("foo/bar/17"), FooBar("foo", 42))
    }.all {
      isInstanceOf(AdvancedClient.CouldNotConnectToAnyUrlException::class)
      messageContains("Failed to PUT to any URL in the chain")
      messageContains("https://firstHost/foo/bar/17")
      messageContains("https://secondHost/foo/bar/17")
      cause().isNotNull()
        .isInstanceOf(ConnectException::class)
      given {
        assertk.assertThat(it.suppressed).hasSize(1)
      }
    }
  }

  @Serializable
  private data class FooBar(val foo: String, val bar: Int)

  companion object {
    private val logger: Logger = LoggerFactory.getLogger("it.neckar.ktor.client.advanced.AdvancedClientTest")
  }
}

