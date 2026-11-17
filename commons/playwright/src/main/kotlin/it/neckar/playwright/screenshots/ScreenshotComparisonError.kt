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
package it.neckar.playwright.screenshots

import it.neckar.playwright.UniqueName
import java.io.File


/**
 * Results of a screenshot comparison
 */
sealed interface ScreenshotComparisonError {
  /**
   * The name of the screenshot
   */
  val name: UniqueName

  /**
   * The base screenshot is missing
   */
  data class BaselineScreenshotMissing(
    /**
     * The name of the test
     */
    override val name: UniqueName,
    /**
     * The expected screenshot file (that does *not* exist)
     */
    val expectedScreenshotFile: File,
    /**
     * The actual screenshot file
     */
    val currentScreenshotFile: File,
  ) : ScreenshotComparisonError {
    override fun toString(): String {
      return "Baseline Screenshot Missing for $name: $expectedScreenshotFile"
    }
  }

  /**
   * There has been a difference detected between the baseline and the actual screenshot
   */
  data class DifferenceDetected(
    override val name: UniqueName,
    val comparisonInfo: ScreenshotDifferenceInfo,
  ) : ScreenshotComparisonError {
    override fun toString(): String {
      return "Difference Detected in $name: $comparisonInfo"
    }
  }

  /**
   * Represents an error that occurred during the screenshot comparison.
   */
  data class OtherError(override val name: UniqueName, val error: Throwable) : ScreenshotComparisonError
}
