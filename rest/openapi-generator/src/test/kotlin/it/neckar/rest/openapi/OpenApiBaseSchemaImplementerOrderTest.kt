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
import it.neckar.open.serialization.path
import it.neckar.rest.openapi.documentation.DocumentationProvider
import it.neckar.rest.openapi.schema.SchemaDescriptor
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.jsonObject
import org.junit.jupiter.api.Test

/**
 * [GadgetDetails] answers [GadgetSource.batteryCells] through a getter and so does not serialize it, while
 * [NewGadgetTemplate] serializes every property. The base schema lists every property whichever implementer
 * is processed first.
 */
class OpenApiBaseSchemaImplementerOrderTest {
  private val jsonGenerator = OpenApiJsonGenerator(documentationProvider = DocumentationProvider.None)

  @Test
  fun testBaseSchemaListsEveryPropertyWhenTheFirstImplementerOmitsOne() {
    val generated = jsonGenerator.generate(
      routes = emptyList(),
      additionalTypes = listOf(SchemaDescriptor.of(GadgetDetails::class), SchemaDescriptor.of(NewGadgetTemplate::class)),
      metaData = OpenApiMetaData.default,
    )

    assertThat(generated.jsonObject).path("components", "schemas", "OpenApiBaseSchemaImplementerOrderTest.GadgetSource", "properties")
      .transform("keys") { it.keys }
      .containsExactlyInAnyOrder("id", "label", "batteryCells")
  }

  @OpenApiBaseSchema
  private interface GadgetSource {
    val id: String
    val label: String?
    val batteryCells: Int?
  }

  @Serializable
  private data class GadgetDetails(
    override val id: String,
    override val label: String?,
    val archived: Boolean,
  ) : GadgetSource {
    override val batteryCells: Int? get() = null
  }

  @Serializable
  private data class NewGadgetTemplate(
    override val id: String,
    override val label: String?,
    override val batteryCells: Int?,
  ) : GadgetSource
}
