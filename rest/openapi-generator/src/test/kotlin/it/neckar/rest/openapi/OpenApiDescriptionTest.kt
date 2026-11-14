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
import it.neckar.open.kotlin.lang.requireNotNull
import it.neckar.open.serialization.isJsonEqualTo
import it.neckar.open.unit.si.W
import it.neckar.rest.openapi.spec.harvest.HarvestedSpecSupport
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import it.neckar.rest.openapi.documentation.DocumentationProvider
import it.neckar.rest.openapi.schema.SchemaDescriptor

/**
 *
 */
class OpenApiDescriptionTest {
  @Test
  fun testGenerateSchema() {
    val allSpecs = HarvestedSpecSupport.loadAllSpecs()
    val propSpec = allSpecs.first().getClassSpec(MyClass::class).findPropertySpec("watt")
    assertThat(propSpec).isNotNull()
    propSpec.requireNotNull()
    assertThat(propSpec.annotations).hasSize(0)
    assertThat(propSpec.typeAnnotations).hasSize(1)

    val documentationProvider = DocumentationProvider.usingHarvestedSpecs(allSpecs)

    assertThat(documentationProvider.getDocumentationForType(MyClass::class).toString()).contains("The doc for MyClass")

    val generator = OpenApiJsonGenerator(
      documentationProvider = documentationProvider
    )

    val generated = generator.generateComponentsSchemasForRoutes(emptyList(), listOf(SchemaDescriptor.of(MyClass::class)))

    assertThat(generated).isJsonEqualTo(
      //language=JSON
      """
        {
          "OpenApiDescriptionTest.MyClass" : {
            "type" : "object",
            "x-source-kotlin-type" : "it.neckar.rest.openapi.OpenApiDescriptionTest.MyClass",
            "description" : "The doc for MyClass",
            "properties" : {
              "watt" : {
                "description" : "The doc for watt! [Watt]",
                "type" : "number",
                "format" : "double",
                "x-unit" : "W"
              }
            },
            "required" : [ "watt" ]
          }
        }
      """.trimIndent()
    )
  }

  /**
   * The doc for MyClass
   */
  @Serializable
  private data class MyClass(
    /**
     * The doc for watt!
     */
    val watt: @W Double
  )
}
