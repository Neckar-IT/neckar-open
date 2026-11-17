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

import assertk.assertThat
import assertk.assertions.exists
import assertk.assertions.isDirectory
import assertk.assertions.isEqualTo
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class ReportOutputConfigurationTest {
  @Test
  fun `prepareDirectories creates every output directory - including the missing parent directories`(@TempDir tempDir: File) {
    //The build directory does not exist yet - exactly the situation on a clean checkout
    val buildDir = File(tempDir, "build")
    val configuration = configurationBelow(buildDir)

    configuration.prepareDirectories()

    assertThat(configuration.screenshotsDir).isDirectory()
    assertThat(configuration.diffsDir!!).isDirectory()
    assertThat(configuration.tracingOutputDir).isDirectory()
    assertThat(configuration.reportsDir).isDirectory()

    //The report can be written now - reportFile() requires an existing parent directory
    configuration.reportFile().writeText("report")
    assertThat(configuration.reportFile()).exists()
  }

  @Test
  fun `prepareDirectories removes the content of a previous run`(@TempDir tempDir: File) {
    val buildDir = File(tempDir, "build")
    val configuration = configurationBelow(buildDir)

    configuration.prepareDirectories()
    File(configuration.screenshotsDir, "stale.png").writeText("stale")

    configuration.prepareDirectories()

    assertThat(configuration.screenshotsDir).isDirectory()
    assertThat(configuration.screenshotsDir.list()!!.toList()).isEqualTo(emptyList())
  }

  private fun configurationBelow(buildDir: File): ReportOutputConfiguration {
    return ReportOutputConfiguration(
      baselineScreenshotsDir = null,
      screenshotsDir = File(buildDir, "screenshots"),
      diffsDir = File(buildDir, "screenshots-diffs"),
      reportsDir = File(buildDir, "reports/playwright"),
      tracingOutputDir = File(buildDir, "tracing"),
    )
  }
}
