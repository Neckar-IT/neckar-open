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

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString
import kotlinx.io.IOException
import kotlinx.io.Sink
import platform.posix.errno
import platform.posix.fileno
import platform.posix.pclose
import platform.posix.popen
import platform.posix.strerror

/**
 * Runs [command] through `/bin/bash -o pipefail -c`, since the `dash` of the hosts knows no `pipefail`, and copies its stdout to [sink].
 * Throws on a copy failure, an exit code other than 0 or a signal; no message quotes [command], which may hold credentials.
 */
@OptIn(ExperimentalForeignApi::class)
internal fun copyCommandOutput(command: String, sink: Sink) {
  val pipe = popen("exec /bin/bash -o pipefail -c ${shellQuote(command)}", "r") ?: throw IOException("Creating the pipe or process of the command failed: ${strerror(errno)?.toKString()}")
  // Reading the descriptor bypasses the stdio buffer of the pipe, which nothing else reads.
  val copyFailure = runCatching { sink.transferFrom(FileDescriptorSource(fileno(pipe))) }.exceptionOrNull()

  // pclose closes the pipe before it waits, so a command still writing after a copy failure ends.
  val status = pclose(pipe)
  copyFailure?.let { throw it }
  if (status == -1) throw IOException("Waiting for the command failed: ${strerror(errno)?.toKString()}")
  val signal = status and 0x7F
  if (signal != 0) throw IOException("Command was ended by signal $signal")
  val exitCode = (status shr 8) and 0xFF
  if (exitCode != 0) throw IOException("Command exited with code $exitCode")
}

/** Wraps [value] in single quotes for `/bin/sh`. */
private fun shellQuote(value: String): String = "'" + value.replace("'", "'\\''") + "'"
