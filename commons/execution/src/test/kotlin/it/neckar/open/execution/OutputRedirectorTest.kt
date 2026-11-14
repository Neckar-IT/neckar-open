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

import it.neckar.open.test.utils.waitUntilFalse
import kotlinx.coroutines.*
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.Assertions
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.fail
import java.io.BufferedInputStream
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.concurrent.atomic.AtomicBoolean

/**
 */
class OutputRedirectorTest {
  @Test
  fun testDefault() {
    val out = ByteArrayOutputStream()
    val outputRedirector = OutputRedirector(ByteArrayInputStream("asdf".toByteArray(StandardCharsets.UTF_8)), out)
    outputRedirector.run()
    assertEquals("asdf", out.toString())
  }

  @Test
  fun testThreaded() {
    val out = ByteArrayOutputStream()
    val outputRedirector = OutputRedirector(ByteArrayInputStream("asdf".toByteArray(StandardCharsets.UTF_8)), out)
    val thread = Thread(outputRedirector)
    thread.start()
    thread.join()
    assertEquals("asdf", out.toString())
  }

  @Test
  fun testStreams() {
    val stream = ByteArrayInputStream("asdf".toByteArray(StandardCharsets.UTF_8))
    assertEquals(4, stream.available().toLong())
    stream.read()
    assertEquals(3, stream.available().toLong())
    stream.read()
    assertEquals(2, stream.available().toLong())
    stream.read()
    assertEquals(1, stream.available().toLong())
    stream.read()
    assertEquals(0, stream.available().toLong())
    assertEquals(0, stream.available().toLong())
    assertEquals(-1, stream.read().toLong())
  }

  @Test
  fun testStreamsBuffered() {
    val stream: InputStream = BufferedInputStream(ByteArrayInputStream("asdf".toByteArray(StandardCharsets.UTF_8)))
    assertEquals(4, stream.available().toLong())
    stream.read()
    assertEquals(3, stream.available().toLong())
    stream.read()
    assertEquals(2, stream.available().toLong())
    stream.read()
    assertEquals(1, stream.available().toLong())
    stream.read()
    assertEquals(0, stream.available().toLong())
    assertEquals(0, stream.available().toLong())
    assertEquals(-1, stream.read().toLong())
  }

}
