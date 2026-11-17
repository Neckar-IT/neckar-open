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
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.convert
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned
import kotlinx.io.Buffer
import kotlinx.io.IOException
import kotlinx.io.RawSink
import kotlinx.io.RawSource
import platform.posix.EINTR
import platform.posix.errno
import platform.posix.read
import platform.posix.strerror
import platform.posix.write

/** The most bytes one `read` or `write` call moves. */
private const val ChunkBytes: Int = 64 * 1024

/**
 * Writes to the open file descriptor [fd], such as stdout; [close] leaves [fd] open.
 * Writes to [fd] directly: opening `/dev/stdout` would truncate a file that `>>` appends to.
 */
@OptIn(ExperimentalForeignApi::class)
internal class FileDescriptorSink(private val fd: Int) : RawSink {
  private val chunk = ByteArray(ChunkBytes)

  override fun write(source: Buffer, byteCount: Long) {
    require(byteCount in 0..source.size) { "byteCount <$byteCount> is outside 0..${source.size}" }
    var remaining = byteCount
    while (remaining > 0) {
      val count = source.readAtMostTo(chunk, 0, minOf(remaining, chunk.size.toLong()).toInt())
      writeFully(count)
      remaining -= count
    }
  }

  private fun writeFully(count: Int) {
    var offset = 0
    chunk.usePinned { pinned ->
      while (offset < count) {
        val written = write(fd, pinned.addressOf(offset), (count - offset).convert())
        if (written < 0) {
          if (errno == EINTR) continue
          throw IOException("Writing to file descriptor $fd failed: ${strerror(errno)?.toKString()}")
        }
        offset += written.toInt()
      }
    }
  }

  override fun flush() {
  }

  override fun close() {
  }
}

/** Reads from the open file descriptor [fd], such as stdin; [close] leaves [fd] open. */
@OptIn(ExperimentalForeignApi::class)
internal class FileDescriptorSource(private val fd: Int) : RawSource {
  private val chunk = ByteArray(ChunkBytes)

  override fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
    require(byteCount >= 0) { "byteCount <$byteCount> is negative" }
    if (byteCount == 0L) return 0
    val requested = minOf(byteCount, chunk.size.toLong()).toInt()
    while (true) {
      val count = chunk.usePinned { read(fd, it.addressOf(0), requested.convert()) }
      if (count < 0) {
        if (errno == EINTR) continue
        throw IOException("Reading from file descriptor $fd failed: ${strerror(errno)?.toKString()}")
      }
      if (count == 0L) return -1
      sink.write(chunk, 0, count.toInt())
      return count
    }
  }

  override fun close() {
  }
}
