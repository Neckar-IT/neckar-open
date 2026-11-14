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
package it.neckar.ktor.jwt.jwks

import it.neckar.open.http.Url
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds
import kotlin.time.toJavaDuration

/**
 * Fetches the JWKS document of one realm.
 *
 * Always the whole document — a source that could deliver a single key would let an attacker who
 * floods unknown `kid`s consume the refresh without the rotated key ever arriving.
 */
fun interface JwksSource {
  /**
   * Fetches the current document. Throws on any transport or parse failure; the caller keeps what it
   * holds and logs.
   */
  suspend fun fetch(): JwksDocument
}

/**
 * [JwksSource] over the realm's `openid-connect/certs` endpoint.
 *
 * Uses the JDK HTTP client rather than a Ktor client: this is one plain `GET` of a static JSON
 * document, and a service that authenticates does not otherwise need an HTTP client on its classpath.
 */
class HttpJwksSource(
  private val jwksUrl: Url.Absolute,
  private val requestTimeout: Duration = DefaultRequestTimeout,
  private val httpClient: HttpClient = defaultHttpClient(),
) : JwksSource {

  override suspend fun fetch(): JwksDocument {
    val request = HttpRequest.newBuilder(URI(jwksUrl.value))
      .GET()
      .header("Accept", "application/json")
      .timeout(requestTimeout.toJavaDuration())
      .build()

    val response = withContext(Dispatchers.IO) {
      httpClient.send(request, HttpResponse.BodyHandlers.ofString())
    }

    if (response.statusCode() != 200) {
      throw JwksFetchFailedException("$jwksUrl answered ${response.statusCode()}")
    }

    return JwksDocument.parse(response.body())
  }

  companion object {
    val DefaultRequestTimeout: Duration = 10.seconds
    val DefaultConnectTimeout: Duration = 5.seconds

    private fun defaultHttpClient(): HttpClient = HttpClient.newBuilder()
      .connectTimeout(DefaultConnectTimeout.toJavaDuration())
      .followRedirects(HttpClient.Redirect.NORMAL)
      .build()
  }
}

/**
 * A JWKS fetch that reached the endpoint and got something other than a document back.
 */
class JwksFetchFailedException(message: String) : RuntimeException(message)
