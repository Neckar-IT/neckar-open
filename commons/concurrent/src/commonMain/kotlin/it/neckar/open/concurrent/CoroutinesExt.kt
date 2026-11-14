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
package it.neckar.open.concurrent

/**
 * Extension methods related to coroutines
 */
import kotlinx.coroutines.*
import kotlin.time.Duration
import kotlin.time.Duration.Companion.milliseconds


/**
 * Basic debounce operators based on [the Stack Overflow answer](https://stackoverflow.com/a/55802898/5102726) from [Patrick](https://stackoverflow.com/users/751581/patrick)
 *
 * Usage:
 * ```
 * val onEmailChange: (String) -> Unit = throttleLatest(300L, viewLifecycleOwner.lifecycleScope, viewModel::onEmailChanged)
 * emailTextView.onTextChanged(onEmailChange)
 * ```
 */

/**
 * Constructs a function that processes input data and passes the latest data to [destinationFunction] in the end of every time window with length of [interval].
 */
public fun <T> throttleLatest(
  interval: Duration = 300.milliseconds,
  coroutineScope: CoroutineScope,
  destinationFunction: (T) -> Unit,
): (T) -> Unit {
  var throttleJob: Job? = null
  var latestParam: T
  return { param: T ->
    latestParam = param
    if (throttleJob?.isCompleted != false) {
      throttleJob = coroutineScope.launch {
        delay(interval)
        destinationFunction(latestParam)
      }
    }
  }
}


/**
 * Constructs a function that processes input data and passes it to [destinationFunction] only if there's no new data for at least [wait]
 */
public fun <T> debounce(
  wait: Duration = 300.milliseconds,
  coroutineScope: CoroutineScope,
  destinationFunction: (T) -> Unit,
): (T) -> Unit {
  var debounceJob: Job? = null
  return { param: T ->
    debounceJob?.cancel()
    debounceJob = coroutineScope.launch {
      delay(wait)
      destinationFunction(param)
    }
  }
}


/**
 * Constructs a function that processes input data and passes the first data to [destinationFunction] and skips all new data for the next [skip].
 */
public fun <T> throttleFirst(
  skip: Duration = 300.milliseconds,
  coroutineScope: CoroutineScope,
  destinationFunction: (T) -> Unit,
): (T) -> Unit {
  var throttleJob: Job? = null
  return { param: T ->
    if (throttleJob?.isCompleted != false) {
      throttleJob = coroutineScope.launch {
        destinationFunction(param)
        delay(skip)
      }
    }
  }
}
