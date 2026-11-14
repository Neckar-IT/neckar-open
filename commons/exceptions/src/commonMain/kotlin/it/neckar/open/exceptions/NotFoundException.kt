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
 * Is thrown if a resource was not found.
 * Can be mapped to 404 automatically
 */
class NotFoundException : RuntimeException {
  /**
   *
   * Constructor for NotFoundException.
   *
   * @param message a String object.
   */
  constructor(message: String) : super(message)

  /**
   *
   * Constructor for NotFoundException.
   *
   * @param message a String object.
   * @param cause   a Throwable object.
   */
  constructor(message: String, cause: Throwable) : super(message, cause)

  /**
   *
   * Constructor for NotFoundException.
   *
   * @param cause a Throwable object.
   */
  constructor(cause: Throwable) : super(cause)
}

/**
 * Checks whether the given value is null and throws a [NotFoundException] if it is.
 *
 * @param lazyMessage a lazy message that will be evaluated only if the value is null
 * @return the value itself if it is not null
 * @throws NotFoundException if the value is null
 */
fun <T> T?.checkFound(lazyMessage: () -> String): T {
  if (this == null) {
    throw NotFoundException(lazyMessage())
  }
  return this
}
