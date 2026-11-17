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

import it.neckar.open.async.PlatformReadWriteLock
import it.neckar.open.async.read
import it.neckar.open.async.write
import kotlinx.coroutines.*
import kotlin.coroutines.CoroutineContext

/**
 * Collects exceptions that coroutines throw.
 *
 * Under a lock: `handleException` runs on whichever thread the failing coroutine ran on, so two
 * coroutines failing at once raced on the list — a concurrent add can lose an element, and a
 * concurrent snapshot can fail with a `ConcurrentModificationException`.
 */
public class CollectingCoroutineExceptionHandler : CoroutineExceptionHandler {
  override val key: CoroutineExceptionHandler.Key = CoroutineExceptionHandler.Key

  private val lock = PlatformReadWriteLock()

  private val exceptions = mutableListOf<Throwable>()

  override fun handleException(context: CoroutineContext, exception: Throwable) {
    lock.write {
      exceptions.add(exception)
    }
  }

  /**
   * Returns a snapshot of the collected exceptions. A copy, so it neither exposes the mutable
   * internal list nor changes when further exceptions are collected afterwards.
   */
  public fun getExceptions(): List<Throwable> = lock.read { exceptions.toList() }
}
