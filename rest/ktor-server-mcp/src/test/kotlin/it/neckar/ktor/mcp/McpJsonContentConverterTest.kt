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
package it.neckar.ktor.mcp

import assertk.assertThat
import assertk.assertions.isEqualTo
import io.ktor.http.*
import io.ktor.http.content.*
import io.ktor.serialization.kotlinx.*
import io.ktor.util.reflect.*
import io.ktor.utils.io.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import org.junit.jupiter.api.Test

class McpJsonContentConverterTest {
  private val converter: McpJsonContentConverter = McpJsonContentConverter(KotlinxSerializationConverter(Json { explicitNulls = true }))

  @Test
  suspend fun `a service value keeps the service's explicit nulls`() {
    val content = converter.serialize(ContentType.Application.Json, Charsets.UTF_8, typeInfo<ServiceValue>(), ServiceValue(name = null)) as TextContent

    assertThat(content.text).isEqualTo("""{"name":null}""")
  }

  @Test
  suspend fun `a nullable receive reaches the service converter`() {
    val received = converter.deserialize(Charsets.UTF_8, typeInfo<ServiceValue?>(), ByteReadChannel("""{"name":"Anna"}"""))

    assertThat(received).isEqualTo(ServiceValue(name = "Anna"))
  }

  @Serializable
  private data class ServiceValue(val name: String?)
}
