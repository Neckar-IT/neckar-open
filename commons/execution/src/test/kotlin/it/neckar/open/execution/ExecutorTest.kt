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
@file:Suppress("RETURN_VALUE_NOT_USED")

package it.neckar.open.execution

import com.google.common.io.ByteStreams
import io.mockk.confirmVerified
import io.mockk.mockk
import io.mockk.verifyOrder
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.io.ByteArrayOutputStream
import java.nio.charset.StandardCharsets
import assertk.*
import assertk.assertions.*
import it.neckar.open.test.utils.SlowTest


/**
 *
 */
@SlowTest
class ExecutorTest {
  private lateinit var targetOut: ByteArrayOutputStream
  private lateinit var targetErr: ByteArrayOutputStream

  @BeforeEach
  fun setUp() {
    targetOut = ByteArrayOutputStream()
    targetErr = ByteArrayOutputStream()
  }

  @Test
  fun testIt() {
    val executor = Executor(ProcessBuilder("java", "-version"), targetOut, targetErr)

    val listener = mockk<ExecutionListener>(relaxed = true)
    executor.addExecutionListener(listener)
    executor.execute()

    verifyOrder {
      listener.executionStarted(any())
      listener.executionFinished(0)
    }

    confirmVerified(listener)
  }

  @Test
  fun testAsync() {
    val executor = Executor(ProcessBuilder("java", "-version"), targetOut, targetErr)
    executor.executeAsync().join()
  }

  @Test
  fun testOutputRedirector() {
    val errorOut = ByteArrayOutputStream()
    val out = ByteArrayOutputStream()
    val executor = Executor(ProcessBuilder("java", "-version"), out, errorOut)
    executor.execute()
    assertEquals("", out.toString())
    assertTrue(errorOut.toString().contains("version"), errorOut.toString())
  }

  @Test
  fun testOutput() {
    val process = ProcessBuilder("java", "-version").start()
    assertEquals("", String(ByteStreams.toByteArray(process.inputStream), StandardCharsets.UTF_8))
    assertThat(String(ByteStreams.toByteArray(process.errorStream), StandardCharsets.UTF_8)).contains("version")
  }

  @Test
  fun testStreams() {
    val process = ProcessBuilder("java", "-version").start()
    val inStream = process.errorStream
    assertEquals(0, inStream.available().toLong())
    assertEquals(0, process.waitFor().toLong())
    val available = inStream.available()
    assertTrue(available > 100)
  }
}
