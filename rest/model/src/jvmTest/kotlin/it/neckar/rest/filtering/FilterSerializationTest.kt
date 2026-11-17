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
package it.neckar.rest.filtering

import assertk.*
import assertk.assertions.*
import it.neckar.open.serialization.roundTrip
import it.neckar.rest.RestResponseFilters
import it.neckar.rest.draft.ConfirmationStatus
import it.neckar.uuid.parseUuid
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import kotlin.uuid.Uuid

/**
 * Test serialization for filters
 */
class FilterSerializationTest {
  @Test
  fun testFilterSerialization() {

    val filterCriteria = listOf(
      FilterDescriptor(MyFilterCriteria.FreeText, "some free text"),
      FilterDescriptor(MyFilterCriteria.Title, "some title"),
      FilterDescriptor(MyFilterCriteria.AuthorId, "6604929e-ead1-43ea-b608-21ca64583556"),
    )

    val filterDescriptors = FilterDescriptors.from<MyFilterCriteria>(filterCriteria)
    assertThat(filterDescriptors.size).isEqualTo(3)

    assertThat(filterDescriptors[MyFilterCriteria.FreeText]?.constraint).isEqualTo("some free text")
    assertThat(filterDescriptors[MyFilterCriteria.Title]?.constraint).isEqualTo("some title")
    assertThat(filterDescriptors[MyFilterCriteria.AuthorId]?.uuidValue()).isEqualTo(parseUuid("6604929e-ead1-43ea-b608-21ca64583556"))


    val filterCriteriaState = MyFilterCriteriaState(
      freeText = filterDescriptors[MyFilterCriteria.FreeText]?.constraint,
      title = filterDescriptors[MyFilterCriteria.Title]?.constraint,
      authorPreview = filterDescriptors[MyFilterCriteria.AuthorId]?.let {
        AuthorPreview(
          id = it.uuidValue(),
          displayName = "my-author",
          confirmationStatus = ConfirmationStatus.Confirmed,
        )
      },
    )

    roundTrip(filterCriteriaState) {
      //language=JSON
      """
        {
          "freeText" : "some free text",
          "title" : "some title",
          "authorPreview" : {
            "id" : "6604929e-ead1-43ea-b608-21ca64583556",
            "displayName" : "my-author",
            "confirmationStatus" : "Confirmed"
          }
        }
      """.trimIndent()
    }
  }

  @Serializable
  private data class MyFilterCriteriaState(
    val freeText: String? = null,
    val title: String? = null,
    val authorPreview: AuthorPreview? = null,
  )

  @Serializable
  private data class AuthorPreview(
    val id: Uuid,
    val displayName: String?,
    val confirmationStatus: ConfirmationStatus,
  )

  @Serializable
  private enum class MyFilterCriteria(
    override val filterType: TypedFilterKey.FilterType = TypedFilterKey.FilterType.String,
  ) : RestResponseFilters {
    FreeText {
      override val typedFilterKey: TypedFilterKey = FilterKey.freeText
    },
    Title,
    AuthorId(TypedFilterKey.FilterType.Uuid),
  }
}
