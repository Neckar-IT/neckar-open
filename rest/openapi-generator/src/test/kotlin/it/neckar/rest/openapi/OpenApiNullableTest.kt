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
import it.neckar.open.test.utils.isJsonEqualTo
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import it.neckar.rest.openapi.documentation.DocumentationProvider
import it.neckar.rest.openapi.schema.SchemaDescriptor

/**
 *
 */
class OpenApiNullableTest {
  @Test
  fun testMyDataClass() {
    assertThat(
      OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None).generateComponentsSchemas(listOf(SchemaDescriptor.of(MyDataClass::class)))
        .toString()
    ).isJsonEqualTo(
      //language=JSON
      """
        {
          "OpenApiNullableTest.MyDataClass" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiNullableTest.MyDataClass",
            "properties" : {
              "name" : {
                "type" : "string"
              },
              "nullable" : {
                "type" : [ "string", "null" ]
              },
              "nullableWithDefaultNull" : {
                "type" : [ "string", "null" ]
              },
              "defaultValue" : {
                "type" : "string"
              },
              "defaultValueNullable" : {
                "type" : [ "string", "null" ]
              }
            },
            "required" : [ "name", "nullable" ]
          }
        }
      """.trimIndent()
    )
  }

  @Serializable
  private data class MyDataClass(
    val name: String,
    val nullable: String?,
    val nullableWithDefaultNull: String? = null,
    val defaultValue: String = "default",
    val defaultValueNullable: String? = "default",
  )
}
