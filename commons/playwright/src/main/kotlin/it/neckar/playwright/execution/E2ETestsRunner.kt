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
package it.neckar.playwright.execution

import com.github.romankh3.image.comparison.ImageComparison
import com.github.romankh3.image.comparison.model.ImageComparisonResult
import it.neckar.geometry.Size
import it.neckar.logging.Logger
import it.neckar.logging.LoggerDelegate
import it.neckar.open.collections.fastForEach
import it.neckar.open.collections.fastForEachIndexed
import it.neckar.open.concurrent.toListLimited
import it.neckar.open.http.toUrl
import it.neckar.open.kotlin.lang.fastFor
import it.neckar.playwright.PlaywrightSupport
import it.neckar.playwright.PlaywrightSupportInstance
import it.neckar.playwright.screenshots.ScreenshotComparisonError
import it.neckar.playwright.screenshots.ScreenshotDifferenceInfo
import it.neckar.playwright.screenshots.ScreenshotResult
import it.neckar.playwright.screenshots.report.ReportOutputConfiguration
import it.neckar.playwright.screenshots.report.ScreenshotDiffReportCreator
import it.neckar.playwright.screenshots.report.SuccessReportCreator
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*
import java.io.File
import javax.imageio.ImageIO

/**
 * Executes (multiple) end-to-end tests.
 */
