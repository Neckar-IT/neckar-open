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
import it.neckar.logging.Logger
import it.neckar.logging.LoggerFactory
import it.neckar.react.common.*
import it.neckar.react.common.button.*
import it.neckar.react.common.error.*
import it.neckar.react.common.featureflags.*
import it.neckar.react.common.toast.*
import react.*
import react.dom.*

private val logger: Logger = LoggerFactory.getLogger("DemoRoot")

val DemoRoot: FC<DemoRootProps> = fc("DemoRoot") { props ->
  errorBoundary(
    errorComponentComponent = { throwable, errorInfo ->
      FallbackErrorPage.create {
        this.throwable = throwable
        this.errorInfo = errorInfo

        this.contactEmail = "planner-support@neckar.it"
        this.contactPhoneNumber = "07473 959 49 60"

        this.logoutAction = null //no logout action
      }
    }) {


    FeatureFlagsContextComponent {
      div("container") {
        div("row") {
          div("col") {
            h2 {
              +"Buttons"
            }
          }
        }

        div("row") {
          div("col") {
            ButtonsDemo {
            }
          }
        }


        div("row") {
          div("col") {
            h2 {
              +"Buggy Components"
            }
          }
        }

        div("row") {
          div("col") {
            BuggyComponentDemo {
            }
          }
        }

        div("row") {
          div("col") {
            h2 {
              +"Progress Bar"
            }
          }
        }

        div("row") {
          div("col") {
            ProgressBarDemo {
            }
          }
        }

        div("row") {
          div("col") {
            h2 {
              +"Table"
            }
          }
        }

        div("row") {
          div("col-6") {
            TableDemo {
            }
          }
        }


        div("row") {
          div("col-6") {
            h3 {
              +"Notification"
            }

            button2(
              label = "Show success",
              action = {
                notifySuccess("It has been a success!!!")
              },
            )

            button2(
              label = "Show error",
              action = {
                notifyError("Oh no!", "Something went wrong!")
              },
            )
          }
        }

        div("row") {
          div("col") {
            h2 {
              +"Busy if null"
            }
          }
        }

        div("row") {
          div("col-6") {
            BusyIfNullDemo {
            }
          }
        }


        div("row") {
          div("col") {
            h2 {
              +"Coroutines"
            }
          }
        }

        div("row") {
          div("col") {
            CoroutinesDemo {
            }
          }
        }


      }
    }
  }
}

external interface DemoRootProps : Props {
}

