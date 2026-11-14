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
package it.neckar.uuid

import assertk.*
import it.neckar.open.test.utils.containsExactly
import it.neckar.open.test.utils.containsExactlyList
import it.neckar.uuid.HasUuidToolsTest.TestData.Id
import org.junit.jupiter.api.Test
import kotlin.uuid.Uuid

/**
 *
 */
class HasUuidToolsTest {
  @Test
  fun testSimple() {
    val testData = TestData(TestData.Id.random(), idList = emptyList())
    assertThat(HasUuidTools.extractIdsRecursivelyUsingReflection(testData)).containsExactly(testData.id)
  }

  @Test
  fun testCollection() {
    val testData = TestData(Id.random(), idList = listOf(Id.random(), Id.random()))
    assertThat(HasUuidTools.extractIdsRecursivelyUsingReflection(testData)).containsExactlyList(listOf(testData.id) + testData.idList)
  }

  @Test
  fun testNested() {
    val testData = NestedTestData(
      nested1 = TestData(Id.random(), idList = listOf(Id.random())),
      nested2 = TestData(Id.random(), idList = listOf(Id.random(), Id.random())),
    )

    assertThat(HasUuidTools.extractIdsRecursivelyUsingReflection(testData)).containsExactlyList(
      buildList {
        add(testData.nested1.id)
        addAll(testData.nested1.idList)
        add(testData.nested2.id)
        addAll(testData.nested2.idList)
      }
    )
  }

  private data class TestData(
    val id: HasUuid,
    val idList: List<Id>,
  ) {

    @JvmInline
    value class Id(
      override val uuid: Uuid,
    ) : ValueClassUuid {

      override fun toString(): String {
        return uuid.toHexDashString()
      }

      companion object {
        /**
         * Creates a new random id
         */
        fun random(): Id = Id(randomUuid4())
      }
    }
  }

  private data class NestedTestData(
    val nested1: TestData,
    val nested2: TestData,
  )
}
