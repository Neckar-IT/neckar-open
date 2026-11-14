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

import it.neckar.open.annotations.NonBlocking
import it.neckar.open.annotations.NonUiThread
import it.neckar.open.annotations.UiThread
import java.util.HashMap
import java.util.concurrent.locks.ReentrantReadWriteLock
import javax.annotation.concurrent.GuardedBy
import kotlin.concurrent.write

/**
 * Abstract base class for async execution of scheduled runnables.
 * Only the latest runnable is executed
 *
 */
public abstract class AbstractAsync {
  private val lock = ReentrantReadWriteLock()

  @GuardedBy("lock")
  private val scheduledRunnables = HashMap<Any, Runnable>()

  /**
   * Asynchronously calls only the last added runnable.
   */
  @NonUiThread
  @UiThread
  public fun last(runnable: Runnable) {
    last(runnable.javaClass, runnable)
  }

  /**
   * Asynchronously calls only the last runnable.
   */
  @NonUiThread
  @UiThread
  public fun last(key: Any, runnable: Runnable) {
    lock.write {
      if (scheduledRunnables.put(key, runnable) != null) {
        //There is another job scheduled, so we do not have to reschedule it
        return
      }
    }

    //There has no other event been scheduled
    runInTargetThread(Runnable { getAndRemove(key).run() })
  }

  /**
   * Returns the runnable for the given key
   */
  @UiThread
  private fun getAndRemove(key: Any): Runnable {
    lock.write {
      return scheduledRunnables.remove(key) ?: throw IllegalStateException("No job found for <$key>")
    }
  }

  /**
   * Run in target thread
   */
  @NonBlocking
  protected abstract fun runInTargetThread(runnable: Runnable)
}
