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
import assertk.assertions.isFalse
import assertk.assertions.isTrue
import kotlin.test.Test

class NetworkFailureJsTest {
  @Test
  fun `matches the Ktor error for the network-error message of every browser`() {
    listOf(
      "Failed to fetch", //Chromium
      "NetworkError when attempting to fetch resource.", //Firefox
      "Load failed", //Safari 17+
    ).forEach { browserMessage ->
      assertThat(Error(KtorFetchFailureMessage, typeError(browserMessage)).isNetworkFailure()).isTrue()
    }
  }

  @Test
  fun `does not match a Ktor error without a TypeError cause`() {
    assertThat(Error(KtorFetchFailureMessage, Error("Failed to fetch")).isNetworkFailure()).isFalse()
    assertThat(Error(KtorFetchFailureMessage, abortError()).isNetworkFailure()).isFalse()
    assertThat(Error(KtorFetchFailureMessage).isNetworkFailure()).isFalse()
  }

  @Test
  fun `does not match the bare browser error`() {
    assertThat(typeError("Failed to fetch").isNetworkFailure()).isFalse()
  }

  private fun typeError(message: String): Throwable = js("new TypeError(message)").unsafeCast<Throwable>()

  private fun abortError(): Throwable = js("new DOMException('The operation was aborted.', 'AbortError')").unsafeCast<Throwable>()
}
