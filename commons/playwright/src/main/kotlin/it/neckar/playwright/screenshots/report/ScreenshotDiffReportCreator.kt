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
import it.neckar.playwright.screenshots.ScreenshotComparisonError
import it.neckar.uuid.randomForEnvironment
import kotlinx.html.body
import kotlinx.html.button
import kotlinx.html.div
import kotlinx.html.dom.createHTMLDocument
import kotlinx.html.dom.serialize
import kotlinx.html.h2
import kotlinx.html.h3
import kotlinx.html.h5
import kotlinx.html.head
import kotlinx.html.hr
import kotlinx.html.html
import kotlinx.html.id
import kotlinx.html.img
import kotlinx.html.p
import kotlinx.html.pre
import kotlinx.html.script
import kotlinx.html.style
import kotlinx.html.title
import kotlinx.html.unsafe
import java.io.File
import java.io.OutputStream
import java.io.Writer
import kotlin.uuid.Uuid

/**
 * Copyright 2023 Neckar IT GmbH, Mössingen, Germany
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
/**
 * Creates an HTML report
 */
class ScreenshotDiffReportCreator(
  /**
   * Configuration for the report output
   */
  val reportOutputConfiguration: ReportOutputConfiguration,
) {
  /**
   * Creates an HTML file with the error report.
   * Also copies the screenshots and diff files to the provided directories.
   */
  fun createErrorReport(errors: List<ScreenshotComparisonError>): File {
    require(errors.isNotEmpty()) { "Need at least one errors" }

    val reportBaselineScreenshotsDir = File(reportOutputConfiguration.reportsDir, "baseline").also { it.mkdir() }
    val reportScreenshotsDir = File(reportOutputConfiguration.reportsDir, "screenshots").also { it.mkdir() }
    val diffsDir = File(reportOutputConfiguration.reportsDir, "diffs").also { it.mkdir() }

    //Copy the screenshots and diff files
    errors.fastForEach { error ->
      when (error) {
        is ScreenshotComparisonError.BaselineScreenshotMissing -> {
          error.currentScreenshotFile.copyTo(File(reportScreenshotsDir, error.currentScreenshotFile.name))
        }

        is ScreenshotComparisonError.DifferenceDetected -> {
          error.comparisonInfo.currentScreenshot.copyTo(File(reportScreenshotsDir, error.comparisonInfo.currentScreenshot.name))
          error.comparisonInfo.baselineScreenshot.copyTo(File(reportBaselineScreenshotsDir, error.comparisonInfo.baselineScreenshot.name))
          error.comparisonInfo.diffsFile.copyTo(File(diffsDir, error.comparisonInfo.baselineScreenshot.name))
        }

        is ScreenshotComparisonError.OtherError -> {
          //nothing to do
        }
      }
    }

    val reportFile = reportOutputConfiguration.reportFile()

    reportFile.outputStream().use { out ->
      writeHtmlErrorReport(errors, out)
    }
    File(reportFile.parentFile, ToggleScriptFileName).writeText(ToggleScript)

    return reportFile
  }

  /**
   * Creates an HTML report
   */
  internal fun writeHtmlErrorReport(errors: List<ScreenshotComparisonError>, out: OutputStream) {
    require(errors.isNotEmpty()) { "Need at least one errors" }

    out.bufferedWriter().use {
      writeHtmlErrorReport(errors, it)
    }
  }

  /**
   * Creates an HTML report for the provided [errors] and writes it to the [out] writer.
   */
  internal fun writeHtmlErrorReport(errors: List<ScreenshotComparisonError>, out: Writer) {
    require(errors.isNotEmpty()) { "Need at least one errors" }

    val doc = createHTMLDocument().html {
      head {
        title {
          +"Report - with ${errors.size} errors"
        }

        style {
          //language=CSS
          unsafe {
            +"""
            .inactive {
              display: none;
            }
            body{
                backgroundColor: #BBBBBB
            }
          """.trimIndent()
          }
        }
      }

      body {
        div {
          h2 {
            +"Report - with ${errors.size} errors"
          }

          errors.fastForEach { error ->
            hr { }

            val uuid = Uuid.Companion.randomForEnvironment()

            h3 {
              +error.name.value
            }

            div {
              when (error) {
                is ScreenshotComparisonError.BaselineScreenshotMissing -> {
                  p {
                    +"Baseline screenshot is missing: ${error.currentScreenshotFile.name}"
                  }
                  img {
                    src = "screenshots/${error.currentScreenshotFile.name}"
                  }
                }

                is ScreenshotComparisonError.DifferenceDetected -> {
                  p {
                    +"Difference detected: ${error.comparisonInfo.diffsFile.name}"
                  }

                  img {
                    src = "diffs/${error.comparisonInfo.diffsFile.name}"
                  }

                  button {
                    +"Toggle"
                    attributes["data-toggle-image"] = uuid.toString()
                  }

                  div {
                    div {
                      id = "current_$uuid"

                      h5 {
                        +"Current"
                      }
                      img {
                        src = "screenshots/${error.comparisonInfo.currentScreenshot.name}"
                      }
                    }

                    div(classes = "inactive") {
                      id = "baseline_$uuid"
                      h5 {
                        +"Baseline"
                      }
                      img {
                        src = "baseline/${error.comparisonInfo.baselineScreenshot.name}"
                      }
                    }
                  }
                }

                is ScreenshotComparisonError.OtherError -> {
                  +error.name.value
                  pre {
                    +error.error.stackTraceToString()
                  }
                }
              }
            }
          }
        }

        // A file beside the report rather than inline: the site serving the report admits no inline script.
        script {
          src = ToggleScriptFileName
        }
      }
    }

    out.write(doc.serialize(true))
  }

  private companion object {
    const val ToggleScriptFileName: String = "report.js"

    //language=js
    val ToggleScript: String = """
      document.addEventListener("click", function (event) {
        var button = event.target.closest("[data-toggle-image]");
        if (button === null) return;
        var uuid = button.dataset.toggleImage;
        document.getElementById("current_" + uuid).classList.toggle("inactive");
        document.getElementById("baseline_" + uuid).classList.toggle("inactive");
      });
    """.trimIndent()
  }
}
