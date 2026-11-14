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
package it.neckar.commons.logback

import assertk.*
import assertk.assertions.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.PrintStream

class LogbackConfigurerTest {
  private lateinit var originalErr: PrintStream
  private lateinit var capturedErr: ByteArrayOutputStream

  @BeforeEach
  fun setUp() {
    capturedErr = ByteArrayOutputStream()
    originalErr = System.err
    System.setErr(PrintStream(capturedErr, true, Charsets.UTF_8))
  }

  @AfterEach
  fun tearDown() {
    System.setErr(originalErr)
    LogbackConfigurer.clearExistingAppenders()
    org.slf4j.LoggerFactoryFriend.reset()
  }

  @Test
  fun `a file appender that cannot open its log file reports the failure`(@TempDir tempDir: File) {
    //A regular file cannot become the parent directory of the log file - logback fails to open it
    val regularFile = File(tempDir, "not-a-directory").also {
      it.writeText("This is a regular file, not a directory")
    }
    val blockedLogFile = File(regularFile, "application.log")

    LogbackConfigurer.addFileAppender(blockedLogFile)

    capturedErr.toString(Charsets.UTF_8).let { reported ->
      assertThat(reported).contains(LogbackConfigurer.FileAppenderName)
      assertThat(reported).contains(blockedLogFile.absolutePath)
      assertThat(reported).contains("failed to start")
      //the reason logback recorded - not just the fact that something went wrong
      assertThat(reported).contains("openFile")
      //...including the exception it caught. The concrete type is filesystem dependent, the package is not.
      assertThat(reported).contains("java.io.")
    }
  }

  @Test
  fun `an appender that started reports nothing`() {
    LogbackConfigurer.addConsoleAppender(ConsoleTarget.Stdout)

    assertThat(capturedErr.toString(Charsets.UTF_8)).isEmpty()
  }
}
