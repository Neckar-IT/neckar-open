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
package it.neckar.react.common.exception

import it.neckar.commons.kotlin.js.exception.ConsoleJsErrorHandler
import it.neckar.commons.kotlin.js.exception.JsErrorHandler
import it.neckar.commons.kotlin.js.exception.JsErrorHandlerMultiplexer
import it.neckar.commons.kotlin.js.exception.errorHandler
import it.neckar.ktor.client.isNetworkFailure
import it.neckar.open.exceptions.PermissionDeniedException
import it.neckar.react.common.toast.*

/**
 * Handles exceptions by showing a toast message
 */
class ToastExceptionHandler(
  val formatter: ExceptionFormatter = StandardExceptionsFormatter,
) : JsErrorHandler {
  override fun otherError(message: dynamic, error: Any) {
    notifyError("Ein Fehler ist aufgetreten", message)
  }

  override fun nullError(message: dynamic) {
    // Fallback path for non-Throwable runtime errors (e.g. a Promise rejected with `null`).
    // Showing a generic toast keeps the user informed without crashing the error pipeline.
    notifyError("Ein Fehler ist aufgetreten", message ?: "Unbekannter Fehler")
  }

  override fun error(throwable: Throwable) {
    val info = formatter.format(throwable) ?: fallbackFormat(throwable)

    when (info.severity) {
      TranslatedExceptionInfo.Severity.Info -> Toast.info(info.message, info.title, ToastOptions(positionClass = info.toastPosition, timeOut = 5000))
      TranslatedExceptionInfo.Severity.Success -> Toast.success(info.message, info.title, ToastOptions(positionClass = info.toastPosition, timeOut = 5000))
      TranslatedExceptionInfo.Severity.Warning -> Toast.warning(info.message, info.title, ToastOptions(positionClass = info.toastPosition, timeOut = 5000))
      TranslatedExceptionInfo.Severity.Error -> Toast.error(info.message, info.title, ToastOptions(positionClass = info.toastPosition, timeOut = 5000))
    }
  }

  fun fallbackFormat(throwable: Throwable): TranslatedExceptionInfo {
    console.info("Falling back to default error handling for <${throwable::class.simpleName}>")
    return TranslatedExceptionInfo("Fehler", "Ein Fehler ist aufgetreten:<br>${throwable.message}", TranslatedExceptionInfo.Severity.Error)
  }

  companion object {
    /**
     * Installs this exception handler
     */
    fun install(formatter: ExceptionFormatter = StandardExceptionsFormatter) {
      JsErrorHandler.registerWindowErrorHandler(
        JsErrorHandlerMultiplexer(listOf(ConsoleJsErrorHandler, ToastExceptionHandler(formatter)))
      )
    }
  }
}

/**
 * Translates / formats exceptions
 */
interface ExceptionFormatter {
  /**
   * Returns the translation for the given [throwable].
   * If null is returned a fallback formatting will be used
   */
  fun format(throwable: Throwable): TranslatedExceptionInfo?
}

/**
 * Contains information about an error that can be shown in a toast
 */
data class TranslatedExceptionInfo(
  val title: String,
  val message: String,
  val severity: Severity,
  /**
   * The position for the position
   */
  val toastPosition: ToastPosition = ToastPosition.BOTTOMCENTER,
) {

  /**
   * The severity of the message
   */
  enum class Severity {
    Info,
    Success,
    Warning,
    Error
  }
}

/**
 * Handles some well known standard exceptions
 */
object StandardExceptionsFormatter : ExceptionFormatter {
  override fun format(throwable: Throwable): TranslatedExceptionInfo? {
    if (throwable.isNetworkFailure()) {
      return TranslatedExceptionInfo("Netzwerkfehler", "Der Server ist aktuell nicht erreichbar", TranslatedExceptionInfo.Severity.Error)
    }

    if (throwable is PermissionDeniedException) {
      return TranslatedExceptionInfo("Zugriff nicht erlaubt", "Der Zugriff wurde verweigert.", TranslatedExceptionInfo.Severity.Error)
    }

    return null
  }
}
