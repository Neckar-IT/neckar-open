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

import java.io.InputStream
import java.io.OutputStream

/**
 * Redirects a stream into another
 *
 */
class OutputRedirector(
  private val inputStream: InputStream,
  private val out: ByteSink
) : Runnable {

  /**
   * Not evaluated: [run] blocks in [InputStream.read] instead of polling.
   */
  var waitingPeriod: Int = 100

  var bufferSize: Int = 1024

  /**
   * Redirects [inputStream] to [out], wrapped in an [OutputStreamByteSink].
   */
  constructor(inputStream: InputStream, out: OutputStream) : this(inputStream, OutputStreamByteSink(out))

  override fun run() {
    val buffer = ByteArray(bufferSize)

    while (!Thread.interrupted()) {
      val readBytes: Int = inputStream.read(buffer)
      if (readBytes < 0) {
        return
      }

      out.take(buffer, readBytes)
    }
  }

  /**
   * Accepts bytes
   */
  @FunctionalInterface
  interface ByteSink {
    /**
     * Accepts the first [length] bytes of [bytes]
     */
    fun take(bytes: ByteArray, length: Int)
  }

  companion object {
    /**
     * Redirects the standard output of [process] to [System.out] and its error output to [System.err].
     * Returns the two redirecting threads.
     */
    fun redirect(process: Process): Array<Thread> {
      val targetOut = System.out
      val targetErr = System.err
      return redirect(process, targetOut, targetErr)
    }

    /**
     * Redirects the standard output of [process] to [targetOut] and its error output to [targetErr].
     * Returns the two redirecting threads.
     */
    fun redirect(process: Process, targetOut: OutputStream, targetErr: OutputStream): Array<Thread> {
      return redirect(process, OutputStreamByteSink(targetOut), OutputStreamByteSink(targetErr))
    }

    fun redirect(process: Process, targetOut: ByteSink, targetErr: ByteSink): Array<Thread> {
      val inputThread = Thread(OutputRedirector(process.inputStream, targetOut))
      inputThread.start()
      val outputThread = Thread(OutputRedirector(process.errorStream, targetErr))
      outputThread.start()

      return arrayOf(inputThread, outputThread)
    }
  }
}
