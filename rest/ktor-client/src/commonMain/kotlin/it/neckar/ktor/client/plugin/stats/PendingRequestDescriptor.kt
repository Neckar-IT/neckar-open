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

import it.neckar.open.unit.si.ms
import io.ktor.http.*


sealed interface RequestDescriptor {
  /**
   * The URL for the request
   */
  val url: Url

  /**
   * The HTTP method
   */
  val method: HttpMethod

  /**
   * When the request started
   */
  val startedAt: @ms Double
}

/**
 * A data class representing an intercepted HTTP request that is currently active.
 *
 * @property url the request URL.
 * @property method the request method.
 */
data class PendingRequestDescriptor(
  /**
   * The URL for the request
   */
  override val url: Url,
  /**
   * The HTTP method
   */
  override val method: HttpMethod,
  /**
   * When the request started
   */
  override val startedAt: @ms Double,
) : RequestDescriptor {
  fun receivedAt(finishedAt: Double): CompletedRequestDescriptor {
    return CompletedRequestDescriptor(url, method, startedAt, finishedAt)
  }

  /**
   * Returns true if this matches the provided request (by comparing all properties)
   */
  fun matches(request: RequestDescriptor): Boolean {
    return this.url == request.url && this.method == request.method && this.startedAt == request.startedAt
  }
}

/**
 * Representing a completed HTTP request
 */
data class CompletedRequestDescriptor(
  /**
   * The URL for the request
   */
  override val url: Url,
  /**
   * The HTTP method
   */
  override val method: HttpMethod,
  /**
   * When the request started
   */
  override val startedAt: @ms Double,

  /**
   * When the request has been completed
   */
  val completedAt: @ms Double,

  ) : RequestDescriptor

