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
import it.neckar.open.annotations.OpenApiBaseSchema
import it.neckar.open.serialization.isJsonEqualTo
import it.neckar.open.serialization.path
import it.neckar.rest.openapi.documentation.DocumentationProvider
import it.neckar.rest.openapi.schema.SchemaDescriptor
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Test

class OpenApiBaseSchemaTighteningTest {
  private val jsonGenerator = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None)

  private fun generateWithDetailsFirst() = jsonGenerator.generate(
    routes = emptyList(),
    additionalTypes = listOf(SchemaDescriptor.of(GadgetDetails::class), SchemaDescriptor.of(NewGadgetTemplate::class)),
    metaData = OpenApiMetaData.default,
  ).jsonObject

  @Test
  fun testImplementerRestatesAPropertyItDeclaresNonNull() {
    assertThat(generateWithDetailsFirst()).path("components", "schemas", "OpenApiBaseSchemaTighteningTest.GadgetDetails")
      .transform("inlined property names") { it.getValue("allOf").jsonArray[1].jsonObject.getValue("properties").jsonObject.keys }
      .containsExactlyInAnyOrder("label", "archived")
  }

  @Test
  fun testBaseSchemaKeepsTheNullableTypeWhenTheNarrowingImplementerComesFirst() {
    assertThat(generateWithDetailsFirst()).path("components", "schemas", "OpenApiBaseSchemaTighteningTest.GadgetSource", "properties", "label").isJsonEqualTo(
      //language=JSON
      """
        {
          "type" : [ "string", "null" ]
        }
      """.trimIndent()
    )
  }

  @Test
  fun testTemplateDeclaringTheInterfaceTypeRestatesNothing() {
    assertThat(generateWithDetailsFirst()).path("components", "schemas", "OpenApiBaseSchemaTighteningTest.NewGadgetTemplate")
      .transform("inlined property names") { it.getValue("allOf").jsonArray[1].jsonObject.getValue("properties").jsonObject.keys }
      .containsExactlyInAnyOrder("draft")
  }

  @OpenApiBaseSchema
  private interface GadgetSource {
    val id: String
    val label: String?
  }

  @Serializable
  private data class GadgetDetails(
    override val id: String,
    override val label: String,
    val archived: Boolean,
  ) : GadgetSource

  @Serializable
  private data class NewGadgetTemplate(
    override val id: String,
    override val label: String?,
    val draft: Boolean,
  ) : GadgetSource
}
