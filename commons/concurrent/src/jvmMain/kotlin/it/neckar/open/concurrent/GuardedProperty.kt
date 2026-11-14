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

import java.util.concurrent.locks.ReentrantReadWriteLock
import javax.annotation.concurrent.GuardedBy
import kotlin.concurrent.read
import kotlin.concurrent.write
import kotlin.reflect.KProperty

/**
 * Helper class that is guarded by a lock.
 * Contains a mutable property
 */
public class GuardedProperty<T>(
  /**
   * The current value
   */
  @GuardedBy("lock")
  private var value: T,

  public val lock: ReentrantReadWriteLock = ReentrantReadWriteLock(),
) {

  public inline fun <Y> read(action: GuardedProperty<T>.() -> Y): Y = lock.read { this.action() }

  public inline fun <Y> write(action: GuardedProperty<T>.() -> Y): Y = lock.write { this.action() }

  /**
   * Returns the current property
   */
  public fun get(): T {
    read {
      return value
    }
  }

  /**
   * Set the new property
   */
  public fun set(newValue: T) {
    write {
      value = newValue
    }
  }

  public operator fun getValue(thisRef: Any?, property: KProperty<*>): T {
    return get()
  }

  public operator fun setValue(thisRef: Any?, property: KProperty<*>, value: T) {
    set(value)
  }
}
