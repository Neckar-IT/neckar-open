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

import assertk.assertThat
import it.neckar.open.serialization.atPath
import it.neckar.open.serialization.isJsonEqualTo
import it.neckar.rest.openapi.documentation.DocumentationProvider
import it.neckar.rest.openapi.documentation.ValueClassMetadata
import it.neckar.rest.openapi.schema.SchemaDescriptor
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass

/**
 * A value class carries its unit and German term as `x-unit-symbol` / `x-unit-name` /
 * `x-german-term` schema keys, resolved from the [DocumentationProvider] and written next to the
 * underlying primitive type.
 */
class OpenApiValueClassMetadataTest {
  @JvmInline
  @Serializable
  value class Weight(val grams: Double)

  @Serializable
  private data class Parcel(val weight: Weight)

  @Serializable
  private data class Shipment(
    val weights: List<Weight>,
    val sampled: Set<Weight>,
    val perParcel: Map<String, Weight>,
  )

  /** Returns unit + German-term metadata for [Weight], nothing else. */
  private val documentationProvider = object : DocumentationProvider by DocumentationProvider.None {
    override fun getValueClassMetadata(clazz: KClass<*>): ValueClassMetadata? =
      if (clazz == Weight::class) ValueClassMetadata(unitSymbol = "g", unitName = "grams", germanTerm = "Gewicht") else null
  }

  @Test
  fun `value class property carries unit and german-term metadata`() {
    val parcel = SchemaDescriptor.of<Parcel>()

    val weight = OpenApiJsonGenerator(documentationProvider = documentationProvider)
      .generateComponentsSchemas(listOf(parcel))
      .atPath(parcel.componentPath() + "properties" + "weight")

    assertThat(weight).isJsonEqualTo(
      //language=JSON
      """
      {
        "x-source-kotlin-type": "it.neckar.rest.openapi.OpenApiValueClassMetadataTest.Weight",
        "x-unit-symbol": "g",
        "x-unit-name": "grams",
        "x-german-term": "Gewicht",
        "type": "number",
        "format": "double"
      }
      """.trimIndent()
    )
  }

  @Test
  fun `a collection element and a map value carry the same metadata as a property`() {
    val shipment = SchemaDescriptor.of<Shipment>()
    val schemas = OpenApiJsonGenerator(documentationProvider = documentationProvider).generateComponentsSchemas(listOf(shipment))

    //language=JSON
    val element = """
      {
        "x-source-kotlin-type": "it.neckar.rest.openapi.OpenApiValueClassMetadataTest.Weight",
        "x-unit-symbol": "g",
        "x-unit-name": "grams",
        "x-german-term": "Gewicht",
        "type": "number",
        "format": "double"
      }
    """.trimIndent()

    assertThat(schemas.atPath(shipment.componentPath() + "properties" + "weights" + "items")).isJsonEqualTo(element)
    assertThat(schemas.atPath(shipment.componentPath() + "properties" + "sampled" + "items")).isJsonEqualTo(element)
    assertThat(schemas.atPath(shipment.componentPath() + "properties" + "perParcel" + "additionalProperties")).isJsonEqualTo(element)
  }
}
