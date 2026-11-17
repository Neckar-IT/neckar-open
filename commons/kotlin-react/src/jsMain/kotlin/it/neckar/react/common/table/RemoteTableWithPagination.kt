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

import it.neckar.commons.kotlin.js.LocalStorageKey
import it.neckar.commons.kotlin.js.LocalStorageKeyPrefix
import it.neckar.commons.kotlin.js.LocalStorageSupport.saveToLocalStorageOptional
import it.neckar.commons.kotlin.js.getNotNull
import it.neckar.commons.kotlin.js.safeGet
import it.neckar.open.kotlin.lang.divCeil
import it.neckar.react.common.*
import it.neckar.react.common.form.*
import react.*
import react.dom.*

/**
 *
 */
fun <T> RBuilder.remoteTableWithPagination(
  tableClasses: String = "table table-hover table-responsive",
  entries: List<T>,
  columns: List<TableColumn<T>>,
  pageSize: StateInstance<Int?>,
  currentPage: StateInstance<Int>,
  numberOfTotalFilteredProjects: Int,
  sortedByFunction: StateInstance<SortedByFunction<T>>,
  keyPrefix: LocalStorageKeyPrefix,
  handler: (RemoteTableProps) -> Unit = {},
): Unit = child(RemoteTableWithPagination) {
  attrs {
    this.tableClasses = tableClasses
    this.entries = entries.unsafeCast<List<Any>>()
    this.columns = columns.unsafeCast<List<TableColumn<Any>>>()
    this.pageSize = pageSize
    this.currentPage = currentPage
    this.numberOfTotalFilteredProjects = numberOfTotalFilteredProjects
    this.sortedByFunction = sortedByFunction.unsafeCast<StateInstance<SortedByFunction<Any>>>()
    this.keyPrefix = keyPrefix
    this.handler = handler.unsafeCast<(RemoteTableProps) -> Unit>()
  }
}

val RemoteTableWithPagination: FC<RemoteTableWithPaginationProps> = fc("RemoteTableWithPagination") { props ->
  val tableClasses = props::tableClasses.safeGet()
  val entries = props::entries.safeGet()
  val columns = props::columns.safeGet()
  val pageSize = props::pageSize.getNotNull()
  val currentPage = props::currentPage.getNotNull()
  val numberOfTotalFilteredProjects = props::numberOfTotalFilteredProjects.getNotNull()
  val sortedByFunction = props::sortedByFunction.getNotNull()
  val keyPrefix = props::keyPrefix.safeGet()
  val handler = props::handler.safeGet()

  val entryPagesCount = pageSize.value?.let { numberOfTotalFilteredProjects.divCeil(it) } ?: 1


  useEffect(currentPage.value) {
    saveToLocalStorageOptional(
      LocalStorageKey("${keyPrefix}${LocalTableStorageKeys.currentPage}"),
      currentPage.value,
    )
  }


  remoteTable(
    tableClasses = tableClasses,
    columns = columns,
    entries = entries,
    sortedByFunction = sortedByFunction,
    handler = handler,
  )

  div("row") {
    div("col-2") {
      floatingSelectNullable(
        valueAndSetter = pageSize,
        idProvider = { int -> int?.toString() ?: "allProjects" },
        formatter = { int -> int?.toString() ?: "ALLE - $numberOfTotalFilteredProjects" },
        availableOptionsWithoutNull = listOf(5, 10, 15, 20, 25, 100),
        fieldName = "pageSizeField",
        title = "Projektanzahl",
        additionalOnChange = { newPageSize ->
          saveToLocalStorageOptional(
            LocalStorageKey("${keyPrefix}${LocalTableStorageKeys.pageSize}"),
            newPageSize,
          )
        },
      )
    }

    div("col-10") {
      pagination(currentPage = currentPage, entryPagesCount = entryPagesCount)
    }
  }

}

external interface RemoteTableWithPaginationProps : Props {
  var tableClasses: String
  var entries: List<Any>
  var columns: List<TableColumn<Any>>
  var pageSize: StateInstance<Int?>
  var currentPage: StateInstance<Int>
  var numberOfTotalFilteredProjects: Int
  var sortedByFunction: StateInstance<SortedByFunction<Any>>
  var keyPrefix: LocalStorageKeyPrefix
  var handler: (RemoteTableProps) -> Unit
}
