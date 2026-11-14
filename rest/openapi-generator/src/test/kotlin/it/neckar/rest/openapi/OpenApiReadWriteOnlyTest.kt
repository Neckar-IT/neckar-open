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
 * A `@ReadOnly` property is emitted with `readOnly: true`, a `@WriteOnly` property with `writeOnly: true`.
 */
class OpenApiReadWriteOnlyTest {
  @Serializable
  private data class Account(val id: String, val password: String, val legacyToken: String)

  private val documentationProvider = object : DocumentationProvider by DocumentationProvider.None {
    override fun isReadOnlyProperty(clazz: KClass<*>, propertyName: String): Boolean = propertyName == "id"
    override fun isWriteOnlyProperty(clazz: KClass<*>, propertyName: String): Boolean = propertyName == "password"
    override fun isDeprecatedProperty(clazz: KClass<*>, propertyName: String): Boolean = propertyName == "legacyToken"
    override fun getDeprecatedReasonForProperty(clazz: KClass<*>, propertyName: String): String? =
      if (propertyName == "legacyToken") "Use the OAuth2 flow instead" else null
  }

  private fun schemaOf(property: String) =
    OpenApiJsonGenerator(documentationProvider = documentationProvider)
      .generateComponentsSchemas(listOf(SchemaDescriptor.of<Account>()))
      .atPath(JsonPath("OpenApiReadWriteOnlyTest.Account", "properties", property))

  @Test
  fun `read-only property is marked readOnly`() {
    assertThat(schemaOf("id")).isJsonEqualTo(
      //language=JSON
      """{ "readOnly": true, "type": "string" }"""
    )
  }

  @Test
  fun `write-only property is marked writeOnly`() {
    assertThat(schemaOf("password")).isJsonEqualTo(
      //language=JSON
      """{ "writeOnly": true, "type": "string" }"""
    )
  }

  @Test
  fun `deprecated property is marked deprecated with its reason`() {
    assertThat(schemaOf("legacyToken")).isJsonEqualTo(
      //language=JSON
      """{ "deprecated": true, "x-deprecated-reason": "Use the OAuth2 flow instead", "type": "string" }"""
    )
  }
}
