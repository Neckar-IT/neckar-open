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
import it.neckar.open.http.UrlParameterName
import it.neckar.open.kotlin.lang.asKClass
import org.junit.jupiter.api.Test
import it.neckar.rest.openapi.dsl.OpenApiRequestBuilder
import it.neckar.rest.openapi.schema.SchemaDescriptor

/**
 * Test enum for testing parameterEnum
 */
enum class TestStatus {
  Active,
  Inactive,
  Pending
}

/**
 * Tests for the convenient builder methods in OpenApiRequest
 */
class OpenApiRequestBuilderMethodsTest {

  @Test
  fun testParameterEnumBuilderMethod() {
    val builder = OpenApiRequestBuilder(emptyList())

    // Use the convenient reified parameterEnum method
    builder.parameterEnum<TestStatus>(UrlParameterName("status"))

    val request = builder.build()

    assertThat(request.parameters).hasSize(1)

    val parameter = request.parameters.first()
    assertThat(parameter.name).isEqualTo(UrlParameterName("status"))

    // Verify the parameter creates a schema reference to the enum type (not inline enum)
    val schema = parameter.type
    assertThat(schema).isInstanceOf<SchemaDescriptor.EnumSchema>()
    val enumSchema = schema as SchemaDescriptor.EnumSchema
    assertThat(enumSchema.enumType.asKClass().simpleName).isEqualTo("TestStatus")
  }

}
