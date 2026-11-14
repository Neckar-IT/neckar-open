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
package it.neckar.react.common.table

import it.neckar.commons.kotlin.js.getNotNull
import it.neckar.commons.kotlin.js.safeGet
import it.neckar.open.kotlin.lang.fastFor
import it.neckar.open.kotlin.lang.toIntCeil
import it.neckar.open.kotlin.lang.toIntFloor
import it.neckar.react.common.*
import kotlinx.html.js.onClickFunction
import react.*
import react.dom.*

/**
 *
 */
fun RBuilder.pagination(currentPage: StateInstance<Int>, entryPagesCount: Int, paginationGroupSize: Int = 3): Unit = child(Pagination) {
  attrs {
    this.currentPage = currentPage
    this.entryPagesCount = entryPagesCount
    this.paginationGroupSize = paginationGroupSize
  }
}


val Pagination: FC<PaginationProps> = fc("Pagination") { props ->
  val currentPage = props::currentPage.getNotNull()
  val entryPagesCount = props::entryPagesCount.safeGet()
  val paginationGroupSize = props::paginationGroupSize.safeGet()


  nav {
    ul("pagination justify-content-center") {
      li("page-item") {
        addClassIf("disabled") { currentPage.value <= 0 }

        attrs {
          onClickFunction = {
            currentPage.setter { oldPage -> (oldPage - 1).coerceAtLeast(0) }
          }
        }

        div(classes = "page-link") {
          span("fas fa-angle-left") {}
        }
      }

      /**
       * If there are only a few pages, just display the normal pagination
       */
      if (entryPagesCount <= paginationGroupSize * 3) {
        entryPagesCount.fastFor { page ->
          tablePaginationButton(page, currentPage)
        }
      } else {
        /**
         * Display the first [paginationGroupSize] buttons
         */
        for (page in 0..<paginationGroupSize) {
          tablePaginationButton(page, currentPage)
        }

        /**
         * Add a separator if necessary
         */
        if (currentPage.value >= (paginationGroupSize * 1.5).toIntCeil()) {
          li("page-item disabled") {
            div(classes = "page-link") {
              +"..."
            }
          }
        }

        /**
         * Fit a button group in the middle of the currently selected page is somewhere in the middle
         */
        if (currentPage.value in (paginationGroupSize - 1)..(entryPagesCount - paginationGroupSize + 1)) {
          val from = (currentPage.value - (paginationGroupSize * 0.5).toIntFloor()).coerceAtLeast(paginationGroupSize)
          val to = (currentPage.value + (paginationGroupSize * 0.5).toIntFloor()).coerceAtMost(entryPagesCount - paginationGroupSize - 1)
          for (page in from..to) {
            tablePaginationButton(page, currentPage)
          }
        }

        /**
         * Add a separator if necessary
         */
        if (currentPage.value < (entryPagesCount - paginationGroupSize * 1.5).toIntFloor() - 1) {
          li("page-item disabled") {
            div(classes = "page-link") {
              +"..."
            }
          }
        }

        /**
         * Display the last [paginationGroupSize] buttons
         */
        for (page in (entryPagesCount - paginationGroupSize)..<entryPagesCount) {
          tablePaginationButton(page, currentPage)
        }
      }

      li("page-item") {
        addClassIf("disabled") { currentPage.value >= entryPagesCount - 1 }

        attrs {
          onClickFunction = {
            currentPage.setter { oldPage -> (oldPage + 1).coerceAtMost(entryPagesCount - 1) }
          }
        }

        div(classes = "page-link") {
          span("fas fa-angle-right") {}
        }
      }
    }
  }
}

private fun RBuilder.tablePaginationButton(page: Int, currentPage: StateInstance<Int>) {
  li("page-item") {
    addClassIf("active") { page == currentPage.value }

    attrs {
      onClickFunction = {
        currentPage.setter(page)
      }
    }

    div(classes = "page-link") {
      +"${page + 1}"
    }
  }
}


external interface PaginationProps : Props {
  var entryPagesCount: Int
  var currentPage: StateInstance<Int>
  var paginationGroupSize: Int
}
