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
import it.neckar.open.http.Url
import it.neckar.open.kotlin.lang.requireNotNull
import kotlinx.coroutines.test.*
import kotlinx.serialization.Serializable
import kotlin.test.Ignore
import kotlin.test.Test

/**
 *
 */
@Ignore
class SimpleJsKtorClientTest {
  @Test
  fun testIt() = runTest {
    val client: HttpClient = createHttpClient("test-client") {
      expectSuccess = false
    }

    client.putJson<FooBar>(
      url = Url.absolute("https://httpbin.org/status/413"),
      objectToSend = FooBar("foo", 42)
    ) {
      val bodyType1 = bodyType.requireNotNull()

      assertThat(bodyType1.type).isEqualTo(FooBar::class)
      assertThat(bodyType1.kotlinType.toString()).isEqualTo("FooBar")
    }

    client.putJson<FooBarInterface>(
      url = Url.absolute("https://httpbin.org/status/413"),
      objectToSend = FooBar("foo", 42)
    ) {
      val bodyType1 = bodyType.requireNotNull()

      assertThat(bodyType1.type).isEqualTo(FooBarInterface::class)
      assertThat(bodyType1.kotlinType.toString()).isEqualTo("FooBarInterface")
      //assertThat(bodyType1.reifiedType).isEqualTo(FooBarInterface::class)
    }
  }

  private interface FooBarInterface

  @Serializable
  private data class FooBar(val foo: String, val bar: Int) : FooBarInterface
}