class E2ETestsRunner(
  /**
   * The playwright support that is used to run the tests
   */
  val playwrightSupport: PlaywrightSupport,

  /**
   * The size of the screenshots
   */
  val screenshotSize: Size = Size.FullHD,

  /**
   * Where and how to store the reports, screenshots, diffs etc.
   */
  val reportOutputConfiguration: ReportOutputConfiguration,

  /**
   * The number of parallel jobs.
   */
  val numberOfParallelJobs: Int = 6,
) {

  /**
   * Where and how to store the reports, screenshots, diffs etc.
   */
  val screenshotDiffReportCreator: ScreenshotDiffReportCreator = ScreenshotDiffReportCreator(
    reportOutputConfiguration = reportOutputConfiguration,
  )

  val screenshotSuccessReportCreator: SuccessReportCreator = SuccessReportCreator(
    reportOutputConfiguration = reportOutputConfiguration,
  )

  /**
   * Executes multiple E2E tests
   */
  context(coroutineScope: CoroutineScope)
  fun executeTests(executableTests: List<ExecutableE2ETest>) {
    logger.debug("Running ${executableTests.size} demos")

    with(coroutineScope) {
      val executableTestsChannel = Channel<ExecutableE2ETest>(Channel.UNLIMITED)

      launch {
        executableTests.fastForEach {
          executableTestsChannel.send(it)
        }
        executableTestsChannel.close() //close channel to signal that no more elements will be sent
      }

      //Collects all errors that occur during the screenshot comparison
      val errorsChannel = Channel<ScreenshotComparisonError>(Channel.UNLIMITED)
      val screenshotFilesChannel = Channel<File>(Channel.UNLIMITED)

      val executionJob = launch {
        numberOfParallelJobs.fastFor {
          launch(Dispatchers.Default) {
            runExecutableE2ETests(executableTestsChannel, errorsChannel,screenshotFilesChannel)
          }
        }
      }

      //Close the error channel when the execution job is done
      launch {
        executionJob.invokeOnCompletion {
          errorsChannel.close()
          screenshotFilesChannel.close()
        }
        executionJob.join() //wait until everything has been executed

        //Collect all errors
        val errors: List<ScreenshotComparisonError> = errorsChannel.consumeAsFlow().toListLimited(1000)
        if (errors.isEmpty()) {
          // No errors -> Create success report
          val screenshotFiles: List<File> = screenshotFilesChannel.consumeAsFlow().toListLimited(1000)
          screenshotSuccessReportCreator.createSuccessReport(screenshotFiles)
        } else {
          //Create the report file
          val reportFile = screenshotDiffReportCreator.createErrorReport(errors)

          val allErrorsAsString = errors.joinToString("\n") { it.toString() }
          throw AssertionError("Errors found:\n${reportFile.toUrl()} $allErrorsAsString")
        }
      }
    }
  }

  /**
   * Runs the executable tests and sends the results to the errors channel
   */
  private suspend fun runExecutableE2ETests(testsChannel: Channel<ExecutableE2ETest>, errorsChannel: Channel<ScreenshotComparisonError>, screenshotFileChannel: Channel<File>) {
    playwrightSupport.createInstance().use { playwrightSupportInstance ->
      testsChannel.consumeEach { executableTest ->
        try {
          with(playwrightSupportInstance) {
            runScreenshotJob(executableTest, errorsChannel, screenshotFileChannel)
          }
        } catch (e: Exception) {
          logger.warn("Error while running screenshot job for ${executableTest.uniqueName(ExecutableE2ETest.ScreenshotIndex.unknown)}", e)
          errorsChannel.send(ScreenshotComparisonError.OtherError(executableTest.uniqueName(ExecutableE2ETest.ScreenshotIndex.unknown), e))
        }
      }
    }
  }

  /**
   * Runs a single screenshot job
   */
  context(playwrightSupportInstance: PlaywrightSupportInstance)
  private suspend fun runScreenshotJob(executableTest: ExecutableE2ETest, errorsChannel: Channel<ScreenshotComparisonError>, screenshotFileChannel: Channel<File>) {
    val screenshotResults = playwrightSupportInstance.createScreenshotForExecutableTest(executableTest, viewportSize = screenshotSize)

    screenshotResults.fastForEachIndexed { index, screenshotResult ->
      val screenshotIndex = ExecutableE2ETest.ScreenshotIndex(index)

      when (screenshotResult) {
        is ScreenshotResult.ErrorOccurred -> {

          val error = screenshotResult.error
          val errorsAsString = screenshotResult.errors.joinToString("\n") { "${it.location()}: ${it.text()}" }
          val messagesAsString = screenshotResult.messages.joinToString("\n") { "${it.location()}: ${it.text()}" }

          throw AssertionError("Error occurred: $error \n -----\n$errorsAsString\n------\nMessages: $messagesAsString")
        }

        is ScreenshotResult.ScreenshotCreated -> {
          val actualScreenshotFile = screenshotResult.screenshotFile
          require(actualScreenshotFile.exists()) { "Screenshot file ${actualScreenshotFile.absolutePath} does not exist!" }

          //Compare
          val expectedScreenshotFile = reportOutputConfiguration.expectedScreenshotFile(actualScreenshotFile.name)

          val uniqueName = executableTest.uniqueName(screenshotIndex)

          if (expectedScreenshotFile.isFile) {
            val expectedScreenshot = ImageIO.read(expectedScreenshotFile)

            try {
              val actualScreenshot = ImageIO.read(actualScreenshotFile)

              val imageComparisonResult: ImageComparisonResult = ImageComparison(expectedScreenshot, actualScreenshot).compareImages()
              if (imageComparisonResult.differencePercent > 0.0) {
                val diffsFile = reportOutputConfiguration.diffsFile(actualScreenshotFile.name)

                ImageIO.write(imageComparisonResult.result, "png", diffsFile)

                val comparisonInfo = ScreenshotDifferenceInfo(expectedScreenshotFile, actualScreenshotFile, imageComparisonResult.imageComparisonState, diffsFile)
                logger.error("Diff detected for $uniqueName")
                errorsChannel.send(ScreenshotComparisonError.DifferenceDetected(uniqueName, comparisonInfo))
              } else {
                // No comparison error -> send the newly created screenshot file
                screenshotFileChannel.send(actualScreenshotFile)
              }
            } catch (e: Exception) {
              logger.error("Error while comparing screenshots for $uniqueName", e)
              errorsChannel.send(ScreenshotComparisonError.OtherError(uniqueName, e))
            }
          } else {
            //No baseline image found!
            logger.error("No baseline image found for $uniqueName")
            errorsChannel.send(ScreenshotComparisonError.BaselineScreenshotMissing(uniqueName, expectedScreenshotFile, actualScreenshotFile))
          }
        }
      }
    }
  }

  /**
   * Creates the screenshot for the given executable test.
   */
  fun PlaywrightSupportInstance.createScreenshotForExecutableTest(executableE2ETest: ExecutableE2ETest, viewportSize: Size): List<ScreenshotResult> {
    with(executableE2ETest) {
      return execute(viewportSize)
    }
  }


  companion object {
    private val logger: Logger by LoggerDelegate()
  }
}
