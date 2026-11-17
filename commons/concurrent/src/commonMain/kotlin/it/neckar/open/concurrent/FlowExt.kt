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

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlin.time.Duration

/**
 * Returns the first element.
 * Throws a [TimeoutCancellationException] if the first element is not emitted within [timeout].
 */
public suspend fun <T> Flow<T>.firstWithTimeout(timeout: Duration): T {
  return withTimeout(timeout) {
    first()
  }
}

/**
 * If the flow has:
 * * no elements: returns null
 * * exactly element: returns the single element
 * * contains more than one element: throws an Exception
 */
public suspend fun <T> Flow<T>.noMoreThanOne(): T? {
  var result: T? = null
  var count = 0

  collect { value ->
    check(count == 0) {
      "Flow contains an element already!"
    }
    result = value
    count++
  }

  return result
}

/**
 * Converts the flow to a list - but ensures that the list does not exceed a "meaningful" limit.
 * Avoid possible out of memory problems, when converting very large flows.
 *
 * Throws an exception if the limit is exceeded.
 */
public suspend fun <T> Flow<T>.toListLimited(maxSize: Int = 100): List<T> {
  val list = mutableListOf<T>()

  collect { value ->
    check(list.size < maxSize) {
      "Flow contains more than $maxSize elements!"
    }
    list.add(value)
  }

  return list
}
