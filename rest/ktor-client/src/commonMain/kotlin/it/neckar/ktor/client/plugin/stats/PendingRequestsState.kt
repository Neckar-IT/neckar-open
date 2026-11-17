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

import it.neckar.open.unit.other.pct

/**
 * A data class representing the state of pending and completed HTTP requests.
 */
data class PendingRequestsState(
  /**
   * The pending requests (does *not* contain the completed requests)
   */
  val pendingRequests: List<PendingRequestDescriptor> = listOf(),

  /**
   * The completed requests
   */
  val completedRequests: List<CompletedRequestDescriptor> = listOf(),
) {

  /**
   * Returns the total requests count - pending and completed
   */
  val totalRequestsCount: Int
    get() {
      return pendingRequests.size + completedRequests.size
    }

  /**
   * Checks if all the started requests have been completed.
   * Returns [empty] if all requests have been completed.
   */
  fun clearedIfCompleted(): PendingRequestsState {
    if (pendingRequests.isEmpty()) {
      return empty
    }

    return this
  }

  /**
   * Calculates the progress percentage of the completed requests.
   *
   * @return the progress percentage.
   */
  fun calculateProgress(): @pct Double {
    if (totalRequestsCount == 0) {
      return 0.0
    }

    val completedRequestsCount = completedRequests.size

    return completedRequestsCount.toDouble() / totalRequestsCount.toDouble()
  }

  /**
   * Returns a new instance with the additional pending request
   */
  fun withPending(request: PendingRequestDescriptor): PendingRequestsState {
    return this.copy(pendingRequests = pendingRequests + request)
  }

  /**
   * Removes the request from pending - and adds a completed request
   */
  fun withCompleted(completedRequestDescriptor: CompletedRequestDescriptor): PendingRequestsState {
    return this.copy(
      pendingRequests = pendingRequests.filter {
        it.matches(completedRequestDescriptor).not() //remove the corresponding pending request
      },
      completedRequests = completedRequests + completedRequestDescriptor
    )
  }

  fun hasPendingRequests(): Boolean {
    return pendingRequests.isNotEmpty()
  }

  companion object {
    val empty: PendingRequestsState = PendingRequestsState(emptyList(), emptyList())
  }
}



