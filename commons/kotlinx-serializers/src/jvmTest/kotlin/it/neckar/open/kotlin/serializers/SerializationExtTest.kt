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
import kotlinx.serialization.KSerializer
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.PrimitiveKind
import kotlinx.serialization.descriptors.PrimitiveSerialDescriptor
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.encoding.Decoder
import kotlinx.serialization.encoding.Encoder
import org.junit.jupiter.api.Test
import kotlin.reflect.typeOf

class SerializationExtTest {
  @Test
  fun testGetElementByName() {
    val descriptor = FooBar.serializer().descriptor.getElementDescriptorByName("companyProfile")
    assertThat(descriptor).isNotNull()
    assertThat(descriptor.serialName).isEqualTo("kotlin.String")
  }

  @Test
  fun testGetDiscriminatorValues() {
    // Test with sealed interface
    val values = SealedTestInterface::class.getDiscriminatorValues()
    assertThat(values).containsExactlyInAnyOrder("TypeA", "TypeB", "TypeC")
  }

  @Test
  fun testGetDiscriminatorValuesWithNestedSealed() {
    // Test with nested sealed interfaces
    val values = NestedSealedInterface::class.getDiscriminatorValues()
    assertThat(values).containsExactlyInAnyOrder("NestedA", "NestedB", "NestedC")
  }

  @Test
  fun testGetDiscriminatorValuesRequiresSealed() {
    // Test that it throws exception for non-sealed class
    assertFailure {
      FooBar::class.getDiscriminatorValues()
    }.isInstanceOf(IllegalArgumentException::class)
      .messageContains("must be sealed")
  }

  @Test
  fun testGetDiscriminatorValuesMissingSerialName() {
    // Test that it throws exception when @SerialName is missing
    assertFailure {
      SealedWithoutSerialName::class.getDiscriminatorValues()
    }.isInstanceOf(IllegalArgumentException::class)
      .hasMessage("<it.neckar.open.kotlin.serializers.SerializationExtTest.SealedWithoutSerialName.MissingAnnotation> carries no @SerialName")
  }

  @Test
  fun testSerialNames() {
    assertThat(serialNames<WireState>()).containsExactly("started", "Done", "failed")
  }

  @Test
  fun testGetEnumBySerialName() {
    assertThat(getEnumBySerialName<WireState>("started")).isEqualTo(WireState.Started)
    assertThat(getEnumBySerialName<WireState>("Done")).isEqualTo(WireState.Done)
    assertThat(getEnumBySerialName<WireState>("failed")).isEqualTo(WireState.Failed)
  }

  @Test
  fun testGetEnumBySerialNameFailsForTheConstantName() {
    assertFailure {
      getEnumBySerialName<WireState>("Started")
    }.isInstanceOf(IllegalArgumentException::class)
      .hasMessage("<it.neckar.open.kotlin.serializers.SerializationExtTest.WireState> has no entry with serial name <Started>; serial names: started, Done, failed")
  }

  @Test
  fun testFindEnumBySerialName() {
    assertThat(findEnumBySerialName<WireState>("failed")).isEqualTo(WireState.Failed)
    assertThat(findEnumBySerialName<WireState>("Failed")).isNull()
  }

  @Test
  fun testFindElementIndex() {
    assertThat(FooBar.serializer().descriptor.findElementIndex("companyProfile")).isEqualTo(0)
    assertThat(FooBar.serializer().descriptor.findElementIndex("unknown")).isNull()
  }

  @Test
  fun testFindElementDescriptorByName() {
    assertThat(FooBar.serializer().descriptor.findElementDescriptorByName("companyProfile")?.serialName).isEqualTo("kotlin.String")
    assertThat(FooBar.serializer().descriptor.findElementDescriptorByName("unknown")).isNull()
  }

  @Test
  fun testSerializerOrNull() {
    assertThat(typeOf<List<String>>().serializerOrNull()?.descriptor?.serialName).isEqualTo("kotlin.collections.ArrayList")
    assertThat(typeOf<NotSerializable>().serializerOrNull()).isNull()
  }

  @Test
  fun testSerialNamesOfAnEnumWithoutSerializable() {
    assertThat(serialNames<UnannotatedState>()).containsExactly("Open", "Closed")
  }

  @Test
  fun testEnumDescriptorFailsForAPrimitiveSerializer() {
    assertFailure {
      enumDescriptor<OrdinalState>()
    }.isInstanceOf(IllegalArgumentException::class)
      .hasMessage("<OrdinalState> is serialized as INT, not as an enum")
  }

  @Test
  fun testEnumSerialName() {
    assertThat(WireState.Started.serialName()).isEqualTo("started")
    assertThat(WireState.Done.serialName()).isEqualTo("Done")
    assertThat(WireState.Failed.serialName()).isEqualTo("failed")
  }

  @Test
  fun testGetSerialName() {
    assertThat(MyIface.Impl::class.getSerialName()).isEqualTo("Impl")
  }

  @Test
  fun testGetSerialNameFailsWithoutAnnotation() {
    assertFailure {
      FooBar::class.getSerialName()
    }.isInstanceOf(IllegalArgumentException::class)
      .hasMessage("<it.neckar.open.kotlin.serializers.SerializationExtTest.FooBar> carries no @SerialName")
  }

  private class NotSerializable

  private enum class UnannotatedState {
    @SerialName("open")
    Open,

    Closed,
  }

  @Serializable(with = OrdinalStateSerializer::class)
  private enum class OrdinalState {
    On,
    Off,
  }

  private object OrdinalStateSerializer : KSerializer<OrdinalState> {
    override val descriptor: SerialDescriptor = PrimitiveSerialDescriptor("OrdinalState", PrimitiveKind.INT)

    override fun serialize(encoder: Encoder, value: OrdinalState) {
      encoder.encodeInt(value.ordinal)
    }

    override fun deserialize(decoder: Decoder): OrdinalState {
      return OrdinalState.entries[decoder.decodeInt()]
    }
  }

  @Serializable
  private enum class WireState {
    @SerialName("started")
    Started,

    Done,

    //A constant with a body compiles to an anonymous subclass
    @SerialName("failed")
    Failed {
      override fun toString(): String = "failed state"
    },
  }

  @Serializable
  private data class FooBar(val companyProfile: String)

  @Serializable
  private sealed interface MyIface {
    @Serializable
    @SerialName("Impl")
    data class Impl(val value: String) : MyIface
  }

  @Serializable
  private sealed interface SealedTestInterface {
    @Serializable
    @SerialName("TypeA")
    data class TypeA(val valueA: String) : SealedTestInterface

    @Serializable
    @SerialName("TypeB")
    data class TypeB(val valueB: Int) : SealedTestInterface

    @Serializable
    @SerialName("TypeC")
    data object TypeC : SealedTestInterface
  }

  @Serializable
  private sealed interface NestedSealedInterface {
    @Serializable
    @SerialName("NestedA")
    data class NestedA(val value: String) : NestedSealedInterface

    @Serializable
    sealed interface NestedLevel : NestedSealedInterface {
      @Serializable
      @SerialName("NestedB")
      data class NestedB(val value: Int) : NestedLevel

      @Serializable
      @SerialName("NestedC")
      data class NestedC(val value: Boolean) : NestedLevel
    }
  }

  @Serializable
  private sealed interface SealedWithoutSerialName {
    @Serializable
    data class MissingAnnotation(val value: String) : SealedWithoutSerialName
  }
}
