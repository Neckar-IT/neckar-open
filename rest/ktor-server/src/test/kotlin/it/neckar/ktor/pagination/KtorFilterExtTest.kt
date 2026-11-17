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
package it.neckar.ktor.pagination

import assertk.assertThat
import assertk.assertions.containsOnly
import assertk.assertions.hasMessage
import assertk.assertions.isEmpty
import io.ktor.http.Parameters
import io.ktor.http.parametersOf
import it.neckar.rest.InvalidQueryParameterException
import it.neckar.rest.filtering.FilterKey
import it.neckar.rest.filtering.HasTypedFilterKey
import it.neckar.rest.filtering.TypedFilterKey
import it.neckar.rest.filtering.uuid
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.assertThrows

class KtorFilterExtTest {
  enum class BookFilterKeys(override val typedFilterKey: TypedFilterKey) : HasTypedFilterKey {
    Author(FilterKey("authorId").uuid()),
    Category(FilterKey("categoryId").uuid()),
  }

  @Test
  fun `an absent scope parameter is the empty scope`() {
    assertThat(Parameters.Empty.scopeFilterKeys<BookFilterKeys>()).isEmpty()
  }

  @Test
  fun `every named filter criterion is accepted`() {
    assertThat(parametersOf("scope", "authorId , categoryId").scopeFilterKeys<BookFilterKeys>())
      .containsOnly(FilterKey("authorId"), FilterKey("categoryId"))
  }

  @Test
  fun `a scope key belonging to no filter criterion names the allowed ones`() {
    val exception = assertThrows<InvalidQueryParameterException> {
      parametersOf("scope", "authorID").scopeFilterKeys<BookFilterKeys>()
    }

    assertThat(exception).hasMessage("Invalid value [authorID] for query parameter [scope]. Value must be one of: [authorId, categoryId]")
  }
}
