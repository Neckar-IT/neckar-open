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
import org.junit.jupiter.api.Test

/**
 *
 */
class PaginationInfoTest {
  @Test
  fun testSelect1() {
    val items = List(300) { it + 1 }

    PaginationInfo.create(
      filteredElementsCount = items.size, pageIndex = PageIndex(0), pageSize = PageSize(10), totalElementsCount = 999
    ).let { paginationInfo ->
      assertThat(paginationInfo.pageSize).isEqualTo(PageSize(10))
      assertThat(paginationInfo.pageCount).isEqualTo(30)
      assertThat(paginationInfo.totalElementsCount).isEqualTo(999)
      assertThat(paginationInfo.filteredElementsCount).isEqualTo(300)
      assertThat(paginationInfo.selectElements(items)).containsExactly(1, 2, 3, 4, 5, 6, 7, 8, 9, 10)
    }

    PaginationInfo.create(
      filteredElementsCount = items.size, pageIndex = PageIndex(1), pageSize = PageSize(10), totalElementsCount = 999
    ).let { paginationInfo ->
      assertThat(paginationInfo.pageSize).isEqualTo(PageSize(10))
      assertThat(paginationInfo.pageCount).isEqualTo(30)
      assertThat(paginationInfo.totalElementsCount).isEqualTo(999)
      assertThat(paginationInfo.filteredElementsCount).isEqualTo(300)
      assertThat(paginationInfo.selectElements(items)).containsExactly(11, 12, 13, 14, 15, 16, 17, 18, 19, 20)
    }

    PaginationInfo.create(
      filteredElementsCount = items.size, pageIndex = PageIndex(2), pageSize = PageSize(10), totalElementsCount = 999
    ).let { paginationInfo ->
      assertThat(paginationInfo.pageSize).isEqualTo(PageSize(10))
      assertThat(paginationInfo.pageCount).isEqualTo(30)
      assertThat(paginationInfo.totalElementsCount).isEqualTo(999)
      assertThat(paginationInfo.filteredElementsCount).isEqualTo(300)
      assertThat(paginationInfo.selectElements(items)).containsExactly(21, 22, 23, 24, 25, 26, 27, 28, 29, 30)
    }


    PaginationInfo(PageIndex(0), PageSize(3), 30, 10, 10).let { paginationInfo ->
      assertThat(paginationInfo.selectElements(items)).containsExactly(1, 2, 3)
    }

    PaginationInfo.create(
      filteredElementsCount = List(2) { it + 1 }.size, pageIndex = PageIndex(0), pageSize = PageSize(1), totalElementsCount = 2
    ).let { paginationInfo ->
      assertThat(paginationInfo.pageSize).isEqualTo(PageSize(1))
      assertThat(paginationInfo.totalElementsCount).isEqualTo(2)
      assertThat(paginationInfo.filteredElementsCount).isEqualTo(2)
      assertThat(paginationInfo.pageCount).isEqualTo(2)
    }
  }

  @Test
  fun `create with unlimited page size yields a single page`() {
    val items = List(300) { it + 1 }

    //PageSize.Unlimited (value -1) must behave like a single page, not divCeil(-1) = -count.
    PaginationInfo.create(
      filteredElementsCount = items.size, pageIndex = PageIndex.Zero, pageSize = PageSize.Unlimited, totalElementsCount = 300
    ).let { paginationInfo ->
      assertThat(paginationInfo.pageSize).isEqualTo(PageSize.Unlimited)
      assertThat(paginationInfo.pageCount).isEqualTo(1)
      assertThat(paginationInfo.filteredElementsCount).isEqualTo(300)
      assertThat(paginationInfo.totalElementsCount).isEqualTo(300)
      assertThat(paginationInfo.selectElements(items)).isEqualTo(items)
    }
  }

  @Test
  fun testLargePage() {
    val items = List(300) { it + 1 }

    PaginationInfo.create(
      filteredElementsCount = items.size, pageIndex = PageIndex(999), pageSize = PageSize(10), totalElementsCount = 999
    ).let { paginationInfo ->
      assertThat(paginationInfo.filteredElementsCount).isEqualTo(300)
      assertThat(paginationInfo.pageSize).isEqualTo(PageSize(10))
      assertThat(paginationInfo.pageCount).isEqualTo(30)
      assertThat(paginationInfo.totalElementsCount).isEqualTo(999)
      assertThat(paginationInfo.selectElements(items)).containsExactly()
    }
  }

  @Test
  fun `the last page holds the remaining elements`() {
    val items = List(25) { it + 1 }

    val paginationInfo = PaginationInfo.create(filteredElementsCount = items.size, pageIndex = PageIndex(2), pageSize = PageSize(10), totalElementsCount = items.size)

    assertThat(paginationInfo.pageCount).isEqualTo(3)
    assertThat(paginationInfo.selectElements(items)).containsExactly(21, 22, 23, 24, 25)
  }

  @Test
  fun `a page index far beyond the list answers an empty page`() {
    val items = (1..300).map { "item$it" }

    // 200000000 * 20 overflows Int into a negative offset, which sliced from the end of the list.
    assertThat(PaginationInfo.create(items.size, PageIndex(200_000_000), PageSize(20), items.size).selectElements(items)).isEmpty()
  }
}
