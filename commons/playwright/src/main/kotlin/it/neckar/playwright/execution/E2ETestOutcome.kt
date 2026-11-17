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

import it.neckar.playwright.UniqueName
import it.neckar.playwright.screenshots.ScreenshotComparisonError
import it.neckar.playwright.screenshots.ScreenshotConsoleWarning
import java.io.File

/**
 * What one [ExecutableE2ETest] produced - the screenshots it captured and the errors that stopped it.
 *
 * One outcome per test, no matter how many screenshots the test takes, so a run reports per test instead of collapsing
 * into one verdict for all of them.
 */
data class E2ETestOutcome(
  /**
   * The test this outcome belongs to.
   */
  val name: UniqueName,

  /**
   * The screenshots that were captured - the ones taken before an error are kept.
   */
  val screenshotFiles: List<File>,

  /**
   * Empty for a test that passed.
   */
  val errors: List<ScreenshotComparisonError>,

  /**
   * Console errors of the observed page. They are reported, they do not fail the test.
   */
  val consoleWarnings: List<ScreenshotConsoleWarning>,
) {
  val failed: Boolean
    get() = errors.isNotEmpty()

  /**
   * The failure text for the assertion of this test.
   */
  fun errorsAsString(): String {
    return errors.joinToString("\n") { it.toString() }
  }
}
