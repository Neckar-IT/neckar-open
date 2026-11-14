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
import it.neckar.adjustable.AdjustableValue
import it.neckar.rest.openapi.schema.SchemaDescriptor.ComponentSchema.ReferenceType
import org.junit.jupiter.api.Test
import kotlin.reflect.typeOf
import it.neckar.rest.openapi.schema.SchemaDescriptor

/**
 *
 */
class OpenApiSchemaGenericsTest {
  @Test
  fun testSimple() {
    assertThat(SchemaDescriptor.of(String::class).toString()).isEqualTo("NativePrimitiveSchema(type=kotlin.String)")
    assertThat(SchemaDescriptor.of(Simple::class).toString()).isEqualTo("ClassSchemaObject(referenceName=OpenApiSchemaGenericsTest.Simple)")
  }

  @Test
  fun testTypeOf() {
    assertThat(SchemaDescriptor.of(typeOf<String>()).toString()).isEqualTo("NativePrimitiveSchema(type=kotlin.String)")
    assertThat(SchemaDescriptor.of(typeOf<Simple>()).toString()).isEqualTo("ClassSchemaObject(referenceName=OpenApiSchemaGenericsTest.Simple)")
  }

  @Test
  fun testTypeOfGenerics() {
    SchemaDescriptor.of(typeOf<Wrapper<String>>()).let {
      assertThat(it).isInstanceOf(SchemaDescriptor.ClassSchemaObject::class)
      assertThat(it.toString()).isEqualTo("ClassSchemaObject(referenceName=OpenApiSchemaGenericsTest.Wrapper<String>)")
    }

    assertThat(SchemaDescriptor.of(typeOf<Wrapper<Double>>()).toString()).isEqualTo("ClassSchemaObject(referenceName=OpenApiSchemaGenericsTest.Wrapper<Double>)")
  }

  @Test
  fun testAdjustable() {
    val schemaDescriptor = SchemaDescriptor.of<AdjustableValue.Adjusted<String>>()

    assertThat(schemaDescriptor).isInstanceOf(SchemaDescriptor.DiscriminatedClassSchemaWrapper::class)
    schemaDescriptor as SchemaDescriptor.DiscriminatedClassSchemaWrapper
    assertThat(schemaDescriptor.baseSchema.toString()).isEqualTo("ClassSchemaObject(referenceName=AdjustableValue.Adjusted<String>)")

    assertThat(schemaDescriptor.schemaWithDiscriminator.reference(ReferenceType.Discriminated).unescapedValue).isEqualTo("AdjustableValue.Adjusted<String>.WithType")
    assertThat(schemaDescriptor.schemaWithDiscriminator.reference(ReferenceType.Plain).unescapedValue).isEqualTo("AdjustableValue.Adjusted<String>")

    assertThat(schemaDescriptor.schemaWithDiscriminator.typeSuffix).isEqualTo(".WithType")

    assertThat(schemaDescriptor.schemaWithDiscriminator.baseSchema.reference(ReferenceType.Discriminated).unescapedValue).isEqualTo("AdjustableValue.Adjusted<String>")
    assertThat(schemaDescriptor.schemaWithDiscriminator.baseSchema.reference(ReferenceType.Plain).unescapedValue).isEqualTo("AdjustableValue.Adjusted<String>")
  }

  private data class Simple(val foo: String)

  private data class Wrapper<T>(
    val value: T
  )
}
