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

import com.microsoft.playwright.Browser
import com.microsoft.playwright.Locator
import com.microsoft.playwright.Page
import com.microsoft.playwright.options.ElementState
import com.microsoft.playwright.options.WaitForSelectorState
import it.neckar.logging.LoggerDelegate
import it.neckar.playwright.screenshots.ScreenshotConsoleWarning
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.*

/**
 * Helper class for playwright
 */
class PlaywrightUtils(val browser: Browser) {

  fun userAgent(): String {
    browser.newPage().use { page ->
      val userAgent = (page.evaluate("() => {return navigator.userAgent}") as String).replace("Headless", "")
      logger.debug("WebView User-Agent is $userAgent")
      return userAgent
    }
  }

  companion object {
    private val logger: it.neckar.logging.Logger by LoggerDelegate()
  }
}

/**
 * Waits until the exact given text is stable and visible on the given page.
 * Throws a TimeoutError after 30 seconds
 * @param last if true, the last element with the given text will be searched for. Otherwise, an exception will be thrown if multiple elements with the given text are found. Set to true by default.
 */
fun waitForStableText(page: Page, text: String, last: Boolean = true) {
  val textLocator: Locator = if (last) page.getByText(text, Page.GetByTextOptions().setExact(true)).last() else page.getByText(text, Page.GetByTextOptions().setExact(true))
  textLocator.waitFor(Locator.WaitForOptions().setState(WaitForSelectorState.VISIBLE))
  textLocator.elementHandle().waitForElementState(ElementState.STABLE)
}


/**
 * sorts the console messages, and sends errors to the given error channel
 */
fun forwardConsoleErrors(page: Page, fileName: String, errorsChannel: Channel<ScreenshotConsoleWarning>?) {
  val messages = page.consoleMessages()
  messages.forEach { consoleMessage ->
    if (consoleMessage.isError()) {
      runBlocking {
        errorsChannel?.send(ScreenshotConsoleWarning.Warning(UniqueName(fileName), consoleMessage.text(), fileName))
      }
    }
  }
}


