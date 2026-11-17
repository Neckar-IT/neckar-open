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

import it.neckar.react.common.*
import it.neckar.react.common.form.*
import react.*
import react.dom.*

/**
 *
 */
fun <T> RBuilder.tableWithPagination(
  tableClasses: String = "table table-hover table-responsive",
  entries: List<T>,
  columns: List<TableColumn<T>>,
  initialPageSize: Int,
  initiallySortedBy: SortedByFunction<T>,
  handler: (TableProps) -> Unit = {},
) {

  val pageSize: StateInstance<Int?> = useState(initialPageSize)
  val currentPage = useState(0)
  val sortedByFunction = useState(initiallySortedBy)

  val entryPages: List<List<T>> = useMemo(entries, pageSize.value, sortedByFunction.value) {
    sortedByFunction.value.getSortedData(entries).chunked(pageSize.value ?: entries.size.coerceAtLeast(1))
  }

  /**
   * if the list of projects is empty pass empty list to the table
   * */
  val entriesOnCurrentPage = useMemo(entryPages, entries, currentPage.value) {
    if (entries.isEmpty()) {
      emptyList()
    } else {
      entryPages[currentPage.value.coerceAtMost((entryPages.size - 1).coerceAtLeast(0))]
    }
  }


  useMemo(entryPages.size) {
    currentPage.setter(currentPage.value.coerceAtMost((entryPages.size - 1).coerceAtLeast(0)))
  }


  table(
    tableClasses = tableClasses,
    columns = columns,
    entries = entriesOnCurrentPage,
    sortedByFunction = sortedByFunction,
    handler = handler,
  )

  div("row") {
    div("col-2") {
      floatingSelectNullable(
        valueAndSetter = pageSize,
        idProvider = { int -> int?.toString() ?: "allProjects" },
        formatter = { int -> int?.toString() ?: "ALLE - ${entries.size}" },
        availableOptionsWithoutNull = listOf(5, 10, 15, 20, 25, 100),
        fieldName = "pageSizeField",
        title = "Projektanzahl",
      )
    }

    div("col-10") {
      pagination(currentPage = currentPage, entryPagesCount = entryPages.size)
    }
  }

}
