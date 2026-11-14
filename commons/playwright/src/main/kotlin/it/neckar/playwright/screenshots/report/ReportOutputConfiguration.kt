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
package it.neckar.playwright.screenshots.report

import it.neckar.open.file.requireIsDirectory
import java.io.File

/**
 * Configuration for the report output
 */
data class ReportOutputConfiguration(
  /**
   * Contains the baseline screenshots for the demos.
   */
  val baselineScreenshotsDir: File? = File("screenshots"),

  /**
   * Where the screenshots are saved in
   */
  val screenshotsDir: File = File("build/screenshots"),

  /**
   * Contains the screenshot diffs
   */
  val diffsDir: File? = File("build/screenshots-diffs"),

  /**
   * Where the reports are placed in
   */
  val reportsDir: File = File("build/reports/playwright"),

  /**
   * Where the traces are placed in
   */
  val tracingOutputDir: File = File("build/tracing"),
) {

  fun prepareDirectories() {
    baselineScreenshotsDir?.requireIsDirectory()

    screenshotsDir.deleteRecursively()
    screenshotsDir.createDirectory()

    diffsDir?.deleteRecursively()
    diffsDir?.createDirectory()

    tracingOutputDir.deleteRecursively()
    tracingOutputDir.createDirectory()

    reportsDir.deleteRecursively()
    reportsDir.createDirectory()
  }

  /**
   * Creates the file for the expected screenshot
   */
  fun expectedScreenshotFile(fileName: String): File {
    return File(baselineScreenshotsDir, fileName)
  }

  fun diffsFile(name: String): File {
    return File(diffsDir, name)
  }

  /**
   * Returns the HTML file where the report is written to.
   */
  fun reportFile(): File {
    return File(reportsDir, "index.html").also {
      require(it.parentFile.isDirectory) { "Parent directory of ${it.absolutePath} is not a directory" }
    }
  }
}

/**
 * Creates this directory - including all missing parent directories.
 *
 * [File.mkdir] is not sufficient: it only reports its failure through the return value, and it fails silently as soon
 * as a single parent directory is missing.
 */
private fun File.createDirectory() {
  check(mkdirs() || isDirectory) { "Could not create the directory <$absolutePath>" }
}
