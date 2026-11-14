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
package it.neckar.open.execution

import it.neckar.open.lang.Os
import java.util.Locale
import java.util.concurrent.TimeUnit
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

object Runtimes {

  /**
   * Executes the command and returns the lines as list
   */
  fun execSimpleCommand(command: List<String>): List<String> {
    val process = ProcessBuilder(command).start()
    return process.inputStream.bufferedReader().useLines { it.toList() }
  }

  /**
   * Counts the processes for the given pattern
   */
  fun countProcesses(pattern: Regex): Int {
    val command = when {
      Os.isWindows -> listOf("tasklist", "/NH")
      Os.isLinux -> listOf("ps", "-ef")
      else -> throw UnsupportedOperationException("No implemented for OS ${Os.osName}")
    }

    return execSimpleCommand(command).count {
      it.contains(pattern)
    }
  }

  /**
   * Returns true if at least one process matching the provided pattern is running.
   */
  fun isProcessRunning(pattern: Regex): Boolean {
    return countProcesses(pattern) > 0
  }

  fun Process.formatProcessInfo(): String {
    return toHandle().formatProcessInfo()
  }

  fun ProcessHandle.formatProcessInfo(): String {
    val info = info()
    val user = info.user().orElse("")
    val pid = pid()
    val ppid = parent().orElseGet { null }?.pid()?.toString() ?: "?"
    val startTime = info.startInstant().orElse(null)
    val cpuDuration = info.totalCpuDuration()?.orElse(null)
    val cmdLine = info.commandLine().orElseGet { "" }

    return String.format(Locale.US, "%-8s %-6d %-6s %-25s %-10s %s", user, pid, ppid, startTime ?: "", cpuDuration ?: "", cmdLine)
  }

  /**
   * Destroys the process
   */
  fun destroyProcess(process: Process, shutdownWaitTime: Duration = 10.seconds) {
    //Capture the process info *before* destroying it: ProcessHandle.info() returns mostly empty
    //fields once the process has been destroyed, so it must be read up front for the error message.
    val info = process.toHandle().formatProcessInfo()

    destroyProcess(process.toHandle())

    //Wait for the process to shut down
    if (process.waitFor(shutdownWaitTime.inWholeMilliseconds, TimeUnit.MILLISECONDS)) {
      //Process ended
      return
    }

    process.destroyForcibly()
    if (process.waitFor(shutdownWaitTime.inWholeMilliseconds, TimeUnit.MILLISECONDS)) {
      return
    }

    throw IllegalStateException("Could not stop process $info")
  }

  /**
   * Destroys a process (and its child processes).
   */
  private fun destroyProcess(process: ProcessHandle) {
    process.children().forEach { destroyProcess(it) }
    process.destroy()
  }
}
