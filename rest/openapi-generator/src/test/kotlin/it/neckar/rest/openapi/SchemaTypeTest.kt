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
package it.neckar.rest.openapi

import assertk.*
import assertk.assertions.*
import it.neckar.open.annotations.ExportAsObject
import it.neckar.open.http.Url
import it.neckar.open.kotlin.lang.asKClass
import it.neckar.open.kotlin.lang.hasSealedSuperType
import it.neckar.open.kotlin.lang.isSealed
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.builtins.serializer
import org.junit.jupiter.api.Test
import kotlin.reflect.full.starProjectedType
import kotlin.reflect.typeOf
import it.neckar.rest.openapi.schema.NativeSchemaType
import it.neckar.rest.openapi.schema.SchemaDescriptor
import it.neckar.rest.openapi.schema.isClassAsInlinePrimitive
import it.neckar.rest.openapi.schema.isEnumEntry
import it.neckar.rest.openapi.schema.isEnumSchema
import it.neckar.rest.openapi.schema.isInline
import it.neckar.rest.openapi.schema.isPrimitive
import it.neckar.rest.openapi.schema.isValueClass
import it.neckar.rest.openapi.schema.toOpenApiNativeType
import it.neckar.rest.openapi.schema.primitiveToOpenApiType
import it.neckar.rest.openapi.schema.isClassSchemaObject
import it.neckar.rest.openapi.schema.isConstSchemaObject

/**
 *
 */
class SchemaTypeTest {
  @Test
  fun testUrl() {
    SchemaDescriptor.of<Url>().also { openApiSchema ->
      assertThat(openApiSchema.isClassAsInlinePrimitive()).isTrue()
      assertThat(openApiSchema.isInline()).isTrue()
      assertThat(openApiSchema.isPrimitive()).isFalse()
      assertThat(openApiSchema.isValueClass()).isFalse()

      assertThat(openApiSchema.asInline().toNativeType()).isEqualTo(NativeSchemaType.Uri)
    }
  }

  @Test
  fun testPrimitive() {
    assertThat(SchemaDescriptor.String.isInline()).isTrue()
    assertThat(SchemaDescriptor.String.isPrimitive()).isTrue()
    assertThat(SchemaDescriptor.of<String>()).isEqualTo(SchemaDescriptor.String)
  }

  @Test
  fun `Float descriptor maps to the float native type, not double`() {
    assertThat(Float.serializer().descriptor.primitiveToOpenApiType()).isEqualTo(NativeSchemaType.Float)
    assertThat(Double.serializer().descriptor.primitiveToOpenApiType()).isEqualTo(NativeSchemaType.Double)
  }

  @Test
  fun testClass() {
    assertThat(SchemaDescriptor.of(SchemaTypeTest::class).isClassSchemaObject()).isTrue()
  }

  @Test
  fun testFromString() {
    assertThat(SchemaDescriptor.of(SchemaTypeTest::class.qualifiedName!!)).isEqualTo(SchemaDescriptor.of(SchemaTypeTest::class))
  }

  @Test
  fun testNothing1() {
    val nothing = Nothing::class
    assertThat(nothing.qualifiedName).isEqualTo("java.lang.Void")

    val starProjected = nothing.starProjectedType
    assertThat(starProjected.toString()).isEqualTo("java.lang.Void")
  }

  @Test
  fun testNothing() {
    assertThat(SchemaDescriptor.of(Nothing::class)).isEqualTo(SchemaDescriptor.EmptySchema)
    assertThat(SchemaDescriptor.of(Nothing::class.starProjectedType)).isEqualTo(SchemaDescriptor.EmptySchema)
  }

  @Test
  fun testValueClass() {
    assertThat(SchemaDescriptor.valueClass(TestValueClass::class).isInline()).isTrue()
    assertThat(SchemaDescriptor.valueClass(TestValueClass::class).isValueClass()).isTrue()
    assertThat(SchemaDescriptor.of(TestValueClass::class).isValueClass()).isTrue()
  }

  @Test
  fun testEnum() {
    assertThat(SchemaDescriptor.enum(TestEnum::class).isEnumSchema()).isTrue()
    assertThat(SchemaDescriptor.of<TestEnum>()).all {
      isInstanceOf(SchemaDescriptor.EnumSchema::class)
      transform { it.isEnumSchema() }.isTrue()
      transform { it.isInline() }.isFalse()
    }
  }

