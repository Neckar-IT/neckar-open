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
package it.neckar.open.container.cli

import kotlinx.io.Source
import kotlinx.io.buffered
import kotlinx.io.writeString
import platform.posix.STDERR_FILENO
import platform.posix.STDIN_FILENO
import platform.posix.STDOUT_FILENO
import kotlin.system.exitProcess

fun main(args: Array<String>) {
  exitProcess(runInvocation(args.toList()))
}

/** Runs one call of `nit-container` on stdin and stdout and returns its exit code. */
private fun runInvocation(args: List<String>): Int {
  val stdout = FileDescriptorSink(STDOUT_FILENO).buffered()
  val invocation = try {
    parseInvocation(args)
  } catch (e: UsageException) {
    printError("${e.message}\n\n$Usage")
    return 2
  }

  return try {
    when (invocation) {
      Invocation.Help -> stdout.use { it.writeString(Usage) }
      is Invocation.Write -> writeContainer(invocation, stdout)
      Invocation.ListEntries -> stdout.use { listContainer(stdin(), it) }
      is Invocation.Extract -> stdout.use { extractEntry(invocation, stdin(), it) }
    }
    0
  } catch (e: Exception) {
    printError(e.message ?: e.toString())
    1
  }
}

private fun stdin(): Source = FileDescriptorSource(STDIN_FILENO).buffered()

private fun printError(message: String) {
  FileDescriptorSink(STDERR_FILENO).buffered().use { it.writeString("nit-container: $message\n") }
}
