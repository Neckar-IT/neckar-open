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
import it.neckar.open.http.Url
import it.neckar.open.serialization.isOpenApiEqualTo
import it.neckar.user.UserLoginName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import kotlin.reflect.typeOf
import it.neckar.rest.openapi.documentation.DocumentationProvider

/**
 *
 */
class OpenApiJsonGeneratorInlineClassTest {
  private lateinit var generator: OpenApiJsonGenerator

  @BeforeEach
  fun setUp() {
    generator = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None)
  }

  @Serializable
  private data class FooUser(
    val loginName: UserLoginName,
    val firstName: String,
    val lastName: String,
    val email: String,
    /**
     * The optional avatar URL
     */
    val avatarUrl: Url? = null,
  )

  @Test
  fun testInlineSerializer() {
    val serializer = MyInlineClass.serializer()
    val descriptor = serializer.descriptor
    assertThat(descriptor::class.qualifiedName).isEqualTo("kotlinx.serialization.internal.InlineClassDescriptor")
    assertThat(descriptor.serialName).isEqualTo("it.neckar.rest.openapi.OpenApiJsonGeneratorInlineClassTest.MyInlineClass")
    assertThat(descriptor.isInline).isTrue()
    assertThat(descriptor.elementsCount).isEqualTo(1)
    assertThat(descriptor.getElementName(0)).isEqualTo("value")
    assertThat(descriptor.getElementAnnotations(0)).isEmpty()

    descriptor.getElementDescriptor(0).let {
      assertThat(it.isNullable).isFalse()
      assertThat(it.isInline).isFalse()
      assertThat(it.serialName).isEqualTo("kotlin.String")
    }
  }

  @Serializable
  @JvmInline
  value class MyInlineClass(val value: String)

  @Test
  fun testBuildRefFooUser() {
    val json = buildJsonObject {
      OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None).buildSchemaTypeForDataClass(typeOf<FooUser>()) {
        throw UnsupportedOperationException("Should not be called - for $it")
      }
    }

    assertThat(json).isOpenApiEqualTo(
      //language=JSON
      """
        {
          "type" : "object",
          "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiJsonGeneratorInlineClassTest.FooUser",
          "properties" : {
            "loginName" : {
              "type" : "string"
            },
            "firstName" : {
              "type" : "string"
            },
            "lastName" : {
              "type" : "string"
            },
            "email" : {
              "type" : "string"
            },
            "avatarUrl" : {
              "type" : [ "string", "null" ],
              "format" : "uri"
            }
          },
          "required" : [ "loginName", "firstName", "lastName", "email" ]
        }
      """.trimIndent()
    )
  }
}
