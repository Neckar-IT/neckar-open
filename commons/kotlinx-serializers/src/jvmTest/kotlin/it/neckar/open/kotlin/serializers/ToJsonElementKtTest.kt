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
package it.neckar.open.kotlin.serializers

import assertk.*
import assertk.assertions.*
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonNull
import org.junit.jupiter.api.Test

class ToJsonElementKtTest {
  @Test
  fun testIt() {
    mapOf("a" to 7).toJsonElement().let {
      assertThat(it).isNotNull()
      assertThat(it).isInstanceOf(JsonObject::class)
      require(it is JsonObject)

      assertThat(it.size).isEqualTo(1)

      it["a"].let {
        requireNotNull(it)
        assertThat(it).isInstanceOf(JsonPrimitive::class)
      }
    }

  }

  @Test
  fun testMapOfMap() {
    mapOf(
      "a" to 7,
      "b" to mapOf(
        "c" to 8,
        "d" to "foobar",
      ),
    ).toJsonElement().let {
      assertThat(it).isNotNull()
      assertThat(it).isInstanceOf(JsonObject::class)
      require(it is JsonObject)

      assertThat(it.size).isEqualTo(2)

      it["a"].let {
        requireNotNull(it)
        assertThat(it).isInstanceOf(JsonPrimitive::class)
      }
      it["b"].let {
        requireNotNull(it)
        assertThat(it).isInstanceOf(JsonObject::class)
        require(it is JsonObject)

        it["c"].let {
          requireNotNull(it)
          assertThat(it).isInstanceOf(JsonPrimitive::class)
        }
        it["d"].let {
          requireNotNull(it)
          require(it is JsonPrimitive)
          assertThat(it.isString).isTrue()
          assertThat(it.content).isEqualTo("foobar")
        }
      }
    }

  }
}
