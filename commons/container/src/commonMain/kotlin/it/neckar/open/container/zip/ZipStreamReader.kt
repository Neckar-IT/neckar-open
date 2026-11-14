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
package it.neckar.open.container.zip

import it.neckar.open.container.InvalidContainerException
import kotlinx.io.Buffer
import kotlinx.io.RawSource
import kotlinx.io.Source
import kotlinx.io.buffered
import no.synth.kmpzip.deflate.DeflateException
import no.synth.kmpzip.kotlinx.SourceInputStream
import no.synth.kmpzip.zip.ZipException
import no.synth.kmpzip.zip.ZipInputStream

/**
 * Reads the entries of a ZIP file in stream order with kmp-zip.
 * Reading an entry to its end checks its CRC-32 and sizes; the next entry drains the rest of the current one first.
 * After the last entry kmp-zip reads the central directory and checks name, offset, CRC-32 and sizes of every entry against it.
 */
internal class ZipStreamReader(private val zipIn: ZipInputStream) : AutoCloseable {

  private var current: EntrySource? = null
  private var ended = false

  /** Set when a read failed; the stream position is unknown, so no further entry follows. */
  private var failed = false
  private var closed = false

  /** The next entry, or null at the central directory. */
  fun nextEntry(): ZipEntry? {
    check(closed.not()) { "The container reader is closed" }
    check(failed.not()) { "An earlier read of the container failed" }
    val previous = current
    previous?.drain()
    previous?.supersede()
    current = null
    if (ended) return null

    val zipEntry = readingStream("the next entry header") {
      zipIn.nextEntry.also {
        if (it != null) verifyName(it.name)
      }
    }
    if (zipEntry == null) {
      ended = true
      return null
    }
    val entrySource = EntrySource(zipEntry.name)
    current = entrySource
    return ZipEntry(zipEntry.name, entrySource.buffered())
  }

  /** kmp-zip decodes an invalid UTF-8 sequence to U+FFFD instead of failing. */
  private fun verifyName(name: String) {
    if (name.contains('\uFFFD')) throw InvalidContainerException("Entry name <$name> is no valid UTF-8")
  }

  /** Runs [block]; any failure marks the reader failed, and a ZIP or deflate failure of kmp-zip becomes an [InvalidContainerException]. */
  private fun <T> readingStream(what: String, block: () -> T): T {
    try {
      return block()
    } catch (e: ZipException) {
      failed = true
      throw InvalidContainerException("Container cannot be read at $what: ${e.message}", e)
    } catch (e: DeflateException) {
      failed = true
      throw InvalidContainerException("Container cannot be read at $what: ${e.message}", e)
    } catch (e: Throwable) {
      failed = true
      throw e
    }
  }

  /** Releases the current entry and the inflater of kmp-zip, and closes the stream below it. */
  override fun close() {
    closed = true
    current?.release()
    current = null
    zipIn.close()
  }

  /** The content of one entry; kmp-zip checks CRC-32 and sizes when it reaches the end. */
  private inner class EntrySource(private val name: String) : RawSource {
    private val bytes = ByteArray(ReadBufferSize)
    private var done = false

    /** Set by [release]: the reader is closed, so the content cannot be read any more. */
    private var released = false

    /** Set by [supersede]: the reader moved on to the next entry. */
    private var superseded = false

    override fun readAtMostTo(sink: Buffer, byteCount: Long): Long {
      checkReadable()
      if (done) return -1
      if (byteCount == 0L) return 0
      return readingStream("entry <$name>") {
        val read = zipIn.read(bytes, 0, minOf(byteCount, bytes.size.toLong()).toInt())
        if (read == -1) {
          done = true
          -1L
        } else {
          sink.write(bytes, 0, read)
          read.toLong()
        }
      }
    }

    /** Reads the rest of the content, which lets kmp-zip check it. */
    fun drain() {
      val scratch = Buffer()
      while (readAtMostTo(scratch, bytes.size.toLong()) != -1L) {
        scratch.clear()
      }
    }

    private fun checkReadable() {
      check(released.not()) { "The container reader is closed" }
      check(failed.not()) { "An earlier read of the container failed" }
      check(superseded.not()) { "Entry <$name> was followed by the next entry" }
    }

    fun supersede() {
      superseded = true
    }

    fun release() {
      released = true
    }

    override fun close() {
      // The ZIP stream stays open; the reader drains the rest of the entry.
    }
  }

  companion object {
    private const val ReadBufferSize: Int = 64 * 1024

    /** Reads the ZIP file from [source]; closing the reader closes [source]. */
    fun from(source: Source): ZipStreamReader = ZipStreamReader(ZipInputStream(SourceInputStream(source), verifyCentralDirectory = true))
  }
}
