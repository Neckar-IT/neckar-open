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

import convertRelease
import it.neckar.react.common.router.*
import kotlinx.browser.window
import org.w3c.dom.events.Event
import org.w3c.dom.events.KeyboardEvent
import react.*

/**
 * Registers a component that catches keystrokes and navigates accordingly
 */
val KeyboardNavigation: FC<KeyboardNavigationProps> = fc("KeyboardNavigation") { props ->
  val navigateUrl = useNavigateUrl()

  val handler = props.handler

  useEffectOnceWithCleanup {
    val listener = { event: Event ->
      // Browser-driven synthetic events (e.g. Chrome Autofill) may fire on `keyup`
      // without being a real KeyboardEvent. Ignore them silently.
      if (event is KeyboardEvent) {
        val keyEvent = event.convertRelease()
        val result = handler.handleKeyStroke(keyEvent.keyStroke, navigateUrl)

        if (result == NavigationKeyStrokeHandler.NavigationResult.Handled) {
          event.stopPropagation()
          event.stopImmediatePropagation()
          event.preventDefault()
        }
      }

      Unit
    }

    window.addEventListener("keyup", listener)

    //Cleanup
    onCleanup {
      window.removeEventListener("keyup", listener)
    }
  }
}


external interface KeyboardNavigationProps : Props {
  var handler: NavigationKeyStrokeHandler
}
