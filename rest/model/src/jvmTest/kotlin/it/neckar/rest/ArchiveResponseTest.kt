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
package it.neckar.rest

import assertk.assertThat
import assertk.assertions.isEqualTo
import it.neckar.open.serialization.roundTrip
import it.neckar.rest.draft.UsedBy
import it.neckar.rest.draft.UsedByCount
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.junit.jupiter.api.Test

class ArchiveResponseTest {
  private val archived: ArchiveResponse<TestUsedByEntityType> = ArchiveResponse.archived(
    UsedBy(listOf(UsedByCount(type = TestUsedByEntityType.Book, activeCount = 2, activeConfirmedCount = 1, activeDraftCount = 1, archivedCount = 3))),
  )

  @Test
  fun `Archived round-trips with usedBy counts`() {
    roundTrip(
      archived,
      ArchiveResponse.serializer(TestUsedByEntityType.serializer()),
    ) {
      //language=JSON
      """
        {
          "type" : "good",
          "usedBy" : {
            "counts" : [
              {
                "type" : "Book",
                "activeCount" : 2,
                "activeConfirmedCount" : 1,
                "activeDraftCount" : 1,
                "archivedCount" : 3
              }
            ]
          }
        }
      """.trimIndent()
    }
  }

  @Test
  fun `AlreadyArchived round-trips as type-only object`() {
    val alreadyArchived: ArchiveResponse<TestUsedByEntityType> = ArchiveResponse.alreadyArchived()
    roundTrip(
      alreadyArchived,
      ArchiveResponse.serializer(TestUsedByEntityType.serializer()),
    ) {
      //language=JSON
      """
        {
          "type" : "bad/AlreadyArchived"
        }
      """.trimIndent()
    }
  }

  @Test
  fun `NotFound round-trips as type-only object`() {
    val notFound: ArchiveResponse<TestUsedByEntityType> = ArchiveResponse.notFound()
    roundTrip(
      notFound,
      ArchiveResponse.serializer(TestUsedByEntityType.serializer()),
    ) {
      //language=JSON
      """
        {
          "type" : "bad/NotFound"
        }
      """.trimIndent()
    }
  }

  /**
   * Ktor's `call.respond<T>` resolves the serializer from the [kotlin.reflect.KType] of `T`.
   */
  @Test
  fun `serializer resolved from the type argument keeps the enum serializer`() {
    val json = Json.encodeToString(serializer<ArchiveResponse<TestUsedByEntityType>>(), archived)

    assertThat(json).isEqualTo(
      """{"type":"good","usedBy":{"counts":[{"type":"Book","activeCount":2,"activeConfirmedCount":1,"activeDraftCount":1,"archivedCount":3}]}}""",
    )
    assertThat(Json.decodeFromString<ArchiveResponse<TestUsedByEntityType>>(json)).isEqualTo(archived)
    assertThat(Json.encodeToString<ArchiveResponse<TestUsedByEntityType>>(ArchiveResponse.alreadyArchived())).isEqualTo("""{"type":"bad/AlreadyArchived"}""")
    assertThat(Json.encodeToString<ArchiveResponse<TestUsedByEntityType>>(ArchiveResponse.notFound())).isEqualTo("""{"type":"bad/NotFound"}""")
  }

  @Serializable
  private enum class TestUsedByEntityType {
    Book,
  }
}
