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

import java.io.IOException
import java.io.OutputStream

/**
 * Executes a process and notifies execution listeners whenever the process has finished
 *
 */
@Deprecated("No longer used!?")
class Executor(
  private val processBuilder: ProcessBuilder,
  private val targetOut: OutputRedirector.ByteSink,
  private val targetErr: OutputRedirector.ByteSink
) {
  private val executionListeners = ArrayList<ExecutionListener>()

  val isRedirectStreams: Boolean
    @Deprecated("")
    get() = true

  @Deprecated("")
  constructor(processBuilder: ProcessBuilder) : this(processBuilder, OutputStreamByteSink(System.out), OutputStreamByteSink(System.err))

  constructor(processBuilder: ProcessBuilder, targetOut: OutputStream, targetErr: OutputStream) : this(processBuilder, OutputStreamByteSink(targetOut), OutputStreamByteSink(targetErr))

  @Throws(IOException::class, InterruptedException::class)
  fun execute(): Int {
    val process = processBuilder.start()
    val threads = redirectStreams(process)
    notifyExecutionStarted(process)

    val answer = process.waitFor()

    //Wait for the redirecting threads
    for (thread in threads) {
      thread.join()
    }

    notifyExecutionFinished(answer)
    return answer
  }

  /**
   * @param process a Process object.
   * @return the redirecting threads (or an empty array)
   */
  fun redirectStreams(process: Process): Array<Thread> {
    return OutputRedirector.redirect(process, targetOut, targetErr)
  }

  fun executeAsync(): Thread {
    val thread = Thread {
      execute()
    }
    thread.start()
    return thread
  }

  private fun notifyExecutionFinished(answer: Int) {
    for (executionListener in executionListeners) {
      executionListener.executionFinished(answer)
    }
  }

  private fun notifyExecutionStarted(process: Process) {
    for (executionListener in executionListeners) {
      executionListener.executionStarted(process)
    }
  }

  /**
   *
   * addExecutionListener
   *
   * @param executionListener a ExecutionListener object.
   */
  fun addExecutionListener(executionListener: ExecutionListener) {
    this.executionListeners.add(executionListener)
  }

  /**
   *
   * removeExecutionListener
   *
   * @param executionListener a ExecutionListener object.
   */
  fun removeExecutionListener(executionListener: ExecutionListener) {
    this.executionListeners.remove(executionListener)
  }
}
