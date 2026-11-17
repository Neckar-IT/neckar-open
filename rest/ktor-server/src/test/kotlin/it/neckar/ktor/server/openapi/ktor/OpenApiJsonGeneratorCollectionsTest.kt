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
package it.neckar.ktor.server.openapi.ktor

import assertk.*
import it.neckar.open.serialization.isJsonEqualTo
import it.neckar.open.serialization.isOpenApiEqualTo
import it.neckar.rest.openapi.documentation.DocumentationProvider
import it.neckar.rest.openapi.OpenApiJsonGenerator
import it.neckar.rest.openapi.schema.SchemaDescriptor
import kotlinx.serialization.Serializable
import kotlinx.serialization.descriptors.SerialDescriptor
import kotlinx.serialization.descriptors.elementDescriptors
import kotlinx.serialization.descriptors.elementNames
import kotlinx.serialization.json.buildJsonObject
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Disabled
import org.junit.jupiter.api.Test
import kotlin.reflect.full.starProjectedType

class OpenApiJsonGeneratorCollectionsTest {
  private lateinit var generator: OpenApiJsonGenerator

  @BeforeEach
  fun setUp() {
    generator = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None)
  }

  @Test
  fun testUrl() {
    val generator = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None)

    assertThat(generator.generateSchemaTypeRef(SchemaDescriptor.of(FooBar::class.starProjectedType), listOf())).isJsonEqualTo(
      //language=JSON
      $$"""
        {
          "schema" : {
            "$ref" : "#/components/schemas/OpenApiJsonGeneratorCollectionsTest.FooBar"
          }
        }
      """.trimIndent()
    )


    val json = with(generator) {
      buildJsonObject {
        buildSchemaType(SchemaDescriptor.referenceObject(FooBar::class)) {
          if (it == SchemaDescriptor.of(Bar::class.starProjectedType)) {
            return@buildSchemaType
          }

          fail("Should not be called for $it")
        }
      }
    }

    assertThat(json).isOpenApiEqualTo(
      //language=JSON
      $$"""
        {
          "OpenApiJsonGeneratorCollectionsTest.FooBar" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.ktor.server.openapi.ktor.OpenApiJsonGeneratorCollectionsTest.FooBar",
            "properties" : {
              "strings" : {
                "type" : "array",
                "items" : {
                  "type" : "string"
                },
                "uniqueItems" : true
              },
              "ints" : {
                "type" : "array",
                "items" : {
                  "type" : "integer"
                }
              },
              "string" : {
                "type" : "string"
              },
              "childNullable" : {
                "oneOf" : [ {
                  "$ref" : "#/components/schemas/OpenApiJsonGeneratorCollectionsTest.Bar"
                }, {
                  "type" : "null"
                } ]
              }
            },
            "required" : [ "strings", "ints", "string" ]
          }
        }
      """.trimIndent()
    )
  }

  @Disabled
  @Test
  fun testPrintSerializer() {
    val serializer = FooBar.serializer()
    val descriptor = serializer.descriptor

    printDescriptor(descriptor, 1)
  }

  fun printDescriptor(descriptor: SerialDescriptor, depth: Int) {
    require(depth < 10)

    val indent = "  ".repeat(depth)

    println("---------------------------------------")
    println("$indent Name: " + descriptor.serialName)
    println("---------------------------------------")

    println("$indent Kind: " + "Kind: " + descriptor.kind)
    println("$indent Elements Count: " + descriptor.elementsCount)
    println("$indent Annotations: " + descriptor.annotations)
    println("$indent isInline" + descriptor.isInline)
    println("$indent isNullable" + descriptor.isNullable)
    println("$indent Element Names: " + descriptor.elementNames.joinToString())

    descriptor.elementDescriptors.forEach {
      printDescriptor(it, depth + 1)
    }
  }

  @Serializable
  data class FooBar(
    val strings: Set<String>,
    val ints: List<Int>,
    val string: String,
    val childNullable: Bar? = null,
  )

  @Serializable
  data class Bar(
    val foo: String,
    val bar: Int = 42,
  )
}
