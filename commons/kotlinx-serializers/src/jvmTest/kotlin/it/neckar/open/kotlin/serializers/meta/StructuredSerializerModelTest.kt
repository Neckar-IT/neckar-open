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
package it.neckar.open.kotlin.serializers.meta

import assertk.*
import assertk.assertions.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test

class StructuredSerializerModelTest {
  @Test
  fun testBoolean() {
    val model: SerializerModel<Boolean> = SerializerModel.create(Boolean.serializer())

    assertThat(model).isNotNull()
    assertThat(model).isInstanceOf(PrimitiveSerializerModel::class)

    val primitiveSerializerModel: PrimitiveSerializerModel<Boolean> = model as PrimitiveSerializerModel
    assertThat(primitiveSerializerModel.serialName).isEqualTo("kotlin.Boolean")
    assertThat(primitiveSerializerModel.type).isEqualTo(Boolean::class)
  }

  @Test
  fun testDouble() {
    val model: SerializerModel<Double> = SerializerModel.create(Double.serializer())

    assertThat(model).isNotNull()
    assertThat(model).isInstanceOf(PrimitiveSerializerModel::class)

    val primitiveSerializerModel: PrimitiveSerializerModel<Double> = model as PrimitiveSerializerModel
    assertThat(primitiveSerializerModel.serialName).isEqualTo("kotlin.Double")
    assertThat(primitiveSerializerModel.type).isEqualTo(Double::class)
  }

  @Disabled
  @Test
  fun testIt() {
    val serializer = FooBar.serializer()

    val model: SerializerModel<FooBar> = SerializerModel.create(serializer, FooBar::class)
    assertThat(model).isNotNull()
    assertThat(model).isInstanceOf(StructuredSerializerModel::class)

    assertThat(model.serialName).isEqualTo("it.neckar.open.kotlin.serializers.meta.StructuredSerializerModelTest.FooBar")
    assertThat(model.type).isEqualTo(FooBar::class)

    val cast = model as StructuredSerializerModel<FooBar>
    assertThat(cast.elements).hasSize(12)
  }

  @Serializable
  class FooBar(
    val foo: String,
    val bar: Int,
  )
}
