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
import kotlinx.serialization.InternalSerializationApi
import kotlinx.serialization.Serializable
import kotlinx.serialization.serializer
import org.junit.jupiter.api.Test

@OptIn(InternalSerializationApi::class)
class KotlinxSerializationDescriptorParsingTest {
  @Test
  fun testFoo() {
    val serializer = Foo::class.serializer()
    assertThat(serializer).isNotNull()
    assertThat(serializer.descriptor).isNotNull()
    assertThat(serializer.descriptor.serialName).isEqualTo("it.neckar.open.kotlin.serializers.KotlinxSerializationDescriptorParsingTest.Foo")
    assertThat(serializer.descriptor::class.qualifiedName).isEqualTo("kotlinx.serialization.internal.PluginGeneratedSerialDescriptor")

    assertThat(serializer.descriptor.elementsCount).isEqualTo(1)
    assertThat(serializer.descriptor.getElementName(0)).isEqualTo("theBar")
  }

  @Test
  fun testBar() {
    val serializer = Foo::class.serializer()
    val barDescriptor = serializer.descriptor.getElementDescriptor(0)

    assertThat(barDescriptor::class.qualifiedName).isEqualTo("kotlinx.serialization.internal.PluginGeneratedSerialDescriptor")
    assertThat(barDescriptor.elementsCount).isEqualTo(1)
    assertThat(barDescriptor.getElementName(0)).isEqualTo("value")

    val innerDescriptor = barDescriptor.getElementDescriptor(0)
    assertThat(innerDescriptor::class.qualifiedName).isEqualTo("kotlinx.serialization.internal.PrimitiveSerialDescriptor")
  }

  @Serializable
  private data class Foo(val theBar: Bar<String>)

  @Serializable
  private data class Bar<T>(val value: T)
}
