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
package it.neckar.open.app.logging

import it.neckar.commons.logback.LogbackConfigurer
import it.neckar.open.app.ApplicationHomeAccess
import it.neckar.open.file.requireIsDirectory
import org.slf4j.event.Level
import java.io.File

/**
 * Configures logback using application home.
 *
 * If only Console log output is required, you could use: `LogbackConfigurer.configureLoggingConsoleOnly(level)`
 *
 * ATTENTION: Projects that use this class must add a dependency to `Projects.open.commons.app` manually:
 * `implementation(project(Projects.open.commons.app))`
 */
class ApplicationHomeLogbackConfigurer(
  val applicationHomeAccess: ApplicationHomeAccess,
) {

  constructor(applicationName: String) : this(ApplicationHomeAccess.create(applicationName))

  /**
   * Configure logging - console and application log file
   */
  fun configureLogging(level: Level = Level.INFO) {
    val logFile = getLogFile()
    LogbackConfigurer.configureLoggingConsoleAndFile(logFile, level)
  }

  /**
   * Returns the log file for the application
   */
  fun getLogFile(): File = getLogFile(applicationHomeAccess)

  companion object {
    /**
     * Returns the log file for the application
     */
    fun getLogFile(applicationName: String): File {
      val applicationHomeAccess = ApplicationHomeAccess.create(applicationName)
      return getLogFile(applicationHomeAccess)
    }

    /**
     * Returns the log file within the given application home access
     */
    fun getLogFile(applicationHomeAccess: ApplicationHomeAccess): File {
      applicationHomeAccess.cacheHome.requireIsDirectory()
      return File(applicationHomeAccess.cacheHome, LogbackConfigurer.LogFileName)
    }
  }
}
