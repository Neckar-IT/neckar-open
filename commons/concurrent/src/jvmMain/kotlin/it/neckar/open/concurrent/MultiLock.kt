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

import java.util.concurrent.TimeUnit
import java.util.concurrent.locks.Condition
import java.util.concurrent.locks.Lock

/** Takes every lock in order and releases them in reverse. Two callers with different orders deadlock. */
public class MultiLock(
  locks: Collection<Lock>
) : Lock {
  private val locks = ArrayList<Lock>()

  public constructor(vararg locks: Lock) : this(locks.asList())

  init {
    this.locks.addAll(locks)
  }

  override fun lock() {
    for (lock in locks) {

      lock.lock()
    }
  }

  @Throws(InterruptedException::class)
  override fun lockInterruptibly() {
    throw UnsupportedOperationException("Not supported for a multi lock")
  }

  override fun tryLock(): Boolean {
    throw UnsupportedOperationException("Not supported for a multi lock")
  }

  @Throws(InterruptedException::class)
  override fun tryLock(time: Long, unit: TimeUnit): Boolean {
    throw UnsupportedOperationException("Not supported for a multi lock")
  }

  override fun unlock() {
    for (lock in locks) {
      lock.unlock()
    }
  }

  override fun newCondition(): Condition {
    throw UnsupportedOperationException("Cannot create a condition for a multi lock")
  }
}
