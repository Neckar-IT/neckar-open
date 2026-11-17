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
package it.neckar.ktor.health

import java.util.concurrent.atomic.AtomicBoolean
import kotlin.system.exitProcess
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

/**
 * Ends the process with a status code. Replaceable so a test observes the exit instead of dying from it.
 */
fun interface ProcessExit {
  /**
   * Ends the process with [status]; returns to the caller, the exit happens on another thread.
   */
  fun exit(status: Int)

  companion object {
    /**
     * `System.exit` on a thread of its own: called on a Netty event loop or inside a shutdown hook, it
     * would block the very shutdown it starts.
     */
    val Jvm: ProcessExit = JvmProcessExit
  }
}

private object JvmProcessExit : ProcessExit {
  /**
   * Longer than a Ktor shutdown takes (1 s + 5 s engine grace, 5 s application dispose); a shutdown hook
   * that hangs beyond it must not keep the stalled process alive.
   */
  private val HaltAfter: Duration = 30.seconds

  private val exiting: AtomicBoolean = AtomicBoolean(false)

  override fun exit(status: Int) {
    //Every StallReaction of the JVM may ask; the first exit is the one that counts
    if (exiting.compareAndSet(false, true).not()) {
      return
    }

    Thread({
      Thread.sleep(HaltAfter.inWholeMilliseconds)
      Runtime.getRuntime().halt(status)
    }, "process-exit-halt").apply { isDaemon = true }.start()
    Thread({ exitProcess(status) }, "process-exit").start()
  }
}
