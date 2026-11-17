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

import assertk.assertThat
import assertk.assertions.containsExactly
import assertk.assertions.hasMessage
import assertk.assertions.isEqualTo
import it.neckar.open.http.Url
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class UrlChainTest {
  private val firstUrl = Url.Absolute("http://localhost:8080")
  private val secondUrl = Url.Absolute("http://localhost:8081")
  private val thirdUrl = Url.Absolute("http://localhost:8082")

  private val chain = UrlChain(urls = listOf(firstUrl, secondUrl, thirdUrl))

  @Test
  fun `fastForEach visits every url in order`() {
    //The order is the fallback order: AdvancedClient tries the first URL first
    val visitedUrls = mutableListOf<Url.Absolute>()

    chain.fastForEach {
      visitedUrls.add(it)
    }

    assertThat(visitedUrls).containsExactly(firstUrl, secondUrl, thirdUrl)
  }

  @Test
  fun `map transforms every url and keeps the order`() {
    assertThat(chain.map { it.value }).containsExactly(
      "http://localhost:8080",
      "http://localhost:8081",
      "http://localhost:8082",
    )
  }

  @Test
  fun `a chain with a single url is valid`() {
    val singleUrlChain = UrlChain(urls = listOf(firstUrl))

    val visitedUrls = mutableListOf<Url.Absolute>()
    singleUrlChain.fastForEach {
      visitedUrls.add(it)
    }

    assertThat(visitedUrls).containsExactly(firstUrl)
  }

  @Test
  fun `duplicate urls are kept - each one is tried`() {
    val chainWithDuplicate = UrlChain(urls = listOf(firstUrl, firstUrl))

    val visitedUrls = mutableListOf<Url.Absolute>()
    chainWithDuplicate.fastForEach {
      visitedUrls.add(it)
    }

    assertThat(visitedUrls).containsExactly(firstUrl, firstUrl)
  }

  @Suppress("RETURN_VALUE_NOT_USED_COERCION") //the constructor never returns - it throws
  @Test
  fun `an empty chain is rejected`() {
    //An empty chain would make AdvancedClient fail without ever having tried a URL
    val exception = assertThrows<IllegalArgumentException> {
      UrlChain(urls = emptyList<Url.Absolute>())
    }

    assertThat(exception).hasMessage("At least one URL must be provided")
  }

  @Test
  fun `chains with the same urls are equal`() {
    assertThat(UrlChain(urls = listOf(firstUrl, secondUrl, thirdUrl))).isEqualTo(chain)
  }
}
