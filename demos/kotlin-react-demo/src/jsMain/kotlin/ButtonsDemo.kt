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
import it.neckar.commons.kotlin.js.debug
import it.neckar.react.common.*
import it.neckar.react.common.button.*
import it.neckar.react.common.form.*
import react.*
import react.dom.*

val ButtonsDemo: FC<ButtonsDemoProps> = fc("ButtonsDemo") { props ->
  div("container") {
    div("row") {
      div("col-sm-3") {
        h4 {
          +"Form Buttons"
        }

        formButtons(
          cancelAction = {
            console.debug("Cancel action")
          }, okAction = {
            console.debug("OK action")
          })
      }

      div("col-sm-3") {
        h4 {
          +"Form Buttons with icons"
        }

        formButtons(
          cancelIcon = ButtonIcon.left("fa fa-times"),
          okIcon = ButtonIcon.left(FontAwesomeIcons.AddUser),

          cancelAction = {
            console.debug("Cancel action")
          }, okAction = {
            console.debug("OK action")
          })
      }

      div("col-sm-3") {
        h4 {
          +"left/right icons"
        }

        formButtons(
          cancelIcon = ButtonIcon.left(FontAwesomeIcons.ArrowLeft),
          cancelText = "Zurück",
          cancelAction = { console.debug("Cancel action") },

          okIcon = ButtonIcon.right(FontAwesomeIcons.ArrowRight),
          okText = "Speichern",
          okAction = { console.debug("OK action") },
        )
      }
    }

    div("row") {
      div("col-sm-3") {
        h4 {
          +"Action Button"
        }

        actionButton(
          icon = ButtonIcon.centered(FontAwesomeIcons.Edit),
          action = {
            console.debug("Action")
          },
        )
      }

      div("col-sm-6") {
        h4 {
          +"Link Button"
        }

        linkButton(
          icon = ButtonIcon.left(FontAwesomeIcons.Edit),
          label = "icon left",
          action = {
            console.debug("Action")
          },
        )

        linkButton(
          icon = ButtonIcon.right(FontAwesomeIcons.Edit),
          label = "icon right",
          action = {
            console.debug("Action")
          },
        )

        linkButton(
          label = "no icon",
          action = {
            console.debug("Action")
          },
        )
      }

      div("col-sm-6") {
        h4 {
          +"Links"
        }

        span("me-2") {
          linkAction(
            label = { "Link with label" },
          ) {
            console.debug("Action")
          }
        }

        span("me-2") {
          linkAction(
            label = { "Link with label and title" },
            title = { "Title" },
          ) {
            console.debug("Action")
          }
        }

        span("me-2") {
          linkAction(
            icon = { FontAwesomeIcons.Edit },
            label = { "Link with label and icon" },
          ) {
            console.debug("Action")
          }
        }

        span("me-2") {
          linkAction(
            icon = { FontAwesomeIcons.Edit },
          ) {
            console.debug("Action")
          }
        }
      }
    }
  }

}

external interface ButtonsDemoProps : Props {
  var daName: String
}
