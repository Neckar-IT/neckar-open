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

import io.ktor.client.*
import io.ktor.http.isSuccess
import it.neckar.ktor.client.putJson
import it.neckar.logging.Logger
import it.neckar.logging.LoggerFactory
import it.neckar.open.collections.fastForEach
import it.neckar.open.http.Url

/**
 * Wrapper for the Ktor client that supports fallback URLs and other advanced features
 */
class AdvancedClient(
  val client: HttpClient,
  /**
   * The base URLs to use for requests
   */
  val urlChain: UrlChain<Url.Absolute>,
) {
  /**
   * Puts the given object using JSON to the given URL chain
   */
  @Throws(CouldNotConnectToAnyUrlException::class)
  suspend inline fun <reified T : Any> putJson(relativeUrl: Url.Relative, objectToPut: T) {
    val exceptionList = mutableListOf<Exception>()
    val triedUrls = mutableListOf<Url.Absolute>()

    urlChain.fastForEach { baseUrl ->
      val url = baseUrl + relativeUrl
      triedUrls.add(url)

      try {
        val response = client.putJson(url, objectToPut)
        require(response.status.isSuccess()) {
          "PUT to $url returned ${response.status}"
        }
        return
      } catch (e: Exception) {
        logger.error("Failed to PUT to $url", e)
        exceptionList.add(e)
      }
    }

    throw CouldNotConnectToAnyUrlException("Failed to PUT to any URL in the chain", triedUrls, exceptionList)
  }

  companion object {
    val logger: Logger = LoggerFactory.getLogger("it.neckar.ktor.client.advanced.AdvancedClient")
  }

  class CouldNotConnectToAnyUrlException(
    message: String,
    val urls: List<Url.Absolute>,
    exceptionList: List<Exception>,
  ) : Exception("$message\nUrls: ${urls.joinToString("\n")}", exceptionList.firstOrNull()) {
    init {
      exceptionList.drop(1).fastForEach {
        addSuppressed(it)
      }
    }
  }
}
