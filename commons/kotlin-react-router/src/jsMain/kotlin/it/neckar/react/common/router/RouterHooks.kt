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
package it.neckar.react.common.router

import kotlin.uuid.Uuid
import it.neckar.logging.Logger
import it.neckar.logging.LoggerFactory
import it.neckar.uuid.decodeFromUrl
import react.router.*

/**
 * Extracts the variable from params
 */
fun useFromParams(variableName: RouterVar): String {
  return requireNotNull(useParams()[variableName.value]) {
    "No parameter found for <$variableName>"
  }
}

/**
 * Returns a parameter value as int
 */
fun useFromParamsInt(variableName: RouterVar): Int {
  val valueAsString = useFromParams(variableName)
  return checkNotNull(valueAsString.toIntOrNull()) {
    "Could not parse value <$valueAsString> for variable $variableName to int"
  }
}

/**
 * Extracts a UUID from params
 * Throws an exception if the UUID could not be found
 */
fun useFromParamsUuid(variableName: RouterVar): Uuid {
  return Uuid.parse(useFromParams(variableName))
}

/**
 * Extract a Base64 encoded param and decodes it back
 * to a UUID
 * */
fun useFromParamsUuidBase64(variableName: RouterVar): Uuid {
  return Uuid.decodeFromUrl(useFromParams(variableName))
}


/**
 * Custom useNavigateHook that is type safe
 */
fun useNavigateUrl(): NavigateUrlFunction {
  val navigate: NavigateFunction = useNavigate()
  return NavigateUrlFunction(navigate)
}

/**
 * Type alias for the navigate function
 */
value class NavigateUrlFunction(
  private val navigateFunction: NavigateFunction,
) {

  operator fun invoke(url: NavigateUrl) {
    logger.debug("Navigating to <$url>")
    navigateFunction(url.value)
  }

  fun back() {
    navigateFunction(-1)
  }
}

private val logger: Logger = LoggerFactory.getLogger("it.neckar.react.common.router.RouterHooks")
