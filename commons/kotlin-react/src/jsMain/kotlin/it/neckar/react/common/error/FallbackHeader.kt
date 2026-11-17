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
@file:Suppress("DuplicatedCode")

package it.neckar.react.common.error

import it.neckar.commons.kotlin.js.safeGet
import it.neckar.react.common.FontAwesome.faReload
import it.neckar.react.common.FontAwesome.faSignOut
import kotlinx.html.ButtonType
import kotlinx.html.js.onClickFunction
import react.*
import react.dom.*

val ErrorPageHeader: FC<ErrorPageHeaderProps> = fc("ErrorPageHeader") { props ->
  val logoutAction = props::logoutAction.safeGet()
  val showReload = props.showReload ?: true

  nav("py-2 bg-light border-bottom") {
    div("container d-flex flex-wrap") {

      ul("nav mx-3") {
        if (showReload) {
          li("nav-item") {

            a(href = "/", classes = "btn btn-link nav-link link-dark px-2") {
              span("pe-2") {
                faReload()
              }
              +"Neu laden"
            }
          }
        }

        if (logoutAction != null) {
          li("nav-item") {
            button(classes = "btn btn-link nav-link link-dark px-2", type = ButtonType.button) {
              span("pe-2") {
                faSignOut()
              }
              +"Logout"
              attrs {
                onClickFunction = {
                  it.preventDefault()
                  logoutAction()
                }
              }
            }
          }
        }
      }
    }
  }
}

external interface ErrorPageHeaderProps : Props {
  var showReload: Boolean?
  var logoutAction: (() -> Unit)?
}
