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

import it.neckar.open.annotations.AnyThread
import it.neckar.open.exceptions.ApplicationException
import it.neckar.open.exceptions.NotificationException
import org.slf4j.LoggerFactory
import java.io.IOException
import java.net.NoRouteToHostException
import java.util.concurrent.RejectedExecutionException
import jakarta.inject.Inject
import jakarta.inject.Singleton

/**
 * Silently handles exceptions
 */
@Singleton
class SilentExceptionHandler @Inject
constructor(
  private val exceptionReporter: ExceptionReporter
) : ExceptionHandler() {

  @AnyThread
  override fun handle(thread: Thread, throwable: Throwable, original: Throwable) {
    if (throwable is ApplicationException) {
      ignoreException(throwable, original)
      return
    }

    if (throwable is RejectedExecutionException) {
      handleRejectedExecutionException(original)
      return
    }

    if (throwable is NoRouteToHostException) {
      ignoreException(throwable, original)
      return
    }

    if (throwable is IOException) {
      reportException(throwable)
      return
    }

    if (throwable is NotificationException) {
      ignoreException(throwable, original)
      return
    }

    reportException(throwable)
  }

  private fun handleRejectedExecutionException(original: Throwable) {
    reportException(original)
  }

  private fun reportException(throwable: Throwable) {
    exceptionReporter.report(throwable)
  }

  companion object {
    private val LOG = LoggerFactory.getLogger(SilentExceptionHandler::class.java.name)

    private fun ignoreException(throwable: Throwable, original: Throwable) {
      LOG.info("throwable <{}>, original = <{}>, will be ignored", throwable, original)
    }
  }
}
