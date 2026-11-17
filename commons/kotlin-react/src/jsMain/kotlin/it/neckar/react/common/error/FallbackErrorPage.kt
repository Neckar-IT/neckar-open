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
package it.neckar.react.common.error

import it.neckar.commons.kotlin.js.safeGet
import it.neckar.logging.Logger
import it.neckar.logging.LoggerFactory
import it.neckar.logging.debug
import it.neckar.react.common.FontAwesome.faBug
import react.*
import react.dom.*

private val logger: Logger = LoggerFactory.getLogger("it.neckar.react.common.error.FallbackErrorPage")


/**
 * Temporary page that is shown while the essentials are loaded
 */
val FallbackErrorPage: FC<FallbackOnErrorProps> = fc("FallbackErrorPage") { props ->
  logger.debug { "Rendering FallbackErrorPage" }

  val contactEmail: String = props::contactEmail.safeGet()
  val contactPhoneNumber: String = props::contactPhoneNumber.safeGet()
  val throwable: Throwable? = props::throwable.safeGet()
  val logoutAction: (() -> Unit)? = props::logoutAction.safeGet()

  logger.debug { "Throwable: $throwable" }
  logger.debug { contactEmail }
  logger.debug { contactPhoneNumber }
  logger.debug { "logoutAction: $logoutAction" }

  /**
   * no safeGet because of Interface
   */
  val errorInfo = props.errorInfo

  div {
    main {
      div("container p-4") {

        ErrorPageHeader {
          attrs {
            this.logoutAction = logoutAction
          }
        }

        div {
          h3 {
            span("pe-2") { faBug() }
            +"Ein unerwarteter Fehler ist aufgetreten"
            span("ps-2") { faBug() }
          }

          throwable?.let {
            h4 {
              +(it.message ?: "Unbekannter Fehler")
            }

            div(classes = "pt-3") {
              p {
                +"Es ist ein unbekannter Fehler aufgetreten. Diesen Fehler bitte melden:"

                a(classes = "ps-2") {
                  i(classes = "fa-solid fa-envelope") {}
                  attrs {
                    href = "mailto:$contactEmail"
                  }
                }
              }

              p {
                +"Telefonischer Support: $contactPhoneNumber"
              }

              details {
                pre {
                  +it.stackTraceToString()
                }
              }
            }

          } ?: p {
            +"Keine Fehlerinformationen verfügbar"
          }

          errorInfo?.let {
            h4 {
              +"Component Stack:"
            }
            pre {
              +(it.componentStack ?: "-")
            }
          }
        }
      }
    }
  }
}


external interface FallbackOnErrorProps : Props {
  var throwable: Throwable?
  var errorInfo: ErrorInfo?

  var contactEmail: String
  var contactPhoneNumber: String

  var logoutAction: (() -> Unit)?
}
