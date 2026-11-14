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
import it.neckar.open.http.Url
import it.neckar.open.http.io.UrlSerializer
import it.neckar.open.serialization.roundTrip
import kotlinx.serialization.ExperimentalSerializationApi
import kotlinx.serialization.descriptors.PrimitiveKind
import org.junit.jupiter.api.Test

class UrlSerializerTest {
  @OptIn(ExperimentalSerializationApi::class)
  @Test
  fun testDescriptorNames() {
    val descriptor = Url.DataScheme.serializer().descriptor
    assertThat(descriptor.serialName).isEqualTo("Url.DataScheme")
    assertThat(descriptor.elementsCount).isEqualTo(0)
    assertThat(descriptor.kind).isEqualTo(PrimitiveKind.STRING)
    assertThat(descriptor.isPrimitive()).isTrue()
    assertThat(Url.DataScheme::class.serializedForm()).isEqualTo(SerializedForm.Primitive)
  }

  @Test
  fun testSerializer() {
    roundTrip(Url.absolute("https://www.google.com"), UrlSerializer) {
      // language=json
      """
          "https://www.google.com"
      """.trimIndent()
    }
  }

  @Test
  fun testRoundtrip() {
    roundTrip<Url>(Url.absolute("https://www.google.com")) {
      // language=json
      """
          "https://www.google.com"
      """.trimIndent()
    }

    roundTrip<Url>(Url.data("data:asdf")) {
      // language=json
      """
          "data:asdf"
      """.trimIndent()
    }

    roundTrip<Url>(Url.rootRelative("/foobar")) {
      // language=json
      """
          "/foobar"
      """.trimIndent()
    }

    roundTrip<Url>(Url.relative("bar")) {
      // language=json
      """
          "bar"
      """.trimIndent()
    }
  }

  @Test
  fun testRoundtripWithoutType() {
    roundTrip(Url.absolute("https://www.google.com")) {
      // language=json
      """
          "https://www.google.com"
      """.trimIndent()
    }

    roundTrip(Url.data("data:asdf")) {
      // language=json
      """
          "data:asdf"
      """.trimIndent()
    }

    roundTrip(Url.rootRelative("/foobar")) {
      // language=json
      """
          "/foobar"
      """.trimIndent()
    }

    roundTrip(Url.relative("bar")) {
      // language=json
      """
          "bar"
      """.trimIndent()
    }
  }
}
