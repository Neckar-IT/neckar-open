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
package it.neckar.resource.loading

import it.neckar.logging.debug
import it.neckar.open.http.Url
import kotlinx.browser.window
import kotlinx.coroutines.*
import org.khronos.webgl.Int8Array
import org.w3c.fetch.RequestInit
import org.w3c.fetch.Response


/**
 * Loads a resource into a byte array
 */
actual suspend fun loadResource(url: Url): ByteArray {
  return fetchByteArray(url)
}

/**
 * Fetches a resource as byte array from the provided URL
 */
suspend fun fetchByteArray(url: Url): ByteArray {
  val fetch = window.fetch(url)
  val response = fetch.await()

  logger.debug { "response code: ${response.status} \n" }
  logger.debug { "response code text: ${response.statusText} \n" }

  if (response.ok.not()) {
    throw createException(response)
  }

  //response.body is a ReadableStream (not awaitable) - read the bytes via arrayBuffer().
  //On Kotlin/JS ByteArray *is* an Int8Array, therefore the unsafeCast.
  val arrayBuffer = response.arrayBuffer().await()
  return Int8Array(arrayBuffer).unsafeCast<ByteArray>()
}


suspend fun <T> request(url: Url, method: String = "GET", body: dynamic = null): T {
  val response = window.fetch(url.toString(), object : RequestInit {
    override var method: String? = method
    override var body: dynamic = body
    //override var headers: dynamic = json("Accept" to "application/json")
  }).await()
  val responseText = response.text().await()

  logger.debug { "response code: ${response.status} \n" }
  logger.debug { "response code text: ${response.statusText} \n" }
  logger.debug { "response: $response \n" }
  logger.debug { "response text: $responseText \n" }

  return if (response.ok) responseText as T else throw createException(response)
}

/**
 * Thrown when fetching a resource fails (non-ok HTTP status).
 *
 * [responseBody] holds the already awaited response body.
 */
class FetchException(message: String, val status: Short, val responseBody: String) : Exception(message)

/**
 * Creates (but does not throw) a [FetchException] from a [Response].
 *
 * Awaits `response.text()` so the body lands in [FetchException.responseBody] as a String;
 * otherwise the raw `Promise<String>` would leak into consumers.
 */
suspend fun createException(response: Response): FetchException {
  val errorResponse: String = response.text().await()
  return FetchException("Request failed: ${response.status}, ${response.statusText}", response.status, errorResponse)
}


