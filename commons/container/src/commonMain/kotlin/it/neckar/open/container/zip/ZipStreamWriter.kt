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

import kotlinx.io.Buffer
import kotlinx.io.RawSink
import kotlinx.io.Sink
import kotlinx.io.buffered
import no.synth.kmpzip.zip.ZipEntry
import no.synth.kmpzip.zip.ZipOutputStream
import no.synth.kmpzip.kotlinx.ZipOutputStream as zipOutputStreamOf

/**
 * Writes a ZIP file to a stream that cannot seek with kmp-zip: every entry is deflated, sizes and CRC-32 follow in a
 * data descriptor, the central directory closes the file.
 */
internal class ZipStreamWriter(
  /** The sink below [zipOut]; [close] closes it and the sink below it. */
  private val output: DiscardableSink,
  private val zipOut: ZipOutputStream,
) : AutoCloseable {

  private var openEntry: EntrySink? = null

  /** Set while the bytes of an entry are being written, from its local file header to its data descriptor. */
  private var entryStarted = false

  /** Set when an entry ended without a data descriptor: neither a further entry nor a central directory may follow. */
  var aborted: Boolean = false
    private set

  private var finished = false
  private var closed = false

  /** Starts the entry [name]; closing the returned sink ends it. */
  fun startEntry(name: String, deflateLevel: Int): Sink {
    checkWritable()
    check(openEntry == null) { "The previous entry is still open" }
    require(name.encodeToByteArray().size <= MaxEntryNameBytes) { "Entry name <$name> exceeds $MaxEntryNameBytes bytes" }

    entryStarted = true
    zipOut.setLevel(deflateLevel)
    zipOut.putNextEntry(ZipEntry(name))
    val entrySink = EntrySink()
    openEntry = entrySink
    return entrySink.buffered()
  }

  /** Releases the open entry without its data descriptor; once its bytes have started, the file stays without central directory. */
  fun abortEntry() {
    openEntry = null
    if (entryStarted) aborted = true
  }

  /** Writes the central directory. */
  fun finish() {
    checkWritable()
    check(openEntry == null) { "The last entry is still open" }
    zipOut.finish()
    zipOut.flush()
    finished = true
  }

  /** Releases the deflater of kmp-zip and closes the sink; without [finish], what kmp-zip writes on close goes nowhere. */
  override fun close() {
    if (closed) return
    closed = true
    if (finished.not()) output.discard = true
    zipOut.close()
  }

  private fun checkWritable() {
    check(closed.not()) { "The ZIP writer is closed" }
    check(finished.not()) { "The central directory is written" }
    check(aborted.not()) { "An entry was aborted" }
  }

  /** Passes the content of one entry to [zipOut]; [close] writes its data descriptor. */
  private inner class EntrySink : RawSink {
    private val bytes = ByteArray(WriteBufferSize)
    private var closed = false

    /** Set when a write failed: CRC and sizes no longer match the written bytes. */
    private var writeFailed = false

    override fun write(source: Buffer, byteCount: Long) {
      check(closed.not()) { "Entry is closed" }
      try {
        var remaining = byteCount
        while (remaining > 0) {
          val read = source.readAtMostTo(bytes, 0, minOf(remaining, bytes.size.toLong()).toInt())
          zipOut.write(bytes, 0, read)
          remaining -= read
        }
      } catch (e: Throwable) {
        writeFailed = true
        throw e
      }
    }

    override fun flush() {
      zipOut.flush()
    }

    /** Writes the data descriptor; after a failed write it leaves the entry unrecorded and the file aborted. */
    override fun close() {
      if (closed) return
      closed = true
      try {
        check(writeFailed.not()) { "A write of the entry failed" }
        zipOut.closeEntry()
      } catch (e: Throwable) {
        aborted = true
        throw e
      } finally {
        openEntry = null
        entryStarted = false
      }
    }
  }

  companion object {
    /** The ZIP format stores the length of an entry name in 16 bits. */
    const val MaxEntryNameBytes: Int = 0xFFFF

    private const val WriteBufferSize: Int = 64 * 1024

    /** Writes the ZIP file to [sink]; closing the writer closes [sink]. */
    fun to(sink: Sink): ZipStreamWriter {
      val output = DiscardableSink(sink)
      return ZipStreamWriter(output, zipOutputStreamOf(output.buffered()))
    }
  }
}
