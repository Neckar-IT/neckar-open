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
package it.neckar.logging

import assertk.*
import assertk.assertions.*
import it.neckar.commons.logback.LogbackConfigurer
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.slf4j.Logger
import org.slf4j.event.Level
import java.io.ByteArrayOutputStream

class LoggerJvmTest {
  lateinit var out: ByteArrayOutputStream

  @BeforeEach
  fun setUp() {
    out = ByteArrayOutputStream()
    LogbackConfigurer.configureLoggingToStreamOnly(out, Level.DEBUG)
  }

  @AfterEach
  fun tearDown() {
    org.slf4j.LoggerFactoryFriend.reset()
  }

  val logger: Logger by LoggerDelegate()

  @Test
  fun testTrace() {
    assertThat(logger.isTraceEnabled).isFalse()
    logger.trace{ fail("Must not be called") }
  }

  @Test
  fun testName() {
    assertThat(logger.name).isEqualTo("it.neckar.logging.LoggerJvmTest")
  }

  @Test
  fun testLoggerLevel() {
    assertThat(LogbackConfigurer.getLoggerLevel(logger)).isEqualTo(Level.DEBUG) //the default level
    assertThat(logger.isTraceEnabled).isFalse()
    assertThat(logger.isDebugEnabled).isTrue()
  }

  @Test
  fun testInstances() {
    assertThat(LoggerFactory.getLogger(LoggerName("A"))).isSameAs(LoggerFactory.getLogger(LoggerName("A")))
  }

  @Test
  fun testExtensionMethods() {
    logger.debug { "Only if enabled!" }
    assertThat(out.toString()).contains("Only if enabled!")

    out.reset()
    LogbackConfigurer.setLoggerLevel(logger, Level.INFO)
    logger.debug { fail("Must not be called") }
    logger.info { "info message" }

    assertThat(out.toString()).contains("info message")

    out.reset()
    LogbackConfigurer.setLoggerLevel(logger, Level.WARN)
    logger.debug { fail("Must not be called") }
    logger.info { fail("Must not be called") }
    logger.warn { "warn message" }

    assertThat(out.toString()).contains("warn message")

    out.reset()
    LogbackConfigurer.setLoggerLevel(logger, Level.ERROR)
    logger.debug { fail("Must not be called") }
    logger.info { fail("Must not be called") }
    logger.warn { fail("Must not be called") }
    logger.error { "error message" }

    assertThat(out.toString()).contains("error message")
  }

  @Test
  fun testLogging() {
    assertThat(LogbackConfigurer.getLoggerLevel(logger)).isEqualTo(Level.DEBUG) //the default level

    logger.info("The message")
    logger.debug("The debug message")

    out.toString("UTF8").let { log ->
      assertThat(log).contains("INFO  it.neckar.logging.LoggerJvmTest - The message")
      assertThat(log).contains("DEBUG it.neckar.logging.LoggerJvmTest - The debug message")
    }

    out.reset()
    assertThat(out.toString("UTF8")).isEmpty()

    LogbackConfigurer.setLoggerLevel(logger, Level.INFO)

    logger.info("The message")
    logger.debug("The debug message")

    out.toString("UTF8").let { log ->
      assertThat(log).contains("INFO  it.neckar.logging.LoggerJvmTest - The message")
      assertThat(log).doesNotContain("The debug message")
    }
  }
}
