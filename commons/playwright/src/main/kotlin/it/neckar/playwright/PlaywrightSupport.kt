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

import kotlin.coroutines.cancellation.CancellationException
import com.microsoft.playwright.Browser
import com.microsoft.playwright.BrowserType
import com.microsoft.playwright.Playwright
import it.neckar.commons.logback.debug
import it.neckar.docker.chrome.ChromeDockerContainerSupport
import it.neckar.docker.nginx.NginxDockerContainerSupport
import it.neckar.logging.Logger
import it.neckar.logging.LoggerFactory
import it.neckar.open.collections.fastForEach
import it.neckar.open.dispose.Disposable
import it.neckar.open.dispose.DisposeSupport
import it.neckar.open.dispose.OnDispose
import it.neckar.open.kotlin.lang.isCloseTo
import it.neckar.playwright.chrome.ChromeChannel
import java.io.File
import javax.annotation.concurrent.ThreadSafe

/**
 * Supports creation of screenshots using playwright.
 */
@ThreadSafe
class PlaywrightSupport(
  /**
   * The directory where the screenshots will be placed in.
   */
  val screenshotsDir: File,
  /**
   * The directory where the tracing files will be placed in.
   */
  val tracingDir: File,
  /**
   * Creates the browser instance
   */
  val playwrightBrowserFactory: PlaywrightBrowserFactory,
) : AutoCloseable, Disposable, OnDispose {

  private val disposeSupport = DisposeSupport()

  override fun onDispose(action: () -> Unit) {
    disposeSupport.onDispose(action)
  }

  override fun dispose() {
    disposeSupport.dispose()
  }

  override fun close() {
    dispose()
  }

  /**
   * Creates a new instance (which is *not* thread safe).
   */
  fun createInstance(): PlaywrightSupportInstance {
    return PlaywrightSupportInstance(
      screenshotsDir = screenshotsDir,
      tracingDir = tracingDir,
      browser = playwrightBrowserFactory.createBrowser(),
    )
  }

  companion object {
    /**
     * Creates a new playwright support instance using a locally executed browser (non docker).
     */
    fun createLocally(
      /**
       * Where to save the screenshots
       */
      screenshotsDir: File,
      /**
       * Where to save the traces (if enabled)
       */
      tracingDir: File,
      /**
       * The chrome channel to use
       */
      chromeChannel: ChromeChannel,
    ): PlaywrightSupport {
      logger.debug { "Using channel <$chromeChannel>" }

      val launchOptions = BrowserType.LaunchOptions()
      when (chromeChannel) {
        ChromeChannel.ChromeBundled -> {
          //Do nothing! Use the bundled chrome
        }

        else -> launchOptions.setChannel(chromeChannel.channelName)
      }

      if (false) {
        launchOptions.headless = false //for debugging purposes
      }
      launchOptions.args = listOf("--incognito", "--no-default-browser-check")


      return PlaywrightSupport(screenshotsDir, tracingDir) {
        val playwright = Playwright.create()
        playwright.chromium().launch(launchOptions)
      }
    }

    /**
     * Creates a new instance that uses a docker container to run the tests.
     * @param dockerImage The name of the docker image
     * @param expectedChrome The expected Chrome version
     * @param chromeDockerContainerSupport The chrome docker container support that is used to start and stop the container
     * @param nginxDockerContainerSupport The nginx docker container support that is used to start and stop the container. Can be set to null if the nginx container is not required.
     */
    suspend fun runDockerContainers(
      screenshotsDir: File,
      tracingDir: File,
      dockerImage: String = "selenium/standalone-chrome",
      expectedChrome: ChromeChannel,
      chromeDockerContainerSupport: ChromeDockerContainerSupport,
      nginxDockerContainerSupport: NginxDockerContainerSupport?,

      ): PlaywrightSupport {
      logger.debug { "Using docker image <$dockerImage>" }

      chromeDockerContainerSupport.start()
      nginxDockerContainerSupport?.start()

      try {
        chromeDockerContainerSupport.waitForContainerToBeRunning()
        nginxDockerContainerSupport?.waitForContainerToBeRunning()
        nginxDockerContainerSupport?.waitForNginxToBeReady()
        chromeDockerContainerSupport.logContainers()
        return PlaywrightSupport(screenshotsDir, tracingDir, PlaywrightBrowserFactory {
          val playwright = Playwright.create()
          val browserType: BrowserType = playwright.chromium()

          browserType.connectOverCDP(chromeDockerContainerSupport.getExternalUrl().value)
        }).also {
          it.onDispose(chromeDockerContainerSupport)
          if (nginxDockerContainerSupport != null) {
            it.onDispose(nginxDockerContainerSupport)
          }
        }
      } catch (e: CancellationException) {
        throw e
      } catch (e: Exception) {
        chromeDockerContainerSupport.stop()
        nginxDockerContainerSupport?.stop()
        throw e
      }
    }

    /**
     * Helper method to verify the version numbers of the installed browsers.
     *
     * If this method fails, update the version numbers in [ChromeChannel].
     */
    @Deprecated("Do not check for version numbers anymore - they change too often")
    fun verifyVersionNumbers(
      chromeChannelsToCheck: List<ChromeChannel>,
      /**
       * The major version tolerance value
       */
      majorVersionTolerance: Int = 0,
    ) {
      require(majorVersionTolerance >= 0) {
        "majorVersionTolerance must be >=0 but was $majorVersionTolerance"
      }

      Playwright.create().use { playwright ->
        chromeChannelsToCheck.fastForEach { chromeChannel ->
          val launchOptions = BrowserType.LaunchOptions()
          launchOptions.setChannel(chromeChannel.channelName)

          playwright.chromium().launch(launchOptions).use { browser ->
            val version = browser.version()
            val majorVersion = browser.majorVersion()

            if (majorVersionTolerance > 0) {
              require(majorVersion.isCloseTo(chromeChannel.majorVersion, majorVersionTolerance)) {
                "Invalid browser major version for [$chromeChannel]: Was $version. But expected major version ${chromeChannel.majorVersion} (tolerance $majorVersionTolerance). Update the version at ${ChromeChannel::class.qualifiedName}(${ChromeChannel::class.simpleName}.kt:21) if necessary."
              }
            } else {
              //Check the corresponding version in [it.neckar.playwright.chrome.ChromeChannel]
              require(majorVersion == chromeChannel.majorVersion) {
                "Invalid browser version for [$chromeChannel]: Was $version. But expected major version ${chromeChannel.majorVersion}. Update the version at ${ChromeChannel::class.qualifiedName}(${ChromeChannel::class.simpleName}.kt:21) if necessary."
              }
            }
          }
        }
      }
    }

    private val logger: Logger = LoggerFactory.getLogger("com.meistercharts.tests.e2e.PlaywrightSupport")
  }
}

