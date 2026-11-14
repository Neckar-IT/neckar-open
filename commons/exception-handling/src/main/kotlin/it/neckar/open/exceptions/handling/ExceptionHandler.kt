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
package it.neckar.open.exceptions.handling

import it.neckar.logging.Logger
import it.neckar.logging.LoggerDelegate
import it.neckar.open.exceptions.CanceledException
import javax.annotation.PostConstruct
import javax.swing.JOptionPane
import javax.swing.SwingUtilities
import kotlin.system.exitProcess

/**
 * Handles exceptions
 */
abstract class ExceptionHandler : Thread.UncaughtExceptionHandler {
  /**
   * Registers the exception handler for all threads.
   */
  @PostConstruct
  fun registerExceptionHandler() {
    Thread.setDefaultUncaughtExceptionHandler(this)
  }

  override fun uncaughtException(t: Thread, e: Throwable) {
    try {
      val purged = ExceptionPurger.purge(e)
      logger.info("Handling Exception", e)

      handle(t, purged, e)
    } catch (_: CanceledException) {
    } catch (throwable: Throwable) {
      logger.error("could not handle exception", throwable)
      logger.error("original exception", e)

      //Last resort if exception handling no longer works
      try {
        SwingUtilities.invokeLater {
          JOptionPane.showMessageDialog(null, "Internal problem. Please contact our technical support.\nThe application will be closed.\n\n" + extractDetailsFromThrowable(e), "Internal Problem", JOptionPane.ERROR_MESSAGE)
          exitProcess(1)
        }
      } catch (lastException: Exception) {
        logger.error("Could not show last exception dialog", lastException)
      }
    }
  }

  /**
   * Handles the given throwable
   *
   * @param thread    the thread the throwable has occurred in
   * @param throwable the throwable (purged)
   * @param original  the original throwable
   */
  protected abstract fun handle(thread: Thread, throwable: Throwable, original: Throwable)

  companion object {
    private val logger: Logger by LoggerDelegate()

    /**
     * Extracts the details if possible
     */
    private fun extractDetailsFromThrowable(t: Throwable): String {
      try {
        return t.javaClass.name + ": " + t.message
      } catch (_: Throwable) {
      }

      return "<undefined>"
    }
  }
}
