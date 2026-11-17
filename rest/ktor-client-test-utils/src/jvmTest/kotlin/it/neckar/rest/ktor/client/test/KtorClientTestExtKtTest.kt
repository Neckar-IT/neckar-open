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
package it.neckar.rest.ktor.client.test

import assertk.*
import assertk.assertions.*
import io.ktor.client.engine.mock.*
import io.ktor.client.request.*
import io.ktor.client.statement.*
import it.neckar.ktor.client.bodyAsTextSuccess
import org.junit.jupiter.api.Test

/**
 *
 */
class KtorClientTestExtKtTest {
  @Test
  suspend fun testSingle() {
    val client = createMockClient {
      respondOk("OK")
    }

    val response = client.get("does-not-matter-at-all")
    assertThat(response.status.value).isEqualTo(200)
    assertThat(response.bodyAsTextSuccess()).isEqualTo("OK")
  }

  @Test
  suspend fun testMultiple() {
    val client = createMockClient(
      clientConfig = {
        expectSuccess = false
      }
    ) {
      val urlAsString = it.url.toString()

      when {
        urlAsString.endsWith("1") -> respondOk("OK1")
        urlAsString.endsWith("2") -> respondOk("OK2")
        urlAsString.endsWith("3") -> respondBadRequest()
        else -> TODO()
      }
    }

    assertThat(client.get("does-not-matter-at-all1")).given {
      assertThat(it.status.value).isEqualTo(200)
      assertThat(it.bodyAsText()).isEqualTo("OK1")
    }

    assertThat(client.get("does-not-matter-at-all2")).given {
      assertThat(it.status.value).isEqualTo(200)
      assertThat(it.bodyAsText()).isEqualTo("OK2")
    }

    assertThat(client.get("does-not-matter-at-all3")).given {
      assertThat(it.status.value).isEqualTo(400)
      assertThat(it.bodyAsText()).isEqualTo("Bad Request")
    }
  }

  @Test
  suspend fun testMultiple2() {
    val client = createMockClient(
      clientConfig = {
        expectSuccess = false
      },
      mockResponseCreator = sequential {
        next {
          respondOk("OK1")
        }
        next {
          respondOk("OK2")
        }
        next {
          respondBadRequest()
        }
      }
    )

    assertThat(client.get("does-not-matter-at-all")).given {
      assertThat(it.status.value).isEqualTo(200)
      assertThat(it.bodyAsText()).isEqualTo("OK1")
    }

    assertThat(client.get("does-not-matter-at-all")).given {
      assertThat(it.status.value).isEqualTo(200)
      assertThat(it.bodyAsText()).isEqualTo("OK2")
    }

    assertThat(client.get("does-not-matter-at-all")).given {
      assertThat(it.status.value).isEqualTo(400)
      assertThat(it.bodyAsText()).isEqualTo("Bad Request")
    }
  }
}
