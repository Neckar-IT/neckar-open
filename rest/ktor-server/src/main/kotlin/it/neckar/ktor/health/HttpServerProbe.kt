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
package it.neckar.ktor.health

import java.net.URI
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.util.concurrent.ExecutionException
import java.util.concurrent.TimeUnit
import java.util.concurrent.TimeoutException
import kotlin.time.Duration
import kotlin.time.toJavaDuration

/**
 * A `GET` with [timeout] for connect and response; any status is an answer.
 */
class HttpServerProbe(
  /**
   * How long one probe waits for the connection and for the headers; the whole exchange ends after twice
   * this.
   */
  private val timeout: Duration,
) : ServerProbe {
  private val client: HttpClient = HttpClient.newBuilder().connectTimeout(timeout.toJavaDuration()).build()

  override fun answers(url: URI): Boolean {
    //The request timeout covers the headers only; the bound on the whole exchange covers a stalled body
    val exchange = client.sendAsync(HttpRequest.newBuilder(url).timeout(timeout.toJavaDuration()).GET().build(), HttpResponse.BodyHandlers.discarding())
    return try {
      exchange.get(timeout.inWholeMilliseconds * 2, TimeUnit.MILLISECONDS)
      true
    } catch (_: ExecutionException) {
      //Refused, reset or timed out: the silence the watchdog measures
      false
    } catch (_: TimeoutException) {
      exchange.cancel(true)
      false
    }
  }

  override fun close() {
    client.shutdownNow()
  }
}
