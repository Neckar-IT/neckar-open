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

import it.neckar.open.exceptions.CanceledException
import com.google.common.collect.ImmutableSet
import com.google.common.util.concurrent.UncheckedExecutionException
import java.lang.reflect.InvocationTargetException
import java.util.concurrent.ExecutionException

/**
 * This is a utility class that is able to work with nested exceptions.
 *
 */
object ExceptionPurger {
  /**
   * These exceptions are ignored. No dialog is shown for them.
   */
  private val ignored: ImmutableSet<Class<out Throwable>> = ImmutableSet.of(CanceledException::class.java)

  /**
   * These exceptions are purged: They are not reported themselves. Instead the cause is shown in the dialog
   */
  private val purged: ImmutableSet<String> = ImmutableSet.of(
    RuntimeException::class.java.name, InvocationTargetException::class.java.name, ExecutionException::class.java.name, UncheckedExecutionException::class.java.name, "com.google.inject.ProvisionException"
  )

  @JvmStatic
  @Throws(CanceledException::class)
  fun purge(throwable: Throwable): Throwable {
    //Check for ignoring
    if (ignored.contains(throwable.javaClass)) {
      throw CanceledException()
    }

    val cause = throwable.cause

    return if (cause != null && purged.contains(throwable.javaClass.name)) {
      purge(cause)
    } else throwable
  }

  /**
   * Searches the exception tree and returns the first instance of the given type.
   * This method will return the given Throwable if it is an instance of the given type.
   *
   * @param e             the throwable
   * @param throwableType the type that is looked for
   * @param <T>           the type
   * @return the found exception of the given type or null
  </T> */
  fun <T : Throwable> find(e: Throwable?, throwableType: Class<T>): T? {
    var current = e

    while (current != null) {
      if (throwableType.isAssignableFrom(current.javaClass)) {
        return throwableType.cast(current)
      }
      current = if (current.cause === current) null else current.cause
    }

    return null
  }

  /**
   * Returns the root cause of [e] — the deepest non-null entry in the cause chain.
   * Stops on a self-referential cause (`cause === current`) so a corrupt chain
   * cannot loop forever.
   */
  fun getRoot(e: Throwable): Throwable {
    var current = e
    while (true) {
      val cause = current.cause ?: return current
      if (cause === current) return current
      current = cause
    }
  }

  fun getRootMessage(e: Throwable): String? {
    return getRoot(e).message
  }

  /**
   * Returns the localized message of the root - or null if the root has no message
   */
  fun getLocalizedRootMessage(e: Throwable): String? {
    return getRoot(e).localizedMessage
  }
}
