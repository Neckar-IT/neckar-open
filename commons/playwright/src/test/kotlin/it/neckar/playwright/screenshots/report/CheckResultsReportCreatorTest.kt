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

import assertk.assertFailure
import assertk.assertThat
import assertk.assertions.contains
import assertk.assertions.exists
import assertk.assertions.isInstanceOf
import assertk.assertions.isLessThan
import it.neckar.playwright.UniqueName
import it.neckar.playwright.execution.E2ETestOutcome
import it.neckar.playwright.screenshots.ScreenshotComparisonError
import it.neckar.playwright.screenshots.ScreenshotConsoleWarning
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.io.TempDir
import java.io.File

class CheckResultsReportCreatorTest {
  @Test
  fun `the report contains the passed sites even when another site failed`(@TempDir tempDir: File) {
    val configuration = prepareConfiguration(tempDir)

    val reportFile = CheckResultsReportCreator(configuration).createReport(
      listOf(
        passed(configuration, "https://neckar.it"),
        failed("https://dev.elektromeister.neckar.it/projects", "Socket timeout has expired"),
      )
    )

    val report = reportFile.readText()
    assertThat(report).contains("https://neckar.it")
    assertThat(report).contains("https://dev.elektromeister.neckar.it/projects")
    assertThat(report).contains("Socket timeout has expired")
  }

  @Test
  fun `every site carries its own status`(@TempDir tempDir: File) {
    val configuration = prepareConfiguration(tempDir)

    val reportFile = CheckResultsReportCreator(configuration).createReport(
      listOf(
        passed(configuration, "https://neckar.it"),
        failed("https://mea.neckar.it", "boom"),
      )
    )

    val report = reportFile.readText()
    assertThat(report).contains("""class="site passed"""")
    assertThat(report).contains("""class="site failed"""")
  }

  @Test
  fun `the screenshot of a passed site is copied next to the report`(@TempDir tempDir: File) {
    val configuration = prepareConfiguration(tempDir)

    val reportFile = CheckResultsReportCreator(configuration).createReport(listOf(passed(configuration, "https://neckar.it")))

    assertThat(File(configuration.reportsDir, "screenshots/https---neckar.it.png")).exists()
    assertThat(reportFile.readText()).contains("screenshots/https---neckar.it.png")
  }

  @Test
  fun `a console warning is named next to its site`(@TempDir tempDir: File) {
    val configuration = prepareConfiguration(tempDir)
    val name = UniqueName("https://neckar.it")

    val reportFile = CheckResultsReportCreator(configuration).createReport(
      listOf(
        E2ETestOutcome(
          name = name,
          screenshotFiles = emptyList(),
          errors = emptyList(),
          consoleWarnings = listOf(ScreenshotConsoleWarning.Warning(name, "Uncaught TypeError", "https---neckar.it")),
        )
      )
    )

    assertThat(reportFile.readText()).contains("Uncaught TypeError")
  }

  /**
   * The report is read when a check went red - the failed sites are what the reader came for.
   */
  @Test
  fun `the failed sites come first`(@TempDir tempDir: File) {
    val configuration = prepareConfiguration(tempDir)

    val reportFile = CheckResultsReportCreator(configuration).createReport(
      listOf(
        passed(configuration, "https://neckar.it"),
        failed("https://mea.neckar.it", "boom"),
      )
    )

    val report = reportFile.readText()
    assertThat(report.indexOf("https://mea.neckar.it")).isLessThan(report.indexOf("https://neckar.it"))
  }

  /**
   * A report of an empty run would silently look like a green run, so the creator refuses it rather than writing one.
   */
  @Test
  fun `an empty run has no report`(@TempDir tempDir: File) {
    val configuration = prepareConfiguration(tempDir)

    assertFailure { CheckResultsReportCreator(configuration).createReport(emptyList()) }
      .isInstanceOf(IllegalArgumentException::class)
  }

  private fun prepareConfiguration(tempDir: File): ReportOutputConfiguration {
    return ReportOutputConfiguration(
      baselineScreenshotsDir = null,
      screenshotsDir = File(tempDir, "screenshots"),
      diffsDir = null,
      reportsDir = File(tempDir, "reports/playwright"),
      tracingOutputDir = File(tempDir, "tracing"),
    ).also { it.prepareDirectories() }
  }

  private fun passed(configuration: ReportOutputConfiguration, url: String): E2ETestOutcome {
    val name = UniqueName(url)
    val screenshotFile = File(configuration.screenshotsDir, name.value.replace(Regex("[:/]"), "-") + ".png").also { it.writeText("png") }

    return E2ETestOutcome(name = name, screenshotFiles = listOf(screenshotFile), errors = emptyList(), consoleWarnings = emptyList())
  }

  private fun failed(url: String, message: String): E2ETestOutcome {
    val name = UniqueName(url)

    return E2ETestOutcome(
      name = name,
      screenshotFiles = emptyList(),
      errors = listOf(ScreenshotComparisonError.OtherError(name, IllegalStateException(message))),
      consoleWarnings = emptyList(),
    )
  }
}
