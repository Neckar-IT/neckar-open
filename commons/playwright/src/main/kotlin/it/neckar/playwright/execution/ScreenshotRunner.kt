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

import it.neckar.geometry.Size
import it.neckar.logging.Logger
import it.neckar.logging.LoggerDelegate
import it.neckar.open.collections.fastForEachIndexed
import it.neckar.open.concurrent.toListLimited
import it.neckar.open.kotlin.lang.fastFor
import it.neckar.playwright.PlaywrightSupport
import it.neckar.playwright.PlaywrightSupportInstance
import it.neckar.playwright.screenshots.ScreenshotComparisonError
import it.neckar.playwright.screenshots.ScreenshotConsoleWarning
import it.neckar.playwright.screenshots.ScreenshotResult
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*
import kotlinx.coroutines.flow.*
import java.io.File

/**
 * Runs end-to-end tests in parallel and reports one outcome per test - the screenshots it captured and the errors that
 * stopped it.
 */
class ScreenshotRunner(
  /**
   * The playwright support that is used to run the tests
   */
  val playwrightSupport: PlaywrightSupport,

  /**
   * The size of the screenshots
   */
  val screenshotSize: Size = Size.FullHD,

  /**
   * The number of parallel jobs.
   */
  val numberOfParallelJobs: Int = 6,
) {

  /**
   * Runs every test and returns one outcome per test, in the order the tests were handed over.
   *
   * A failing test does not fail this call: the caller decides what a failure means and writes the report, so a run
   * always reports on all of its tests.
   */
  suspend fun runTests(executableTests: List<ExecutableE2ETest>): List<E2ETestOutcome> {
    logger.debug("Running ${executableTests.size} tests")

    if (executableTests.isEmpty()) {
      return emptyList()
    }

    //Outcomes are matched back to their test by name, and two tests of the same name would also write their screenshots to the same files
    require(executableTests.distinctBy { it.name }.size == executableTests.size) {
      "Every test needs a name of its own, but there are duplicates in <${executableTests.map { it.name }}>"
    }

    val outcomes: List<E2ETestOutcome> = coroutineScope {
      val executableTestsChannel = Channel<ExecutableE2ETest>(Channel.UNLIMITED)
      val outcomesChannel = Channel<E2ETestOutcome>(Channel.UNLIMITED)

      launch {
        executableTests.forEach {
          executableTestsChannel.send(it)
        }
        executableTestsChannel.close() //close channel to signal that no more elements will be sent
      }

      val executionJob = launch {
        numberOfParallelJobs.fastFor {
          launch(Dispatchers.Default) {
            runScreenshotJobs(executableTestsChannel, outcomesChannel)
          }
        }
      }

      executionJob.join()
      outcomesChannel.close()

      outcomesChannel.consumeAsFlow().toListLimited(1000)
    }

    //The tests finish in the order the parallel jobs happen to complete them - the caller sees the order they were handed over in
    val outcomesByName = outcomes.associateBy { it.name }

    return executableTests.map { checkNotNull(outcomesByName[it.name]) { "No outcome for <${it.name}>" } }
  }

  /**
   * Runs the executable tests and sends one outcome per test to the outcomes channel
   */
  private suspend fun runScreenshotJobs(testsChannel: Channel<ExecutableE2ETest>, outcomesChannel: Channel<E2ETestOutcome>) {
    playwrightSupport.createInstance().use { playwrightSupportInstance ->
      testsChannel.consumeEach { executableTest ->
        val outcome = try {
          with(playwrightSupportInstance) {
            runScreenshotJob(executableTest)
          }
        } catch (e: CancellationException) {
          throw e
        } catch (e: Exception) {
          logger.warn("Error while running screenshot job for ${executableTest.name}", e)
          E2ETestOutcome(
            name = executableTest.name,
            screenshotFiles = emptyList(),
            errors = listOf(ScreenshotComparisonError.OtherError(executableTest.name, e)),
            consoleWarnings = emptyList(),
          )
        }

        outcomesChannel.send(outcome)
      }
    }
  }

  /**
   * Runs a single screenshot job
   */
  context(playwrightSupportInstance: PlaywrightSupportInstance)
  private suspend fun runScreenshotJob(executableTest: ExecutableE2ETest): E2ETestOutcome {
    //One channel per test: the console errors belong to the page the test looked at
    val consoleWarningsChannel = Channel<ScreenshotConsoleWarning>(Channel.UNLIMITED)

    val screenshotResults = try {
      playwrightSupportInstance.createScreenshotForExecutableTest(executableTest, viewportSize = screenshotSize, consoleWarningsChannel)
    } finally {
      consoleWarningsChannel.close()
    }

    val screenshotFiles = mutableListOf<File>()
    val errors = mutableListOf<ScreenshotComparisonError>()

    screenshotResults.fastForEachIndexed { index, screenshotResult ->
      val uniqueName = executableTest.uniqueName(ExecutableE2ETest.ScreenshotIndex(index))

      when (screenshotResult) {
        is ScreenshotResult.ErrorOccurred -> {
          val errorsAsString = screenshotResult.errors.joinToString("\n") { "${it.location()}: ${it.text()}" }
          val messagesAsString = screenshotResult.messages.joinToString("\n") { "${it.location()}: ${it.text()}" }

          errors.add(
            ScreenshotComparisonError.OtherError(
              uniqueName,
              AssertionError("Error occurred: ${screenshotResult.error} \n -----\n$errorsAsString\n------\nMessages: $messagesAsString"),
            )
          )
        }

        is ScreenshotResult.ScreenshotCreated -> {
          val actualScreenshotFile = screenshotResult.screenshotFile
          require(actualScreenshotFile.exists()) { "Screenshot file ${actualScreenshotFile.absolutePath} does not exist!" }

          screenshotFiles.add(actualScreenshotFile)
        }
      }
    }

    return E2ETestOutcome(
      name = executableTest.name,
      screenshotFiles = screenshotFiles,
      errors = errors,
      consoleWarnings = consoleWarningsChannel.consumeAsFlow().toListLimited(1000),
    )
  }

  /**
   * Creates the screenshot for the given executable test.
   */
  fun PlaywrightSupportInstance.createScreenshotForExecutableTest(executableE2ETest: ExecutableE2ETest, viewportSize: Size, consoleWarningsChannel: Channel<ScreenshotConsoleWarning>): List<ScreenshotResult> {
    with(executableE2ETest) {
      return execute(viewportSize, consoleWarningsChannel)
    }
  }


  companion object {
    private val logger: Logger by LoggerDelegate()
  }
}
