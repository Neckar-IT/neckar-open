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
package it.neckar.open.concurrent.repeating

import kotlinx.coroutines.delay
import kotlin.coroutines.cancellation.CancellationException
import kotlin.time.Duration

/**
 * Runs [block] until it returns, at most [maxAttempts] times, and returns its result.
 *
 * After a failed attempt `n` the call waits `backoff.delayAfter(n)` before attempt `n + 1`; [onFailedAttempt] learns of
 * the failure right before that pause. A [CancellationException] is rethrown at once. An exception [retryOn] rejects and
 * the exception of the last attempt are thrown without a pause and without [onFailedAttempt].
 *
 * ```
 * val status = retrying(maxAttempts = 5, backoff = ErrorBackoff.exponential(2.seconds, 16.seconds), retryOn = { it is IOException }) {
 *   client.get(url)
 * }
 * ```
 *
 * @param pause waits the given duration; tests replace it to record the pauses instead of waiting
 * @param block receives the number of the attempt, counting from 1
 */
public suspend fun <T> retrying(
  maxAttempts: Int,
  backoff: ErrorBackoff,
  retryOn: (Exception) -> Boolean = { true },
  onFailedAttempt: (FailedAttempt) -> Unit = {},
  pause: suspend (Duration) -> Unit = { delay(it) },
  block: suspend (attempt: Int) -> T,
): T {
  require(maxAttempts >= 1) { "maxAttempts must be at least 1 but was $maxAttempts" }

  var attempt = 1
  while (true) {
    try {
      return block(attempt)
    } catch (e: CancellationException) {
      throw e
    } catch (e: Exception) {
      if (attempt == maxAttempts || retryOn(e).not()) {
        throw e
      }
      val pauseAfter = backoff.delayAfter(attempt)
      onFailedAttempt(FailedAttempt(attempt = attempt, maxAttempts = maxAttempts, exception = e, pause = pauseAfter))
      pause(pauseAfter)
      attempt++
    }
  }
}

/**
 * Attempt [attempt] of [maxAttempts] failed with [exception]; [retrying] waits [pause] before the next one.
 */
public data class FailedAttempt(val attempt: Int, val maxAttempts: Int, val exception: Exception, val pause: Duration)
