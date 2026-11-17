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
import kotlinx.html.body
import kotlinx.html.div
import kotlinx.html.dom.createHTMLDocument
import kotlinx.html.dom.serialize
import kotlinx.html.h2
import kotlinx.html.h3
import kotlinx.html.head
import kotlinx.html.hr
import kotlinx.html.html
import kotlinx.html.img
import kotlinx.html.p
import kotlinx.html.style
import kotlinx.html.title
import kotlinx.html.unsafe
import java.io.File
import java.io.OutputStream
import java.io.Writer

/**
 * Creates an HTML report
 */
class SuccessReportCreator(
  /**
   * Configuration for the report output
   */
  val reportOutputConfiguration: ReportOutputConfiguration,
) {
  /**
   * Creates an HTML file with the success report.
   * Also copies the screenshots to the provided directory.
   */
  fun createSuccessReport(screenshotFiles: List<File>): File {
    require(screenshotFiles.isNotEmpty()) { "At least one screenshot file is required" }
    val reportScreenshotsDir = File(reportOutputConfiguration.reportsDir, "screenshots").also { it.mkdir() }

    ////Copy the screenshots and diff files
    screenshotFiles.fastForEach { file ->
      file.copyTo(File(reportScreenshotsDir, file.name))
    }

    val reportFile = reportOutputConfiguration.reportFile()

    reportFile.outputStream().use { out ->
      writeHtmlSuccessReport(screenshots = screenshotFiles, out = out)
    }

    return reportFile
  }

  /**
   * Creates an HTML report
   */
  internal fun writeHtmlSuccessReport(screenshots: List<File>, out: OutputStream) {
    require(screenshots.isNotEmpty()) { "At least one screenshot file is required" }

    out.bufferedWriter().use {
      writeHtmlSuccessReport(screenshots, it)
    }
  }

  /**
   * Creates an HTML report for the provided [screenshots] and writes it to the [out] writer.
   */
  internal fun writeHtmlSuccessReport(screenshots: List<File>, out: Writer) {
    require(screenshots.isNotEmpty()) { "At least one screenshot file is required" }

    val doc = createHTMLDocument().html {
      head {
        title {
          +"Success - created ${screenshots.size} screenshots"
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
            +"Success - created ${screenshots.size} screenshots"

          }

          screenshots.fastForEach { file ->
            hr { }

            h3 {
              +file.name
            }

            div {
              p {
                +"Screenshot created and no difference detected: ${file.name}"
              }
              img {
                src = "screenshots/${file.name}"
              }
            }
          }
        }
      }
    }

    out.write(doc.serialize(true))
  }
}
