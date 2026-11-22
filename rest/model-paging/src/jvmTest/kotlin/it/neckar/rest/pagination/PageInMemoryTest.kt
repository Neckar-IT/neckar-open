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
package it.neckar.rest.pagination

import assertk.*
import assertk.assertions.*
import it.neckar.rest.RestResponseSorts
import it.neckar.rest.sorting.Sort
import org.junit.jupiter.api.Test

class PageInMemoryTest {
  private enum class ItemSorts : RestResponseSorts { Name, Size }

  private data class Item(val name: String, val size: Int)

  private val items = listOf(Item("b", 2), Item("c", 1), Item("a", 2))

  private fun List<Item>.page(page: PageQuery<ItemSorts>, defaultOrder: Sort.Order, totalCount: Int = size): Page<Item> =
    pageInMemory(page, ItemSorts.Name, defaultOrder, totalCount) { sortBy ->
      when (sortBy) {
        ItemSorts.Name -> compareBy { it.name }
        ItemSorts.Size -> compareBy { it.size }
      }
    }

  private fun query(sortBy: ItemSorts? = null, sortOrder: Sort.Order = Sort.Order.Asc, pageIndex: Int = 0, pageSize: Int = 20): PageQuery<ItemSorts> =
    PageQuery(PageIndex(pageIndex), PageSize(pageSize), sortBy, sortOrder)

  @Test
  fun `without sortBy the default criterion sorts in the default order`() {
    assertThat(items.page(query(), Sort.Order.Asc).pageElements.map { it.name }).containsExactly("a", "b", "c")
    assertThat(items.page(query(), Sort.Order.Desc).pageElements.map { it.name }).containsExactly("c", "b", "a")
  }

  @Test
  fun `an explicit sortBy ignores the default order`() {
    assertThat(items.page(query(sortBy = ItemSorts.Name), Sort.Order.Desc).pageElements.map { it.name }).containsExactly("a", "b", "c")
  }

  @Test
  fun `Desc reverses the stable ascending sort including equal keys`() {
    assertThat(items.page(query(sortBy = ItemSorts.Size), Sort.Order.Asc).pageElements.map { it.name }).containsExactly("c", "b", "a")
    assertThat(items.page(query(sortBy = ItemSorts.Size, sortOrder = Sort.Order.Desc), Sort.Order.Asc).pageElements.map { it.name })
      .containsExactly("a", "b", "c")
  }

  @Test
  fun `the page counts filtered and total elements apart`() {
    val page = items.page(query(pageIndex = 1, pageSize = 2), Sort.Order.Asc, totalCount = 7)

    assertThat(page.pageElements.map { it.name }).containsExactly("c")
    assertThat(page.paginationInfo.filteredElementsCount).isEqualTo(3)
    assertThat(page.paginationInfo.totalElementsCount).isEqualTo(7)
    assertThat(page.paginationInfo.pageCount).isEqualTo(2)
    assertThat(page.paginationInfo.pageIndex).isEqualTo(PageIndex(1))
  }
}
