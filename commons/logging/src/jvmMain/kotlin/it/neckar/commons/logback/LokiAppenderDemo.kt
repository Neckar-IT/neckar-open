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

import it.neckar.commons.logback.loki.configureBatch
import it.neckar.logging.Logger
import it.neckar.logging.LoggerFactory
import it.neckar.runtime.context.Hostname
import org.slf4j.event.Level


fun main() {
  LogbackConfigurer.configureLoggingConsoleAndLoki(
    lokiServerConfiguration = LokiServerConfiguration.neckarIt(
      username = System.getenv("LOKI_PUSH_USERNAME") ?: error("LOKI_PUSH_USERNAME is not set"),
      password = System.getenv("LOKI_PUSH_PASSWORD") ?: error("LOKI_PUSH_PASSWORD is not set"),
    ),
    app = "loki-appender-demo-app",
    hostname = Hostname("test 123"),
    lokiEnabled = true, //force enabled, even in the local development context
    levelForRoot = Level.INFO,
    levelForNeckarIt = Level.DEBUG
  ) {
    configureBatch {
      setTimeoutMs(1000)
    }
  }

  LokiAppenderDemo().runLoggingDemo()
  Thread.sleep(5000)
}


/**
 *
 */
class LokiAppenderDemo {
  fun runLoggingDemo() {
    logger.info("Hello, World! Info")
    logger.warn { "Hello, World! Warn" }
    logger.error("Hello, World! Error", IllegalStateException("This is an error"))
  }

  companion object {
    private val logger: Logger = LoggerFactory.getLogger("it.neckar.commons.logback.LokiAppenderDemo-Foobar")
  }
}
