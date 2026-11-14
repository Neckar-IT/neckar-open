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
package it.neckar.open.tracing

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.isEqualTo
import assertk.assertions.isInstanceOf
import assertk.assertions.isNull
import it.neckar.open.serialization.roundTrip
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

class TraceIdTest {
  @Test
  fun `accepts a 32 character lowercase hex value`() {
    assertThat(TraceId("0af7651916cd43dd8448eb211c80319c").value).isEqualTo("0af7651916cd43dd8448eb211c80319c")
  }

  @Test
  fun `rejects anything that is not a W3C trace ID`() {
    assertFailure { TraceId("0af7651916cd43dd") }.isInstanceOf(IllegalArgumentException::class)
    assertFailure { TraceId("0AF7651916CD43DD8448EB211C80319C") }.isInstanceOf(IllegalArgumentException::class)
    assertFailure { TraceId("0af7651916cd43dd8448eb211c80319z") }.isInstanceOf(IllegalArgumentException::class)
  }

  @Test
  fun `orNull answers null for everything that points at no span`() {
    assertThat(TraceId.orNull(null)).isNull()
    assertThat(TraceId.orNull("")).isNull()
    assertThat(TraceId.orNull("not-a-trace-id")).isNull()
    assertThat(TraceId.orNull(TraceId.Invalid.value)).isNull()
  }

  @Test
  fun `orNull answers the trace ID for a valid value`() {
    assertThat(TraceId.orNull("0af7651916cd43dd8448eb211c80319c")).isEqualTo(TraceId("0af7651916cd43dd8448eb211c80319c"))
  }

  @Test
  fun `toString is the bare hex value, so a log line carries no wrapper`() {
    assertThat(TraceId("0af7651916cd43dd8448eb211c80319c").toString()).isEqualTo("0af7651916cd43dd8448eb211c80319c")
  }

  @Test
  fun `the wire form is the bare hex string every OpenTelemetry tool expects`() {
    roundTrip(TraceId("0af7651916cd43dd8448eb211c80319c")) {
      //language=JSON
      """"0af7651916cd43dd8448eb211c80319c""""
    }
  }

  /**
   * Own-service traffic only, so a malformed value is a broken producer rather than a value to
   * tolerate — the same rule the protocol version check applies.
   */
  @Test
  fun `decoding a malformed value fails instead of yielding a trace ID that points nowhere`() {
    assertFailure { Json.decodeFromString(TraceId.serializer(), """"nonsense"""") }.isInstanceOf(IllegalArgumentException::class)
  }
}
