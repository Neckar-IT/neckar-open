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
package it.neckar.ktor.proxy

import assertk.assertThat
import assertk.assertions.isEqualTo
import io.ktor.http.*
import org.junit.jupiter.api.Test

class ForwardedClientAddressTest {

  @Test
  fun `takes the entry the proxy appended, not the ones the caller sent`() {
    assertThat(ForwardedClientAddress.of(headersOf(HttpHeaders.XForwardedFor, "forged text, 10.0.0.1 , 203.0.113.7"), socketAddress = "172.18.0.2"))
      .isEqualTo(ForwardedClientAddress("203.0.113.7"))
  }

  @Test
  fun `reads the header line the proxy appended, not one the caller sent`() {
    val headers: Headers = headersOf(HttpHeaders.XForwardedFor, listOf("forged", "9.9.9.9, 203.0.113.7"))

    assertThat(ForwardedClientAddress.of(headers, socketAddress = "172.18.0.2")).isEqualTo(ForwardedClientAddress("203.0.113.7"))
  }

  @Test
  fun `falls back to the socket address without a forwarded header`() {
    assertThat(ForwardedClientAddress.of(Headers.Empty, socketAddress = "172.18.0.2")).isEqualTo(ForwardedClientAddress("172.18.0.2"))
  }

  @Test
  fun `falls back to the socket address for an empty forwarded header`() {
    assertThat(ForwardedClientAddress.of(headersOf(HttpHeaders.XForwardedFor, " , "), socketAddress = "172.18.0.2")).isEqualTo(ForwardedClientAddress("172.18.0.2"))
  }
}
