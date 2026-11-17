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
import it.neckar.open.collections.fastForEach
import it.neckar.react.common.*
import it.neckar.react.common.FontAwesome.faSort
import it.neckar.react.common.FontAwesome.faSortDown
import it.neckar.react.common.FontAwesome.faSortUp
import kotlinx.html.ThScope
import kotlinx.html.js.onClickFunction
import react.*
import react.dom.*

/**
 *
 */
fun <T> RBuilder.remoteTable(
  tableClasses: String = "table table-hover table-responsive",
  /**
   * List of information how what columns to display and how to format their data
   */
  columns: List<TableColumn<T>>,

  /**
   * List of the [T]s to be displayed
   */
  entries: List<T>,
  footerEntry: T? = null,

  sortedByFunction: StateInstance<SortedByFunction<T>>,

  handler: (RemoteTableProps) -> Unit = {},

  ): Unit = child(RemoteTable) {
  attrs {
    this.tableClasses = tableClasses
    this.columns = columns.unsafeCast<List<TableColumn<Any>>>()
    this.tableEntries = entries.unsafeCast<List<Any>>()
    this.footerEntry = footerEntry.unsafeCast<Any?>()
    this.sortedByFunction = sortedByFunction.unsafeCast<StateInstance<SortedByFunction<Any>>>()

    handler(this)
  }
}

val RemoteTable: FC<RemoteTableProps> = fc("RemoteTable") { props ->
  val tableClasses = props::tableClasses.safeGet()
  val columns = props::columns.safeGet()
  val tableEntries = props::tableEntries.safeGet()
  val footerEntry = props::footerEntry.safeGet()
  val (sortedByFunction, sortedByFunctionSetter) = props::sortedByFunction.getNotNull()


  table(classes = tableClasses) {

    /**
     * Generate the Table Header
     */
    thead {
      tr {

        columns.fastForEach { tableColumn ->
          val title = tableColumn.title
          val sortFunction = tableColumn.sortFunction

          th(scope = ThScope.col) {
            attrs {
              addClass(tableColumn.titleClasses)
              addClassIf("pointer") { sortFunction != null }
              if (sortFunction != null) {
                onClickFunction = {
                  val newSortedBy = sortedByFunction.let { if (it.isSortedBy(tableColumn)) it.sortedBy.next() else SortedBy.SortedAscending }
                  sortedByFunctionSetter(
                    sortedByFunction.copy(
                      sortedBy = newSortedBy,
                      sortedColumn = tableColumn,
                    )
                  )
                }
              }
            }

            div(classes = "d-flex") {
              title?.let { +it }

              if (sortFunction != null) {
                span("ps-2") {
                  if (sortedByFunction.isSortedBy(tableColumn)) {
                    addClassIf("text-primary") { true }
                    when (sortedByFunction.sortedBy) {
                      SortedBy.Unsorted -> faSort()
                      SortedBy.SortedAscending -> faSortUp()
                      SortedBy.SortedDescending -> faSortDown()
                    }
                  } else {
                    addClassIf("text-secondary") { true }
                    faSort()
                  }
                }
              }
            }

          }
        }

      }
    }

    /**
     * Generate the Table Body
     */
    tbody {
      tableEntries.fastForEach { entry ->
        tr {

          columns.fastForEach { tableColumn ->
            when (tableColumn) {
              is TableHeaderColumn -> th(scope = ThScope.row, block = tableColumn.columnContent(entry))
              is TableDataColumn -> td(block = tableColumn.columnContent(entry))
            }
          }

        }
      }
    }

    footerEntry?.let { entry ->
      tfoot {
        tr {

          columns.fastForEach { tableColumn ->
            when (tableColumn) {
              is TableHeaderColumn -> th(scope = ThScope.row, block = tableColumn.columnContent(entry))
              is TableDataColumn -> td(block = tableColumn.columnContent(entry))
            }
          }

        }
      }
    }

  }

}

external interface RemoteTableProps : Props {
  var tableClasses: String

  /**
   * List of information what columns display, their headers, and how to format their data
   */
  var columns: List<TableColumn<Any>>

  /**
   * List of the entries to be displayed
   *  can be null and shows busy indicator
   */
  var tableEntries: List<Any>
  var footerEntry: Any?

  var sortedByFunction: StateInstance<SortedByFunction<Any>>
}
