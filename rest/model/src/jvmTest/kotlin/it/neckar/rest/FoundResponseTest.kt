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
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.serializer
import org.junit.jupiter.api.Test

class FoundResponseTest {
  private val good: FoundResponse<TestPreview> = FoundResponse.good(TestPreview(name = "Mark Twain"))

  @Test
  fun `Good round-trips with the data`() {
    roundTrip(good, FoundResponse.serializer(TestPreview.serializer())) {
      //language=JSON
      """
        {
          "type" : "good",
          "data" : {
            "name" : "Mark Twain"
          }
        }
      """.trimIndent()
    }
  }

  @Test
  fun `NotFound round-trips as type-only object`() {
    val notFound: FoundResponse<TestPreview> = FoundResponse.notFound()
    roundTrip(notFound, FoundResponse.serializer(TestPreview.serializer())) {
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
  fun `serializer resolved from the type argument keeps the data serializer`() {
    val json = Json.encodeToString(serializer<FoundResponse<TestPreview>>(), good)

    assertThat(json).isEqualTo("""{"type":"good","data":{"name":"Mark Twain"}}""")
    assertThat(Json.decodeFromString<FoundResponse<TestPreview>>(json)).isEqualTo(good)
    assertThat(Json.encodeToString<FoundResponse<TestPreview>>(FoundResponse.notFound())).isEqualTo("""{"type":"bad/NotFound"}""")
    assertThat(Json.decodeFromString<FoundResponse<TestPreview>>("""{"type":"bad/NotFound"}""")).isEqualTo(FoundResponse.notFound())
  }

  @Test
  fun `createFor answers NotFound for null and Good for data`() {
    assertThat(FoundResponse.createFor<TestPreview>(null)).isEqualTo(FoundResponse.notFound())
    assertThat(FoundResponse.createFor(TestPreview(name = "Mark Twain"))).isEqualTo(good)
  }

  @Serializable
  private data class TestPreview(val name: String)
}
