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
package it.neckar.playwright

import com.google.common.base.Stopwatch
import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserContext
import com.microsoft.playwright.ConsoleMessage
import com.microsoft.playwright.Page
import com.microsoft.playwright.Tracing
import com.microsoft.playwright.options.ScreenshotAnimations
import it.neckar.commons.logback.debug
import it.neckar.datetime.minimal.TimeZone
import it.neckar.logging.Logger
import it.neckar.logging.LoggerFactory
import it.neckar.open.http.Url
import it.neckar.open.i18n.Locale
import it.neckar.playwright.screenshots.ScreenshotResult
import java.io.File
import java.util.concurrent.TimeUnit
import javax.annotation.concurrent.NotThreadSafe
import kotlin.io.path.absolutePathString

/**
 * Represents a Playwright instance that is *not* thread safe
 */
@NotThreadSafe
class PlaywrightSupportInstance(
  /**
   * The directory where the screenshots will be placed in.
   */
  val screenshotsDir: File,
  /**
   * The directory where the tracing files will be placed in.
   */
  val tracingDir: File,

  /**
   * The browser instance
   */
  val browser: Browser,
) : AutoCloseable {

  override fun close() {
    browser.close()
  }

  /**
   * Creates a screenshot for the provided URL and saves it to the [screenshotsDir] with the provided [fileName].
   */
  fun createScreenshot(
    url: Url,
    /**
     * The file name to use - must be encoded as a valid file name.
     */
    fileName: String,
    /**
     * The viewport width of the browser window
     */
    viewportWidth: Int = 1920,
    /**
     * The viewport height of the browser window
     */
    viewportHeight: Int = 1080,
    /**
     * The run options to use
     */
    runOptions: PlaywrightRunOptions = PlaywrightRunOptions.Default,

    /**
     * If css animations are allowed. Default: ALLOW
     */
    screenshotAnimations: ScreenshotAnimations = ScreenshotAnimations.ALLOW,

    /**
     * When true, takes a screenshot of the full scrollable page, instead of the currently visible viewport.
     */
    screenshotFullPage: Boolean = true,

    /**
     * Callback to prepare the page before the screenshot is taken
     */
    preparePage: (page: Page) -> Unit,

    ): ScreenshotResult {
    val screenshotFile = File(screenshotsDir, "$fileName.png")
    logger.debug {
      "Create screenshot for: $url"
    }

    val stopwatch = Stopwatch.createStarted()

    val detailedLogger: Logger = LoggerFactory.getLogger("PlaywrightSupport-$fileName")

    require(browser.isConnected) {
      "Browser is not connected"
    }

    browser.newContext(
      Browser.NewContextOptions()
        .setLocale(Locale.US.locale)
        .setTimezoneId(TimeZone.Berlin.zoneId)

    ).use { browserContext: BrowserContext ->
      logger.debug("Context created after ${stopwatch.elapsed(TimeUnit.MILLISECONDS)} ms")

      if (runOptions.tracingActive) {
        browserContext.tracing().start(
          Tracing.StartOptions().also {
            it.screenshots = true
            it.snapshots = true
          })
      }

      logger.debug("Tracing activated after ${stopwatch.elapsed(TimeUnit.MILLISECONDS)} ms")

      try {
        browserContext.newPage().use { page ->
          logger.debug("Page created after ${stopwatch.elapsed(TimeUnit.MILLISECONDS)} ms")
          page.setViewportSize(viewportWidth, viewportHeight)

          //Store all console messages
          val consoleMessages = mutableListOf<ConsoleMessage>()
          page.onConsoleMessage {
            detailedLogger.debug { "Console: [${it.type()}] ${it.text()} (${it.location()})" }
            it.args().forEach { arg ->
              detailedLogger.debug { "\tConsole message arg: $arg" }
            }
            consoleMessages.add(it)
          }

          var pageError: String? = null
          page.onPageError {
            logger.error("Page Error detected: $it")
            pageError = it
          }

          val response = page.navigate(url)
          if (response.ok().not()) {
            return ScreenshotResult.ErrorOccurred("Response not OK: ${response.status()}", emptyList(), emptyList())
          }
          logger.debug("Navigated after ${stopwatch.elapsed(TimeUnit.MILLISECONDS)} ms")

          page.waitForLoadState()
          logger.debug("Loaded after ${stopwatch.elapsed(TimeUnit.MILLISECONDS)} ms")

          preparePage(page)

          logger.debug("Prepare called after ${stopwatch.elapsed(TimeUnit.MILLISECONDS)} ms")

          val errors = consoleMessages.filter { it.isError() }
          val messages = consoleMessages.filter { it.isNotError() }

          pageError?.let {
            return ScreenshotResult.ErrorOccurred(it, errors, messages)
          }

          val screenshot = page.screenshot(Page.ScreenshotOptions().apply {
            fullPage = screenshotFullPage
            animations = screenshotAnimations
          })

          logger.debug("Screenshot created after ${stopwatch.elapsed(TimeUnit.MILLISECONDS)} ms")
          screenshotFile.writeBytes(screenshot)
          logger.debug("Screenshot saved after ${stopwatch.elapsed(TimeUnit.MILLISECONDS)} ms")
        }

        logger.debug("Page closed after ${stopwatch.elapsed(TimeUnit.MILLISECONDS)} ms")
      } finally {
        if (runOptions.tracingActive) {
          browserContext.tracing().stop(
            Tracing.StopOptions().also {
              it.path = File(tracingDir, "$fileName.zip").toPath().also { tracePath ->
                logger.debug("Trace saved @ ${tracePath.absolutePathString()}")
              }
            })
        }
        logger.debug("Trace saved after ${stopwatch.elapsed(TimeUnit.MILLISECONDS)} ms")
      }
    }
    logger.debug("Context closed after ${stopwatch.elapsed(TimeUnit.MILLISECONDS)} ms")

    return ScreenshotResult.ScreenshotCreated(screenshotFile)
  }

  companion object {
    private val logger: Logger = LoggerFactory.getLogger("it.neckar.playwright.PlaywrightSupportInstance")
  }

}
