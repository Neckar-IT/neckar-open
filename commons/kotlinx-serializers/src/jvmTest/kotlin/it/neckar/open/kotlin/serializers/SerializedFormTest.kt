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
import kotlinx.serialization.ContextualSerializer
import kotlinx.serialization.KSerializer
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.MapSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.junit.jupiter.api.Test

class SerializedFormTest {
  @Test
  fun `every kind of serializer maps to its form`() {
    assertThat(Url.DataScheme::class.serializedForm()).isEqualTo(SerializedForm.Primitive)
    assertThat(String::class.serializedForm()).isEqualTo(SerializedForm.Primitive)
    assertThat(Color::class.serializedForm()).isEqualTo(SerializedForm.Enum)
    assertThat(Point::class.serializedForm()).isEqualTo(SerializedForm.Class)
    assertThat(Origin::class.serializedForm()).isEqualTo(SerializedForm.Object)
    assertThat(Tags::class.serializedForm()).isEqualTo(SerializedForm.List)
    assertThat(Labels::class.serializedForm()).isEqualTo(SerializedForm.Map)
    assertThat(Shape::class.serializedForm()).isEqualTo(SerializedForm.Sealed)
    assertThat(Plugin::class.serializedForm()).isEqualTo(SerializedForm.Open)
    assertThat(Resolved::class.serializedForm()).isEqualTo(SerializedForm.Contextual)
  }

  @Test
  fun `a class without serializer is NoSerializer`() {
    assertThat(List::class.serializedForm()).isEqualTo(SerializedForm.NoSerializer)
    assertThat(Unannotated::class.serializedForm()).isEqualTo(SerializedForm.NoSerializer)
  }

  @Serializable
  enum class Color { Red, Green }

  @Serializable
  data class Point(val x: Int, val y: Int)

  @Serializable
  object Origin

  @Serializable(with = Tags.TagsSerializer::class)
  class Tags(val values: List<String>) {
    object TagsSerializer : KSerializer<Tags> {
      private val delegate = ListSerializer(String.serializer())
      override val descriptor: SerialDescriptor = delegate.descriptor
      override fun serialize(encoder: Encoder, value: Tags): Unit = encoder.encodeSerializableValue(delegate, value.values)
      override fun deserialize(decoder: Decoder): Tags = Tags(decoder.decodeSerializableValue(delegate))
    }
  }

  @Serializable(with = Labels.LabelsSerializer::class)
  class Labels(val values: Map<String, String>) {
    object LabelsSerializer : KSerializer<Labels> {
      private val delegate = MapSerializer(String.serializer(), String.serializer())
      override val descriptor: SerialDescriptor = delegate.descriptor
      override fun serialize(encoder: Encoder, value: Labels): Unit = encoder.encodeSerializableValue(delegate, value.values)
      override fun deserialize(decoder: Decoder): Labels = Labels(decoder.decodeSerializableValue(delegate))
    }
  }

  @Serializable
  sealed class Shape {
    @Serializable
    data class Circle(val radius: Double) : Shape()
  }

  @Serializable
  abstract class Plugin

  @Serializable(with = Resolved.ResolvedSerializer::class)
  class Resolved {
    object ResolvedSerializer : KSerializer<Resolved> by ContextualSerializer(Resolved::class)
  }

  class Unannotated
}
