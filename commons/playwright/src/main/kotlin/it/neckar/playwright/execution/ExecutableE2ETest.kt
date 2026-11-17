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
import it.neckar.playwright.PlaywrightSupportInstance
import it.neckar.playwright.UniqueName
import it.neckar.playwright.screenshots.ScreenshotConsoleWarning
import it.neckar.playwright.screenshots.ScreenshotResult
import it.neckar.uuid.HasUuid
import kotlinx.coroutines.channels.Channel

/**
 * Represents an executable end-to-end test.
 */
interface ExecutableE2ETest : HasUuid {
  /**
   * Names the test itself - the screenshot names derive from it.
   */
  val name: UniqueName

  /**
   * Returns the unique name of the test.
   * Is used for file names, etc.
   */
  fun uniqueName(screenshotIndex: ScreenshotIndex): UniqueName

  /**
   * Creates the screenshot for the given viewport size.
   * The error channel can be used to send errors while in preparePageForCapture.
   */
  context(playwrightSupportInstance: PlaywrightSupportInstance)
  fun execute(viewportSize: Size, consoleWarningsChannel: Channel<ScreenshotConsoleWarning>? = null): List<ScreenshotResult>


  /**
   * The index of the screenshot in the sequence of screenshots.
   */
  @JvmInline
  value class ScreenshotIndex(val value: Int) {
    override fun toString(): String {
      return value.toString()
    }

    companion object{
      val unknown: ScreenshotIndex = ScreenshotIndex(-1)
      val Zero: ScreenshotIndex = ScreenshotIndex(0)
    }
  }
}

