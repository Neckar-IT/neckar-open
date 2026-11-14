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
package it.neckar.react.common.router.key

import it.neckar.events.KeyStroke
import it.neckar.logging.Logger
import it.neckar.logging.LoggerFactory
import it.neckar.react.common.router.*

/**
 * Handles key strokes for navigation elements
 */
class NavigationKeyStrokeHandler {
  private val keyStroke2Action = mutableMapOf<KeyStroke, NavigationElement>()

  /**
   * Registers a key stroke for a navigation element
   */
  fun registerKeyStroke(navigationElement: NavigationElement, keyStroke: KeyStroke) {
    logger.debug("Registering key stroke $keyStroke for navigation element ${navigationElement.completePath()}")
    keyStroke2Action[keyStroke] = navigationElement
  }

  /**
   * Should be called for each keystroke
   */
  fun handleKeyStroke(keyStroke: KeyStroke, navigateUrl: NavigateUrlFunction): NavigationResult {
    val navigationElement = keyStroke2Action[keyStroke] ?: return NavigationResult.Ignored

    logger.debug("Found navigation element for [$keyStroke]: ${navigationElement.completePath()}")
    navigateUrl(NavigateUrl(navigationElement.completePath()))
    return NavigationResult.Handled
  }

  companion object {
    private val logger: Logger = LoggerFactory.getLogger("it.neckar.react.common.router.key.NavigationKeyStrokeHandler")
  }

  enum class NavigationResult {
    Handled,
    Ignored,

  }
}