  @Test
  fun testEnumObject() {
    assertThat(SchemaDescriptor.enumEntry(TestEnumAsObject.A)).all {
      isInstanceOf(SchemaDescriptor.EnumEntrySchema::class)
      transform { it.isEnumEntry() }.isTrue()
      transform { it.isInline() }.isFalse()
    }
  }

  @Test
  fun testObject() {
    assertThat(SchemaDescriptor.const(TestObject::class).isConstSchemaObject()).isTrue()
    assertThat(SchemaDescriptor.of(TestObject::class).isConstSchemaObject()).isTrue()
    assertThat(SchemaDescriptor.of<TestObject>().isConstSchemaObject()).isTrue()
  }

  @Test
  fun testInlineSerialization() {
    assertThat(SchemaDescriptor.of(Url::class).isClassAsInlinePrimitive()).isTrue()
    assertThat(SchemaDescriptor.of<Url>().isClassAsInlinePrimitive()).isTrue()
  }

  @Test
  fun testValueClassNonPrimitive() {
    assertFailure {
      SchemaDescriptor.valueClass(TestValueClassNonPrimitive::class)
    }.hasMessage("Currently only value classes wrapping a primitive type are supported")
  }

  @Test
  fun testIt() {
    val schema = SchemaDescriptor.of<FooBarObject>()
    assertThat(schema.isConstSchemaObject()).isTrue()

    val returnType = FooBarObject::elements.returnType
    assertThat(returnType.toString()).isEqualTo("kotlin.collections.List<kotlin.String>")
    assertThat(returnType.isMarkedNullable).isFalse()
    assertThat(returnType.arguments).all {
      hasSize(1)
      first().transform { it.toString() }.isEqualTo("kotlin.String")
    }

    val returnTypeAsClass = returnType.asKClass()
    assertThat(returnTypeAsClass).isEqualTo(List::class)
    val nativeType: NativeSchemaType? = returnTypeAsClass.toOpenApiNativeType()

    assertThat(nativeType).isNull()
  }

  @Test
  fun testEnumDifferent() {
    assertThat(SchemaDescriptor.of(TestEnum::class)).isInstanceOf(SchemaDescriptor.EnumSchema::class)
    assertThat(SchemaDescriptor.of(TestEnumAsObject::class)).isInstanceOf(SchemaDescriptor.ComponentEnumSchemaObject::class)
  }

  @Test
  fun testDiscriminatedClassSchemaRegistration() {
    assertThat(SchemaDescriptor.of<TestDiscriminated.Foo>()).isInstanceOf(SchemaDescriptor.DiscriminatedClassSchemaWrapper::class)

    val discriminated = SchemaDescriptor.discriminatedClass(TestDiscriminated.Foo::class.starProjectedType)
    assertThat(discriminated).isNotNull()
    assertThat(discriminated.baseSchema.type).isEqualTo(TestDiscriminated.Foo::class.starProjectedType)
  }

  @Test
  fun testIsSealed() {
    assertThat(typeOf<FooBarObject>().isSealed()).isFalse()
    assertThat(typeOf<TestValueClass>().isSealed()).isFalse()
    assertThat(typeOf<TestValueClassNonPrimitive>().isSealed()).isFalse()
    assertThat(typeOf<TestEnum>().isSealed()).isFalse()

    //Sealed classes always have a discriminator
    assertThat(typeOf<TestDiscriminated>().isSealed()).isTrue()
    assertThat(typeOf<TestDiscriminated.Foo>().hasSealedSuperType()).isTrue()
  }

  internal object FooBarObject {
    val elements: List<String> = listOf("a", "b", "c")
  }

  @JvmInline
  private value class TestValueClass(val value: String)

  @JvmInline
  private value class TestValueClassNonPrimitive(val value: List<String>)

  @Serializable
  private enum class TestEnum {
    A, B, C
  }

  @ExportAsObject
  @Serializable
  private enum class TestEnumAsObject(val foo: String) {
    A("aa"),
    B("bb"),
    C("cc"),
  }

  internal data object TestObject

  @Serializable
  private sealed interface TestDiscriminated {
    @Serializable
    @SerialName("theFoo")
    data class Foo(val value: String) : TestDiscriminated
  }
}
