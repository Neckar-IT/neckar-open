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

import com.google.common.collect.ImmutableMap
import it.neckar.open.annotations.AnyThread
import it.neckar.open.exceptions.ApplicationException
import it.neckar.open.exceptions.NotificationException
import it.neckar.open.exceptions.handling.notification.DetailsCallback
import it.neckar.open.exceptions.handling.notification.FxNotificationService
import it.neckar.open.exceptions.handling.notification.Notification
import it.neckar.open.javafx.FxUtils
import it.neckar.open.version.Version
import javafx.application.Platform
import javafx.stage.Stage
import org.slf4j.LoggerFactory
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean

/**
 * JavaFX based exception handler
 */
open class FxExceptionHandler @JvmOverloads constructor(
  private val notificationService: FxNotificationService,
  val applicationVersion: Version,
  val exceptionReporter: ExceptionReporter,
  exceptionTypeHandlers: Map<Class<out Throwable>, ExceptionTypeHandler> = HashMap()
) :
  ExceptionHandler() {

  private val exceptionTypeHandlers: Map<Class<out Throwable>, ExceptionTypeHandler> = ImmutableMap.copyOf(exceptionTypeHandlers)

  /**
   * Is set to true if a dialog is currently open
   */
  private val dialogOpen = AtomicBoolean(false)

  override fun handle(thread: Thread, throwable: Throwable, original: Throwable) {
    if (dialogOpen.get()) {
      //Dialog already open, ignore additional exceptions
      return
    }

    val exceptionTypeHandler = exceptionTypeHandlers[throwable.javaClass]
    if (exceptionTypeHandler != null) {
      exceptionTypeHandler.handle(this, thread, throwable, original)
      return
    }

    if (throwable is ApplicationException) {
      showApplicationExceptionDialog(throwable)
      return
    }

    if (throwable is IOException) {
      handleIoException(throwable, original)
      return
    }

    if (throwable is NotificationException) {
      handleNotificationException(throwable, original)
      return
    }

    handleInternalError(throwable, original)
  }

  fun handleNotificationException(throwable: NotificationException, original: Throwable) {
    Platform.runLater {
      val notification = Notification(throwable.title, throwable.message.orEmpty(), object : DetailsCallback {
        override fun detailsClicked(notification: Notification) {
          handleInternalError(original, original)
        }
      })
      notificationService.showNotification(notification)
    }
  }

  open fun handleIoException(throwable: IOException, original: Throwable) {
    showExceptionDialog(original, FxUtils.stageSafe)
  }

  /**
   * Shows an internal error dialog
   *
   * @param throwable the (purged) exception
   * @param original  the original exception
   */
  open fun handleInternalError(throwable: Throwable, original: Throwable) {
    showExceptionDialog(original, FxUtils.stageSafe)
  }

  @AnyThread
  private fun showExceptionDialog(original: Throwable, owner: Stage?) {
    Platform.runLater(Runnable {
      try {
        if (dialogOpen.get()) {
          return@Runnable
        }

        try {
          dialogOpen.set(true)
          val internalExceptionDialog = InternalExceptionFxDialog(original)
          internalExceptionDialog.initOwner(owner)
          internalExceptionDialog.showAndWait()
        } finally {
          dialogOpen.set(false)
        }
      } catch (e: Throwable) {
        LOG.error("Error on handling exception", e)
      }
    })
  }

  /**
   * Shows the application exception dialog
   */
  @AnyThread
  fun showApplicationExceptionDialog(applicationException: ApplicationException) {
    Platform.runLater {
      if (dialogOpen.get()) {
        return@runLater
      }

      try {
        dialogOpen.set(true)
        val dialog = ApplicationExceptionFxDialog(applicationException)
        dialog.initOwner(FxUtils.stageSafe)
        dialog.show()
      } finally {
        dialogOpen.set(false)
      }
    }
  }

  /**
   * Implementing classes support an exception
   */
  @FunctionalInterface
  interface ExceptionTypeHandler {
    fun handle(exceptionHandler: FxExceptionHandler, thread: Thread, throwable: Throwable, original: Throwable)
  }

  companion object {
    private val LOG = LoggerFactory.getLogger(FxExceptionHandler::class.java.name)
  }
}
