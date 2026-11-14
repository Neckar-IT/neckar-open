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
package it.neckar.ktor.client.plugin.stats

import assertk.assertThat
import assertk.assertions.isEqualTo
import io.ktor.http.HttpMethod
import io.ktor.http.Url
import org.junit.jupiter.api.Test

class PendingRequestsStateTest {
  private fun pending(startedAt: Double) = PendingRequestDescriptor(Url("https://example.org/$startedAt"), HttpMethod.Get, startedAt)
  private fun completed(startedAt: Double) = CompletedRequestDescriptor(Url("https://example.org/$startedAt"), HttpMethod.Get, startedAt, startedAt + 1)

  @Test
  fun `progress is completed over total`() {
    // 2 of 4 requests done → 0.5
    val state = PendingRequestsState(
      pendingRequests = listOf(pending(1.0), pending(2.0)),
      completedRequests = listOf(completed(3.0), completed(4.0)),
    )
    assertThat(state.calculateProgress()).isEqualTo(0.5)
  }

  @Test
  fun `progress is 1 when all requests completed`() {
    val state = PendingRequestsState(
      completedRequests = listOf(completed(1.0), completed(2.0), completed(3.0)),
    )
    assertThat(state.calculateProgress()).isEqualTo(1.0)
  }

  @Test
  fun `progress is 0 with no requests`() {
    assertThat(PendingRequestsState().calculateProgress()).isEqualTo(0.0)
  }
}
