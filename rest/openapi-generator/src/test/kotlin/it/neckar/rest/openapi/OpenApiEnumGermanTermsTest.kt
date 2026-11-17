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
import it.neckar.open.kotlin.serializers.JsonPath
import it.neckar.open.serialization.atPath
import it.neckar.open.serialization.isJsonEqualTo
import it.neckar.rest.openapi.documentation.DocumentationProvider
import it.neckar.rest.openapi.schema.SchemaDescriptor
import kotlinx.serialization.Serializable
import org.junit.jupiter.api.Test
import kotlin.reflect.KClass

/**
 * The `@GermanTerm` of an enum class is emitted as `x-german-term`, the one of each constant as `x-enum-german-terms`
 * (an object keyed by enum value).
 */
class OpenApiEnumGermanTermsTest {
  @Serializable
  enum class ContactRole {
    Editorial,
    Sales,
  }

  private val documentationProvider = object : DocumentationProvider by DocumentationProvider.None {
    override fun getGermanTermForClass(clazz: KClass<*>): String? = "Funktion".takeIf { clazz == ContactRole::class }

    override fun getGermanTermForEnumEntry(enumClass: KClass<*>, entryName: String): String? = when (entryName) {
      "Editorial" -> "Lektorat"
      else -> null
    }
  }

  @Test
  fun `German terms of an enum class and its constants are emitted`() {
    val schemas = OpenApiJsonGenerator(documentationProvider = documentationProvider)
      .generateComponentsSchemas(listOf(SchemaDescriptor.of<ContactRole>()))

    assertThat(schemas.atPath(JsonPath("OpenApiEnumGermanTermsTest.ContactRole"))).isJsonEqualTo(
      //language=JSON
      """
      {
        "type": "string",
        "enum": ["Editorial", "Sales"],
        "x-enum-german-terms": {
          "Editorial": "Lektorat"
        },
        "x-german-term": "Funktion"
      }
      """.trimIndent()
    )
  }
}
