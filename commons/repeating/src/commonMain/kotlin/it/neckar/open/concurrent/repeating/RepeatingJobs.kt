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

import it.neckar.open.async.PlatformReadWriteLock
import it.neckar.open.async.read
import it.neckar.open.async.write
import it.neckar.open.dispose.Disposable
import it.neckar.open.unit.other.GuardedBy

/**
 * The [Repeating] jobs a component started, cancelled together when it shuts down.
 *
 * The bookkeeping every scheduler-like component would otherwise write itself: a service that starts
 * one job per task, per schedule or per connection needs exactly this, and getting the shutdown right
 * is easy to get subtly wrong.
 *
 * Thread-safe: [add] runs on the thread that boots the component, [cancelAll] on the one that shuts it
 * down, and nothing guarantees they are the same. A job left running because the shutdown iterated a
 * stale view would keep working after the component was told to stop.
 */
public class RepeatingJobs : Disposable {
  private val lock = PlatformReadWriteLock()

  @GuardedBy("lock")
  private val started: MutableList<Repeating> = mutableListOf()

  /**
   * How many jobs are currently held.
   */
  public val size: Int
    get() = lock.read { started.size }

  /**
   * Adds an already started [job] and returns it, so it reads as one statement:
   *
   * ```
   * jobs.add(Repeating.every(1.minutes) { sweep() }.start(scope))
   * ```
   *
   * Ignorable: most callers hand the job over and never touch it again.
   */
  @IgnorableReturnValue
  public fun add(job: Repeating): Repeating {
    return lock.write {
      started.add(job)
      job
    }
  }

  /**
   * Cancels every held job and forgets it. Idempotent — a second call finds nothing left.
   */
  public fun cancelAll() {
    //Drain instead of iterate-then-clear: two concurrent shutdowns would otherwise both cancel every
    //job, and a job added between the two operations would be forgotten without being cancelled.
    while (true) {
      val job = lock.write { started.removeFirstOrNull() } ?: return
      job.cancel()
    }
  }

  override fun dispose() {
    cancelAll()
  }
}
