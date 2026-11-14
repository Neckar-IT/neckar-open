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
package it.neckar.open.exceptions


/**
 * Base class for all "expected" exceptions.
 * These are exceptions that can't be avoided - and we know already about it (e.g. network unavailable, invalid input)
 *
 * @noinspection AbstractClassWithoutAbstractMethods
 */
abstract class ApplicationException
/**
 * Creates a new ApplicationException including a cause (which is never presented to the user).
 */
protected constructor(
  /**
   * The (optional) cause of this exception.
   */
  cause: Throwable?,
) : RuntimeException(cause) {

  /**
   * The error code for this exception.
   * Can be used to identify the exception and translation of error messages.
   */
  abstract val errorCode: ErrorCode

  /**
   * The message arguments
   */
  abstract val parameters: Map<String, Any>?

  /**
   * @return the message (based on the error code)
   */
  override val message: String?
    get() = errorCode.toString()

  /**
   * Returns the fallback message (not translated)
   */
  abstract val fallbackMessage: String
}
