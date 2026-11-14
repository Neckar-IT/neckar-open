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

import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * One coroutine [Mutex] per key: callers with the same key run one after the other, callers with different keys run at the same
 * time. A key holds an entry only while a caller holds or waits for its lock, so the entries do not grow with the number of keys seen.
 */
public class KeyedMutex<K : Any> {
  private val entriesLock: Mutex = Mutex()
  private val entries: MutableMap<K, Entry> = mutableMapOf()

  /**
   * Runs [block] while holding the lock of [key] and returns its result.
   */
  public suspend fun <T> withLock(key: K, block: suspend () -> T): T {
    val entry: Entry = entriesLock.withLock { entries.getOrPut(key) { Entry() }.also { it.users++ } }
    try {
      return entry.mutex.withLock { block() }
    } finally {
      withContext(NonCancellable) {
        entriesLock.withLock {
          entry.users--
          if (entry.users == 0) {
            entries.remove(key)
          }
        }
      }
    }
  }

  /**
   * The number of keys a caller holds or waits for.
   */
  public suspend fun activeKeyCount(): Int = entriesLock.withLock { entries.size }

  private class Entry {
    val mutex: Mutex = Mutex()

    /** Callers holding or waiting for [mutex]. */
    var users: Int = 0
  }
}
