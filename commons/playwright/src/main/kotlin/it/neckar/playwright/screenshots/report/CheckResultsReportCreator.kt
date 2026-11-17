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

import it.neckar.open.collections.fastForEach
import it.neckar.playwright.execution.E2ETestOutcome
import kotlinx.html.body
import kotlinx.html.div
import kotlinx.html.dom.createHTMLDocument
import kotlinx.html.dom.serialize
import kotlinx.html.h1
import kotlinx.html.h3
import kotlinx.html.head
import kotlinx.html.html
import kotlinx.html.img
import kotlinx.html.p
import kotlinx.html.pre
import kotlinx.html.style
import kotlinx.html.title
import kotlinx.html.unsafe
import java.io.File
import java.io.Writer

/**
 * Creates the HTML report of a run: every test with its status and its screenshots, passed and failed side by side.
 */
class CheckResultsReportCreator(
  /**
   * Configuration for the report output
   */
  val reportOutputConfiguration: ReportOutputConfiguration,
) {
  /**
   * Writes the report and copies the screenshots next to it.
   */
  fun createReport(outcomes: List<E2ETestOutcome>): File {
    require(outcomes.isNotEmpty()) { "At least one outcome is required" }

    val reportScreenshotsDir = File(reportOutputConfiguration.reportsDir, "screenshots").also { it.mkdirs() }

    outcomes.fastForEach { outcome ->
      outcome.screenshotFiles.fastForEach { file ->
        file.copyTo(File(reportScreenshotsDir, file.name), overwrite = true)
      }
    }

    val reportFile = reportOutputConfiguration.reportFile()

    reportFile.bufferedWriter().use { out ->
      writeHtmlReport(outcomes, out)
    }

    return reportFile
  }

  internal fun writeHtmlReport(outcomes: List<E2ETestOutcome>, out: Writer) {
    require(outcomes.isNotEmpty()) { "At least one outcome is required" }

    val failedCount = outcomes.count { it.failed }
    val summary = "${outcomes.size - failedCount} of ${outcomes.size} checks passed"

    //The failed ones first: the report is read when a check went red, and looking for the red site among eleven is the work it should save
    val failedFirst = outcomes.sortedByDescending { it.failed }

    val doc = createHTMLDocument().html {
      head {
        title {
          +summary
        }

        style {
          //language=CSS
          unsafe {
            +"""
            body { font-family: -apple-system, BlinkMacSystemFont, 'Segoe UI', Roboto, sans-serif; background: #002e46; color: #f9fafb; padding: 24px; }
            .site { border-radius: 12px; padding: 16px; margin-bottom: 16px; border: 1px solid rgba(255,255,255,0.12); }
            .site.passed { background: rgba(34,197,94,0.12); }
            .site.failed { background: rgba(239,68,68,0.16); }
            .status { text-transform: uppercase; letter-spacing: 0.08em; font-size: 0.8rem; }
            .error, .warning { white-space: pre-wrap; background: rgba(0,0,0,0.35); border-radius: 8px; padding: 12px; overflow-x: auto; }
            .warning { border-left: 4px solid rgba(241,131,34,0.8); }
            img { max-width: 100%; border-radius: 8px; margin-top: 12px; }
          """.trimIndent()
          }
        }
      }

      body {
        h1 {
          +summary
        }

        failedFirst.fastForEach { outcome ->
          div("site ${if (outcome.failed) "failed" else "passed"}") {
            h3 {
              +outcome.name.value
            }

            p("status") {
              +if (outcome.failed) "failed" else "passed"
            }

            outcome.errors.fastForEach { error ->
              pre("error") {
                +error.toString()
              }
            }

            outcome.consoleWarnings.fastForEach { warning ->
              pre("warning") {
                +warning.toString()
              }
            }

            outcome.screenshotFiles.fastForEach { file ->
              img {
                src = "screenshots/${file.name}"
                alt = file.name
              }
            }
          }
        }
      }
    }

    out.write(doc.serialize(true))
  }
}
